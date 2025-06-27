package com.cdcs.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cdcs.data.local.Converters

@Keep // Giữ lại class và constructor khi build release
@Entity(tableName = "users")
@TypeConverters(Converters::class) // Chỉ cho Room cách xử lý List<String>
data class UserEntity(
    @PrimaryKey val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val publicKey: String? = null,
    // << SỬA LỖI: Thêm các trường này >>
    val friends: List<String> = emptyList(),
    val fcmTokens: List<String> = emptyList()
)
