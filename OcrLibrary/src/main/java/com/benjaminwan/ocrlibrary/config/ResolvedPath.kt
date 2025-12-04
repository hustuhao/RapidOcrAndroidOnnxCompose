package com.benjaminwan.ocrlibrary.config

/**
 * 已解析的模型路径集合
 *
 * 包含所有模型文件的解析后路径
 *
 * @param detPath 检测模型路径
 * @param clsPath 分类模型路径
 * @param recPath 识别模型路径
 * @param keysPath 字典文件路径
 */
internal data class ResolvedPaths(
    val detPath: ResolvedPath,
    val clsPath: ResolvedPath,
    val recPath: ResolvedPath,
    val keysPath: ResolvedPath
)

/**
 * 单个已解析的路径
 *
 * 记录路径字符串、类型和来源信息
 *
 * @param path 路径字符串（绝对路径或相对路径）
 * @param isAbsolute 是否为绝对路径（以 / 开头）
 * @param source 路径来源（自定义配置、全局配置或版本默认）
 */
internal data class ResolvedPath(
    val path: String,
    val isAbsolute: Boolean,
    val source: PathSource
)

/**
 * 路径来源
 *
 * 用于追踪路径的配置来源，便于调试和日志记录
 */
internal enum class PathSource {
    /**
     * 用户自定义配置
     *
     * 来自实例级 OcrConfig 的 pathConfig
     */
    CUSTOM_CONFIG,

    /**
     * 全局配置
     *
     * 来自 OcrConfigManager 的全局配置
     */
    GLOBAL_CONFIG,

    /**
     * 版本默认路径
     *
     * 来自 OcrModelVersion 枚举定义的默认路径
     */
    VERSION_DEFAULT
}

/**
 * 路径验证结果
 */
internal sealed class ValidationResult {
    /**
     * 验证成功
     */
    object Success : ValidationResult()

    /**
     * 验证失败
     *
     * @param errors 错误信息列表
     */
    data class Error(val errors: List<String>) : ValidationResult()
}
