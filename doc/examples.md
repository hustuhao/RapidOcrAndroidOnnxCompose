# OcrLibrary 使用示例集合

本文档提供了 OcrLibrary 的各种实际使用场景示例。

## 目录

- [基础示例](#基础示例)
- [配置示例](#配置示例)
- [高级场景](#高级场景)
- [集成示例](#集成示例)
- [最佳实践](#最佳实践)

---

## 基础示例

### 示例 1：最简单的使用

```kotlin
class SimpleOcrActivity : AppCompatActivity() {
    private lateinit var ocrEngine: OcrEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 创建引擎
        ocrEngine = OcrEngine(this, OcrModelVersion.V3)

        // 从相册选择图片后...
        val bitmap = loadBitmapFromUri(imageUri)
        recognizeImage(bitmap)
    }

    private fun recognizeImage(bitmap: Bitmap) {
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

        // 显示结果
        findViewById<TextView>(R.id.tvResult).text = result.text
        findViewById<ImageView>(R.id.ivBoxImage).setImageBitmap(result.boxImage)
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrEngine.close()
    }
}
```

### 示例 2：在 Fragment 中使用

```kotlin
class OcrFragment : Fragment() {
    private var ocrEngine: OcrEngine? = null
    private var _binding: FragmentOcrBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOcrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 创建引擎
        ocrEngine = OcrEngine(requireContext(), OcrModelVersion.V3)

        // 设置点击事件
        binding.btnRecognize.setOnClickListener {
            val bitmap = loadBitmap()
            recognizeImage(bitmap)
        }
    }

    private fun recognizeImage(bitmap: Bitmap) {
        ocrEngine?.let { engine ->
            val result = engine.detect(
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

            binding.tvResult.text = result.text
            binding.ivBoxImage.setImageBitmap(result.boxImage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ocrEngine?.close()
        ocrEngine = null
        _binding = null
    }
}
```

---

## 配置示例

### 示例 3：使用自定义模型路径

```kotlin
class CustomPathActivity : AppCompatActivity() {
    private lateinit var ocrEngine: OcrEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 配置自定义路径
        val config = OcrConfig.builder()
            .pathConfig {
                fromVersion("/sdcard/Download/ocr_models", OcrModelVersion.V4)
            }
            .loadStrategy(OcrLoadStrategy.FILE_FIRST)
            .build()

        // 创建引擎
        ocrEngine = OcrEngine(this, OcrModelVersion.V4, config)
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrEngine.close()
    }
}
```

### 示例 4：全局配置

```kotlin
// Application 类
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 设置全局配置
        setupOcrConfig()
    }

    private fun setupOcrConfig() {
        val config = OcrConfig.builder()
            .pathConfig {
                // 使用应用内部存储
                fromVersion(filesDir.absolutePath + "/models", OcrModelVersion.V4)
            }
            .loadStrategy(OcrLoadStrategy.FILE_FIRST)
            .build()

        OcrConfigManager.setGlobalConfig(config)
        Log.i("App", "OCR global config set")
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

    override fun onDestroy() {
        super.onDestroy()
        ocrEngine.close()
    }
}
```

### 示例 5：混合配置（部分自定义）

```kotlin
class MixedConfigActivity : AppCompatActivity() {
    private lateinit var ocrEngine: OcrEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 只自定义检测模型，其他使用默认
        val config = OcrConfig.builder()
            .pathConfig {
                detModelPath("/sdcard/models/custom_det.onnx")
                // cls, rec, keys 将使用默认路径
            }
            .loadStrategy(OcrLoadStrategy.FILE_FIRST)
            .build()

        ocrEngine = OcrEngine(this, OcrModelVersion.V4, config)
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrEngine.close()
    }
}
```

---

## 高级场景

### 示例 6：多版本共存

```kotlin
class MultiVersionManager(private val context: Context) {
    private val engines = mutableMapOf<OcrModelVersion, OcrEngine>()

    init {
        // 初始化多个版本
        initEngines()
    }

    private fun initEngines() {
        // V3 - 使用内置模型
        engines[OcrModelVersion.V3] = OcrEngine(context, OcrModelVersion.V3)

        // V4 - 使用下载目录
        val v4Config = OcrConfig.builder()
            .pathConfig {
                fromVersion(context.filesDir.absolutePath + "/models/v4", OcrModelVersion.V4)
            }
            .build()
        engines[OcrModelVersion.V4] = OcrEngine(context, OcrModelVersion.V4, v4Config)

        // V5 - 使用外部存储
        val v5Config = OcrConfig.builder()
            .pathConfig {
                fromVersion("/sdcard/ocr_v5", OcrModelVersion.V5)
            }
            .loadStrategy(OcrLoadStrategy.FILE_ONLY)
            .build()
        engines[OcrModelVersion.V5] = OcrEngine(context, OcrModelVersion.V5, v5Config)
    }

    fun recognizeWithVersion(bitmap: Bitmap, version: OcrModelVersion): OcrResult? {
        return engines[version]?.detect(
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

    fun getAvailableVersions(): List<OcrModelVersion> {
        return engines.keys.toList()
    }

    fun close() {
        engines.values.forEach { it.close() }
        engines.clear()
    }
}

// 使用
class MainActivity : AppCompatActivity() {
    private lateinit var multiVersionManager: MultiVersionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        multiVersionManager = MultiVersionManager(this)

        // 使用不同版本识别
        val bitmap = loadBitmap()
        val resultV3 = multiVersionManager.recognizeWithVersion(bitmap, OcrModelVersion.V3)
        val resultV4 = multiVersionManager.recognizeWithVersion(bitmap, OcrModelVersion.V4)
    }

    override fun onDestroy() {
        super.onDestroy()
        multiVersionManager.close()
    }
}
```

### 示例 7：动态切换模型

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

        Log.i("OCR", "Switched to ${version.versionName} at $modelDir")
    }

    fun recognize(bitmap: Bitmap): OcrResult? {
        val engine = currentEngine ?: run {
            Log.w("OCR", "Engine not initialized")
            return null
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

    fun getCurrentVersion(): OcrModelVersion? = currentVersion

    fun close() {
        currentEngine?.close()
        currentEngine = null
        currentVersion = null
    }
}

// 使用
class SwitchableOcrActivity : AppCompatActivity() {
    private lateinit var ocrManager: DynamicOcrManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ocrManager = DynamicOcrManager(this)

        // 初始使用 V3
        ocrManager.switchToVersion(
            OcrModelVersion.V3,
            filesDir.absolutePath + "/models"
        )

        // 版本切换按钮
        findViewById<Button>(R.id.btnSwitchV4).setOnClickListener {
            ocrManager.switchToVersion(
                OcrModelVersion.V4,
                "/sdcard/models/v4"
            )
        }

        // 识别按钮
        findViewById<Button>(R.id.btnRecognize).setOnClickListener {
            val bitmap = loadBitmap()
            val result = ocrManager.recognize(bitmap)
            result?.let { displayResult(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrManager.close()
    }
}
```

### 示例 8：严格模式验证

```kotlin
class SafeOcrInitializer(private val context: Context) {

    fun initializeWithValidation(
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

            Log.i("OCR", "Engine initialized successfully")
            Result.success(engine)

        } catch (e: ModelLoadException) {
            Log.e("OCR", "Model validation failed: ${e.message}", e)
            Result.failure(e)

        } catch (e: Exception) {
            Log.e("OCR", "Engine initialization failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun initializeWithFallback(
        primaryVersion: OcrModelVersion,
        primaryDir: String,
        fallbackVersion: OcrModelVersion
    ): OcrEngine {
        // 尝试使用主版本
        val primaryResult = initializeWithValidation(primaryVersion, primaryDir)

        return primaryResult.getOrNull() ?: run {
            // 失败时使用备用版本（V3 内置）
            Log.w("OCR", "Primary version failed, fallback to ${fallbackVersion.versionName}")
            OcrEngine(context, fallbackVersion)
        }
    }
}

// 使用
class SafeOcrActivity : AppCompatActivity() {
    private var ocrEngine: OcrEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initializer = SafeOcrInitializer(this)

        // 方式 1：显式处理结果
        val result = initializer.initializeWithValidation(
            OcrModelVersion.V4,
            "/sdcard/models"
        )

        result.onSuccess { engine ->
            ocrEngine = engine
            Toast.makeText(this, "OCR 初始化成功", Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            Toast.makeText(this, "OCR 初始化失败: ${error.message}", Toast.LENGTH_LONG).show()
            // 使用默认版本
            ocrEngine = OcrEngine(this, OcrModelVersion.V3)
        }

        // 方式 2：自动回退
        ocrEngine = initializer.initializeWithFallback(
            primaryVersion = OcrModelVersion.V4,
            primaryDir = "/sdcard/models",
            fallbackVersion = OcrModelVersion.V3
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrEngine?.close()
    }
}
```

---

## 集成示例

### 示例 9：与 ViewModel 集成

```kotlin
class OcrViewModel(application: Application) : AndroidViewModel(application) {
    private val ocrEngine: OcrEngine = OcrEngine(application, OcrModelVersion.V3)

    private val _ocrResult = MutableLiveData<OcrResult>()
    val ocrResult: LiveData<OcrResult> = _ocrResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun recognizeImage(bitmap: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        _isLoading.postValue(true)

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

            _ocrResult.postValue(result)

        } catch (e: Exception) {
            _error.postValue(e.message ?: "识别失败")
            Log.e("OcrViewModel", "Recognition failed", e)

        } finally {
            _isLoading.postValue(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.close()
    }
}

// Activity 中使用
class OcrActivity : AppCompatActivity() {
    private val viewModel: OcrViewModel by viewModels()
    private lateinit var binding: ActivityOcrBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()

        binding.btnRecognize.setOnClickListener {
            val bitmap = loadBitmap()
            viewModel.recognizeImage(bitmap)
        }
    }

    private fun observeViewModel() {
        viewModel.ocrResult.observe(this) { result ->
            binding.tvResult.text = result.text
            binding.ivBoxImage.setImageBitmap(result.boxImage)
            binding.tvTime.text = "耗时: ${result.fullTime}ms"
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRecognize.isEnabled = !isLoading
        }

        viewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }
}
```

### 示例 10：与 Repository 模式集成

```kotlin
// Repository
class OcrRepository(private val context: Context) {
    private val ocrEngine = OcrEngine(context, OcrModelVersion.V3)

    suspend fun recognize(bitmap: Bitmap): Result<OcrResult> = withContext(Dispatchers.IO) {
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
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        ocrEngine.close()
    }
}

// ViewModel
class OcrViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OcrRepository(application)

    private val _uiState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    fun recognizeImage(bitmap: Bitmap) = viewModelScope.launch {
        _uiState.value = OcrUiState.Loading

        val result = repository.recognize(bitmap)

        _uiState.value = result.fold(
            onSuccess = { OcrUiState.Success(it) },
            onFailure = { OcrUiState.Error(it.message ?: "Unknown error") }
        )
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}

// UI State
sealed class OcrUiState {
    object Idle : OcrUiState()
    object Loading : OcrUiState()
    data class Success(val result: OcrResult) : OcrUiState()
    data class Error(val message: String) : OcrUiState()
}

// Compose UI
@Composable
fun OcrScreen(viewModel: OcrViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (uiState) {
            is OcrUiState.Idle -> {
                Text("请选择图片")
            }
            is OcrUiState.Loading -> {
                CircularProgressIndicator()
            }
            is OcrUiState.Success -> {
                val result = (uiState as OcrUiState.Success).result
                Text("识别结果:")
                Text(result.text)
                Image(
                    bitmap = result.boxImage.asImageBitmap(),
                    contentDescription = "Box Image"
                )
            }
            is OcrUiState.Error -> {
                val error = (uiState as OcrUiState.Error).message
                Text("错误: $error", color = Color.Red)
            }
        }
    }
}
```

---

## 最佳实践

### 示例 11：单例模式

```kotlin
object OcrEngineManager {
    @Volatile
    private var instance: OcrEngine? = null
    private val lock = Any()

    fun getInstance(context: Context): OcrEngine {
        return instance ?: synchronized(lock) {
            instance ?: createEngine(context).also { instance = it }
        }
    }

    private fun createEngine(context: Context): OcrEngine {
        val appContext = context.applicationContext

        // 优先使用全局配置
        val config = OcrConfigManager.getGlobalConfig() ?: run {
            // 如果没有全局配置，创建默认配置
            OcrConfig.builder()
                .pathConfig {
                    fromVersion(appContext.filesDir.absolutePath + "/models", OcrModelVersion.V3)
                }
                .loadStrategy(OcrLoadStrategy.FILE_FIRST)
                .build()
        }

        return OcrEngine(appContext, OcrModelVersion.V3, config)
    }

    fun release() {
        synchronized(lock) {
            instance?.close()
            instance = null
        }
    }
}

// 使用
class AnyActivity : AppCompatActivity() {
    private val ocrEngine by lazy { OcrEngineManager.getInstance(this) }

    fun recognizeImage(bitmap: Bitmap) {
        val result = ocrEngine.detect(/* ... */)
        // 处理结果
    }

    // 不需要在 onDestroy 中关闭，由 OcrEngineManager 统一管理
}
```

### 示例 12：错误处理

```kotlin
class RobustOcrManager(private val context: Context) {
    private var ocrEngine: OcrEngine? = null

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        try {
            val config = OcrConfig.builder()
                .pathConfig {
                    fromVersion(context.filesDir.absolutePath + "/models", OcrModelVersion.V4)
                }
                .loadStrategy(OcrLoadStrategy.FILE_FIRST)
                .build()

            ocrEngine = OcrEngine(context, OcrModelVersion.V4, config, strictMode = true)
            Log.i("OCR", "Engine initialized with V4")

        } catch (e: ModelLoadException) {
            Log.w("OCR", "Failed to load V4, falling back to V3", e)
            // 回退到 V3 内置模型
            try {
                ocrEngine = OcrEngine(context, OcrModelVersion.V3)
                Log.i("OCR", "Fallback to V3 successful")
            } catch (fallbackError: Exception) {
                Log.e("OCR", "Failed to initialize OCR engine", fallbackError)
            }
        }
    }

    fun recognize(bitmap: Bitmap): Result<OcrResult> {
        val engine = ocrEngine ?: return Result.failure(
            IllegalStateException("OCR engine not initialized")
        )

        return try {
            val result = engine.detect(
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
            Result.success(result)

        } catch (e: Exception) {
            Log.e("OCR", "Recognition failed", e)
            Result.failure(e)
        }
    }

    fun close() {
        ocrEngine?.close()
        ocrEngine = null
    }
}
```

### 示例 13：性能监控

```kotlin
class PerformanceMonitoringOcr(private val context: Context) {
    private val ocrEngine = OcrEngine(context, OcrModelVersion.V3)
    private val performanceData = mutableListOf<PerformanceRecord>()

    data class PerformanceRecord(
        val timestamp: Long,
        val detTime: Double,
        val clsTime: Double,
        val recTime: Double,
        val fullTime: Double,
        val textBoxCount: Int
    )

    fun recognizeWithMonitoring(bitmap: Bitmap): OcrResult {
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

        // 记录性能数据
        val record = PerformanceRecord(
            timestamp = System.currentTimeMillis(),
            detTime = result.detTime,
            clsTime = result.clsTime,
            recTime = result.recTime,
            fullTime = result.fullTime,
            textBoxCount = result.detResults.size
        )
        performanceData.add(record)

        // 日志输出
        Log.i("OCR_PERF", """
            识别完成:
            - 检测: ${result.detTime}ms
            - 分类: ${result.clsTime}ms
            - 识别: ${result.recTime}ms
            - 总计: ${result.fullTime}ms
            - 文字框: ${result.detResults.size}
        """.trimIndent())

        return result
    }

    fun getAveragePerformance(): Map<String, Double> {
        if (performanceData.isEmpty()) return emptyMap()

        return mapOf(
            "avgDetTime" to performanceData.map { it.detTime }.average(),
            "avgClsTime" to performanceData.map { it.clsTime }.average(),
            "avgRecTime" to performanceData.map { it.recTime }.average(),
            "avgFullTime" to performanceData.map { it.fullTime }.average(),
            "avgTextBoxCount" to performanceData.map { it.textBoxCount.toDouble() }.average()
        )
    }

    fun close() {
        ocrEngine.close()
    }
}
```

---

## 更多示例

请查看项目的 `app/` 模块，其中包含了完整的演示应用。

---

**文档版本**：0.1.0
**最后更新**：2025-12-02
