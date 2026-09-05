package com.indian.nutrition.tracker.ui.screens.calculator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indian.nutrition.tracker.data.export.CsvExporter
import com.indian.nutrition.tracker.data.export.JsonBackup
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.domain.calculator.TargetCalculator
import com.indian.nutrition.tracker.domain.model.ActivityLevel
import com.indian.nutrition.tracker.domain.model.CalculatorResult
import com.indian.nutrition.tracker.domain.model.GoalType
import com.indian.nutrition.tracker.domain.model.ProteinBasis
import com.indian.nutrition.tracker.domain.model.Sex
import com.indian.nutrition.tracker.domain.model.UnitSystem
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.ui.appViewModel
import com.indian.nutrition.tracker.util.UnitConverters
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Target calculator, profile, export/import, and data management
 * (port of the web CalculatorSettingsScreen).
 */
@Composable
fun CalculatorScreen(
    container: AppContainer,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
) {
    val viewModel = appViewModel(container) { CalculatorViewModel(it) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dailyLogs by viewModel.dailyLogs.collectAsStateWithLifecycle()
    val waterLogs by viewModel.waterLogs.collectAsStateWithLifecycle()
    val weightLogs by viewModel.weightLogs.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Form state (initialized once from settings) ---
    var initialized by rememberSaveable { mutableStateOf(false) }
    var currentWeight by rememberSaveable { mutableStateOf("82") }
    var targetWeight by rememberSaveable { mutableStateOf("74") }
    var heightCm by rememberSaveable { mutableStateOf("176") }
    var ageYears by rememberSaveable { mutableStateOf("28") }
    var sex by rememberSaveable { mutableStateOf(Sex.M.name) }
    var activity by rememberSaveable { mutableStateOf(ActivityLevel.MODERATE.name) }
    var goal by rememberSaveable { mutableStateOf(GoalType.LOSE.name) }
    var goalRate by rememberSaveable { mutableStateOf(-0.5) }
    var proteinBasis by rememberSaveable { mutableStateOf(ProteinBasis.CURRENT.name) }
    var unit by rememberSaveable { mutableStateOf(UnitSystem.KG.name) }
    var exportDays by rememberSaveable { mutableStateOf("30") }

    var customMode by rememberSaveable { mutableStateOf(false) }
    var customCalories by rememberSaveable { mutableStateOf("1950") }
    var customProtein by rememberSaveable { mutableStateOf("115") }
    var customWater by rememberSaveable { mutableStateOf("2750") }

    var showImportChoice by remember { mutableStateOf<JsonBackup.BackupDto?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showValidation by remember { mutableStateOf(false) }

    val s = settings
    LaunchedEffect(s) {
        if (s != null && !initialized) {
            initialized = true
            currentWeight = if (s.unitSystem == UnitSystem.LB)
                UnitConverters.kgToLb(s.currentWeightKg).toString() else s.currentWeightKg.toString()
            targetWeight = if (s.unitSystem == UnitSystem.LB)
                UnitConverters.kgToLb(s.targetWeightKg).toString() else s.targetWeightKg.toString()
            heightCm = s.heightCm.roundToInt().toString()
            ageYears = s.ageYears.toString()
            sex = s.sex.name
            activity = s.activityLevel.name
            goal = s.goalType.name
            goalRate = s.goalRateKgPerWeek
            proteinBasis = s.proteinBasis.name
            unit = s.unitSystem.name
            customCalories = s.dailyCalorieTarget.toString()
            customProtein = s.dailyProteinTarget.toString()
            customWater = s.dailyWaterTargetMl.toString()
        }
    }

    val unitSystem = if (unit == UnitSystem.LB.name) UnitSystem.LB else UnitSystem.KG
    val currentKg = (currentWeight.toDoubleOrNull() ?: 0.0).let {
        if (unitSystem == UnitSystem.LB) UnitConverters.lbToKg(it) else it
    }
    val targetKg = (targetWeight.toDoubleOrNull() ?: 0.0).let {
        if (unitSystem == UnitSystem.LB) UnitConverters.lbToKg(it) else it
    }

    val calcResult: CalculatorResult? = remember(
        currentKg, targetKg, heightCm, ageYears, sex, activity, goal, goalRate, proteinBasis,
    ) {
        val h = heightCm.toDoubleOrNull() ?: 0.0
        val age = ageYears.toIntOrNull() ?: 0
        if (currentKg > 0.0 && h > 0.0 && age > 0) {
            TargetCalculator.calculateTargets(
                UserSettings(
                    currentWeightKg = currentKg,
                    targetWeightKg = targetKg,
                    heightCm = h,
                    ageYears = age,
                    sex = Sex.valueOf(sex),
                    activityLevel = ActivityLevel.valueOf(activity),
                    goalType = GoalType.valueOf(goal),
                    goalRateKgPerWeek = goalRate,
                    dailyCalorieTarget = 0,
                    dailyProteinTarget = 0,
                    dailyWaterTargetMl = 0,
                    proteinBasis = ProteinBasis.valueOf(proteinBasis),
                    unitSystem = unitSystem,
                    defaultChartRange = s?.defaultChartRange ?: com.indian.nutrition.tracker.domain.model.DateRange.D14,
                ),
            )
        } else null
    }

    // --- SAF launchers ---
    fun writeUri(uri: android.net.Uri, content: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            val days = exportDays.toIntOrNull() ?: 0
            val content = CsvExporter.export(dailyLogs, waterLogs, weightLogs, days)
            writeUri(uri, content)
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) writeUri(uri, viewModel.exportJson()) }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                if (raw.isNullOrBlank()) {
                    importError = "Could not read the selected file."
                } else {
                    when (val result = JsonBackup.parse(raw)) {
                        is JsonBackup.ImportResult.Success ->
                            showImportChoice = result.backup
                        is JsonBackup.ImportResult.Error ->
                            importError = result.message
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Target Calculator & Profile", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Mifflin–St Jeor BMR, TDEE, & Macronutrient Estimator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            ProfileCard(
                unitSystem = unitSystem,
                currentWeight = currentWeight,
                targetWeight = targetWeight,
                heightCm = heightCm,
                ageYears = ageYears,
                sex = sex,
                activity = activity,
                goal = goal,
                goalRate = goalRate,
                proteinBasis = proteinBasis,
                onUnitToggle = { newUnit ->
                    if (newUnit != unitSystem) {
                        val c = currentKg
                        val t = targetKg
                        if (newUnit == UnitSystem.LB) {
                            currentWeight = UnitConverters.kgToLb(c).toString()
                            targetWeight = UnitConverters.kgToLb(t).toString()
                        } else {
                            currentWeight = c.toString()
                            targetWeight = t.toString()
                        }
                        unit = newUnit.name
                    }
                },
                onCurrentWeight = { currentWeight = it },
                onTargetWeight = { targetWeight = it },
                onHeight = { heightCm = it },
                onAge = { ageYears = it },
                onSex = { sex = it },
                onActivity = { activity = it },
                onGoal = { newGoal ->
                    goal = newGoal.name
                    goalRate = when (newGoal) {
                        GoalType.MAINTAIN -> 0.0
                        GoalType.LOSE -> if (goalRate >= 0.0) -0.5 else goalRate
                        GoalType.GAIN -> if (goalRate <= 0.0) 0.25 else goalRate
                    }
                },
                onGoalRate = { goalRate = it },
                onProteinBasis = { proteinBasis = it },
            )
        }

        calcResult?.let { result ->
            item {
                ResultsCard(
                    result = result,
                    customMode = customMode,
                    customCalories = customCalories,
                    customProtein = customProtein,
                    customWater = customWater,
                    onModeChange = { manual ->
                        customMode = manual
                        if (!manual) {
                            customCalories = result.recommendedCalories.toString()
                            customProtein = result.recommendedProtein.toString()
                            customWater = result.recommendedWaterMl.toString()
                        }
                    },
                    onCustomCalories = { customCalories = it },
                    onCustomProtein = { customProtein = it },
                    onCustomWater = { customWater = it },
                    onSave = {
                        val cal = if (customMode) customCalories.toIntOrNull()
                        else result.recommendedCalories
                        val protein = if (customMode) customProtein.toIntOrNull()
                        else result.recommendedProtein
                        val water = if (customMode) customWater.toIntOrNull()
                        else result.recommendedWaterMl
                        if (cal == null || protein == null || water == null ||
                            currentKg <= 0.0 || targetKg <= 0.0 ||
                            heightCm.toDoubleOrNull() == null || ageYears.toIntOrNull() == null
                        ) {
                            showValidation = true
                        } else {
                            showValidation = false
                            viewModel.saveSettings(
                                UserSettings(
                                    currentWeightKg = currentKg,
                                    targetWeightKg = targetKg,
                                    heightCm = heightCm.toDouble(),
                                    ageYears = ageYears.toInt(),
                                    sex = Sex.valueOf(sex),
                                    activityLevel = ActivityLevel.valueOf(activity),
                                    goalType = GoalType.valueOf(goal),
                                    goalRateKgPerWeek = goalRate,
                                    dailyCalorieTarget = cal,
                                    dailyProteinTarget = protein,
                                    dailyWaterTargetMl = water,
                                    proteinBasis = ProteinBasis.valueOf(proteinBasis),
                                    unitSystem = unitSystem,
                                    defaultChartRange = s?.defaultChartRange
                                        ?: com.indian.nutrition.tracker.domain.model.DateRange.D14,
                                ),
                            )
                        }
                    },
                    showValidation = showValidation,
                )
            }
        }

        item {
            DataManagementCard(
                exportDays = exportDays,
                onExportDays = { exportDays = it },
                onExportCsv = {
                    val days = exportDays.toIntOrNull() ?: 0
                    val suffix = if (days > 0) "_last_${days}_days" else "_all"
                    csvLauncher.launch("nutrition_logs$suffix_${LocalDate.now()}.csv")
                },
                onExportJson = {
                    jsonExportLauncher.launch("nutrition_weight_backup_${LocalDate.now()}.json")
                },
                onImportJson = { jsonImportLauncher.launch(arrayOf("application/json", "text/plain")) },
                onClearLogs = { showClearConfirm = true },
            )
        }

        item { AboutCard() }
    }

    // --- Dialogs / messages ---

    importError?.let { error ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("Import failed") },
            text = { Text(error) },
            confirmButton = { Button(onClick = { importError = null }) { Text("OK") } },
        )
    }

    showImportChoice?.let { backup ->
        AlertDialog(
            onDismissRequest = { showImportChoice = null },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "Found ${backup.dailyLogs.size} food logs, ${backup.weightLogs.size} weights, " +
                        "${backup.waterLogs.size} water entries, ${backup.customFoods.size} custom foods " +
                        "(exported ${backup.exportedAt ?: "unknown"}).\n\n" +
                        "Replace wipes current logs first; Merge keeps existing entries."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importBackup(backup, replace = true) { showImportChoice = null }
                }) { Text("Replace All") }
            },
            dismissButton = {
                Row {
                    OutlinedButton(onClick = {
                        viewModel.importBackup(backup, replace = false) { showImportChoice = null }
                    }) { Text("Merge") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { showImportChoice = null }) { Text("Cancel") }
                }
            },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all logged data?") },
            text = {
                Text(
                    "Wipes all food entries, weight history, and water logs to start with 0. " +
                        "Your profile targets will be kept."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearLogs()
                    showClearConfirm = false
                }) { Text("Reset to 0") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
}

