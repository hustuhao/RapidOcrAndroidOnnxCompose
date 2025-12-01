package com.benjaminwan.ocrlibrary

import android.content.Context
import android.content.res.AssetManager
import java.io.File

/**
 * 模型文件加载工具类
 * 统一处理从内部存储或assets加载模型文件和字典文件的逻辑
 */
internal object ModelLoader {

    /**
     * 加载模型文件
     * 优先从内部存储（context.filesDir/models/）加载，若不存在则降级到assets目录
     */
    fun loadModel(context: Context, modelName: String): ByteArray {
        // 优先从内部存储加载
        val file = File(context.filesDir, "models/$modelName")
        return if (file.exists()) {
            file.readBytes()
        } else {
            // 降级到assets
            context.assets.open(modelName, AssetManager.ACCESS_UNKNOWN).readBytes()
        }
    }

    /**
     * 加载字典文件
     * 优先从内部存储（context.filesDir/models/）加载，若不存在则降级到assets目录
     */
    fun loadKeys(context: Context, keysName: String): java.io.BufferedReader {
        // 优先从内部存储加载
        val file = File(context.filesDir, "models/$keysName")
        return if (file.exists()) {
            file.bufferedReader()
        } else {
            // 降级到assets
            context.assets.open(keysName, AssetManager.ACCESS_UNKNOWN).bufferedReader()
        }
    }
}
