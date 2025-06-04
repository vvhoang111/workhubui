//package com.workhubui.model
//
//data class ChatMessage(
//    val sender: String,
//    val receiver: String,
//    val content: String,
//    val timestamp: Long
//)
//
//// Extension để chuyển từ Entity → Model
//fun com.workhubui.data.local.entity.ChatMessageEntity.toModel(): ChatMessage =
//    ChatMessage(sender, receiver, content, timestamp)
package com.workhubui.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ChatMessage model đại diện cho một tin nhắn trong ứng dụng.
// Nó cũng được đánh dấu là @Entity để sử dụng với Room Database.
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Khóa chính tự động tăng cho Room
    val sender: String, // Người gửi tin nhắn
    val receiver: String, // Người nhận tin nhắn
    val content: String, // Nội dung tin nhắn
    val timestamp: Long // Thời gian gửi tin nhắn (dạng timestamp)
)

// Extension function để chuyển đổi từ ChatMessageEntity (Room) sang ChatMessage (Model)
// (Bạn đã có sẵn trong ChatRepository, nhưng tôi sẽ định nghĩa lại ở đây để đảm bảo tính nhất quán)
// Lưu ý: Nếu bạn có ChatMessageEntity riêng, hãy đảm bảo mapping đúng.
// Hiện tại tôi sẽ dùng ChatMessage làm cả Entity và Model để đơn giản hóa.
// Nếu bạn muốn tách biệt, hãy tạo một ChatMessageEntity riêng và mapping giữa chúng.