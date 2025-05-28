package com.workhubui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workhubui.data.repository.ChatRepository
import com.workhubui.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date // Cần import Date

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val currentUser: String, // Lưu trữ currentUser
    private val chatWithUser: String // Đổi tên để rõ ràng hơn, đây là người mà currentUser đang chat cùng
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    init {
        loadMessages()
    }

    // Trong ChatViewModel.kt, hàm loadMessages() sẽ như sau:
    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getMessagesBetweenUsers(currentUser, chatWithUser).collectLatest {
                _messages.value = it
            }
        }
    }

    fun sendMessage(content: String) { // Bỏ currentUser và chatWith từ tham số vì đã có trong constructor
        if (content.isBlank()) return

        viewModelScope.launch {
            val newMessage = ChatMessage(
                sender = currentUser,
                receiver = chatWithUser,
                content = content.trim(),
                timestamp = Date().time // Sử dụng java.util.Date().time cho timestamp hiện tại
            )
            chatRepository.sendMessage(newMessage)
            // Sau khi gửi, có thể không cần load lại toàn bộ tin nhắn nếu Repository và Flow hoạt động đúng
            // _messages (Flow) sẽ tự động cập nhật nếu insert vào Room trigger nó.
            // TODO: Xử lý việc gửi FCM tại đây nếu cần thiết, sau khi cấu hình FcmService [cite: 19]
        }
    }
}