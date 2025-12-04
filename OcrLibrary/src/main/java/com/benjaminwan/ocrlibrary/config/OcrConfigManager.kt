package com.benjaminwan.ocrlibrary.config

/**
 * OCR 全局配置管理器
 *
 * 提供全局默认配置，可被实例级配置覆盖
 *
 * 线程安全：使用 @Volatile 保证多线程环境下的可见性
 *
 * @sample
 * ```kotlin
 * // 在 Application.onCreate() 中设置全局配置
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *
 *         val config = OcrConfig.builder()
 *             .pathConfig {
 *                 fromVersion("/sdcard/models", OcrModelVersion.V4)
 *             }
 *             .build()
 *
 *         OcrConfigManager.setGlobalConfig(config)
 *     }
 * }
 *
 * // 在其他地方创建 OcrEngine 时，会自动使用全局配置
 * val engine = OcrEngine(context, OcrModelVersion.V4)
 * ```
 */
object OcrConfigManager {
    @Volatile
    private var globalConfig: OcrConfig? = null

    /**
     * 设置全局配置
     *
     * 此配置将作为所有 OcrEngine 实例的默认配置
     * 实例级配置可以覆盖全局配置
     *
     * @param config 全局配置对象
     */
    @JvmStatic
    fun setGlobalConfig(config: OcrConfig) {
        globalConfig = config
    }

    /**
     * 获取全局配置
     *
     * @return 全局配置对象，未设置时返回 null
     */
    @JvmStatic
    fun getGlobalConfig(): OcrConfig? = globalConfig

    /**
     * 清除全局配置
     *
     * 清除后，未指定实例级配置的 OcrEngine 将使用默认行为
     */
    @JvmStatic
    fun clearGlobalConfig() {
        globalConfig = null
    }

    /**
     * 检查是否已设置全局配置
     *
     * @return true 表示已设置全局配置，false 表示未设置
     */
    @JvmStatic
    fun hasGlobalConfig(): Boolean = globalConfig != null
}
