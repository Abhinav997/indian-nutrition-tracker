package com.indian.nutrition.tracker.domain.model

/** Human-readable formula breakdown shown to the user on the calculator screen. */
data class FormulaDetails(
    val bmrFormula: String,
    val tdeeFormula: String,
    val targetFormula: String,
    val proteinFormula: String,
    val waterFormula: String,
)

/** Output of the Mifflin-St Jeor based target calculation. */
data class CalculatorResult(
    val bmr: Int,
    val tdee: Int,
    val activityFactor: Double,
    val recommendedCalories: Int,
    val calorieAdjustment: Int,
    val recommendedProtein: Int,
    val proteinMultiplier: Double,
    val effectiveWeight: Double,
    val recommendedWaterMl: Int,
    val formulaDetails: FormulaDetails,
)
