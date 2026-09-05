package com.indian.nutrition.tracker.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.ui.appViewModel
import com.indian.nutrition.tracker.ui.components.DateSwitcherBar
import com.indian.nutrition.tracker.ui.components.EditFoodLogSheet
import com.indian.nutrition.tracker.ui.components.MacroProgressBar
import com.indian.nutrition.tracker.ui.components.WaterCard
import com.indian.nutrition.tracker.ui.components.WeightSheet
import com.indian.nutrition.tracker.util.DateUtils
import com.indian.nutrition.tracker.util.UnitConverters
import kotlin.math.roundToInt

@Composable
fun HomeScreen(container: AppContainer, onOpenFoodSearch: (MealType, java.time.LocalDate) -> Unit) {
    val viewModel = appViewModel(container) { HomeViewModel(it) }
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dailyLogs by viewModel.dailyLogs.collectAsStateWithLifecycle()
    val waterLogs by viewModel.waterLogs.collectAsStateWithLifecycle()
    val weightLogs by viewModel.weightLogs.collectAsStateWithLifecycle()

    var showWeightSheet by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<DailyLog?>(null) }
    var preferredMealName by rememberSaveable { mutableStateOf(MealType.LUNCH.name) }
    val listState = rememberLazyListState()

    val s = settings ?: return LoadingHome()
    val preferredMeal = MealType.entries.firstOrNull { it.name == preferredMealName } ?: MealType.LUNCH
    val logsByMeal = remember(dailyLogs) {
        MealType.entries.associateWith { meal -> dailyLogs.filter { it.mealType == meal } }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.testTag("app-top-header")) {
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
                selectedDate = selectedDate,
                // The serving sheet still lets the user change the meal; the
                // selected date is carried through so backfilled entries stay
                // on the day the user is viewing.
                onAddFood = { onOpenFoodSearch(preferredMeal, selectedDate) },
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
                onEditWater = viewModel::updateWater,
                onDeleteWater = viewModel::deleteWater,
            )
        }

        item {
            Text("Meals Logged for ${selectedDate}", style = MaterialTheme.typography.titleSmall)
        }

        MealType.entries.forEach { meal ->
            val items = logsByMeal[meal].orEmpty()
            item(key = "meal-${meal.name}", contentType = "meal-section") {
                MealSection(
                    meal = meal,
                    items = items,
                    onAdd = {
                        preferredMealName = it.name
                        onOpenFoodSearch(it, selectedDate)
                    },
                    onEdit = { editingLog = it },
                    onDelete = viewModel::deleteLog,
                )
            }
        }
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

    editingLog?.let { log ->
        EditFoodLogSheet(
            log = log,
            onDismiss = { editingLog = null },
            onSave = { updated ->
                viewModel.updateLog(updated)
                editingLog = null
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
private fun IntakeCard(
    logs: List<DailyLog>,
    settings: UserSettings,
    selectedDate: java.time.LocalDate,
    onAddFood: () -> Unit,
) {
    val totalKcal = logs.sumOf { it.calories }
    val totalProtein = HomeViewModel.round1(logs.sumOf { it.protein })
    val totalCarbs = HomeViewModel.round1(logs.sumOf { it.carbs })
    val totalFat = HomeViewModel.round1(logs.sumOf { it.fat })

    Card(modifier = Modifier.fillMaxWidth().testTag("today-intake-card")) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        if (selectedDate == DateUtils.today()) "Today's Intake"
                        else "Intake · ${DateUtils.dayLabel(selectedDate)}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "Target vs Logged Nutrition",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onAddFood, modifier = Modifier.testTag("home-quick-add-food-btn")) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add Food", modifier = Modifier.padding(start = 4.dp))
                }
            }
            MacroProgressBar(
                label = "Calories",
                value = totalKcal.toDouble(),
                target = settings.dailyCalorieTarget.toDouble(),
                unit = "kcal",
                accent = MaterialTheme.colorScheme.secondary,
            )
            MacroProgressBar(
                label = "Protein Target",
                value = totalProtein,
                target = settings.dailyProteinTarget.toDouble(),
                unit = "g",
                accent = MaterialTheme.colorScheme.tertiary,
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

    Card(modifier = Modifier.fillMaxWidth().testTag("home-weight-summary-card")) {
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
            Button(onClick = onLogWeight, modifier = Modifier.testTag("home-log-weight-btn")) { Text("Log Weight") }
        }
    }
}

@Composable
private fun MealSection(
    meal: MealType,
    items: List<DailyLog>,
    onAdd: (MealType) -> Unit,
    onEdit: (DailyLog) -> Unit,
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
                IconButton(onClick = { onAdd(meal) }, modifier = Modifier.testTag("meal-add-${meal.name}")) {
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
                    key(item.id) {
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
                        IconButton(
                            onClick = { onEdit(item) },
                            modifier = Modifier.testTag("edit-food-log-${item.id}"),
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit ${item.foodName}")
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
}


