# Proguard rules for AG-SMSForwarder

# MediaPipe Tasks GenAI
-keep class com.google.mediapipe.tasks.genai.** { *; }
-keep class com.google.mediapipe.tasks.core.** { *; }

# Room SQLite
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
