# RapidOcrAndroidOnnxCompose

[![Issue](https://img.shields.io/github/issues/RapidAI/RapidOcrAndroidOnnxCompose.svg)](https://github.com/RapidAI/RapidOcrAndroidOnnxCompose/issues)
[![Star](https://img.shields.io/github/stars/RapidAI/RapidOcrAndroidOnnxCompose.svg)](https://github.com/RapidAI/RapidOcrAndroidOnnxCompose)

<details open>
    <summary>目录</summary>

- [RapidOcrAndroidOnnxCompose](#RapidOcrAndroidOnnxCompose)
    - [联系方式](#联系方式)
    - [项目完整源码](#项目完整源码)
    - [APK下载](#APK下载)
    - [简介](#简介)
    - [总体说明](#总体说明)
    - [更新说明](#更新说明)
    - [编译说明](#编译说明)
        - [编译Release包](#编译Release包)
    - [常见问题](#常见问题)
        - [输入参数说明](#输入参数说明)
    - [关于作者](#关于作者)
    - [版权声明](#版权声明)

</details>

## 联系方式

[QQ群](https://rapidai.github.io/RapidOCRDocs/main/communicate/#qq)

## 项目完整源码

* 整合好源码和依赖库的完整工程项目，可到Q群共享内下载或Release下载，以Project开头的压缩包文件为源码工程，例：Project_RapidOcrAndroidOnnxCompose-版本号.7z
* 如果想自己折腾，则请继续阅读本说明

## APK下载

* 编译好的demo apk，可以在release中下载，或者Q群共享内下载，文件名例：RapidOcrAndroidOnnxCompose-版本号-release.apk

## 简介

RapidOcr onnxruntime推理 for Android

使用技术：jetpack compose + kotlin + 协程

## 主要特性

* ✅ **纯 Kotlin 实现**：全部使用 Kotlin 编写，无需 C++ 和 JNI
* ✅ **多版本支持**：支持 PP-OCR V3/V4/V5 三个版本
* ✅ **灵活配置**：支持自定义模型路径和加载策略
* ✅ **易于集成**：提供 AAR 库，可直接集成到项目
* ✅ **高性能**：基于 ONNX Runtime 推理引擎
* ✅ **现代化架构**：Jetpack Compose + Kotlin 协程

## 与之前的版本不同点：

* RapidOcrAndroidOnnx的推理代码使用C++编写，再通过JNI调用
* RapidOcrAndroidOnnxCompose全部使用kotlin编写

## 主要使用的依赖库：

* onnxruntime 1.19.2 [https://github.com/microsoft/onnxruntime](https://github.com/microsoft/onnxruntime)
* opencv 4.6.0 [https://github.com/opencv/opencv](https://github.com/opencv/opencv)

## 更新说明

#### 2025-12-02 update 0.1.0

* ✨ **新增配置系统**：支持自定义模型路径和加载策略
* ✨ **多版本支持**：支持 PP-OCR V3/V4/V5 三个版本切换
* ✨ **灵活配置**：全局配置 + 实例级配置，支持绝对路径和相对路径
* ✨ **4 种加载策略**：FILE_FIRST（默认）、ASSETS_FIRST、FILE_ONLY、ASSETS_ONLY
* ✨ **Builder 模式 API**：类型安全、IDE 友好的配置接口
* ✨ **严格模式验证**：可选的路径预验证功能
* 📦 升级 onnxruntime 1.13.1 → 1.19.2（支持 PP-OCRv5）
* 📖 完整的 API 文档和使用示例
* ✅ 向后兼容，现有代码无需修改

#### 2022-11-12 update 0.0.1

* 跑通完整识别流程
* opencv 4.6.0
* onnxruntime 1.13.1
* compose ui 1.3.1
* kotlin 1.7.10

## 编译说明

1. AndroidStudio 2021.3.1或以上；
2. 整合好的范例工程自带了模型，在OcrLibrary/src/main/assets文件夹中
3. 下载[opencv-4.6.0-android-sdk.zip](https://github.com/opencv/opencv/releases/tag/4.6.0)
   解压后目录结构为

```
项目根目录/sdk
    └── native
        ├── java
        ├── ……
        └── native
```

### 编译Release包

* mac/linux使用命令编译```./gradlew assembleRelease```
* win使用命令编译```gradlew.bat assembleRelease```
* 输出apk文件在app/build/outputs/apk

## 快速开始

### 1. 基础使用

```kotlin
// 创建 OCR 引擎（使用 V3 内置模型）
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

// 获取结果
val text = result.text
val boxImage = result.boxImage

// 释放资源
ocrEngine.close()
```

### 2. 使用自定义模型路径

```kotlin
val config = OcrConfig.builder()
    .pathConfig {
        fromVersion("/sdcard/models", OcrModelVersion.V4)
    }
    .loadStrategy(OcrLoadStrategy.FILE_FIRST)
    .build()

val ocrEngine = OcrEngine(context, OcrModelVersion.V4, config)
```

### 3. 设置全局配置

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

## 文档

* 📖 [API 参考文档](./doc/api-reference.md) - 完整的 API 说明和使用示例
* 🚀 [快速开始指南](./doc/quick-start.md) - 5 分钟快速上手
* 🎨 [配置系统设计](./doc/ocr-config-design.md) - 详细的设计文档
* 📋 [版本切换计划](./doc/ocr-model-version-switch-plan.md) - 多版本切换方案

## AAR 库

OcrLibrary 已打包为 AAR 库，可直接集成到项目：

* **位置**：`OcrLibrary/build/outputs/aar/`
* **文件**：
  - `OcrLibrary-0.1.0-debug.aar` (12MB)
  - `OcrLibrary-0.1.0-release.aar` (12MB)

### 集成方式

```gradle
// 1. 添加依赖仓库
repositories {
    flatDir {
        dirs 'libs'
    }
}

// 2. 添加依赖
dependencies {
    implementation(name: 'OcrLibrary-0.1.0-release', ext: 'aar')

    // 必需的依赖
    implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.19.2'
    implementation 'com.orhanobut:logger:2.2.0'
}
```

## 常见问题

### Q: APK 体积大？

**A**: 因为 opencv 没有经过裁剪，请自行折腾 opencv 裁剪编译。

### Q: 如何切换模型版本？

**A**: 参见 [快速开始指南](./doc/quick-start.md) 或 [API 文档](./doc/api-reference.md)。

### Q: 支持哪些模型版本？

**A**:
* **V3**：内置在 assets，开箱即用
* **V4**：需要下载，支持更高精度
* **V5**：需要下载，最新版本

### Q: 如何自定义模型路径？

**A**: 使用配置系统：

```kotlin
val config = OcrConfig.builder()
    .pathConfig {
        detModelPath("/custom/det.onnx")
        recModelPath("/custom/rec.onnx")
    }
    .build()

val ocrEngine = OcrEngine(context, OcrModelVersion.V4, config)
```

更多问题请查看 [API 文档 - 常见问题](./doc/api-reference.md#常见问题)。

## 性能优化建议

1. **复用 OcrEngine 实例**：创建和初始化开销较大，应尽量复用
2. **在后台线程执行**：`detect()` 是耗时操作，必须在后台线程执行
3. **调整图片尺寸**：根据需求调整 `maxSideLen`，过大会影响性能
4. **选择合适的模型版本**：V3 最快，V4/V5 精度更高但更慢
5. **按需开启分类**：如果确定文字方向正确，可关闭 `doCls`

## 关于作者

* Android demo编写：[benjaminwan](https://github.com/benjaminwan)
* 模型来自：[PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)

## 版权声明

- OCR模型版权归[PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)所有；
- 其它工程代码版权归本仓库所有者所有；

