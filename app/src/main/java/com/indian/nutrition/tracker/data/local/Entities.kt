package com.indian.nutrition.tracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One logged food serving (dates stored as ISO-8601 strings, matching web). */
@Entity(tableName = "daily_logs", indices = [Index("date")])
data class DailyLogEntity(
    @PrimaryKey val id: String,
    val date: String,
    val foodId: String,
    val foodName: String,
    val source: String,
    val servingGrams: Int,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val mealType: String,
    val createdAt: Long,
)

/** One weight measurement per date (date is unique — later entries replace). */
@Entity(tableName = "weight_logs", indices = [Index(value = ["date"], unique = true)])
data class WeightLogEntity(
    @PrimaryKey val id: String,
    val date: String,
    val weightKg: Double,
    val note: String?,
    val createdAt: Long,
)

/** A single water intake entry. */
@Entity(tableName = "water_logs", indices = [Index("date")])
data class WaterLogEntity(
    @PrimaryKey val id: String,
    val date: String,
    val amountMl: Int,
    val time: String?,
    val createdAt: Long,
)

/** A user-defined recipe/food. */
@Entity(tableName = "custom_foods")
data class CustomFoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double?,
    val typicalServingDescription: String?,
    val typicalServingGrams: Int?,
    val notes: String?,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "'GRAMS'")
    val servingUnit: String = "GRAMS",
)

/** Open Food Facts product cache entry — keyed by barcode or normalized name. */
@Entity(tableName = "off_cache")
data class OffCacheEntity(
    @PrimaryKey val key: String,
    val barcode: String?,
    val productName: String,
    val brand: String?,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val lastFetched: Long,
)
