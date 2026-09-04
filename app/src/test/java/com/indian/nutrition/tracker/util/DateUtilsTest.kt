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
        assertEquals("Mon, Sep 1", DateUtils.dayLabel(LocalDate.of(2026, 9, 1), today))
    }

    @Test
    fun offsetDate() {
        val now = LocalDate.now()
        assertEquals(now.minusDays(1), DateUtils.offsetDate(-1))
        assertEquals(now.plusDays(30), DateUtils.offsetDate(30))
    }
}
