package com.indian.nutrition.tracker.data.export

import com.indian.nutrition.tracker.domain.model.ActivityLevel
import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.DateRange
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.GoalType
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.domain.model.ProteinBasis
import com.indian.nutrition.tracker.domain.model.Sex
import com.indian.nutrition.tracker.domain.model.UnitSystem
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * JSON backup compatible with the web app's `db.exportDataJSON()` format
 * (snake_case fields via @SerialName), plus validation on import so corrupt
 * files are rejected instead of crashing (Phase 6 gate: import round-trips).
 */
// --- Web backup DTOs (snake_case, matching the old localStorage rows) ---


@Serializable
data class BackupDto(
    val settings: SettingsDto,
    val dailyLogs: List<DailyLogDto> = emptyList(),
    val weightLogs: List<WeightLogDto> = emptyList(),
    val waterLogs: List<WaterLogDto> = emptyList(),
    val customFoods: List<CustomFoodDto> = emptyList(),
    val exportedAt: String? = null,
    val version: String = "1.0",
)

@Serializable
data class SettingsDto(
    @SerialName("current_weight_kg") val currentWeightKg: Double,
    @SerialName("target_weight_kg") val targetWeightKg: Double,
    @SerialName("height_cm") val heightCm: Double,
    @SerialName("age_years") val ageYears: Int,
    val sex: String,
    @SerialName("activity_level") val activityLevel: String,
    @SerialName("goal_type") val goalType: String,
    @SerialName("goal_rate_kg_per_week") val goalRateKgPerWeek: Double,
    @SerialName("daily_calorie_target") val dailyCalorieTarget: Int,
    @SerialName("daily_protein_target") val dailyProteinTarget: Int,
    @SerialName("daily_water_target_ml") val dailyWaterTargetMl: Int,
    @SerialName("protein_basis") val proteinBasis: String,
    @SerialName("unit_system") val unitSystem: String,
    @SerialName("default_chart_range") val defaultChartRange: String,
)

