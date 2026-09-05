package com.indian.nutrition.tracker

import android.app.Application
import com.indian.nutrition.tracker.di.AppContainer

class IndianNutritionTrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
