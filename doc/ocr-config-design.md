# OcrLibrary 配置系统优化方案

## 一、需求概述

**目标**：优化 OcrLibrary，使其支持可配置的 OCR 引擎和模型文件路径。

**用户决策**：
- **引擎类型**：仅支持 ONNX Runtime（简化设计）
- **配置粒度**：全局配置 + 实例级配置（支持覆盖）
- **配置方式**：代码 API（Builder 模式）
- **兼容性**：允许破坏性变更

## 二、整体架构设计

### 2.1 核心类结构

```
OcrConfig（配置容器）
├── OcrPathConfig（路径配置）
│   ├── detModelPath: String?
│   ├── clsModelPath: String?
│   ├── recModelPath: String?
│   └── keysPath: String?
└── OcrLoadStrategy（加载策略）
    ├── FILE_FIRST（优先文件系统，降级到 assets）
    ├── ASSETS_FIRST（优先 assets，降级到文件系统）
    ├── FILE_ONLY（仅文件系统）
    └── ASSETS_ONLY（仅 assets）

OcrConfigManager（全局配置管理）
├── setGlobalConfig(config: OcrConfig)
└── getGlobalConfig(): OcrConfig?

ModelPathResolver（路径解析器）
├── resolve(context, config, version): ResolvedPaths
└── validate(paths): ValidationResult
```

### 2.2 配置优先级

优先级从高到低：
1. **实例级配置** - OcrEngine 构造时传入的 OcrConfig
2. **全局配置** - 通过 OcrConfigManager 设置的全局配置
3. **版本默认配置** - 基于 OcrModelVersion 的默认路径
4. **兜底降级** - ModelLoader 的降级逻辑（filesDir -> assets）

### 2.3 关键设计决策

- **路径支持**：绝对路径（以 / 开头）和相对路径（相对于 filesDir/models 或 assets）
- **向后兼容**：保留现有构造函数，添加新的重载
- **模型粒度**：支持 4 个模型文件的独立配置（det, cls, rec, keys）
- **错误处理**：默认延迟加载时报错，提供 strictMode 参数支持初始化时验证

## 三、API 设计

### 3.1 配置类（OcrConfig.kt）

```kotlin
data class OcrConfig(
    val pathConfig: OcrPathConfig? = null,
    val loadStrategy: OcrLoadStrategy = OcrLoadStrategy.FILE_FIRST
) {
    class Builder {
        fun pathConfig(pathConfig: OcrPathConfig): Builder
        fun pathConfig(block: OcrPathConfig.Builder.() -> Unit): Builder
        fun loadStrategy(strategy: OcrLoadStrategy): Builder
        fun build(): OcrConfig
    }
}

data class OcrPathConfig(
    val detModelPath: String? = null,
    val clsModelPath: String? = null,
    val recModelPath: String? = null,
    val keysPath: String? = null
) {
    class Builder {
        fun detModelPath(path: String): Builder
        fun clsModelPath(path: String): Builder
        fun recModelPath(path: String): Builder
        fun keysPath(path: String): Builder

        // 便捷方法：批量设置同一目录下的所有模型
        fun allPaths(baseDir: String, detName: String, clsName: String,
                     recName: String, keysName: String): Builder

        // 便捷方法：基于版本设置路径
        fun fromVersion(baseDir: String, version: OcrModelVersion): Builder

        fun build(): OcrPathConfig
    }
}

enum class OcrLoadStrategy {
    FILE_FIRST,    // 优先文件系统，降级到 assets（默认）
    ASSETS_FIRST,  // 优先 assets，降级到文件系统
    FILE_ONLY,     // 仅文件系统，不降级
    ASSETS_ONLY    // 仅 assets，不降级
}
```

### 3.2 全局配置管理器（OcrConfigManager.kt）

```kotlin
object OcrConfigManager {
    @Volatile
    private var globalConfig: OcrConfig? = null

    fun setGlobalConfig(config: OcrConfig)
    fun getGlobalConfig(): OcrConfig?
    fun clearGlobalConfig()
    fun hasGlobalConfig(): Boolean
}
```

