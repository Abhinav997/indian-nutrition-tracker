package com.indian.nutrition.tracker.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.domain.model.DateRange
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.domain.model.WeightLog
import com.indian.nutrition.tracker.ui.appViewModel
import com.indian.nutrition.tracker.ui.components.ProgressionChart
import com.indian.nutrition.tracker.ui.components.WeightSheet
import com.indian.nutrition.tracker.util.UnitConverters
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(container: AppContainer) {
    val viewModel = appViewModel(container) { ProgressViewModel(it) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dailyLogs by viewModel.dailyLogs.collectAsStateWithLifecycle()
    val weightLogs by viewModel.weightLogs.collectAsStateWithLifecycle()
    val waterLogs by viewModel.waterLogs.collectAsStateWithLifecycle()
    val metric by viewModel.metric.collectAsStateWithLifecycle()
    val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()

    var showWeightSheet by remember { mutableStateOf(false) }
    var editingWeight by remember { mutableStateOf<WeightLog?>(null) }

    val s = settings ?: return LoadingProgress()

    val listState = rememberLazyListState()
    val dates = remember(dateRange) { ChartMath.rangeDates(dateRange) }
    val calorieMap = remember(dailyLogs) { ChartMath.dailyCalories(dailyLogs) }
    val proteinMap = remember(dailyLogs) { ChartMath.dailyProtein(dailyLogs) }
    val waterMap = remember(waterLogs) { ChartMath.dailyWater(waterLogs) }

    val series = remember(metric, dates, weightLogs, calorieMap, proteinMap, waterMap, s) {
        when (metric) {
            ChartMetric.WEIGHT -> ChartMath.weightSeries(dates, weightLogs, s)
            ChartMetric.CALORIES -> ChartMath.intakeSeries(
                metric, dates, calorieMap, s.dailyCalorieTarget.toDouble(), "kcal",
            )
            ChartMetric.PROTEIN -> ChartMath.intakeSeries(
                metric, dates, proteinMap, s.dailyProteinTarget.toDouble(), "g",
            )
            ChartMetric.WATER -> ChartMath.intakeSeries(
                metric, dates, waterMap, s.dailyWaterTargetMl.toDouble(), "ml",
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { StatsBanner(settings = s, weightLogs = weightLogs, onLogWeight = { showWeightSheet = true }) }

        item { MetricSelector(selected = metric, onSelect = viewModel::setMetric) }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = when (metric) {
                        ChartMetric.WEIGHT -> "Body Weight Progression"
                        ChartMetric.CALORIES -> "Daily Calorie Intake vs Target"
                        ChartMetric.PROTEIN -> "Daily Protein Intake vs Target"
                        ChartMetric.WATER -> "Daily Water Hydration vs Target"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                RangeSelector(selected = dateRange, onSelect = viewModel::setDateRange)
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ProgressionChart(series = series)
                }
            }
        }

        item { WeightSummaryGrid(settings = s, weightLogs = weightLogs) }

        item {
            IntakeAveragesCard(
                settings = s,
                dates = dates,
                calorieMap = calorieMap,
                proteinMap = proteinMap,
                waterMap = waterMap,
                range = dateRange,
            )
        }

        item { WeightHistoryHeader(count = weightLogs.size, onAdd = { showWeightSheet = true }) }

        if (weightLogs.isEmpty()) {
            item {
                Text(
                    "No weight entries logged yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(weightLogs.reversed(), key = { it.id }, contentType = { "weight-history-row" }) { log ->
                WeightHistoryRow(
                    log = log,
                    settings = s,
                    onEdit = { editingWeight = it },
                    onDelete = viewModel::deleteWeight,
                )
            }
        }
    }

    if (showWeightSheet || editingWeight != null) {
        WeightSheet(
            unitSystem = s.unitSystem,
            currentWeightKg = s.currentWeightKg,
            weightLog = editingWeight,
            onDismiss = {
                showWeightSheet = false
                editingWeight = null
            },
            onSave = { kg, note ->
                viewModel.saveWeight(editingWeight?.date ?: java.time.LocalDate.now(), kg, note)
                showWeightSheet = false
                editingWeight = null
            },
        )
    }
}

@Composable
private fun LoadingProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Loading…", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StatsBanner(
    settings: UserSettings,
    weightLogs: List<WeightLog>,
    onLogWeight: () -> Unit,
) {
    val latest = weightLogs.lastOrNull()?.weightKg ?: settings.currentWeightKg
    val starting = weightLogs.firstOrNull()?.weightKg ?: settings.currentWeightKg
    val target = settings.targetWeightKg
    val diffFromStart = ((latest - starting) * 10).roundToInt() / 10.0
    val diffFromTarget = ((latest - target) * 10).roundToInt() / 10.0
    val bmi = UnitConverters.calculateBmi(latest, settings.heightCm)

    val bannerContainer = MaterialTheme.colorScheme.primaryContainer
    val bannerText = MaterialTheme.colorScheme.onPrimaryContainer
    val bannerMuted = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bannerContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        "PROGRESS & TRENDS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Body Composition & Intake",
                        style = MaterialTheme.typography.titleMedium,
                        color = bannerText,
                    )
                }
                Button(onClick = onLogWeight, modifier = Modifier.testTag("progress-log-weight-btn")) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log Weight")
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BannerStat(
                    label = "Current Weight",
                    value = UnitConverters.formatWeight(latest, settings.unitSystem),
                    sub = bmi?.let {
                        "${it.bmi} · ${it.category.name.lowercase().replaceFirstChar { c -> c.uppercase() }}"
                    } ?: "",
                    valueColor = bannerText,
                    subColor = MaterialTheme.colorScheme.primary,
                )
                BannerStat(
                    label = "Net Change",
                    value = (if (diffFromStart > 0) "+" else "") +
                        UnitConverters.formatWeight(diffFromStart, settings.unitSystem),
                    sub = "From start",
                    valueColor = if (diffFromStart <= 0) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.secondary,
                    subColor = bannerMuted,
                )
                BannerStat(
                    label = "Goal Target",
                    value = UnitConverters.formatWeight(target, settings.unitSystem),
                    sub = if (abs(diffFromTarget) <= 0.2) "Reached!"
                    else "${UnitConverters.formatWeight(abs(diffFromTarget), settings.unitSystem)} left",
                    valueColor = MaterialTheme.colorScheme.primary,
                    subColor = bannerMuted,
                )
            }
        }
    }
}

