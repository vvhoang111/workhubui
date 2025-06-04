//package com.workhubui.screens
//
//import android.app.Application
//import android.os.Build
//import androidx.annotation.RequiresApi
//// ... (các import khác giữ nguyên hoặc thêm nếu cần)
//import androidx.compose.foundation.layout.padding // Thêm nếu chưa có
//import androidx.compose.material3.Scaffold // Thêm nếu chưa có
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue // Thêm nếu chưa có
//import androidx.compose.ui.Modifier // Thêm nếu chưa có
//import androidx.compose.ui.platform.LocalContext
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavDestination.Companion.hierarchy // Thêm import này
//import androidx.navigation.NavGraph.Companion.findStartDestination // Thêm import này
//import androidx.navigation.NavType
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.currentBackStackEntryAsState
//import androidx.navigation.compose.rememberNavController
//import androidx.navigation.navArgument
//import com.workhubui.components.BottomNavBar
//import com.workhubui.navigation.Routes
//import com.workhubui.screens.auth.AuthViewModel
//import com.workhubui.screens.auth.AuthViewModelFactory
//import com.workhubui.screens.auth.LoginScreen
//import com.workhubui.screens.auth.SignupScreen
//import com.workhubui.screens.chat.ChatListScreen // << THÊM IMPORT CHO MÀN HÌNH MỚI
//import com.workhubui.screens.chat.ChatScreen
//import com.workhubui.screens.home.HomeScreen
//import com.workhubui.screens.schedule.ScheduleScreen
//import com.workhubui.screens.setting.SettingScreen
//import com.workhubui.screens.taskboard.TaskboardScreen
//import com.workhubui.screens.vault.VaultScreen
//
//
//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun MainScreen() {
//    val navController = rememberNavController()
//    val application = LocalContext.current.applicationContext as Application
//    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application)) // [cite: 185]
//
//    val navBackStackEntry by navController.currentBackStackEntryAsState()
//    val currentDestination = navBackStackEntry?.destination
//
//    // Xác định các route mà BottomNavBar nên hiển thị
//    val bottomBarRoutes = setOf(
//        Routes.HOME,
//        Routes.SCHEDULE,
//        Routes.CHAT_LIST, // BottomNav sẽ trỏ đến CHAT_LIST
//        Routes.TASKBOARD,
//        Routes.VAULT
//    )
//    // Điều kiện để hiển thị BottomBar: route hiện tại nằm trong danh sách bottomBarRoutes
//    // HOẶC route hiện tại là CHAT (chi tiết) và chúng ta muốn tab CHAT_LIST vẫn active
//    val showBottomBar = currentDestination?.route in bottomBarRoutes ||
//            (currentDestination?.route?.startsWith(Routes.CHAT + "/") == true && Routes.CHAT_LIST in bottomBarRoutes)
//
//
//    Scaffold(
//        bottomBar = {
//            if (showBottomBar) {
//                BottomNavBar(navController = navController)
//            }
//        }
//    ) { innerPadding ->
//        NavHost(
//            navController = navController,
//            startDestination = Routes.SPLASH,
//            modifier = Modifier.padding(innerPadding)
//        ) {
//            composable(Routes.SPLASH) {
//                SplashScreen(navController)
//            }
//            composable(Routes.LOGIN) { // [cite: 307]
//                LoginScreen(navController) // [cite: 307]
//            }
//            composable(Routes.SIGNUP) { // [cite: 186]
//                SignupScreen(navController) // [cite: 186]
//            }
//            composable(Routes.HOME) { // [cite: 307]
//                HomeScreen(navController = navController) // [cite: 184, 307]
//            }
//
//            composable(Routes.CHAT_LIST) { // << ĐỊNH NGHĨA ROUTE CHO CHAT_LIST
//                ChatListScreen(navController = navController) // Tạo Composable này
//            }
//
//            composable(
//                route = Routes.CHAT + "/{currentUser}/{chatWith}", // Route cho chi tiết cuộc trò chuyện [cite: 22]
//                arguments = listOf(
//                    navArgument("currentUser") { type = NavType.StringType }, // [cite: 21, 22]
//                    navArgument("chatWith")    { type = NavType.StringType } // [cite: 21, 22]
//                )
//            ) { backStackEntry -> // Đổi tên biến để tránh trùng lặp
//                val currentUserArg = backStackEntry.arguments?.getString("currentUser") ?: "unknown_user" // [cite: 23, 188]
//                val chatWithArg    = backStackEntry.arguments?.getString("chatWith")    ?: "unknown_chat" // [cite: 23, 188]
//                ChatScreen(
//                    navController = navController,
//                    currentUser   = currentUserArg, // [cite: 189]
//                    chatWith      = chatWithArg // [cite: 189, 310]
//                )
//            }
//
//            composable(Routes.SCHEDULE) { // [cite: 310]
//                ScheduleScreen() // [cite: 189, 310]
//            }
//            composable(Routes.SETTINGS) { // [cite: 311]
//                SettingScreen(
//                    onBack   = { navController.popBackStack() }, // [cite: 311]
//                    onLogout = { // [cite: 312]
//                        authViewModel.logoutUser() // [cite: 190]
//                        navController.navigate(Routes.LOGIN) { // [cite: 190]
//                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true } // [cite: 191]
//                            launchSingleTop = true
//                        }
//                    }
//                )
//            }
//            composable(Routes.TASKBOARD) { // [cite: 312]
//                TaskboardScreen() // [cite: 312]
//            }
//            composable(Routes.VAULT) { // [cite: 312]
//                VaultScreen() // [cite: 312]
//            }
//        }
//    }
//}
package com.workhubui.screens

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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.workhubui.components.BottomNavBar
import com.workhubui.navigation.Routes
import com.workhubui.screens.auth.AuthViewModel
import com.workhubui.screens.auth.AuthViewModelFactory
import com.workhubui.screens.auth.LoginScreen
import com.workhubui.screens.auth.SignupScreen
import com.workhubui.screens.chat.ChatListScreen
import com.workhubui.screens.chat.ChatScreen
import com.workhubui.screens.home.HomeScreen
import com.workhubui.screens.setting.SettingScreen
import com.workhubui.screens.setting.ProfileScreen
import com.workhubui.screens.vault.VaultScreen
import com.workhubui.screens.chat.AddFriendScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Xác định các route mà BottomNavBar nên hiển thị
    val bottomBarRoutes = setOf(
        Routes.HOME,
        Routes.CHAT_LIST, // BottomNav sẽ trỏ đến CHAT_LIST
        Routes.VAULT,
        Routes.SETTINGS // Thêm Settings vào BottomNav
    )
    // Điều kiện để hiển thị BottomBar: route hiện tại nằm trong danh sách bottomBarRoutes
    // HOẶC route hiện tại là CHAT (chi tiết) và chúng ta muốn tab CHAT_LIST vẫn active
    val showBottomBar = currentDestination?.route in bottomBarRoutes ||
            (currentDestination?.route?.startsWith(Routes.CHAT + "/") == true && Routes.CHAT_LIST in bottomBarRoutes) ||
            currentDestination?.route == Routes.PROFILE || // Hiển thị BottomBar khi ở Profile
            currentDestination?.route == Routes.ADD_FRIEND // Hiển thị BottomBar khi ở AddFriend


    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        AppNavGraph(navController = navController, authViewModel = authViewModel) // Sử dụng AppNavGraph
    }
}