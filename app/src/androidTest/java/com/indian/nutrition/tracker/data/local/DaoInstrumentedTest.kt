package com.indian.nutrition.tracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for every Room DAO (in-memory database). */
@RunWith(AndroidJUnit4::class)
class DaoInstrumentedTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun daily(id: String, date: String, name: String = "Dal", createdAt: Long = 1L) = DailyLogEntity(
        id = id, date = date, foodId = "nin_1", foodName = name, source = "NIN",
        servingGrams = 150, calories = 180, protein = 9.0, carbs = 28.0, fat = 2.0,
        mealType = "LUNCH", createdAt = createdAt,
    )

    @Test
    fun dailyLogInsertReplaceAndDelete() = runTest {
        val dao = db.dailyLogDao()
        dao.insert(daily("a", "2026-09-05"))
        dao.insert(daily("b", "2026-09-01"))
        // observeAll is ordered date DESC, createdAt ASC
        val all = dao.observeAll().first()
        assertEquals(listOf("a", "b"), all.map { it.id })

        // same id replaces rather than duplicating
        dao.insert(daily("a", "2026-09-05", name = "Kheer", createdAt = 2L))
        val afterReplace = dao.observeAll().first()
        assertEquals(2, afterReplace.size)
        assertEquals("Kheer", afterReplace.first { it.id == "a" }.foodName)

        dao.deleteById("a")
        assertEquals(listOf("b"), dao.getAll().map { it.id })

        val forDate = dao.observeForDate("2026-09-01").first()
        assertEquals(listOf("b"), forDate.map { it.id })

        dao.deleteAll()
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun weightUpsertUpdateAndUniqueDate() = runTest {
        val dao = db.weightLogDao()
        dao.insert(WeightLogEntity("w1", "2026-09-05", 82.0, "morning", 1L))
        assertEquals(82.0, dao.findByDate("2026-09-05")?.weightKg ?: 0.0, 0.001)

        // same id replaces
        dao.insert(WeightLogEntity("w1", "2026-09-05", 82.5, "evening", 2L))
        assertEquals(82.5, dao.findByDate("2026-09-05")?.weightKg ?: 0.0, 0.001)

        dao.update("w1", 83.0, null, 3L)
        val updated = dao.findByDate("2026-09-05")
        assertEquals(83.0, updated?.weightKg ?: 0.0, 0.001)
        assertNull(updated?.note)

        // one row per date: a second entity with a different id but same date must fail
        // (unique index) — repositories route through findByDate/upsert instead.
        var conflict = false
        try {
            dao.insert(WeightLogEntity("w2", "2026-09-05", 90.0, null, 4L))
        } catch (_: Exception) {
            conflict = true
        }
        assertTrue(conflict)

        dao.deleteById("w1")
        assertNull(dao.findByDate("2026-09-05"))
        dao.deleteAll()
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun waterInsertReplaceAndOrdering() = runTest {
        val dao = db.waterLogDao()
        dao.insert(WaterLogEntity("x1", "2026-09-05", 250, "7:00 AM", 1L))
        dao.insert(WaterLogEntity("x2", "2026-09-05", 500, "12:00 PM", 2L))
        assertEquals(listOf("x1", "x2"), dao.observeForDate("2026-09-05").first().map { it.id })

        dao.insert(WaterLogEntity("x1", "2026-09-05", 750, "7:00 AM", 3L))
        val list = dao.observeAll().first()
        assertEquals(2, list.size)
        assertEquals(750, list.first { it.id == "x1" }.amountMl)

        dao.deleteById("x2")
        assertEquals(1, dao.observeAll().first().size)
        dao.deleteAll()
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun customFoodInsertUpdateAndClear() = runTest {
        val dao = db.customFoodDao()
        dao.insert(CustomFoodEntity(
            "c1", "Paneer", 290.0, 18.0, 4.0, 22.0, 1.0, "1 bowl", 150, "recipe", 1L,
        ))
        dao.insert(CustomFoodEntity(
            "c2", "Ragi", 350.0, 10.0, 70.0, 2.0, null, null, null, null, 2L,
        ))
        assertEquals(listOf("c2", "c1"), dao.observeAll().first().map { it.id })

        dao.update("c1", "Grilled Paneer", 300.0, 20.0, 4.0, 24.0, 0.5, "2 slices", 100, "grilled", 3L)
        val updated = dao.observeAll().first().first { it.id == "c1" }
        assertEquals("Grilled Paneer", updated.name)
        assertEquals(300.0, updated.kcalPer100g, 0.001)

        dao.deleteById("c1")
        assertEquals(listOf("c2"), dao.observeAll().first().map { it.id })
        dao.deleteAll()
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun offCacheEvictionAndCounting() = runTest {
        val dao = db.offCacheDao()
        fun entry(key: String, fetched: Long) = OffCacheEntity(
            key = key, barcode = key, productName = "P $key", brand = null,
            kcalPer100g = 100.0, proteinPer100g = 5.0, carbsPer100g = 10.0, fatPer100g = 2.0,
            lastFetched = fetched,
        )
        dao.upsert(entry("a", 1000L))
        dao.upsert(entry("b", 2000L))
        dao.upsert(entry("c", 3000L))
        assertEquals(3, dao.count())

        dao.deleteOlderThan(2000L)
        assertEquals(listOf("b", "c"), dao.getAll().map { it.key })

        dao.deleteOldest(1)
        assertEquals(listOf("c"), dao.getAll().map { it.key })

        // upsert replaces, keeping count stable
        dao.upsert(entry("c", 4000L))
        assertEquals(1, dao.count())
        assertNotNull(dao.getAll().first().productName)

        dao.deleteAll()
        assertEquals(0, dao.count())
    }
}
