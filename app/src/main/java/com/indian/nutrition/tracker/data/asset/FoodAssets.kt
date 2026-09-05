package com.indian.nutrition.tracker.data.asset

import android.content.Context
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.ServingUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Serialized shape of the food asset JSON files (app/src/main/assets/data/). */
@Serializable
data class FoodDto(
    val id: String,
    val name: String,
    val source: String,
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
    val servingUnit: String? = null,
)

/** Loads and parses the curated food datasets shipped with the app. */
object FoodAssets {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true          // tolerate non-standard numeric literals
        coerceInputValues = true  // null for missing/invalid optional fields
    }

    private fun FoodDto.toFood() = Food(
        id = id,
        name = name,
        source = FoodSource.entries.firstOrNull { it.name == source } ?: FoodSource.OFF,
        kcalPer100g = kcalPer100g,
        proteinPer100g = proteinPer100g,
        carbsPer100g = carbsPer100g,
        fatPer100g = fatPer100g,
        fiberPer100g = fiberPer100g,
        typicalServingDescription = typicalServingDescription,
        typicalServingGrams = typicalServingGrams,
        brand = brand,
        category = category,
        barcode = barcode,
        imageUrl = imageUrl,
        servingUnit = ServingUnit.entries.firstOrNull { it.name == this@toFood.servingUnit }
            ?: ServingUnit.GRAMS,
    )

    private fun load(context: Context, asset: String): List<Food> {
        val text = context.assets.open(asset).bufferedReader().use { it.readText() }
        return json.decodeFromString<List<FoodDto>>(text).map { it.toFood() }
    }

    /** Loads the NIN/IFCT + curated packaged datasets once and caches the result. */
    fun loadAll(context: Context): Pair<List<Food>, List<Food>> {
        val nin = load(context, "data/nin_ifct.json")
        val packaged = load(context, "data/packaged_foods.json")
        return nin to packaged
    }
}
