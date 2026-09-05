package com.indian.nutrition.tracker.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.indian.nutrition.tracker.data.local.DefaultSettings
import com.indian.nutrition.tracker.data.local.SettingsDataStore
import com.indian.nutrition.tracker.domain.model.ActivityLevel
import com.indian.nutrition.tracker.domain.model.DateRange
import com.indian.nutrition.tracker.domain.model.GoalType
import com.indian.nutrition.tracker.domain.model.ProteinBasis
import com.indian.nutrition.tracker.domain.model.Sex
import com.indian.nutrition.tracker.domain.model.UnitSystem
import com.indian.nutrition.tracker.domain.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * JVM tests for settings persistence using the multiplatform DataStore core
 * (real file-backed store, no Android runtime required).
 */
class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun repository(file: File, scope: CoroutineScope): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        return SettingsRepository(SettingsDataStore(dataStore))
    }

    private fun customSettings() = UserSettings(
        currentWeightKg = 70.5,
        targetWeightKg = 65.0,
        heightCm = 165.0,
        ageYears = 34,
        sex = Sex.F,
        activityLevel = ActivityLevel.ACTIVE,
        goalType = GoalType.GAIN,
        goalRateKgPerWeek = 0.25,
        dailyCalorieTarget = 2400,
        dailyProteinTarget = 120,
        dailyWaterTargetMl = 3000,
        proteinBasis = ProteinBasis.TARGET,
        unitSystem = UnitSystem.LB,
        defaultChartRange = DateRange.D30,
    )

    @Test
    fun unsetStoreReturnsWebDefaults() = runTest {
        val repo = repository(tmp.newFile("defaults.preferences_pb"), backgroundScope)
        assertEquals(DefaultSettings.value, repo.settings.first())
    }

    @Test
    fun saveThenReadRoundTrips() = runTest {
        val repo = repository(tmp.newFile("roundtrip.preferences_pb"), backgroundScope)
        repo.save(customSettings())
        assertEquals(customSettings(), repo.settings.first())
    }

    @Test
    fun updatesOverridePreviouslySavedValues() = runTest {
        val repo = repository(tmp.newFile("update.preferences_pb"), backgroundScope)
        repo.save(customSettings())
        assertEquals(customSettings(), repo.settings.first())

        val updated = customSettings().copy(dailyCalorieTarget = 2600, goalType = GoalType.MAINTAIN)
        repo.save(updated)
        assertEquals(updated, repo.settings.first())
    }

    @Test
    fun corruptStoreFallsBackToDefaults() = runTest {
        val file: File = tmp.newFile("corrupt.preferences_pb")
        file.writeBytes(byteArrayOf(0x7F, 0x01, 0x03)) // not a valid preferences file

        val repo = repository(file, backgroundScope)
        // The store surfaces a CorruptionException (an IOException); our
        // catch in SettingsDataStore emits empty prefs → web defaults.
        assertEquals(DefaultSettings.value, repo.settings.first())
    }
}
