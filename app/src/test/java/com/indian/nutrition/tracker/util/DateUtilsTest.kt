package com.indian.nutrition.tracker.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun formatAndParseRoundTrip() {
        val date = LocalDate.of(2026, 9, 4)
        assertEquals("2026-09-04", DateUtils.formatDateKey(date))
        assertEquals(date, DateUtils.parseDateKey("2026-09-04"))
    }

    @Test
    fun dayLabels() {
        val today = LocalDate.of(2026, 9, 4)
        assertEquals("Today", DateUtils.dayLabel(today, today))
        assertEquals("Yesterday", DateUtils.dayLabel(today.minusDays(1), today))
        assertEquals("Tomorrow", DateUtils.dayLabel(today.plusDays(1), today))
        assertEquals("Tue, Sep 1", DateUtils.dayLabel(LocalDate.of(2026, 9, 1), today))
    }

    @Test
    fun offsetDate() {
        val now = LocalDate.now()
        assertEquals(now.minusDays(1), DateUtils.offsetDate(-1))
        assertEquals(now.plusDays(30), DateUtils.offsetDate(30))
    }

    @Test
    fun monthAndYearRollover() {
        val today = LocalDate.of(2026, 12, 31)
        assertEquals("2026-12-31", DateUtils.formatDateKey(today))
        assertEquals("2027-01-01", DateUtils.formatDateKey(today.plusDays(1)))
        assertEquals("2026-12-01", DateUtils.formatDateKey(today.minusDays(30)))
        assertEquals("2025-12-01", DateUtils.formatDateKey(today.minusDays(395)))
        // Leap day boundary
        assertEquals("2028-03-01", DateUtils.formatDateKey(LocalDate.of(2028, 2, 29).plusDays(1)))
    }

    @Test
    fun parseRejectsBadKeys() {
        val bad = listOf("", "2026-13-01", "2026-02-30", "yesterday", "2026/09/05")
        bad.forEach { key ->
            try {
                DateUtils.parseDateKey(key)
                throw AssertionError("expected parse failure for \"$key\"")
            } catch (_: Exception) {
                // expected
            }
        }
    }

    @Test
    fun keySortOrderIsChronological() {
        // ISO keys sort lexicographically == chronologically (no DST ambiguity
        // for date-only keys), which the DAOs rely on.
        val dates = listOf("2026-09-05", "2026-09-01", "2026-08-31", "2025-12-31")
        assertEquals(dates.sorted(), dates.sortedBy { DateUtils.parseDateKey(it).toEpochDay() })
    }

}
