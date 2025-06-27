package com.cdcs.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cdcs.screens.SplashScreen
import com.cdcs.screens.auth.AuthViewModel
import com.cdcs.screens.auth.LoginScreen
import com.cdcs.screens.auth.SignupScreen
import com.cdcs.screens.chat.AddFriendScreen
import com.cdcs.screens.chat.ChatListScreen
import com.cdcs.screens.chat.ChatScreen
import com.cdcs.screens.home.HomeScreen
import com.cdcs.screens.setting.ProfileScreen
import com.cdcs.screens.setting.SettingScreen
import com.cdcs.screens.vault.VaultScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
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
            HomeScreen(navController = navController)
        }
        composable(Routes.CHAT_LIST) {
            ChatListScreen(navController = navController)
        }

        // << THAY ĐỔI: Cập nhật route và tham số cho màn hình Chat >>
        composable(
            route = Routes.CHAT + "/{currentUserUid}/{friendUid}", // Định nghĩa route với tham số
            arguments = listOf(
                navArgument("currentUserUid") { type = NavType.StringType },
                navArgument("friendUid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Lấy các tham số UID từ backStackEntry
            val currentUserUid = backStackEntry.arguments?.getString("currentUserUid") ?: "unknown_user"
            val friendUid = backStackEntry.arguments?.getString("friendUid") ?: "unknown_chat"

            // Truyền UID vào ChatScreen
            ChatScreen(
                navController = navController,
                currentUserUid = currentUserUid,
                friendUid = friendUid
            )
        }

        composable(Routes.VAULT) {
            VaultScreen()
        }
        composable(Routes.SETTINGS) {
            SettingScreen(
                navController = navController,
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
        composable(Routes.PROFILE) {
            ProfileScreen(navController = navController)
        }
        composable(Routes.ADD_FRIEND) {
            AddFriendScreen(navController = navController)
        }

        // Placeholder cho các màn hình khác
        composable(Routes.ADD_TASK) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Màn hình Thêm Task (Placeholder)")
            }
        }
        composable(Routes.ADD_SCHEDULE_ITEM) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Màn hình Thêm Lịch/Meeting (Placeholder)")
            }
        }
    }
}
