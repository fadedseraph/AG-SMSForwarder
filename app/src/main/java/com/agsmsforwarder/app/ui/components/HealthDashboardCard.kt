package com.agsmsforwarder.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agsmsforwarder.app.ai.ModelLoadState
import com.agsmsforwarder.app.ui.theme.StatusErrorBg
import com.agsmsforwarder.app.ui.theme.StatusErrorFg
import com.agsmsforwarder.app.ui.theme.StatusInfoBg
import com.agsmsforwarder.app.ui.theme.StatusInfoFg
import com.agsmsforwarder.app.ui.theme.StatusSuccessBg
import com.agsmsforwarder.app.ui.theme.StatusSuccessFg
import com.agsmsforwarder.app.ui.theme.StatusWarningBg
import com.agsmsforwarder.app.ui.theme.StatusWarningFg

import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen

@Composable
fun HealthDashboardCard(
    hasSmsPermission: Boolean,
    hasNotificationAccess: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    modelLoadState: ModelLoadState,
    lastLatencyMs: Long,
    onGrantSmsPermission: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onSelectModelFile: () -> Unit,
    onOpenModelDownloader: () -> Unit,
    onOpenAiSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
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
                Text(
                    text = "System & AI Health",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val allGood = hasSmsPermission && hasNotificationAccess && isIgnoringBatteryOptimizations
                StatusPill(
                    text = if (allGood) "System Ready" else "Attention Needed",
                    isPositive = allGood
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permission & Service Rows
            HealthItemRow(
                icon = Icons.Default.Sms,
                title = "SMS Permission",
                statusText = if (hasSmsPermission) "Granted" else "Missing",
                isOk = hasSmsPermission,
                actionLabel = if (!hasSmsPermission) "Grant" else null,
                onAction = onGrantSmsPermission
            )

            Spacer(modifier = Modifier.height(10.dp))

            HealthItemRow(
                icon = Icons.Default.NotificationsActive,
                title = "Notification Listener",
                statusText = if (hasNotificationAccess) "Active" else "Disabled in Settings",
                isOk = hasNotificationAccess,
                actionLabel = if (!hasNotificationAccess) "Enable" else null,
                onAction = onOpenNotificationAccessSettings
            )

            Spacer(modifier = Modifier.height(10.dp))

            HealthItemRow(
                icon = Icons.Default.BatteryAlert,
                title = "Battery Optimization",
                statusText = if (isIgnoringBatteryOptimizations) "Unrestricted" else "Restricted",
                isOk = isIgnoringBatteryOptimizations,
                actionLabel = if (!isIgnoringBatteryOptimizations) "Allow" else null,
                onAction = onRequestBatteryExemption
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AI Model Status Card
            AiEngineStatusCard(
                modelLoadState = modelLoadState,
                lastLatencyMs = lastLatencyMs,
                onSelectModelFile = onSelectModelFile,
                onOpenModelDownloader = onOpenModelDownloader,
                onOpenAiSettings = onOpenAiSettings
            )
        }
    }
}

@Composable
private fun HealthItemRow(
    icon: ImageVector,
    title: String,
    statusText: String,
    isOk: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOk) StatusSuccessFg else StatusErrorFg
                )
            }
        }

        if (actionLabel != null) {
            FilledTonalButton(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Active",
                tint = StatusSuccessFg,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AiEngineStatusCard(
    modelLoadState: ModelLoadState,
    lastLatencyMs: Long,
    onSelectModelFile: () -> Unit,
    onOpenModelDownloader: () -> Unit,
    onOpenAiSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "AI Engine",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google AI Edge (MediaPipe)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onOpenAiSettings,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Config", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (modelLoadState) {
                is ModelLoadState.Loaded -> {
                    val fileName = modelLoadState.modelPath.substringAfterLast("/")
                    Text(
                        text = "Model: $fileName (Loaded in ${modelLoadState.loadDurationMs}ms)",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusSuccessFg,
                        fontWeight = FontWeight.Medium
                    )
                    if (lastLatencyMs > 0) {
                        Text(
                            text = "Last Inference Latency: ${lastLatencyMs}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is ModelLoadState.Loading -> {
                    Text(
                        text = "Loading on-device model weights into memory...",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusInfoFg
                    )
                }
                is ModelLoadState.Error -> {
                    Text(
                        text = "Error: ${modelLoadState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusErrorFg
                    )
                    Text(
                        text = "Fallback active: High-precision Regex parsing will be used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is ModelLoadState.Uninitialized -> {
                    Text(
                        text = "No .task / .bin model file loaded. Operating in Regex Fallback Mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusWarningFg
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onSelectModelFile,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pick File", style = MaterialTheme.typography.labelSmall)
                }

                FilledTonalButton(
                    onClick = onOpenModelDownloader,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download Model", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, isPositive: Boolean) {
    val bg = if (isPositive) StatusSuccessBg else StatusWarningBg
    val fg = if (isPositive) StatusSuccessFg else StatusWarningFg
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}
