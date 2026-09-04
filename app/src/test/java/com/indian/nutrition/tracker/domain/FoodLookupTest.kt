package com.indian.nutrition.tracker.domain

import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FoodLookupTest {

    private fun food(id: String, name: String, source: FoodSource = FoodSource.NIN, brand: String? = null,
                     category: String? = null) = Food(
        id = id, name = name, source = source,
        kcalPer100g = 100.0, proteinPer100g = 10.0, carbsPer100g = 10.0, fatPer100g = 5.0,
        brand = brand, category = category,
    )

    // ---- master list / dedupe ----

    @Test
    fun masterListPrioritizesCustomOverCuratedAndCached() {
        val custom = CustomFood(
            id = "cust_1", name = "Amul Butter", kcalPer100g = 700.0, proteinPer100g = 0.0,
            carbsPer100g = 0.0, fatPer100g = 80.0, createdAt = 1L,
        )
        val packaged = food("pkg_amul_butter", "Amul Butter", FoodSource.OFF, brand = "Amul")
        val cached = food("off_8901262010054", "Amul Butter", FoodSource.OFF, brand = "Amul")

        val master = FoodLookup.masterList(
            customFoods = listOf(custom),
            ninFoods = listOf(food("nin_dal", "Dal")),
            packagedFoods = listOf(packaged),
            offCacheFoods = listOf(cached),
        )

        // Duplicate names are collapsed, keeping custom entry (100g serving mapping intact)
        assertEquals(2, master.size)
        assertEquals("cust_1", master[0].id)
        assertEquals(FoodSource.CUSTOM, master[0].source)
        assertEquals("nin_dal", master[1].id)
    }

    @Test
    fun masterListKeepsDistinctNamesFromAllSources() {
        val master = FoodLookup.masterList(
            customFoods = emptyList(),
            ninFoods = listOf(food("nin_dal", "Dal"), food("nin_rice", "Rice")),
            packagedFoods = listOf(food("pkg_maggi", "Maggi Noodles", FoodSource.OFF)),
            offCacheFoods = listOf(food("off_x", "Protein Bar", FoodSource.OFF)),
        )
        assertEquals(4, master.size)
    }

    // ---- search ----

    @Test
    fun searchMatchesNameBrandAndCategory() {
        val master = listOf(
            food("nin_dal", "Yellow Dal Tadka", category = "Dals & Legumes"),
            food("pkg_maggi", "Maggi Noodles", FoodSource.OFF, brand = "Nestle", category = "Noodles"),
        )
        assertEquals(listOf("pkg_maggi"), FoodLookup.search(master, "nestle").map { it.id })
        assertEquals(listOf("nin_dal"), FoodLookup.search(master, "DAL").map { it.id })
        assertEquals(listOf("pkg_maggi"), FoodLookup.search(master, "noodles").map { it.id })
        assertEquals(2, FoodLookup.search(master, "").size)
    }

    @Test
    fun searchAppliesSourceFilter() {
        val master = listOf(
            food("nin_dal", "Dal", FoodSource.NIN),
            food("pkg_sattu", "Sattu", FoodSource.OFF),
        )
        val ninOnly = FoodLookup.search(master, "a", FoodSource.NIN)
        assertTrue(ninOnly.all { it.source == FoodSource.NIN })
        assertFalse(ninOnly.any { it.source == FoodSource.OFF })
    }

    // ---- frequency ----

    private fun log(foodId: String, n: Int = 1): List<DailyLog> =
        (1..n).map { i ->
            DailyLog(
                id = "$foodId-$i", date = LocalDate.of(2026, 9, 1), foodId = foodId,
                foodName = foodId, source = FoodSource.NIN, servingGrams = 100,
                calories = 100, protein = 10.0, carbs = 10.0, fat = 5.0,
                mealType = MealType.LUNCH, createdAt = i.toLong(),
            )
        }

    @Test
    fun frequentlyUsedRanksByCount() {
        val master = listOf(
            food("nin_dal", "Dal"),
            food("nin_rice", "Rice"),
            food("nin_roti", "Roti"),
        )
        val logs = log("nin_rice", 5) + log("nin_dal", 2)
        val result = FoodLookup.frequentlyUsed(master, master, logs, limit = 3)
        assertEquals(listOf("nin_rice", "nin_dal", "nin_roti"), result.map { it.id })
    }

    @Test
    fun frequentlyUsedFallsBackToStaples() {
        val master = listOf(food("nin_dal", "Dal"), food("nin_roti", "Roti"), food("nin_rice", "Rice"))
        val result = FoodLookup.frequentlyUsed(master, master, emptyList(), limit = 2)
        assertEquals(listOf("nin_dal", "nin_roti"), result.map { it.id })
    }
}
