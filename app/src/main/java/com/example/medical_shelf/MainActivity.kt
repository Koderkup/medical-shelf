package com.example.medical_shelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.medical_shelf.ui.components.BottomNavBar
import com.example.medical_shelf.ui.navigation.Screen
import com.example.medical_shelf.ui.screens.AddEditMedicineScreen
import com.example.medical_shelf.ui.screens.ChecklistScreen
import com.example.medical_shelf.ui.screens.IntakeScreen
import com.example.medical_shelf.ui.screens.PaywallScreen
import com.example.medical_shelf.ui.screens.ScannerScreen
import com.example.medical_shelf.ui.screens.SettingsScreen
import com.example.medical_shelf.ui.screens.ShelfScreen
import com.example.medical_shelf.ui.theme.MedicalshelfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalshelfTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = when (currentRoute) {
        Screen.Intake.route,
        Screen.Shelf.route,
        Screen.AddMedicine.route,
        Screen.Settings.route -> true
        else -> false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Intake.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Intake.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Intake.route) {
                IntakeScreen(
                    onOpenPaywall = { navController.navigate(Screen.Paywall.route) }
                )
            }

            composable(Screen.Shelf.route) {
                ShelfScreen(
                    onNavigateToAddMed = { medId, boxId ->
                        navController.navigate(Screen.AddMedicine.createRoute(medId, boxId))
                    },
                    onNavigateToChecklist = { boxId ->
                        navController.navigate(Screen.Checklist.createRoute(boxId))
                    },
                    onOpenPaywall = { navController.navigate(Screen.Paywall.route) }
                )
            }

            composable(
                route = Screen.AddMedicine.route,
                arguments = listOf(
                    navArgument("medId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("boxId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val medId = backStackEntry.arguments?.getLong("medId")
                val boxId = backStackEntry.arguments?.getLong("boxId")

                AddEditMedicineScreen(
                    medicineId = if (medId != null && medId > 0) medId else null,
                    defaultBoxId = if (boxId != null && boxId > 0) boxId else null,
                    onOpenPaywall = { navController.navigate(Screen.Paywall.route) },
                    onDone = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenPaywall = { navController.navigate(Screen.Paywall.route) }
                )
            }

            composable(Screen.Paywall.route) {
                PaywallScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Checklist.route,
                arguments = listOf(navArgument("boxId") { type = NavType.LongType })
            ) { backStackEntry ->
                val boxId = backStackEntry.arguments?.getLong("boxId") ?: 1L
                ChecklistScreen(boxId = boxId)
            }

            composable(
                route = Screen.Scanner.route,
                arguments = listOf(navArgument("mode") { type = NavType.StringType })
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "barcode"
                ScannerScreen(
                    initialMode = mode,
                    onOpenPaywall = { navController.navigate(Screen.Paywall.route) },
                    onResultFound = { barcode, name, date ->
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}