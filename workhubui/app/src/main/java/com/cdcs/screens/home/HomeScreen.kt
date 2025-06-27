package com.cdcs.screens.home

import android.app.Application
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.cdcs.data.local.AppDatabase
import com.cdcs.data.local.entity.ScheduleItemEntity
import com.cdcs.data.remote.FirebaseRepository
import com.cdcs.data.repository.ChatRepository
import com.cdcs.model.ChatMessage
import com.cdcs.navigation.Routes
import com.cdcs.screens.Meeting
import com.cdcs.screens.auth.AuthViewModel
import com.cdcs.screens.auth.AuthViewModelFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale


internal fun getGreetingInternal(userName: String?): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greetingPeriod = when (hour) {
        in 5..11 -> "Chào buổi sáng"
        in 12..17 -> "Chào buổi chiều"
        else -> "Chào buổi tối"
    }
    return if (!userName.isNullOrBlank()) {
        val nameToShow = userName.substringBefore("@")
        "$greetingPeriod, $nameToShow!"
    } else {
        greetingPeriod
    }
}

internal fun formatTimestampInternal(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

internal fun formatTimeRangeInternal(start: Long, end: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    val context = LocalContext.current

    val homeViewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val chatDao = AppDatabase.getInstance(context).chatMessageDao()
                val firebaseRepository = FirebaseRepository()
                val chatRepo = ChatRepository(chatDao, firebaseRepository)
                HomeViewModel(application, chatRepo)
            }
        }
    )

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(application))

    // << SỬA LỖI: Lấy đối tượng currentUser và suy ra email từ đó >>
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserEmail = currentUser?.email

    val scheduleList by homeViewModel.scheduleList.collectAsState()
    val recentChats by homeViewModel.recentChats.collectAsState()
    val upcomingMeetings by homeViewModel.upcomingMeetings.collectAsState()
    val selectedCalendarDate by homeViewModel.selectedDateForCalendar.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    val tasksForSelectedDate = remember(selectedCalendarDate) {
        if (selectedCalendarDate == LocalDate.now()) {
            mutableStateListOf(
                "Tạo wireframes cho ứng dụng (mẫu)",
                "Xem xét yêu cầu của khách hàng (mẫu)"
            )
        } else {
            mutableStateListOf<String>()
        }
    }
    val checkedStates = remember(tasksForSelectedDate) {
        mutableStateListOf<Boolean>().apply { addAll(List(tasksForSelectedDate.size) { false }) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Tạo mới")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getGreetingInternal(currentUserEmail),
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

            Spacer(Modifier.height(24.dp))

            val today = LocalDate.now()
            val daysUntilSelectedDate = ChronoUnit.DAYS.between(today, selectedCalendarDate)
            val scheduleTitleText = when {
                selectedCalendarDate == today -> "Lịch trình hôm nay"
                daysUntilSelectedDate == 1L -> "Lịch trình ngày ${selectedCalendarDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} (Ngày mai)"
                daysUntilSelectedDate > 1L -> "Lịch trình ngày ${selectedCalendarDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} (${daysUntilSelectedDate} ngày nữa)"
                daysUntilSelectedDate == -1L -> "Lịch trình ngày ${selectedCalendarDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} (Hôm qua)"
                daysUntilSelectedDate < -1L -> "Lịch trình ngày ${selectedCalendarDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} (${-daysUntilSelectedDate} ngày trước)"
                else -> "Lịch trình ngày ${selectedCalendarDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}"
            }

            Text(text = scheduleTitleText, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            TwoSidedDailyTimeline(
                tasks = tasksForSelectedDate,
                checkedStates = checkedStates,
                meetings = upcomingMeetings.filter { it.date == selectedCalendarDate },
                selectedDate = selectedCalendarDate,
                onDateSelected = { date: LocalDate ->
                    homeViewModel.onDateSelectedInCalendar(date)
                },
                dailyScheduleItems = scheduleList.filter {
                    val itemDate = LocalDate.ofEpochDay(it.startTime / (1000 * 60 * 60 * 24))
                    itemDate == selectedCalendarDate
                }
            )

            Spacer(Modifier.height(24.dp))

            RecentChatSection(
                recentChats = recentChats,
                currentUser = currentUser?.uid ?: "unknown_user_id",
                navController = navController
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Tạo mới") },
            text = { Text("Bạn muốn tạo gì?") },
            confirmButton = {
                Button(onClick = {
                    showCreateDialog = false
                    navController.navigate(Routes.ADD_TASK)
                }) { Text("Việc cần làm") }
            },
            dismissButton = {
                Button(onClick = {
                    showCreateDialog = false
                    navController.navigate(Routes.ADD_SCHEDULE_ITEM)
                }) { Text("Lịch/Meeting") }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TwoSidedDailyTimeline(
    tasks: List<String>,
    checkedStates: MutableList<Boolean>,
    meetings: List<Meeting>,
    dailyScheduleItems: List<ScheduleItemEntity>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        InternalMonthCalendar(
            meetings = meetings,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Việc cần làm (${selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                if (tasks.isEmpty()) {
                    Text("Không có việc cần làm cho ngày này.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    tasks.forEachIndexed { index, task ->
                        TaskRow(
                            task = task,
                            checked = checkedStates.getOrElse(index) { false },
                            onCheckedChange = { checkedStates[index] = it }
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Lịch trình & Họp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                val combinedSchedule = meetings.map { meeting ->
                    ScheduleItemEntity(
                        id = meeting.hashCode().toLong().toInt(),
                        title = meeting.title,
                        detail = "Cuộc họp lúc ${meeting.time.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                        startTime = selectedDate.atTime(meeting.time).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        endTime = selectedDate.atTime(meeting.time.plusHours(1)).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                } + dailyScheduleItems

                val sortedCombinedSchedule = combinedSchedule.sortedBy { it.startTime }

                if (sortedCombinedSchedule.isEmpty()) {
                    Text("Không có lịch trình hoặc cuộc họp nào cho ngày này.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    sortedCombinedSchedule.forEach { item ->
                        HomeScreenScheduleCard(item = item)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Spacer(Modifier.width(12.dp))
        Text(task, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RecentChatSection(
    recentChats: List<ChatMessage>,
    currentUser: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chat gần đây", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Xem tất cả",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable {
                        if (currentUser != "unknown_user_id") {
                            navController.navigate(Routes.CHAT_LIST)
                        } else {
                            Toast.makeText(context, "Không thể xác định người dùng hiện tại.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(Modifier.height(12.dp))
        if (recentChats.isEmpty()){
            Text("Không có cuộc trò chuyện gần đây.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            recentChats.take(3).forEach { chat ->
                val otherUserId = if (chat.sender == currentUser) chat.receiver else chat.sender
                if (otherUserId.isNotBlank() && otherUserId != currentUser) {
                    ChatRow(
                        name = otherUserId.substring(0, 5) + "...", // Cần lấy displayName từ UserEntity
                        message = chat.content,
                        time = formatTimestampInternal(chat.timestamp),
                        onClick = {
                            if (currentUser != "unknown_user_id") {
                                navController.navigate("${Routes.CHAT}/$currentUser/$otherUserId")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatRow(name: String, message: String, time: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Ảnh đại diện của $name",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun HomeScreenScheduleCard(item: ScheduleItemEntity) {
    val timeRange = formatTimeRangeInternal(item.startTime, item.endTime)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(timeRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            item.detail?.let {
                if (it.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun InternalMonthCalendar(
    meetings: List<Meeting>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember(selectedDate.year, selectedDate.month) { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = LocalDate.now()

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value % 7)

    val meetingsInCurrentMonthMap = remember(meetings, currentMonth) {
        meetings
            .filter { meeting -> YearMonth.from(meeting.date) == currentMonth }
            .groupBy { it.date.dayOfMonth }
    }

    val dayCellHeightEstimate: Dp = 48.dp
    val numberOfRowsEstimate = 6
    val calendarGridHeightEstimate = dayCellHeightEstimate * numberOfRowsEstimate

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Tháng trước")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Tháng sau")
            }
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            val daysOfWeek = remember {
                listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
            }
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(calendarGridHeightEstimate),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(dayOfWeekOffset) {
                Box(modifier = Modifier.aspectRatio(1f))
            }

            items(daysInMonth) { index ->
                val day = index + 1
                val dateInGrid = currentMonth.atDay(day)
                val isSelectedDate = dateInGrid == selectedDate
                val isTodayDate = dateInGrid == today
                val hasMeeting = meetingsInCurrentMonthMap.containsKey(day)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            when {
                                isSelectedDate -> MaterialTheme.colorScheme.primary
                                isTodayDate -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onDateSelected(dateInGrid) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = day.toString(),
                            fontSize = 12.sp,
                            color = when {
                                isSelectedDate -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (hasMeeting) {
                            Box(
                                Modifier
                                    .padding(top = 2.dp)
                                    .size(5.dp)
                                    .background(
                                        if (isSelectedDate) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.tertiary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
