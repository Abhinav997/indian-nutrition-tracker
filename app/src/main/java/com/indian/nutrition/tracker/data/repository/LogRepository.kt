package com.indian.nutrition.tracker.data.repository

import com.indian.nutrition.tracker.data.local.DailyLogDao
import com.indian.nutrition.tracker.data.mapper.toDomain
import com.indian.nutrition.tracker.data.mapper.toEntity
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.MealType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/** Persists food-log entries. */
class LogRepository(private val dao: DailyLogDao) {

    fun observeAll(): Flow<List<DailyLog>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeForDate(date: LocalDate): Flow<List<DailyLog>> =
        dao.observeForDate(date.toString()).map { list -> list.map { it.toDomain() } }

    suspend fun add(
        date: LocalDate,
        foodId: String,
        foodName: String,
        source: String,
        servingGrams: Int,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        mealType: MealType,
    ): DailyLog {
        val log = DailyLog(
            id = "log_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
            date = date,
            foodId = foodId,
            foodName = foodName,
            source = com.indian.nutrition.tracker.domain.model.FoodSource.entries
                .firstOrNull { it.name == source }
                ?: com.indian.nutrition.tracker.domain.model.FoodSource.CUSTOM,
            servingGrams = servingGrams,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            mealType = mealType,
            createdAt = System.currentTimeMillis(),
        )
        dao.insert(log.toEntity())
        return log
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun clear() = dao.deleteAll()

    /** Bulk import (backup restore): keeps original ids, replaces on conflict. */
    suspend fun importAll(logs: List<DailyLog>) {
        logs.forEach { dao.insert(it.toEntity()) }
    }
}
