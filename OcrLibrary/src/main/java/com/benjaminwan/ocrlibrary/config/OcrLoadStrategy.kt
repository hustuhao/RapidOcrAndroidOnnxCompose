package com.benjaminwan.ocrlibrary.config

/**
 * 模型加载策略
 *
 * 定义从不同来源加载模型文件的策略，支持文件系统和 assets 的组合加载
 */
enum class OcrLoadStrategy {
    /**
     * 优先从文件系统加载，失败时降级到 assets
     * 这是默认策略，与当前行为一致
     *
     * 加载顺序：
     * 1. context.filesDir/models/{modelName}
     * 2. assets/{modelName}
     */
    FILE_FIRST,

    /**
     * 优先从 assets 加载，失败时降级到文件系统
     *
     * 加载顺序：
     * 1. assets/{modelName}
     * 2. context.filesDir/models/{modelName}
     */
    ASSETS_FIRST,

    /**
     * 仅从文件系统加载，不降级
     * 适用于必须使用外部模型的场景
     *
     * 加载来源：
     * - context.filesDir/models/{modelName}
     * - 或绝对路径指定的文件
     */
    FILE_ONLY,

    /**
     * 仅从 assets 加载，不降级
     * 适用于仅使用内置模型的场景
     *
     * 加载来源：
     * - assets/{modelName}
     */
    ASSETS_ONLY
}
