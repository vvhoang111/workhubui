package com.cdcs.screens.auth

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdcs.data.local.AppDatabase
import com.cdcs.data.local.entity.UserEntity
import com.cdcs.data.remote.FirebaseRepository
import com.cdcs.data.repository.ChatRepository
import com.cdcs.data.repository.UserRepository
import com.cdcs.security.CryptoManager
import com.cdcs.security.TokenManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.util.Log // **GIẢI PHÁP: Thêm dòng import này**
import androidx.core.content.edit //

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuthInstance: FirebaseAuth = FirebaseAuth.getInstance()
    private val tokenManager: TokenManager = TokenManager(application.applicationContext)
    private val sharedPrefs = application.getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)

    private val userDao = AppDatabase.getInstance(application).userDao()
    private val chatMessageDao = AppDatabase.getInstance(application).chatMessageDao()
    private val userRepository = UserRepository(userDao)
    private val firebaseRepository = FirebaseRepository()
    private val chatRepository = ChatRepository(chatMessageDao, firebaseRepository)
    private val cryptoManager = CryptoManager()

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isBiometricLoginEnabled = MutableStateFlow(sharedPrefs.getBoolean("biometric_enabled", false))
    val isBiometricLoginEnabled: StateFlow<Boolean> = _isBiometricLoginEnabled.asStateFlow()

    private val _shouldPromptBiometric = MutableStateFlow(false)
    val shouldPromptBiometric: StateFlow<Boolean> = _shouldPromptBiometric.asStateFlow()

    private val _navigateToOtpVerify = MutableSharedFlow<String>()
    val navigateToOtpVerify = _navigateToOtpVerify.asSharedFlow()

    //**GIẢI PHÁP: Khai báo `authStateListener` ở đây, bên ngoài hàm init()**
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        _currentUser.value = auth.currentUser
        if (auth.currentUser == null) {
            tokenManager.clearTokens()
            // Không tự động tắt, để người dùng tự quyết định trong lần đăng nhập sau
            // setBiometricLoginEnabled(false)
        }
    }

    init {
        _currentUser.value = firebaseAuthInstance.currentUser
        // Nếu người dùng đã đăng nhập và đã bật sinh trắc học, gợi ý họ đăng nhập bằng sinh trắc học
        if (!isLoggedIn() && isBiometricLoginEnabled.value) {
            _shouldPromptBiometric.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuthInstance.removeAuthStateListener(authStateListener)
    }

    fun isLoggedIn(): Boolean = firebaseAuthInstance.currentUser != null

    private suspend fun handleSuccessfulLogin(firebaseUser: FirebaseUser, showToast: Boolean = true) {
        _currentUser.value = firebaseUser
        tokenManager.saveAccessToken(firebaseUser.getIdToken(true).await().token!!)
        syncUserKeysAndData(firebaseUser)
        updateFcmToken(firebaseUser.uid)
        if (showToast) {
            _authResult.value = AuthResult.Success("Đăng nhập thành công!")
        }
    }

    fun loginUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authResult.value = AuthResult.Error("Email và mật khẩu không được để trống.")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val authResultFirebase = firebaseAuthInstance.signInWithEmailAndPassword(email, password).await()
                handleSuccessfulLogin(authResultFirebase.user!!)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi đăng nhập: ${e.localizedMessage}")
            } finally { _isLoading.value = false }
        }
    }

    fun signupUser(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            if (email.isBlank() || password.isBlank()) {
                _authResult.value = AuthResult.Error("Email và mật khẩu không được để trống."); _isLoading.value = false; return@launch
            }
            try {
                val authResultFirebase = firebaseAuthInstance.createUserWithEmailAndPassword(email, password).await()
                // Sau khi tạo user trên Auth, gọi luồng xử lý chung như khi đăng nhập
                handleSuccessfulLogin(authResultFirebase.user!!)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi đăng ký: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResultFirebase = firebaseAuthInstance.signInWithCredential(credential).await()
                handleSuccessfulLogin(authResultFirebase.user!!)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi đăng nhập Google: ${e.localizedMessage}")
            } finally { _isLoading.value = false }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            firebaseAuthInstance.signOut()
            tokenManager.clearTokens()
            // Không tắt sinh trắc học của người dùng khi họ chỉ đăng xuất
            // setBiometricLoginEnabled(false)
            _currentUser.value = null
            withContext(Dispatchers.IO) {
                userRepository.clearUsers()
                chatRepository.clearChatMessages()
            }
            _authResult.value = AuthResult.Idle
        }
    }

    private fun updateFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("AuthViewModel", "Fetching FCM token failed", task.exception); return@addOnCompleteListener
            }
            val token = task.result
            viewModelScope.launch {
                try {
                    val userProfile = firebaseRepository.getUserProfile(uid)
                    if (userProfile != null && !userProfile.fcmTokens.contains(token)) {
                        val updatedTokens = userProfile.fcmTokens + token
                        val updatedUser = userProfile.copy(fcmTokens = updatedTokens)
                        firebaseRepository.saveUserToFirestore(updatedUser)
                        Log.d("AuthViewModel", "FCM token updated for user $uid")
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed to update FCM token in Firestore", e)
                }
            }
        }
    }

    private suspend fun syncUserKeysAndData(firebaseUser: FirebaseUser) {
        val uid = firebaseUser.uid
        withContext(Dispatchers.IO) {
            // Lấy thông tin người dùng từ Firestore để kiểm tra
            var userProfile = firebaseRepository.getUserProfile(uid)

            if (userProfile == null) {
                // Nếu người dùng chưa tồn tại trên Firestore -> đây là lần đăng nhập đầu tiên
                Log.d("AuthViewModel", "User not found in Firestore. Creating new document for $uid.")
                val localKeyPair = cryptoManager.getOrCreateUserKeyPair(uid)
                val localPublicKeyString = Base64.encodeToString(localKeyPair.public.encoded, Base64.DEFAULT)

                userProfile = UserEntity(
                    uid = uid,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@') ?: firebaseUser.phoneNumber,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    publicKey = localPublicKeyString,
                    friends = emptyList(),
                    fcmTokens = emptyList() // Khởi tạo rỗng
                )
            }
            // Dù là người dùng mới hay cũ, cũng lưu lại thông tin (để cập nhật displayName, photoUrl, etc.)
            firebaseRepository.saveUserToFirestore(userProfile)
            userRepository.insertUser(userProfile)
            Log.d("AuthViewModel", "User data sync complete for $uid.")
        }
    }

    fun resetAuthResult() { _authResult.value = AuthResult.Idle }

    // --- Biometric Functions ---
    fun setBiometricLoginEnabled(enabled: Boolean) {
        if (enabled && !isLoggedIn()) {
            // Không cho phép bật nếu chưa đăng nhập
            _authResult.value = AuthResult.Error("Vui lòng đăng nhập trước khi bật tính năng này.")
            return
        }
        sharedPrefs.edit {
            putBoolean("biometric_enabled", enabled)
        }
        _isBiometricLoginEnabled.value = enabled
    }

    fun loginWithBiometrics() {
        viewModelScope.launch {
            _isLoading.value = true
            // **THAY ĐỔI: Logic kiểm tra mạnh mẽ hơn**
            // Lấy token đã lưu. Nếu không có token, không thể đăng nhập sinh trắc học.
            val token = tokenManager.getAccessToken()
            if (token != null) {
                // Giả định token vẫn còn hạn. Có thể thêm logic refresh token ở đây nếu cần.
                val user = firebaseAuthInstance.currentUser
                if (user != null) {
                    // Không cần hiển thị Toast "Đăng nhập thành công" vì đã có Toast "Xác thực thành công"
                    handleSuccessfulLogin(user, showToast = false)
                    // Cập nhật lại AuthResult để điều hướng
                    _authResult.value = AuthResult.Success(null)
                } else {
                    // Trường hợp hiếm gặp: có token nhưng không có user object
                    _authResult.value = AuthResult.Error("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.")
                }
            } else {
                // Đây là lỗi người dùng đang gặp phải
                _authResult.value = AuthResult.Error("Không tìm thấy thông tin đăng nhập. Vui lòng đăng nhập lại bằng mật khẩu.")
                setBiometricLoginEnabled(false)
            }
            _isLoading.value = false
        }
    }

    fun biometricPromptFinished() {
        _shouldPromptBiometric.value = false
    }
    // --- Phone Auth Functions ---
    private val phoneAuthCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val authResultFirebase = firebaseAuthInstance.signInWithCredential(credential).await()
                    handleSuccessfulLogin(authResultFirebase.user!!)
                } catch (e: Exception) {
                    _authResult.value = AuthResult.Error("Lỗi tự động xác thực: ${e.localizedMessage}")
                } finally {
                    _isLoading.value = false
                }
            }
        }

        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
            _isLoading.value = false
            _authResult.value = AuthResult.Error("Lỗi xác thực SĐT: ${e.message}")
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            _isLoading.value = false
            viewModelScope.launch {
                _navigateToOtpVerify.emit(verificationId)
            }
        }
    }

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _isLoading.value = true
        val options = PhoneAuthOptions.newBuilder(firebaseAuthInstance)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(phoneAuthCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtpAndSignIn(verificationId: String, code: String) {
        _isLoading.value = true
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        viewModelScope.launch {
            try {
                val authResultFirebase = firebaseAuthInstance.signInWithCredential(credential).await()
                // Nếu là người dùng mới với SĐT, tạo hồ sơ
                if(authResultFirebase.additionalUserInfo?.isNewUser == true){
                    val firebaseUser = authResultFirebase.user!!
                    val keyPair = cryptoManager.getOrCreateUserKeyPair(firebaseUser.uid)
                    val publicKeyString = Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT)
                    val newUserEntity = UserEntity(
                        uid = firebaseUser.uid,
                        displayName = firebaseUser.phoneNumber, // Dùng SĐT làm tên hiển thị tạm
                        publicKey = publicKeyString,
                    )
                    firebaseRepository.saveUserToFirestore(newUserEntity)
                }
                handleSuccessfulLogin(authResultFirebase.user!!)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi xác thực mã OTP: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}