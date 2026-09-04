package com.indian.nutrition.tracker.data.repository

import com.indian.nutrition.tracker.data.local.WeightLogDao
import com.indian.nutrition.tracker.data.local.WeightLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeightRepositoryTest {

    private class FakeWeightLogDao : WeightLogDao {
        val store = mutableMapOf<String, WeightLogEntity>()

        override fun observeAll(): Flow<List<WeightLogEntity>> =
            MutableStateFlow(store.values.sortedBy { it.date })
        override suspend fun findByDate(date: String): WeightLogEntity? =
            store.values.firstOrNull { it.date == date }
        override suspend fun insert(entity: WeightLogEntity) { store[entity.id] = entity }
        override suspend fun update(id: String, weightKg: Double, note: String?, createdAt: Long) {
            val existing = store[id] ?: return
            store[id] = existing.copy(weightKg = weightKg, note = note, createdAt = createdAt)
        }
        override suspend fun deleteById(id: String) { store.remove(id) }
        override suspend fun deleteAll() { store.clear() }
    }

    @Test
    fun upsertSameDateUpdatesInsteadOfInserting() = runTest {
        val dao = FakeWeightLogDao()
        val repo = WeightRepository(dao)
        val date = LocalDate.of(2026, 9, 1)

        val first = repo.upsert(date, 82.0, "morning")
        val second = repo.upsert(date, 81.4, "evening")

        assertEquals(1, dao.store.size)
        assertEquals(first.id, second.id) // stable id (improvement over web)
        assertEquals(81.4, second.weightKg, 0.001)
        assertEquals("evening", second.note)
    }

    @Test
    fun upsertDifferentDatesKeepsBothSorted() = runTest {
        val dao = FakeWeightLogDao()
        val repo = WeightRepository(dao)
        repo.upsert(LocalDate.of(2026, 9, 2), 82.0, null)
        repo.upsert(LocalDate.of(2026, 9, 1), 83.0, null)

        val logs = repo.observeAll().first()
        assertEquals(2, logs.size)
        assertEquals(LocalDate.of(2026, 9, 1), logs[0].date)
        assertEquals(LocalDate.of(2026, 9, 2), logs[1].date)
    }

    @Test
    fun deleteRemovesEntry() = runTest {
        val dao = FakeWeightLogDao()
        val repo = WeightRepository(dao)
        val log = repo.upsert(LocalDate.of(2026, 9, 1), 82.0, null)
        repo.delete(log.id)
        assertTrue(dao.store.isEmpty())
    }
}
