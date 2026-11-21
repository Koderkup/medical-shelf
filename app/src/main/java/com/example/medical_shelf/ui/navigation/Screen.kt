package com.example.medical_shelf.ui.navigation

sealed class Screen(val route: String) {
    object Intake : Screen("intake")
    object Shelf : Screen("shelf")
    object AddMedicine : Screen("add_medicine?medId={medId}&boxId={boxId}") {
        fun createRoute(medId: Long? = null, boxId: Long? = null): String {
            val med = medId ?: -1L
            val box = boxId ?: -1L
            return "add_medicine?medId=$med&boxId=$box"
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