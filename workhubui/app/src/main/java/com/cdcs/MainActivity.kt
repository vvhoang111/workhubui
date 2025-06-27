package com.cdcs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.cdcs.screens.MainScreen
import com.cdcs.ui.theme.WorkhubuiTheme

class MainActivity : ComponentActivity() {

    // Trình khởi chạy (launcher) để xử lý kết quả của việc xin quyền
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Quyền đã được cấp. Không cần làm gì thêm ở đây.
        } else {
            // Người dùng từ chối cấp quyền.
            // Bạn có thể hiển thị một thông báo giải thích rằng họ sẽ không nhận được thông báo chat.
        }
    }

    // Hàm để kiểm tra và yêu cầu quyền gửi thông báo
    private fun askNotificationPermission() {
        // Chỉ áp dụng cho Android 13 (API 33) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Nếu chưa có quyền, hiển thị hộp thoại xin quyền
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O) // Yêu cầu cho các màn hình con
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Gọi hàm xin quyền ngay khi ứng dụng khởi động
        askNotificationPermission()

        setContent {
            WorkhubuiTheme {
                val navController = rememberNavController()

                // Truyền NavController đã tạo vào MainScreen
                MainScreen(navController = navController)

                // Kiểm tra một lần duy nhất khi Composable được khởi tạo
                // xem ứng dụng có được mở từ một Intent chứa dữ liệu "route" hay không.
                LaunchedEffect(Unit) {
                    intent.getStringExtra("route")?.let { route ->
                        // Nếu có, điều hướng đến route đó
                        navController.navigate(route)
                        // Xóa dữ liệu route khỏi intent để tránh việc điều hướng lặp lại
                        // nếu Activity được tạo lại (ví dụ: khi xoay màn hình).
                        intent.removeExtra("route")
                    }
                }
            }
        }
    }
}