@Serializable
data class DailyLogDto(
    val id: String,
    val date: String,
    @SerialName("food_id") val foodId: String,
    @SerialName("food_name") val foodName: String,
    val source: String,
    @SerialName("serving_grams") val servingGrams: Int,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    @SerialName("meal_type") val mealType: String,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class WeightLogDto(
    val id: String,
    val date: String,
    @SerialName("weight_kg") val weightKg: Double,
    val note: String? = null,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class WaterLogDto(
    val id: String,
    val date: String,
    @SerialName("amount_ml") val amountMl: Int,
    val time: String? = null,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class CustomFoodDto(
    val id: String,
    val name: String,
    @SerialName("kcal_per_100g") val kcalPer100g: Double,
    @SerialName("protein_per_100g") val proteinPer100g: Double,
    @SerialName("carbs_per_100g") val carbsPer100g: Double,
    @SerialName("fat_per_100g") val fatPer100g: Double,
    @SerialName("fiber_per_100g") val fiberPer100g: Double? = null,
    @SerialName("typical_serving_description") val typicalServingDescription: String? = null,
    @SerialName("typical_serving_grams") val typicalServingGrams: Int? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long,
)

object JsonBackup {

    val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    // --- Import result ---

    sealed class ImportResult {
        data class Success(val backup: BackupDto) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    // --- Export / import ---

    fun export(
        settings: UserSettings,
        dailyLogs: List<DailyLog>,
        weightLogs: List<WeightLog>,
        waterLogs: List<WaterLog>,
        customFoods: List<CustomFood>,
    ): String {
        val dto = BackupDto(
            settings = settings.toDto(),
            dailyLogs = dailyLogs.map { it.toDto() },
            weightLogs = weightLogs.map { it.toDto() },
            waterLogs = waterLogs.map { it.toDto() },
            customFoods = customFoods.map { it.toDto() },
            exportedAt = java.time.Instant.now().toString(),
            version = "1.0",
        )
        return json.encodeToString(BackupDto.serializer(), dto)
    }

    /** Parse + validate a backup file. Returns [ImportResult.Error] on any bad row. */
    fun parse(raw: String): ImportResult {
        val backup = try {
            json.decodeFromString(BackupDto.serializer(), raw)
        } catch (e: Exception) {
            return ImportResult.Error("Not a valid backup file (${e.message ?: "parse error"})")
        }
        val errors = mutableListOf<String>()

        // Validate settings
        val s = backup.settings
        if (s.currentWeightKg <= 0.0 || s.targetWeightKg <= 0.0 || s.heightCm <= 0.0) {
            errors += "settings contain invalid weights/height"
        }
        if (s.ageYears < 12 || s.ageYears > 100) errors += "age out of range: ${s.ageYears}"

        backup.dailyLogs.forEachIndexed { i, l ->
            val dateError = parseDateError(l.date)
            if (dateError != null) errors += "daily log #$i: $dateError"
            if (l.servingGrams <= 0 || l.servingGrams > 5000) errors += "daily log #$i: serving ${l.servingGrams}"
            if (l.calories < 0 || l.calories > 10000) errors += "daily log #$i: calories ${l.calories}"
            if (l.protein < 0 || l.carbs < 0 || l.fat < 0) errors += "daily log #$i: negative macro"
            if (l.foodName.isBlank()) errors += "daily log #$i: empty food name"
        }
        backup.weightLogs.forEachIndexed { i, w ->
            val dateError = parseDateError(w.date)
            if (dateError != null) errors += "weight log #$i: $dateError"
            if (w.weightKg < 20.0 || w.weightKg > 350.0) {
                errors += "weight log #$i: ${w.weightKg} kg out of 20–350"
            }
        }
        backup.waterLogs.forEachIndexed { i, w ->
            val dateError = parseDateError(w.date)
            if (dateError != null) errors += "water log #$i: $dateError"
            if (w.amountMl <= 0 || w.amountMl > 10000) {
                errors += "water log #$i: amount ${w.amountMl} ml"
            }
        }
        backup.customFoods.forEachIndexed { i, c ->
            if (c.name.isBlank()) errors += "custom food #$i: empty name"
            if (c.kcalPer100g < 0.0 || c.proteinPer100g < 0.0 ||
                c.carbsPer100g < 0.0 || c.fatPer100g < 0.0
            ) errors += "custom food #$i: negative macro"
        }

        return if (errors.isEmpty()) ImportResult.Success(backup) else
            ImportResult.Error("Invalid backup: ${errors.take(3).joinToString("; ")}")
    }

    fun parseDateError(date: String): String? =
        try {
            LocalDate.parse(date); null
        } catch (_: Exception) {
            "bad date '$date'"
        }

}
// --- Conversions (snake_case DTO → domain, tolerant of web labels) ---

fun SettingsDto.toDomain(): UserSettings = UserSettings(
    currentWeightKg = currentWeightKg,
    targetWeightKg = targetWeightKg,
    heightCm = heightCm,
    ageYears = ageYears,
    sex = coerceSex(sex),
    activityLevel = coerceActivity(activityLevel),
    goalType = coerceGoal(goalType),
    goalRateKgPerWeek = goalRateKgPerWeek,
    dailyCalorieTarget = dailyCalorieTarget,
    dailyProteinTarget = dailyProteinTarget,
    dailyWaterTargetMl = dailyWaterTargetMl,
    proteinBasis = coerceProteinBasis(proteinBasis),
    unitSystem = coerceUnit(unitSystem),
    defaultChartRange = coerceDateRange(defaultChartRange),
)

fun DailyLogDto.toDomain(): DailyLog = DailyLog(
    id = id,
    date = LocalDate.parse(date),
    foodId = foodId,
    foodName = foodName,
    source = coerceSource(source),
    servingGrams = servingGrams,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    mealType = coerceMeal(mealType),
    createdAt = createdAt,
)

fun WeightLogDto.toDomain(): WeightLog = WeightLog(
    id = id,
    date = LocalDate.parse(date),
    weightKg = weightKg,
    note = note,
    createdAt = createdAt,
)

fun WaterLogDto.toDomain(): WaterLog = WaterLog(
    id = id,
    date = LocalDate.parse(date),
    amountMl = amountMl,
    time = time,
    createdAt = createdAt,
)

fun CustomFoodDto.toDomain(): CustomFood = CustomFood(
    id = id,
    name = name,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    typicalServingDescription = typicalServingDescription,
    typicalServingGrams = typicalServingGrams,
    notes = notes,
    createdAt = createdAt,
)

// --- Domain → DTO ---

fun UserSettings.toDto(): SettingsDto = SettingsDto(
    currentWeightKg = currentWeightKg,
    targetWeightKg = targetWeightKg,
    heightCm = heightCm,
    ageYears = ageYears,
    sex = sex.toWebLabel(),
    activityLevel = activityLevel.toWebLabel(),
    goalType = goalType.toWebLabel(),
    goalRateKgPerWeek = goalRateKgPerWeek,
    dailyCalorieTarget = dailyCalorieTarget,
    dailyProteinTarget = dailyProteinTarget,
    dailyWaterTargetMl = dailyWaterTargetMl,
    proteinBasis = if (proteinBasis == ProteinBasis.TARGET) "target" else "current",
    unitSystem = if (unitSystem == UnitSystem.LB) "lb" else "kg",
    defaultChartRange = defaultChartRange.toWebLabel(),
)

fun DailyLog.toDto(): DailyLogDto = DailyLogDto(
    id = id, date = date.toString(), foodId = foodId, foodName = foodName,
    source = source.name, servingGrams = servingGrams, calories = calories,
    protein = protein, carbs = carbs, fat = fat, mealType = mealType.displayName,
    createdAt = createdAt,
)

fun WeightLog.toDto(): WeightLogDto = WeightLogDto(
    id = id, date = date.toString(), weightKg = weightKg, note = note, createdAt = createdAt,
)

fun WaterLog.toDto(): WaterLogDto = WaterLogDto(
    id = id, date = date.toString(), amountMl = amountMl, time = time, createdAt = createdAt,
)

fun CustomFood.toDto(): CustomFoodDto = CustomFoodDto(
    id = id, name = name, kcalPer100g = kcalPer100g, proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g, fatPer100g = fatPer100g, fiberPer100g = fiberPer100g,
    typicalServingDescription = typicalServingDescription, typicalServingGrams = typicalServingGrams,
    notes = notes, createdAt = createdAt,
)

// --- Coercion helpers (web labels → native enums; unknown → default) ---

fun Sex.toWebLabel(): String = when (this) {
    Sex.F -> "F"
    Sex.OTHER -> "Other"
    Sex.M -> "M"
}

fun ActivityLevel.toWebLabel(): String = when (this) {
    ActivityLevel.SEDENTARY -> "Sedentary"
    ActivityLevel.LIGHT -> "Light"
    ActivityLevel.MODERATE -> "Moderate"
    ActivityLevel.ACTIVE -> "Active"
    ActivityLevel.VERY_ACTIVE -> "Very Active"
}

fun GoalType.toWebLabel(): String = when (this) {
    GoalType.LOSE -> "Lose"
    GoalType.MAINTAIN -> "Maintain"
    GoalType.GAIN -> "Gain"
}

fun DateRange.toWebLabel(): String = when (this) {
    DateRange.D7 -> "7d"
    DateRange.D14 -> "14d"
    DateRange.D30 -> "30d"
    DateRange.ALL -> "All"
}

fun coerceSex(value: String): Sex = when (value.lowercase()) {
    "f", "female" -> Sex.F
    "other", "o" -> Sex.OTHER
    else -> Sex.M
}

fun coerceActivity(value: String): ActivityLevel =
    ActivityLevel.entries.firstOrNull { it.name.equals(value, true) } ?: when (value.lowercase()) {
        "sedentary" -> ActivityLevel.SEDENTARY
        "light", "light exercise" -> ActivityLevel.LIGHT
        "moderate", "moderate exercise" -> ActivityLevel.MODERATE
        "active" -> ActivityLevel.ACTIVE
        "very active" -> ActivityLevel.VERY_ACTIVE
        else -> ActivityLevel.MODERATE
    }

fun coerceGoal(value: String): GoalType = when (value.lowercase()) {
    "lose" -> GoalType.LOSE
    "maintain" -> GoalType.MAINTAIN
    "gain" -> GoalType.GAIN
    else -> GoalType.LOSE
}

fun coerceProteinBasis(value: String): ProteinBasis = when (value.lowercase()) {
    "target" -> ProteinBasis.TARGET
    else -> ProteinBasis.CURRENT
}

fun coerceUnit(value: String): UnitSystem = when (value.lowercase()) {
    "lb" -> UnitSystem.LB
    else -> UnitSystem.KG
}

fun coerceDateRange(value: String): DateRange = when (value.lowercase().trim()) {
    "7d" -> DateRange.D7
    "30d" -> DateRange.D30
    "all", "60d" -> DateRange.ALL
    else -> DateRange.D14
}

fun coerceSource(value: String): FoodSource =
    FoodSource.entries.firstOrNull { it.name.equals(value, true) } ?: FoodSource.OFF

fun coerceMeal(value: String): MealType =
    MealType.entries.firstOrNull { it.name.equals(value, true) } ?: MealType.LUNCH

