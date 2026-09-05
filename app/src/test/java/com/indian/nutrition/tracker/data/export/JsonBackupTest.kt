package com.indian.nutrition.tracker.data.export

import com.indian.nutrition.tracker.domain.model.ActivityLevel
import com.indian.nutrition.tracker.domain.model.CustomFood
import com.indian.nutrition.tracker.domain.model.DailyLog
import com.indian.nutrition.tracker.domain.model.DateRange
import com.indian.nutrition.tracker.domain.model.FoodSource
import com.indian.nutrition.tracker.domain.model.GoalType
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.domain.model.ProteinBasis
import com.indian.nutrition.tracker.domain.model.Sex
import com.indian.nutrition.tracker.domain.model.UnitSystem
import com.indian.nutrition.tracker.domain.model.UserSettings
import com.indian.nutrition.tracker.domain.model.WaterLog
import com.indian.nutrition.tracker.domain.model.WeightLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JsonBackupTest {

    private val date = LocalDate.of(2026, 9, 5)

    private val settings = UserSettings(
        currentWeightKg = 82.0, targetWeightKg = 74.0, heightCm = 176.0, ageYears = 28,
        sex = Sex.M, activityLevel = ActivityLevel.MODERATE, goalType = GoalType.LOSE,
        goalRateKgPerWeek = -0.5, dailyCalorieTarget = 1950, dailyProteinTarget = 115,
        dailyWaterTargetMl = 2750, proteinBasis = ProteinBasis.CURRENT,
        unitSystem = UnitSystem.KG, defaultChartRange = DateRange.D14,
    )

    private fun sampleExport(): String = JsonBackup.export(
        settings = settings,
        dailyLogs = listOf(
            DailyLog("l1", date, "nin_dal", "Dal", FoodSource.NIN, 150, 180, 9.0, 28.0, 2.0, MealType.LUNCH, 100L),
        ),
        weightLogs = listOf(WeightLog("w1", date, 82.5, "after gym", 200L)),
        waterLogs = listOf(WaterLog("wt1", date, 250, "7:00 AM", 300L)),
        customFoods = listOf(
            CustomFood("c1", "My Paneer", 290.0, 18.0, 4.0, 22.0, 1.0, "1 bowl", 150, "recipe", 400L),
        ),
    )

    @Test
    fun emitsWebSnakeCaseFormat() {
        val raw = sampleExport()
        assertTrue(raw.contains("\"current_weight_kg\": 82.0"))
        assertTrue(raw.contains("\"food_id\": \"nin_dal\""))
        assertTrue(raw.contains("\"serving_grams\": 150"))
        assertTrue(raw.contains("\"weight_kg\": 82.5"))
        assertTrue(raw.contains("\"amount_ml\": 250"))
        assertTrue(raw.contains("\"created_at\": 100"))
        assertTrue(raw.contains("\"version\": \"1.0\""))
        assertTrue(raw.contains("\"exportedAt\""))
    }

    @Test
    fun roundTripPreservesAllData() {
        val result = JsonBackup.parse(sampleExport())
        assertTrue(result is JsonBackup.ImportResult.Success)
        val backup = (result as JsonBackup.ImportResult.Success).backup

        assertEquals(settings, backup.settings.toDomain())
        assertEquals(1, backup.dailyLogs.size)
        assertEquals("Dal", backup.dailyLogs[0].toDomain().foodName)
        assertEquals(82.5, backup.weightLogs[0].toDomain().weightKg, 0.001)
        assertEquals("7:00 AM", backup.waterLogs[0].toDomain().time)
        assertEquals("My Paneer", backup.customFoods[0].toDomain().name)
    }

    @Test
    fun importsLegacyWebLabels() {
        val raw = """
        {
          "settings": {
            "current_weight_kg": 70.0, "target_weight_kg": 65.0, "height_cm": 165,
            "age_years": 30, "sex": "Other", "activity_level": "Light Exercise",
            "goal_type": "Gain", "goal_rate_kg_per_week": 0.25,
            "daily_calorie_target": 2200, "daily_protein_target": 100,
            "daily_water_target_ml": 2500, "protein_basis": "target",
            "unit_system": "lb", "default_chart_range": "All"
          },
          "dailyLogs": [
            {"id":"l1","date":"2026-09-05","food_id":"f","food_name":"Roti",
             "source":"NIN","serving_grams":50,"calories":120,"protein":4.0,
             "carbs":24.0,"fat":1.0,"meal_type":"Breakfast","created_at":1}
          ],
          "weightLogs": [{"id":"w1","date":"2026-09-05","weight_kg":70,"created_at":1}],
          "waterLogs": [{"id":"x1","date":"2026-09-05","amount_ml":500,"time":"8 AM","created_at":1}],
          "customFoods": [], "exportedAt": "2026-09-05T00:00:00Z", "version": "1.0"
        }
        """.trimIndent()
        val result = JsonBackup.parse(raw)
        assertTrue(result is JsonBackup.ImportResult.Success)
        val backup = (result as JsonBackup.ImportResult.Success).backup
        val s = backup.settings.toDomain()
        assertEquals(Sex.OTHER, s.sex)
        assertEquals(ActivityLevel.LIGHT, s.activityLevel)
        assertEquals(GoalType.GAIN, s.goalType)
        assertEquals(ProteinBasis.TARGET, s.proteinBasis)
        assertEquals(UnitSystem.LB, s.unitSystem)
        assertEquals(DateRange.ALL, s.defaultChartRange)
        assertEquals(MealType.BREAKFAST, backup.dailyLogs[0].toDomain().mealType)
    }

    @Test
    fun rejectsCorruptJson() {
        val result = JsonBackup.parse("not json {{{")
        assertTrue(result is JsonBackup.ImportResult.Error)
    }

    @Test
    fun rejectsInvalidRows() {
        val raw = sampleExport().replace("\"serving_grams\": 150", "\"serving_grams\": 0")
        val result = JsonBackup.parse(raw)
        assertTrue(result is JsonBackup.ImportResult.Error)
        assertTrue((result as JsonBackup.ImportResult.Error).message.contains("serving"))

        val badWeight = sampleExport().replace("\"weight_kg\": 82.5", "\"weight_kg\": 999.0")
        val w = JsonBackup.parse(badWeight)
        assertTrue(w is JsonBackup.ImportResult.Error)
        assertTrue((w as JsonBackup.ImportResult.Error).message.contains("20–350"))

        val badDate = sampleExport().replace("\"date\": \"2026-09-05\"", "\"date\": \"not-a-date\"")
        val d = JsonBackup.parse(badDate)
        assertTrue(d is JsonBackup.ImportResult.Error)
        assertTrue((d as JsonBackup.ImportResult.Error).message.contains("bad date"))
    }
}
