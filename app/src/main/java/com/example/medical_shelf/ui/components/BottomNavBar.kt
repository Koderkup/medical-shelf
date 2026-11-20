package com.example.medical_shelf.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.medical_shelf.R
import com.example.medical_shelf.ui.navigation.Screen

data class NavItem(
    val screen: Screen,
    val titleResId: Int,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    NavItem(Screen.Intake, R.string.nav_intake, Icons.Default.Alarm),
    NavItem(Screen.Shelf, R.string.nav_shelf, Icons.Default.MedicalServices),
    NavItem(Screen.AddMedicine, R.string.nav_add, Icons.Default.AddCircle),
    NavItem(Screen.Settings, R.string.nav_settings, Icons.Default.Settings)
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute?.startsWith(item.screen.route.split("?")[0].split("/")[0]) == true
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.screen) },
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                label = { Text(text = stringResource(id = item.titleResId)) }
            )
        }
    }
}