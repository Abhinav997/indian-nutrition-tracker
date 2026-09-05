package com.indian.nutrition.tracker.util

import com.indian.nutrition.tracker.domain.model.UnitSystem
import java.util.Locale
import kotlin.math.roundToInt

/** Weight unit conversions and BMI. Matches the web app's math. */
object UnitConverters {

    private const val KG_PER_LB = 0.45359237
    private const val LB_PER_KG = 2.20462

    /** kg → lb (1 decimal, as the web app did). */
    fun kgToLb(kg: Double): Double = NumberUtils.round1(kg * LB_PER_KG)

    /** lb → kg (1 decimal). */
    fun lbToKg(lb: Double): Double = NumberUtils.round1(lb / LB_PER_KG)

    /** Format a weight in the user's selected unit, e.g. "82.0 kg" or "180.8 lb". */
    fun formatWeight(kg: Double, unit: UnitSystem): String =
        when (unit) {
            UnitSystem.KG -> String.format(Locale.US, "%.1f kg", kg)
            UnitSystem.LB -> String.format(Locale.US, "%.1f lb", kgToLb(kg))
        }

    /** BMI category as computed by the web app. */
    enum class BmiCategory { UNKNOWN, UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }

    data class BmiResult(val bmi: Double, val category: BmiCategory)

    /**
     * BMI = weight(kg) / height(m)² with the same cut-offs as the web app:
     * <18.5 underweight, <24.9 normal, <29.9 overweight, else obese.
     */
    fun calculateBmi(weightKg: Double, heightCm: Double): BmiResult? {
        if (weightKg <= 0.0 || heightCm <= 0.0) return null
        val heightM = heightCm / 100.0
        val bmi = (weightKg / (heightM * heightM) * 10).roundToInt() / 10.0
        val category = when {
            bmi < 18.5 -> BmiCategory.UNDERWEIGHT
            bmi < 24.9 -> BmiCategory.NORMAL
            bmi < 29.9 -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }
        return BmiResult(bmi, category)
    }
}
