package com.benjaminwan.ocrlibrary

import android.content.Context
import android.content.res.AssetManager
import com.benjaminwan.ocrlibrary.config.OcrLoadStrategy
import com.benjaminwan.ocrlibrary.config.ResolvedPath
import com.orhanobut.logger.Logger
import java.io.BufferedReader
import java.io.File

/**
 * 模型文件加载工具类
 *
 * 统一处理从内部存储或 assets 加载模型文件和字典文件的逻辑
 * 支持多种加载策略和路径配置
 */
internal object ModelLoader {

    /**
     * 加载模型文件（新方法）
     *
     * 根据解析后的路径和加载策略加载模型
     *
     * @param context Android Context
     * @param resolvedPath 已解析的路径
     * @param strategy 加载策略
     * @return 模型文件的字节数组
     * @throws ModelLoadException 加载失败时抛出
     */
    fun loadModel(
        context: Context,
        resolvedPath: ResolvedPath,
        strategy: OcrLoadStrategy
    ): ByteArray {
        Logger.i("Loading model from ${resolvedPath.path} with strategy $strategy")

        return when (strategy) {
            OcrLoadStrategy.FILE_FIRST -> {
                tryLoadFromFile(context, resolvedPath)
                    ?: tryLoadFromAssets(context, resolvedPath)
                    ?: throw ModelLoadException("Failed to load model from ${resolvedPath.path}")
            }
            OcrLoadStrategy.ASSETS_FIRST -> {
                tryLoadFromAssets(context, resolvedPath)
                    ?: tryLoadFromFile(context, resolvedPath)
                    ?: throw ModelLoadException("Failed to load model from ${resolvedPath.path}")
            }
            OcrLoadStrategy.FILE_ONLY -> {
                tryLoadFromFile(context, resolvedPath)
                    ?: throw ModelLoadException("Failed to load model from file: ${resolvedPath.path}")
            }
            OcrLoadStrategy.ASSETS_ONLY -> {
                tryLoadFromAssets(context, resolvedPath)
                    ?: throw ModelLoadException("Failed to load model from assets: ${resolvedPath.path}")
            }
        }
    }

    /**
     * 加载字典文件（新方法）
     *
     * 根据解析后的路径和加载策略加载字典文件
     *
     * @param context Android Context
     * @param resolvedPath 已解析的路径
     * @param strategy 加载策略
     * @return BufferedReader 用于读取字典内容
     * @throws ModelLoadException 加载失败时抛出
     */
    fun loadKeys(
        context: Context,
        resolvedPath: ResolvedPath,
        strategy: OcrLoadStrategy
    ): BufferedReader {
        Logger.i("Loading keys from ${resolvedPath.path} with strategy $strategy")

        return when (strategy) {
            OcrLoadStrategy.FILE_FIRST -> {
                tryLoadKeysFromFile(context, resolvedPath)
                    ?: tryLoadKeysFromAssets(context, resolvedPath)
                    ?: throw ModelLoadException("Failed to load keys from ${resolvedPath.path}")
            }
            OcrLoadStrategy.ASSETS_FIRST -> {
                tryLoadKeysFromAssets(context, resolvedPath)
                    ?: tryLoadKeysFromFile(context, resolvedPath)
                    ?: throw ModelLoadException("Failed to load keys from ${resolvedPath.path}")
            }
            OcrLoadStrategy.FILE_ONLY -> {
                tryLoadKeysFromFile(context, resolvedPath)
                    ?: throw ModelLoadException("Failed to load keys from file: ${resolvedPath.path}")
            }
            OcrLoadStrategy.ASSETS_ONLY -> {
                tryLoadKeysFromAssets(context, resolvedPath)
                    ?: throw ModelLoadException("Failed to load keys from assets: ${resolvedPath.path}")
            }
        }
    }

    /**
     * 尝试从文件系统加载模型
     *
     * @param context Android Context
     * @param resolvedPath 已解析的路径
     * @return 模型字节数组，失败时返回 null
     */
    private fun tryLoadFromFile(context: Context, resolvedPath: ResolvedPath): ByteArray? {
        return try {
            val file = if (resolvedPath.isAbsolute) {
                File(resolvedPath.path)
            } else {
                File(context.filesDir, "models/${resolvedPath.path}")
            }

            if (file.exists()) {
                Logger.i("Loading model from file: ${file.absolutePath}")
                file.readBytes()
            } else {
                Logger.d("Model file not found: ${file.absolutePath}")
                null
            }
        } catch (e: Exception) {
            Logger.w("Failed to load from file: ${e.message}")
            null
        }
    }

