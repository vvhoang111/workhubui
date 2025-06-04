package com.workhubui.screens

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding // Đảm bảo Modifier.padding được import
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier // Đảm bảo Modifier được import
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.workhubui.components.BottomNavBar
import com.workhubui.navigation.AppNavGraph // Import AppNavGraph
import com.workhubui.navigation.Routes
import com.workhubui.screens.auth.AuthViewModel
import com.workhubui.screens.auth.AuthViewModelFactory
// HomeScreen được điều hướng bởi AppNavGraph, không cần import trực tiếp ở đây.
// KHÔNG import com.workhubui.screens.home.HomeScreen ở đây.

/**
 * Composable chính của ứng dụng, thiết lập Scaffold và Navigation.
 * Đây là nơi DUY NHẤT định nghĩa `fun MainScreen()`.
 */
@RequiresApi(Build.VERSION_CODES.O) // Cần thiết nếu AppNavGraph hoặc các màn hình con yêu cầu API level này
@Composable
fun MainScreen() { // << ĐẢM BẢO ĐÂY LÀ ĐỊNH NGHĨA DUY NHẤT
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Các route mà BottomNavBar sẽ hiển thị
    val bottomBarRoutes = setOf(
        Routes.HOME,
        Routes.CHAT_LIST,
        Routes.VAULT,
        Routes.SETTINGS
    )
    // Điều kiện để hiển thị BottomNavBar
    val showBottomBar = currentDestination?.route in bottomBarRoutes ||
            // Hiển thị BottomBar nếu đang ở màn hình chi tiết Chat (và CHAT_LIST là một tab)
            (currentDestination?.route?.startsWith(Routes.CHAT + "/") == true && Routes.CHAT_LIST in bottomBarRoutes) ||
            // Hiển thị BottomBar nếu đang ở màn hình Profile hoặc AddFriend (và SETTINGS là một tab)
            currentDestination?.route == Routes.PROFILE ||
            currentDestination?.route == Routes.ADD_FRIEND

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding -> // innerPadding được cung cấp bởi Scaffold
        // Truyền innerPadding vào AppNavGraph để áp dụng cho NavHost
        AppNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            modifier = Modifier.padding(innerPadding) // << ÁP DỤNG innerPadding CHO AppNavGraph
        )
    }
}
