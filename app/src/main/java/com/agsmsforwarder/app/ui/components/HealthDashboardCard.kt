package com.agsmsforwarder.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agsmsforwarder.app.ai.ModelLoadState
import com.agsmsforwarder.app.ui.theme.StatusErrorBg
import com.agsmsforwarder.app.ui.theme.StatusErrorFg
import com.agsmsforwarder.app.ui.theme.StatusInfoBg
import com.agsmsforwarder.app.ui.theme.StatusInfoFg
import com.agsmsforwarder.app.ui.theme.StatusSuccessBg
import com.agsmsforwarder.app.ui.theme.StatusSuccessFg
import com.agsmsforwarder.app.ui.theme.StatusWarningFg
import com.agsmsforwarder.app.ui.theme.VaultOutlineVariant
import com.agsmsforwarder.app.ui.theme.VaultPrimary
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainer
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainerHigh
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainerHighest
import com.agsmsforwarder.app.ui.theme.VaultTertiary

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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header with NPU Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM HEALTH",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // NPU status badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(VaultSurfaceContainerHigh)
                    .border(1.dp, VaultOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(9999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = VaultTertiary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (modelLoadState is ModelLoadState.Loaded) "AI Edge Engine" else "Regex Engine",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (lastLatencyMs > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VaultSurfaceContainer)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "~${lastLatencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Horizontal scrolling status pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // SMS Permission Pill
            StatusCardPill(
                icon = Icons.Default.Sms,
                title = "SMS Perms",
                statusLabel = if (hasSmsPermission) "Granted" else "Action Needed",
                isOk = hasSmsPermission,
                onClick = onGrantSmsPermission
            )

            // Notification Listener Pill
            StatusCardPill(
                icon = Icons.Default.NotificationsActive,
                title = "Listener",
                statusLabel = if (hasNotificationAccess) "Active" else "Disabled",
                isOk = hasNotificationAccess,
                onClick = onOpenNotificationAccessSettings
            )

            // Battery Optimization Pill
            StatusCardPill(
                icon = Icons.Default.BatteryAlert,
                title = "Battery Opt",
                statusLabel = if (isIgnoringBatteryOptimizations) "Unrestricted" else "Action Needed",
                isOk = isIgnoringBatteryOptimizations,
                onClick = onRequestBatteryExemption
            )
        }

        // AI Engine Container Card
        AiEngineStatusCard(
            modelLoadState = modelLoadState,
            lastLatencyMs = lastLatencyMs,
            onSelectModelFile = onSelectModelFile,
            onOpenModelDownloader = onOpenModelDownloader,
            onOpenAiSettings = onOpenAiSettings
        )
    }
}

@Composable
private fun StatusCardPill(
    icon: ImageVector,
    title: String,
    statusLabel: String,
    isOk: Boolean,
    onClick: () -> Unit
) {
    val iconBg = if (isOk) StatusSuccessBg else StatusErrorBg
    val iconFg = if (isOk) StatusSuccessFg else StatusErrorFg

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VaultSurfaceContainer)
            .border(1.dp, VaultOutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconFg,
                modifier = Modifier.size(18.dp)
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = iconFg,
                fontWeight = FontWeight.Medium
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VaultOutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StatusInfoBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "AI Engine",
                            tint = VaultPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Google AI Edge (MediaPipe)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "On-Device SLM (Gemma / LiteRT)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Clean Config Icon Button
                IconButton(
                    onClick = onOpenAiSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaultSurfaceContainerHigh)
                        .border(1.dp, VaultOutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "AI Configuration",
                        tint = VaultPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (modelLoadState) {
                is ModelLoadState.Loaded -> {
                    val fileName = modelLoadState.modelPath.substringAfterLast("/")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(VaultTertiary)
                        )
                        Text(
                            text = "Model: $fileName (${modelLoadState.loadDurationMs}ms load)",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                is ModelLoadState.Loading -> {
                    Text(
                        text = "Loading model weights into NPU/CPU memory...",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusInfoFg
                    )
                }
                is ModelLoadState.Error -> {
                    Text(
                        text = "Model Error: ${modelLoadState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusErrorFg
                    )
                    Text(
                        text = "Fallback active: Fast Regex extractor will process alerts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is ModelLoadState.Uninitialized -> {
                    Text(
                        text = "No on-device model loaded. Operating in Regex Fallback Mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusWarningFg
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onSelectModelFile,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, VaultOutlineVariant.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pick File", style = MaterialTheme.typography.labelSmall)
                }

                FilledTonalButton(
                    onClick = onOpenModelDownloader,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = VaultSurfaceContainerHighest,
                        contentColor = VaultPrimary
                    )
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download Model", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
