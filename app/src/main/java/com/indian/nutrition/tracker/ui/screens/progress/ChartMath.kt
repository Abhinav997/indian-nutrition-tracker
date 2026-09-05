package com.indian.nutrition.tracker.ui.screens.progress

import com.indian.nutrition.tracker.domain.model.DateRange
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.UnitSystem
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import com.indian.nutrition.tracker.util.UnitConverters
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** The four chart metrics (matches the web app's segmented control). */
enum class ChartMetric(val label: String) {
    WEIGHT("Weight"),
    CALORIES("Calories"),
    PROTEIN("Protein"),
    WATER("Water"),
}

/** One plotted value for one day. `actual` = real measurement vs forward-filled. */
data class ChartPoint(
    val date: LocalDate,
    val value: Double,
    val target: Double,
    val actual: Boolean,
)

/** Fully computed chart series (geometry-agnostic, unit-testable). */
data class ChartSeries(
    val metric: ChartMetric,
    val points: List<ChartPoint>,
    val minValue: Double,
    val maxValue: Double,
    val target: Double,
    val unit: String,
) {
    val isLine: Boolean get() = metric == ChartMetric.WEIGHT

    /** Bars aren't drawn when the series has no entries. */
    val isEmpty: Boolean get() = points.isEmpty()
}

/**
 * Pure chart math (port of the web `Charts.tsx`, with the averages bug
 * fixed: the web divided by days-with-data; we divide by the full range).
 */
object ChartMath {

    /** Last [range.days] dates, oldest first, ending today (web parity). */
    fun rangeDates(range: DateRange, today: LocalDate = LocalDate.now()): List<LocalDate> =
        (range.days - 1 downTo 0).map { today.minusDays(it.toLong()) }

    /** Weight in the user's display unit (kg stays, lb converts). */
    fun weightValue(kg: Double, unit: UnitSystem): Double =
        if (unit == UnitSystem.LB) UnitConverters.kgToLb(kg) else kg

    /**
     * Weight line with forward fill: days without a measurement carry the
     * most recent known value (starting from the first log or current weight).
     */
    fun weightSeries(
        dates: List<LocalDate>,
        logs: List<WeightLog>,
        settings: UserSettings,
    ): ChartSeries {
        if (dates.isEmpty()) {
            return ChartSeries(ChartMetric.WEIGHT, emptyList(), 0.0, 1.0, 0.0, "kg")
        }
        val logsByDate = logs.associateBy { it.date }
        val starting = logs.firstOrNull()?.weightKg ?: settings.currentWeightKg
        val target = weightValue(settings.targetWeightKg, settings.unitSystem)
        var last = starting
        val points = dates.map { date ->
            logsByDate[date]?.let { last = it.weightKg }
            ChartPoint(
                date = date,
                value = round1(weightValue(last, settings.unitSystem)),
                target = round1(target),
                actual = logsByDate[date] != null,
            )
        }
        return finishSeries(ChartMetric.WEIGHT, points, "kg", settings.unitSystem)
    }

    /** Daily calories per date. */
    fun dailyCalories(logs: List<DailyLog>): Map<LocalDate, Double> =
        logs.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.calories }.toDouble() }

    /** Daily protein per date (rounded to 0.1, like the web). */
    fun dailyProtein(logs: List<DailyLog>): Map<LocalDate, Double> =
        logs.groupBy { it.date }.mapValues { (_, list) -> round1(list.sumOf { it.protein }) }

    /** Daily water intake per date (ml). */
    fun dailyWater(waterLogs: List<WaterLog>): Map<LocalDate, Double> =
        waterLogs.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.amountMl }.toDouble() }

    /** Bar series (calories / protein / water) with a constant target. */
    fun intakeSeries(
        metric: ChartMetric,
        dates: List<LocalDate>,
        valuesByDate: Map<LocalDate, Double>,
        target: Double,
        unit: String,
    ): ChartSeries {
        if (dates.isEmpty()) {
            return ChartSeries(metric, emptyList(), 0.0, 1.0, 0.0, unit)
        }
        val points = dates.map { date ->
            val value = valuesByDate[date] ?: 0.0
            ChartPoint(date = date, value = value, target = target, actual = value > 0.0)
        }
        return finishSeries(metric, points, unit, null)
    }

    /**
     * Average over the FULL range (web bug fix: it divided by days with data,
     * inflating averages on sparse logs). `decimals`: 0 → integer, 1 → 0.1.
     */
    fun average(
        valuesByDate: Map<LocalDate, Double>,
        dates: List<LocalDate>,
        decimals: Int = 0,
    ): Double {
        if (dates.isEmpty()) return 0.0
        val avg = dates.sumOf { valuesByDate[it] ?: 0.0 } / dates.size
        val factor = when {
            decimals <= 0 -> 1
            decimals == 1 -> 10
            else -> 100
        }
        return (avg * factor).roundToInt() / factor.toDouble()
    }

    private fun finishSeries(
        metric: ChartMetric,
        points: List<ChartPoint>,
        unit: String,
        unitSystem: UnitSystem?,
    ): ChartSeries {
        val values = points.map { it.value }
        val target = points.first().target
        val minValue = if (metric == ChartMetric.WEIGHT) {
            floor((values.minOrNull() ?: 0.0) - 2.0)
        } else {
            0.0
        }
        val rawMax = maxOf(values.maxOrNull() ?: 0.0, target * 1.1)
        val maxValue = ceil(rawMax + if (metric == ChartMetric.WEIGHT) 2.0 else 10.0)
            .coerceAtLeast(minValue + 1.0)
        val unitLabel = if (metric == ChartMetric.WEIGHT) {
            if (unitSystem == UnitSystem.LB) "lb" else "kg"
        } else {
            unit
        }
        return ChartSeries(metric, points, minValue, maxValue, target, unitLabel)
    }

    private fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0
}
