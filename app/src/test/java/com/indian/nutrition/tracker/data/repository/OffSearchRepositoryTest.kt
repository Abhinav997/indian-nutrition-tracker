package com.indian.nutrition.tracker.data.repository

import com.indian.nutrition.tracker.data.local.OffCacheDao
import com.indian.nutrition.tracker.data.local.OffCacheEntity
import com.indian.nutrition.tracker.data.remote.OffApiClient
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.OffCacheProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OffSearchRepositoryTest {

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

    private val amulCurated = Food(
        id = "pkg_amul_butter",
        name = "Amul Butter",
        source = FoodSource.OFF,
        kcalPer100g = 740.0,
        proteinPer100g = 0.6,
        carbsPer100g = 0.4,
        fatPer100g = 82.0,
        brand = "Amul",
        category = "Dairy",
    )

    private fun networkProduct(name: String, code: String) = """
        {"products":[{"code":"$code","product_name":"$name","brands":"Amul",
          "nutriments":{"energy-kcal_100g":740,"proteins_100g":0.6,
                        "carbohydrates_100g":0.4,"fat_100g":82}}]}
    """.trimIndent()

    private fun serverRepo(server: MockWebServer, dao: FakeOffCacheDao = FakeOffCacheDao()): Pair<OffSearchRepository, FakeOffCacheDao> {
        val cacheRepo = OffCacheRepository(dao)
        val apiClient = OffApiClient(
            inBaseUrl = server.url("/").toString(),
            worldBaseUrl = server.url("/").toString(),
        )
        return OffSearchRepository(
            offApiClient = apiClient,
            offCacheRepository = cacheRepo,
            packagedFoods = { listOf(amulCurated) },
        ) to dao
    }

    @Test
    fun shortQueriesReturnNothing() = runTest {
        val dao = FakeOffCacheDao()
        val repo = OffSearchRepository(
            offApiClient = OffApiClient("http://localhost:1/", "http://localhost:1/"),
            offCacheRepository = OffCacheRepository(dao),
            packagedFoods = { listOf(amulCurated) },
        )
        val result = repo.search("a")
        assertTrue(result.foods.isEmpty())
        assertFalse(result.fromCache)
        assertFalse(result.offline)
        assertTrue(dao.store.isEmpty())
    }

    @Test
    fun fallsBackToCuratedAndCacheWhenOffline() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))
        server.start()
        val (repo, dao) = serverRepo(server)
        try {
            // Seed a cached network item for "butter".
            dao.store["amul_butter"] = OffCacheEntity(
                key = "amul_butter", barcode = null, productName = "Amul Butter Lite",
                brand = "Amul", kcalPer100g = 300.0, proteinPer100g = 0.0,
                carbsPer100g = 0.0, fatPer100g = 33.0, lastFetched = 1L,
            )

            val result = repo.search("butter")
            assertTrue(result.fromCache)
            assertTrue(result.offline)
            // Curated wins over cached by name; only one "Amul Butter" row.
            assertEquals(2, result.foods.size)
            assertEquals("Amul Butter", result.foods.first { it.name == "Amul Butter" }.name)
            assertTrue(result.foods.any { it.name == "Amul Butter Lite" })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun networkSuccessCachesOnlyNetworkProducts() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(networkProduct("Amul Butter", "8901")))
        server.start()
        val (repo, dao) = serverRepo(server)
        try {
            val result = repo.search("butter")
            assertFalse(result.fromCache)
            assertFalse(result.offline)

            // Web bug: curated items were cached too; native keeps cache network-only.
            assertEquals(1, dao.store.size)
            assertTrue(dao.store.containsKey("8901"))
            // Curated + network merged into exactly one "Amul Butter" row.
            assertEquals(1, result.foods.count { it.name == "Amul Butter" })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun dedupeByNameCollapsesCaseAndWhitespace() {
        val packed = listOf(
            amulCurated,
            amulCurated.copy(name = "  amul   butter "),
            amulCurated.copy(name = "AMUL BUTTER"),
        )
        val result = OffSearchRepository.dedupeByName(packed)
        assertEquals(1, result.size)
        assertEquals("Amul Butter", result[0].name)
    }
}
