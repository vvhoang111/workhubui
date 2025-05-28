package com.workhubui.screens

import android.app.Application
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource // Cho logo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.workhubui.R // Import R để lấy logo, bạn cần thêm logo vào drawable
import com.workhubui.navigation.Routes
import com.workhubui.screens.auth.AuthViewModel
import com.workhubui.screens.auth.AuthViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))
    val scale = remember { Animatable(0f) } // Cho animation

    LaunchedEffect(key1 = true) { // Sử dụng key1 = true để chỉ chạy 1 lần
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = { OvershootInterpolator(2f).getInterpolation(it) }
            )
        )
        delay(1500L) // Thời gian hiển thị Splash Screen
        if (authViewModel.isLoggedIn()) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        } else {
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Thêm logo của bạn vào thư mục res/drawable (ví dụ: logo_workhub.png)
        // và thay thế R.drawable.ic_launcher_foreground bằng ID của logo đó.
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground), // THAY THẾ BẰNG LOGO CỦA BẠN
            contentDescription = "WorkHub Logo",
            modifier = Modifier
                .size(120.dp)
                .scale(scale.value) // Áp dụng animation scale
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "WorkHub",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp // Điều chỉnh kích thước
            ),
            modifier = Modifier.scale(scale.value)
        )
        Text(
            "Your Productivity Partner", // Slogan
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.scale(scale.value)
        )
    }
}