//package com.workhubui.screens.home
//
//// Bỏ import ScrollState và rememberScrollState nếu không dùng nữa
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.workhubui.data.local.entity.ScheduleItemEntity
//import java.text.SimpleDateFormat
//import java.util.*
//
//@Composable
//fun ScheduleBox(
//    scheduleList: List<ScheduleItemEntity>
//    // Bỏ scrollState: ScrollState = rememberScrollState()
//) {
//    Column(
//        modifier = Modifier.fillMaxWidth()
//        // Bỏ .verticalScroll(scrollState)
//    ) {
//        if (scheduleList.isEmpty()) {
//            // Có thể để trống hoặc hiển thị một Text nhỏ,
//            // vì HomeScreen đã có logic kiểm tra rỗng cho scheduleList
//        } else {
//            scheduleList.forEach { item ->
//                ScheduleCard(item)
//                Spacer(Modifier.height(12.dp)) // [cite: 138]
//            }
//        }
//    }
//}
//
//// ScheduleCard và formatTimeRange giữ nguyên như bạn đã có
//@Composable
//fun ScheduleCard(item: ScheduleItemEntity) {
//    val timeRange = formatTimeRange(item.startTime, item.endTime)
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 4.dp, vertical = 2.dp), // Thêm chút padding
//        shape = MaterialTheme.shapes.medium,
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Thêm độ nổi
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Text(item.title, style = MaterialTheme.typography.titleMedium) // [cite: 139]
//            Spacer(Modifier.height(4.dp))
//            Text(timeRange, style = MaterialTheme.typography.bodySmall)
//            item.description?.let {
//                if (it.isNotBlank()) { // Chỉ hiển thị nếu mô tả không rỗng
//                    Spacer(Modifier.height(4.dp))
//                    Text(it, style = MaterialTheme.typography.bodyMedium)
//                }
//            }
//        }
//    }
//}
//
//fun formatTimeRange(start: Long, end: Long): String {
//    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
//    return "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
//}
package com.cdcs.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cdcs.data.local.entity.ScheduleItemEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleBox(
    scheduleList: List<ScheduleItemEntity>
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (scheduleList.isEmpty()) {
            Text("Không có hoạt động nào được lên lịch.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        } else {
            scheduleList.forEach { item ->
                ScheduleCard(item)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ScheduleCard(item: ScheduleItemEntity) {
    val timeRange = formatTimeRange(item.startTime, item.endTime)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(timeRange, style = MaterialTheme.typography.bodySmall)
            item.detail?.let { // Hiển thị detail nếu có
                if (it.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

fun formatTimeRange(start: Long, end: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
}