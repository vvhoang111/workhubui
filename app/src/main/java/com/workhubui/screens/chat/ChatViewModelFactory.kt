//package com.workhubui.screens.chat
//
//import android.app.Application
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.workhubui.data.local.AppDatabase
//import com.workhubui.data.repository.ChatRepository
//
//class ChatViewModelFactory(
//    private val application: Application,
//    private val currentUser: String, // Thêm currentUser
//    private val chatWith: String
//) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
//            val chatMessageDao = AppDatabase.getInstance(application).chatMessageDao()
//            val chatRepository = ChatRepository(chatMessageDao)
//            @Suppress("UNCHECKED_CAST")
//            return ChatViewModel(chatRepository, currentUser, chatWith) as T // Truyền currentUser và chatWith
//        }
//        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
//    }
//}
package com.workhubui.screens.chat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.workhubui.data.local.AppDatabase
import com.workhubui.data.repository.ChatRepository

class ChatViewModelFactory(
    private val application: Application,
    private val currentUser: String, // Thêm currentUser
    private val chatWith: String // Thêm chatWith
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val chatMessageDao = AppDatabase.getInstance(application).chatMessageDao()
            val chatRepository = ChatRepository(chatMessageDao)
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepository, currentUser, chatWith) as T // Truyền đầy đủ tham số
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}