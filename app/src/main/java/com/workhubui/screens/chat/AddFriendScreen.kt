package com.workhubui.screens.chat

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workhubui.data.local.AppDatabase
import com.workhubui.data.remote.FirebaseRepository
import com.workhubui.data.repository.UserRepository
import com.workhubui.screens.auth.AuthViewModel
import com.workhubui.screens.auth.AuthViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController

// ViewModel cho màn hình AddFriend
class AddFriendViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val firebaseRepository: FirebaseRepository
) : AndroidViewModel(application) {

    private val _addFriendStatus = MutableStateFlow<String?>(null)
    val addFriendStatus: StateFlow<String?> = _addFriendStatus

    fun addFriendByEmail(email: String, currentUserEmail: String?) {
        viewModelScope.launch {
            if (email.isBlank() || email == currentUserEmail) {
                _addFriendStatus.value = "Email không hợp lệ hoặc là email của bạn."
                return@launch
            }

            _addFriendStatus.value = "Đang tìm kiếm..."
            try {
                val friendUserEntity = firebaseRepository.getUserByEmailFromFirestore(email)

                if (friendUserEntity != null) {
                    userRepository.insertUser(friendUserEntity)
                    _addFriendStatus.value =
                        "Đã thêm ${friendUserEntity.displayName ?: friendUserEntity.email} vào danh sách bạn bè!"
                } else {
                    _addFriendStatus.value = "Không tìm thấy người dùng với email này."
                }
            } catch (e: Exception) {
                _addFriendStatus.value = "Lỗi khi thêm bạn bè: ${e.localizedMessage}"
            }
        }
    }

    fun clearStatus() {
        _addFriendStatus.value = null
    }
}

// Factory cho AddFriendViewModel
class AddFriendViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddFriendViewModel::class.java)) {
            val userDao = AppDatabase.getInstance(application).userDao()
            val userRepository = UserRepository(userDao)
            val firebaseRepository = FirebaseRepository()
            @Suppress("UNCHECKED_CAST")
            return AddFriendViewModel(application, userRepository, firebaseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))
    val currentUserEmail by authViewModel.currentUserEmail.collectAsState()

    val addFriendViewModel: AddFriendViewModel = viewModel(factory = AddFriendViewModelFactory(application))
    val addFriendStatus by addFriendViewModel.addFriendStatus.collectAsState()

    val friendEmailState = remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(addFriendStatus) {
        addFriendStatus?.let { status ->
            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
            addFriendViewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm bạn bè") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = "Thêm bạn bè",
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))

            BasicTextField(
                value = friendEmailState.value,
                onValueChange = { friendEmailState.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = TextStyle.Default.copy(fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (friendEmailState.value.isEmpty()) {
                            Text(
                                text = "Email của bạn bè",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    addFriendViewModel.addFriendByEmail(friendEmailState.value, currentUserEmail)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Thêm bạn", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
