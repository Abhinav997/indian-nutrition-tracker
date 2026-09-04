package com.indian.nutrition.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Labeled calories/protein progress card (web parity, Material 3). */
@Composable
fun MacroProgressBar(
    label: String,
    value: Double,
    target: Double,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val progress = if (target > 0) (value / target).toFloat().coerceIn(0f, 1f) else 0f
    val percent = if (target > 0) ((value / target) * 100).roundToInt() else 0
    val remaining = target - value

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "${value.toInt()} / ${target.toInt()} $unit",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("$percent% consumed", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = if (remaining >= 0) "${remaining.toInt()} $unit left" else "+${(-remaining).toInt()} $unit over target",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining >= 0) accent else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
