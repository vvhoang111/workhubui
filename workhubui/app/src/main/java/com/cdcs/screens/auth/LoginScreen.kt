// app/src/main/java/com/cdcs/screens/auth/LoginScreen.kt
package com.cdcs.screens.auth

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cdcs.R
import com.cdcs.navigation.Routes
import com.cdcs.security.BiometricHelper
import com.cdcs.ui.theme.WorkhubuiTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authResult by authViewModel.authResult.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState() // Sử dụng isLoading từ ViewModel

    val credentialManager = remember { CredentialManager.create(context) }

    val serverClientId = context.getString(R.string.default_web_client_id)

    // --- Bổ sung cho Biometric ---
    val activity = context as? AppCompatActivity
    val shouldPromptBiometric by authViewModel.shouldPromptBiometric.collectAsState()

    // Hiển thị biometric prompt khi cần
    LaunchedEffect(shouldPromptBiometric, activity) {
        if (shouldPromptBiometric && activity != null && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                onSuccess = {
                    Toast.makeText(context, "Xác thực thành công!", Toast.LENGTH_SHORT).show()
                    authViewModel.loginWithBiometrics()
                },
                onError = { _, errString ->
                    Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                    authViewModel.biometricPromptFinished()
                },
                onFailure = {
                    Toast.makeText(context, "Xác thực thất bại.", Toast.LENGTH_SHORT).show()
                    authViewModel.biometricPromptFinished()
                }
            )
        }
    }
    // --- Kết thúc phần bổ sung cho Biometric ---

    // Launcher cho kết quả từ Intent đăng nhập Google truyền thống
    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val idToken = account.idToken
            if (idToken != null) {
                Log.d("GoogleSignIn", "5. Đã lấy ID Token từ GoogleSignInLauncher thành công. Đang gửi đến ViewModel.")
                authViewModel.signInWithGoogle(idToken)
            } else {
                Log.e("GoogleSignIn", "6. Google ID Token is null from GoogleSignInLauncher.")
                Toast.makeText(context, "Lỗi: Không thể lấy Google ID Token.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "7. Google Sign-In failed with code: ${e.statusCode}", e)
            Toast.makeText(context, "Đăng nhập Google thất bại: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "8. Lỗi không mong muốn từ GoogleSignInLauncher:", e)
            Toast.makeText(context, "Đã xảy ra lỗi không mong muốn khi đăng nhập Google.", Toast.LENGTH_LONG).show()
        }
    }


    fun launchGoogleSignIn() {
        coroutineScope.launch {
            try {
                Log.d("GoogleSignIn", "1. Bắt đầu quá trình đăng nhập Google (Credential Manager)...")
                val nonce = UUID.randomUUID().toString()

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setNonce(nonce)
                    .build()

                val credentialRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                Log.d("GoogleSignIn", "2. Đã tạo request. Đang gọi CredentialManager...")
                val result = credentialManager.getCredential(request = credentialRequest, context = context)
                Log.d("GoogleSignIn", "3. Đã nhận kết quả từ CredentialManager.")

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken
                    Log.d("GoogleSignIn", "4. Lấy ID Token thành công từ CredentialManager. Đang gửi đến ViewModel.")
                    authViewModel.signInWithGoogle(googleIdToken)
                } else {
                    Log.e("GoogleSignIn", "Loại thông tin đăng nhập không mong muốn từ CredentialManager: ${credential::class.java.name}")
                    Toast.makeText(context, "Loại thông tin đăng nhập không được hỗ trợ.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NoCredentialException) {
                Log.w("GoogleSignIn", "Không tìm thấy thông tin đăng nhập hoặc One Tap không hiển thị. Chuyển sang đăng nhập Google truyền thống.", e)
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(serverClientId)
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)

            } catch (e: GetCredentialException) {
                Log.e("GoogleSignIn", "GetCredentialException bị bắt:", e)
                Toast.makeText(context, "Đăng nhập Google thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "Một lỗi không mong muốn đã xảy ra:", e)
                Toast.makeText(context, "Đã xảy ra lỗi không mong muốn.", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                Toast.makeText(context, result.message ?: "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                authViewModel.resetAuthResult()
            }
            is AuthResult.Error -> {
                Toast.makeText(context, result.errorMessage, Toast.LENGTH_LONG).show()
                authViewModel.resetAuthResult()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "WorkHub",
            style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Đăng nhập", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (email.isNotBlank() && password.isNotBlank()) {
                    authViewModel.loginUser(email, password)
                } else {
                    Toast.makeText(context, "Email và mật khẩu không được để trống.", Toast.LENGTH_SHORT).show()
                }
            }),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Cập nhật Switch Biometric ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val isBiometricEnabled by authViewModel.isBiometricLoginEnabled.collectAsState()
            Text("Đăng nhập bằng sinh trắc học", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = isBiometricEnabled,
                onCheckedChange = { isEnabled ->
                    authViewModel.setBiometricLoginEnabled(isEnabled)
                },
                modifier = Modifier.scale(1.1f),
                enabled = !isLoading && BiometricHelper.isBiometricAvailable(context)
            )
        }
        // --- Kết thúc cập nhật Switch ---

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                if (email.isNotBlank() && password.isNotBlank()) {
                    authViewModel.loginUser(email, password)
                } else {
                    Toast.makeText(context, "Email và mật khẩu không được để trống.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Đăng nhập", fontSize = 18.sp)
            }
        }

        OrDivider()

        GoogleSignInButton(
            isLoading = isLoading,
            onClick = { launchGoogleSignIn() }
        )

        // --- Thêm nút đăng nhập bằng SĐT ---
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { navController.navigate(Routes.PHONE_AUTH) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, Color.LightGray),
            enabled = !isLoading
        ) {
            Text("Đăng nhập bằng số điện thoại", color = MaterialTheme.colorScheme.onSurface)
        }
        // --- Kết thúc thêm nút SĐT ---

        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chưa có tài khoản?")
            TextButton(onClick = { if (!isLoading) navController.navigate(Routes.SIGNUP) }) {
                Text("Đăng ký ngay", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        TextButton(onClick = { /* TODO: Quên mật khẩu */ }) {
            Text("Quên mật khẩu?")
        }
    }
}

@Composable
fun GoogleSignInButton(isLoading: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Color.LightGray),
        enabled = !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = "Google Logo", modifier = Modifier.size(24.dp))
            Text(text = "Đăng nhập với Google", modifier = Modifier.padding(start = 12.dp), color = Color.Black.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(text = "HOẶC", modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview(){
    WorkhubuiTheme {
        LoginScreen(navController = rememberNavController())
    }
}