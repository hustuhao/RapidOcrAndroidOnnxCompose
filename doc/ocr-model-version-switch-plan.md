# PP-OCR模型版本切换功能实现计划

## 概述

为RapidOCR Android应用添加在PP-OCRv3、v4、v5之间切换模型的功能。用户可以在应用内选择不同版本的OCR模型，系统会自动下载并加载对应版本，选择会被持久化保存。

## 一、架构设计

### 1.1 模型版本配置枚举

**新建文件**: `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/OcrModelVersion.kt`

定义枚举类封装每个版本的所有配置：
- 模型文件名（det/rec/cls/keys）
- 预处理参数（mean/norm values）
- 下载URL
- 版本显示名称

```kotlin
enum class OcrModelVersion(
    val versionName: String,
    val detModelName: String,
    val recModelName: String,
    val clsModelName: String,
    val keysName: String,
    val detUrl: String,
    val recUrl: String,
    val keysUrl: String?  // V5需要下载字典
) {
    V3(...),
    V4(...),
    V5(...)  // 注意V5使用不同的字典文件
}
```

### 1.2 OcrEngine重构

**修改文件**: `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/OcrEngine.kt`

核心修改：
- 构造函数添加 `modelVersion: OcrModelVersion = OcrModelVersion.V3` 参数
- 移除companion object中的硬编码常量（DET_NAME, CLS_NAME等）
- 使用 `modelVersion.detModelName` 等动态获取文件名
- 将预处理参数传递给Det/Cls/Rec类

### 1.3 Det/Cls/Rec类参数化

**修改文件**:
- `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Det.kt`
- `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Cls.kt`
- `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Rec.kt`

每个类添加构造参数：
- `meanValues: FloatArray`（如果V3/V4/V5有差异）
- `normValues: FloatArray`（如果V3/V4/V5有差异）
- 移除companion object中的硬编码mean/norm常量

**注意**: 需要先验证V3/V4/V5的预处理参数是否一致。根据PaddleOCR官方文档：
- Det: mean=[0.485*255, 0.456*255, 0.406*255], norm=[1/0.229/255, 1/0.224/255, 1/0.225/255]
- Rec: mean=[127.5, 127.5, 127.5], norm=[1/127.5, 1/127.5, 1/127.5]
- 如果V3/V4/V5参数一致，可以简化实现，不需要参数化mean/norm

## 二、模型文件管理

### 2.1 模型下载器

**新建文件**: `app/src/main/java/com/benjaminwan/ocr/utils/ModelDownloader.kt`

功能：
- 从default_models.yaml中的URL下载模型文件
- 保存到应用内部存储（context.filesDir/models/）
- 显示下载进度
- SHA256校验（可选）
- 错误处理和重试

下载策略：
- 首次选择某个版本时触发下载
- 下载完成前禁用OCR识别功能
- 显示下载进度对话框

### 2.2 模型文件位置

**方案**: 混合存储
- V3模型继续预置在assets目录（向后兼容）
- V4/V5模型首次使用时下载到内部存储：`context.filesDir/models/`

**OcrEngine加载逻辑**:
```kotlin
private fun loadModel(modelName: String): ByteArray {
    // 优先从内部存储加载
    val file = File(context.filesDir, "models/$modelName")
    if (file.exists()) {
        return file.readBytes()
    }
    // 降级到assets
    return assetManager.open(modelName).readBytes()
}
```

### 2.3 需要下载的文件

**PP-OCRv4**:
1. `ch_PP-OCRv4_det_infer.onnx` (~2.4MB)
2. `ch_PP-OCRv4_rec_infer.onnx` (~10.7MB)

**PP-OCRv5**:
1. `ch_PP-OCRv5_mobile_det.onnx` (~2.4MB)
2. `ch_PP-OCRv5_rec_mobile_infer.onnx` (~10.7MB)
3. `ppocrv5_dict.txt` (~26KB) - **重要**: V5使用不同字典

下载URL来自：`default_models.yaml` → onnxruntime → PP-OCRv4/v5

## 三、UI实现

### 3.1 UI位置建议

**推荐方案**: Gallery的Parameter Tab顶部

理由：
- 模型版本是影响识别效果的核心参数，与其他参数属于同一类别
- 用户在调整参数时可以同时切换版本进行对比
- 无需创建额外的设置界面，实现简单
- 符合现有的参数配置模式

