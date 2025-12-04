package com.benjaminwan.ocrlibrary

/**
 * 模型加载异常
 *
 * 当模型文件加载失败时抛出此异常
 *
 * 常见原因：
 * - 文件不存在
 * - 文件权限不足
 * - 文件格式错误
 * - 加载策略限制（如 FILE_ONLY 但文件不存在）
 *
 * @param message 错误信息
 * @param cause 原始异常（可选）
 */
class ModelLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
