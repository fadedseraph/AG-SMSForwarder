package com.agsmsforwarder.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agsmsforwarder.app.data.db.TransactionLogEntity
import com.agsmsforwarder.app.data.model.SmsDeliveryStatus
import com.agsmsforwarder.app.ui.theme.StatusErrorFg
import com.agsmsforwarder.app.ui.theme.StatusSuccessFg
import com.agsmsforwarder.app.ui.theme.StatusWarningFg
import com.agsmsforwarder.app.ui.theme.VaultOutlineVariant
import com.agsmsforwarder.app.ui.theme.VaultPrimary
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainer
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainerHigh
import com.agsmsforwarder.app.ui.theme.VaultTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionLogsCard(
    logs: List<TransactionLogEntity>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT ACTIVITY",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (logs.isNotEmpty()) {
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Logs",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (logs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VaultOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No bank notifications processed yet.\nIncoming alerts will appear here in real-time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                logs.take(20).forEach { log ->
                    VaultActivityLogItem(log = log)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Activity History") },
            text = { Text("Are you sure you want to clear all processed transaction records from local storage?") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearLogs()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VaultActivityLogItem(log: TransactionLogEntity) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }

    val dotColor = when (log.smsDeliveryStatus) {
        SmsDeliveryStatus.SENT, SmsDeliveryStatus.DELIVERED, SmsDeliveryStatus.SIMULATED_TEST -> VaultTertiary
        SmsDeliveryStatus.PENDING -> VaultPrimary
        SmsDeliveryStatus.SKIPPED_NOT_TRANSACTION, SmsDeliveryStatus.SKIPPED_NO_RECIPIENT -> StatusWarningFg
        else -> StatusErrorFg
    }

    val statusIcon = when (log.smsDeliveryStatus) {
        SmsDeliveryStatus.SENT, SmsDeliveryStatus.DELIVERED, SmsDeliveryStatus.SIMULATED_TEST -> Icons.Default.CheckCircle
        SmsDeliveryStatus.PENDING -> Icons.Default.HourglassEmpty
        else -> Icons.Default.Error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VaultOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pulsing dot indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                // Main Info: Parsed Result + Time
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.parsedResult.ifBlank { log.appName },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${log.appName} • ${if (log.isAiParsed) "AI" else "Regex"} (~${log.latencyMs}ms)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Icon
                Icon(
                    imageVector = statusIcon,
                    contentDescription = log.smsDeliveryStatus.label,
                    tint = dotColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expanded Raw Notification Details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp, start = 20.dp)) {
                    HorizontalDivider(color = VaultOutlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Raw Alert:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${log.rawNotificationTitle}: ${log.rawNotificationText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (log.smsRecipient.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Forwarded To: ${log.smsRecipient} (${log.smsDeliveryStatus.label})",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultTertiary
                        )
                    }
                }
            }
        }
    }
}
