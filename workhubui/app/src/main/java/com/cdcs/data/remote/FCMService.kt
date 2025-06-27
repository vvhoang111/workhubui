package com.cdcs.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.cdcs.MainActivity
import com.cdcs.R
import kotlin.random.Random

class FcmService : FirebaseMessagingService() {

    // Được gọi khi Firebase cấp một token mới cho thiết bị
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FcmService", "New FCM token: $token")
        // Ở đây bạn cần một cơ chế để gửi token này lên server,
        // AuthViewModel đã xử lý việc này sau khi người dùng đăng nhập/đăng ký.
    }

    // Được gọi khi có tin nhắn FCM đến trong lúc ứng dụng đang ở foreground hoặc background
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FcmService", "From: ${message.from}")

        // Xử lý payload dạng data (dữ liệu tùy chỉnh)
        if (message.data.isNotEmpty()) {
            Log.d("FcmService", "Message data payload: " + message.data)

            // Lấy thông tin từ payload để hiển thị thông báo
            val title = message.data["title"] ?: "Tin nhắn mới"
            val body = message.data["body"] ?: "Bạn có một tin nhắn mới."
            val senderId = message.data["senderId"]
            val currentUserUid = message.data["currentUserUid"]

            // Chỉ hiển thị thông báo nếu có đủ thông tin cần thiết
            if (senderId != null && currentUserUid != null) {
                sendNotification(title, body, senderId, currentUserUid)
            }
        }
    }

    private fun sendNotification(title: String, body: String, senderId: String, currentUserUid: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "workhub_chat_channel"

        // Tạo Notification Channel (cần cho Android 8.0 Oreo trở lên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Tạo Intent để mở MainActivity khi người dùng nhấn vào thông báo
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Đính kèm dữ liệu để MainActivity biết cần điều hướng đến màn hình chat nào
            putExtra("route", "Chat/${currentUserUid}/${senderId}")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Xây dựng thông báo
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Thay bằng icon của bạn
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Tự động xóa thông báo khi người dùng nhấn vào
            .setContentIntent(pendingIntent) // Gán PendingIntent

        // Hiển thị thông báo với một ID ngẫu nhiên
        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }
}