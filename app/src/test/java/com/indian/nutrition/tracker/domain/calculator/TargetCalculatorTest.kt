package com.indian.nutrition.tracker.domain.calculator

import com.indian.nutrition.tracker.domain.model.ActivityLevel
import com.indian.nutrition.tracker.domain.model.DateRange
import com.indian.nutrition.tracker.domain.model.GoalType
import com.indian.nutrition.tracker.domain.model.ProteinBasis
import com.indian.nutrition.tracker.domain.model.Sex
import com.indian.nutrition.tracker.domain.model.UnitSystem
import com.indian.nutrition.tracker.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetCalculatorTest {

    private fun settings(
        weight: Double = 82.0,
        targetWeight: Double = 74.0,
        height: Double = 176.0,
        age: Int = 28,
        sex: Sex = Sex.M,
        activity: ActivityLevel = ActivityLevel.MODERATE,
        goal: GoalType = GoalType.LOSE,
        rate: Double = -0.5,
        basis: ProteinBasis = ProteinBasis.CURRENT,
    ) = UserSettings(
        currentWeightKg = weight,
        targetWeightKg = targetWeight,
        heightCm = height,
        ageYears = age,
        sex = sex,
        activityLevel = activity,
        goalType = goal,
        goalRateKgPerWeek = rate,
        dailyCalorieTarget = 1950,
        dailyProteinTarget = 115,
        dailyWaterTargetMl = 2750,
        proteinBasis = basis,
        unitSystem = UnitSystem.KG,
        defaultChartRange = DateRange.D14,
    )

    /** Reference values mirror the web app's calculator.ts (verified with Node). */
    @Test
    fun defaultMaleModerateLose() {
        val r = TargetCalculator.calculateTargets(settings())
        assertEquals(1785, r.bmr)
        assertEquals(2767, r.tdee)
        assertEquals(1.55, r.activityFactor, 0.0001)
        assertEquals(-500, r.calorieAdjustment)
        assertEquals(2267, r.recommendedCalories)
        assertEquals(98, r.recommendedProtein)
        assertEquals(1.2, r.proteinMultiplier, 0.0001)
        assertEquals(82.0, r.effectiveWeight, 0.0001)
        assertEquals(3250, r.recommendedWaterMl)
    }

    @Test
    fun femaleSedentaryMaintain() {
        val r = TargetCalculator.calculateTargets(
            settings(weight = 60.0, targetWeight = 60.0, height = 165.0, age = 30,
                sex = Sex.F, activity = ActivityLevel.SEDENTARY, goal = GoalType.MAINTAIN, rate = 0.0)
        )
        assertEquals(1320, r.bmr)
        assertEquals(1584, r.tdee)
        assertEquals(0, r.calorieAdjustment)
        assertEquals(1584, r.recommendedCalories)
        assertEquals(66, r.recommendedProtein)
        assertEquals(2000, r.recommendedWaterMl)
    }

    @Test
    fun otherLightGain() {
        val r = TargetCalculator.calculateTargets(
            settings(weight = 70.0, targetWeight = 75.0, height = 170.0, age = 25,
                sex = Sex.OTHER, activity = ActivityLevel.LIGHT, goal = GoalType.GAIN, rate = 0.25)
        )
        assertEquals(1560, r.bmr)
        assertEquals(2145, r.tdee)
        assertEquals(250, r.calorieAdjustment)
        assertEquals(2395, r.recommendedCalories)
        assertEquals(105, r.recommendedProtein)
        assertEquals(1.5, r.proteinMultiplier, 0.0001)
        assertEquals(2500, r.recommendedWaterMl)
    }

    @Test
    fun proteinBasedOnTargetWeight() {
        val r = TargetCalculator.calculateTargets(
            settings(weight = 82.0, targetWeight = 74.0, basis = ProteinBasis.TARGET)
        )
        assertEquals(74.0, r.effectiveWeight, 0.0001)
        assertEquals(89, r.recommendedProtein)
    }

    @Test
    fun femaleCalorieFloorApplies() {
        val r = TargetCalculator.calculateTargets(
            settings(weight = 45.0, targetWeight = 40.0, height = 150.0, age = 70,
                sex = Sex.F, activity = ActivityLevel.SEDENTARY, goal = GoalType.LOSE, rate = -0.75)
        )
        assertEquals(877, r.bmr)
        assertEquals(1052, r.tdee)
        assertEquals(-750, r.calorieAdjustment)
        assertEquals(1200, r.recommendedCalories) // floor, not 302
        assertEquals(2000, r.recommendedWaterMl)
    }

    @Test
    fun formulaStringsAreTransparent() {
        val r = TargetCalculator.calculateTargets(settings())
        assertEquals("10 × 82.0kg + 6.25 × 176.0cm - 5 × 28 + 5 = 1785 kcal/day", r.formulaDetails.bmrFormula)
        assertEquals("1785 (BMR) × 1.55 (MODERATE) = 2767 kcal/day", r.formulaDetails.tdeeFormula)
        assert(r.formulaDetails.targetFormula.contains("- 500 kcal"))
        assert(r.formulaDetails.targetFormula.contains("2267 kcal"))
        assert(r.formulaDetails.proteinFormula.contains("1.2 g/kg"))
        assert(r.formulaDetails.waterFormula.contains("3250 ml/day (~3.3L)"))
    }

    @Test
    fun otherSexFormulaUsesMinus78() {
        val r = TargetCalculator.calculateTargets(
            settings(weight = 70.0, height = 170.0, age = 25, sex = Sex.OTHER,
                activity = ActivityLevel.LIGHT, goal = GoalType.GAIN, rate = 0.25)
        )
        assert(r.formulaDetails.bmrFormula.contains("- 78"))
        assert(!r.formulaDetails.bmrFormula.contains("+ 5"))
    }
}
