//package com.workhubui.data.local.entity
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
//@Entity(tableName = "users") // Đảm bảo tên bảng là "users"
//data class UserEntity(
//    @PrimaryKey val uid: String, // Khóa chính
//    val email: String?,
//    val displayName: String?,
//    val photoUrl: String?
//    // Thêm các trường khác nếu cần
//)
package com.workhubui.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// UserEntity đại diện cho một người dùng (hoặc bạn bè) trong Room Database.
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String, // User ID từ Firebase (khóa chính)
    val email: String?, // Email của người dùng
    val displayName: String?, // Tên hiển thị của người dùng
    val photoUrl: String? // URL ảnh đại diện của người dùng
    // Bạn có thể thêm các trường khác nếu cần, ví dụ: status, lastSeen, etc.
)