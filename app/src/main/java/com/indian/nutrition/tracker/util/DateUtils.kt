package com.indian.nutrition.tracker.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Date helpers — all app dates are ISO `LocalDate` keys (YYYY-MM-DD). */
object DateUtils {

    private val shortFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

    /** Format a [LocalDate] as an ISO-8601 date key (YYYY-MM-DD). */
    fun formatDateKey(date: LocalDate): String = date.toString()

    /** Parse an ISO-8601 date key into a [LocalDate]. */
    fun parseDateKey(dateKey: String): LocalDate = LocalDate.parse(dateKey)

    /** Today's date. */
    fun today(): LocalDate = LocalDate.now()

    /** A date offset from today by [days] (negative = past). */
    fun offsetDate(days: Long): LocalDate = LocalDate.now().plusDays(days)

    /** Friendly label: "Today", "Yesterday", "Tomorrow", else "Thu, Sep 4". */
    fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(shortFormatter)
    }
}
