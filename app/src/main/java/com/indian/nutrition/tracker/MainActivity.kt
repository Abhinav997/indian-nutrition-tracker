package com.indian.nutrition.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.indian.nutrition.tracker.ui.NutritionApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as IndianNutritionTrackerApp).container
        setContent {
            NutritionApp(container = container)
        }
    }
}
