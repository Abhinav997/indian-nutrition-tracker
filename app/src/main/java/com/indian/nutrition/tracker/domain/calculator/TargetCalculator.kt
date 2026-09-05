package com.indian.nutrition.tracker.domain.calculator

import com.indian.nutrition.tracker.domain.model.ActivityLevel
import com.indian.nutrition.tracker.domain.model.CalculatorResult
import com.indian.nutrition.tracker.domain.model.FormulaDetails
import com.indian.nutrition.tracker.domain.model.GoalType
import com.indian.nutrition.tracker.domain.model.ProteinBasis
import com.indian.nutrition.tracker.domain.model.Sex
import com.indian.nutrition.tracker.domain.model.UserSettings
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Calories, protein, and water targets via the Mifflin-St Jeor equation.
 * Direct port of the web app's `utils/calculator.ts`.
 */
object TargetCalculator {

    data class ActivityLevelInfo(val factor: Double, val label: String, val desc: String)

    data class ProteinGuideline(val min: Double, val max: Double, val default: Double, val desc: String)

    val ACTIVITY_LEVELS: Map<ActivityLevel, ActivityLevelInfo> = mapOf(
        ActivityLevel.SEDENTARY to ActivityLevelInfo(1.2, "Sedentary", "Little or no exercise, desk job"),
        ActivityLevel.LIGHT to ActivityLevelInfo(1.375, "Light Exercise", "1–3 days/week light workout or brisk walking"),
        ActivityLevel.MODERATE to ActivityLevelInfo(1.55, "Moderate Exercise", "3–5 days/week moderate gym or sports"),
        ActivityLevel.ACTIVE to ActivityLevelInfo(1.725, "Active", "6–7 days/week hard exercise / physical job"),
        ActivityLevel.VERY_ACTIVE to ActivityLevelInfo(1.9, "Very Active", "Intense daily training / athlete / physical labor"),
    )

    val PROTEIN_GUIDELINES: Map<GoalType, ProteinGuideline> = mapOf(
        GoalType.LOSE to ProteinGuideline(1.1, 1.3, 1.2, "1.1–1.3 g/kg (Protects muscle during caloric deficit)"),
        GoalType.MAINTAIN to ProteinGuideline(1.0, 1.2, 1.1, "1.0–1.2 g/kg (General health & maintenance)"),
        GoalType.GAIN to ProteinGuideline(1.2, 1.6, 1.5, "1.2–1.6 g/kg (Muscle hypertrophy & lean mass building)"),
    )

    fun calculateTargets(settings: UserSettings): CalculatorResult {
        val w = settings.currentWeightKg
        val tw = settings.targetWeightKg
        val h = settings.heightCm
        val age = settings.ageYears

        // Mifflin-St Jeor BMR
        val sexOffset = when (settings.sex) {
            Sex.F -> -161
            Sex.OTHER -> -78
            Sex.M -> 5
        }
        val bmr = (10 * w + 6.25 * h - 5 * age + sexOffset).roundToInt()

        // TDEE
        val activityFactor = ACTIVITY_LEVELS[settings.activityLevel]?.factor ?: 1.2
        val tdee = (bmr * activityFactor).roundToInt()

        // Calorie adjustment: 1 kg body fat ≈ 7700 kcal (standard 500/250 kcal approximations)
        val calorieAdjustment = when (settings.goalType) {
            GoalType.LOSE -> {
                val rate = if (abs(settings.goalRateKgPerWeek) == 0.0) 0.5 else abs(settings.goalRateKgPerWeek)
                -((rate / 0.5) * 500).roundToInt()
            }
            GoalType.GAIN -> {
                val rate = if (abs(settings.goalRateKgPerWeek) == 0.0) 0.25 else abs(settings.goalRateKgPerWeek)
                ((rate / 0.25) * 250).roundToInt()
            }
            GoalType.MAINTAIN -> 0
        }

        // Sensible floor (1200 kcal for women, 1400 otherwise)
        val minSafeCalories = if (settings.sex == Sex.F) 1200 else 1400
        val recommendedCalories = maxOf(minSafeCalories, tdee + calorieAdjustment)

        // Protein target based on current or target weight
        val effectiveWeight = if (settings.proteinBasis == ProteinBasis.TARGET && tw > 0) tw else w
        val proteinMultiplier = PROTEIN_GUIDELINES[settings.goalType]?.default ?: 1.2
        val recommendedProtein = (effectiveWeight * proteinMultiplier).roundToInt()

        // Water: ~35 ml/kg + activity bonus, rounded to nearest 250 ml glass, min 2 L
        val activityWaterBonus = when (settings.activityLevel) {
            ActivityLevel.VERY_ACTIVE -> 750
            ActivityLevel.ACTIVE -> 500
            ActivityLevel.MODERATE -> 350
            else -> 0
        }
        val rawWaterMl = w * 35 + activityWaterBonus
        val recommendedWaterMl = maxOf(2000, (rawWaterMl / 250).roundToInt() * 250)

        // Formula strings for transparent user education
        val bmrFormula = when (settings.sex) {
            Sex.F -> "10 × ${w}kg + 6.25 × ${h}cm - 5 × ${age} - 161 = $bmr kcal/day"
            Sex.OTHER -> "10 × ${w}kg + 6.25 × ${h}cm - 5 × ${age} - 78 = $bmr kcal/day"
            Sex.M -> "10 × ${w}kg + 6.25 × ${h}cm - 5 × ${age} + 5 = $bmr kcal/day"
        }
        val tdeeFormula = "$bmr (BMR) × $activityFactor (${settings.activityLevel}) = $tdee kcal/day"

        val targetFormula = when (settings.goalType) {
            GoalType.LOSE ->
                "$tdee (TDEE) - ${abs(calorieAdjustment)} kcal (${settings.goalRateKgPerWeek} kg/wk deficit) = $recommendedCalories kcal"
            GoalType.GAIN ->
                "$tdee (TDEE) + $calorieAdjustment kcal (${settings.goalRateKgPerWeek} kg/wk surplus) = $recommendedCalories kcal"
            GoalType.MAINTAIN -> "$tdee kcal (Maintenance)"
        }

        val proteinBasisLabel = if (settings.proteinBasis == ProteinBasis.TARGET) "target" else "current"
        val proteinFormula =
            "${effectiveWeight}kg ($proteinBasisLabel weight) × $proteinMultiplier g/kg (${settings.goalType} goal) = $recommendedProtein g"

        val waterFormula =
            "${w}kg × 35ml + ${activityWaterBonus}ml (${settings.activityLevel}) = $recommendedWaterMl ml/day (~${String.format(Locale.US, "%.1f", recommendedWaterMl / 1000.0)}L)"

        return CalculatorResult(
            bmr = bmr,
            tdee = tdee,
            activityFactor = activityFactor,
            recommendedCalories = recommendedCalories,
            calorieAdjustment = calorieAdjustment,
            recommendedProtein = recommendedProtein,
            proteinMultiplier = proteinMultiplier,
            effectiveWeight = effectiveWeight,
            recommendedWaterMl = recommendedWaterMl,
            formulaDetails = FormulaDetails(
                bmrFormula = bmrFormula,
                tdeeFormula = tdeeFormula,
                targetFormula = targetFormula,
                proteinFormula = proteinFormula,
                waterFormula = waterFormula,
            ),
        )
    }
}
