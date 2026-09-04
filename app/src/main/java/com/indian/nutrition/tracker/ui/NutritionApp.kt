package com.indian.nutrition.tracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.ui.navigation.AppDestination
import com.indian.nutrition.tracker.ui.screens.calculator.CalculatorScreen
import com.indian.nutrition.tracker.ui.screens.home.HomeScreen
import com.indian.nutrition.tracker.ui.screens.progress.ProgressScreen
import com.indian.nutrition.tracker.ui.screens.search.FoodSearchScreen
import com.indian.nutrition.tracker.ui.theme.NutritionTrackerTheme

/**
 * Root composable: Material 3 theme, app scaffold with bottom navigation,
 * and the Navigation Compose graph for the four main destinations.
 */
@Composable
fun NutritionApp(container: AppContainer) {
    NutritionTrackerTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val current = AppDestination.fromRoute(currentDestination?.route)

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    AppDestination.all.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy
                                ?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(AppDestination.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(AppDestination.Home.route) { HomeScreen(container) }
                composable(AppDestination.FoodSearch.route) { FoodSearchScreen() }
                composable(AppDestination.Progress.route) { ProgressScreen() }
                composable(AppDestination.Calculator.route) { CalculatorScreen() }
            }
        }
    }
}
