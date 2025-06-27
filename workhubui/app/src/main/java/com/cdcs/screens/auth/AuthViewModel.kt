package com.cdcs.screens.auth

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Base64
import android.util.Log
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

    // --- Biometric States ---
    private val _isBiometricLoginEnabled = MutableStateFlow(sharedPrefs.getBoolean("biometric_enabled", false))
    val isBiometricLoginEnabled: StateFlow<Boolean> = _isBiometricLoginEnabled.asStateFlow()

    private val _shouldPromptBiometric = MutableStateFlow(false)
    val shouldPromptBiometric: StateFlow<Boolean> = _shouldPromptBiometric.asStateFlow()

    // --- Phone Auth States ---
    private val _navigateToOtpVerify = MutableSharedFlow<String>()
    val navigateToOtpVerify = _navigateToOtpVerify.asSharedFlow()

    init {
        _currentUser.value = firebaseAuthInstance.currentUser
        if (isBiometricLoginEnabled.value && isLoggedIn()) {
            _shouldPromptBiometric.value = true
        }
    }

    fun isLoggedIn(): Boolean {
        return _currentUser.value != null && tokenManager.getAccessToken() != null
    }

    private suspend fun handleSuccessfulLogin(firebaseUser: FirebaseUser) {
        _currentUser.value = firebaseUser
        tokenManager.saveAccessToken(firebaseUser.getIdToken(true).await().token!!)
        updateFcmToken(firebaseUser.uid)
        syncUserKeysAndData(firebaseUser)
        _authResult.value = AuthResult.Success("Đăng nhập thành công!")
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            if (email.isBlank() || password.isBlank()) {
                _authResult.value = AuthResult.Error("Email và mật khẩu không được để trống."); _isLoading.value = false; return@launch
            }
            try {
                val authResultFirebase = firebaseAuthInstance.signInWithEmailAndPassword(email, password).await()
                handleSuccessfulLogin(authResultFirebase.user!!)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi đăng nhập: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
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
                val firebaseUser = authResultFirebase.user!!
                _currentUser.value = firebaseUser

                tokenManager.saveAccessToken(firebaseUser.getIdToken(true).await().token!!)

                val keyPair = cryptoManager.getOrCreateUserKeyPair(firebaseUser.uid)
                val publicKeyString = Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT)

                val newUserEntity = UserEntity(
                    uid = firebaseUser.uid, email = firebaseUser.email,
                    displayName = firebaseUser.email?.substringBefore('@'), photoUrl = null,
                    publicKey = publicKeyString, friends = emptyList()
                )
                userRepository.insertUser(newUserEntity)
                firebaseRepository.saveUserToFirestore(newUserEntity)
                updateFcmToken(firebaseUser.uid)
                _authResult.value = AuthResult.Success("Đăng ký thành công!")
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
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            try {
                val authResultFirebase = firebaseAuthInstance.signInWithCredential(credential).await()
                handleSuccessfulLogin(authResultFirebase.user!!)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi đăng nhập Google: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                userRepository.clearUsers()
                chatRepository.clearChatMessages()
            }
            firebaseAuthInstance.signOut()
            tokenManager.clearTokens()
            setBiometricLoginEnabled(false) // Tắt sinh trắc học khi đăng xuất
            _currentUser.value = null
            _authResult.value = AuthResult.Idle
        }
    }

    private fun updateFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("AuthViewModel", "Fetching FCM token failed", task.exception); return@addOnCompleteListener
            }
            viewModelScope.launch { firebaseRepository.updateUserFcmToken(uid, task.result) }
        }
    }

    fun resetAuthResult() {
        _authResult.value = AuthResult.Idle
    }

    private suspend fun syncUserKeysAndData(firebaseUser: FirebaseUser) {
        val uid = firebaseUser.uid
        withContext(Dispatchers.IO) {
            val localKeyPair = cryptoManager.getOrCreateUserKeyPair(uid)
            val localPublicKeyString = Base64.encodeToString(localKeyPair.public.encoded, Base64.DEFAULT)

            var userProfile = firebaseRepository.getUserProfile(uid)

            if (userProfile == null || userProfile.publicKey != localPublicKeyString) {
                Log.d("AuthViewModel", "Creating/updating user profile in Firestore.")
                val newUserEntity = UserEntity(
                    uid = uid,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@'),
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    publicKey = localPublicKeyString,
                    friends = userProfile?.friends ?: emptyList()
                )
                firebaseRepository.saveUserToFirestore(newUserEntity)
                userProfile = newUserEntity
            }

            userRepository.insertUser(userProfile)
            Log.d("AuthViewModel", "User data sync complete.")
        }
    }

    // --- Biometric Functions ---
    fun setBiometricLoginEnabled(enabled: Boolean) {
        if (enabled && !isLoggedIn()) {
            _authResult.value = AuthResult.Error("Vui lòng đăng nhập trước khi bật tính năng này.")
            return
        }
        sharedPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricLoginEnabled.value = enabled
    }

    fun loginWithBiometrics() {
        viewModelScope.launch {
            _isLoading.value = true
            val token = tokenManager.getAccessToken()
            val user = firebaseAuthInstance.currentUser
            if (token != null && user != null) {
                handleSuccessfulLogin(user)
            } else {
                _authResult.value = AuthResult.Error("Không tìm thấy thông tin đăng nhập. Vui lòng đăng nhập lại.")
            }
            _isLoading.value = false
            biometricPromptFinished()
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