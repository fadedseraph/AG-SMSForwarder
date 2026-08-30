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
                id = "gemma-2b-it-cpu-int4",
                title = "Gemma 2B IT (CPU INT4)",
                description = "Google's lightweight 2B instruction-tuned model optimized for CPU inference with MediaPipe.",
                sizeLabel = "~1.3 GB",
                downloadUrl = "https://huggingface.co/google/gemma-2b-it-cpu-int4/resolve/main/gemma-2b-it-cpu-int4.bin",
                fileName = "gemma-2b-it-cpu-int4.bin",
                isGatedHuggingFace = true,
                recommendedFor = "Recommended for most Android devices (CPU)"
            ),
            ModelCatalogItem(
                id = "gemma-2b-it-gpu-int4",
                title = "Gemma 2B IT (GPU INT4)",
                description = "Google's 2B instruction-tuned model with GPU/OpenCL acceleration for ultra-fast latency.",
                sizeLabel = "~1.3 GB",
                downloadUrl = "https://huggingface.co/google/gemma-2b-it-gpu-int4/resolve/main/gemma-2b-it-gpu-int4.bin",
                fileName = "gemma-2b-it-gpu-int4.bin",
                isGatedHuggingFace = true,
                recommendedFor = "Modern devices with Adreno / Mali GPUs"
            ),
            ModelCatalogItem(
                id = "gemma-3-1b-it",
                title = "Gemma 3 1B IT (LiteRT / MediaPipe)",
                description = "Ultra-compact 1B parameter model with minimal RAM footprint and fast response times.",
                sizeLabel = "~950 MB",
                downloadUrl = "https://huggingface.co/google/gemma-3-1b-it/resolve/main/gemma-3-1b-it.bin",
                fileName = "gemma-3-1b-it.bin",
                isGatedHuggingFace = true,
                recommendedFor = "Low-RAM / Budget devices"
            )
        )
    }
}
