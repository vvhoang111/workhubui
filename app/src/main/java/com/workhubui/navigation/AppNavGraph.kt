//package com.workhubui.navigation
//
//import androidx.compose.runtime.Composable
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.NavType
//import androidx.navigation.navArgument
//import com.workhubui.screens.chat.ChatScreen
//import com.workhubui.screens.home.HomeScreen
//
//
//@Composable
//fun AppNavGraph(navController: NavHostController) {
//    NavHost(
//        navController = navController,
//        startDestination = Routes.HOME
//    ) {
//        composable(Routes.HOME) {
//            HomeScreen(navController)
//        }
//        composable(
//            route = Routes.CHAT + "/{currentUser}/{chatWith}",
//            arguments = listOf(
//                navArgument("currentUser") { type = NavType.StringType },
//                navArgument("chatWith") { type = NavType.StringType }
//            )
//        ) { backStackEntry ->
//            val current = backStackEntry.arguments?.getString("currentUser")!!
//            val chatWith = backStackEntry.arguments?.getString("chatWith")!!
//            ChatScreen(navController, current, chatWith)
//        }
//    }
//}
package com.workhubui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.workhubui.screens.SplashScreen
import com.workhubui.screens.auth.LoginScreen
import com.workhubui.screens.auth.SignupScreen
import com.workhubui.screens.chat.ChatListScreen
import com.workhubui.screens.chat.ChatScreen
import com.workhubui.screens.home.HomeScreen
import com.workhubui.screens.setting.SettingScreen
import com.workhubui.screens.setting.ProfileScreen // Import ProfileScreen
import com.workhubui.screens.vault.VaultScreen
import com.workhubui.screens.chat.AddFriendScreen // Import AddFriendScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(navController: NavHostController, authViewModel: com.workhubui.screens.auth.AuthViewModel) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH // Bắt đầu từ Splash Screen
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(navController)
        }
        composable(Routes.LOGIN) {
            LoginScreen(navController)
        }
        composable(Routes.SIGNUP) {
            SignupScreen(navController)
        }
        composable(Routes.HOME) {
            HomeScreen(navController)
        }

        composable(Routes.CHAT_LIST) { // Route cho màn hình danh sách chat
            ChatListScreen(navController = navController)
        }

        composable(
            route = Routes.CHAT + "/{currentUser}/{chatWith}", // Route cho chi tiết cuộc trò chuyện
            arguments = listOf(
                navArgument("currentUser") { type = NavType.StringType },
                navArgument("chatWith") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val currentUserArg = backStackEntry.arguments?.getString("currentUser") ?: "unknown_user"
            val chatWithArg = backStackEntry.arguments?.getString("chatWith") ?: "unknown_chat"
            ChatScreen(
                navController = navController,
                currentUser = currentUserArg,
                chatWith = chatWithArg
            )
        }

        composable(Routes.VAULT) { // Route cho màn hình Vault
            VaultScreen()
        }

        composable(Routes.SETTINGS) { // Route cho màn hình Settings
            SettingScreen(
                navController = navController, // Truyền navController để điều hướng đến Profile
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logoutUser()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.PROFILE) { // Route cho màn hình Profile
            ProfileScreen(navController = navController)
        }

        composable(Routes.ADD_FRIEND) { // Route cho màn hình thêm bạn bè
            AddFriendScreen(navController = navController)
        }
        // Các route cũ của Schedule và Taskboard sẽ không còn ở đây vì đã tích hợp vào Home
        // composable(Routes.SCHEDULE) { ScheduleScreen() }
        // composable(Routes.TASKBOARD) { TaskboardScreen() }
    }
}