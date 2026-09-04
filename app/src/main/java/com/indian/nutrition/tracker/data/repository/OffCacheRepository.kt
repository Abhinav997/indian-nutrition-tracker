package com.indian.nutrition.tracker.data.repository

import com.indian.nutrition.tracker.data.local.OffCacheDao
import com.indian.nutrition.tracker.data.local.OffCacheEntity
import com.indian.nutrition.tracker.data.mapper.toDomain
import com.indian.nutrition.tracker.data.mapper.toFood
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.OffCacheProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Open Food Facts cache with the web-bug fixes applied:
 * - only genuine OFF network results are stored (callers must not pass curated items)
 * - bounded: 30-day TTL + max 500 rows (oldest evicted)
 * - keyed by barcode or normalized name (no silent overwrites of unrelated products)
 */
class OffCacheRepository(private val dao: OffCacheDao) {

    companion object {
        const val TTL_MS = 1000L * 60 * 60 * 24 * 30       // 30 days
        const val MAX_ENTRIES = 500

        /** Canonical cache key for a product. */
        fun keyFor(barcode: String?, productName: String): String =
            (barcode ?: productName.lowercase().trim().replace(Regex("\\s+"), "_"))
    }

    fun observeAll(): Flow<List<OffCacheProduct>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<OffCacheProduct> = dao.getAll().map { it.toDomain() }

    /** Store network results, then enforce TTL + size caps. */
    suspend fun remember(products: List<OffCacheProduct>) {
        val now = System.currentTimeMillis()
        products.forEach { product ->
            dao.upsert(
                OffCacheEntity(
                    key = product.key,
                    barcode = product.barcode,
                    productName = product.productName,
                    brand = product.brand,
                    kcalPer100g = product.kcalPer100g,
                    proteinPer100g = product.proteinPer100g,
                    carbsPer100g = product.carbsPer100g,
                    fatPer100g = product.fatPer100g,
                    lastFetched = now,
                )
            )
        }
        enforceLimits(now)
    }

    /** Evict stale rows and trim to [MAX_ENTRIES]. Exposed for unit testing. */
    suspend fun enforceLimits(now: Long) {
        dao.deleteOlderThan(now - TTL_MS)
        val over = dao.count() - MAX_ENTRIES
        if (over > 0) dao.deleteOldest(over)
    }

    /** Search the local cache (name/brand). */
    suspend fun searchLocal(query: String): List<OffCacheProduct> {
        val q = query.trim().lowercase()
        return dao.getAll()
            .map { it.toDomain() }
            .filter {
                q.isEmpty() ||
                    it.productName.lowercase().contains(q) ||
                    (it.brand?.lowercase()?.contains(q) == true)
            }
    }

    suspend fun clear() = dao.deleteAll()

    /** Convert cached products to Foods (id prefix `off_`). */
    fun toFoods(products: List<OffCacheProduct>): List<Food> = products.map { it.toFood() }
}
