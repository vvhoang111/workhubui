package com.workhubui.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users") // Đảm bảo tên bảng là "users"
data class UserEntity(
    @PrimaryKey val uid: String, // Khóa chính
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
    // Thêm các trường khác nếu cần
)