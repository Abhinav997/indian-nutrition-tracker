package com.indian.nutrition.tracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** The app's destinations; the food log is presented as a focused overlay. */
sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : AppDestination("home", "Dashboard", Icons.Filled.Home)
    data object FoodSearch : AppDestination("search", "Food Log", Icons.Filled.Search)
    data object Progress : AppDestination("progress", "Progress", Icons.AutoMirrored.Filled.TrendingUp)
    data object Calculator : AppDestination("calculator", "Calculator", Icons.Filled.Calculate)

    val sublabel: String
        get() = when (this) {
            Home -> "Daily Summary"
            FoodSearch -> "Search & Log"
            Progress -> "Charts & Stats"
            Calculator -> "TDEE & Settings"
        }

    companion object {
        /** Destinations represented by the persistent bottom navigation bar. */
        val bottomNav: List<AppDestination> = listOf(Home, Progress, Calculator)

        /** All destinations, including the food-log overlay route. */
        val all: List<AppDestination> = listOf(Home, FoodSearch, Progress, Calculator)

        /**
         * Route lookup is tolerant of query arguments such as
         * `search?meal=BREAKFAST&date=2026-09-05`.
         */
        fun fromRoute(route: String?): AppDestination =
            all.firstOrNull { it.route == route || route?.startsWith("${it.route}?") == true }
                ?: Home
    }
}