@Composable
private fun BannerStat(
    label: String,
    value: String,
    sub: String,
    valueColor: Color,
    subColor: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
        )
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Black)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = subColor)
    }
}

@Composable
private fun metricColor(metric: ChartMetric): Color = when (metric) {
    ChartMetric.WEIGHT -> MaterialTheme.colorScheme.primary
    ChartMetric.CALORIES -> MaterialTheme.colorScheme.secondary
    ChartMetric.PROTEIN -> MaterialTheme.colorScheme.tertiary
    ChartMetric.WATER -> MaterialTheme.colorScheme.primary
}

@Composable
private fun MetricSelector(selected: ChartMetric, onSelect: (ChartMetric) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ChartMetric.entries.forEach { metric ->
            FilterChip(
                selected = selected == metric,
                onClick = { onSelect(metric) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(metricColor(metric)),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(metric.label)
                    }
                },
            )
        }
    }
}

private fun rangeLabel(range: DateRange): String = when (range) {
    DateRange.D7 -> "7d"
    DateRange.D14 -> "14d"
    DateRange.D30 -> "30d"
    DateRange.ALL -> "All"
}

@Composable
private fun RangeSelector(selected: DateRange, onSelect: (DateRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DateRange.entries.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelect(range) },
                label = { Text(rangeLabel(range)) },
            )
        }
    }
}

@Composable
private fun WeightSummaryGrid(settings: UserSettings, weightLogs: List<WeightLog>) {
    val latest = weightLogs.lastOrNull()?.weightKg ?: settings.currentWeightKg
    val starting = weightLogs.firstOrNull()?.weightKg ?: settings.currentWeightKg
    val target = settings.targetWeightKg

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryTile(
            title = "Starting Weight",
            value = UnitConverters.formatWeight(starting, settings.unitSystem),
            footer = "Baseline",
            valueColor = MaterialTheme.colorScheme.onSurface,
            footerColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        SummaryTile(
            title = "Current Weight",
            value = UnitConverters.formatWeight(latest, settings.unitSystem),
            footer = (if (latest - starting <= 0) "↓ " else "↑ ") + UnitConverters.formatWeight(
                ((latest - starting) * 10.0).roundToInt() / 10.0, settings.unitSystem,
            ),
            valueColor = MaterialTheme.colorScheme.primary,
            footerColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        SummaryTile(
            title = "Target Weight",
            value = UnitConverters.formatWeight(target, settings.unitSystem),
            footer = if (abs(latest - target) <= 0.2) "Goal Reached!"
            else "${UnitConverters.formatWeight(abs(latest - target), settings.unitSystem)} to go",
            valueColor = MaterialTheme.colorScheme.onSurface,
            footerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryTile(
    title: String,
    value: String,
    footer: String,
    valueColor: Color,
    footerColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor, fontWeight = FontWeight.Bold)
            Text(footer, style = MaterialTheme.typography.labelSmall, color = footerColor)
        }
    }
}

@Composable
private fun IntakeAveragesCard(
    settings: UserSettings,
    dates: List<LocalDate>,
    calorieMap: Map<LocalDate, Double>,
    proteinMap: Map<LocalDate, Double>,
    waterMap: Map<LocalDate, Double>,
    range: DateRange,
) {
    // Web bug fix: averages divide by the full range, not days with data.
    val avgCalories = ChartMath.average(calorieMap, dates, 0)
    val avgProtein = ChartMath.average(proteinMap, dates, 1)
    val avgWater = ChartMath.average(waterMap, dates, 0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Intake & Hydration Averages (${rangeLabel(range)} Range)",
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AverageTile("Avg Calories", "${avgCalories.toInt()}", "kcal/d", "Target: ${settings.dailyCalorieTarget}", Modifier.weight(1f))
                AverageTile("Avg Protein", "${avgProtein}", "g/d", "Target: ${settings.dailyProteinTarget}g", Modifier.weight(1f))
                AverageTile("Avg Water", "${avgWater.toInt()}", "ml/d", "Target: ${settings.dailyWaterTargetMl}ml", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AverageTile(title: String, value: String, unit: String, target: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$value $unit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Text(target, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeightHistoryHeader(count: Int, onAdd: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "Weight Log History ($count entries)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Button(onClick = onAdd) { Text("+ Add Entry") }
    }
}

@Composable
private fun WeightHistoryRow(
    log: WeightLog,
    settings: UserSettings,
    onEdit: (WeightLog) -> Unit,
    onDelete: (String) -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "⚖️",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        UnitConverters.formatWeight(log.weightKg, settings.unitSystem),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        log.date.format(dateFormatter) + (log.note?.let { " · \"$it\"" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = { onEdit(log) },
                modifier = Modifier.testTag("edit-weight-${log.id}"),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit weight entry")
            }
            IconButton(onClick = { onDelete(log.id) }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete weight entry",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
