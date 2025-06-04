
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
import com.workhubui.navigation.AppNavGraph
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