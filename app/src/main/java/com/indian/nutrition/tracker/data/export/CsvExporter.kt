package com.indian.nutrition.tracker.data.export

import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * CSV export with the web app's exact schema (byte-identical header and
 * row shapes), so existing spreadsheets keep working:
 *
 * ```
 * Type,Date,Detail1,Detail2,Detail3,Detail4,Detail5,Detail6,Detail7
 * # Food Logs: Type,Date,Meal,Food Name,Source,Serving (g),Calories (kcal),Protein (g),Carbs (g),Fat (g)
 * FOOD,2026-09-05,Lunch,"Dal",NIN,150,180,9.0,28.0,2.0
 * # Water Logs: Type,Date,Time,Amount (ml)
 * WATER,2026-09-05,"7:00 AM",250
 * # Weight Logs: Type,Date,Weight (kg),Note
 * WEIGHT,2026-09-05,82.5,"after gym"
 * ```
 */
object CsvExporter {

    private const val HEADER = "Type,Date,Detail1,Detail2,Detail3,Detail4,Detail5,Detail6,Detail7\n"
    private const val FOOD_COMMENT =
        "# Food Logs: Type,Date,Meal,Food Name,Source,Serving (g),Calories (kcal),Protein (g),Carbs (g),Fat (g)\n"
    private const val WATER_COMMENT = "# Water Logs: Type,Date,Time,Amount (ml)\n"
    private const val WEIGHT_COMMENT = "# Weight Logs: Type,Date,Weight (kg),Note\n"

    /**
     * @param days 0 = all data; >0 = last [days] days (inclusive cutoff date,
     *             web parity).
     */
    fun export(
        dailyLogs: List<DailyLog>,
        waterLogs: List<WaterLog>,
        weightLogs: List<WeightLog>,
        days: Int = 0,
        today: LocalDate = LocalDate.now(),
    ): String {
        val cutoff = if (days > 0) today.minusDays(days.toLong()) else null
        fun inRange(date: LocalDate): Boolean = cutoff == null || !date.isBefore(cutoff)

        val foods = dailyLogs
            .filter { inRange(it.date) }
            .sortedWith(compareBy<DailyLog> { it.date }.thenBy { it.createdAt })
        val waters = waterLogs
            .filter { inRange(it.date) }
            .sortedWith(compareBy<WaterLog> { it.date }.thenBy { it.createdAt })
        val weights = weightLogs
            .filter { inRange(it.date) }
            .sortedWith(compareBy<WeightLog> { it.date }.thenBy { it.createdAt })

        val sb = StringBuilder()
        sb.append(HEADER)
        sb.append(FOOD_COMMENT)
        foods.forEach { log ->
            sb.append("FOOD,${log.date},")
            sb.append(log.mealType.displayName)
            sb.append(',').append(quote(log.foodName))
            sb.append(',').append(log.source.name)
            sb.append(',').append(log.servingGrams)
            sb.append(',').append(log.calories)
            sb.append(',').append(trimmed(log.protein))
            sb.append(',').append(trimmed(log.carbs))
            sb.append(',').append(trimmed(log.fat))
            sb.append('\n')
        }
        sb.append(WATER_COMMENT)
        waters.forEach { w ->
            sb.append("WATER,${w.date},")
            sb.append(quote(w.time ?: ""))
            sb.append(',').append(w.amountMl)
            sb.append('\n')
        }
        sb.append(WEIGHT_COMMENT)
        weights.forEach { w ->
            sb.append("WEIGHT,${w.date},")
            sb.append(trimmed(w.weightKg))
            sb.append(',').append(w.note?.let { quote(it) } ?: "")
            sb.append('\n')
        }
        return sb.toString()
    }

    /** Double formatting that avoids trailing ".0" for whole values (JS parity). */
    internal fun trimmed(value: Double): String {
        val rounded = (value * 10).roundToInt() / 10.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
        else rounded.toString()
    }

    private fun quote(value: String): String =
        "\"${value.replace("\"", "\"\"")}\""
}
