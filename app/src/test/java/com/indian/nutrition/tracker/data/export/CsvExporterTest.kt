package com.indian.nutrition.tracker.data.export

import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import com.indian.nutrition.tracker.util.NumberUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvExporterTest {

    private val today = LocalDate.of(2026, 9, 5)

    private fun log(
        date: LocalDate,
        name: String = "Dal",
        created: Long = 1L,
        serving: Int = 150,
        kcal: Int = 180,
        protein: Double = 9.0,
        carbs: Double = 28.0,
        fat: Double = 2.0,
    ) = DailyLog(
        id = "l1", date = date, foodId = "nin_1", foodName = name, source = FoodSource.NIN,
        servingGrams = serving, calories = kcal, protein = protein, carbs = carbs, fat = fat,
        mealType = MealType.LUNCH, createdAt = created,
    )

    private fun water(date: LocalDate, amount: Int = 250) = WaterLog(
        id = "w1", date = date, amountMl = amount, time = "7:00 AM", createdAt = 1L,
    )

    private fun weight(date: LocalDate, note: String? = null, kg: Double = 82.0) = WeightLog(
        id = "wt1", date = date, weightKg = kg, note = note, createdAt = 1L,
    )

    @Test
    fun schemaMatchesWebByteForByte() {
        val csv = CsvExporter.export(
            dailyLogs = listOf(log(today, name = "D\"al \"spicy\"")),
            waterLogs = listOf(water(today)),
            weightLogs = listOf(weight(today, note = "after \"gym\"")),
            days = 0,
            today = today,
        )
        val expected = listOf(
            "Type,Date,Detail1,Detail2,Detail3,Detail4,Detail5,Detail6,Detail7",
            "# Food Logs: Type,Date,Meal,Food Name,Source,Serving (g),Calories (kcal),Protein (g),Carbs (g),Fat (g)",
            "FOOD,2026-09-05,Lunch,\"D\"\"al \"\"spicy\"\"\",NIN,150,180,9,28,2",
            "# Water Logs: Type,Date,Time,Amount (ml)",
            "WATER,2026-09-05,\"7:00 AM\",250",
            "# Weight Logs: Type,Date,Weight (kg),Note",
            "WEIGHT,2026-09-05,82,\"after \"\"gym\"\"\"",
        ).joinToString("\n") + "\n"
        assertEquals(expected, csv)
    }

    @Test
    fun emptyNoteProducesTrailingComma() {
        val csv = CsvExporter.export(
            dailyLogs = emptyList(), waterLogs = emptyList(),
            weightLogs = listOf(weight(today, note = null)),
            days = 0, today = today,
        )
        assertTrue(csv.contains("WEIGHT,2026-09-05,82,\n"))
    }

    @Test
    fun daysFilterUsesCutoffInclusive() {
        val csv = CsvExporter.export(
            dailyLogs = listOf(
                log(today.minusDays(30)),
                log(today.minusDays(29)),
                log(today),
            ),
            waterLogs = emptyList(), weightLogs = emptyList(),
            days = 30, today = today,
        )
        // Web parity: cutoff date is inclusive (l.date >= today-days).
        assertTrue(csv.contains(today.minusDays(30).toString()))
        assertTrue(csv.contains(today.minusDays(29).toString()))
        assertTrue(csv.contains(today.toString()))
        assertFalse(csv.contains(today.minusDays(31).toString()))
    }

    @Test
    fun rowsAreSortedChronologically() {
        val csv = CsvExporter.export(
            dailyLogs = listOf(log(today), log(today.minusDays(2))),
            waterLogs = listOf(water(today.minusDays(1))),
            weightLogs = listOf(weight(today.minusDays(1))),
            days = 0, today = today,
        )
        val foodLast = csv.indexOf("FOOD,${today.minusDays(2)}")
        val foodToday = csv.indexOf("FOOD,$today")
        val waterIdx = csv.indexOf("WATER,${today.minusDays(1)}")
        assertTrue(foodLast in 0 until foodToday)
        assertTrue(foodToday < waterIdx || waterIdx < 0)
    }

    @Test
    fun wholeDoublesPrintWithoutDecimal() {
        assertEquals("82", NumberUtils.trimmed(82.0))
        assertEquals("82.5", NumberUtils.trimmed(82.5))
        assertEquals("0", NumberUtils.trimmed(0.0))
        assertEquals("20.1", NumberUtils.trimmed(20.05)) // rounds to 0.1
    }

    @Test
    fun hugeAndFutureValuesSerializeLosslessly() {
        val csv = CsvExporter.export(
            dailyLogs = listOf(
                log(today, name = "Feast", serving = 4999, kcal = 9876, protein = 400.0, carbs = 500.0, fat = 300.0),
                log(LocalDate.of(2027, 1, 1), name = "Future Meal"),
            ),
            waterLogs = listOf(WaterLog("w2", LocalDate.of(2027, 1, 1), 10000, "12:00", 9L)),
            weightLogs = listOf(WeightLog("wg2", today, 350.0, "max", 9L)),
            days = 0,
            today = today,
        )
        assertTrue(csv.contains("FOOD,$today,Lunch,\"Feast\",NIN,4999,9876,400,500,300"))
        assertTrue(csv.contains("FOOD,2027-01-01,Lunch,\"Future Meal\",NIN,150,180,9,28,2"))
        assertTrue(csv.contains("WATER,2027-01-01,\"12:00\",10000"))
        assertTrue(csv.contains("WEIGHT,$today,350,\"max\""))
    }

}
