package com.indian.nutrition.tracker.domain

import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.FoodSource

/**
 * Pure food-selection logic (port of the web app's unified master / search /
 * frequently-used behavior, with the duplicate-row bug fixed). Kept free of
 * Android dependencies so it is unit-testable.
 */
object FoodLookup {

    /**
     * Unified master list with priority order: custom → NIN → packaged → OFF
     * cache. Duplicates by normalized name are collapsed keeping the first
     * (highest priority) entry.
     */
    fun masterList(
        customFoods: List<CustomFood>,
        ninFoods: List<Food>,
        packagedFoods: List<Food>,
        offCacheFoods: List<Food>,
    ): List<Food> {
        val custom = customFoods.map { c ->
            Food(
                id = c.id,
                name = c.name,
                source = FoodSource.CUSTOM,
                kcalPer100g = c.kcalPer100g,
                proteinPer100g = c.proteinPer100g,
                carbsPer100g = c.carbsPer100g,
                fatPer100g = c.fatPer100g,
                fiberPer100g = c.fiberPer100g,
                typicalServingDescription = c.typicalServingDescription ?: "1 serving",
                typicalServingGrams = c.typicalServingGrams ?: 100,
                category = "Custom Foods",
            )
        }
        return dedupeByName(custom + ninFoods + packagedFoods + offCacheFoods)
    }

    /** Case-insensitive search over name/brand/category with optional source filter. */
    fun search(
        master: List<Food>,
        query: String,
        source: FoodSource? = null,
    ): List<Food> {
        val q = query.trim().lowercase()
        return master.filter { food ->
            val matchesQuery = q.isEmpty() ||
                food.name.lowercase().contains(q) ||
                (food.brand?.lowercase()?.contains(q) == true) ||
                (food.category?.lowercase()?.contains(q) == true)
            val matchesSource = source == null || food.source == source
            matchesQuery && matchesSource
        }
    }

    /**
     * Frequently-used ranking by log count, padded with [ninStaples] when the
     * user has fewer than [limit] distinct foods in history.
     */
    fun frequentlyUsed(
        master: List<Food>,
        ninStaples: List<Food>,
        logs: List<DailyLog>,
        limit: Int = 12,
    ): List<Food> {
        val counts = logs.groupingBy { it.foodId }.eachCount()
        val byId = master.associateBy { it.id }
        val ranked = counts.entries
            .sortedByDescending { it.value }
            .mapNotNull { byId[it.key] }

        val result = ranked.toMutableList()
        for (food in ninStaples) {
            if (result.size >= limit) break
            if (result.none { it.id == food.id }) result.add(food)
        }
        return result.take(limit)
    }

    private fun dedupeByName(foods: List<Food>): List<Food> {
        val seen = HashSet<String>()
        return foods.filter { f -> seen.add(f.name.lowercase().trim()) }
    }
}
