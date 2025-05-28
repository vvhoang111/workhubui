package com.workhubui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.workhubui.screens.chat.ChatScreen
import com.workhubui.screens.home.HomeScreen


@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(navController)
        }
        composable(
            route = Routes.CHAT + "/{currentUser}/{chatWith}",
            arguments = listOf(
                navArgument("currentUser") { type = NavType.StringType },
                navArgument("chatWith") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val current = backStackEntry.arguments?.getString("currentUser")!!
            val chatWith = backStackEntry.arguments?.getString("chatWith")!!
            ChatScreen(navController, current, chatWith)
        }
    }
}
