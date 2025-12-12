package com.example.runlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.runlogger.ui.screens.AddEditRunScreen
import com.example.runlogger.ui.screens.CalendarScreen
import com.example.runlogger.ui.screens.MarathonTrainingScreen
import com.example.runlogger.ui.screens.RunDetailScreen
import com.example.runlogger.ui.screens.RunListScreen
import com.example.runlogger.ui.screens.SplashScreen
import com.example.runlogger.ui.screens.SummaryScreen
import com.example.runlogger.ui.theme.RunLoggerTheme
import com.example.runlogger.viewmodel.RunViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunLoggerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RunLoggerApp()
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object RunList : Screen("run_list")
    data object Summary : Screen("summary")
    data object Calendar : Screen("calendar")
    data object MarathonTraining : Screen("marathon_training")
    data object AddRun : Screen("add_run")
    data object EditRun : Screen("edit_run/{runId}") {
        fun createRoute(runId: Int) = "edit_run/$runId"
    }
    data object RunDetail : Screen("run_detail/{runId}") {
        fun createRoute(runId: Int) = "run_detail/$runId"
    }
}

@Composable
fun RunLoggerApp() {
    val navController = rememberNavController()
    val viewModel: RunViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.RunList.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.RunList.route) {
            RunListScreen(
                viewModel = viewModel,
                onAddRunClick = {
                    navController.navigate(Screen.AddRun.route)
                },
                onRunClick = { runId ->
                    navController.navigate(Screen.RunDetail.createRoute(runId))
                },
                onSummaryClick = {
                    navController.navigate(Screen.Summary.route)
                },
                onCalendarClick = {
                    navController.navigate(Screen.Calendar.route)
                },
                onMarathonTrainingClick = {
                    navController.navigate(Screen.MarathonTraining.route)
                }
            )
        }
        
        composable(Screen.Summary.route) {
            SummaryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Calendar.route) {
            CalendarScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.MarathonTraining.route) {
            MarathonTrainingScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AddRun.route) {
            AddEditRunScreen(
                viewModel = viewModel,
                runId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.EditRun.route,
            arguments = listOf(navArgument("runId") { type = NavType.IntType })
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getInt("runId") ?: 0
            AddEditRunScreen(
                viewModel = viewModel,
                runId = runId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.RunDetail.route,
            arguments = listOf(navArgument("runId") { type = NavType.IntType })
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getInt("runId") ?: 0
            RunDetailScreen(
                viewModel = viewModel,
                runId = runId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditClick = { id ->
                    navController.navigate(Screen.EditRun.createRoute(id))
                }
            )
        }
    }
}
