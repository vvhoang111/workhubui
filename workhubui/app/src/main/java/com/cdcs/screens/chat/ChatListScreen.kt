package com.cdcs.screens.chat

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.cdcs.data.local.AppDatabase
import com.cdcs.data.local.entity.UserEntity
import com.cdcs.data.remote.FirebaseRepository
import com.cdcs.data.repository.ChatRepository
import com.cdcs.data.repository.UserRepository
import com.cdcs.navigation.Routes
import com.cdcs.screens.auth.AuthViewModel
import com.cdcs.screens.auth.AuthViewModelFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class để kết hợp thông tin phòng chat và hồ sơ bạn bè cho UI
data class UiChatRoom(
    val chatRoomId: String,
    val friend: UserEntity,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val lastMessageSenderId: String
)

// ViewModel cho màn hình danh sách chat
class ChatListViewModel(
    application: Application,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    authViewModel: AuthViewModel // Nhận AuthViewModel để theo dõi
) : AndroidViewModel(application) {

    private val _uiChatRooms = MutableStateFlow<List<UiChatRoom>>(emptyList())
    val uiChatRooms: StateFlow<List<UiChatRoom>> = _uiChatRooms.asStateFlow()

    init {
        // << SỬA LỖI: Lắng nghe trạng thái của currentUser thay vì isAuthReady >>
        viewModelScope.launch {
            authViewModel.currentUser.collect { user ->
                if (user != null) {
                    // Nếu có người dùng, bắt đầu lắng nghe các phòng chat của họ
                    listenForChatRooms(user.uid)
                } else {
                    // Nếu không có người dùng (đã đăng xuất), xóa danh sách chat
                    _uiChatRooms.value = emptyList()
                }
            }
        }
    }

    private fun listenForChatRooms(userUid: String) {
        viewModelScope.launch {
            chatRepository.listenToChatRooms(userUid).collectLatest { rooms ->
                val friendUids = rooms.mapNotNull { room ->
                    room.participants.find { it != userUid }
                }.distinct()

                if (friendUids.isEmpty()) {
                    _uiChatRooms.value = emptyList()
                    return@collectLatest
                }

                // Giả định rằng UserRepository đã có hàm getUsers
                val friendProfiles = userRepository.getUsers(friendUids)
                val friendProfileMap = friendProfiles.associateBy { it.uid }

                val uiRooms = rooms.mapNotNull { room ->
                    val friendUid = room.participants.find { it != userUid }
                    val friendProfile = friendProfileMap[friendUid]

                    if (friendProfile != null) {
                        UiChatRoom(
                            chatRoomId = if (userUid > friendUid!!) "$userUid-$friendUid" else "$friendUid-$userUid",
                            friend = friendProfile,
                            lastMessage = room.lastMessage,
                            lastMessageTimestamp = room.lastMessageTimestamp?.time ?: 0,
                            lastMessageSenderId = room.lastMessageSenderId
                        )
                    } else { null }
                }
                _uiChatRooms.value = uiRooms
            }
        }
    }
}

// Factory được cập nhật để truyền AuthViewModel vào
class ChatListViewModelFactory(
    private val application: Application,
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatListViewModel::class.java)) {
            val userDao = AppDatabase.getInstance(application).userDao()
            val chatDao = AppDatabase.getInstance(application).chatMessageDao()
            val firebaseRepo = FirebaseRepository()
            val userRepo = UserRepository(userDao)
            val chatRepo = ChatRepository(chatDao, firebaseRepo)

            @Suppress("UNCHECKED_CAST")
            return ChatListViewModel(application, chatRepo, userRepo, authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Giao diện người dùng
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))
    val chatListViewModel: ChatListViewModel = viewModel(factory = ChatListViewModelFactory(application, authViewModel))

    val chatRooms by chatListViewModel.uiChatRooms.collectAsState()

    // << SỬA LỖI: Lấy currentUser để kiểm tra trạng thái đăng nhập >>
    val currentUser by authViewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.ADD_FRIEND) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Bắt đầu cuộc trò chuyện mới")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.Center) {
            // << SỬA LỖI: Hiển thị nội dung dựa trên currentUser >>
            if (currentUser == null) {
                // Hiển thị vòng xoay tải trong khi chờ auth
                CircularProgressIndicator()
            } else {
                if (chatRooms.isEmpty()) {
                    Text("Chưa có cuộc trò chuyện nào.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp)
                    ) {
                        items(chatRooms, key = { it.chatRoomId }) { room ->
                            ChatListItem(
                                room = room,
                                currentUserUid = currentUser?.uid ?: ""
                            ) {
                                navController.navigate(Routes.CHAT + "/${currentUser?.uid}/${room.friend.uid}")
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(room: UiChatRoom, currentUserUid: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "Avatar",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(room.friend.displayName ?: "Người dùng mới", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (room.lastMessageSenderId == currentUserUid) {
                    Text("Bạn: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    room.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            formatTimestamp(room.lastMessageTimestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
