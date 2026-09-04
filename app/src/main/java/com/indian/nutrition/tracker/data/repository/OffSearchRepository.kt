package com.indian.nutrition.tracker.data.repository

import com.indian.nutrition.tracker.data.mapper.toFood
import com.indian.nutrition.tracker.data.remote.OffApiClient
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.OffCacheProduct

/** Result of a combined local + network food search. */
data class OffSearchResult(
    val foods: List<Food>,
    /** True when no live OFF result was used (only curated + cached items). */
    val fromCache: Boolean,
    /** True when a network attempt failed and we fell back to local data. */
    val offline: Boolean,
)

/**
 * Orchestrates an OFF lookup the way the web app did, with the dirty data
 * fixed:
 * - curated packaged + cached OFF items are always shown as fallbacks
 * - merged lists are deduped by normalized name (web produced duplicate
 *   rows when the same product appeared in cache and in live results)
 * - only genuine network results are written to the OFF cache (web also
 *   cached curated items, which is why the cache was unusable offline for
 *   some entries and silently grew)
 */
class OffSearchRepository(
    private val offApiClient: OffApiClient,
    private val offCacheRepository: OffCacheRepository,
    private val packagedFoods: () -> List<Food>,
) {

    suspend fun search(query: String): OffSearchResult {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            return OffSearchResult(emptyList(), fromCache = false, offline = false)
        }
        val q = trimmed.lowercase()

        val packagedMatches = packagedFoods().filter { it.matches(q) }
        val cachedMatches = offCacheRepository.searchLocal(trimmed).map { it.toFood() }
        val localFallbacks = dedupeByName(packagedMatches + cachedMatches)

        return try {
            val fetched = offApiClient.search(trimmed)
            if (fetched.isEmpty()) {
                OffSearchResult(localFallbacks, fromCache = true, offline = true)
            } else {
                // Network-only cache writes.
                offCacheRepository.remember(fetched.map { it.toCacheProduct() })
                OffSearchResult(
                    foods = dedupeByName(localFallbacks + fetched),
                    fromCache = false,
                    offline = false,
                )
            }
        } catch (_: Exception) {
            OffSearchResult(
                foods = localFallbacks,
                fromCache = true,
                offline = localFallbacks.isNotEmpty(),
            )
        }
    }

    private fun Food.matches(q: String): Boolean =
        name.lowercase().contains(q) ||
            (brand?.lowercase()?.contains(q) == true) ||
            (category?.lowercase()?.contains(q) == true)

    private fun Food.toCacheProduct(): OffCacheProduct = OffCacheProduct(
        key = OffCacheRepository.keyFor(barcode, name),
        barcode = barcode,
        productName = name,
        brand = brand,
        kcalPer100g = kcalPer100g,
        proteinPer100g = proteinPer100g,
        carbsPer100g = carbsPer100g,
        fatPer100g = fatPer100g,
        lastFetched = System.currentTimeMillis(),
    )

    companion object {
        /**
         * Name-based dedupe (web parity: the merge key is the lowercased
         * name; first occurrence — curated/cached — wins). Also trims, so
         * "Amul  Butter" and "amul butter" collapse — the web bug fix.
         */
        fun dedupeByName(foods: List<Food>): List<Food> {
            val seen = HashSet<String>()
            return foods.filter { f -> seen.add(f.name.lowercase().trim()) }
        }
    }
}