    /**
     * 尝试从 assets 加载模型
     *
     * @param context Android Context
     * @param resolvedPath 已解析的路径
     * @return 模型字节数组，失败时返回 null
     */
    private fun tryLoadFromAssets(context: Context, resolvedPath: ResolvedPath): ByteArray? {
        // 绝对路径不能从 assets 加载
        if (resolvedPath.isAbsolute) {
            Logger.d("Cannot load absolute path from assets: ${resolvedPath.path}")
            return null
        }

        return try {
            Logger.i("Loading model from assets: ${resolvedPath.path}")
            context.assets.open(resolvedPath.path, AssetManager.ACCESS_UNKNOWN).readBytes()
        } catch (e: Exception) {
            Logger.w("Failed to load from assets: ${e.message}")
            null
        }
    }

    /**
     * 尝试从文件系统加载字典
     *
     * @param context Android Context
     * @param resolvedPath 已解析的路径
     * @return BufferedReader，失败时返回 null
     */
    private fun tryLoadKeysFromFile(context: Context, resolvedPath: ResolvedPath): BufferedReader? {
        return try {
            val file = if (resolvedPath.isAbsolute) {
                File(resolvedPath.path)
            } else {
                File(context.filesDir, "models/${resolvedPath.path}")
            }

            if (file.exists()) {
                Logger.i("Loading keys from file: ${file.absolutePath}")
                file.bufferedReader()
            } else {
                Logger.d("Keys file not found: ${file.absolutePath}")
                null
            }
        } catch (e: Exception) {
            Logger.w("Failed to load keys from file: ${e.message}")
            null
        }
    }

    /**
     * 尝试从 assets 加载字典
     *
     * @param context Android Context
     * @param resolvedPath 已解析的路径
     * @return BufferedReader，失败时返回 null
     */
    private fun tryLoadKeysFromAssets(context: Context, resolvedPath: ResolvedPath): BufferedReader? {
        // 绝对路径不能从 assets 加载
        if (resolvedPath.isAbsolute) {
            Logger.d("Cannot load absolute path from assets: ${resolvedPath.path}")
            return null
        }

        return try {
            Logger.i("Loading keys from assets: ${resolvedPath.path}")
            context.assets.open(resolvedPath.path, AssetManager.ACCESS_UNKNOWN).bufferedReader()
        } catch (e: Exception) {
            Logger.w("Failed to load keys from assets: ${e.message}")
            null
        }
    }

    // ========== 向后兼容：保留旧接口 ==========

    /**
     * 加载模型文件（旧方法，已废弃）
     *
     * 优先从内部存储（context.filesDir/models/）加载，若不存在则降级到 assets 目录
     *
     * @deprecated 使用 loadModel(Context, ResolvedPath, OcrLoadStrategy) 替代
     */
    @Deprecated(
        message = "Use loadModel(Context, ResolvedPath, OcrLoadStrategy) instead",
        replaceWith = ReplaceWith("loadModel(context, resolvedPath, strategy)")
    )
    fun loadModel(context: Context, modelName: String): ByteArray {
        // 优先从内部存储加载
        val file = File(context.filesDir, "models/$modelName")
        return if (file.exists()) {
            file.readBytes()
        } else {
            // 降级到 assets
            context.assets.open(modelName, AssetManager.ACCESS_UNKNOWN).readBytes()
        }
    }

    /**
     * 加载字典文件（旧方法，已废弃）
     *
     * 优先从内部存储（context.filesDir/models/）加载，若不存在则降级到 assets 目录
     *
     * @deprecated 使用 loadKeys(Context, ResolvedPath, OcrLoadStrategy) 替代
     */
    @Deprecated(
        message = "Use loadKeys(Context, ResolvedPath, OcrLoadStrategy) instead",
        replaceWith = ReplaceWith("loadKeys(context, resolvedPath, strategy)")
    )
    fun loadKeys(context: Context, keysName: String): BufferedReader {
        // 优先从内部存储加载
        val file = File(context.filesDir, "models/$keysName")
        return if (file.exists()) {
            file.bufferedReader()
        } else {
            // 降级到 assets
            context.assets.open(keysName, AssetManager.ACCESS_UNKNOWN).bufferedReader()
        }
    }
}
