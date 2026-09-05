package com.indian.nutrition.tracker.data.repository

import com.indian.nutrition.tracker.data.local.SettingsDataStore
import com.indian.nutrition.tracker.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** Single access point for user settings. */
class SettingsRepository(private val settingsDataStore: SettingsDataStore) {

    val settings: Flow<UserSettings> = settingsDataStore.settings

    suspend fun save(settings: UserSettings) = settingsDataStore.save(settings)
}
