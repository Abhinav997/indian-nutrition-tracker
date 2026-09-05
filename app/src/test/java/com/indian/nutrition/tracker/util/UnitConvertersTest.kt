package com.indian.nutrition.tracker.util

import com.indian.nutrition.tracker.domain.model.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitConvertersTest {

    @Test
    fun kgToLb() {
        assertEquals(180.8, UnitConverters.kgToLb(82.0), 0.001)
        assertEquals(132.3, UnitConverters.kgToLb(60.0), 0.001)
    }

    @Test
    fun lbToKg() {
        assertEquals(82.0, UnitConverters.lbToKg(180.8), 0.001)
        assertEquals(70.1, UnitConverters.lbToKg(154.5), 0.001)
    }

    @Test
    fun roundTripIsStable() {
        val kg = 82.4
        assertEquals(kg, UnitConverters.lbToKg(UnitConverters.kgToLb(kg)), 0.15)
    }

    @Test
    fun formatWeight() {
        assertEquals("82.0 kg", UnitConverters.formatWeight(82.0, UnitSystem.KG))
        assertEquals("180.8 lb", UnitConverters.formatWeight(82.0, UnitSystem.LB))
    }

    @Test
    fun bmiCategories() {
        assertEquals(UnitConverters.BmiCategory.UNDERWEIGHT, UnitConverters.calculateBmi(50.0, 176.0)?.category)
        assertEquals(UnitConverters.BmiCategory.NORMAL, UnitConverters.calculateBmi(75.0, 176.0)?.category)
        assertEquals(UnitConverters.BmiCategory.OVERWEIGHT, UnitConverters.calculateBmi(82.0, 176.0)?.category)
        assertEquals(UnitConverters.BmiCategory.OBESE, UnitConverters.calculateBmi(100.0, 176.0)?.category)
        assertNull(UnitConverters.calculateBmi(0.0, 176.0))
        assertNull(UnitConverters.calculateBmi(82.0, 0.0))
    }

    @Test
    fun bmiValueMatchesWeb() {
        // 82 / 1.76² = 26.47 → 26.5 (same rounding as the web app)
        val bmi = UnitConverters.calculateBmi(82.0, 176.0)
        assertEquals(26.5, bmi?.bmi ?: 0.0, 0.001)
    }
}
