package com.zaid.speedtrail.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zaid.speedtrail.ui.detail.DetailScreen
import com.zaid.speedtrail.ui.history.HistoryScreen
import com.zaid.speedtrail.ui.live.LiveScreen

object Routes {
    const val LIVE = "live"
    const val HISTORY = "history"
    const val DETAIL = "detail/{tripId}"
    fun detail(tripId: Long) = "detail/$tripId"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.LIVE) {
        composable(Routes.LIVE) {
            LiveScreen(onOpenHistory = { nav.navigate(Routes.HISTORY) })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { nav.popBackStack() },
                onOpenTrip = { tripId -> nav.navigate(Routes.detail(tripId)) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId") ?: return@composable
            DetailScreen(tripId = tripId, onBack = { nav.popBackStack() })
        }
    }
}