@Composable
private fun ProfileCard(
    unitSystem: UnitSystem,
    currentWeight: String,
    targetWeight: String,
    heightCm: String,
    ageYears: String,
    sex: String,
    activity: String,
    goal: String,
    goalRate: Double,
    proteinBasis: String,
    onUnitToggle: (UnitSystem) -> Unit,
    onCurrentWeight: (String) -> Unit,
    onTargetWeight: (String) -> Unit,
    onHeight: (String) -> Unit,
    onAge: (String) -> Unit,
    onSex: (String) -> Unit,
    onActivity: (String) -> Unit,
    onGoal: (GoalType) -> Unit,
    onGoalRate: (Double) -> Unit,
    onProteinBasis: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("1. Physical Profile & Biometrics",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    UnitSystem.entries.forEach { u ->
                        FilterChip(
                            selected = unitSystem == u,
                            onClick = { onUnitToggle(u) },
                            label = { Text(u.name.lowercase()) },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NumberField("Current Weight (${unitSystem.name.lowercase()})", currentWeight, onCurrentWeight, Modifier.weight(1f))
                NumberField("Target Weight (${unitSystem.name.lowercase()})", targetWeight, onTargetWeight, Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NumberField("Height (cm)", heightCm, onHeight, Modifier.weight(1f))
                NumberField("Age (years)", ageYears, onAge, Modifier.weight(1f))
            }

            SectionLabel("Biological Sex")
            ChipRow(Sex.entries.map { it.name to sexLabel(it) }, sex, onSex)

            SectionLabel("Activity Level (TDEE Multiplier)")
            ChipRow(
                ActivityLevel.entries.map {
                    it.name to "${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} ×" +
                        "${TargetCalculator.ACTIVITY_LEVELS[it]?.factor}"
                },
                activity,
                onActivity,
            )

            SectionLabel("Goal Strategy")
            ChipRow(GoalType.entries.map { it.name to it.name }, goal, { onGoal(GoalType.valueOf(it)) })

            SectionLabel("Rate of Change (${unitSystem.name.lowercase()}/wk)")
            val rates = when (GoalType.valueOf(goal)) {
                GoalType.LOSE -> listOf(-0.25, -0.5, -0.75)
                GoalType.GAIN -> listOf(0.25, 0.5)
                GoalType.MAINTAIN -> listOf(0.0)
            }
            ChipRow(
                rates.map { it.toString() to "${it} kg/wk" },
                goalRate.toString(),
                { onGoalRate(it.toDouble()) },
            )

            SectionLabel("Protein Target Basis")
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProteinBasis.entries.forEach { basis ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = proteinBasis == basis.name,
                            onClick = { onProteinBasis(basis.name) },
                        )
                        Text(basis.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                            style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun <T> ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun NumberField(
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

@Composable
private fun ResultsCard(
    result: CalculatorResult,
    customMode: Boolean,
    customCalories: String,
    customProtein: String,
    customWater: String,
    onModeChange: (Boolean) -> Unit,
    onCustomCalories: (String) -> Unit,
    onCustomProtein: (String) -> Unit,
    onCustomWater: (String) -> Unit,
    onSave: () -> Unit,
    showValidation: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("2. Calorie, Protein & Water Goals",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !customMode, onClick = { onModeChange(false) },
                    label = { Text("Pre-defined (Auto Formula)") })
                FilterChip(selected = customMode, onClick = { onModeChange(true) },
                    label = { Text("Custom Manual Targets") })
            }

            if (!customMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TargetTile("Calories", "${result.recommendedCalories}", "kcal/day", Modifier.weight(1f))
                    TargetTile("Protein", "${result.recommendedProtein}", "g/day", Modifier.weight(1f))
                    TargetTile("Hydration", "${result.recommendedWaterMl}", "ml/day (${result.recommendedWaterMl / 250} glasses)", Modifier.weight(1f))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NumberField("Calories (kcal)", customCalories, onCustomCalories, Modifier.weight(1f))
                    NumberField("Protein (g)", customProtein, onCustomProtein, Modifier.weight(1f))
                    NumberField("Water (ml)", customWater, onCustomWater, Modifier.weight(1f))
                }
                Text(
                    "Tip: auto-formula suggests ${result.recommendedCalories} kcal, " +
                        "${result.recommendedProtein}g protein, ${result.recommendedWaterMl}ml water.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Formula math breakdown
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Body Metrics & Formula Math:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FormulaRow("BMR (Mifflin–St Jeor):", result.formulaDetails.bmrFormula)
                FormulaRow("TDEE:", result.formulaDetails.tdeeFormula)
                FormulaRow("Auto Calorie Goal:", result.formulaDetails.targetFormula)
                FormulaRow("Auto Protein Goal:", result.formulaDetails.proteinFormula)
                FormulaRow("Auto Hydration Goal:", result.formulaDetails.waterFormula)
            }

            if (showValidation) {
                Text(
                    "Please check the input fields (valid weights, height, age, targets).",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Save & Use These Targets")
            }
        }
    }
}

@Composable
private fun FormulaRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TargetTile(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(unit, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DataManagementCard(
    exportDays: String,
    onExportDays: (String) -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onClearLogs: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Data Management & Export", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            NumberField("Days to export (0 = all)", exportDays, onExportDays, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onExportCsv, modifier = Modifier.weight(1f)) { Text("Export CSV") }
                OutlinedButton(onClick = onExportJson, modifier = Modifier.weight(1f)) { Text("Backup JSON") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onImportJson, modifier = Modifier.weight(1f)) { Text("Import JSON") }
                OutlinedButton(onClick = onClearLogs, modifier = Modifier.weight(1f)) { Text("Reset to 0") }
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("About & Data Attributions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Indian Food Composition Database: Derived from Indian Food Composition Tables (IFCT 2017) " +
                    "published by the National Institute of Nutrition (NIN), Indian Council of Medical Research (ICMR).",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Branded Products: Branded product data provided by Open Food Facts under Open Database License (ODbL).",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "All calculations and user logs remain 100% private and stored locally on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun sexLabel(sex: Sex): String = when (sex) {
    Sex.M -> "Male (+5)"
    Sex.F -> "Female (-161)"
    Sex.OTHER -> "Other (-78)"
}
