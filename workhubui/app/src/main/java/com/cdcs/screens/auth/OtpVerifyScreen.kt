package com.cdcs.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination // **GIẢI PHÁP 1: THÊM IMPORT NÀY**
import androidx.navigation.NavHostController
import com.cdcs.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerifyScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    verificationId: String
) {
    var otpCode by remember { mutableStateOf("") }
    val isLoading by authViewModel.isLoading.collectAsState()
    val authResult by authViewModel.authResult.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                Toast.makeText(context, result.message ?: "Xác thực thành công!", Toast.LENGTH_SHORT).show()

                navController.navigate(Routes.HOME) {
                    // **GIẢI PHÁP 2: SỬ DỤNG CÚ PHÁP MỚI CHO popUpTo**
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
                authViewModel.resetAuthResult()
            }
            is AuthResult.Error -> {
                Toast.makeText(context, result.errorMessage, Toast.LENGTH_LONG).show()
                authViewModel.resetAuthResult()
            }
            else -> Unit
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhập mã OTP") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Xác thực mã OTP",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Vui lòng nhập mã gồm 6 chữ số đã được gửi tới số điện thoại của bạn.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                label = { Text("Mã OTP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (verificationId.isNotBlank()) {
                        authViewModel.verifyOtpAndSignIn(verificationId, otpCode)
                    } else {
                        Toast.makeText(context, "Lỗi: Không có ID xác thực.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading && otpCode.length == 6
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Xác nhận", fontSize = 18.sp)
                }
            }
        }
    }
}