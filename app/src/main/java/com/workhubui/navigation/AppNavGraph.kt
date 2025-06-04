package com.workhubui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // << ĐẢM BẢO IMPORT MODIFIER
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.workhubui.screens.SplashScreen
import com.workhubui.screens.auth.AuthViewModel // Import AuthViewModel
import com.workhubui.screens.auth.LoginScreen
import com.workhubui.screens.auth.SignupScreen
import com.workhubui.screens.chat.AddFriendScreen
import com.workhubui.screens.chat.ChatListScreen
import com.workhubui.screens.chat.ChatScreen
import com.workhubui.screens.home.HomeScreen // << IMPORT HOMESCREEN TỪ ĐÚNG PACKAGE
import com.workhubui.screens.setting.ProfileScreen
import com.workhubui.screens.setting.SettingScreen
import com.workhubui.screens.vault.VaultScreen

@RequiresApi(Build.VERSION_CODES.O) // Cần thiết nếu các màn hình con (ví dụ: HomeScreen) yêu cầu API level này
@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel, // authViewModel được truyền vào để sử dụng nếu cần (ví dụ: trong SettingScreen cho logout)
    modifier: Modifier = Modifier // << THÊM THAM SỐ MODIFIER VỚI GIÁ TRỊ MẶC ĐỊNH
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier // << ÁP DỤNG MODIFIER Ở ĐÂY (SẼ NHẬN innerPadding TỪ MainScreen)
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
            HomeScreen(navController = navController) // Gọi HomeScreen tại đây
        }
        composable(Routes.CHAT_LIST) {
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
        composable(Routes.VAULT) {
            VaultScreen() // Đảm bảo VaultScreen đã được định nghĩa và import đúng
        }
        composable(Routes.SETTINGS) {
            SettingScreen(
                navController = navController, // Truyền navController để điều hướng từ Settings (ví dụ: đến Profile)
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
            ProfileScreen(navController = navController) // Đảm bảo ProfileScreen đã được định nghĩa và import đúng
        }
        composable(Routes.ADD_FRIEND) {
            AddFriendScreen(navController = navController) // Đảm bảo AddFriendScreen đã được định nghĩa và import đúng
        }

        // Composable placeholder cho các màn hình mới
        composable(Routes.ADD_TASK) {
            // Đây là màn hình placeholder, bạn sẽ thay thế bằng màn hình thêm Task thực tế
            // Ví dụ: AddTaskScreen(navController)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Màn hình Thêm Task (Placeholder)")
            }
        }
        composable(Routes.ADD_SCHEDULE_ITEM) {
            // Đây là màn hình placeholder, bạn sẽ thay thế bằng màn hình thêm Lịch/Meeting thực tế
            // Ví dụ: AddScheduleItemScreen(navController)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Màn hình Thêm Lịch/Meeting (Placeholder)")
            }
        }
        // Thêm các composable cho các route khác nếu có
    }
}
