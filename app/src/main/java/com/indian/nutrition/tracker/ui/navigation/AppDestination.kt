package com.indian.nutrition.tracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

/** The four main tabs of the app (matches the web app's bottom navigation). */
sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : AppDestination("home", "Today", Icons.Filled.Home)
    data object FoodSearch : AppDestination("search", "Food Log", Icons.Filled.Search)
    data object Progress : AppDestination("progress", "Progress", Icons.Filled.TrendingUp)
    data object Calculator : AppDestination("calculator", "Calculator", Icons.Filled.Calculate)

    val sublabel: String
        get() = when (this) {
            Home -> "Daily Summary"
            FoodSearch -> "Search & Log"
            Progress -> "Charts & Stats"
            Calculator -> "TDEE & Settings"
        }

    companion object {
        val all: List<AppDestination> = listOf(Home, FoodSearch, Progress, Calculator)

        /** Bottom-nav order: [route] → destination, tolerant of unknown routes. */
        fun fromRoute(route: String?): AppDestination =
            all.firstOrNull { it.route == route } ?: Home
    }
}
