package com.benjaminwan.ocrlibrary.config

import android.content.Context
import android.content.res.AssetManager
import com.benjaminwan.ocrlibrary.OcrModelVersion
import com.orhanobut.logger.Logger
import java.io.File

/**
 * 模型路径解析器
 *
 * 负责解析和验证模型文件路径，处理配置优先级
 *
 * 配置优先级（从高到低）：
 * 1. 实例级配置（OcrEngine 构造时传入的 config）
 * 2. 全局配置（OcrConfigManager 中的全局配置）
 * 3. 版本默认路径（OcrModelVersion 定义的文件名）
 */
internal object ModelPathResolver {

    /**
     * 解析模型路径
     *
     * 根据配置优先级解析所有模型文件的路径
     *
     * @param context Android Context
     * @param config 配置对象（可能来自实例或全局配置）
     * @param version 模型版本
     * @return 解析后的路径集合
     */
    fun resolve(
        context: Context,
        config: OcrConfig?,
        version: OcrModelVersion
    ): ResolvedPaths {
        val pathConfig = config?.pathConfig

        Logger.i("Resolving model paths for version ${version.versionName}")
        if (pathConfig != null) {
            Logger.i("Using custom path config")
        } else {
            Logger.i("Using default paths from version")
        }

        return ResolvedPaths(
            detPath = resolveSinglePath(
                customPath = pathConfig?.detModelPath,
                defaultName = version.detModelName,
                pathType = "det"
            ),
            clsPath = resolveSinglePath(
                customPath = pathConfig?.clsModelPath,
                defaultName = version.clsModelName,
                pathType = "cls"
            ),
            recPath = resolveSinglePath(
                customPath = pathConfig?.recModelPath,
                defaultName = version.recModelName,
                pathType = "rec"
            ),
            keysPath = resolveSinglePath(
                customPath = pathConfig?.keysPath,
                defaultName = version.keysName,
                pathType = "keys"
            )
        )
    }

    /**
     * 解析单个路径
     *
     * @param customPath 自定义路径（来自配置）
     * @param defaultName 默认文件名（来自版本定义）
     * @param pathType 路径类型（用于日志）
     * @return 解析后的路径
     */
    private fun resolveSinglePath(
        customPath: String?,
        defaultName: String,
        pathType: String
    ): ResolvedPath {
        // 处理空字符串为 null
        val effectivePath = customPath?.takeIf { it.isNotBlank() }

        return if (effectivePath != null) {
            val isAbsolute = effectivePath.startsWith("/")
            Logger.i("Resolved $pathType path: $effectivePath (absolute=$isAbsolute, source=CUSTOM_CONFIG)")
            ResolvedPath(
                path = effectivePath,
                isAbsolute = isAbsolute,
                source = PathSource.CUSTOM_CONFIG
            )
        } else {
            Logger.i("Resolved $pathType path: $defaultName (absolute=false, source=VERSION_DEFAULT)")
            ResolvedPath(
                path = defaultName,
                isAbsolute = false,
                source = PathSource.VERSION_DEFAULT
            )
        }
    }

    /**
     * 验证路径是否可访问
     *
     * 根据加载策略检查路径是否存在且可读
     *
     * @param context Android Context
     * @param paths 已解析的路径集合
     * @param strategy 加载策略
     * @return 验证结果
     */
    fun validate(
        context: Context,
        paths: ResolvedPaths,
        strategy: OcrLoadStrategy
    ): ValidationResult {
        Logger.i("Validating model paths with strategy $strategy")
        val errors = mutableListOf<String>()

        listOf(
            "det" to paths.detPath,
            "cls" to paths.clsPath,
            "rec" to paths.recPath,
            "keys" to paths.keysPath
        ).forEach { (type, resolvedPath) ->
            if (!canLoadPath(context, resolvedPath, strategy)) {
                val error = "Cannot load $type model from ${resolvedPath.path} with strategy $strategy"
                Logger.w(error)
                errors.add(error)
            } else {
                Logger.i("Validated $type model path: ${resolvedPath.path}")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * 检查路径是否可加载
     *
     * 根据加载策略判断路径是否可访问
     *
     * @param context Android Context
     * @param resolvedPath 已解析的路径
     * @param strategy 加载策略
     * @return true 表示可加载，false 表示不可加载
     */
    private fun canLoadPath(
        context: Context,
        resolvedPath: ResolvedPath,
        strategy: OcrLoadStrategy
    ): Boolean {
        return when (strategy) {
            OcrLoadStrategy.FILE_FIRST, OcrLoadStrategy.ASSETS_FIRST -> {
                // 双向降级策略，只要有一个源可用就返回 true
                canLoadFromFile(context, resolvedPath) || canLoadFromAssets(context, resolvedPath)
            }
            OcrLoadStrategy.FILE_ONLY -> {
                canLoadFromFile(context, resolvedPath)
            }
            OcrLoadStrategy.ASSETS_ONLY -> {
                canLoadFromAssets(context, resolvedPath)
            }
        }
    }

    /**
     * 检查是否可以从文件系统加载
     *
     * @param context Android Context
     * @param resolvedPath 已解析的路径
     * @return true 表示文件存在且可读
     */
    private fun canLoadFromFile(context: Context, resolvedPath: ResolvedPath): Boolean {
        val file = if (resolvedPath.isAbsolute) {
            File(resolvedPath.path)
        } else {
            File(context.filesDir, "models/${resolvedPath.path}")
        }
        return file.exists() && file.canRead()
    }

    /**
     * 检查是否可以从 assets 加载
     *
     * @param context Android Context
     * @param resolvedPath 已解析的路径
     * @return true 表示 assets 中存在该文件
     */
    private fun canLoadFromAssets(context: Context, resolvedPath: ResolvedPath): Boolean {
        return try {
            // 绝对路径不能从 assets 加载
            if (resolvedPath.isAbsolute) return false

            // 尝试打开文件以检查是否存在
            context.assets.open(resolvedPath.path, AssetManager.ACCESS_UNKNOWN).use { true }
        } catch (e: Exception) {
            false
        }
    }
}
