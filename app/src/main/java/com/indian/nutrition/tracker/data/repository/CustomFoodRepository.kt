package com.indian.nutrition.tracker.data.repository

import com.indian.nutrition.tracker.data.local.CustomFoodDao
import com.indian.nutrition.tracker.data.mapper.toDomain
import com.indian.nutrition.tracker.data.mapper.toEntity
import com.indian.nutrition.tracker.domain.model.CustomFood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Persists user-defined custom foods/recipes. */
class CustomFoodRepository(private val dao: CustomFoodDao) {

    fun observeAll(): Flow<List<CustomFood>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun add(food: CustomFood): CustomFood {
        val newFood = food.copy(
            id = "cust_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
            createdAt = System.currentTimeMillis(),
        )
        dao.insert(newFood.toEntity())
        return newFood
    }

    suspend fun update(food: CustomFood) {
        dao.update(
            id = food.id,
            name = food.name,
            kcal = food.kcalPer100g,
            protein = food.proteinPer100g,
            carbs = food.carbsPer100g,
            fat = food.fatPer100g,
            fiber = food.fiberPer100g,
            servingDesc = food.typicalServingDescription,
            servingGrams = food.typicalServingGrams,
            notes = food.notes,
            servingUnit = food.servingUnit.name,
        )
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun clear() = dao.deleteAll()

    /** Bulk import (backup restore): keeps original ids, replaces on conflict. */
    suspend fun importAll(foods: List<CustomFood>) {
        foods.forEach { dao.insert(it.toEntity()) }
    }
}
