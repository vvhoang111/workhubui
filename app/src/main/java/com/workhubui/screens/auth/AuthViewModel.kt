package com.workhubui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.workhubui.data.local.AppDatabase
import com.workhubui.data.local.entity.UserEntity
import com.workhubui.data.remote.FirebaseRepository
import com.workhubui.data.repository.UserRepository
import com.workhubui.security.TokenManager
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

    private val firebaseAuthInstance: FirebaseAuth = FirebaseAuth.getInstance() // Renamed to avoid conflict
    private val tokenManager: TokenManager = TokenManager(application.applicationContext)
    // For saving user to local and remote DB
    private val userDao = AppDatabase.getInstance(application).userDao()
    private val userRepository = UserRepository(userDao) // For local Room operations
    private val firebaseRepository = FirebaseRepository() // For Firestore operations


    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val firebaseUser = firebaseAuthInstance.currentUser
        val token = tokenManager.getAccessToken()

        if (firebaseUser != null && token != null) {
            _currentUserEmail.value = firebaseUser.email
            _currentUser.value = firebaseUser
        } else {
            if (firebaseUser == null || token == null) {
                tokenManager.clearTokens()
                _currentUserEmail.value = null
                _currentUser.value = null
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return firebaseAuthInstance.currentUser != null && tokenManager.getAccessToken() != null
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            if (email.isBlank() || password.isBlank()) {
                _authResult.value = AuthResult.Error("Email và mật khẩu không được để trống.")
                return@launch
            }
            try {
                val authResultFirebase = firebaseAuthInstance.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResultFirebase.user
                if (firebaseUser != null) {
                    val idToken = firebaseUser.getIdToken(true).await().token
                    if (idToken != null) {
                        tokenManager.saveAccessToken(idToken)
                        _currentUserEmail.value = firebaseUser.email
                        _currentUser.value = firebaseUser
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
            try {
                val authResultFirebase = firebaseAuthInstance.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResultFirebase.user
                if (firebaseUser != null) {
                    val idToken = firebaseUser.getIdToken(true).await().token
                    if (idToken != null) {
                        tokenManager.saveAccessToken(idToken)
                        _currentUserEmail.value = firebaseUser.email
                        _currentUser.value = firebaseUser

                        // Save user to local Room and Firebase Firestore
                        val newUserEntity = UserEntity(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email,
                            displayName = firebaseUser.email?.substringBefore('@'), // Default display name
                            photoUrl = null
                        )
                        userRepository.insertUser(newUserEntity) // Save to Room
                        firebaseRepository.saveUserToFirestore(newUserEntity) // Save to Firestore


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
            firebaseAuthInstance.signOut()
            tokenManager.clearTokens()
            _currentUserEmail.value = null
            _currentUser.value = null
            _authResult.value = AuthResult.Idle
        }
    }

    fun resetAuthResult() {
        _authResult.value = AuthResult.Idle
    }
}
