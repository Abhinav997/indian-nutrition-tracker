package com.indian.nutrition.tracker.data.repository

import android.content.Context
import com.indian.nutrition.tracker.data.asset.FoodAssets
import com.indian.nutrition.tracker.data.mapper.toFood
import com.indian.nutrition.tracker.domain.FoodLookup
import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.OffCacheProduct

/**
 * Loads the curated food datasets from assets and exposes the unified
 * master/search/frequency logic (pure logic lives in [FoodLookup]).
 */
class FoodRepository(context: Context) {

    private val appContext = context.applicationContext

    @Volatile
    private var curated: List<Food>? = null

    /** Loads NIN + curated packaged datasets once (thread-safe). */
    fun ensureLoaded(): List<Food> {
        curated?.let { return it }
        synchronized(this) {
            curated?.let { return it }
            val (nin, packaged) = FoodAssets.loadAll(appContext)
            curated = nin + packaged
            return curated!!
        }
    }

    fun ninFoods(): List<Food> = ensureLoaded().filter { it.source == FoodSource.NIN }

    fun packagedFoods(): List<Food> = ensureLoaded().filter { it.source == FoodSource.OFF }

    fun masterList(
        customFoods: List<CustomFood>,
        offCache: List<OffCacheProduct>,
    ): List<Food> = FoodLookup.masterList(
        customFoods = customFoods,
        ninFoods = ensureLoaded().filter { it.source == FoodSource.NIN },
        packagedFoods = ensureLoaded().filter { it.source == FoodSource.OFF },
        offCacheFoods = offCache.map { it.toFood() },
    )

    fun search(
        master: List<Food>,
        query: String,
        source: FoodSource? = null,
    ): List<Food> = FoodLookup.search(master, query, source)

    fun frequentlyUsed(
        master: List<Food>,
        logs: List<DailyLog>,
        limit: Int = 12,
    ): List<Food> = FoodLookup.frequentlyUsed(
        master = master,
        ninStaples = ninFoods().take(8),
        logs = logs,
        limit = limit,
    )
}
