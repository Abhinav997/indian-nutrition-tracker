package com.indian.nutrition.tracker.di

import android.content.Context
import com.indian.nutrition.tracker.data.local.AppDatabase
import com.indian.nutrition.tracker.data.local.SettingsDataStore
import com.indian.nutrition.tracker.data.local.inwSettingsDataStore
import com.indian.nutrition.tracker.data.repository.CustomFoodRepository
import com.indian.nutrition.tracker.data.repository.FoodRepository
import com.indian.nutrition.tracker.data.repository.LogRepository
import com.indian.nutrition.tracker.data.repository.OffCacheRepository
import com.indian.nutrition.tracker.data.repository.SettingsRepository
import com.indian.nutrition.tracker.data.repository.WaterRepository
import com.indian.nutrition.tracker.data.repository.WeightRepository

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

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val foodRepository: FoodRepository by lazy { FoodRepository(appContext) }

    val logRepository: LogRepository by lazy { LogRepository(database.dailyLogDao()) }

    val weightRepository: WeightRepository by lazy { WeightRepository(database.weightLogDao()) }

    val waterRepository: WaterRepository by lazy { WaterRepository(database.waterLogDao()) }

    val customFoodRepository: CustomFoodRepository by lazy { CustomFoodRepository(database.customFoodDao()) }

    val offCacheRepository: OffCacheRepository by lazy { OffCacheRepository(database.offCacheDao()) }
}
