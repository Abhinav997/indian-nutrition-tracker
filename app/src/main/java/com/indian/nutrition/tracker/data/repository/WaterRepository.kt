package com.indian.nutrition.tracker.data.repository

import com.indian.nutrition.tracker.data.local.WaterLogDao
import com.indian.nutrition.tracker.data.mapper.toDomain
import com.indian.nutrition.tracker.data.mapper.toEntity
import com.indian.nutrition.tracker.domain.model.WaterLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Persists water intake entries. */
class WaterRepository(private val dao: WaterLogDao) {

    fun observeAll(): Flow<List<WaterLog>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeForDate(date: LocalDate): Flow<List<WaterLog>> =
        dao.observeForDate(date.toString()).map { list -> list.map { it.toDomain() } }

    suspend fun add(date: LocalDate, amountMl: Int, time: String?): WaterLog {
        val timeStr = time ?: LocalDate.now()
            .atTime(java.time.LocalTime.now())
            .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
        val log = WaterLog(
            id = "water_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
            date = date,
            amountMl = amountMl,
            time = timeStr,
            createdAt = System.currentTimeMillis(),
        )
        dao.insert(log.toEntity())
        return log
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun clear() = dao.deleteAll()
}
