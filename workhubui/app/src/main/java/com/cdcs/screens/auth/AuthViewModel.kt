package com.cdcs.screens.auth

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.cdcs.data.local.AppDatabase
import com.cdcs.data.local.entity.UserEntity
import com.cdcs.data.remote.FirebaseRepository
import com.cdcs.data.repository.ChatRepository
import com.cdcs.data.repository.UserRepository
import com.cdcs.security.CryptoManager
import com.cdcs.security.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        _currentUser.value = firebaseAuthInstance.currentUser
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
            _authResult.value = AuthResult.Loading
            if (email.isBlank() || password.isBlank()) {
                _authResult.value = AuthResult.Error("Email và mật khẩu không được để trống."); return@launch
            }
            try {
                val authResultFirebase = firebaseAuthInstance.signInWithEmailAndPassword(email, password).await()
                handleSuccessfulLogin(authResultFirebase.user!!)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi đăng nhập: ${e.localizedMessage}")
            }
        }
    }

    fun signupUser(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            if (email.isBlank() || password.isBlank()) {
                _authResult.value = AuthResult.Error("Email và mật khẩu không được để trống."); return@launch
            }
            try {
                val authResultFirebase = firebaseAuthInstance.createUserWithEmailAndPassword(email, password).await()
                _currentUser.value = authResultFirebase.user
                val firebaseUser = authResultFirebase.user!!

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
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            try {
                val authResultFirebase = firebaseAuthInstance.signInWithCredential(credential).await()
                handleSuccessfulLogin(authResultFirebase.user!!)
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error("Lỗi đăng nhập Google: ${e.localizedMessage}")
            }
        }
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

            val friendUids = userProfile.friends
            if (friendUids.isNotEmpty()) {
                val friendProfiles = firebaseRepository.getUserProfiles(friendUids)
                userRepository.clearUsers()
                userRepository.insertUsers(friendProfiles + userProfile)
            } else {
                userRepository.clearUsers()
                userRepository.insertUsers(listOf(userProfile))
            }
            Log.d("AuthViewModel", "User data sync complete.")
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
}
