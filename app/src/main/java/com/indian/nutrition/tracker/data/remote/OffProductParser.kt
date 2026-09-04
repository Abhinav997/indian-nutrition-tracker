package com.indian.nutrition.tracker.data.remote

import com.indian.nutrition.tracker.domain.model.Food
import com.indian.nutrition.tracker.domain.model.FoodSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.roundToInt

/**
 * Open Food Facts search response. Products are kept as raw [JsonElement]s
 * so parsing can tolerate both numeric and string values (OFF serves a mix).
 */
@Serializable
data class OffSearchResponseDto(
    val products: List<JsonElement> = emptyList(),
)

/**
 * Pure parser for an OFF product (port of the web app's `parseOffProduct`,
 * with the web bugs fixed):
 * - kcal fallback chain: energy-kcal_100g → energy-kcal → energy_kcal_100g
 *   → energy_100g / 4.184, rounded to Int
 * - proteins_100g → protein_100g fallback; macros rounded to 0.1
 * - products without a name or with all-zero macros are dropped
 * - code-less products get a deterministic id (web used Math.random → the
 *   duplicate-row bug in search results)
 */
object OffProductParser {

    private val json = Json { ignoreUnknownKeys = true }

    /** Parse the raw `products` array of an OFF response. */
    fun parseProducts(raw: String): List<Food> {
        val response = try {
            json.decodeFromString<OffSearchResponseDto>(raw)
        } catch (_: Exception) {
            return emptyList()
        }
        return response.products.mapNotNull { parseProduct(it) }
    }

    fun parseProduct(element: JsonElement): Food? {
        val obj = element as? JsonObject ?: return null
        val name = obj.string("product_name")?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        val nutriments = obj["nutriments"] as? JsonObject ?: JsonObject(emptyMap())
        val kcal = (nutriments.number("energy-kcal_100g")
            ?: nutriments.number("energy-kcal")
            ?: nutriments.number("energy_kcal_100g")
            ?: nutriments.number("energy_100g")?.let { it / 4.184 })?.roundToInt() ?: 0
        val protein = nutriments.number("proteins_100g") ?: nutriments.number("protein_100g") ?: 0.0
        val carbs = nutriments.number("carbohydrates_100g") ?: 0.0
        val fat = nutriments.number("fat_100g") ?: 0.0
        val fiber = nutriments.number("fiber_100g") ?: 0.0

        if (kcal == 0 && protein == 0.0 && carbs == 0.0 && fat == 0.0) return null

        val code = obj.string("code")
        val normalizedName = name.lowercase().trim().replace(Regex("\\s+"), "_")
        return Food(
            id = "off_${code ?: "net_$normalizedName"}",
            name = name,
            source = FoodSource.OFF,
            kcalPer100g = kcal.toDouble(),
            proteinPer100g = round1(protein),
            carbsPer100g = round1(carbs),
            fatPer100g = round1(fat),
            fiberPer100g = if (fiber > 0.0) round1(fiber) else null,
            typicalServingDescription = "100g packaged serving",
            typicalServingGrams = 100,
            brand = obj.string("brands")?.takeIf { it.isNotBlank() } ?: "Packaged Product",
            category = obj["categories_tags"]?.let { cats ->
                (cats as? JsonArray)?.firstOrNull()?.let { it.contentOrNull }
            },
            barcode = code,
            imageUrl = obj.string("image_front_small_url"),
        )
    }

    private fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0

    private fun JsonObject.string(key: String): String? {
        val el = this[key] ?: return null
        return el.contentOrNull ?: return null
    }

    /**
     * Reads a number from a JSON object. Handles both numeric literals
     * (OFF's usual output) and numeric strings (seen in some products).
     */
    private fun JsonObject.number(key: String): Double? {
        val el = this[key] ?: return null
        el.doubleOrNull?.let { return it }
        return el.contentOrNull?.toDoubleOrNull()
    }
}
