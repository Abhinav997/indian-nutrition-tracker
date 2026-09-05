package com.indian.nutrition.tracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.ServingUnit
import kotlin.math.roundToInt

/** A temporary ingredient row used by the recipe builder. */
private data class RecipeIngredient(val food: Food, val grams: Int)

/** Create/edit dialog for custom foods/recipes (port of the web CustomFoodModal). */
@Composable
fun CustomFoodDialog(
    foodToEdit: CustomFood?,
    availableFoods: List<Food> = emptyList(),
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
        servingUnit: ServingUnit,
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
    var servingUnit by rememberSaveable(foodToEdit?.id) {
        mutableStateOf(foodToEdit?.servingUnit ?: ServingUnit.GRAMS)
    }
    var notes by rememberSaveable(foodToEdit?.id) { mutableStateOf(foodToEdit?.notes ?: "") }
    var showValidation by rememberSaveable(foodToEdit?.id) { mutableStateOf(false) }
    var ingredientQuery by rememberSaveable(foodToEdit?.id) { mutableStateOf("") }
    var ingredientAmount by rememberSaveable(foodToEdit?.id) { mutableStateOf("100") }
    var selectedIngredient by remember { mutableStateOf<Food?>(null) }
    var ingredients by remember(foodToEdit?.id) { mutableStateOf(emptyList<RecipeIngredient>()) }

    val ingredientMatches = remember(availableFoods, ingredientQuery) {
        val query = ingredientQuery.trim().lowercase()
        if (query.isEmpty()) emptyList()
        else availableFoods.filter {
            it.name.lowercase().contains(query) || it.brand?.lowercase()?.contains(query) == true
        }.take(5)
    }
    val ingredientTotalGrams = ingredients.sumOf { it.grams }
    val ingredientKcal = if (ingredientTotalGrams > 0) {
        ingredients.sumOf { it.food.kcalPer100g * it.grams } / ingredientTotalGrams * 100
    } else 0.0
    val ingredientProtein = if (ingredientTotalGrams > 0) {
        ingredients.sumOf { it.food.proteinPer100g * it.grams } / ingredientTotalGrams * 100
    } else 0.0
    val ingredientCarbs = if (ingredientTotalGrams > 0) {
        ingredients.sumOf { it.food.carbsPer100g * it.grams } / ingredientTotalGrams * 100
    } else 0.0
    val ingredientFat = if (ingredientTotalGrams > 0) {
        ingredients.sumOf { it.food.fatPer100g * it.grams } / ingredientTotalGrams * 100
    } else 0.0

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

                if (availableFoods.isNotEmpty()) {
                    Text("Recipe ingredients (optional)", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = ingredientQuery,
                        onValueChange = {
                            ingredientQuery = it
                            selectedIngredient = null
                        },
                        label = { Text("Search foods to add") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("recipe-ingredient-search-input"),
                    )
                    if (selectedIngredient == null) {
                        ingredientMatches.forEach { food ->
                            AssistChip(
                                onClick = {
                                    selectedIngredient = food
                                    ingredientQuery = food.name
                                },
                                label = { Text(food.name) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                selectedIngredient?.name.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = ingredientAmount,
                                onValueChange = { ingredientAmount = it.filter(Char::isDigit).take(6) },
                                label = { Text("g") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(84.dp),
                            )
                            TextButton(
                                onClick = {
                                    val food = selectedIngredient
                                    val grams = ingredientAmount.toIntOrNull()
                                    if (food != null && grams != null && grams > 0) {
                                        ingredients = ingredients + RecipeIngredient(food, grams)
                                        ingredientQuery = ""
                                        ingredientAmount = "100"
                                        selectedIngredient = null
                                    }
                                },
                            ) { Text("Add") }
                        }
                    }
                    ingredients.forEachIndexed { index, ingredient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "${ingredient.food.name} · ${ingredient.grams}g",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                ingredients = ingredients.toMutableList().also { it.removeAt(index) }
                            }) { Text("Remove") }
                        }
                    }
                    if (ingredients.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                kcal = ingredientKcal.roundToInt().toString()
                                protein = round1(ingredientProtein).toString()
                                carbs = round1(ingredientCarbs).toString()
                                fat = round1(ingredientFat).toString()
                                if (servingGrams == "100") servingGrams = ingredientTotalGrams.toString()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("apply-recipe-macros-btn"),
                        ) {
                            Text("Use ingredient totals ($ingredientTotalGrams g)")
                        }
                    }
                }

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
                    MacroField(
                        if (servingUnit == ServingUnit.GRAMS) "Serving grams" else "Weight per unit (g)",
                        servingGrams,
                        { servingGrams = it },
                        Modifier.weight(1f).testTag("custom-food-serving-grams-input"),
                    )
                }

                Text("Serving unit", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ServingUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = servingUnit == unit,
                            onClick = { servingUnit = unit },
                            label = { Text(unit.label) },
                        )
                    }
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
                        text = "Enter a name, non-negative macros, and a serving weight from 1 to 100,000g.",
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
                    val proteinV = protein.toDoubleOrNull() ?: 0.0
                    val carbsV = carbs.toDoubleOrNull() ?: 0.0
                    val fatV = fat.toDoubleOrNull() ?: 0.0
                    val servingWeight = servingGrams.toIntOrNull()
                    if (name.trim().isEmpty() || kcalV == null || kcalV < 0 ||
                        proteinV < 0 || carbsV < 0 || fatV < 0 ||
                        servingWeight !in 1..100_000
                    ) {
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
                            notes + ingredients.takeIf { it.isNotEmpty() }?.let { list ->
                                "\nIngredients: " + list.joinToString(", ") { "${it.food.name} ${it.grams}g" }
                            }.orEmpty(),
                            servingUnit,
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
