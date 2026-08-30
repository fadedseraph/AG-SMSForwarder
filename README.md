# AI Bank Notification SMS Forwarder (AG-SMSForwarder)

An Android application built with **Jetpack Compose**, **Material 3**, and **Google AI Edge** (`com.google.mediapipe:tasks-genai`) that intercepts incoming bank notifications, reformats/cleans them into concise financial summaries using an on-device Large Language Model (LLM), and forwards them via SMS.

---

## 🌟 Key Features

1. **On-Device LLM Inference (Google AI Edge / LiteRT)**:
   - Uses `com.google.mediapipe:tasks-genai` to run lightweight LLMs (such as Gemma 2B, Gemma 3 1B, or Falcon `.task` / `.bin` models) directly on the device.
   - Structured few-shot prompt for extracting `[Bank]: $[Amount] spent at [Merchant]`.
   - Automatic classification and skipping of non-transaction notifications (e.g., OTPs, marketing, password resets).

2. **High-Precision Regex Fallback**:
   - If the AI model is uninitialized, missing, or throws an exception, the system automatically falls back to an embedded regex parser to ensure zero dropped transaction alerts.

3. **Resilient Background Processing (`BankNotificationService`)**:
   - Extends Android's `NotificationListenerService`.
   - **60-Second Sliding Window Cache**: Prevents duplicate SMS alerts from repeating notifications.
   - Auto-rebind hook on `onListenerDisconnected()` for continuous background uptime.
   - Battery optimization exemption workflow.

4. **Multi-Recipient SMS Forwarder (`SmsDispatcher`)**:
   - Multipart SMS delivery handling with `SmsManager`.
   - PendingIntent broadcast receivers for real-time `SENT` and `DELIVERED` status updates.

5. **Local SQLite Persistence (Room)**:
   - Maintains a searchable history of the last 100 transactions with timestamps, raw notification text, AI vs. Regex source badge, execution latency (ms), and SMS delivery status.

6. **Jetpack Compose UI & Live Test Playground**:
   - **Health Dashboard**: Real-time permission checks (`SEND_SMS`, Notification Listener, Battery Optimization, Model status).
   - **App Filter Manager**: Pre-configured presets for major banks (Chase, Bank of America, Wells Fargo, Citi, Amex, Capital One, Revolut, etc.) + installed app picker + custom package entries.
   - **Live Playground**: Test sample bank alerts interactively, view AI inference latency, and preview formatted outputs before sending test SMS.
   - **Advanced AI Tuning**: Sliders for temperature, top-K, max tokens, and custom prompt template editor.

---

## 🏗️ Architecture

```
com.agsmsforwarder.app/
├── SmsForwarderApp.kt            # Application instance & service locator
├── ai/
│   ├── TransactionAiFormatter.kt # MediaPipe LlmInference engine & few-shot prompt
│   └── RegexFallbackExtractor.kt # Regex parser fallback
├── data/
│   ├── db/                       # Room SQLite Database & DAO for logs
│   ├── model/                    # Data classes & Enums
│   └── preferences/              # Jetpack DataStore Preferences
├── service/
│   ├── BankNotificationService.kt# NotificationListenerService with sliding cache
│   └── SmsDispatcher.kt          # SmsManager dispatch & broadcast intent tracking
├── receiver/
│   ├── SmsDeliveryReceiver.kt    # Sent/Delivered status updates
│   └── BootCompletedReceiver.kt  # Auto-rebind on boot / app update
└── ui/
    ├── MainActivity.kt           # Permissions, SAF file picker, lifecycle
    ├── MainViewModel.kt          # UI state, testing, preferences
    ├── screens/
    │   └── MainDashboardScreen.kt# Main scrollable dashboard
    ├── components/               # Compose widgets (Dashboard, Test Bench, Logs)
    └── theme/                    # Material 3 palette & typography
```

---

## 🚀 Loading an AI Model (`.task` or `.bin`)

You can supply any MediaPipe-compatible on-device LLM (such as Gemma 2B or Gemma 3 1B):

### Option A: In-App File Picker (SAF)
1. Open the app.
2. Under **Google AI Edge (MediaPipe)**, tap **Select Model (.task/.bin)**.
3. Select the model file from your device's Downloads or storage. The app will copy it to internal storage and initialize the engine.

### Option B: Push via ADB to `/data/local/tmp/`
```bash
adb push gemma-2b-it-cpu.task /data/local/tmp/model.task
```
In the app, enter `/data/local/tmp/model.task` to load the model directly.

---

## 🛠️ Build & Requirements
- **Android Studio Ladybug (2024.2+)** or newer.
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **JDK**: 17
