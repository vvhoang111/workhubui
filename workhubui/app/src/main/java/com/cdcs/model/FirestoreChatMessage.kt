package com.cdcs.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Model này đại diện cho đối tượng tin nhắn được lưu trữ trên Cloud Firestore.
// Nó chứa dữ liệu đã được mã hóa.
data class FirestoreChatMessage(
    // Dùng @ServerTimestamp để Firebase tự điền thời gian của server, đảm bảo đồng nhất
    @ServerTimestamp
    val timestamp: Date? = null,

    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",

    // Nội dung tin nhắn đã được mã hóa bằng AES, lưu dưới dạng chuỗi Base64
    val encryptedContent: String = "",

    // IV (Initialization Vector) của mã hóa AES, dưới dạng chuỗi Base64
    val contentIv: String = "",

    // Khóa session (AES) đã được mã hóa bằng Public Key của người gửi (để họ tự đọc lại)
    val encryptedSessionKeyForSender: String = "",

    // Khóa session (AES) đã được mã hóa bằng Public Key của người nhận
    val encryptedSessionKeyForReceiver: String = ""
)