package com.workhubui.screens.home

import android.app.Application
import android.os.Build
import android.widget.Toast // Thêm import cho Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ScrollState //
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.workhubui.data.local.AppDatabase
import com.workhubui.data.local.entity.ScheduleItemEntity // Đảm bảo import này
import com.workhubui.data.repository.ChatRepository
import com.workhubui.model.ChatMessage
import com.workhubui.navigation.Routes
import com.workhubui.screens.Meeting // Import Meeting data class
import com.workhubui.screens.auth.AuthViewModel
import com.workhubui.screens.auth.AuthViewModelFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle // Thêm FormatStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O) // Cần cho LocalDate và các API java.time khác
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val app = LocalContext.current.applicationContext as Application
    val context = LocalContext.current

    // ViewModel cho dữ liệu cụ thể của HomeScreen
    val homeViewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                // Giả sử ChatRepository là dependency duy nhất cần cho HomeViewModel theo định nghĩa cũ
                // Nếu HomeViewModel cần thêm ScheduleRepository, bạn sẽ cần cung cấp nó ở đây
                val chatDao = AppDatabase.getInstance(context).chatMessageDao()
                val chatRepo = ChatRepository(chatDao)
                HomeViewModel(app, chatRepo)
            }
        }
    )

    // ViewModel cho trạng thái xác thực
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(app))
    val currentUserEmail by authViewModel.currentUserEmail.collectAsState()

    val scheduleList by homeViewModel.scheduleList.collectAsState()
    val recentChats by homeViewModel.recentChats.collectAsState()
    val upcomingMeetings by homeViewModel.upcomingMeetings.collectAsState()
    val selectedCalendarDate by homeViewModel.selectedDateForCalendar.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Tạo Task hoặc Meeting")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Áp dụng padding từ Scaffold
                .padding(horizontal = 16.dp) // Padding ngang ban đầu
                .verticalScroll(rememberScrollState()) // Cho phép toàn bộ cột cuộn được
        ) {
            // --- Biểu tượng Cài đặt & Lời chào người dùng ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp), // Thêm padding trên
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentUserEmail?.let { "Hi, ${it.substringBefore("@")}" } ?: getGreeting(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { navController.navigate(Routes.SETTINGS) },
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Cài đặt")
                }
            }

            Spacer(Modifier.height(24.dp)) // Tăng khoảng cách

            // --- Lịch Mini Cuộc họp Sắp tới ---
            Text(text = "Cuộc họp sắp tới", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            UpcomingMeetingsCalendar( // Gọi Composable lịch mini
                meetings = upcomingMeetings,
                selectedDate = selectedCalendarDate,
                onDateSelected = { date: LocalDate -> // Chỉ định kiểu rõ ràng cho date
                    homeViewModel.onDateSelectedInCalendar(date)
                },
                modifier = Modifier.fillMaxWidth()
            )
            // Hiển thị các cuộc họp cho ngày được chọn từ lịch mini
            val meetingsForSelectedDate = upcomingMeetings.filter { it.date == selectedCalendarDate }
            if (meetingsForSelectedDate.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cuộc họp ngày ${selectedCalendarDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                meetingsForSelectedDate.forEach { meeting ->
                    Text(
                        "- ${meeting.title} lúc ${meeting.time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))}", // Sử dụng Locale US cho AM/PM
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(16.dp))


            // --- Phần Lịch trình Hôm nay ---
            Text(text = "Lịch trình hôm nay", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (scheduleList.isEmpty()) {
                Text("Không có hoạt động nào được lên lịch cho hôm nay.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
            } else {
                ScheduleBox(scheduleList = scheduleList) // ScheduleBox không cần cuộn riêng nữa
            }

            Spacer(Modifier.height(24.dp))

            // --- Phần Chat Gần đây ---
            RecentChatSection(
                recentChats = recentChats,
                currentUser = currentUserEmail ?: "unknown_user_id", // Truyền email người dùng hiện tại
                navController = navController
            )

            Spacer(Modifier.height(16.dp)) // Spacer ở cuối
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tạo mới") },
            text = { Text("Bạn muốn tạo gì?") },
            confirmButton = {
                Button(onClick = {
                    // TODO: Điều hướng đến Màn hình Tạo Task
                    showDialog = false
                    Toast.makeText(context, "Chức năng tạo Task chưa sẵn sàng", Toast.LENGTH_SHORT).show()
                }) { Text("Task") }
            },
            dismissButton = {
                Button(onClick = {
                    // TODO: Điều hướng đến Màn hình/Dialog Tạo Meeting
                    showDialog = false
                    Toast.makeText(context, "Chức năng tạo Meeting chưa sẵn sàng", Toast.LENGTH_SHORT).show()
                }) { Text("Meeting") }
            }
        )
    }
}

@Composable
fun RecentChatSection(
    recentChats: List<ChatMessage>,
    currentUser: String,
    navController: NavHostController
) {
    val context = LocalContext.current // Lấy context cho Toast
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chat gần đây", style = MaterialTheme.typography.titleMedium)
            Text(
                "Xem thêm",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable {
                        if (currentUser != "unknown_user_id") {
                            // TODO: Điều hướng đến màn hình danh sách chat đầy đủ
                            // Ví dụ: navController.navigate("${Routes.CHAT_LIST}/$currentUser")
                            Toast.makeText(context, "Điều hướng đến danh sách chat đầy đủ", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        if (recentChats.isEmpty()){
            Text("Không có cuộc trò chuyện gần đây.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        } else {
            recentChats.take(3).forEach { chat ->
                val otherUser = if (chat.sender.equals(currentUser, ignoreCase = true)) chat.receiver else chat.sender
                // Chỉ hiển thị nếu otherUser không rỗng và không phải là chính người dùng hiện tại (nếu tin nhắn tự gửi)
                if (otherUser.isNotBlank() && !otherUser.equals(currentUser, ignoreCase = true)) {
                    ChatRow( // Đảm bảo ChatRow được định nghĩa
                        name = otherUser.substringBefore("@"), // Hiển thị phần trước @ của email (hoặc tên nếu có)
                        message = chat.content,
                        time = formatTimestamp(chat.timestamp), // Đảm bảo formatTimestamp được định nghĩa
                        onClick = {
                            if (currentUser != "unknown_user_id") {
                                navController.navigate("${Routes.CHAT}/$currentUser/$otherUser")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatRow(name: String, message: String, time: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AccountCircle, contentDescription = "Avatar", modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) // Thay đổi style
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium, // Thay đổi style
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Thêm màu cho dễ đọc hơn
            )
        }
        Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // Thêm màu
    }
}

// Hàm định dạng thời gian
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Hàm lời chào
fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Chào buổi sáng"
        in 12..17 -> "Chào buổi chiều"
        else -> "Chào buổi tối"
    }
}