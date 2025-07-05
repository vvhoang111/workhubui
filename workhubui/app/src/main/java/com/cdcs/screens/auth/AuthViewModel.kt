package com.cdcs.screens.auth

import android.app.Activity
import android.app.Application
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

    private val userDao = AppDatabase.getInstance(application).userDao()
    private val chatMessageDao = AppDatabase.getInstance(application).chatMessageDao()
    private val userRepository = UserRepository(userDao)
    private val firebaseRepository = FirebaseRepository()
    private val chatRepository = ChatRepository(chatMessageDao, firebaseRepository)
    private val cryptoManager = CryptoManager()

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult.asStateFlow()

    private val _currentUser = MutableStateFlow(firebaseAuthInstance.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _navigateToOtpVerify = MutableSharedFlow<String>()
    val navigateToOtpVerify = _navigateToOtpVerify.asSharedFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        _currentUser.value = auth.currentUser
    }

    init {
        firebaseAuthInstance.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuthInstance.removeAuthStateListener(authStateListener)
    }

    fun isLoggedIn(): Boolean = firebaseAuthInstance.currentUser != null

    private suspend fun handleSuccessfulLogin(firebaseUser: FirebaseUser, showToast: Boolean = true) {
        tokenManager.saveAccessToken(firebaseUser.getIdToken(true).await().token!!)
        syncUserKeysAndData(firebaseUser)
        updateFcmToken(firebaseUser.uid)
        if (showToast) {
            _authResult.value = AuthResult.Success("Đăng nhập thành công!")
        }
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
                _authResult.value = AuthResult.Error("Email và mật khẩu không được để trống.")
                _isLoading.value = false
                return@launch
            }
            try {
                val authResultFirebase = firebaseAuthInstance.createUserWithEmailAndPassword(email, password).await()
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
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            firebaseAuthInstance.signOut()
            tokenManager.clearTokens()
            withContext(Dispatchers.IO) {
                userRepository.clearUsers()
                chatRepository.clearChatMessages()
            }
            _authResult.value = AuthResult.Idle
            Log.d("AuthViewModel", "User logged out and session cleared.")
        }
    }

    private fun updateFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("AuthViewModel", "Fetching FCM token failed", task.exception);
                return@addOnCompleteListener
            }
            val token = task.result ?: return@addOnCompleteListener
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val userProfile = firebaseRepository.getUserProfile(uid)
                    if (userProfile?.fcmTokens?.contains(token) == false) {
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
            try {
                // 1. Đồng bộ hóa khóa
                val localKeyPair = cryptoManager.getOrCreateUserKeyPair(uid)
                val localPublicKeyString = Base64.encodeToString(localKeyPair.public.encoded, Base64.DEFAULT)
                var remoteProfile = firebaseRepository.getUserProfile(uid)
                val profileToSync: UserEntity

                if (remoteProfile == null || remoteProfile.publicKey != localPublicKeyString) {
                    Log.d("AuthViewModel", "Local/Remote key mismatch or new user. Updating Firestore.")
                    profileToSync = UserEntity(
                        uid = uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@'),
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        publicKey = localPublicKeyString,
                        friends = remoteProfile?.friends ?: emptyList(),
                        fcmTokens = remoteProfile?.fcmTokens ?: emptyList()
                    )
                    firebaseRepository.saveUserToFirestore(profileToSync)
                } else {
                    profileToSync = remoteProfile
                }

                // 2. Lấy thông tin bạn bè từ server
                val friendUids = profileToSync.friends
                val friendProfiles = if (friendUids.isNotEmpty()) {
                    firebaseRepository.getUserProfiles(friendUids)
                } else {
                    emptyList()
                }

                // 3. Cập nhật database local với dữ liệu mới nhất
                val allUsersToInsert = friendProfiles + profileToSync
                userRepository.clearUsers()
                userRepository.insertUsers(allUsersToInsert)
                Log.d("AuthViewModel", "User data synced. User: $uid, Friends: ${friendUids.size}")

            } catch(e: Exception) {
                Log.e("AuthViewModel", "Failed to sync user data.", e)
            }
        }
    }

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
                handleSuccessfulLogin(authResultFirebase.user!!)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi xác thực mã OTP: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetAuthResult() {
        _authResult.value = AuthResult.Idle
    }
}