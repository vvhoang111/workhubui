package com.cdcs.model

import androidx.annotation.Keep
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class ChatRoomMetadata(
    // UID của những người tham gia
    val participants: List<String> = emptyList(),

    // Thông tin về tin nhắn cuối cùng để hiển thị preview
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp
    val lastMessageTimestamp: Date? = null,

    // Thêm các trường khác nếu cần, ví dụ: số tin nhắn chưa đọc
    // val unreadCount: Map<String, Int> = emptyMap()
)
