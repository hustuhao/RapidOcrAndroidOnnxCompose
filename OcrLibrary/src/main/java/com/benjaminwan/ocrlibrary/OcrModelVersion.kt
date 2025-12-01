package com.benjaminwan.ocrlibrary

enum class OcrModelVersion(
    val versionName: String,
    val detModelName: String,
    val recModelName: String,
    val clsModelName: String,
    val keysName: String,
    val detUrl: String,
    val recUrl: String,
    val keysUrl: String? = null
) {
    V3(
        versionName = "PP-OCRv3",
        detModelName = "ch_PP-OCRv3_det_infer.onnx",
        recModelName = "ch_PP-OCRv3_rec_infer.onnx",
        clsModelName = "ch_ppocr_mobile_v2.0_cls_infer.onnx",
        keysName = "ppocr_keys_v1.txt",
        detUrl = "", // V3已预置在assets
        recUrl = "",
        keysUrl = null
    ),
    V4(
        versionName = "PP-OCRv4",
        detModelName = "ch_PP-OCRv4_det_infer.onnx",
        recModelName = "ch_PP-OCRv4_rec_infer.onnx",
        clsModelName = "ch_ppocr_mobile_v2.0_cls_infer.onnx",
        keysName = "ppocr_keys_v1.txt",
        detUrl = "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/onnx/PP-OCRv4/det/ch_PP-OCRv4_det_infer.onnx",
        recUrl = "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/onnx/PP-OCRv4/rec/ch_PP-OCRv4_rec_infer.onnx",
        keysUrl = null // V4使用与V3相同的字典
    ),
    V5(
        versionName = "PP-OCRv5",
        detModelName = "ch_PP-OCRv5_mobile_det.onnx",
        recModelName = "ch_PP-OCRv5_rec_mobile_infer.onnx",
        clsModelName = "ch_ppocr_mobile_v2.0_cls_infer.onnx",
        keysName = "ppocrv5_dict.txt",
        detUrl = "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/onnx/PP-OCRv5/det/ch_PP-OCRv5_mobile_det.onnx",
        recUrl = "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/onnx/PP-OCRv5/rec/ch_PP-OCRv5_rec_mobile_infer.onnx",
        keysUrl = "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/paddle/PP-OCRv5/rec/ch_PP-OCRv5_rec_mobile_infer/ppocrv5_dict.txt"
    );

    companion object {
        fun fromName(name: String): OcrModelVersion {
            return values().find { it.name == name } ?: V3
        }
    }
}
