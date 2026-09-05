package com.indian.nutrition.tracker.util

import kotlin.math.roundToInt

/**
 * Shared number formatting/rounding helpers.
 *
 * `trimmed` mirrors JavaScript's `Number.toString()` rounding so exported
 * CSV/JSON values byte-match the web app (whole numbers have no ".0").
 */
object NumberUtils {

    /** Rounds to one decimal and drops a trailing ".0" for whole values. */
    fun trimmed(value: Double): String {
        val rounded = round1(value)
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
        else rounded.toString()
    }

    /** Rounds a double to one decimal place (82.55 -> 82.6). */
    fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0

    /** Chart/axis label: whole units for kcal/water, one decimal otherwise. */
    fun formatValue(value: Double, unit: String): String =
        if (unit == "kcal" || unit == "ml") "${value.roundToInt()} $unit"
        else "${round1(value)} $unit"
}
