package com.agsmsforwarder.app.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val fileName: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPct: Float,
        val speedText: String
    ) : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ModelDownloadManager(private val context: Context) {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var activeJob: Job? = null

    /**
     * Downloads a model file from a URL to the app's internal filesDir/models/ directory.
     */
    suspend fun downloadModel(
        urlStr: String,
        fileName: String,
        bearerToken: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        val targetFile = File(modelsDir, fileName)
        val tempFile = File(modelsDir, "$fileName.download")

        _downloadState.value = DownloadState.Downloading(
            fileName = fileName,
            bytesDownloaded = 0L,
            totalBytes = -1L,
            progressPct = 0f,
            speedText = "Connecting..."
        )

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            var currentUrl = urlStr
            var redirects = 0
            val maxRedirects = 6

            // Handle HTTP 3xx Redirects (standard for HuggingFace CDN)
            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "AG-SMSForwarder/1.0 (Android)")
                    if (!bearerToken.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer ${bearerToken.trim()}")
                    }
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (location.isNullOrBlank()) {
                        throw IllegalStateException("Received redirect $responseCode without Location header")
                    }
                    currentUrl = location
                    connection.disconnect()
                    redirects++
                } else if (responseCode in 200..299) {
                    break
                } else if (responseCode == 401 || responseCode == 403) {
                    throw IllegalStateException("Access Denied (HTTP $responseCode). If downloading from Hugging Face, please supply a User Access Token.")
                } else if (responseCode == 404) {
                    throw IllegalStateException("File not found (HTTP 404). Please verify the URL.")
                } else {
                    throw IllegalStateException("Server returned HTTP $responseCode: ${connection.responseMessage}")
                }
            }

            if (redirects >= maxRedirects) {
                throw IllegalStateException("Too many redirects ($redirects)")
            }

            val totalBytes = connection?.contentLengthLong ?: -1L
            inputStream = connection?.inputStream ?: throw IllegalStateException("Input stream is null")
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(64 * 1024) // 64 KB buffer
            var bytesRead: Int
            var totalDownloaded = 0L
            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L
            var currentSpeedText = "Calculating..."

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!coroutineContext.isActive) {
                    throw CancellationException("Download cancelled by user")
                }

                outputStream.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastUpdateTime
                if (elapsed >= 500) { // Update UI twice per second
                    val speedBytesPerSec = (bytesSinceLastUpdate * 1000) / elapsed
                    currentSpeedText = formatSpeed(speedBytesPerSec)
                    val progressPct = if (totalBytes > 0) totalDownloaded.toFloat() / totalBytes else 0f

                    _downloadState.value = DownloadState.Downloading(
                        fileName = fileName,
                        bytesDownloaded = totalDownloaded,
                        totalBytes = totalBytes,
                        progressPct = progressPct,
                        speedText = currentSpeedText
                    )

                    lastUpdateTime = now
                    bytesSinceLastUpdate = 0L
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            // Move temp file to final target file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                // Fallback copy if rename fails
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            _downloadState.value = DownloadState.Completed(targetFile)
            Log.i(TAG, "Model downloaded successfully to ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            Result.success(targetFile)
        } catch (e: CancellationException) {
            Log.w(TAG, "Download cancelled")
            tempFile.delete()
            _downloadState.value = DownloadState.Idle
            Result.failure(e)
        } catch (e: Throwable) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            tempFile.delete()
            val errMsg = e.localizedMessage ?: "Download failed: Unknown error"
            _downloadState.value = DownloadState.Error(errMsg)
            Result.failure(e)
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    fun cancelDownload() {
        activeJob?.cancel()
        _downloadState.value = DownloadState.Idle
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytesPerSec.toDouble() / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format(Locale.US, "%.0f KB/s", bytesPerSec.toDouble() / 1024)
            else -> "$bytesPerSec B/s"
        }
    }

    companion object {
        private const val TAG = "ModelDownloadManager"
    }
}
