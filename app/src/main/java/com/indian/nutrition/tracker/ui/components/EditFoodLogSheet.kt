package com.indian.nutrition.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.MealType
import kotlin.math.roundToInt

/** Edit an existing food log without losing its original food identity. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFoodLogSheet(
    log: DailyLog,
    onDismiss: () -> Unit,
    onSave: (DailyLog) -> Unit,
) {
    var gramsInput by rememberSaveable(log.id) { mutableStateOf(log.servingGrams.toString()) }
    var mealType by rememberSaveable(log.id) { mutableStateOf(log.mealType) }
    val grams = gramsInput.toIntOrNull()?.coerceIn(1, 100_000) ?: 0
    val valid = grams > 0
    val factor = if (log.servingGrams > 0) grams.toDouble() / log.servingGrams else 1.0
    val calories = (log.calories * factor).roundToInt()
    val protein = round1(log.protein * factor)
    val carbs = round1(log.carbs * factor)
    val fat = round1(log.fat * factor)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = Modifier.testTag("edit-food-log-sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Edit food log", style = MaterialTheme.typography.titleMedium)
                    Text(log.foodName, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = gramsInput,
                onValueChange = { gramsInput = it.filter(Char::isDigit).take(6) },
                label = { Text("Serving (g)") },
                supportingText = { Text("Macros scale from the original logged serving.") },
                isError = !valid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("edit-food-serving-input"),
            )

            Text("Meal", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MealType.entries.forEach { type ->
                    FilterChip(
                        selected = mealType == type,
                        onClick = { mealType = type },
                        label = { Text(type.displayName) },
                    )
                }
            }

            Text(
                "$calories kcal · ${protein}g protein · ${carbs}g carbs · ${fat}g fat",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    onSave(
                        log.copy(
                            servingGrams = grams,
                            calories = calories,
                            protein = protein,
                            carbs = carbs,
                            fat = fat,
                            mealType = mealType,
                        ),
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().testTag("save-edited-food-log-btn"),
            ) { Text("Save changes") }
        }
    }
}

private fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0
