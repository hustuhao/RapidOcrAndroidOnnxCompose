package com.benjaminwan.ocrlibrary.config

/**
 * OCR 引擎配置
 *
 * 顶层配置类，包含路径配置和加载策略
 *
 * @param pathConfig 路径配置，为 null 时使用默认路径
 * @param loadStrategy 加载策略，默认为 FILE_FIRST
 *
 * @sample
 * ```kotlin
 * val config = OcrConfig.builder()
 *     .pathConfig {
 *         detModelPath("/sdcard/models/det.onnx")
 *         recModelPath("/sdcard/models/rec.onnx")
 *     }
 *     .loadStrategy(OcrLoadStrategy.FILE_FIRST)
 *     .build()
 * ```
 */
data class OcrConfig(
    val pathConfig: OcrPathConfig? = null,
    val loadStrategy: OcrLoadStrategy = OcrLoadStrategy.FILE_FIRST
) {
    companion object {
        /**
         * 创建配置构建器
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    /**
     * 配置构建器
     *
     * 提供流式 API 构建配置对象
     */
    class Builder {
        private var pathConfig: OcrPathConfig? = null
        private var loadStrategy: OcrLoadStrategy = OcrLoadStrategy.FILE_FIRST

        /**
         * 设置路径配置
         *
         * @param pathConfig 路径配置对象
         */
        fun pathConfig(pathConfig: OcrPathConfig): Builder {
            this.pathConfig = pathConfig
            return this
        }

        /**
         * 通过 Lambda 配置路径
         *
         * 提供 DSL 风格的配置方式
         *
         * @param block 配置 Lambda
         *
         * @sample
         * ```kotlin
         * pathConfig {
         *     detModelPath("/sdcard/det.onnx")
         *     recModelPath("/sdcard/rec.onnx")
         * }
         * ```
         */
        fun pathConfig(block: OcrPathConfig.Builder.() -> Unit): Builder {
            this.pathConfig = OcrPathConfig.builder().apply(block).build()
            return this
        }

        /**
         * 设置加载策略
         *
         * @param strategy 加载策略
         */
        fun loadStrategy(strategy: OcrLoadStrategy): Builder {
            this.loadStrategy = strategy
            return this
        }

        /**
         * 构建配置对象
         */
        fun build(): OcrConfig {
            return OcrConfig(pathConfig, loadStrategy)
        }
    }
}
