package com.workhubui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier // Added import for Modifier
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
import com.workhubui.screens.setting.ProfileScreen
import com.workhubui.screens.vault.VaultScreen
import com.workhubui.screens.chat.AddFriendScreen
import com.workhubui.screens.auth.AuthViewModel // Import AuthViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel, // Added authViewModel parameter
    modifier: Modifier = Modifier // Added modifier parameter
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier // Applied modifier to NavHost
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

        composable(Routes.CHAT_LIST) {
            ChatListScreen(navController = navController)
        }

        composable(
            route = Routes.CHAT + "/{currentUser}/{chatWith}",
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
    }
}
