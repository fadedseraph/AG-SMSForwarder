package com.agsmsforwarder.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.agsmsforwarder.app.data.model.FormattedTransaction
import com.agsmsforwarder.app.ui.theme.StatusInfoBg
import com.agsmsforwarder.app.ui.theme.StatusInfoFg
import com.agsmsforwarder.app.ui.theme.StatusSuccessBg
import com.agsmsforwarder.app.ui.theme.StatusSuccessFg
import com.agsmsforwarder.app.ui.theme.StatusWarningBg
import com.agsmsforwarder.app.ui.theme.StatusWarningFg

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
    var testText by remember { mutableStateOf("You spent $45.20 at TRADER JOE'S #123 on 08/29.") }

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Test Playground",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simulate an incoming notification to test on-device parsing and SMS formatting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sample Quick Presets
            Text(
                text = "Preset Samples:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = {
                        testTitle = "Chase Mobile"
                        testText = "You spent $45.20 at TRADER JOE'S #123 on 08/29."
                    },
                    label = { Text("Chase $45.20") }
                )
                AssistChip(
                    onClick = {
                        testTitle = "Bank of America"
                        testText = "Alert: $120.00 debit card purchase at SHELL OIL 5744."
                    },
                    label = { Text("BofA $120.00") }
                )
                AssistChip(
                    onClick = {
                        testTitle = "Wells Fargo"
                        testText = "Your one-time verification code is 849201 for login."
                    },
                    label = { Text("Wells Fargo OTP") }
                )
                AssistChip(
                    onClick = {
                        testTitle = "Capital One"
                        testText = "Purchase authorized: $89.99 at AMAZON.COM*1A2B on card 4091."
                    },
                    label = { Text("CapOne $89.99") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = testTitle,
                onValueChange = { testTitle = it },
                label = { Text("Notification Title / App") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                label = { Text("Notification Body Text") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onRunTest(testTitle, testText) },
                    enabled = !isTesting && (testTitle.isNotBlank() || testText.isNotBlank()),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Inferring...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test AI Inference")
                    }
                }
            }

            // Results Display Card
            if (testResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parsed Result:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (testResult.isAiGenerated) {
                                    BadgePill(text = "AI Formatted", bg = StatusSuccessBg, fg = StatusSuccessFg)
                                } else {
                                    BadgePill(text = "Regex Fallback", bg = StatusWarningBg, fg = StatusWarningFg)
                                }
                                if (testResult.latencyMs > 0) {
                                    BadgePill(text = "${testResult.latencyMs}ms", bg = StatusInfoBg, fg = StatusInfoFg)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = testResult.formattedMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (testResult.formattedMessage == "SKIP") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )

                        if (testResult.formattedMessage != "SKIP" && destinationNumber.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { onSendTestSms(testResult.formattedMessage) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Test SMS to $destinationNumber")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}
