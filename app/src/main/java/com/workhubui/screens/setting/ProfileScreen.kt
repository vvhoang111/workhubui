package com.workhubui.screens.setting

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.workhubui.screens.auth.AuthViewModel
import com.workhubui.screens.auth.AuthViewModelFactory
// Import UserEntity if you plan to display more details from a local cache
// import com.workhubui.data.local.entity.UserEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))

    // Observe the FirebaseUser object
    val firebaseUser by authViewModel.currentUser.collectAsState()
    // You can also observe currentUserEmail if only email is needed
    // val currentUserEmail by authViewModel.currentUserEmail.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ của tôi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (firebaseUser != null) {
                Text(
                    text = "Thông tin người dùng",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Display user information
                ProfileInfoRow(label = "Email:", value = firebaseUser?.email ?: "Không có")
                ProfileInfoRow(label = "Tên hiển thị:", value = firebaseUser?.displayName ?: firebaseUser?.email?.substringBefore('@') ?: "Chưa đặt")
                ProfileInfoRow(label = "User ID:", value = firebaseUser?.uid ?: "Không có")
                // Add more fields as needed, e.g., photo URL with an Image composable

                Spacer(modifier = Modifier.weight(1f))

                Button(onClick = { /* TODO: Implement edit profile functionality */ }) {
                    Text("Chỉnh sửa hồ sơ")
                }
            } else {
                Text("Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.")
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(120.dp) // Adjust width as needed
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
