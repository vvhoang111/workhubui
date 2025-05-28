package com.workhubui.screens.auth

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.workhubui.navigation.Routes
import com.workhubui.ui.theme.WorkhubuiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var useBiometrics by remember { mutableStateOf(false) } // TODO: Implement Biometric logic

    val authResult by authViewModel.authResult.collectAsState()
    val isLoading = authResult is AuthResult.Loading

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                Toast.makeText(context, result.message ?: "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
                authViewModel.resetAuthResult() // Quan trọng: reset để không trigger lại
            }
            is AuthResult.Error -> {
                Toast.makeText(context, result.errorMessage, Toast.LENGTH_LONG).show()
                authViewModel.resetAuthResult()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp), // Thêm padding dọc
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "WorkHub",
            fontSize = 50.sp,
            // modifier = Modifier.offset(y = (-80).dp), // Có thể không cần offset cứng
            style = MaterialTheme.typography.headlineLarge.copy(
                color = MaterialTheme.colorScheme.primary, // Sử dụng màu từ theme
                fontWeight = FontWeight.Bold,
            )
        )

        Spacer(modifier = Modifier.height(24.dp)) // Tăng khoảng cách

        Text(
            "Đăng nhập", // Tiếng Việt
            fontSize = 32.sp, // Điều chỉnh kích thước
            // modifier = Modifier.offset(y = (-40).dp),
            fontWeight = FontWeight.SemiBold // Điều chỉnh độ đậm
        )
        Spacer(modifier = Modifier.height(24.dp)) // Tăng khoảng cách

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") }, // Tiếng Việt
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (email.isNotBlank() && password.isNotBlank()) {
                        authViewModel.loginUser(email, password)
                    } else {
                        Toast.makeText(context, "Email và mật khẩu không được để trống.", Toast.LENGTH_SHORT).show()
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Căn chỉnh tốt hơn
        ) {
            Text(
                "Sử dụng sinh trắc học", // Tiếng Việt
                fontSize = 18.sp, // Điều chỉnh
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = useBiometrics,
                onCheckedChange = { useBiometrics = it }, // TODO: Kết nối với logic Biometric
                modifier = Modifier.scale(1.1f), // Điều chỉnh kích thước Switch
                enabled = !isLoading
            )
        }

        Spacer(modifier = Modifier.height(32.dp)) // Tăng khoảng cách

        Button(
            onClick = {
                focusManager.clearFocus() // Ẩn bàn phím
                if (email.isNotBlank() && password.isNotBlank()) {
                    authViewModel.loginUser(email, password)
                } else {
                    Toast.makeText(context, "Email và mật khẩu không được để trống.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium // Thêm shape
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp // Làm thanh mảnh hơn
                )
            } else {
                Text("Đăng nhập", fontSize = 18.sp) // Tiếng Việt
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chưa có tài khoản?", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = { if (!isLoading) navController.navigate(Routes.SIGNUP) },
                enabled = !isLoading
            ) {
                Text("Đăng ký ngay", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) // Tiếng Việt
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = {
                Toast.makeText(context, "Chức năng Quên mật khẩu chưa sẵn sàng.", Toast.LENGTH_SHORT).show()
                // TODO: Điều hướng đến màn hình Quên mật khẩu
            },
            enabled = !isLoading
        ) {
            Text("Quên mật khẩu?", fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview(){
    WorkhubuiTheme { // Bọc trong Theme để xem trước đúng
        LoginScreen(navController = rememberNavController())
    }
}