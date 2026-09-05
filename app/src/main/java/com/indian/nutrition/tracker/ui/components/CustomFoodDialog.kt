package com.indian.nutrition.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.indian.nutrition.tracker.domain.model.CustomFood
import kotlin.math.roundToInt

/** Create/edit dialog for custom foods/recipes (port of the web CustomFoodModal). */
@Composable
fun CustomFoodDialog(
    foodToEdit: CustomFood?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double?,
        servingDesc: String?,
        servingGrams: Int?,
        notes: String?,
    ) -> Unit,
) {
    var name by rememberSaveable(foodToEdit?.id) { mutableStateOf(foodToEdit?.name ?: "") }
    var kcal by rememberSaveable(foodToEdit?.id) { mutableStateOf(foodToEdit?.kcalPer100g?.roundToInt()?.toString() ?: "") }
    var protein by rememberSaveable(foodToEdit?.id) { mutableStateOf(foodToEdit?.proteinPer100g?.toString() ?: "") }
    var carbs by rememberSaveable(foodToEdit?.id) { mutableStateOf(foodToEdit?.carbsPer100g?.toString() ?: "") }
    var fat by rememberSaveable(foodToEdit?.id) { mutableStateOf(foodToEdit?.fatPer100g?.toString() ?: "") }
    var fiber by rememberSaveable(foodToEdit?.id) { mutableStateOf(foodToEdit?.fiberPer100g?.toString() ?: "") }
    var servingDesc by rememberSaveable(foodToEdit?.id) { mutableStateOf(foodToEdit?.typicalServingDescription ?: "") }
    var servingGrams by rememberSaveable(foodToEdit?.id) { mutableStateOf(foodToEdit?.typicalServingGrams?.toString() ?: "100") }
    var notes by rememberSaveable(foodToEdit?.id) { mutableStateOf(foodToEdit?.notes ?: "") }
    var showValidation by rememberSaveable(foodToEdit?.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("custom-food-modal-dialog"),
        title = {
            Text(if (foodToEdit == null) "Create Custom Food / Recipe" else "Edit Custom Food")
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food / Recipe Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom-food-name-input"),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MacroField("Calories (kcal) *", kcal, { kcal = it }, Modifier.weight(1f).testTag("custom-food-kcal-input"))
                    MacroField("Protein (g) *", protein, { protein = it }, Modifier.weight(1f).testTag("custom-food-protein-input"))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MacroField("Carbs (g)", carbs, { carbs = it }, Modifier.weight(1f).testTag("custom-food-carbs-input"))
                    MacroField("Fat (g)", fat, { fat = it }, Modifier.weight(1f).testTag("custom-food-fat-input"))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MacroField("Fiber (g)", fiber, { fiber = it }, Modifier.weight(1f).testTag("custom-food-fiber-input"))
                    MacroField("Serving grams", servingGrams, { servingGrams = it }, Modifier.weight(1f).testTag("custom-food-serving-grams-input"))
                }

                OutlinedButton(
                    onClick = {
                        val p = protein.toDoubleOrNull() ?: 0.0
                        val c = carbs.toDoubleOrNull() ?: 0.0
                        val f = fat.toDoubleOrNull() ?: 0.0
                        val approx = (p * 4 + c * 4 + f * 9).roundToInt()
                        if (approx > 0) kcal = approx.toString()
                    },
                    modifier = Modifier.align(Alignment.End),
                ) { Text("Auto-calc kcal (4P + 4C + 9F)") }

                OutlinedTextField(
                    value = servingDesc,
                    onValueChange = { servingDesc = it },
                    label = { Text("Typical Serving Desc (e.g. 1 bowl)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom-food-serving-desc-input"),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Recipe Ingredients / Notes (optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("custom-food-notes-input"),
                )

                if (showValidation) {
                    Text(
                        text = "Please enter a name and valid calories per 100g.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val kcalV = kcal.toDoubleOrNull()
                    if (name.trim().isEmpty() || kcalV == null || kcalV < 0) {
                        showValidation = true
                    } else {
                        onSave(
                            name,
                            kcalV,
                            protein.toDoubleOrNull() ?: 0.0,
                            carbs.toDoubleOrNull() ?: 0.0,
                            fat.toDoubleOrNull() ?: 0.0,
                            if (fiber.isNotBlank()) fiber.toDoubleOrNull() else null,
                            servingDesc,
                            servingGrams.toIntOrNull(),
                            notes,
                        )
                    }
                },
                modifier = Modifier.testTag("save-custom-food-submit-btn"),
            ) { Text(if (foodToEdit == null) "Save Custom Food" else "Update Food") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MacroField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
