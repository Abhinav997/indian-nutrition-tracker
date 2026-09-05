package com.indian.nutrition.tracker.domain.model

/** Cached Open Food Facts product (used for instant offline results). */
data class OffCacheProduct(
    val key: String,
    val barcode: String?,
    val productName: String,
    val brand: String?,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val lastFetched: Long,
)
