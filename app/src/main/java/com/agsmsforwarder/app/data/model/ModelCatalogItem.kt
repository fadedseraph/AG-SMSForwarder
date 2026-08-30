package com.agsmsforwarder.app.data.model

data class ModelCatalogItem(
    val id: String,
    val title: String,
    val description: String,
    val sizeLabel: String,
    val downloadUrl: String,
    val fileName: String,
    val isGatedHuggingFace: Boolean = false,
    val recommendedFor: String = "CPU & GPU"
) {
    companion object {
        val PRESET_MODELS = listOf(
            ModelCatalogItem(
                id = "gemma-2b-it-cpu-int4-open",
                title = "Gemma 2B IT (CPU INT4) [Open Mirror]",
                description = "Pre-quantized 4-bit CPU model ready for instant download without needing a HuggingFace account.",
                sizeLabel = "~1.35 GB",
                downloadUrl = "https://huggingface.co/xianbao/mediapipe-gemma-2b-it/resolve/main/gemma-2b-it-cpu-int4.bin",
                fileName = "gemma-2b-it-cpu-int4.bin",
                isGatedHuggingFace = false,
                recommendedFor = "Instant 1-Tap Download (No Token Required)"
            ),
            ModelCatalogItem(
                id = "gemma-2b-it-cpu-int4-google",
                title = "Google Gemma 2B IT (CPU INT4) [Official]",
                description = "Official Google LiteRT/MediaPipe 4-bit CPU weights from google/gemma-2b-it-tflite.",
                sizeLabel = "~1.35 GB",
                downloadUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-cpu-int4.bin",
                fileName = "gemma-2b-it-cpu-int4.bin",
                isGatedHuggingFace = true,
                recommendedFor = "Official Google Weights (Requires HF Token)"
            ),
            ModelCatalogItem(
                id = "gemma-2b-it-gpu-int4-google",
                title = "Google Gemma 2B IT (GPU INT4) [Official]",
                description = "Official Google LiteRT/MediaPipe GPU/OpenCL accelerated model for ultra-low latency.",
                sizeLabel = "~1.35 GB",
                downloadUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-gpu-int4.bin",
                fileName = "gemma-2b-it-gpu-int4.bin",
                isGatedHuggingFace = true,
                recommendedFor = "Adreno / Mali GPU Acceleration (Requires HF Token)"
            ),
            ModelCatalogItem(
                id = "gemma-2b-it-cpu-int8-google",
                title = "Google Gemma 2B IT (CPU INT8) [Official]",
                description = "Higher fidelity 8-bit quantization for devices with 8GB+ RAM.",
                sizeLabel = "~2.6 GB",
                downloadUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-cpu-int8.bin",
                fileName = "gemma-2b-it-cpu-int8.bin",
                isGatedHuggingFace = true,
                recommendedFor = "Higher Accuracy / 8GB+ RAM (Requires HF Token)"
            )
        )
    }
}
