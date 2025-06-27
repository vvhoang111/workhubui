package com.cdcs.screens.setting

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.cdcs.navigation.Routes
import com.cdcs.screens.auth.AuthViewModel
import com.cdcs.screens.auth.AuthViewModelFactory
import com.cdcs.security.BiometricHelper

data class SettingItem(
    val icon: ImageVector,
    val title: String,
    val description: String? = null,
    val action: @Composable () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    navController: NavHostController,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(context.applicationContext as android.app.Application))
    val isBiometricEnabled by authViewModel.isBiometricLoginEnabled.collectAsState()
    val canUseBiometrics = BiometricHelper.isBiometricAvailable(context)

    val settings = listOf(
        SettingItem(
            icon = Icons.Filled.Person,
            title = "Hồ sơ",
            description = "Xem và chỉnh sửa thông tin cá nhân",
            action = {
                TextButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                    Text("Xem")
                }
            }
        ),
        // **THAY ĐỔI: Thêm mục cài đặt sinh trắc học**
        SettingItem(
            icon = Icons.Filled.Fingerprint,
            title = "Đăng nhập sinh trắc học",
            description = if (canUseBiometrics) "Sử dụng vân tay/khuôn mặt để đăng nhập" else "Thiết bị không hỗ trợ hoặc chưa cài đặt",
            action = {
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = {
                        if (authViewModel.isLoggedIn()) {
                            authViewModel.setBiometricLoginEnabled(it)
                        } else {
                            Toast.makeText(context, "Vui lòng đăng nhập để bật tính năng này", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = canUseBiometrics && authViewModel.isLoggedIn()
                )
            }
        ),
        SettingItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = "Đăng xuất",
            action = {
                TextButton(onClick = onLogout, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Đăng xuất")
                }
            }
        )
        // Bạn có thể thêm các mục cài đặt khác ở đây
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            items(settings) { item ->
                SettingRow(item)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun SettingRow(item: SettingItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge)
            item.description?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item.action()
    }
}