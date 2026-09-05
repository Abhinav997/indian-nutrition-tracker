package com.indian.nutrition.tracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberUtilsTest {

    @Test
    fun trimmedDropsTrailingZero() {
        assertEquals("82", NumberUtils.trimmed(82.0))
        assertEquals("0", NumberUtils.trimmed(0.0))
        assertEquals("2000", NumberUtils.trimmed(2000.0))
    }

    @Test
    fun trimmedRoundsToOneDecimal() {
        assertEquals("82.5", NumberUtils.trimmed(82.5))
        assertEquals("82.6", NumberUtils.trimmed(82.55)) // banker's-free half-up at 1 dp
        assertEquals("20.1", NumberUtils.trimmed(20.05))
        assertEquals("9.1", NumberUtils.trimmed(9.09))
        assertEquals("9", NumberUtils.trimmed(9.04))
    }

    @Test
    fun round1HalfUp() {
        assertEquals(82.6, NumberUtils.round1(82.55), 0.0001)
        assertEquals(1.3, NumberUtils.round1(1.25), 0.0001)
        assertEquals(-0.4, NumberUtils.round1(-0.45), 0.0001) // Math.round half-up toward +inf
    }

    @Test
    fun formatValueUsesWholeNumbersForKcalAndMl() {
        assertEquals("1950 kcal", NumberUtils.formatValue(1950.0, "kcal"))
        assertEquals("2750 ml", NumberUtils.formatValue(2750.4, "ml"))
        assertEquals("82.5 kg", NumberUtils.formatValue(82.5, "kg"))
        assertEquals("82.0 kg", NumberUtils.formatValue(82.0, "kg"))
    }
}
