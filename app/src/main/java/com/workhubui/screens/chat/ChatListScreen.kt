package com.workhubui.screens.chat

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.workhubui.model.ChatMessage // Giả sử bạn có model này cho các cuộc trò chuyện gần đây
import com.workhubui.navigation.Routes
import com.workhubui.screens.auth.AuthViewModel // Để lấy currentUser
import com.workhubui.screens.auth.AuthViewModelFactory
// import com.workhubui.screens.home.formatTimestamp // Nếu bạn muốn dùng lại hàm này, cần di chuyển nó ra utility

// Tạm thời định nghĩa formatTimestamp ở đây, nên đưa vào file utils chung
private fun formatTimestampLocal(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}


// Dummy data class cho danh sách chat
data class ChatRoom(val id: String, val name: String, val lastMessage: String, val timestamp: Long, val unreadCount: Int = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))
    val currentUserEmail by authViewModel.currentUserEmail.collectAsState()

    // TODO: Khởi tạo ChatListViewModel để lấy danh sách các phòng chat
    // val chatListViewModel: ChatListViewModel = viewModel(...)
    // val chatRooms by chatListViewModel.chatRooms.collectAsState()

    // Dữ liệu giả lập cho danh sách phòng chat
    val dummyChatRooms = remember {
        listOf(
            ChatRoom("user2@example.com", "Alice Wonderland", "Great, see you then!", System.currentTimeMillis() - 100000, 2),
            ChatRoom("user3@example.com", "Bob The Builder", "Okay, I will check it.", System.currentTimeMillis() - 500000),
            ChatRoom("user4@example.com", "Charlie Brown", "Sounds good!", System.currentTimeMillis() - 1000000, 1)
        )
    }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn") },
                actions = {
                    IconButton(onClick = { /* TODO: Handle search action */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "Tìm kiếm")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant ,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Thanh tìm kiếm (ví dụ cơ bản)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Tìm kiếm cuộc trò chuyện...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null)}
            )

            if (dummyChatRooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có cuộc trò chuyện nào.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(dummyChatRooms.filter { it.name.contains(searchQuery, ignoreCase = true) || it.lastMessage.contains(searchQuery, ignoreCase = true) }) { room ->
                        ChatListItem(chatRoom = room) {
                            currentUserEmail?.let { currentEmail ->
                                // Điều hướng đến màn hình chi tiết cuộc trò chuyện
                                navController.navigate(Routes.CHAT + "/${currentEmail}/${room.id}")
                            }
                        }
                        // HorizontalDivider() // Nếu bạn muốn có đường kẻ giữa các mục
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
            Text(
                formatTimestampLocal(chatRoom.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            if (chatRoom.unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                // Badge cho tin nhắn chưa đọc (ví dụ đơn giản)
                // Bạn có thể dùng Badge Composable của Material 3 nếu muốn phức tạp hơn
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