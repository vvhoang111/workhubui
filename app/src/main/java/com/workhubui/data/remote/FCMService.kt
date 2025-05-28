package com.workhubui.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FcmService", "New FCM token: $token")
        // TODO: gửi token lên server để mapping email→token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // TODO: parse message.data hoặc message.notification
        // và insert vào Room thông qua Repository
        Log.d("FcmService", "Received FCM: ${message.data}")
    }
}
