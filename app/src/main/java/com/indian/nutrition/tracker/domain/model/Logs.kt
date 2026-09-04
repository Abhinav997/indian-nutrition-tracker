package com.indian.nutrition.tracker.domain.model

import java.time.LocalDate

/** One logged food serving for a given date. */
data class DailyLog(
    val id: String,
    val date: LocalDate,
    val foodId: String,
    val foodName: String,
    val source: FoodSource,
    val servingGrams: Int,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val mealType: MealType,
    val createdAt: Long,
)

/** A body-weight measurement for a date (one per date; latest replaces earlier ones). */
data class WeightLog(
    val id: String,
    val date: LocalDate,
    val weightKg: Double,
    val note: String? = null,
    val createdAt: Long,
)

/** A single water intake entry. */
data class WaterLog(
    val id: String,
    val date: LocalDate,
    val amountMl: Int,
    val time: String? = null,
    val createdAt: Long,
)

/** A user-defined recipe/food with custom macros (per 100 g). */
data class CustomFood(
    val id: String,
    val name: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double? = null,
    val typicalServingDescription: String? = null,
    val typicalServingGrams: Int? = null,
    val notes: String? = null,
    val createdAt: Long,
)
