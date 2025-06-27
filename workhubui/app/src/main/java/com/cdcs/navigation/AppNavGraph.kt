package com.cdcs.navigation

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cdcs.screens.SplashScreen
import com.cdcs.screens.auth.AuthViewModel
import com.cdcs.screens.auth.AuthViewModelFactory
import com.cdcs.screens.auth.LoginScreen
import com.cdcs.screens.auth.OtpVerifyScreen
import com.cdcs.screens.auth.PhoneAuthScreen
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
    val application = LocalContext.current.applicationContext as Application
    val mainAuthViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))

    // Lắng nghe sự kiện điều hướng OTP từ ViewModel
    LaunchedEffect(Unit) {
        mainAuthViewModel.navigateToOtpVerify.collect { verificationId ->
            navController.navigate("${Routes.OTP_VERIFY}/$verificationId")
        }
    }

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
        composable(
            route = Routes.CHAT + "/{currentUserUid}/{friendUid}",
            arguments = listOf(
                navArgument("currentUserUid") { type = NavType.StringType },
                navArgument("friendUid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val currentUserUid = backStackEntry.arguments?.getString("currentUserUid") ?: "unknown_user"
            val friendUid = backStackEntry.arguments?.getString("friendUid") ?: "unknown_chat"
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
                    mainAuthViewModel.logoutUser()
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

        // --- Các route mới cho OTP ---
        composable(Routes.PHONE_AUTH) {
            PhoneAuthScreen(navController, mainAuthViewModel)
        }
        composable(
            route = "${Routes.OTP_VERIFY}/{verificationId}",
            arguments = listOf(navArgument("verificationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            OtpVerifyScreen(navController, mainAuthViewModel, verificationId)
        }
        // --- Kết thúc các route mới ---

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