package com.indian.nutrition.tracker.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OffProductParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun product(raw: String) =
        OffProductParser.parseProduct(json.parseToJsonElement(raw).jsonObject)

    @Test
    fun parsesFullProduct() {
        val food = product(
            """
            {
              "code": "8901234567890",
              "product_name": "Amul Butter",
              "brands": "Amul",
              "image_front_small_url": "https://images.openfoodfacts.org/1.jpg",
              "categories_tags": ["en:butters"],
              "nutriments": {
                "energy-kcal_100g": 740.2,
                "proteins_100g": 0.6,
                "carbohydrates_100g": 0.4,
                "fat_100g": 82.1,
                "fiber_100g": 0
              }
            }
            """
        )!!

        assertEquals("off_8901234567890", food.id)
        assertEquals("Amul Butter", food.name)
        assertEquals("Amul", food.brand)
        assertEquals("https://images.openfoodfacts.org/1.jpg", food.imageUrl)
        assertEquals("en:butters", food.category)
        assertEquals("8901234567890", food.barcode)
        assertEquals(740, food.kcalPer100g.toInt())
        assertEquals(0.6, food.proteinPer100g, 0.0001)
        assertEquals(0.4, food.carbsPer100g, 0.0001)
        assertEquals(82.1, food.fatPer100g, 0.0001)
        assertNull(food.fiberPer100g) // zero fiber → null (web parity)
        assertEquals("100g packaged serving", food.typicalServingDescription)
        assertEquals(100, food.typicalServingGrams)
    }

    @Test
    fun kcalFallbackChain() {
        // energy-kcal (non-100g) → energy_kcal_100g → energy_100g/4.184
        val alt = product(
            """{"product_name":"X","nutriments":{"energy-kcal":512.6,"protein_100g":3.4}}"""
        )!!
        assertEquals(513, alt.kcalPer100g.toInt())

        val snake = product(
            """{"product_name":"X","nutriments":{"energy_kcal_100g":300.4}}"""
        )!!
        assertEquals(300, snake.kcalPer100g.toInt())

        val joules = product(
            """{"product_name":"X","nutriments":{"energy_100g":900.0}}"""
        )!!
        assertEquals(215, joules.kcalPer100g.toInt()) // 900 / 4.184 ≈ 215.1
    }

    @Test
    fun proteinFallbackKey() {
        val food = product(
            """{"product_name":"X","nutriments":{"energy-kcal_100g":100,"protein_100g":11.25}}"""
        )!!
        assertEquals(11.3, food.proteinPer100g, 0.0001) // rounded to 0.1
    }

    @Test
    fun stringNumbersAreParsed() {
        val food = product(
            """
            {"product_name":"X","nutriments":{
              "energy-kcal_100g":"52.6","proteins_100g":"1.00",
              "carbohydrates_100g":"8.2","fat_100g":"0.9"}}
            """
        )!!
        assertEquals(53, food.kcalPer100g.toInt())
        assertEquals(1.0, food.proteinPer100g, 0.0001)
    }

    @Test
    fun dropsProductsWithoutNameOrMacros() {
        assertNull(product("""{"code":"1"}"""))
        assertNull(
            product(
                """{"product_name":"Zero","nutriments":{
                    "energy-kcal_100g":0,"proteins_100g":0,
                    "carbohydrates_100g":0,"fat_100g":0}}"""
            )
        )
    }

    @Test
    fun deterministicIdForCodeLessProducts() {
        // Web used Math.random → duplicate rows; native uses normalized name.
        val a = product("""{"product_name":"  Amul  Butter ","nutriments":{"energy-kcal_100g":100}}""")!!
        val b = product("""{"product_name":"Amul Butter","nutriments":{"energy-kcal_100g":100}}""")!!
        assertEquals(a.id, b.id)
        assertEquals("off_net_amul_butter", a.id)
    }

    @Test
    fun parseProductsSkipsInvalidAndKeepsValid() {
        val raw = """
            {"products":[
              {"product_name":"Good","nutriments":{"energy-kcal_100g":120}},
              {"nutriments":{"energy-kcal_100g":50}},
              {"product_name":"Zero","nutriments":{"energy-kcal_100g":0}}
            ]}
        """
        val foods = OffProductParser.parseProducts(raw)
        assertEquals(1, foods.size)
        assertEquals("Good", foods[0].name)
        assertTrue(OffProductParser.parseProducts("not json").isEmpty())
    }
}
