package com.cdcs.screens

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cdcs.components.BottomNavBar
import com.cdcs.navigation.AppNavGraph
import com.cdcs.navigation.Routes
import com.cdcs.screens.auth.AuthViewModel
import com.cdcs.screens.auth.AuthViewModelFactory

/**
 * Composable chính của ứng dụng, thiết lập Scaffold và Navigation.
 * Nó nhận một NavController từ bên ngoài (cụ thể là từ MainActivity).
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Các route mà BottomNavBar sẽ được hiển thị
    val bottomBarRoutes = setOf(
        Routes.HOME,
        Routes.CHAT_LIST,
        Routes.VAULT,
        Routes.SETTINGS
    )

    // Điều kiện để quyết định có hiển thị BottomNavBar hay không
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
    ) { innerPadding ->
        // Truyền NavController và innerPadding vào AppNavGraph
        AppNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
