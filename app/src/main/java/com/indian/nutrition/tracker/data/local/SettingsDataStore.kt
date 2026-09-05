package com.indian.nutrition.tracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.indian.nutrition.tracker.domain.model.ActivityLevel
import com.indian.nutrition.tracker.domain.model.DateRange
import com.indian.nutrition.tracker.domain.model.GoalType
import com.indian.nutrition.tracker.domain.model.ProteinBasis
import com.indian.nutrition.tracker.domain.model.Sex
import com.indian.nutrition.tracker.domain.model.UnitSystem
import com.indian.nutrition.tracker.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** DataStore install point (singleton per process). */
val Context.inwSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "inw_settings")

/** Default settings — identical to the web app's DEFAULT_SETTINGS. */
object DefaultSettings {
    val value = UserSettings(
        currentWeightKg = 82.0,
        targetWeightKg = 74.0,
        heightCm = 176.0,
        ageYears = 28,
        sex = Sex.M,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.LOSE,
        goalRateKgPerWeek = -0.5,
        dailyCalorieTarget = 1950,
        dailyProteinTarget = 115,
        dailyWaterTargetMl = 2750,
        proteinBasis = ProteinBasis.CURRENT,
        unitSystem = UnitSystem.KG,
        defaultChartRange = DateRange.D14,
    )
}

/**
 * Persists [UserSettings] in Preferences DataStore (replaces the web app's
 * `inw_user_settings_v1` localStorage entry). All writes are atomic and
 * survive process death.
 */
class SettingsDataStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<UserSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            UserSettings(
                currentWeightKg = prefs[KEY_CURRENT_WEIGHT] ?: DefaultSettings.value.currentWeightKg,
                targetWeightKg = prefs[KEY_TARGET_WEIGHT] ?: DefaultSettings.value.targetWeightKg,
                heightCm = prefs[KEY_HEIGHT_CM] ?: DefaultSettings.value.heightCm,
                ageYears = prefs[KEY_AGE_YEARS] ?: DefaultSettings.value.ageYears,
                sex = prefs[KEY_SEX].toEnum(Sex.M),
                activityLevel = prefs[KEY_ACTIVITY].toEnum(ActivityLevel.MODERATE),
                goalType = prefs[KEY_GOAL].toEnum(GoalType.LOSE),
                goalRateKgPerWeek = prefs[KEY_GOAL_RATE] ?: DefaultSettings.value.goalRateKgPerWeek,
                dailyCalorieTarget = prefs[KEY_CAL_TARGET] ?: DefaultSettings.value.dailyCalorieTarget,
                dailyProteinTarget = prefs[KEY_PROTEIN_TARGET] ?: DefaultSettings.value.dailyProteinTarget,
                dailyWaterTargetMl = prefs[KEY_WATER_TARGET] ?: DefaultSettings.value.dailyWaterTargetMl,
                proteinBasis = prefs[KEY_PROTEIN_BASIS].toEnum(ProteinBasis.CURRENT),
                unitSystem = prefs[KEY_UNIT_SYSTEM].toEnum(UnitSystem.KG),
                defaultChartRange = prefs[KEY_CHART_RANGE].toEnum(DateRange.D14),
            )
        }

    suspend fun save(settings: UserSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_CURRENT_WEIGHT] = settings.currentWeightKg
            prefs[KEY_TARGET_WEIGHT] = settings.targetWeightKg
            prefs[KEY_HEIGHT_CM] = settings.heightCm
            prefs[KEY_AGE_YEARS] = settings.ageYears
            prefs[KEY_SEX] = settings.sex.name
            prefs[KEY_ACTIVITY] = settings.activityLevel.name
            prefs[KEY_GOAL] = settings.goalType.name
            prefs[KEY_GOAL_RATE] = settings.goalRateKgPerWeek
            prefs[KEY_CAL_TARGET] = settings.dailyCalorieTarget
            prefs[KEY_PROTEIN_TARGET] = settings.dailyProteinTarget
            prefs[KEY_WATER_TARGET] = settings.dailyWaterTargetMl
            prefs[KEY_PROTEIN_BASIS] = settings.proteinBasis.name
            prefs[KEY_UNIT_SYSTEM] = settings.unitSystem.name
            prefs[KEY_CHART_RANGE] = settings.defaultChartRange.name
        }
    }

    private fun <T : Enum<T>> String?.toEnum(fallback: T): T {
        if (this == null) return fallback
        @Suppress("UNCHECKED_CAST")
        val enumClass = fallback.javaClass as Class<T>
        return runCatching { java.lang.Enum.valueOf(enumClass, this) }.getOrDefault(fallback)
    }

    private companion object {
        val KEY_CURRENT_WEIGHT = doublePreferencesKey("current_weight_kg")
        val KEY_TARGET_WEIGHT = doublePreferencesKey("target_weight_kg")
        val KEY_HEIGHT_CM = doublePreferencesKey("height_cm")
        val KEY_AGE_YEARS = intPreferencesKey("age_years")
        val KEY_SEX = stringPreferencesKey("sex")
        val KEY_ACTIVITY = stringPreferencesKey("activity_level")
        val KEY_GOAL = stringPreferencesKey("goal_type")
        val KEY_GOAL_RATE = doublePreferencesKey("goal_rate_kg_per_week")
        val KEY_CAL_TARGET = intPreferencesKey("daily_calorie_target")
        val KEY_PROTEIN_TARGET = intPreferencesKey("daily_protein_target")
        val KEY_WATER_TARGET = intPreferencesKey("daily_water_target_ml")
        val KEY_PROTEIN_BASIS = stringPreferencesKey("protein_basis")
        val KEY_UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val KEY_CHART_RANGE = stringPreferencesKey("default_chart_range")
    }
}
