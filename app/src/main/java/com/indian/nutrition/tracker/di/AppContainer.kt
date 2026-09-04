package com.indian.nutrition.tracker.di

import android.content.Context
import com.indian.nutrition.tracker.data.local.SettingsDataStore
import com.indian.nutrition.tracker.data.local.inwSettingsDataStore
import com.indian.nutrition.tracker.data.repository.SettingsRepository

/**
 * Manual dependency container created once in
 * [com.indian.nutrition.tracker.IndianNutritionTrackerApp]. Kept simple —
 * the app is small enough that Hilt is unnecessary (see implementation plan).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(appContext.inwSettingsDataStore)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(settingsDataStore)
    }
}
