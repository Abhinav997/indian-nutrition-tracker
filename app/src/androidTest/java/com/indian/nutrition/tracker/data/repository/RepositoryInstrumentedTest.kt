package com.indian.nutrition.tracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.indian.nutrition.tracker.data.local.AppDatabase
import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Instrumented tests for import/clear semantics of each repository. */
@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {

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

    @Test
    fun weightUpsertIsOneRowPerDate() = runTest {
        val repo = WeightRepository(db.weightLogDao())
        val first = repo.upsert(LocalDate.of(2026, 9, 5), 82.0, "morning")
        val second = repo.upsert(LocalDate.of(2026, 9, 5), 81.5, "evening")
        assertEquals(first.id, second.id)
        assertEquals(1, repo.observeAll().first().size)
        assertEquals(81.5, repo.observeAll().first().single().weightKg, 0.001)
    }

    @Test
    fun weightImportCollapsesDuplicateDates() = runTest {
        val repo = WeightRepository(db.weightLogDao())
        val date = LocalDate.of(2026, 9, 5)
        repo.importAll(listOf(
            WeightLog("w1", date, 82.0, "first", 1L),
            WeightLog("w2", date, 83.0, "second", 2L),
        ))
        val all = repo.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(83.0, all.single().weightKg, 0.001)
        assertEquals("second", all.single().note)
    }

    @Test
    fun logAndWaterImportKeepIdsAndReplaceOnConflict() = runTest {
        val logs = LogRepository(db.dailyLogDao())
        val waters = WaterRepository(db.waterLogDao())
        val date = LocalDate.of(2026, 9, 5)

        val food = DailyLog("keep-id", date, "nin_1", "Dal", FoodSource.NIN, 150, 180, 9.0, 28.0, 2.0, MealType.LUNCH, 1L)
        logs.importAll(listOf(food, food.copy(id = "second", foodName = "Kheer", createdAt = 2L)))
        assertEquals(setOf("keep-id", "second"), logs.observeAll().first().map { it.id }.toSet())

        // same id twice -> replace, no duplicate
        logs.importAll(listOf(food.copy(foodName = "Updated Dal", createdAt = 3L)))
        val foods = logs.observeAll().first()
        assertEquals(2, foods.size)
        assertEquals("Updated Dal", foods.first { it.id == "keep-id" }.foodName)

        val water = WaterLog("keep-water", date, 500, "12:00 PM", 1L)
        waters.importAll(listOf(water, water.copy(id = "w2", amountMl = 750, createdAt = 2L)))
        waters.importAll(listOf(water.copy(amountMl = 250, createdAt = 3L)))
        val allWater = waters.observeAll().first()
        assertEquals(2, allWater.size)
        assertEquals(250, allWater.first { it.id == "keep-water" }.amountMl)
    }

    @Test
    fun customImportAndClearAllTables() = runTest {
        val customs = CustomFoodRepository(db.customFoodDao())
        val logs = LogRepository(db.dailyLogDao())
        val waters = WaterRepository(db.waterLogDao())
        val weights = WeightRepository(db.weightLogDao())

        customs.importAll(listOf(CustomFood("c1", "Poha", 260.0, 6.0, 45.0, 8.0, null, "1 bowl", 150, null, 1L)))
        logs.add(LocalDate.of(2026, 9, 5), "nin_1", "Dal", "NIN", 150, 180, 9.0, 28.0, 2.0, MealType.LUNCH)
        waters.add(LocalDate.of(2026, 9, 5), 250, null)
        weights.upsert(LocalDate.of(2026, 9, 5), 82.0, null)

        assertEquals(1, customs.observeAll().first().size)
        assertTrue(logs.observeAll().first().isNotEmpty())
        assertTrue(waters.observeAll().first().isNotEmpty())
        assertTrue(weights.observeAll().first().isNotEmpty())

        customs.clear()
        logs.clear()
        waters.clear()
        weights.clear()

        assertTrue(customs.observeAll().first().isEmpty())
        assertTrue(logs.observeAll().first().isEmpty())
        assertTrue(waters.observeAll().first().isEmpty())
        assertTrue(weights.observeAll().first().isEmpty())
    }
}
