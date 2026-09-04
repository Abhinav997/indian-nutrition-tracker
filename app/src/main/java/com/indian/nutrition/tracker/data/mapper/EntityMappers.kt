package com.indian.nutrition.tracker.data.mapper

import com.indian.nutrition.tracker.data.local.CustomFoodEntity
import com.indian.nutrition.tracker.data.local.DailyLogEntity
import com.indian.nutrition.tracker.data.local.OffCacheEntity
import com.indian.nutrition.tracker.data.local.WaterLogEntity
import com.indian.nutrition.tracker.data.local.WeightLogEntity
import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.domain.model.OffCacheProduct
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import java.time.LocalDate

/** Room entities ⇄ domain model mappers. Dates stay ISO strings in the DB. */

fun sourceFromString(value: String): FoodSource =
    FoodSource.entries.firstOrNull { it.name == value } ?: FoodSource.CUSTOM

fun DailyLogEntity.toDomain() = DailyLog(
    id = id,
    date = LocalDate.parse(date),
    foodId = foodId,
    foodName = foodName,
    source = sourceFromString(source),
    servingGrams = servingGrams,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    mealType = MealType.entries.firstOrNull { it.name == mealType } ?: MealType.LUNCH,
    createdAt = createdAt,
)

fun DailyLog.toEntity() = DailyLogEntity(
    id = id,
    date = date.toString(),
    foodId = foodId,
    foodName = foodName,
    source = source.name,
    servingGrams = servingGrams,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    mealType = mealType.name,
    createdAt = createdAt,
)

fun WeightLogEntity.toDomain() = WeightLog(
    id = id,
    date = LocalDate.parse(date),
    weightKg = weightKg,
    note = note,
    createdAt = createdAt,
)

fun WeightLog.toEntity() = WeightLogEntity(
    id = id,
    date = date.toString(),
    weightKg = weightKg,
    note = note,
    createdAt = createdAt,
)

fun WaterLogEntity.toDomain() = WaterLog(
    id = id,
    date = LocalDate.parse(date),
    amountMl = amountMl,
    time = time,
    createdAt = createdAt,
)

fun WaterLog.toEntity() = WaterLogEntity(
    id = id,
    date = date.toString(),
    amountMl = amountMl,
    time = time,
    createdAt = createdAt,
)

fun CustomFoodEntity.toDomain() = CustomFood(
    id = id,
    name = name,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    typicalServingDescription = typicalServingDescription,
    typicalServingGrams = typicalServingGrams,
    notes = notes,
    createdAt = createdAt,
)

fun CustomFood.toEntity() = CustomFoodEntity(
    id = id,
    name = name,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    typicalServingDescription = typicalServingDescription,
    typicalServingGrams = typicalServingGrams,
    notes = notes,
    createdAt = createdAt,
)

fun OffCacheEntity.toDomain() = OffCacheProduct(
    key = key,
    barcode = barcode,
    productName = productName,
    brand = brand,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    lastFetched = lastFetched,
)

/** Converts an OFF cache row to a [Food] (source OFF). */
fun OffCacheProduct.toFood() = Food(
    id = "off_${barcode ?: key}",
    name = productName,
    source = FoodSource.OFF,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    brand = brand,
    barcode = barcode,
    typicalServingDescription = "Standard 100g portion",
    typicalServingGrams = 100,
)
