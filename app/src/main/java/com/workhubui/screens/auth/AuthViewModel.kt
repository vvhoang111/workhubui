package com.workhubui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.workhubui.security.TokenManager // Đảm bảo import đúng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    data class Success(val message: String? = null) : AuthResult()
    data class Error(val errorMessage: String) : AuthResult()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val tokenManager: TokenManager = TokenManager(application.applicationContext)

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val firebaseUser = firebaseAuth.currentUser
        val token = tokenManager.getAccessToken() // Lấy token một lần

        if (firebaseUser != null && token != null) {
            _currentUserEmail.value = firebaseUser.email
            // TODO: Cân nhắc kiểm tra tính hợp lệ của token với server hoặc logic refresh token ở đây
        } else {
            if (firebaseUser == null || token == null) { // Chỉ clear nếu một trong hai không tồn tại
                tokenManager.clearTokens()
                _currentUserEmail.value = null // Đảm bảo email cũng được clear
            }
        }
    }


    fun isLoggedIn(): Boolean {
        // Kiểm tra lại checkCurrentUser để đảm bảo trạng thái là mới nhất
        // Hoặc dựa vào _currentUserEmail và token trực tiếp
        return firebaseAuth.currentUser != null && tokenManager.getAccessToken() != null
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            if (email.isBlank() || password.isBlank()) {
                _authResult.value = AuthResult.Error("Email và mật khẩu không được để trống.")
                return@launch
            }
            try {
                val authResultFirebase = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResultFirebase.user
                if (firebaseUser != null) {
                    val idToken = firebaseUser.getIdToken(true).await().token
                    if (idToken != null) {
                        tokenManager.saveAccessToken(idToken)
                        _currentUserEmail.value = firebaseUser.email
                        _authResult.value = AuthResult.Success("Đăng nhập thành công!")
                    } else {
                        _authResult.value = AuthResult.Error("Không thể lấy token xác thực.")
                    }
                } else {
                    _authResult.value = AuthResult.Error("Đăng nhập thất bại. Vui lòng thử lại.")
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authResult.value = AuthResult.Error("Email hoặc mật khẩu không đúng.")
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi đăng nhập: ${e.localizedMessage ?: "Vui lòng thử lại."}")
            }
        }
    }

    fun signupUser(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            if (email.isBlank() || password.isBlank()) {
                _authResult.value = AuthResult.Error("Email và mật khẩu không được để trống.")
                return@launch
            }
            // TODO: Thêm kiểm tra định dạng email hợp lệ
            try {
                val authResultFirebase = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResultFirebase.user
                if (firebaseUser != null) {
                    // TODO: Gửi email xác thực nếu cần
                    // firebaseUser.sendEmailVerification().await()

                    val idToken = firebaseUser.getIdToken(true).await().token
                    if (idToken != null) {
                        tokenManager.saveAccessToken(idToken)
                        _currentUserEmail.value = firebaseUser.email
                        _authResult.value = AuthResult.Success("Đăng ký thành công! Đang đăng nhập...")
                    } else {
                        _authResult.value = AuthResult.Error("Đăng ký thành công nhưng không thể lấy token.")
                    }
                } else {
                    _authResult.value = AuthResult.Error("Đăng ký thất bại. Vui lòng thử lại.")
                }
            } catch (e: FirebaseAuthWeakPasswordException) {
                _authResult.value = AuthResult.Error("Mật khẩu quá yếu. Vui lòng chọn mật khẩu mạnh hơn (ít nhất 6 ký tự).")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authResult.value = AuthResult.Error("Địa chỉ email không hợp lệ.")
            } catch (e: FirebaseAuthUserCollisionException) {
                _authResult.value = AuthResult.Error("Địa chỉ email này đã được sử dụng.")
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi đăng ký: ${e.localizedMessage ?: "Vui lòng thử lại."}")
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            firebaseAuth.signOut()
            tokenManager.clearTokens()
            _currentUserEmail.value = null
            _authResult.value = AuthResult.Idle
        }
    }

    fun resetAuthResult() {
        _authResult.value = AuthResult.Idle
    }
}