### 3.3 OcrEngine 改造

```kotlin
// 现有构造函数（向后兼容）
OcrEngine(context: Context, modelVersion: OcrModelVersion = OcrModelVersion.V3)

// 新增构造函数（支持配置）
OcrEngine(
    context: Context,
    modelVersion: OcrModelVersion = OcrModelVersion.V3,
    config: OcrConfig? = null
)

// 配置优先级处理
private val effectiveConfig = config ?: OcrConfigManager.getGlobalConfig()

// 解析最终路径
private val resolvedPaths = ModelPathResolver.resolve(context, effectiveConfig, modelVersion)

// 组件初始化使用解析后的路径
private val det by lazy {
    Det(ortEnv, context, resolvedPaths.detPath, effectiveConfig?.loadStrategy ?: OcrLoadStrategy.FILE_FIRST)
}
```

## 四、路径解析与验证

### 4.1 路径解析器（ModelPathResolver.kt）

```kotlin
internal data class ResolvedPaths(
    val detPath: ResolvedPath,
    val clsPath: ResolvedPath,
    val recPath: ResolvedPath,
    val keysPath: ResolvedPath
)

internal data class ResolvedPath(
    val path: String,           // 路径字符串
    val isAbsolute: Boolean,    // 是否为绝对路径
    val source: PathSource      // 路径来源
)

internal enum class PathSource {
    CUSTOM_CONFIG,    // 用户自定义配置
    GLOBAL_CONFIG,    // 全局配置
    VERSION_DEFAULT   // 版本默认路径
}

internal object ModelPathResolver {
    // 解析路径（优先级：实例配置 > 全局配置 > 版本默认）
    fun resolve(context: Context, config: OcrConfig?, version: OcrModelVersion): ResolvedPaths

    // 验证路径是否可访问
    fun validate(context: Context, paths: ResolvedPaths, strategy: OcrLoadStrategy): ValidationResult
}

internal sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
}
```

### 4.2 ModelLoader 重构

```kotlin
internal object ModelLoader {
    // 新方法：支持 ResolvedPath 和加载策略
    fun loadModel(context: Context, resolvedPath: ResolvedPath, strategy: OcrLoadStrategy): ByteArray
    fun loadKeys(context: Context, resolvedPath: ResolvedPath, strategy: OcrLoadStrategy): BufferedReader

    // 旧方法：保留用于兼容性（标记为 Deprecated）
    @Deprecated("Use loadModel with ResolvedPath")
    fun loadModel(context: Context, modelName: String): ByteArray

    @Deprecated("Use loadKeys with ResolvedPath")
    fun loadKeys(context: Context, keysName: String): BufferedReader

    // 内部方法
    private fun tryLoadFromFile(context: Context, resolvedPath: ResolvedPath): ByteArray?
    private fun tryLoadFromAssets(context: Context, resolvedPath: ResolvedPath): ByteArray?
}

class ModelLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
```

### 4.3 组件类改造

```kotlin
// Det.kt
class Det(
    private val ortEnv: OrtEnvironment,
    private val context: Context,
    private val resolvedPath: ResolvedPath,
    private val loadStrategy: OcrLoadStrategy
) {
    private val session by lazy {
        val model = ModelLoader.loadModel(context, resolvedPath, loadStrategy)
        ortEnv.createSession(model)
    }
}

// Cls.kt 类似
// Rec.kt 需要两个路径（rec 模型 + keys 文件）
```

## 五、实现步骤

### 阶段 1：基础设施（新建文件）

**优先级**：P0 - 基础类定义

1. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/OcrLoadStrategy.kt`
   - 定义加载策略枚举

2. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/OcrPathConfig.kt`
   - 定义路径配置数据类和 Builder

3. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/OcrConfig.kt`
   - 定义顶层配置类和 Builder

4. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/OcrConfigManager.kt`
   - 实现全局配置管理器

5. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/ResolvedPath.kt`
   - 定义解析后的路径数据结构

6. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/ModelPathResolver.kt`
   - 实现路径解析和验证逻辑

7. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/ModelLoadException.kt`
   - 定义自定义异常类

### 阶段 2：核心逻辑改造（修改现有文件）

**优先级**：P0 - 核心功能

8. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/ModelLoader.kt`
   - 重构以支持 ResolvedPath 和 OcrLoadStrategy
   - 实现 4 种加载策略
   - 保留旧接口并标记为 Deprecated

9. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Det.kt`
   - 修改构造函数接受 ResolvedPath 和 OcrLoadStrategy
   - 使用新的 ModelLoader API

10. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Cls.kt`
    - 同 Det.kt

11. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Rec.kt`
    - 修改构造函数接受两个 ResolvedPath（rec + keys）
    - 使用新的 ModelLoader API

12. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/OcrEngine.kt`
    - 添加新的构造函数重载（接受 OcrConfig 参数）
    - 集成 ModelPathResolver 解析路径
    - 更新 Det/Cls/Rec 的初始化逻辑

### 阶段 3：文档和示例（可选）

**优先级**：P1 - 文档说明

13. 创建 `doc/ocr-config-design.md` - 详细设计文档
14. 更新 `README.md` - 添加配置使用说明
15. 更新示例代码 - 在 app 模块中添加配置示例

## 六、使用示例

### 6.1 全局配置

```kotlin
// 在 Application.onCreate() 中
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 设置全局配置
        val config = OcrConfig.builder()
            .pathConfig {
                fromVersion("/sdcard/custom_models", OcrModelVersion.V4)
            }
            .loadStrategy(OcrLoadStrategy.FILE_FIRST)
            .build()

        OcrConfigManager.setGlobalConfig(config)
    }
}
```

### 6.2 实例级配置

```kotlin
// 在 ViewModel 中
class GalleryViewModel {
    fun initOcrEngine() {
        // 方式 1：使用全局配置
        val engine1 = OcrEngine(context, OcrModelVersion.V3)

        // 方式 2：实例配置（覆盖全局）
        val config = OcrConfig.builder()
            .pathConfig {
                allPaths(
                    baseDir = "/sdcard/Download/ocr_models",
                    detName = "custom_det.onnx",
                    clsName = "custom_cls.onnx",
                    recName = "custom_rec.onnx",
                    keysName = "custom_keys.txt"
                )
            }
            .loadStrategy(OcrLoadStrategy.FILE_ONLY)
            .build()

        val engine2 = OcrEngine(context, OcrModelVersion.V4, config)

        // 方式 3：混合配置（部分自定义，部分默认）
        val mixedConfig = OcrConfig.builder()
            .pathConfig {
                detModelPath("/sdcard/my_det_model.onnx")
                // cls, rec, keys 使用默认路径
            }
            .build()

        val engine3 = OcrEngine(context, OcrModelVersion.V5, mixedConfig)
    }
}
```

### 6.3 多版本共存

```kotlin
class MultiVersionManager(private val context: Context) {
    private val v3Engine: OcrEngine
    private val v4Engine: OcrEngine
    private val v5Engine: OcrEngine

    init {
        // V3 使用默认路径（assets）
        v3Engine = OcrEngine(context, OcrModelVersion.V3)

        // V4 使用下载目录
        val v4Config = OcrConfig.builder()
            .pathConfig {
                fromVersion(context.filesDir.absolutePath + "/models", OcrModelVersion.V4)
            }
            .build()
        v4Engine = OcrEngine(context, OcrModelVersion.V4, v4Config)

        // V5 使用外部 SD 卡
        val v5Config = OcrConfig.builder()
            .pathConfig {
                fromVersion("/sdcard/Download/ocr_v5", OcrModelVersion.V5)
            }
            .loadStrategy(OcrLoadStrategy.FILE_ONLY)
            .build()
        v5Engine = OcrEngine(context, OcrModelVersion.V5, v5Config)
    }
}
```

## 七、错误处理

### 7.1 路径验证

**用户决策**：支持两种验证策略
- **默认**：延迟验证（在实际加载模型时才报错，OcrEngine 创建很快）
- **严格模式**：提供 `strictMode` 参数，在 OcrEngine 构造时就验证所有路径

