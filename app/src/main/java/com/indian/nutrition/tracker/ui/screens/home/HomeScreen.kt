package com.indian.nutrition.tracker.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.ui.appViewModel
import com.indian.nutrition.tracker.ui.components.DateSwitcherBar
import com.indian.nutrition.tracker.ui.components.FoodPickerDialog
import com.indian.nutrition.tracker.ui.components.MacroProgressBar
import com.indian.nutrition.tracker.ui.components.ServingSheet
import com.indian.nutrition.tracker.ui.components.WaterCard
import com.indian.nutrition.tracker.ui.components.WeightSheet
import com.indian.nutrition.tracker.util.DateUtils
import com.indian.nutrition.tracker.util.UnitConverters
import kotlin.math.roundToInt

@Composable
fun HomeScreen(container: AppContainer) {
    val viewModel = appViewModel(container) { HomeViewModel(it) }
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dailyLogs by viewModel.dailyLogs.collectAsStateWithLifecycle()
    val waterLogs by viewModel.waterLogs.collectAsStateWithLifecycle()
    val weightLogs by viewModel.weightLogs.collectAsStateWithLifecycle()

    var servingFood by remember { mutableStateOf<Food?>(null) }
    var servingMeal by remember { mutableStateOf(MealType.LUNCH) }
    var showFoodPicker by remember { mutableStateOf(false) }
    var showWeightSheet by remember { mutableStateOf(false) }

    val s = settings ?: return LoadingHome()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    text = if (selectedDate == DateUtils.today()) "Today's Intake" else
                        "Intake · ${DateUtils.dayLabel(selectedDate)}",
                    style = MaterialTheme.typography.titleLarge,
                )
                DateSwitcherBar(
                    selectedDate = selectedDate,
                    onPrevious = viewModel::previousDay,
                    onNext = viewModel::nextDay,
                    onToday = viewModel::jumpToToday,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            IntakeCard(
                logs = dailyLogs,
                settings = s,
                onAddFood = {
                    servingMeal = it ?: MealType.LUNCH
                    showFoodPicker = true
                },
            )
        }

        item {
            WeightSummaryCard(
                settings = s,
                weightLogs = weightLogs,
                onLogWeight = { showWeightSheet = true },
            )
        }

        item {
            WaterCard(
                logs = waterLogs,
                targetMl = s.dailyWaterTargetMl,
                onAddWater = viewModel::addWater,
                onDeleteWater = viewModel::deleteWater,
            )
        }

        item {
            Text("Meals Logged for ${selectedDate}", style = MaterialTheme.typography.titleSmall)
        }

        MealType.entries.forEach { meal ->
            val items = dailyLogs.filter { it.mealType == meal }
            item(key = "meal-${meal.name}") {
                MealSection(
                    meal = meal,
                    items = items,
                    onAdd = {
                        servingMeal = it
                        showFoodPicker = true
                    },
                    onDelete = viewModel::deleteLog,
                )
            }
        }
    }

    if (showFoodPicker) {
        FoodPickerDialog(
            foodRepository = container.foodRepository,
            onSelect = { food ->
                showFoodPicker = false
                servingFood = food
            },
            onDismiss = { showFoodPicker = false },
        )
    }

    servingFood?.let { food ->
        ServingSheet(
            food = food,
            initialMeal = servingMeal,
            onDismiss = { servingFood = null },
            onSave = { grams, quantity, meal ->
                viewModel.addServing(food, grams, quantity, meal)
                servingFood = null
            },
        )
    }

    if (showWeightSheet) {
        WeightSheet(
            unitSystem = s.unitSystem,
            currentWeightKg = s.currentWeightKg,
            onDismiss = { showWeightSheet = false },
            onSave = { kg, note ->
                viewModel.addWeight(kg, note)
                showWeightSheet = false
            },
        )
    }
}

@Composable
private fun LoadingHome() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Loading…", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun IntakeCard(logs: List<DailyLog>, settings: UserSettings, onAddFood: (MealType?) -> Unit) {
    val totalKcal = logs.sumOf { it.calories }
    val totalProtein = HomeViewModel.round1(logs.sumOf { it.protein })
    val totalCarbs = HomeViewModel.round1(logs.sumOf { it.carbs })
    val totalFat = HomeViewModel.round1(logs.sumOf { it.fat })

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text("Today's Intake", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Target vs Logged Nutrition",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = { onAddFood(null) }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add Food", modifier = Modifier.padding(start = 4.dp))
                }
            }
            MacroProgressBar(
                label = "Calories",
                value = totalKcal.toDouble(),
                target = settings.dailyCalorieTarget.toDouble(),
                unit = "kcal",
                accent = Color(0xFFF59E0B),
            )
            MacroProgressBar(
                label = "Protein Target",
                value = totalProtein,
                target = settings.dailyProteinTarget.toDouble(),
                unit = "g",
                accent = Color(0xFF10B981),
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Carbohydrates", style = MaterialTheme.typography.bodySmall)
                Text("${totalCarbs.toInt()}g", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Fats", style = MaterialTheme.typography.bodySmall)
                Text("${totalFat.toInt()}g", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun WeightSummaryCard(
    settings: UserSettings,
    weightLogs: List<com.indian.nutrition.tracker.domain.model.WeightLog>,
    onLogWeight: () -> Unit,
) {
    val latest = weightLogs.lastOrNull()?.weightKg ?: settings.currentWeightKg
    val bmi = UnitConverters.calculateBmi(latest, settings.heightCm)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Scale, contentDescription = null)
                    Text(
                        text = "Current Body Weight",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Text(
                    text = UnitConverters.formatWeight(latest, settings.unitSystem),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Target: ${UnitConverters.formatWeight(settings.targetWeightKg, settings.unitSystem)}" +
                        (bmi?.let { " · BMI ${it.bmi} (${it.category.name.lowercase()})" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onLogWeight) { Text("Log Weight") }
        }
    }
}

@Composable
private fun MealSection(
    meal: MealType,
    items: List<DailyLog>,
    onAdd: (MealType) -> Unit,
    onDelete: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(meal.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${items.sumOf { it.calories }} kcal · ${items.sumOf { it.protein }.roundToInt()}g P",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onAdd(meal) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add to ${meal.displayName}")
                }
            }
            if (items.isEmpty()) {
                Text(
                    "No items logged yet. Tap + to add.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.foodName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${item.servingGrams}g · ${item.calories} kcal · ${item.protein.toInt()}g protein",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete ${item.foodName}",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}


