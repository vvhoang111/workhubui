package com.workhubui.screens

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding // Ensure this is imported
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier // Ensure this is imported
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.workhubui.components.BottomNavBar
import com.workhubui.navigation.AppNavGraph // Import AppNavGraph
import com.workhubui.navigation.Routes
import com.workhubui.screens.auth.AuthViewModel
import com.workhubui.screens.auth.AuthViewModelFactory

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomBarRoutes = setOf(
        Routes.HOME,
        Routes.CHAT_LIST,
        Routes.VAULT,
        Routes.SETTINGS
    )
    val showBottomBar = currentDestination?.route in bottomBarRoutes ||
            (currentDestination?.route?.startsWith(Routes.CHAT + "/") == true && Routes.CHAT_LIST in bottomBarRoutes) ||
            currentDestination?.route == Routes.PROFILE ||
            currentDestination?.route == Routes.ADD_FRIEND


    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding -> // This is the padding from the Scaffold
        AppNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            modifier = Modifier.padding(innerPadding) // Apply the padding to the NavHost via AppNavGraph
        )
    }
}
