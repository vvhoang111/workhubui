package com.workhubui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthCalendar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val daysInMonth = selectedDate.lengthOfMonth()
    val firstDayOfMonth = selectedDate.withDayOfMonth(1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7

    Column {
        LazyVerticalGrid(columns = GridCells.Fixed(7)) {
            // Empty space for the start of the month
            items(dayOfWeekOffset) {
                Text("")
            }

            items(daysInMonth) { index ->
                val day = index + 1
                val date = selectedDate.withDayOfMonth(day)
                Text(
                    text = day.toString(),
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { onDateSelected(date) }
                        .background(if (date == today) Color.LightGray else Color.Transparent)
                )
            }
        }
    }
}
