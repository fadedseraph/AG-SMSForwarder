package com.agsmsforwarder.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agsmsforwarder.app.ai.ModelLoadState
import com.agsmsforwarder.app.data.db.TransactionLogEntity
import com.agsmsforwarder.app.data.model.FormattedTransaction
import com.agsmsforwarder.app.data.preferences.AppPreferences
import com.agsmsforwarder.app.ui.components.AdvancedAiDialog
import com.agsmsforwarder.app.ui.components.BankSelectorDialog
import com.agsmsforwarder.app.ui.components.HealthDashboardCard
import com.agsmsforwarder.app.ui.components.LiveTestPlayground
import com.agsmsforwarder.app.ui.components.TransactionLogsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    preferences: AppPreferences,
    logs: List<TransactionLogEntity>,
    modelLoadState: ModelLoadState,
    lastLatencyMs: Long,
    hasSmsPermission: Boolean,
    hasNotificationAccess: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    isTesting: Boolean,
    testResult: FormattedTransaction?,
    onGrantSmsPermission: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onSelectModelFile: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onUpdateDestinationNumber: (String) -> Unit,
    onToggleAiFormatting: (Boolean) -> Unit,
    onUpdateCustomPrefix: (String) -> Unit,
    onTogglePackage: (String, Boolean) -> Unit,
    onUpdateAiParameters: (temperature: Float, topK: Int, maxTokens: Int, customPrompt: String) -> Unit,
    onRunTest: (title: String, text: String) -> Unit,
    onSendTestSms: (message: String) -> Unit,
    onClearLogs: () -> Unit
) {
    var showBankDialog by remember { mutableStateOf(false) }
    var showAiConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Bank SMS Forwarder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (preferences.isServiceEnabled) "Active • Monitoring Alerts" else "Paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (preferences.isServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Switch(
                            checked = preferences.isServiceEnabled,
                            onCheckedChange = onToggleService
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Health & Permission Dashboard
            HealthDashboardCard(
                hasSmsPermission = hasSmsPermission,
                hasNotificationAccess = hasNotificationAccess,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                modelLoadState = modelLoadState,
                lastLatencyMs = lastLatencyMs,
                onGrantSmsPermission = onGrantSmsPermission,
                onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                onRequestBatteryExemption = onRequestBatteryExemption,
                onSelectModelFile = onSelectModelFile,
                onOpenAiSettings = { showAiConfigDialog = true }
            )

            // 2. Target Banking Apps Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monitored Banking Apps",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = { showBankDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Manage (${preferences.enabledPackages.size})")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notifications from ${preferences.enabledPackages.size} selected app(s) will be intercepted, cleaned, and forwarded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. SMS Destination & Formatting Configuration
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SMS Dispatch Configuration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Destination Phone Number
                    OutlinedTextField(
                        value = preferences.destinationPhoneNumber,
                        onValueChange = onUpdateDestinationNumber,
                        label = { Text("Destination Phone Number(s)") },
                        placeholder = { Text("+1 (555) 019-2834") },
                        supportingText = { Text("Separate multiple numbers with commas.") },
                        leadingIcon = { Icon(Icons.Default.Send, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom SMS Prefix
                    OutlinedTextField(
                        value = preferences.customSmsPrefix,
                        onValueChange = onUpdateCustomPrefix,
                        label = { Text("SMS Tag / Prefix") },
                        placeholder = { Text("[ALERT]") },
                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // AI Toggle vs Raw
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use On-Device AI Formatter",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (preferences.useAiFormatting) "Cleans noise, normalizes merchant & amounts." else "Forwards raw notification text without AI parsing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.useAiFormatting,
                            onCheckedChange = onToggleAiFormatting
                        )
                    }
                }
            }

            // 4. Live Test Playground
            LiveTestPlayground(
                destinationNumber = preferences.destinationPhoneNumber,
                isTesting = isTesting,
                testResult = testResult,
                onRunTest = onRunTest,
                onSendTestSms = onSendTestSms
            )

            // 5. Transaction Logs History
            TransactionLogsCard(
                logs = logs,
                onClearLogs = onClearLogs
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialogs
    if (showBankDialog) {
        BankSelectorDialog(
            enabledPackages = preferences.enabledPackages,
            onTogglePackage = onTogglePackage,
            onDismiss = { showBankDialog = false }
        )
    }

    if (showAiConfigDialog) {
        AdvancedAiDialog(
            initialTemperature = preferences.aiTemperature,
            initialTopK = preferences.aiTopK,
            initialMaxTokens = preferences.aiMaxTokens,
            initialSystemPrompt = preferences.customSystemPrompt,
            onSave = onUpdateAiParameters,
            onDismiss = { showAiConfigDialog = false }
        )
    }
}
