package com.cdcs.screens.schedule

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cdcs.components.MeetingItem
import com.cdcs.components.MonthCalendar
import com.cdcs.screens.Meeting
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleScreen() {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val meetings = listOf(
        Meeting("Design Review", LocalDate.of(2025, 5, 20), LocalTime.of(9, 0)),
        Meeting("Client Presentation", LocalDate.of(2025, 5, 20), LocalTime.of(11, 0)),
        Meeting("Team Meeting", LocalDate.of(2025, 5, 22), LocalTime.of(14, 0))
    )

    val todayMeetings = meetings.filter { it.date == selectedDate }

    Column(modifier = Modifier.padding(16.dp)) {
        MonthCalendar(selectedDate = selectedDate, onDateSelected = { selectedDate = it })

        Spacer(modifier = Modifier.height(16.dp))

        Text("Lịch họp ngày ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        todayMeetings.forEach { meeting ->
            MeetingItem(meeting)
        }
    }
}
