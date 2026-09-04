package com.indian.nutrition.tracker.data.repository

import com.indian.nutrition.tracker.data.local.OffCacheDao
import com.indian.nutrition.tracker.data.local.OffCacheEntity
import com.indian.nutrition.tracker.domain.model.OffCacheProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OffCacheRepositoryTest {

    private class FakeOffCacheDao : OffCacheDao {
        val store = mutableMapOf<String, OffCacheEntity>()

        override suspend fun upsert(entity: OffCacheEntity) { store[entity.key] = entity }
        override fun observeAll(): Flow<List<OffCacheEntity>> =
            MutableStateFlow(store.values.toList())
        override suspend fun getAll(): List<OffCacheEntity> = store.values.toList()
        override suspend fun deleteOlderThan(cutoff: Long) {
            store.entries.removeAll { it.value.lastFetched < cutoff }
        }
        override suspend fun deleteOldest(count: Int) {
            store.values.sortedBy { it.lastFetched }.take(count).forEach { store.remove(it.key) }
        }
        override suspend fun count(): Int = store.size
        override suspend fun deleteAll() { store.clear() }
    }

    private fun product(key: String, barcode: String?, name: String, ts: Long) = OffCacheProduct(
        key = key, barcode = barcode, productName = name, brand = "Amul",
        kcalPer100g = 90.0, proteinPer100g = 3.0, carbsPer100g = 4.0, fatPer100g = 6.0,
        lastFetched = ts,
    )

    @Test
    fun keyForUsesBarcodeOrNormalizedName() {
        assertEquals("8901262010054", OffCacheRepository.keyFor("8901262010054", "Amul Butter"))
        assertEquals("amul_butter", OffCacheRepository.keyFor(null, "  Amul  Butter "))
    }

    @Test
    fun rememberUpsertsAndSearchLocalMatchesName() = runTest {
        val dao = FakeOffCacheDao()
        val repo = OffCacheRepository(dao)
        repo.remember(listOf(product("k1", null, "Amul Butter", 1L)))
        repo.remember(listOf(product("k1", null, "Amul Butter", 2L))) // same key → upsert

        assertEquals(1, dao.store.size)
        assertEquals(1, repo.searchLocal("butter").size)
        assertEquals(0, repo.searchLocal("maggi").size)
    }

    @Test
    fun enforceLimitsEvictsStaleAndCapsSize() = runTest {
        val dao = FakeOffCacheDao()
        val repo = OffCacheRepository(dao)
        val now = 10_000_000L

        // 600 fresh entries
        repeat(600) { i ->
            dao.store["k$i"] = OffCacheEntity(
                key = "k$i", barcode = null, productName = "P$i", brand = null,
                kcalPer100g = 1.0, proteinPer100g = 0.0, carbsPer100g = 0.0, fatPer100g = 0.0,
                lastFetched = now,
            )
        }
        repo.enforceLimits(now)
        assertEquals(500, dao.count())

        // stale entry evicted
        dao.store["stale"] = dao.store["k0"]!!.copy(key = "stale", lastFetched = 1L)
        repo.enforceLimits(now)
        assertTrue(!dao.store.containsKey("stale"))
    }

    @Test
    fun clearRemovesAll() = runTest {
        val dao = FakeOffCacheDao()
        val repo = OffCacheRepository(dao)
        repo.remember(listOf(product("k1", null, "Amul Butter", 1L)))
        repo.clear()
        assertEquals(0, dao.count())
        assertTrue(repo.observeAll().first().isEmpty())
    }
}
