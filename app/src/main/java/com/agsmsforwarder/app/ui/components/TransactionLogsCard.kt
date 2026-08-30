package com.agsmsforwarder.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agsmsforwarder.app.data.db.TransactionLogEntity
import com.agsmsforwarder.app.data.model.SmsDeliveryStatus
import com.agsmsforwarder.app.ui.theme.StatusErrorBg
import com.agsmsforwarder.app.ui.theme.StatusErrorFg
import com.agsmsforwarder.app.ui.theme.StatusInfoBg
import com.agsmsforwarder.app.ui.theme.StatusInfoFg
import com.agsmsforwarder.app.ui.theme.StatusSuccessBg
import com.agsmsforwarder.app.ui.theme.StatusSuccessFg
import com.agsmsforwarder.app.ui.theme.StatusWarningBg
import com.agsmsforwarder.app.ui.theme.StatusWarningFg
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Alerts (${logs.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (logs.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Logs",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No bank notifications processed yet.\nIncoming alerts will appear here in real-time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    logs.take(20).forEach { log ->
                        LogItemView(log = log)
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Transaction History") },
            text = { Text("Are you sure you want to clear all processed transaction logs from the local database?") },
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
private fun LogItemView(log: TransactionLogEntity) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: App name, Time, SMS status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.appName.ifBlank { log.packageName.substringAfterLast(".") },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeliveryStatusBadge(status = log.smsDeliveryStatus)
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parsed Message Body
            Text(
                text = log.parsedResult,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (log.parsedResult == "SKIP") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            // Badges row: AI vs Regex, Latency
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (log.isAiParsed) {
                    MiniBadge(text = "AI Formatted", bg = StatusSuccessBg, fg = StatusSuccessFg)
                } else {
                    MiniBadge(text = "Regex", bg = StatusWarningBg, fg = StatusWarningFg)
                }
                if (log.latencyMs > 0) {
                    MiniBadge(text = "${log.latencyMs}ms", bg = StatusInfoBg, fg = StatusInfoFg)
                }
            }

            // Expanded details: Raw text & Recipient
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Raw Notification:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${log.rawNotificationTitle}: ${log.rawNotificationText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (log.smsRecipient.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Forwarded To: ${log.smsRecipient}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryStatusBadge(status: SmsDeliveryStatus) {
    val (bg, fg) = when (status) {
        SmsDeliveryStatus.DELIVERED, SmsDeliveryStatus.SENT, SmsDeliveryStatus.SIMULATED_TEST -> StatusSuccessBg to StatusSuccessFg
        SmsDeliveryStatus.PENDING -> StatusInfoBg to StatusInfoFg
        SmsDeliveryStatus.SKIPPED_NOT_TRANSACTION, SmsDeliveryStatus.SKIPPED_NO_RECIPIENT -> StatusWarningBg to StatusWarningFg
        SmsDeliveryStatus.FAILED_GENERIC, SmsDeliveryStatus.FAILED_NO_SERVICE,
        SmsDeliveryStatus.FAILED_NULL_PDU, SmsDeliveryStatus.FAILED_RADIO_OFF -> StatusErrorBg to StatusErrorFg
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MiniBadge(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Medium
        )
    }
}
