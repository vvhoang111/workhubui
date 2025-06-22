package com.workhubui.screens.chat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.workhubui.data.local.AppDatabase
import com.workhubui.data.remote.FirebaseRepository
import com.workhubui.data.repository.ChatRepository
import com.workhubui.security.CryptoManager

// << THAY ĐỔI: Thêm các tham số mới là ID của người dùng >>
class ChatViewModelFactory(
    private val application: Application,
    private val currentUserUid: String,
    private val friendUid: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            // Khởi tạo các repository và manager cần thiết
            val chatMessageDao = AppDatabase.getInstance(application).chatMessageDao()
            val firebaseRepository = FirebaseRepository()
            val chatRepository = ChatRepository(chatMessageDao, firebaseRepository)
            val cryptoManager = CryptoManager()

            @Suppress("UNCHECKED_CAST")
            // Cung cấp đầy đủ các dependency cho ChatViewModel
            return ChatViewModel(
                application,
                chatRepository,
                firebaseRepository,
                cryptoManager,
                currentUserUid,
                friendUid
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}