package com.benjaminwan.ocrlibrary.config

import com.benjaminwan.ocrlibrary.OcrModelVersion

/**
 * OCR 模型路径配置
 *
 * 支持配置检测、分类、识别模型和字典文件的路径
 *
 * 路径支持两种形式：
 * - **绝对路径**：以 / 开头的完整文件路径，如 "/sdcard/models/det.onnx"
 * - **相对路径**：相对于默认模型目录的路径，如 "det_v4.onnx"
 *   - 文件系统相对路径：context.filesDir/models/
 *   - Assets 相对路径：assets/
 *
 * @param detModelPath 检测模型路径（可选）
 * @param clsModelPath 分类模型路径（可选）
 * @param recModelPath 识别模型路径（可选）
 * @param keysPath 字典文件路径（可选）
 */
data class OcrPathConfig(
    val detModelPath: String? = null,
    val clsModelPath: String? = null,
    val recModelPath: String? = null,
    val keysPath: String? = null
) {
    companion object {
        /**
         * 创建配置构建器
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    /**
     * 路径配置构建器
     *
     * 使用 Builder 模式提供类型安全的配置 API
     *
     * @sample
     * ```kotlin
     * val config = OcrPathConfig.builder()
     *     .detModelPath("/sdcard/models/det.onnx")
     *     .recModelPath("/sdcard/models/rec.onnx")
     *     .build()
     * ```
     */
    class Builder {
        private var detModelPath: String? = null
        private var clsModelPath: String? = null
        private var recModelPath: String? = null
        private var keysPath: String? = null

        /**
         * 设置检测模型路径
         */
        fun detModelPath(path: String): Builder {
            this.detModelPath = path
            return this
        }

        /**
         * 设置分类模型路径
         */
        fun clsModelPath(path: String): Builder {
            this.clsModelPath = path
            return this
        }

        /**
         * 设置识别模型路径
         */
        fun recModelPath(path: String): Builder {
            this.recModelPath = path
            return this
        }

        /**
         * 设置字典文件路径
         */
        fun keysPath(path: String): Builder {
            this.keysPath = path
            return this
        }

        /**
         * 批量设置所有路径（用于同一目录下的模型）
         *
         * 将基础目录与各个模型文件名拼接成完整路径
         *
         * @param baseDir 基础目录路径，会自动去除末尾的 /
         * @param detName 检测模型文件名
         * @param clsName 分类模型文件名
         * @param recName 识别模型文件名
         * @param keysName 字典文件名
         *
         * @sample
         * ```kotlin
         * allPaths(
         *     baseDir = "/sdcard/ocr_models",
         *     detName = "det.onnx",
         *     clsName = "cls.onnx",
         *     recName = "rec.onnx",
         *     keysName = "keys.txt"
         * )
         * ```
         */
        fun allPaths(
            baseDir: String,
            detName: String,
            clsName: String,
            recName: String,
            keysName: String
        ): Builder {
            val base = baseDir.trimEnd('/')
            this.detModelPath = "$base/$detName"
            this.clsModelPath = "$base/$clsName"
            this.recModelPath = "$base/$recName"
            this.keysPath = "$base/$keysName"
            return this
        }

        /**
         * 基于模型版本设置路径（使用自定义目录）
         *
         * 从 [OcrModelVersion] 中获取默认文件名，与指定目录拼接
         *
         * @param baseDir 基础目录路径
         * @param version 模型版本
         *
         * @sample
         * ```kotlin
         * fromVersion("/sdcard/ocr_v4", OcrModelVersion.V4)
         * // 将使用 V4 版本的文件名：
         * // - /sdcard/ocr_v4/ch_PP-OCRv4_det_infer.onnx
         * // - /sdcard/ocr_v4/ch_PP-OCRv4_rec_infer.onnx
         * // - 等等
         * ```
         */
        fun fromVersion(baseDir: String, version: OcrModelVersion): Builder {
            val base = baseDir.trimEnd('/')
            return allPaths(
                baseDir = base,
                detName = version.detModelName,
                clsName = version.clsModelName,
                recName = version.recModelName,
                keysName = version.keysName
            )
        }

        /**
         * 构建配置对象
         */
        fun build(): OcrPathConfig {
            return OcrPathConfig(detModelPath, clsModelPath, recModelPath, keysPath)
        }
    }
}
