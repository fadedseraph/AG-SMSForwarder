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
                description = "Verified 4-bit CPU model ready for instant 1-tap download without requiring a HuggingFace account.",
                sizeLabel = "~1.28 GB",
                downloadUrl = "https://huggingface.co/ASahu16/gemma/resolve/main/gemma-2b-it-cpu-int4.bin",
                fileName = "gemma-2b-it-cpu-int4.bin",
                isGatedHuggingFace = false,
                recommendedFor = "Instant 1-Tap Download (No Token Required)"
            ),
            ModelCatalogItem(
                id = "gemma-2b-it-gpu-int4-open",
                title = "Gemma 2B IT (GPU INT4) [Open Mirror]",
                description = "Verified 4-bit GPU/OpenCL accelerated model ready for instant 1-tap download.",
                sizeLabel = "~1.29 GB",
                downloadUrl = "https://huggingface.co/ASahu16/gemma/resolve/main/gemma-2b-it-gpu-int4.bin",
                fileName = "gemma-2b-it-gpu-int4.bin",
                isGatedHuggingFace = false,
                recommendedFor = "Adreno / Mali GPU (No Token Required)"
            ),
            ModelCatalogItem(
                id = "gemma-2-2b-it-cpu-int8-open",
                title = "Gemma 2 2B IT (CPU INT8) [Open Mirror]",
                description = "Higher accuracy Gemma 2 2B .task bundle for devices with 6GB+ RAM.",
                sizeLabel = "~3.05 GB",
                downloadUrl = "https://huggingface.co/ASahu16/gemma/resolve/main/gemma2-2b-it-cpu-int8.task",
                fileName = "gemma2-2b-it-cpu-int8.task",
                isGatedHuggingFace = false,
                recommendedFor = "High Fidelity (No Token Required)"
            ),
            ModelCatalogItem(
                id = "gemma-2b-it-cpu-int4-google",
                title = "Google Gemma 2B IT (CPU INT4) [Official]",
                description = "Official Google LiteRT/MediaPipe 4-bit CPU weights from google/gemma-2b-it-tflite.",
                sizeLabel = "~1.28 GB",
                downloadUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-cpu-int4.bin",
                fileName = "gemma-2b-it-cpu-int4.bin",
                isGatedHuggingFace = true,
                recommendedFor = "Official Google Weights (Requires HF Token)"
            ),
            ModelCatalogItem(
                id = "gemma-2b-it-gpu-int4-google",
                title = "Google Gemma 2B IT (GPU INT4) [Official]",
                description = "Official Google LiteRT/MediaPipe GPU/OpenCL accelerated weights.",
                sizeLabel = "~1.29 GB",
                downloadUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-gpu-int4.bin",
                fileName = "gemma-2b-it-gpu-int4.bin",
                isGatedHuggingFace = true,
                recommendedFor = "Official Google GPU Weights (Requires HF Token)"
            )
        )
    }
}
