package com.indian.nutrition.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.indian.nutrition.tracker.domain.model.WaterLog
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Water & hydration card: progress, glasses, quick-add presets, history. */
@Composable
fun WaterCard(
    logs: List<WaterLog>,
    targetMl: Int,
    onAddWater: (Int) -> Unit,
    onDeleteWater: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = logs.sumOf { it.amountMl }
    val progress = if (targetMl > 0) (total.toFloat() / targetMl).coerceIn(0f, 1f) else 0f
    val glasses = total / 250.0
    val targetGlasses = targetMl / 250
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showCustom by rememberSaveable { mutableStateOf(false) }
    var customAmount by rememberSaveable { mutableStateOf("250") }

    Card(modifier = modifier.fillMaxWidth().testTag("water-tracker-widget")) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text("Water & Hydration", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "$total / $targetMl ml",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = { showHistory = !showHistory }, modifier = Modifier.testTag("toggle-water-history-btn")) {
                    Text(if (showHistory) "Hide Logs" else "Logs (${logs.size})")
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${(progress * 100).roundToInt()}% of daily goal · $glasses of $targetGlasses glasses",
                style = MaterialTheme.typography.bodySmall,
            )

            // Glass grid
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items((1..maxOf(targetGlasses, ceil(glasses).toInt())).toList()) { idx ->
                    val filled = idx <= glasses.toInt()
                    Card(
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.padding(0.dp),
                    ) {
                        Text(
                            text = if (filled) "✓" else "$idx",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (filled) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { onAddWater(250) }, modifier = Modifier.testTag("water-add-250-btn")) { Text("+250") }
                FilledTonalButton(onClick = { onAddWater(500) }, modifier = Modifier.testTag("water-add-500-btn")) { Text("+500") }
                FilledTonalButton(onClick = { onAddWater(750) }, modifier = Modifier.testTag("water-add-750-btn")) { Text("+750") }
                OutlinedButton(onClick = { showCustom = !showCustom }, modifier = Modifier.testTag("water-add-custom-toggle-btn")) { Text("Custom") }
            }

            if (showCustom) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it.filter { c -> c.isDigit() } },
                        label = { Text("ml") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(
                        onClick = {
                            customAmount.toIntOrNull()?.let { if (it > 0) onAddWater(it) }
                            customAmount = "250"
                            showCustom = false
                        },
                    ) { Text("Add") }
                }
            }

            if (showHistory) {
                if (logs.isEmpty()) {
                    Text("No water logged yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    logs.forEach { log ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WaterDrop, contentDescription = null)
                                Text(
                                    text = "${log.amountMl} ml" + (log.time?.let { " · $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                            IconButton(onClick = { onDeleteWater(log.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete water entry")
                            }
                        }
                    }
                }
            }
        }
    }
}
