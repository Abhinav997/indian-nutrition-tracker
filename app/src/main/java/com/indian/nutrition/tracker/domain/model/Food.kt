package com.indian.nutrition.tracker.domain.model

/** Where a food entry comes from. */
enum class FoodSource { NIN, OFF, CUSTOM }

/** Meal slots used for grouping daily logs. */
enum class MealType(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    SNACK("Snack"),
    DINNER("Dinner"),
}

/** A food item from any source (NIN/IFCT, curated packaged, Open Food Facts, custom). */
data class Food(
    val id: String,
    val name: String,
    val source: FoodSource,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double? = null,
    val typicalServingDescription: String? = null,
    val typicalServingGrams: Int? = null,
    val brand: String? = null,
    val category: String? = null,
    val barcode: String? = null,
    val imageUrl: String? = null,
)
