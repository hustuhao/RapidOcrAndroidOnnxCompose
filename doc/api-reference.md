# OcrLibrary API 参考文档

## 目录

- [快速开始](#快速开始)
- [核心 API](#核心-api)
  - [OcrEngine](#ocrengine)
  - [OcrConfig](#ocrconfig)
  - [OcrConfigManager](#ocrconfigmanager)
  - [OcrPathConfig](#ocrpathconfig)
  - [OcrLoadStrategy](#ocrloadstrategy)
  - [OcrModelVersion](#ocrmodelversion)
- [使用示例](#使用示例)
- [迁移指南](#迁移指南)

---

## 快速开始

### 1. 添加依赖

#### 方式 1：本地 AAR

```gradle
// settings.gradle 或 build.gradle (项目级)
repositories {
    flatDir {
        dirs 'libs'
    }
}

// build.gradle (模块级)
dependencies {
    implementation(name: 'OcrLibrary-0.1.0-release', ext: 'aar')

    // 必需的依赖
    implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.19.2'
    implementation 'com.orhanobut:logger:2.2.0'
}
```

#### 方式 2：模块依赖

```gradle
dependencies {
    implementation project(':OcrLibrary')
}
```

### 2. 基础使用

```kotlin
// 创建 OCR 引擎
val ocrEngine = OcrEngine(context, OcrModelVersion.V3)

// 执行识别
val result = ocrEngine.detect(
    bmp = bitmap,
    scaleUp = false,
    maxSideLen = 960,
    padding = 50,
    boxScoreThresh = 0.5f,
    boxThresh = 0.3f,
    unClipRatio = 1.6f,
    doCls = true,
    mostCls = false
)

// 获取识别结果
val text = result.text
val boxImage = result.boxImage

// 记得关闭引擎
ocrEngine.close()
```

---

## 核心 API

### OcrEngine

OCR 引擎主类，提供文本检测、方向分类和文本识别功能。

#### 构造方法

```kotlin
// 方式 1：使用默认配置
OcrEngine(
    context: Context,
    modelVersion: OcrModelVersion = OcrModelVersion.V3
): OcrEngine

// 方式 2：使用自定义配置
OcrEngine(
    context: Context,
    modelVersion: OcrModelVersion = OcrModelVersion.V3,
    config: OcrConfig? = null,
    strictMode: Boolean = false
): OcrEngine
```

**参数说明**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `context` | `Context` | 必需 | Android Context |
| `modelVersion` | `OcrModelVersion` | `V3` | 模型版本（V3/V4/V5） |
| `config` | `OcrConfig?` | `null` | 配置对象，为 null 时使用全局配置 |
| `strictMode` | `Boolean` | `false` | 严格模式，true 时在构造时验证所有路径 |

**示例**：

```kotlin
// 默认配置
val engine1 = OcrEngine(context)

// 指定版本
val engine2 = OcrEngine(context, OcrModelVersion.V4)

// 自定义配置
val config = OcrConfig.builder()
    .pathConfig { fromVersion("/sdcard/models", OcrModelVersion.V4) }
    .build()
val engine3 = OcrEngine(context, OcrModelVersion.V4, config)

// 严格模式（初始化时验证路径）
val engine4 = OcrEngine(context, OcrModelVersion.V3, null, strictMode = true)
```

#### detect() 方法

执行完整的 OCR 识别（检测 + 分类 + 识别）。

```kotlin
fun detect(
    bmp: Bitmap,
    scaleUp: Boolean,
    maxSideLen: Int,
    padding: Int,
    boxScoreThresh: Float,
    boxThresh: Float,
    unClipRatio: Float,
    doCls: Boolean,
    mostCls: Boolean
): OcrResult
```

**参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `bmp` | `Bitmap` | 输入的图片 |
| `scaleUp` | `Boolean` | 放大使能；false 时只缩小不放大，true 时可放大可缩小 |
| `maxSideLen` | `Int` | 长边缩放目标长度（像素），0 代表不缩放 |
| `padding` | `Int` | 增加白边（像素），提升边缘文字识别率 |
| `boxScoreThresh` | `Float` | 文字框置信度门限，减小此值可检出更多文字框 |
| `boxThresh` | `Float` | 用于过滤检测过程中的噪点 |
| `unClipRatio` | `Float` | 文字框大小倍率，越大时单个文字框越大 |
| `doCls` | `Boolean` | 是否进行文字方向分类（仅当图片倒置时需要） |
| `mostCls` | `Boolean` | 文字方向投票，true 时以最大概率作为全文方向 |

**返回值**：

```kotlin
data class OcrResult(
    val detResults: List<DetResult>,    // 检测结果
    val detTime: Double,                 // 检测耗时（毫秒）
    val clsResults: List<ClsResult>,    // 分类结果
    val clsTime: Double,                 // 分类耗时（毫秒）
    val recResults: List<RecResult>,    // 识别结果
    val recTime: Double,                 // 识别耗时（毫秒）
    val boxImage: Bitmap,                // 标注了文字框的图片
    val fullTime: Double,                // 总耗时（毫秒）
    val text: String                     // 识别的完整文本（用换行符分隔）
)
```

**推荐参数**：

```kotlin
// 通用场景
val result = ocrEngine.detect(
    bmp = bitmap,
    scaleUp = false,
    maxSideLen = 960,
    padding = 50,
    boxScoreThresh = 0.5f,
    boxThresh = 0.3f,
    unClipRatio = 1.6f,
    doCls = true,
    mostCls = false
)

// 高精度场景（检测更多文字）
val result = ocrEngine.detect(
    bmp = bitmap,
    scaleUp = false,
    maxSideLen = 1920,    // 更大的分辨率
    padding = 80,
    boxScoreThresh = 0.3f,  // 更低的阈值
    boxThresh = 0.2f,
    unClipRatio = 2.0f,
    doCls = true,
    mostCls = false
)

// 快速识别场景
val result = ocrEngine.detect(
    bmp = bitmap,
    scaleUp = false,
    maxSideLen = 640,      // 更小的分辨率
    padding = 30,
    boxScoreThresh = 0.6f,
    boxThresh = 0.4f,
    unClipRatio = 1.5f,
    doCls = false,          // 跳过分类
    mostCls = false
)
```

#### close() 方法

关闭引擎，释放资源。

```kotlin
fun close()
```

**重要**：使用完毕后必须调用 `close()` 释放 ONNX Runtime 资源。

---

### OcrConfig

顶层配置类，包含路径配置和加载策略。

#### 构造方法

```kotlin
data class OcrConfig(
    val pathConfig: OcrPathConfig? = null,
    val loadStrategy: OcrLoadStrategy = OcrLoadStrategy.FILE_FIRST
)
```

#### Builder API

```kotlin
OcrConfig.builder()
    .pathConfig(pathConfig: OcrPathConfig): Builder
    .pathConfig(block: OcrPathConfig.Builder.() -> Unit): Builder
    .loadStrategy(strategy: OcrLoadStrategy): Builder
    .build(): OcrConfig
```

**示例**：

```kotlin
// 方式 1：传入 OcrPathConfig 对象
val pathConfig = OcrPathConfig.builder()
    .detModelPath("/sdcard/models/det.onnx")
    .build()

val config = OcrConfig.builder()
    .pathConfig(pathConfig)
    .loadStrategy(OcrLoadStrategy.FILE_FIRST)
    .build()

// 方式 2：使用 DSL 风格（推荐）
val config = OcrConfig.builder()
    .pathConfig {
        detModelPath("/sdcard/models/det.onnx")
        recModelPath("/sdcard/models/rec.onnx")
    }
    .loadStrategy(OcrLoadStrategy.FILE_FIRST)
    .build()

// 方式 3：使用便捷方法
val config = OcrConfig.builder()
    .pathConfig {
        fromVersion("/sdcard/ocr_models", OcrModelVersion.V4)
    }
    .build()
```

---

### OcrConfigManager

全局配置管理器（单例），提供全局默认配置。

#### API 方法

```kotlin
object OcrConfigManager {
    // 设置全局配置
    fun setGlobalConfig(config: OcrConfig)

    // 获取全局配置
    fun getGlobalConfig(): OcrConfig?

    // 清除全局配置
    fun clearGlobalConfig()

    // 检查是否已设置全局配置
    fun hasGlobalConfig(): Boolean
}
```

**使用场景**：

在 `Application.onCreate()` 中设置全局配置，所有未指定配置的 `OcrEngine` 实例将自动使用全局配置。

**示例**：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 设置全局配置
        val config = OcrConfig.builder()
            .pathConfig {
                fromVersion(filesDir.absolutePath + "/models", OcrModelVersion.V4)
            }
            .loadStrategy(OcrLoadStrategy.FILE_FIRST)
            .build()

        OcrConfigManager.setGlobalConfig(config)
    }
}

// 在其他地方使用时，会自动应用全局配置
class MainActivity : AppCompatActivity() {
    private lateinit var ocrEngine: OcrEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 自动使用全局配置
        ocrEngine = OcrEngine(this, OcrModelVersion.V4)
    }
}
```

---

### OcrPathConfig

路径配置类，支持配置检测、分类、识别模型和字典文件的路径。

#### 构造方法

```kotlin
data class OcrPathConfig(
    val detModelPath: String? = null,
    val clsModelPath: String? = null,
    val recModelPath: String? = null,
    val keysPath: String? = null
)
```

#### Builder API

```kotlin
OcrPathConfig.builder()
    .detModelPath(path: String): Builder
    .clsModelPath(path: String): Builder
    .recModelPath(path: String): Builder
    .keysPath(path: String): Builder
    .allPaths(baseDir: String, detName: String, clsName: String,
              recName: String, keysName: String): Builder
    .fromVersion(baseDir: String, version: OcrModelVersion): Builder
    .build(): OcrPathConfig
```

**路径格式**：

- **绝对路径**：以 `/` 开头，如 `/sdcard/models/det.onnx`
- **相对路径**：不以 `/` 开头，相对于默认目录
  - 文件系统相对路径：`context.filesDir/models/`
  - Assets 相对路径：`assets/`

**示例**：

```kotlin
// 方式 1：单独设置每个路径（绝对路径）
val config = OcrPathConfig.builder()
    .detModelPath("/sdcard/models/det.onnx")
    .clsModelPath("/sdcard/models/cls.onnx")
    .recModelPath("/sdcard/models/rec.onnx")
    .keysPath("/sdcard/models/keys.txt")
    .build()

// 方式 2：单独设置每个路径（相对路径）
val config = OcrPathConfig.builder()
    .detModelPath("custom/det.onnx")  // -> filesDir/models/custom/det.onnx
    .recModelPath("custom/rec.onnx")
    .build()

// 方式 3：批量设置（同一目录）
val config = OcrPathConfig.builder()
    .allPaths(
        baseDir = "/sdcard/ocr_models",
        detName = "det_v4.onnx",
        clsName = "cls_v2.onnx",
        recName = "rec_v4.onnx",
        keysName = "keys_v1.txt"
    )
    .build()

// 方式 4：基于版本设置（推荐）
val config = OcrPathConfig.builder()
    .fromVersion("/sdcard/ocr_v4", OcrModelVersion.V4)
    .build()
// 将使用 V4 版本的默认文件名：
// - /sdcard/ocr_v4/ch_PP-OCRv4_det_infer.onnx
// - /sdcard/ocr_v4/ch_PP-OCRv4_rec_infer.onnx
// - 等等

// 方式 5：混合配置（部分自定义，部分默认）
val config = OcrPathConfig.builder()
    .detModelPath("/custom/det.onnx")  // 自定义检测模型
    // cls, rec, keys 使用默认路径
    .build()
```

---

### OcrLoadStrategy

模型加载策略枚举。

```kotlin
enum class OcrLoadStrategy {
    FILE_FIRST,    // 优先文件系统，降级到 assets（默认）
    ASSETS_FIRST,  // 优先 assets，降级到文件系统
    FILE_ONLY,     // 仅文件系统，不降级
    ASSETS_ONLY    // 仅 assets，不降级
}
```

#### 策略说明

| 策略 | 加载顺序 | 使用场景 |
|------|----------|----------|
| `FILE_FIRST` | 1. filesDir/models/<br>2. assets | **默认策略**，优先使用下载的模型，不存在时使用内置模型 |
| `ASSETS_FIRST` | 1. assets<br>2. filesDir/models | 优先使用内置模型，适合需要确保模型一致性的场景 |
| `FILE_ONLY` | 仅 filesDir/models | 必须使用外部模型，适合动态更新模型的场景 |
| `ASSETS_ONLY` | 仅 assets | 仅使用内置模型，适合离线环境或确保模型版本的场景 |

**示例**：

```kotlin
// 场景 1：优先使用下载的模型（默认）
val config = OcrConfig.builder()
    .loadStrategy(OcrLoadStrategy.FILE_FIRST)
    .build()

// 场景 2：仅使用外部模型
val config = OcrConfig.builder()
    .pathConfig {
        fromVersion("/sdcard/models", OcrModelVersion.V4)
    }
    .loadStrategy(OcrLoadStrategy.FILE_ONLY)
    .build()

// 场景 3：仅使用内置模型
val config = OcrConfig.builder()
    .loadStrategy(OcrLoadStrategy.ASSETS_ONLY)
    .build()
```

---

### OcrModelVersion

模型版本枚举，定义支持的 PP-OCR 模型版本。

```kotlin
enum class OcrModelVersion(
    val versionName: String,
    val detModelName: String,
    val recModelName: String,
    val clsModelName: String,
    val keysName: String,
    val detUrl: String,
    val recUrl: String,
    val keysUrl: String?
)
```

#### 支持的版本

| 版本 | 说明 | 模型来源 |
|------|------|----------|
| `V3` | PP-OCRv3 | 内置在 assets 中 |
| `V4` | PP-OCRv4 | 需要下载（ModelScope） |
| `V5` | PP-OCRv5 | 需要下载（ModelScope） |

**示例**：

```kotlin
// 使用 V3（内置）
val engine = OcrEngine(context, OcrModelVersion.V3)

// 使用 V4（需要先下载模型）
val engine = OcrEngine(context, OcrModelVersion.V4)

// 使用 V5（需要先下载模型）
val engine = OcrEngine(context, OcrModelVersion.V5)

// 从字符串转换
val version = OcrModelVersion.fromName("V4")  // 返回 OcrModelVersion.V4
```

---

## 使用示例

### 示例 1：基础使用（默认配置）

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var ocrEngine: OcrEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建引擎（使用 V3 内置模型）
        ocrEngine = OcrEngine(this, OcrModelVersion.V3)
    }

    fun recognizeImage(bitmap: Bitmap) {
        val result = ocrEngine.detect(
            bmp = bitmap,
            scaleUp = false,
            maxSideLen = 960,
            padding = 50,
            boxScoreThresh = 0.5f,
            boxThresh = 0.3f,
            unClipRatio = 1.6f,
            doCls = true,
            mostCls = false
        )

        // 显示结果
        textView.text = result.text
        imageView.setImageBitmap(result.boxImage)

        Log.i("OCR", "识别耗时: ${result.fullTime}ms")
        Log.i("OCR", "检测到 ${result.detResults.size} 个文字框")
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrEngine.close()
    }
}
```

### 示例 2：使用自定义模型路径

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var ocrEngine: OcrEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 配置自定义模型路径
        val config = OcrConfig.builder()
            .pathConfig {
                fromVersion("/sdcard/Download/ocr_models", OcrModelVersion.V4)
            }
            .loadStrategy(OcrLoadStrategy.FILE_FIRST)
            .build()

        // 创建引擎
        ocrEngine = OcrEngine(this, OcrModelVersion.V4, config)
    }

    // ... 其他代码
}
```

### 示例 3：全局配置

```kotlin
// Application 类
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 设置全局配置
        val config = OcrConfig.builder()
            .pathConfig {
                fromVersion(filesDir.absolutePath + "/models", OcrModelVersion.V4)
            }
            .loadStrategy(OcrLoadStrategy.FILE_FIRST)
            .build()

        OcrConfigManager.setGlobalConfig(config)
    }
}

// Activity 中使用（自动应用全局配置）
class MainActivity : AppCompatActivity() {
    private lateinit var ocrEngine: OcrEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 不指定 config，自动使用全局配置
        ocrEngine = OcrEngine(this, OcrModelVersion.V4)
    }
}
```

### 示例 4：多版本共存

```kotlin
class MultiVersionOcrManager(private val context: Context) {
    private val v3Engine: OcrEngine
    private val v4Engine: OcrEngine
    private val v5Engine: OcrEngine

    init {
        // V3 使用默认内置模型
        v3Engine = OcrEngine(context, OcrModelVersion.V3)

        // V4 使用下载目录
        val v4Config = OcrConfig.builder()
            .pathConfig {
                fromVersion(context.filesDir.absolutePath + "/models/v4", OcrModelVersion.V4)
            }
            .build()
        v4Engine = OcrEngine(context, OcrModelVersion.V4, v4Config)

        // V5 使用 SD 卡
        val v5Config = OcrConfig.builder()
            .pathConfig {
                fromVersion("/sdcard/ocr_v5", OcrModelVersion.V5)
            }
            .loadStrategy(OcrLoadStrategy.FILE_ONLY)
            .build()
        v5Engine = OcrEngine(context, OcrModelVersion.V5, v5Config)
    }

    fun detectWithVersion(bitmap: Bitmap, version: OcrModelVersion): OcrResult {
        val engine = when (version) {
            OcrModelVersion.V3 -> v3Engine
            OcrModelVersion.V4 -> v4Engine
            OcrModelVersion.V5 -> v5Engine
        }

        return engine.detect(
            bmp = bitmap,
            scaleUp = false,
            maxSideLen = 960,
            padding = 50,
            boxScoreThresh = 0.5f,
            boxThresh = 0.3f,
            unClipRatio = 1.6f,
            doCls = true,
            mostCls = false
        )
    }

    fun close() {
        v3Engine.close()
        v4Engine.close()
        v5Engine.close()
    }
}
```

### 示例 5：动态切换模型

```kotlin
class DynamicOcrManager(private val context: Context) {
    private var currentEngine: OcrEngine? = null
    private var currentVersion: OcrModelVersion? = null

    fun switchToVersion(version: OcrModelVersion, modelDir: String) {
        // 关闭旧引擎
        currentEngine?.close()

        // 创建新配置
        val config = OcrConfig.builder()
            .pathConfig {
                fromVersion(modelDir, version)
            }
            .loadStrategy(OcrLoadStrategy.FILE_FIRST)
            .build()

        // 创建新引擎
        currentEngine = OcrEngine(context, version, config)
        currentVersion = version

        Log.i("OCR", "Switched to version ${version.versionName}")
    }

    fun detect(bitmap: Bitmap): OcrResult? {
        val engine = currentEngine ?: return null

        return engine.detect(
            bmp = bitmap,
            scaleUp = false,
            maxSideLen = 960,
            padding = 50,
            boxScoreThresh = 0.5f,
            boxThresh = 0.3f,
            unClipRatio = 1.6f,
            doCls = true,
            mostCls = false
        )
    }

    fun close() {
        currentEngine?.close()
        currentEngine = null
    }
}

// 使用
val manager = DynamicOcrManager(context)
manager.switchToVersion(OcrModelVersion.V4, "/sdcard/models/v4")
val result = manager.detect(bitmap)
```

### 示例 6：严格模式验证

```kotlin
class SafeOcrEngine(private val context: Context) {

    fun createEngineWithValidation(
        version: OcrModelVersion,
        modelDir: String
    ): Result<OcrEngine> {
        return try {
            val config = OcrConfig.builder()
                .pathConfig {
                    fromVersion(modelDir, version)
                }
                .loadStrategy(OcrLoadStrategy.FILE_ONLY)
                .build()

            // 使用严格模式，在构造时验证所有路径
            val engine = OcrEngine(
                context = context,
                modelVersion = version,
                config = config,
                strictMode = true  // 启用严格模式
            )

            Result.success(engine)
        } catch (e: ModelLoadException) {
            Log.e("OCR", "Model validation failed", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("OCR", "Engine creation failed", e)
            Result.failure(e)
        }
    }
}

// 使用
val safeEngine = SafeOcrEngine(context)
val result = safeEngine.createEngineWithValidation(
    OcrModelVersion.V4,
    "/sdcard/models"
)

result.onSuccess { engine ->
    // 引擎创建成功，所有路径已验证
    val ocrResult = engine.detect(/* ... */)
}.onFailure { error ->
    // 处理错误
    Toast.makeText(context, "模型加载失败: ${error.message}", Toast.LENGTH_SHORT).show()
}
```

### 示例 7：协程集成

```kotlin
class OcrViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var ocrEngine: OcrEngine

    init {
        ocrEngine = OcrEngine(application, OcrModelVersion.V3)
    }

    fun recognizeImage(bitmap: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val result = ocrEngine.detect(
                bmp = bitmap,
                scaleUp = false,
                maxSideLen = 960,
                padding = 50,
                boxScoreThresh = 0.5f,
                boxThresh = 0.3f,
                unClipRatio = 1.6f,
                doCls = true,
                mostCls = false
            )

            withContext(Dispatchers.Main) {
                // 更新 UI
                _ocrResult.value = result
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _error.value = e.message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.close()
    }

    private val _ocrResult = MutableLiveData<OcrResult>()
    val ocrResult: LiveData<OcrResult> = _ocrResult

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
}
```

---

## 迁移指南

### 从旧版本迁移

#### 旧版本代码（0.0.x）

```kotlin
// 旧版本：只能使用默认路径
val ocrEngine = OcrEngine(context, OcrModelVersion.V3)
```

#### 新版本代码（0.1.0+）

```kotlin
// 方式 1：保持不变（向后兼容）
val ocrEngine = OcrEngine(context, OcrModelVersion.V3)

// 方式 2：使用新的配置系统
val config = OcrConfig.builder()
    .pathConfig {
        fromVersion("/custom/path", OcrModelVersion.V4)
    }
    .build()
val ocrEngine = OcrEngine(context, OcrModelVersion.V4, config)
```

**重要提示**：

- ✅ 旧代码无需修改，完全向后兼容
- ✅ `ModelLoader.loadModel(Context, String)` 标记为 `@Deprecated`，但仍可使用
- ✅ 新版本仅添加新功能，不破坏现有 API

### 新功能采用建议

1. **如果使用默认路径**：无需修改代码
2. **如果需要自定义路径**：
   - 在 Application 中设置全局配置
   - 或在创建 OcrEngine 时传入配置对象
3. **如果需要严格验证**：使用 `strictMode = true`

---

## 常见问题

### Q1: 如何下载模型文件？

**A**: V4 和 V5 版本需要从 ModelScope 下载：

```kotlin
// 使用 ModelDownloader 类（假设已实现）
val downloader = ModelDownloader(context)
downloader.downloadVersion(OcrModelVersion.V4) { progress ->
    // 更新进度
}
```

### Q2: 模型文件应该放在哪里？

**A**: 推荐位置：

1. **内置模型**：放在 `assets/` 目录（V3）
2. **下载模型**：放在 `context.filesDir/models/` 目录
3. **外部模型**：可放在任意路径，使用绝对路径配置

### Q3: 如何知道使用了哪个路径的模型？

**A**: 查看日志输出：

```
I/OcrLibrary: Resolved det path: ch_PP-OCRv3_det_infer.onnx (absolute=false, source=VERSION_DEFAULT)
I/OcrLibrary: Loading model from assets: ch_PP-OCRv3_det_infer.onnx
```

### Q4: strictMode 什么时候使用？

**A**:

- **开发/调试阶段**：使用 `strictMode = true`，提前发现配置错误
- **生产环境**：使用默认 `strictMode = false`，延迟加载以加快启动速度

### Q5: 如何优化识别性能？

**A**:

1. **减小 maxSideLen**：降低分辨率，加快速度
2. **关闭 doCls**：跳过方向分类，适用于正向文字
3. **调整阈值**：提高 `boxScoreThresh` 和 `boxThresh`，减少检测框数量
4. **使用更快的模型**：V3 比 V4/V5 更快

### Q6: 多线程安全吗？

**A**:

- ❌ 单个 `OcrEngine` 实例**不是线程安全的**
- ✅ 可以为每个线程创建独立的 `OcrEngine` 实例
- ✅ 或使用同步机制保护访问

---

## 更多资源

- **设计文档**：`doc/ocr-config-design.md`
- **示例项目**：`app/` 模块
- **源码**：`OcrLibrary/` 模块

---

**版本**：0.1.0
**最后更新**：2025-12-02
