package com.indian.nutrition.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indian.nutrition.tracker.data.repository.FoodRepository
import com.indian.nutrition.tracker.domain.FoodLookup
import com.indian.nutrition.tracker.domain.model.Food

/**
 * Lightweight curated-database picker used by the Home screen until Phase 4
 * replaces it with the full search screen (NIN + packaged + Open Food Facts).
 */
@Composable
fun FoodPickerDialog(
    foodRepository: FoodRepository,
    onSelect: (Food) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val master = remember { foodRepository.ensureLoaded() }
    val results = remember(master, query) { FoodLookup.search(master, query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick add food") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search Indian foods…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn {
                    items(results, key = { it.id }) { food ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(food) }
                                .padding(vertical = 8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(food.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${food.kcalPer100g.toInt()} kcal · ${food.proteinPer100g.toInt()}g P per 100g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = food.source.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
