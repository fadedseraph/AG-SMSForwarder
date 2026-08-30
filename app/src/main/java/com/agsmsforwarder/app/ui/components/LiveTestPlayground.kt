package com.agsmsforwarder.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.agsmsforwarder.app.data.model.FormattedTransaction
import com.agsmsforwarder.app.ui.theme.StatusInfoBg
import com.agsmsforwarder.app.ui.theme.StatusInfoFg
import com.agsmsforwarder.app.ui.theme.StatusSuccessBg
import com.agsmsforwarder.app.ui.theme.StatusSuccessFg
import com.agsmsforwarder.app.ui.theme.StatusWarningBg
import com.agsmsforwarder.app.ui.theme.StatusWarningFg
import com.agsmsforwarder.app.ui.theme.VaultOnPrimary
import com.agsmsforwarder.app.ui.theme.VaultOutlineVariant
import com.agsmsforwarder.app.ui.theme.VaultPrimary
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainer
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainerHighest
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainerLowest
import com.agsmsforwarder.app.ui.theme.VaultTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveTestPlayground(
    destinationNumber: String,
    isTesting: Boolean,
    testResult: FormattedTransaction?,
    onRunTest: (title: String, text: String) -> Unit,
    onSendTestSms: (message: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var testTitle by remember { mutableStateOf("Chase Mobile") }
    var testText by remember { mutableStateOf("Debit ending in 4102 charged $42.50 at Trader Joe's.") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, VaultOutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = VaultPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI PLAYGROUND",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Presets
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PresetChip(label = "Chase $42.50") {
                    testTitle = "Chase Mobile"
                    testText = "Debit ending in 4102 charged $42.50 at Trader Joe's."
                }
                PresetChip(label = "BofA $120.00") {
                    testTitle = "Bank of America"
                    testText = "Alert: $120.00 debit card purchase at SHELL OIL 5744."
                }
                PresetChip(label = "Wells Fargo OTP") {
                    testTitle = "Wells Fargo"
                    testText = "Your one-time verification code is 849201 for login."
                }
                PresetChip(label = "Amex $89.99") {
                    testTitle = "American Express"
                    testText = "Purchase authorized: $89.99 at AMAZON.COM*1A2B on card 4091."
                }
            }

            // Raw Input Textarea
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceContainerLowest)
                    .border(1.dp, VaultOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    OutlinedTextField(
                        value = testTitle,
                        onValueChange = { testTitle = it },
                        placeholder = { Text("App Name (e.g. Chase Mobile)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        placeholder = { Text("Enter sample bank notification alert...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Raw Input",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            // Run AI Button
            Button(
                onClick = { onRunTest(testTitle, testText) },
                enabled = !isTesting && (testTitle.isNotBlank() || testText.isNotBlank()),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultPrimary,
                    contentColor = VaultOnPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = VaultOnPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inferring on-device...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.ModelTraining, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run On-Device AI", fontWeight = FontWeight.Bold)
                }
            }

            // Output Card
            AnimatedVisibility(
                visible = testResult != null,
                enter = fadeIn() + expandVertically()
            ) {
                if (testResult != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(VaultSurfaceContainerHighest)
                                .border(1.dp, VaultOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Emerald left accent pill
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(VaultTertiary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = testResult.formattedMessage,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (testResult.formattedMessage == "SKIP") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = VaultTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (testResult.isAiGenerated) "AI Parsed in ${testResult.latencyMs}ms" else "Regex Parsed in ${testResult.latencyMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultTertiary
                                    )
                                }
                            }
                        }

                        if (testResult.formattedMessage != "SKIP" && destinationNumber.isNotBlank()) {
                            OutlinedButton(
                                onClick = { onSendTestSms(testResult.formattedMessage) },
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(VaultPrimary)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = VaultPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Test SMS to $destinationNumber", color = VaultPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        shape = RoundedCornerShape(8.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = VaultSurfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultOutlineVariant.copy(alpha = 0.3f))
    )
}