```kotlin
// OcrEngine 构造函数支持严格模式
class OcrEngine(
    context: Context,
    modelVersion: OcrModelVersion = OcrModelVersion.V3,
    config: OcrConfig? = null,
    strictMode: Boolean = false  // false=延迟验证，true=立即验证
) {
    init {
        if (strictMode) {
            // 严格模式：立即验证所有路径
            val validationResult = ModelPathResolver.validate(
                context, resolvedPaths, effectiveConfig?.loadStrategy ?: OcrLoadStrategy.FILE_FIRST
            )
            if (validationResult is ValidationResult.Error) {
                throw ModelLoadException("Model validation failed: ${validationResult.errors}")
            }
        }
        // 默认模式：延迟到实际加载时才验证
    }
}
```

**使用场景**：
- 普通场景：使用默认延迟验证，快速创建引擎
- 调试/测试场景：使用 `strictMode = true`，提前发现配置错误

### 7.2 边界情况

- **路径为空字符串**：视为 null，使用默认值
- **路径不存在**：根据 loadStrategy 降级或抛出 ModelLoadException
- **模型文件损坏**：由 ONNX Runtime 抛出异常
- **并发访问**：OcrConfigManager 使用 @Volatile 保证可见性

## 八、性能影响

- **延迟初始化**：保持现有 `lazy` 模式，配置不影响性能
- **路径解析**：仅在 OcrEngine 构造时执行一次，O(1) 复杂度
- **内存开销**：每个 OcrEngine 实例增加约 200-300 字节
- **线程安全**：轻量级同步，无锁竞争

## 九、测试策略

### 9.1 单元测试

```kotlin
// OcrConfigTest: 测试 Builder 模式
// ModelPathResolverTest: 测试路径解析和优先级
// ModelLoaderTest: 测试加载策略
```

### 9.2 集成测试

```kotlin
// OcrEngineConfigTest: 测试不同配置下的引擎创建
// 验证默认配置、全局配置、实例配置的正确性
```

## 十、迁移指南

### 10.1 向后兼容

所有现有代码无需修改，继续正常工作：

```kotlin
// 这些代码继续工作
val engine = OcrEngine(context, OcrModelVersion.V3)
val engine2 = OcrEngine(context, OcrModelVersion.V4)
```

### 10.2 迁移到新 API

需要自定义路径时，使用新 API：

```kotlin
// 旧方式（继续工作）
val engine = OcrEngine(context, OcrModelVersion.V4)

// 新方式（推荐）
val config = OcrConfig.builder()
    .pathConfig { fromVersion("/custom/path", OcrModelVersion.V4) }
    .build()
val engine = OcrEngine(context, OcrModelVersion.V4, config)
```

## 十一、关键文件列表

### 需要创建的文件（7 个）

1. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/OcrLoadStrategy.kt`
2. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/OcrPathConfig.kt`
3. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/OcrConfig.kt`
4. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/OcrConfigManager.kt`
5. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/ResolvedPath.kt`
6. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/config/ModelPathResolver.kt`
7. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/ModelLoadException.kt`

### 需要修改的文件（5 个）

1. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/ModelLoader.kt`
2. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/OcrEngine.kt`
3. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Det.kt`
4. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Cls.kt`
5. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Rec.kt`

## 十二、预计工作量

- **阶段 1（基础设施）**：1-2 天
- **阶段 2（核心逻辑）**：2-3 天
- **阶段 3（文档示例）**：1 天
- **测试和调试**：1-2 天

**总计**：5-8 天

## 十三、方案优势

1. **灵活性高**：支持全局和实例级配置，支持绝对/相对路径
2. **易用性好**：Builder 模式 API 清晰，IDE 提示友好
3. **向后兼容**：现有代码无需修改
4. **扩展性强**：未来可轻松添加新的加载策略或配置项
5. **性能影响小**：配置仅在初始化时处理，运行时无额外开销
6. **错误处理完善**：提供验证机制，支持严格模式
7. **维护成本低**：清晰的类结构，良好的代码组织
