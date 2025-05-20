package com.workhubui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workhubui.screens.Routes
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LOGIN) { LoginScreen(navController) }
            composable(Routes.HOME) { HomeScreen(navController) }
            composable(Routes.SCHEDULE) { ScheduleScreen() }
            composable(Routes.CHAT) { ChatScreen() }
            composable(Routes.TASKBOARD) { TaskboardScreen() }
            composable(Routes.VAULT) { VaultScreen() }
        }
    }
}

