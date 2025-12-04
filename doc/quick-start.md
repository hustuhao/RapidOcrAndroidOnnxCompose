# OcrLibrary 快速开始指南

## 5 分钟快速上手

### 1. 添加依赖

```gradle
dependencies {
    implementation(name: 'OcrLibrary-0.1.0-release', ext: 'aar')

    // 必需依赖
    implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.19.2'
    implementation 'com.orhanobut:logger:2.2.0'
}
```

### 2. 创建引擎并识别

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var ocrEngine: OcrEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建引擎（使用 V3 内置模型）
        ocrEngine = OcrEngine(this, OcrModelVersion.V3)
    }

    fun recognizeImage(bitmap: Bitmap) {
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

        // 获取结果
        val text = result.text
        val boxImage = result.boxImage

        // 显示结果
        textView.text = text
        imageView.setImageBitmap(boxImage)
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrEngine.close()  // 记得释放资源
    }
}
```

## 进阶使用

### 场景 1：使用自定义模型路径

```kotlin
val config = OcrConfig.builder()
    .pathConfig {
        fromVersion("/sdcard/models", OcrModelVersion.V4)
    }
    .build()

val ocrEngine = OcrEngine(this, OcrModelVersion.V4, config)
```

### 场景 2：设置全局配置

```kotlin
// 在 Application 中
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = OcrConfig.builder()
            .pathConfig {
                fromVersion(filesDir.absolutePath + "/models", OcrModelVersion.V4)
            }
            .build()

        OcrConfigManager.setGlobalConfig(config)
    }
}

// 在 Activity 中（自动使用全局配置）
val ocrEngine = OcrEngine(this, OcrModelVersion.V4)
```

### 场景 3：混合配置（部分自定义）

```kotlin
val config = OcrConfig.builder()
    .pathConfig {
        detModelPath("/custom/det.onnx")  // 自定义检测模型
        // cls, rec, keys 使用默认路径
    }
    .loadStrategy(OcrLoadStrategy.FILE_FIRST)
    .build()

val ocrEngine = OcrEngine(this, OcrModelVersion.V4, config)
```

### 场景 4：严格模式（初始化时验证路径）

```kotlin
try {
    val ocrEngine = OcrEngine(
        context = this,
        modelVersion = OcrModelVersion.V4,
        config = config,
        strictMode = true  // 启用严格模式
    )
    // 路径验证通过，可以安全使用
} catch (e: ModelLoadException) {
    // 路径验证失败，处理错误
    Log.e("OCR", "模型加载失败: ${e.message}")
}
```

## 参数调优

### 通用场景（推荐）

```kotlin
val result = ocrEngine.detect(
    bmp = bitmap,
    scaleUp = false,
    maxSideLen = 960,      // 适中的分辨率
    padding = 50,          // 边缘留白
    boxScoreThresh = 0.5f, // 适中的置信度
    boxThresh = 0.3f,
    unClipRatio = 1.6f,
    doCls = true,          // 启用方向分类
    mostCls = false
)
```

### 高精度场景（检测更多文字）

```kotlin
val result = ocrEngine.detect(
    bmp = bitmap,
    scaleUp = false,
    maxSideLen = 1920,     // 更高分辨率
    padding = 80,
    boxScoreThresh = 0.3f, // 更低的阈值
    boxThresh = 0.2f,
    unClipRatio = 2.0f,
    doCls = true,
    mostCls = false
)
```

### 快速识别场景（速度优先）

```kotlin
val result = ocrEngine.detect(
    bmp = bitmap,
    scaleUp = false,
    maxSideLen = 640,      // 更低分辨率
    padding = 30,
    boxScoreThresh = 0.6f, // 更高的阈值
    boxThresh = 0.4f,
    unClipRatio = 1.5f,
    doCls = false,         // 跳过方向分类
    mostCls = false
)
```

## 4 种加载策略

| 策略 | 说明 | 使用场景 |
|------|------|----------|
| `FILE_FIRST` | 优先文件系统，降级到 assets | **默认**，优先使用下载的模型 |
| `ASSETS_FIRST` | 优先 assets，降级到文件系统 | 确保模型一致性 |
| `FILE_ONLY` | 仅文件系统 | 必须使用外部模型 |
| `ASSETS_ONLY` | 仅 assets | 仅使用内置模型 |

## 3 种模型版本

| 版本 | 说明 | 可用性 |
|------|------|--------|
| `V3` | PP-OCRv3 | ✅ 内置在 assets |
| `V4` | PP-OCRv4 | ⬇️ 需要下载 |
| `V5` | PP-OCRv5 | ⬇️ 需要下载 |

## 常见错误处理

### 错误 1：模型文件不存在

```kotlin
try {
    val ocrEngine = OcrEngine(this, OcrModelVersion.V4, config, strictMode = true)
} catch (e: ModelLoadException) {
    // 提示用户下载模型
    Toast.makeText(this, "请先下载 V4 模型", Toast.LENGTH_SHORT).show()
}
```

### 错误 2：忘记关闭引擎

```kotlin
// ❌ 错误：没有关闭引擎
class MainActivity : AppCompatActivity() {
    private lateinit var ocrEngine: OcrEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ocrEngine = OcrEngine(this, OcrModelVersion.V3)
    }
    // 没有在 onDestroy 中调用 close()
}

// ✅ 正确：在 onDestroy 中关闭
class MainActivity : AppCompatActivity() {
    private lateinit var ocrEngine: OcrEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ocrEngine = OcrEngine(this, OcrModelVersion.V3)
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrEngine.close()  // 释放资源
    }
}
```

## 最佳实践

### 1. 使用 ViewModel 管理生命周期

```kotlin
class OcrViewModel(application: Application) : AndroidViewModel(application) {
    private val ocrEngine = OcrEngine(application, OcrModelVersion.V3)

    fun recognizeImage(bitmap: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        val result = ocrEngine.detect(/* ... */)
        // 更新 UI
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.close()  // ViewModel 销毁时自动关闭
    }
}
```

### 2. 全局单例模式

```kotlin
object OcrEngineHolder {
    private var engine: OcrEngine? = null

    fun get(context: Context): OcrEngine {
        return engine ?: synchronized(this) {
            engine ?: OcrEngine(
                context.applicationContext,
                OcrModelVersion.V3
            ).also { engine = it }
        }
    }

    fun release() {
        engine?.close()
        engine = null
    }
}
```

### 3. 协程 + Flow

```kotlin
class OcrRepository(private val context: Context) {
    private val ocrEngine = OcrEngine(context, OcrModelVersion.V3)

    fun recognize(bitmap: Bitmap): Flow<OcrResult> = flow {
        val result = withContext(Dispatchers.IO) {
            ocrEngine.detect(
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
        emit(result)
    }.flowOn(Dispatchers.IO)

    fun close() {
        ocrEngine.close()
    }
}
```

## 性能优化建议

1. **复用 OcrEngine 实例**：创建和初始化开销较大，应尽量复用
2. **在后台线程执行**：`detect()` 是耗时操作，必须在后台线程执行
3. **调整图片尺寸**：根据需求调整 `maxSideLen`，过大会影响性能
4. **选择合适的模型版本**：V3 最快，V4/V5 精度更高但更慢
5. **按需开启分类**：如果确定文字方向正确，可关闭 `doCls`

## 下一步

- 📖 查看完整 [API 参考文档](./api-reference.md)
- 🎨 查看 [设计文档](./ocr-config-design.md)
- 💡 查看 app 模块的示例代码
- 🐛 遇到问题？查看 [常见问题](./api-reference.md#常见问题)

---

**Happy Coding! 🚀**
