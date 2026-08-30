package com.agsmsforwarder.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agsmsforwarder.app.data.preferences.AppPreferences
import com.agsmsforwarder.app.ui.theme.VaultSurfaceContainer
import kotlin.math.roundToInt

@Composable
fun AdvancedAiDialog(
    initialTemperature: Float,
    initialTopK: Int,
    initialMaxTokens: Int,
    initialSystemPrompt: String,
    onSave: (temperature: Float, topK: Int, maxTokens: Int, customPrompt: String) -> Unit,
    onDismiss: () -> Unit
) {
    var temperature by remember { mutableFloatStateOf(initialTemperature) }
    var topK by remember { mutableIntStateOf(initialTopK) }
    var maxTokens by remember { mutableIntStateOf(initialMaxTokens) }
    var systemPrompt by remember { mutableStateOf(initialSystemPrompt) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurfaceContainer,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Advanced AI Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Temperature Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Temperature: ${String.format("%.2f", temperature)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Lower = deterministic", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0.0f..1.0f,
                    steps = 19
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Top-K Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Top-K: $topK", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = topK.toFloat(),
                    onValueChange = { topK = it.roundToInt() },
                    valueRange = 1f..100f,
                    steps = 98
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Max Tokens Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Max Tokens: $maxTokens", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { maxTokens = it.roundToInt() },
                    valueRange = 256f..2048f,
                    steps = 13
                )

                Spacer(modifier = Modifier.height(12.dp))

                // System Prompt Editor
                Text(
                    text = "System / Few-shot Prompt Template:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        temperature = 0.2f
                        topK = 40
                        maxTokens = 128
                        systemPrompt = AppPreferences.DEFAULT_SYSTEM_PROMPT
                    }
                ) {
                    Text("Reset to Defaults")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(temperature, topK, maxTokens, systemPrompt)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
