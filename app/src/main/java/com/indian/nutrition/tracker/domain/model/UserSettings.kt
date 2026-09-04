package com.indian.nutrition.tracker.domain.model

/** Biological sex used by the Mifflin-St Jeor formula. */
enum class Sex { M, F, OTHER }

/** Activity level — maps to a TDEE multiplier. */
enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }

/** Weight goal — drives the calorie adjustment and protein guidelines. */
enum class GoalType { LOSE, MAINTAIN, GAIN }

/** Weight of the body the protein target is based on. */
enum class ProteinBasis { CURRENT, TARGET }

/** Display units for weight. */
enum class UnitSystem { KG, LB }

/** Chart range presets on the progress screen. */
enum class DateRange(val days: Int) {
    D7(7),
    D14(14),
    D30(30),
    ALL(60),
}

/** Persisted user profile and daily targets. */
data class UserSettings(
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val heightCm: Double,
    val ageYears: Int,
    val sex: Sex,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val goalRateKgPerWeek: Double,
    val dailyCalorieTarget: Int,
    val dailyProteinTarget: Int,
    val dailyWaterTargetMl: Int,
    val proteinBasis: ProteinBasis,
    val unitSystem: UnitSystem,
    val defaultChartRange: DateRange,
)