### 3.2 GalleryState扩展

**修改文件**: `app/src/main/java/com/benjaminwan/ocr/screens/gallery/GalleryState.kt`

添加字段：
```kotlin
data class GalleryState(
    val modelVersion: OcrModelVersion = OcrModelVersion.V3,
    val isDownloadingModel: Boolean = false,
    val downloadProgress: Float = 0f,
    // ... 现有字段
)
```

### 3.3 GalleryViewModel修改

**修改文件**: `app/src/main/java/com/benjaminwan/ocr/screens/gallery/GalleryViewModel.kt`

核心修改：
1. 添加 `PreferencesManager` 依赖注入
2. 将 `ocrEngine` 从 `val` 改为 `var`，支持重新初始化
3. init中从SharedPreferences加载保存的版本
4. 新增方法：
   ```kotlin
   fun setModelVersion(version: OcrModelVersion) {
       // 1. 检查模型文件是否存在
       // 2. 不存在则触发下载
       // 3. 下载完成后关闭旧OcrEngine
       // 4. 创建新OcrEngine(context, version)
       // 5. 保存到SharedPreferences
       // 6. 更新state
   }
   ```

### 3.4 GalleryScreen UI

**修改文件**: `app/src/main/java/com/benjaminwan/ocr/screens/gallery/GalleryScreen.kt`

在ParamView顶部添加：
```kotlin
// 模型版本选择区域
Column {
    Text("OCR模型版本", style = MaterialTheme.typography.h6)
    Text("切换版本后首次使用会自动下载", style = MaterialTheme.typography.caption)

    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
        OcrModelVersion.values().forEach { version ->
            FilterChip(
                selected = state.modelVersion == version,
                onClick = { vm.setModelVersion(version) },
                enabled = !state.isDownloadingModel
            ) {
                Text(version.versionName)
            }
        }
    }

    // 下载进度条（条件显示）
    if (state.isDownloadingModel) {
        LinearProgressIndicator(progress = state.downloadProgress)
    }
}
```

## 四、持久化存储

### 4.1 PreferencesManager

**新建文件**: `app/src/main/java/com/benjaminwan/ocr/storage/PreferencesManager.kt`

使用SharedPreferences存储：
```kotlin
class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("ocr_prefs", Context.MODE_PRIVATE)

    fun getModelVersion(): String =
        prefs.getString("model_version", "V3") ?: "V3"

    fun setModelVersion(version: String) {
        prefs.edit().putString("model_version", version).apply()
    }
}
```

### 4.2 依赖注入

在Application类中初始化：
```kotlin
// App.kt
val preferencesManager by lazy { PreferencesManager(this) }
```

## 五、预处理参数兼容性处理

### 5.1 参数验证方法

**需要验证的来源**:
1. PaddleOCR GitHub官方文档
2. 查看 `tools/infer/predict_det.py` 和 `predict_rec.py`
3. 检查V3/V4/V5的配置文件

**如果参数一致**:
- 无需修改Det/Cls/Rec类
- OcrModelVersion枚举简化，只存储文件名和URL

**如果参数不同**:
- Det/Cls/Rec添加mean/norm参数
- OcrModelVersion枚举存储完整参数
- OcrEngine传递参数到Det/Cls/Rec

### 5.2 V5字典文件特殊处理

**关键差异**: V5使用 `ppocrv5_dict.txt` 而非 `ppocr_keys_v1.txt`

影响：
- 字符集可能扩展
- 需要下载V5字典文件
- Rec类加载字典时使用version.keysName

## 六、版本切换流程

### 6.1 切换逻辑

```
用户点击V4 FilterChip
    ↓
GalleryViewModel.setModelVersion(V4)
    ↓
检查模型文件是否存在
    ↓ (不存在)
显示下载对话框
    ↓
ModelDownloader.download(V4.detUrl, V4.recUrl)
    ↓
更新下载进度 → UI显示进度条
    ↓
下载完成
    ↓
ocrEngine.close()  // 释放V3的OrtSession
    ↓
ocrEngine = OcrEngine(context, V4)  // 创建新实例
    ↓
preferencesManager.setModelVersion("V4")
    ↓
setState { modelVersion = V4, isDownloadingModel = false }
    ↓
显示成功提示
```

