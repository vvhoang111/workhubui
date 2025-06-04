

package com.workhubui.screens.setting

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.workhubui.navigation.Routes
import com.workhubui.screens.auth.AuthViewModel
import com.workhubui.screens.auth.AuthViewModelFactory

data class SettingItem(
    val icon: ImageVector,
    val title: String,
    val description: String? = null,
    val isToggle: Boolean = false,
    val toggled: Boolean = false,
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    navController: NavHostController, // Thêm navController
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkThemeEnabled by remember { mutableStateOf(false) }

    val settings = listOf(
        SettingItem(
            icon = Icons.Filled.Person,
            title = "Hồ sơ", // Tiếng Việt
            description = "Xem và chỉnh sửa thông tin cá nhân", // Tiếng Việt
            onClick = { navController.navigate(Routes.PROFILE) } // Điều hướng đến ProfileScreen
        ),
        SettingItem(
            icon = Icons.Filled.Notifications,
            title = "Thông báo", // Tiếng Việt
            isToggle = true,
            toggled = notificationsEnabled,
            onClick = { notificationsEnabled = !notificationsEnabled }
        ),
        SettingItem(
            icon = Icons.Filled.Palette,
            title = "Chủ đề tối", // Tiếng Việt
            isToggle = true,
            toggled = darkThemeEnabled,
            onClick = { darkThemeEnabled = !darkThemeEnabled }
        ),
        SettingItem(
            icon = Icons.Filled.Language,
            title = "Ngôn ngữ", // Tiếng Việt
            description = "Tiếng Việt", // Tiếng Việt
            onClick = { /* TODO: open language selector */ }
        ),
        SettingItem(
            icon = Icons.Filled.Info,
            title = "Về chúng tôi", // Tiếng Việt
            onClick = { /* TODO: show about dialog */ }
        ),
        SettingItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = "Đăng xuất", // Tiếng Việt
            onClick = onLogout
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") }, // Tiếng Việt
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại" // Tiếng Việt
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
            .clickable(enabled = !item.isToggle) { item.onClick() }
            .padding(16.dp)
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
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (item.isToggle) {
            Switch(
                checked = item.toggled,
                onCheckedChange = { item.onClick() }
            )
        }
    }
}