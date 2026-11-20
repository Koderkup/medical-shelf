package com.example.medical_shelf.ui.navigation

sealed class Screen(val route: String) {
    object Intake : Screen("intake")
    object Shelf : Screen("shelf")
    object AddMedicine : Screen("add_medicine?medId={medId}") {
        fun createRoute(medId: Long? = null): String {
            return if (medId != null && medId > 0) "add_medicine?medId=$medId" else "add_medicine"
        }
    }
    object Settings : Screen("settings")
    object Paywall : Screen("paywall")
    object Checklist : Screen("checklist/{boxId}") {
        fun createRoute(boxId: Long): String = "checklist/$boxId"
    }
    object Scanner : Screen("scanner/{mode}") {
        fun createRoute(mode: String): String = "scanner/$mode"
    }
}