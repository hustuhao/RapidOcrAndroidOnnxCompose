package com.benjaminwan.ocr.screens.gallery

import android.content.Context
import android.net.Uri
import com.airbnb.mvrx.*
import com.benjaminwan.ocr.app.App
import com.benjaminwan.ocr.base.BaseViewModel
import com.benjaminwan.ocr.screens.gallery.GalleryState.Companion.boxScoreThreshRange
import com.benjaminwan.ocr.screens.gallery.GalleryState.Companion.boxThreshRange
import com.benjaminwan.ocr.screens.gallery.GalleryState.Companion.maxSideLenRange
import com.benjaminwan.ocr.screens.gallery.GalleryState.Companion.paddingRange
import com.benjaminwan.ocr.screens.gallery.GalleryState.Companion.unClipRatioRange
import com.benjaminwan.ocr.storage.PreferencesManager
import com.benjaminwan.ocr.utils.ModelDownloader
import com.benjaminwan.ocr.utils.decodeUri
import com.benjaminwan.ocr.utils.toClipboard
import com.benjaminwan.ocrlibrary.OcrEngine
import com.benjaminwan.ocrlibrary.OcrModelVersion
import com.orhanobut.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryViewModel(
    initialState: GalleryState,
    private val context: Context,
) : BaseViewModel<GalleryState>(initialState) {

    private val preferencesManager = PreferencesManager(App.INST.applicationContext)
    private val modelDownloader = ModelDownloader(App.INST.applicationContext)
    private var ocrEngine: OcrEngine

    init {
        // 从持久化存储加载保存的版本
        val savedVersionName = preferencesManager.getModelVersion()
        val savedVersion = OcrModelVersion.fromName(savedVersionName)
        setState { copy(modelVersion = savedVersion) }

        // 初始化 OcrEngine
        ocrEngine = OcrEngine(App.INST.applicationContext, savedVersion)
    }

    fun setSelectTab(tab: GalleryTab) {
        setState { copy(selectTab = tab) }
    }

    fun setUri(uri: Uri? = null) {
        setState { copy(imageUri = uri, selectTab = tabs.first()) }
    }

    fun setScaleUp(input: Boolean) {
        setState { copy(scaleUp = input) }
    }

    fun setMaxSideLen(input: String) {
        val value = try {
            input.toInt()
        } catch (e: Exception) {
            null
        }
        setState { copy(maxSideLen = input, maxSideLenError = value == null || value !in maxSideLenRange) }
    }

    fun setPadding(input: String) {
        val value = try {
            input.toInt()
        } catch (e: Exception) {
            null
        }
        setState { copy(padding = input, paddingError = value == null || value !in paddingRange) }
    }

    fun setBoxScoreThresh(input: String) {
        val value = input.toFloatOrNull()
        setState { copy(boxScoreThresh = input, boxScoreThreshError = value == null || value !in boxScoreThreshRange) }
    }

    fun setBoxThresh(input: String) {
        val value = input.toFloatOrNull()
        setState { copy(boxThresh = input, boxThreshError = value == null || value !in boxThreshRange) }
    }

    fun setUnClipRatio(input: String) {
        val value = input.toFloatOrNull()
        setState { copy(unClipRatio = input, unClipRatioError = value == null || value !in unClipRatioRange) }
    }

    fun setDoCls(input: Boolean) {
        setState { copy(doCls = input, mostCls = if (!input) false else mostCls) }
    }

    fun setMostCls(input: Boolean) {
        setState { copy(mostCls = input) }
    }

    fun setModelVersion(version: OcrModelVersion) {
        viewModelScope.launch {
            try {
                setState { copy(isDownloadingModel = true, downloadProgress = 0f) }

                // 检查模型是否已下载
                if (!modelDownloader.isVersionDownloaded(version)) {
                    Logger.i("Downloading models for ${version.versionName}")

                    // 下载模型
                    val result = modelDownloader.downloadModelsForVersion(version) { progress ->
                        setState { copy(downloadProgress = progress) }
                    }

                    when (result) {
                        is ModelDownloader.DownloadResult.Success -> {
                            Logger.i("Models downloaded successfully")
                        }
                        is ModelDownloader.DownloadResult.Error -> {
                            showError("下载失败: ${result.message}")
                            setState { copy(isDownloadingModel = false) }
                            return@launch
                        }
                    }
                } else {
                    Logger.i("Models for ${version.versionName} already exist")
                }

                // 关闭旧的 OcrEngine
                ocrEngine.close()

                // 创建新的 OcrEngine
                ocrEngine = OcrEngine(App.INST.applicationContext, version)

                // 保存到持久化存储
                preferencesManager.setModelVersion(version.name)

                // 更新状态
                setState {
                    copy(
                        modelVersion = version,
                        isDownloadingModel = false,
                        downloadProgress = 1f
                    )
                }

                showSuccess("已切换到 ${version.versionName}")
            } catch (e: Exception) {
                Logger.e("Failed to switch model version", e)
                showError("切换版本失败: ${e.message}")
                setState { copy(isDownloadingModel = false) }
            }
        }
    }

    fun detect() = suspend {
        val state = awaitState()
        val uri = state.imageUri ?: throw Exception("uri is null")
        val bmp = context.decodeUri(uri) ?: throw Exception("bitmap is null")
        val scaleUp = state.scaleUp
        val maxSideLen = state.maxSideLen.toInt()
        val padding = state.padding.toInt()
        val boxScoreThresh = state.boxScoreThresh.toFloat()
        val boxThresh = state.boxThresh.toFloat()
        val unClipRatio = state.unClipRatio.toFloat()
        val doCls = state.doCls
        val mostCls = state.mostCls
        withContext(Dispatchers.IO) {
            /*val count = 10
            val time = measureTimeMillis {
                (0 until count).forEach {
                    ocrEngine.detect(bmp, scaleUp, maxSideLen, padding, boxScoreThresh, boxThresh, unClipRatio, doCls, mostCls)
                }
            }
            Logger.e("平均耗时=${time.toFloat() / count.toFloat()}")*/
            ocrEngine.detect(bmp, scaleUp, maxSideLen, padding, boxScoreThresh, boxThresh, unClipRatio, doCls, mostCls)
        }
    }
        .execute {
            if (it is Fail) showError(it.error.toString())
            val select = when (it) {
                Uninitialized -> tabs.first()
                is Fail -> tabs.first()
                is Loading -> tabs.first()
                is Success -> GalleryTab.TextResult
            }
            copy(detectRequest = it, selectTab = select)
        }

    fun toClipboard(text: String) {
        context.toClipboard(text)
        showInfo("已复制到剪切板!")
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.close()
    }

    companion object : MavericksViewModelFactory<GalleryViewModel, GalleryState> {
        override fun create(viewModelContext: ViewModelContext, state: GalleryState): GalleryViewModel? {
            val context = viewModelContext.activity.applicationContext
            return GalleryViewModel(state, context)
        }
    }

}