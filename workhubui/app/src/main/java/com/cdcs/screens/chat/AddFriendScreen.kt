package com.cdcs.screens.chat

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.cdcs.data.local.AppDatabase
import com.cdcs.data.remote.FirebaseRepository
import com.cdcs.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel cho màn hình Thêm bạn
class AddFriendViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val firebaseRepository: FirebaseRepository
) : AndroidViewModel(application) {

    private val _addFriendStatus = MutableStateFlow<String?>(null)
    val addFriendStatus: StateFlow<String?> = _addFriendStatus.asStateFlow()

    // Hàm tìm kiếm, kết bạn và tạo phòng chat
    fun establishFriendship(friendEmail: String, currentUserUid: String) {
        viewModelScope.launch {
            if (friendEmail.isBlank()) {
                _addFriendStatus.value = "Email không được để trống."
                return@launch
            }
            val ownEmail = FirebaseAuth.getInstance().currentUser?.email
            if (friendEmail.equals(ownEmail, ignoreCase = true)) {
                _addFriendStatus.value = "Bạn không thể tự kết bạn với chính mình."
                return@launch
            }

            _addFriendStatus.value = "Đang xử lý..."
            try {
                // Tìm người dùng trên Firestore
                val friendUserEntity = firebaseRepository.getUserByEmailFromFirestore(friendEmail)

                if (friendUserEntity != null) {
                    // Gọi hàm mới để tạo mối quan hệ bạn bè và phòng chat
                    firebaseRepository.establishFriendshipAndCreateChatRoom(currentUserUid, friendUserEntity.uid)

                    // Lưu hồ sơ bạn bè vào DB cục bộ để UI cập nhật ngay
                    userRepository.insertUser(friendUserEntity)

                    _addFriendStatus.value = "Đã thêm ${friendUserEntity.displayName ?: friendUserEntity.email} thành công!"
                } else {
                    _addFriendStatus.value = "Không tìm thấy người dùng với email này."
                }
            } catch (e: Exception) {
                _addFriendStatus.value = "Lỗi khi thêm bạn bè: ${e.localizedMessage}"
                Log.e("AddFriendDebug", "Error in ViewModel", e)
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

// Giao diện người dùng
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
    val addFriendViewModel: AddFriendViewModel = viewModel(factory = AddFriendViewModelFactory(application))
    val addFriendStatus by addFriendViewModel.addFriendStatus.collectAsState()

    var friendEmail by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(addFriendStatus) {
        addFriendStatus?.let { status ->
            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
            // Sau khi thêm thành công, quay lại màn hình danh sách chat
            if (status.contains("thành công")) {
                navController.popBackStack()
            }
            addFriendViewModel.clearStatus()
        }
    }

    fun handleAddFriendClick() {
        if (currentUserUid != null) {
            addFriendViewModel.establishFriendship(friendEmail.trim(), currentUserUid)
        } else {
            Toast.makeText(context, "Không thể xác thực người dùng hiện tại.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm bạn bè & Bắt đầu chat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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

            OutlinedTextField(
                value = friendEmail,
                onValueChange = { friendEmail = it },
                label = { Text("Email của người bạn muốn chat") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { handleAddFriendClick() })
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { handleAddFriendClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Thêm bạn", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
