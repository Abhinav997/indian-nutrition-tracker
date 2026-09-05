package com.indian.nutrition.tracker.ui.screens.progress

import com.indian.nutrition.tracker.domain.model.DateRange
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.domain.model.UnitSystem
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ChartMathTest {

    private val today = LocalDate.of(2026, 9, 5)
    private val d = { i: Long -> today.minusDays(i) } // d(0)=today, d(1)=yesterday...

    private fun settings(weightKg: Double = 82.0, targetKg: Double = 76.0) = UserSettings(
        currentWeightKg = weightKg,
        targetWeightKg = targetKg,
        heightCm = 176.0,
        ageYears = 28,
        sex = com.indian.nutrition.tracker.domain.model.Sex.M,
        activityLevel = com.indian.nutrition.tracker.domain.model.ActivityLevel.MODERATE,
        goalType = com.indian.nutrition.tracker.domain.model.GoalType.LOSE,
        goalRateKgPerWeek = 0.5,
        dailyCalorieTarget = 1950,
        dailyProteinTarget = 115,
        dailyWaterTargetMl = 2500,
        proteinBasis = com.indian.nutrition.tracker.domain.model.ProteinBasis.CURRENT,
        unitSystem = UnitSystem.KG,
        defaultChartRange = DateRange.D14,
    )

    private fun weightLog(date: LocalDate, kg: Double) = WeightLog(
        id = "w_$date", date = date, weightKg = kg, createdAt = 1L,
    )

    private fun log(date: LocalDate, kcal: Int, protein: Double) = DailyLog(
        id = "l_$date", date = date, foodId = "f", foodName = "F",
        source = FoodSource.NIN, servingGrams = 100, calories = kcal,
        protein = protein, carbs = 0.0, fat = 0.0, mealType = MealType.LUNCH, createdAt = 1L,
    )

    private fun water(date: LocalDate, ml: Int) = WaterLog(
        id = "w_$date", date = date, amountMl = ml, createdAt = 1L,
    )

    @Test
    fun rangeDatesEndsTodayAndHasRangeDays() {
        assertEquals(7, ChartMath.rangeDates(DateRange.D7, today).size)
        assertEquals(60, ChartMath.rangeDates(DateRange.ALL, today).size)
        assertEquals(today, ChartMath.rangeDates(DateRange.D7, today).last())
        assertEquals(today.minusDays(6), ChartMath.rangeDates(DateRange.D7, today).first())
    }

    @Test
    fun weightSeriesForwardFillsFromFirstLog() {
        val logs = listOf(
            weightLog(d(5), 84.0),
            weightLog(d(2), 83.0),
            weightLog(d(0), 82.5),
        )
        val series = ChartMath.weightSeries(ChartMath.rangeDates(DateRange.D7, today), logs, settings())
        assertEquals(7, series.points.size)
        // Before the first log, baseline = first log (web parity). d(6)=idx0 fill.
        assertEquals(84.0, series.points[0].value, 0.001)
        // d(5)=idx1 actual, d(4)=idx2 and d(3)=idx3 carry 84.0.
        assertFalse(series.points[0].actual)
        assertTrue(series.points[1].actual)
        assertEquals(84.0, series.points[3].value, 0.001)
        // d(2)=idx4 is an actual 83.0.
        assertEquals(83.0, series.points[4].value, 0.001)
        assertTrue(series.points[4].actual)
        assertEquals(82.5, series.points[6].value, 0.001)
        // Target in kg.
        assertEquals(76.0, series.target, 0.001)
        assertEquals("kg", series.unit)
    }

    @Test
    fun weightSeriesUsesSettingsCurrentAsBaselineWhenNoLogs() {
        val series = ChartMath.weightSeries(ChartMath.rangeDates(DateRange.D14, today), emptyList(), settings(weightKg = 80.5))
        assertTrue(series.points.all { it.value == 80.5 })
        assertTrue(series.points.none { it.actual })
    }

    @Test
    fun weightSeriesConvertsToLb() {
        val series = ChartMath.weightSeries(
            ChartMath.rangeDates(DateRange.D7, today),
            listOf(weightLog(d(0), 82.0)),
            settings().copy(unitSystem = UnitSystem.LB),
        )
        assertEquals(180.8, series.points.last().value, 0.05) // 82 kg ≈ 180.78 lb
        assertEquals("lb", series.unit)
    }

    @Test
    fun intakeSeriesBuildsBarsWithTargetAndZeros() {
        val kcalMap = mapOf(d(0) to 2100.0, d(1) to 0.0, d(2) to 1800.0)
        val series = ChartMath.intakeSeries(
            ChartMetric.CALORIES,
            ChartMath.rangeDates(DateRange.D7, today),
            kcalMap, 1950.0, "kcal",
        )
        assertEquals(7, series.points.size)
        assertEquals(2100.0, series.points[6].value, 0.001)
        assertEquals(0.0, series.points[5].value, 0.001)
        assertFalse(series.points[5].actual)
        assertEquals(1950.0, series.target, 0.001)
        assertEquals("kcal", series.unit)
        assertEquals(0.0, series.minValue, 0.001)
        assertTrue(series.maxValue >= series.target * 1.1)
    }

    @Test
    fun averagesUseFullRangeDenominator() {
        // Web bug: divided by 2 days-with-data → 1950; correct = 1950 * 2/7 ≈ 557.
        val kcalMap = mapOf(d(0) to 2100.0, d(1) to 1800.0)
        val dates = ChartMath.rangeDates(DateRange.D7, today)
        val avg = ChartMath.average(kcalMap, dates, 0)
        assertEquals(557, avg)
        assertEquals(0, ChartMath.average(emptyMap(), dates, 0))
    }

    @Test
    fun dailyAggregationsSumPerDate() {
        val kcalMap = ChartMath.dailyCalories(
            listOf(log(d(0), 500, 20.0), log(d(0), 700, 30.0), log(d(1), 100, 5.0)),
        )
        assertEquals(1200.0, kcalMap[d(0)]!!, 0.001)
        assertEquals(100.0, kcalMap[d(1)]!!, 0.001)

        val proteinMap = ChartMath.dailyProtein(
            listOf(log(d(0), 500, 20.1), log(d(0), 700, 30.1)),
        )
        assertEquals(50.2, proteinMap[d(0)]!!, 0.001) // rounded to 0.1

        val waterMap = ChartMath.dailyWater(listOf(water(d(0), 700), water(d(0), 400)))
        assertEquals(1100.0, waterMap[d(0)]!!, 0.001)
    }

    @Test
    fun weightSeriesMinMaxPadding() {
        val logs = listOf(weightLog(d(0), 82.0), weightLog(d(1), 80.0))
        val series = ChartMath.weightSeries(ChartMath.rangeDates(DateRange.D7, today), logs, settings())
        assertEquals(78.0, series.minValue, 0.001) // floor(min - 2)
        assertTrue(series.maxValue >= 84.0) // ceil(max(values, target*1.1) + 2)
    }
}
