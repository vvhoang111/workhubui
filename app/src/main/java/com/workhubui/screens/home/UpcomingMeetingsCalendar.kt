
package com.workhubui.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workhubui.screens.Meeting
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthCalendar(
    meetings: List<Meeting>, // Thêm danh sách meetings để hiển thị chấm sự kiện
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember(selectedDate.year, selectedDate.month) { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = LocalDate.now()

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7

    val meetingsInCurrentMonthMap = remember(meetings, currentMonth) {
        meetings
            .filter { meeting -> YearMonth.from(meeting.date) == currentMonth }
            .groupBy { it.date.dayOfMonth }
    }

    val dayCellHeightEstimate: Dp = 48.dp
    val numberOfRowsEstimate = 6
    val calendarGridHeightEstimate = dayCellHeightEstimate * numberOfRowsEstimate

    Column(modifier = modifier.fillMaxWidth()) {
        // Month Header
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

        // Day of Week Headers
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

        // Calendar Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(calendarGridHeightEstimate),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
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