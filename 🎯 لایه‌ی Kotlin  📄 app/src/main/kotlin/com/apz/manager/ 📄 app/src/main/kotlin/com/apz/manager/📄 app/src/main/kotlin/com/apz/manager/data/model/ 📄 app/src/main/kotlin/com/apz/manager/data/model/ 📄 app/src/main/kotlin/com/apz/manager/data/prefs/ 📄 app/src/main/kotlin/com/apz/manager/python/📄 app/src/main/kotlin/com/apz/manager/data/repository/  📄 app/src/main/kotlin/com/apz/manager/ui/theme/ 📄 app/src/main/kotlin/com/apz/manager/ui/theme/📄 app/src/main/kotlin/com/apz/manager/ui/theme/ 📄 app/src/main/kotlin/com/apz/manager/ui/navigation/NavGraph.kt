```kotlin
package com.apz.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.apz.manager.ui.screens.AnalysisScreen
import com.apz.manager.ui.screens.HistoryScreen
import com.apz.manager.ui.screens.HomeScreen
import com.apz.manager.ui.screens.ReportScreen
import com.apz.manager.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val ANALYSIS = "analysis"
    const val REPORT = "report/{sha256}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun report(sha: String) = "report/$sha"
}

@Composable
fun NavGraph(nav: NavHostController) {
    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onAnalyze = { nav.navigate(Routes.ANALYSIS) },
                onHistory = { nav.navigate(Routes.HISTORY) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ANALYSIS) {
            AnalysisScreen(
                onBack = { nav.popBackStack() },
                onDone = { sha ->
                    nav.navigate(Routes.report(sha)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.REPORT) { entry ->
            val sha = entry.arguments?.getString("sha256").orEmpty()
            ReportScreen(
                sha256 = sha,
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { nav.popBackStack() },
                onOpen = { sha -> nav.navigate(Routes.report(sha)) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
```
