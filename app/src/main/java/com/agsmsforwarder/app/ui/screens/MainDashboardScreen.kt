package com.agsmsforwarder.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agsmsforwarder.app.ai.DownloadState
import com.agsmsforwarder.app.ai.ModelLoadState
import com.agsmsforwarder.app.data.db.TransactionLogEntity
import com.agsmsforwarder.app.data.model.FormattedTransaction
import com.agsmsforwarder.app.data.preferences.AppPreferences
import com.agsmsforwarder.app.ui.components.AdvancedAiDialog
import com.agsmsforwarder.app.ui.components.BankSelectorDialog
import com.agsmsforwarder.app.ui.components.HealthDashboardCard
import com.agsmsforwarder.app.ui.components.LiveTestPlayground
import com.agsmsforwarder.app.ui.components.ModelDownloaderDialog
import com.agsmsforwarder.app.ui.components.TransactionLogsCard
import com.agsmsforwarder.app.ui.theme.VaultBackground
import com.agsmsforwarder.app.ui.theme.VaultOnPrimary
import com.agsmsforwarder.app.ui.theme.VaultOutlineVariant
import com.agsmsforwarder.app.ui.theme.VaultPrimary
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainer
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainerHigh
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainerHighest
import com.agsmsforwarder.app.ui.theme.VaultTertiary

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.agsmsforwarder.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    preferences: AppPreferences,
    logs: List<TransactionLogEntity>,
    modelLoadState: ModelLoadState,
    downloadState: DownloadState,
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
    onStartDownload: (url: String, fileName: String, token: String?) -> Unit,
    onCancelDownload: () -> Unit,
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
    var showModelDownloaderDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = VaultBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vaultpulse_logo),
                            contentDescription = "VaultPulse Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "VaultPulse",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (preferences.isServiceEnabled) VaultTertiary else MaterialTheme.colorScheme.error)
                                )
                                Text(
                                    text = if (preferences.isServiceEnabled) "SERVICE RUNNING" else "SERVICE PAUSED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (preferences.isServiceEnabled) VaultTertiary else MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    Switch(
                        checked = preferences.isServiceEnabled,
                        onCheckedChange = onToggleService,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VaultOnPrimary,
                            checkedTrackColor = VaultPrimary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = VaultSurfaceContainerHighest
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VaultBackground
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
            // 1. System Health Section
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
                onOpenModelDownloader = { showModelDownloaderDialog = true },
                onOpenAiSettings = { showAiConfigDialog = true }
            )

            // 2. Routing Rules Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VaultOutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = VaultPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ROUTING RULES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }

                    // Target Bank App selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Target Bank Apps",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(VaultSurfaceContainerHighest)
                                .border(1.dp, VaultOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .clickable { showBankDialog = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(VaultPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = VaultPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (preferences.enabledPackages.isEmpty()) "No apps selected"
                                    else "${preferences.enabledPackages.size} Banking App(s) Monitored",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Manage",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Forward Alert To
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Forward Alert To",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = preferences.destinationPhoneNumber,
                            onValueChange = onUpdateDestinationNumber,
                            placeholder = { Text("+1 (555) 019-2834") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = VaultPrimary, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = VaultSurfaceContainerHighest,
                                unfocusedContainerColor = VaultSurfaceContainerHighest,
                                focusedBorderColor = VaultPrimary,
                                unfocusedBorderColor = VaultOutlineVariant.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // SMS Tag / Prefix
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SMS Tag / Prefix",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = preferences.customSmsPrefix,
                            onValueChange = onUpdateCustomPrefix,
                            placeholder = { Text("[ALERT]") },
                            leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = VaultPrimary, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = VaultSurfaceContainerHighest,
                                unfocusedContainerColor = VaultSurfaceContainerHighest,
                                focusedBorderColor = VaultPrimary,
                                unfocusedBorderColor = VaultOutlineVariant.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Smart Summarize Toggle (Stitch layout)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Summarize",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Uses local AI to condense and clean alerts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.useAiFormatting,
                            onCheckedChange = onToggleAiFormatting,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VaultOnPrimary,
                                checkedTrackColor = VaultPrimary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = VaultSurfaceContainerHighest
                            )
                        )
                    }
                }
            }

            // 3. Interactive AI Playground
            LiveTestPlayground(
                destinationNumber = preferences.destinationPhoneNumber,
                isTesting = isTesting,
                testResult = testResult,
                onRunTest = onRunTest,
                onSendTestSms = onSendTestSms
            )

            // 4. Recent Activity Logs
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

    if (showModelDownloaderDialog) {
        ModelDownloaderDialog(
            downloadState = downloadState,
            onStartDownload = onStartDownload,
            onCancelDownload = onCancelDownload,
            onDismiss = { showModelDownloaderDialog = false }
        )
    }
}
