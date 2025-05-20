package com.workhubui.components



import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workhubui.screens.Meeting
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MeetingItem(meeting: Meeting) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = meeting.title, fontWeight = FontWeight.Bold)
        Text(text = meeting.time.format(DateTimeFormatter.ofPattern("HH:mm")))
    }
}
