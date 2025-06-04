//package com.workhubui.data.local.entity
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
//@Entity(tableName = "schedule")
//data class ScheduleItemEntity(
//    @PrimaryKey(autoGenerate = true) val id: Int = 0,
//    val title: String,
//    val description: String?,
//    val startTime: Long,
//    val endTime: Long
//)
package com.workhubui.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// ScheduleItemEntity đại diện cho một mục lịch trình trong Room Database.
@Entity(tableName = "schedule")
data class ScheduleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Khóa chính tự động tăng
    val title: String, // Tiêu đề của lịch trình
    val detail: String?, // Chi tiết/mô tả thêm của lịch trình (có thể null)
    val startTime: Long, // Thời gian bắt đầu (dạng timestamp)
    val endTime: Long // Thời gian kết thúc (dạng timestamp)
)