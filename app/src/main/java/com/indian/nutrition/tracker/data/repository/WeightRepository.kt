package com.indian.nutrition.tracker.data.repository

import com.indian.nutrition.tracker.data.local.WeightLogDao
import com.indian.nutrition.tracker.data.mapper.toDomain
import com.indian.nutrition.tracker.data.mapper.toEntity
import com.indian.nutrition.tracker.domain.model.WeightLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/**
 * Persists weight measurements. One entry per date — logging twice on the
 * same day updates the existing row (web parity, but keeps a stable id).
 */
class WeightRepository(private val dao: WeightLogDao) {

    fun observeAll(): Flow<List<WeightLog>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun upsert(date: LocalDate, weightKg: Double, note: String?): WeightLog {
        val existing = dao.findByDate(date.toString())
        val now = System.currentTimeMillis()
        return if (existing != null) {
            dao.update(existing.id, weightKg, note, now)
            existing.copy(weightKg = weightKg, note = note, createdAt = now).toDomain()
        } else {
            val log = WeightLog(
                id = "w_${now}_${UUID.randomUUID().toString().take(8)}",
                date = date,
                weightKg = weightKg,
                note = note,
                createdAt = now,
            )
            dao.insert(log.toEntity())
            log
        }
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun clear() = dao.deleteAll()
}
