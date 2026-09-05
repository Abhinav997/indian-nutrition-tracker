package com.indian.nutrition.tracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.indian.nutrition.tracker.di.AppContainer
import com.indian.nutrition.tracker.domain.model.MealType
import com.indian.nutrition.tracker.ui.navigation.AppDestination
import com.indian.nutrition.tracker.ui.screens.calculator.CalculatorScreen
import com.indian.nutrition.tracker.ui.screens.home.HomeScreen
import com.indian.nutrition.tracker.ui.screens.progress.ProgressScreen
import com.indian.nutrition.tracker.ui.screens.search.FoodSearchScreen
import com.indian.nutrition.tracker.ui.theme.NutritionTrackerTheme
import com.indian.nutrition.tracker.util.DateUtils
import java.time.LocalDate

private const val SEARCH_MEAL_ARG = "meal"
private const val SEARCH_DATE_ARG = "date"

/** Search route with optional meal and date presets. */
private const val SEARCH_ROUTE_PATTERN = "search?meal={$SEARCH_MEAL_ARG}&date={$SEARCH_DATE_ARG}"

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
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag("bottom-navigation-bar")) {
                    AppDestination.all.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy
                                ?.any { it.route == destination.route || it.route?.startsWith("${destination.route}?") == true } == true,
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
                composable(AppDestination.Home.route) {
                    HomeScreen(
                        container = container,
                        onOpenFoodSearch = { meal, date ->
                            navController.navigate(
                                "search?$SEARCH_MEAL_ARG=${meal.name}&$SEARCH_DATE_ARG=$date",
                            ) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(
                    route = SEARCH_ROUTE_PATTERN,
                    arguments = listOf(
                        navArgument(SEARCH_MEAL_ARG) {
                            type = NavType.StringType
                            defaultValue = MealType.LUNCH.name
                        },
                        navArgument(SEARCH_DATE_ARG) {
                            type = NavType.StringType
                            defaultValue = DateUtils.today().toString()
                        },
                    ),
                ) { entry ->
                    val mealName = entry.arguments?.getString(SEARCH_MEAL_ARG)
                    val meal = MealType.entries.firstOrNull { it.name == mealName } ?: MealType.LUNCH
                    val loggingDate = entry.arguments?.getString(SEARCH_DATE_ARG)
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        ?: DateUtils.today()
                    FoodSearchScreen(
                        container = container,
                        initialMeal = meal,
                        loggingDate = loggingDate,
                        snackbarHostState = snackbarHostState,
                    )
                }
                composable(AppDestination.Progress.route) { ProgressScreen(container) }
                composable(AppDestination.Calculator.route) {
                    CalculatorScreen(container, snackbarHostState)
                }
            }
        }
    }
}
