//package com.workhubui.components
//
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.Chat
//import androidx.compose.material.icons.automirrored.filled.List
//import androidx.compose.material.icons.filled.DateRange
//import androidx.compose.material.icons.filled.Home
//import androidx.compose.material.icons.filled.Lock
//import androidx.compose.material3.Icon
//import androidx.compose.material3.NavigationBar
//import androidx.compose.material3.NavigationBarItem
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.navigation.NavDestination.Companion.hierarchy // Thêm import này
//import androidx.navigation.NavGraph.Companion.findStartDestination // Thêm import này
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.currentBackStackEntryAsState
//import com.workhubui.navigation.Routes
//
//@Composable
//fun BottomNavBar(navController: NavHostController) {
//    val items = listOf(
//        BottomNavItem("Home", Icons.Filled.Home, Routes.HOME), // [cite: 2]
//        BottomNavItem("Schedule", Icons.Filled.DateRange, Routes.SCHEDULE), // [cite: 2]
//        BottomNavItem("Chat", Icons.AutoMirrored.Filled.Chat, Routes.CHAT_LIST), // << ĐIỀU HƯỚNG ĐẾN CHAT_LIST [cite: 2]
//        BottomNavItem("Taskboard", Icons.AutoMirrored.Filled.List, Routes.TASKBOARD), // [cite: 2]
//        BottomNavItem("Vault", Icons.Filled.Lock, Routes.VAULT) // [cite: 2]
//    )
//    val navBackStackEntry by navController.currentBackStackEntryAsState()
//    val currentDestination = navBackStackEntry?.destination
//
//    NavigationBar {
//        items.forEach { item ->
//            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true ||
//                    (item.route == Routes.CHAT_LIST && currentDestination?.route?.startsWith(Routes.CHAT + "/") == true)
//
//            NavigationBarItem(
//                icon = { Icon(item.icon, contentDescription = item.label) }, // [cite: 3]
//                label = { Text(item.label) },
//                selected = selected,
//                onClick = {
//                    navController.navigate(item.route) {
//                        // Pop up to the start destination of the graph to
//                        // avoid building up a large stack of destinations
//                        // on the back stack as users select items
//                        popUpTo(navController.graph.findStartDestination().id) {
//                            saveState = true
//                        }
//                        // Avoid multiple copies of the same destination when
//                        // reselecting the same item
//                        launchSingleTop = true // [cite: 4]
//                        // Restore state when reselecting a previously selected item
//                        restoreState = true
//                    }
//                }
//            )
//        }
//    }
//}
//
//data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)
package com.cdcs.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings // Import icon Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cdcs.navigation.Routes

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Home", Icons.Filled.Home, Routes.HOME),
        // BottomNavItem("Schedule", Icons.Filled.DateRange, Routes.SCHEDULE), // Xóa Schedule khỏi BottomNav
        BottomNavItem("Chat", Icons.AutoMirrored.Filled.Chat, Routes.CHAT_LIST), // Điều hướng đến CHAT_LIST
        // BottomNavItem("Taskboard", Icons.AutoMirrored.Filled.List, Routes.TASKBOARD), // Xóa Taskboard khỏi BottomNav
        BottomNavItem("Vault", Icons.Filled.Lock, Routes.VAULT),
        BottomNavItem("Settings", Icons.Filled.Settings, Routes.SETTINGS) // Thêm Settings vào BottomNav
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == item.route ||
                        // Nếu route hiện tại là CHAT (chi tiết), vẫn highlight CHAT_LIST
                        (item.route == Routes.CHAT_LIST && it.route?.startsWith(Routes.CHAT + "/") == true) ||
                        // Nếu route hiện tại là PROFILE hoặc ADD_FRIEND, vẫn highlight SETTINGS
                        (item.route == Routes.SETTINGS && (it.route == Routes.PROFILE || it.route == Routes.ADD_FRIEND))
            } == true

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)