### 6.2 异常处理

**场景1**: 模型文件缺失
- 显示错误提示："模型文件缺失，请重新下载"
- 提供重试按钮

**场景2**: 下载失败
- 显示网络错误提示
- 保持当前版本不变
- 提供重试按钮

**场景3**: 模型加载失败
- 降级到V3版本
- 显示错误日志

## 七、实现步骤

### 步骤1: 参数验证（优先）
- [ ] 查阅PaddleOCR官方文档，确认V3/V4/V5的mean/norm参数
- [ ] 确定是否需要参数化Det/Cls/Rec类

### 步骤2: 基础架构重构
- [ ] 创建 `OcrModelVersion.kt` 枚举
- [ ] 重构 `OcrEngine.kt` 支持版本参数
- [ ] （如果需要）重构 `Det.kt`, `Cls.kt`, `Rec.kt` 支持参数化

### 步骤3: 模型下载管理
- [ ] 创建 `ModelDownloader.kt`
- [ ] 实现从URL下载到内部存储
- [ ] 实现进度回调
- [ ] OcrEngine支持从内部存储加载模型

### 步骤4: 持久化与UI
- [ ] 创建 `PreferencesManager.kt`
- [ ] 修改 `GalleryState.kt` 添加版本字段
- [ ] 修改 `GalleryViewModel.kt` 实现版本切换逻辑
- [ ] 修改 `GalleryScreen.kt` 添加版本选择UI

### 步骤5: 测试验证
- [ ] 测试V3/V4/V5识别效果
- [ ] 测试版本切换流程
- [ ] 测试下载和持久化
- [ ] 性能测试（内存、速度）

## 八、关键文件清单

### 需要新建的文件（3个）
1. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/OcrModelVersion.kt`
2. `app/src/main/java/com/benjaminwan/ocr/utils/ModelDownloader.kt`
3. `app/src/main/java/com/benjaminwan/ocr/storage/PreferencesManager.kt`

### 需要修改的文件（7个）
1. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/OcrEngine.kt` - 核心重构
2. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Det.kt` - 可选，取决于参数验证结果
3. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Cls.kt` - 可选，取决于参数验证结果
4. `OcrLibrary/src/main/java/com/benjaminwan/ocrlibrary/Rec.kt` - 可选，取决于参数验证结果
5. `app/src/main/java/com/benjaminwan/ocr/screens/gallery/GalleryState.kt`
6. `app/src/main/java/com/benjaminwan/ocr/screens/gallery/GalleryViewModel.kt`
7. `app/src/main/java/com/benjaminwan/ocr/screens/gallery/GalleryScreen.kt`

### 可能需要修改（1个）
- `app/src/main/java/com/benjaminwan/ocr/app/App.kt` - 初始化PreferencesManager

## 九、技术要点

### 9.1 V5关键差异
- 字典文件不同：`ppocrv5_dict.txt` vs `ppocr_keys_v1.txt`
- 需要单独下载V5字典文件
- 模型文件名格式：`ch_PP-OCRv5_mobile_det.onnx` (注意命名变化)

### 9.2 OrtSession生命周期
- 切换版本前必须调用 `ocrEngine.close()` 释放资源
- lazy初始化的Det/Cls/Rec会在首次使用时加载新模型
- 避免识别过程中切换版本（通过editEnabled控制）

### 9.3 APK体积优化
- 仅预置V3模型（~12.9MB）
- V4/V5按需下载（共约26MB）
- 总计增加存储空间需求：约39MB

### 9.4 下载体验优化
- 首次切换时显示下载对话框
- 实时更新进度条
- 支持后台下载
- 下载完成后自动应用新版本

## 十、测试要点

### 功能测试
- [ ] V3→V4切换，验证识别结果
- [ ] V4→V5切换，验证识别结果
- [ ] V5→V3切换，验证降级
- [ ] 重启应用，验证版本持久化
- [ ] 下载中断处理
- [ ] 网络异常处理

### 性能测试
- [ ] 不同版本识别速度对比
- [ ] 内存占用对比
- [ ] 版本切换耗时

### 兼容性测试
- [ ] V5字典文件正确性
- [ ] 不同Android版本（API 21-33）
- [ ] 不同设备（低端/高端）
