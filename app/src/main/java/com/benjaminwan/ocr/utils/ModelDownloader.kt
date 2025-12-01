package com.benjaminwan.ocr.utils

import android.content.Context
import com.benjaminwan.ocrlibrary.OcrModelVersion
import com.orhanobut.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class ModelDownloader(private val context: Context) {

    sealed class DownloadResult {
        object Success : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

    suspend fun downloadModelsForVersion(
        version: OcrModelVersion,
        onProgress: (Float) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            val filesToDownload = mutableListOf<Pair<String, String>>()

            // 检查需要下载哪些文件
            if (version.detUrl.isNotEmpty()) {
                val detFile = File(modelsDir, version.detModelName)
                if (!detFile.exists()) {
                    filesToDownload.add(version.detUrl to version.detModelName)
                }
            }

            if (version.recUrl.isNotEmpty()) {
                val recFile = File(modelsDir, version.recModelName)
                if (!recFile.exists()) {
                    filesToDownload.add(version.recUrl to version.recModelName)
                }
            }

            if (version.keysUrl != null) {
                val keysFile = File(modelsDir, version.keysName)
                if (!keysFile.exists()) {
                    filesToDownload.add(version.keysUrl!! to version.keysName)
                }
            }

            if (filesToDownload.isEmpty()) {
                Logger.i("All models for ${version.versionName} already exist")
                return@withContext DownloadResult.Success
            }

            Logger.i("Downloading ${filesToDownload.size} files for ${version.versionName}")

            // 下载每个文件
            filesToDownload.forEachIndexed { index, (url, fileName) ->
                val baseProgress = index.toFloat() / filesToDownload.size
                val progressRange = 1f / filesToDownload.size

                Logger.i("Downloading $fileName from $url")
                downloadFile(
                    url = url,
                    targetFile = File(modelsDir, fileName),
                    onProgress = { fileProgress ->
                        onProgress(baseProgress + fileProgress * progressRange)
                    }
                )
            }

            Logger.i("All models downloaded successfully")
            DownloadResult.Success
        } catch (e: Exception) {
            Logger.e("Failed to download models: ${e.message}", e)
            DownloadResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun downloadFile(
        url: String,
        targetFile: File,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection()
        connection.connect()

        val fileLength = connection.contentLength
        val input = connection.getInputStream()
        val output = targetFile.outputStream()

        val buffer = ByteArray(8192)
        var total: Long = 0
        var count: Int

        try {
            while (input.read(buffer).also { count = it } != -1) {
                total += count
                output.write(buffer, 0, count)

                if (fileLength > 0) {
                    onProgress(total.toFloat() / fileLength)
                }
            }
        } finally {
            output.flush()
            output.close()
            input.close()
        }
    }

    fun isVersionDownloaded(version: OcrModelVersion): Boolean {
        val modelsDir = File(context.filesDir, "models")

        // V3 预置在assets中，不需要下载
        if (version == OcrModelVersion.V3) {
            return true
        }

        // 检查必需的模型文件是否存在
        val detFile = File(modelsDir, version.detModelName)
        val recFile = File(modelsDir, version.recModelName)
        val keysFile = if (version.keysUrl != null) {
            File(modelsDir, version.keysName)
        } else {
            null  // V4 使用 V3 的字典，不需要单独下载
        }

        return detFile.exists() && recFile.exists() && (keysFile?.exists() != false)
    }
}
