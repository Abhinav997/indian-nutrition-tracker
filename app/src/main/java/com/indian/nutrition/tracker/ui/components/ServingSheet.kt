package com.indian.nutrition.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.MealType
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServingSheet(
    food: Food,
    onDismiss: () -> Unit,
    onSave: (servingGrams: Int, quantity: Double, mealType: MealType) -> Unit,
    initialMeal: MealType = MealType.LUNCH,
) {
    var grams by rememberSaveable(food.id) { mutableStateOf("${food.typicalServingGrams ?: 100}") }
    var multiplier by rememberSaveable(food.id) { mutableStateOf(1.0) }
    var mealType by rememberSaveable(food.id) { mutableStateOf(initialMeal) }

    val effectiveGrams = max(1, (grams.toDoubleOrNull() ?: 0.0).times(multiplier).roundToInt())
    val kcal = (food.kcalPer100g * effectiveGrams / 100).roundToInt()
    val protein = food.proteinPer100g * effectiveGrams / 100
    val carbs = food.carbsPer100g * effectiveGrams / 100
    val fat = food.fatPer100g * effectiveGrams / 100

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(food.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${food.kcalPer100g.toInt()} kcal · ${food.proteinPer100g.toInt()}g protein per 100g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

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

            Text("Portion", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val presets = buildList {
                    food.typicalServingGrams?.let { add(it)
                    }
                    add(50); add(100); add(150); add(200)
                }.distinct().sorted()
                presets.forEach { preset ->
                    FilterChip(
                        selected = grams.toIntOrNull() == preset && multiplier == 1.0,
                        onClick = { grams = preset.toString(); multiplier = 1.0 },
                        label = { Text("${preset}g") },
                    )
                }
            }

            OutlinedTextField(
                value = grams,
                onValueChange = { grams = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("Serving (g)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Quantity", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.5, 1.0, 1.5, 2.0).forEach { q ->
                    FilterChip(
                        selected = multiplier == q,
                        onClick = { multiplier = q },
                        label = { Text("${q}x") },
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("kcal", style = MaterialTheme.typography.labelMedium)
                Text(kcal.toString(), style = MaterialTheme.typography.titleMedium)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Protein / Carbs / Fat", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "${protein.toInt()}g / ${carbs.toInt()}g / ${fat.toInt()}g",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Button(
                onClick = { onSave(effectiveGrams, multiplier, mealType) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add to log") }
        }
    }
}
