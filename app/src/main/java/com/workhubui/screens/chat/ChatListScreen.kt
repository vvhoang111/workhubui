package com.workhubui.screens.chat

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.* // Import này bao gồm getValue và setValue cho by delegate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel // Đảm bảo import ViewModel
import androidx.lifecycle.ViewModelProvider // Đảm bảo import ViewModelProvider
import androidx.lifecycle.viewModelScope // Đảm bảo import viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.workhubui.data.local.AppDatabase
import com.workhubui.data.local.entity.UserEntity
import com.workhubui.data.repository.UserRepository
import com.workhubui.navigation.Routes
import com.workhubui.screens.auth.AuthViewModel
import com.workhubui.screens.auth.AuthViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow // Đảm bảo import MutableStateFlow
import kotlinx.coroutines.flow.StateFlow // Đảm bảo import StateFlow
import kotlinx.coroutines.flow.collectLatest // Đảm bảo import collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState // Đảm bảo import collectAsState

// ChatListViewModel
class ChatListViewModel(application: Application, private val userRepository: UserRepository) : ViewModel() {
    private val _users = MutableStateFlow<List<UserEntity>>(emptyList())
    val users: StateFlow<List<UserEntity>> = _users

    init {
        // Sử dụng viewModelScope để khởi chạy coroutine
        viewModelScope.launch {
            loadUsers()
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers().collectLatest { usersList -> // Đổi 'it' thành 'usersList' để rõ ràng hơn
                _users.value = usersList
            }
        }
    }
}

// Factory cho ChatListViewModel
class ChatListViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    // Sửa lỗi 'create' overrides nothing và ViewModel type mismatch
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatListViewModel::class.java)) {
            val userDao = AppDatabase.getInstance(application).userDao()
            val userRepository = UserRepository(userDao)
            @Suppress("UNCHECKED_CAST")
            return ChatListViewModel(application, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))
    val currentUserEmail by authViewModel.currentUserEmail.collectAsState()

    val chatListViewModel: ChatListViewModel = viewModel(factory = ChatListViewModelFactory(application))
    val allUsers by chatListViewModel.users.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn") },
                actions = {
                    IconButton(onClick = { /* TODO: Handle search action */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "Tìm kiếm")
                    }
                    IconButton(onClick = { navController.navigate(Routes.ADD_FRIEND) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Thêm bạn bè")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Tìm kiếm cuộc trò chuyện...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )

            val filteredUsers = allUsers.filter { user ->
                // Lọc bỏ chính người dùng hiện tại khỏi danh sách chat
                user.email != currentUserEmail &&
                        (user.displayName?.contains(searchQuery, ignoreCase = true) == true ||
                                user.email?.contains(searchQuery, ignoreCase = true) == true)
            }

            if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có cuộc trò chuyện nào.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Sửa lỗi 'uid' bằng cách sử dụng safe call và Elvis operator
                    items(filteredUsers, key = { it.uid ?: "" }) { user ->
                        // Lấy tin nhắn cuối cùng cho cuộc trò chuyện này (TODO: cần logic phức tạp hơn)
                        val lastMessage = "Tin nhắn cuối cùng..." // Placeholder
                        val lastMessageTimestamp = System.currentTimeMillis() // Placeholder

                        ChatListItem(
                            chatRoom = ChatRoom(
                                id = user.uid ?: "", // Sửa lỗi 'uid'
                                name = user.displayName ?: user.email?.substringBefore("@") ?: "Unknown User", // Sửa lỗi 'displayName' và 'email'
                                lastMessage = lastMessage,
                                timestamp = lastMessageTimestamp
                            )
                        ) {
                            currentUserEmail?.let { currentEmail ->
                                navController.navigate(Routes.CHAT + "/${currentEmail}/${user.email}") // Sửa lỗi 'email'
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(chatRoom: ChatRoom, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "Avatar ${chatRoom.name}",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(chatRoom.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                chatRoom.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            // Sửa lỗi 'formatTimestamp' bằng cách sử dụng hàm placeholder hoặc đảm bảo import đúng
            Text(
                formatTimestamp(chatRoom.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            if (chatRoom.unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        chatRoom.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Dummy data class cho danh sách chat (có thể thay thế bằng UserEntity hoặc một model phức tạp hơn)
data class ChatRoom(val id: String, val name: String, val lastMessage: String, val timestamp: Long, val unreadCount: Int = 0)

// Hàm placeholder cho formatTimestamp.
// Bạn cần đảm bảo hàm này được định nghĩa ở đâu đó trong project của bạn
// (ví dụ: trong file com.workhubui.screens.home.formatTimestamp như import ban đầu)
// Nếu không, hãy giữ hàm này hoặc thay thế bằng logic định dạng thời gian của bạn.
fun formatTimestamp(timestamp: Long): String {
    return java.text.SimpleDateFormat("HH:mm").format(java.util.Date(timestamp))
}
