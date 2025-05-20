package com.workhubui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.workhubui.ui.theme.WorkhubuiTheme
import com.workhubui.screens.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkhubuiTheme {
                MainScreen()
            }
        }
    }
}
