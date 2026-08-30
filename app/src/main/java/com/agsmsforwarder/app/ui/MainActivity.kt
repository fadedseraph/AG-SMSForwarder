package com.agsmsforwarder.app.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agsmsforwarder.app.service.BankNotificationService
import com.agsmsforwarder.app.ui.screens.MainDashboardScreen
import com.agsmsforwarder.app.ui.theme.AGSMSForwarderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // SAF Model File Picker
    private val modelPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Not all SAF providers support persistable permissions, continue copy
            }
            viewModel.importModelFile(uri)
            Toast.makeText(this, "Loading selected model file...", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for SMS and Notifications
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        if (!smsGranted) {
            Toast.makeText(this, "SMS permission is required to forward bank alerts.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AGSMSForwarderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val lifecycleOwner = LocalLifecycleOwner.current

                    var hasSmsPermission by remember {
                        mutableStateOf(checkSmsPermission())
                    }
                    var hasNotificationAccess by remember {
                        mutableStateOf(MainViewModel.isNotificationServiceEnabled(this))
                    }
                    var isIgnoringBatteryOptimizations by remember {
                        mutableStateOf(MainViewModel.isIgnoringBatteryOptimizations(this))
                    }

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                hasSmsPermission = checkSmsPermission()
                                hasNotificationAccess = MainViewModel.isNotificationServiceEnabled(this@MainActivity)
                                isIgnoringBatteryOptimizations = MainViewModel.isIgnoringBatteryOptimizations(this@MainActivity)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    val preferences by viewModel.preferences.collectAsState()
                    val logs by viewModel.logs.collectAsState()
                    val modelLoadState by viewModel.modelLoadState.collectAsState()
                    val lastLatencyMs by viewModel.lastLatencyMs.collectAsState()
                    val isTesting by viewModel.isTesting.collectAsState()
                    val testResult by viewModel.testResult.collectAsState()

                    MainDashboardScreen(
                        preferences = preferences,
                        logs = logs,
                        modelLoadState = modelLoadState,
                        lastLatencyMs = lastLatencyMs,
                        hasSmsPermission = hasSmsPermission,
                        hasNotificationAccess = hasNotificationAccess,
                        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                        isTesting = isTesting,
                        testResult = testResult,
                        onGrantSmsPermission = { requestSmsPermission() },
                        onOpenNotificationAccessSettings = { openNotificationAccessSettings() },
                        onRequestBatteryExemption = { requestBatteryOptimizationExemption() },
                        onSelectModelFile = {
                            modelPickerLauncher.launch(arrayOf("*/*"))
                        },
                        onToggleService = { viewModel.setServiceEnabled(it) },
                        onUpdateDestinationNumber = { viewModel.setDestinationPhoneNumber(it) },
                        onToggleAiFormatting = { viewModel.setUseAiFormatting(it) },
                        onUpdateCustomPrefix = { viewModel.setCustomSmsPrefix(it) },
                        onTogglePackage = { pkg, enabled -> viewModel.togglePackage(pkg, enabled) },
                        onUpdateAiParameters = { temp, topK, maxTokens, prompt ->
                            viewModel.updateAiParameters(temp, topK, maxTokens, prompt)
                        },
                        onRunTest = { title, text -> viewModel.runTestInference(title, text) },
                        onSendTestSms = { msg -> viewModel.sendTestSms(msg) },
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }
            }
        }
    }

    private fun checkSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        val permissions = mutableListOf(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun openNotificationAccessSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        ComponentName(this@MainActivity, BankNotificationService::class.java).flattenToString()
                    )
                }
                startActivity(intent)
            } else {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun requestBatteryOptimizationExemption() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(fallback)
        }
    }
}
