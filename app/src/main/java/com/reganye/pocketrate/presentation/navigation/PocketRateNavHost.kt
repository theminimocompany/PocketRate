package com.reganye.pocketrate.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.reganye.pocketrate.presentation.ui.charts.HistoricalChartScreen
import com.reganye.pocketrate.presentation.ui.converter.ConverterScreen
import com.reganye.pocketrate.presentation.ui.onboarding.OnboardingScreen
import com.reganye.pocketrate.presentation.ui.settings.SettingsScreen
import com.reganye.pocketrate.presentation.ui.trips.AddExpenseScreen
import com.reganye.pocketrate.presentation.ui.trips.CategoryBreakdownScreen
import com.reganye.pocketrate.presentation.ui.trips.CreateTripScreen
import com.reganye.pocketrate.presentation.ui.trips.SplitCostsScreen
import com.reganye.pocketrate.presentation.ui.trips.TripDetailScreen
import com.reganye.pocketrate.presentation.ui.trips.TripsListScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val CONVERTER = "converter"
    const val TRIPS = "trips"
    const val CREATE_TRIP = "create_trip"
    const val TRIP_DETAIL = "trip_detail/{tripId}"
    fun tripDetail(tripId: String) = "trip_detail/$tripId"
    const val ADD_EXPENSE = "add_expense/{tripId}?expenseId={expenseId}"
    fun addExpense(tripId: String, expenseId: String? = null) =
        if (expenseId != null) "add_expense/$tripId?expenseId=$expenseId" else "add_expense/$tripId"
    const val CATEGORY_BREAKDOWN = "category_breakdown/{tripId}"
    fun categoryBreakdown(tripId: String) = "category_breakdown/$tripId"
    const val SPLIT_COSTS = "split_costs/{tripId}"
    fun splitCosts(tripId: String) = "split_costs/$tripId"
    const val HISTORICAL_CHART = "historical_chart/{fromCurrency}/{toCurrency}"
    fun historicalChart(fromCurrency: String, toCurrency: String) = "historical_chart/$fromCurrency/$toCurrency"
    const val SETTINGS = "settings"
}

@Composable
fun PocketRateNavHost(
    viewModel: PocketRateNavHostViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

    // The splash screen is kept on screen until onboardingCompleted is non-null,
    // so this branch is only a defensive fallback.
    if (onboardingCompleted == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val startDestination = if (onboardingCompleted == true) Routes.CONVERTER else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Routes.CONVERTER) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CONVERTER) {
            ConverterScreen(
                onNavigateToTrips = { navController.navigate(Routes.TRIPS) },
                onNavigateToCharts = { from, to ->
                    navController.navigate(Routes.historicalChart(from, to))
                },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.TRIPS) {
            TripsListScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateTrip = { navController.navigate(Routes.CREATE_TRIP) },
                onTripSelected = { navController.navigate(Routes.tripDetail(it)) }
            )
        }
        composable(Routes.CREATE_TRIP) {
            CreateTripScreen(
                onTripCreated = {
                    navController.popBackStack()
                    navController.navigate(Routes.tripDetail(it))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.TRIP_DETAIL) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            TripDetailScreen(
                tripId = tripId,
                onNavigateBack = { navController.popBackStack() },
                onAddExpense = { navController.navigate(Routes.addExpense(tripId)) },
                onEditExpense = { expenseId ->
                    navController.navigate(Routes.addExpense(tripId, expenseId))
                },
                onCategoryBreakdown = { navController.navigate(Routes.categoryBreakdown(tripId)) },
                onSplitCosts = { navController.navigate(Routes.splitCosts(tripId)) }
            )
        }
        composable(
            route = Routes.ADD_EXPENSE,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            AddExpenseScreen(
                tripId = tripId,
                onExpenseSaved = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CATEGORY_BREAKDOWN) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            CategoryBreakdownScreen(
                tripId = tripId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SPLIT_COSTS) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            SplitCostsScreen(
                tripId = tripId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.HISTORICAL_CHART,
            arguments = listOf(
                navArgument("fromCurrency") { type = NavType.StringType },
                navArgument("toCurrency") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val fromCurrency = backStackEntry.arguments?.getString("fromCurrency") ?: ""
            val toCurrency = backStackEntry.arguments?.getString("toCurrency") ?: ""
            HistoricalChartScreen(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
