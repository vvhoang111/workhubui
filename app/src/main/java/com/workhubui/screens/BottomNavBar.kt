package com.workhubui.screens

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import com.workhubui.screens.Routes

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Home", Icons.Filled.Home, Routes.HOME),
        BottomNavItem("Schedule", Icons.Filled.DateRange, Routes.SCHEDULE),
        BottomNavItem("Chat", Icons.Filled.Chat, Routes.CHAT),
        BottomNavItem("Taskboard", Icons.Filled.List, Routes.TASKBOARD),
        BottomNavItem("Vault", Icons.Filled.Lock, Routes.VAULT)
    )
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true

                } }
            )
        }
    }
}

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

