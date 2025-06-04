package com.workhubui

import android.os.Build // Import Build để sử dụng VERSION_CODES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi // Import RequiresApi
import com.workhubui.ui.theme.WorkhubuiTheme
import com.workhubui.screens.MainScreen // << ĐẢM BẢO IMPORT CHÍNH XÁC MainScreen TỪ PACKAGE screens

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O) // Thêm annotation này vì MainScreen và các thành phần con có thể yêu cầu API level O (ví dụ: java.time trong HomeScreen)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkhubuiTheme {
                MainScreen() // Gọi MainScreen duy nhất đã được định nghĩa trong com.workhubui.screens.MainScreen.kt
            }
        }
    }
}
