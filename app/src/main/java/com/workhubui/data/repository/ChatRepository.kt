package com.workhubui.data.repository

import com.workhubui.data.local.dao.ChatMessageDao
import com.workhubui.data.local.entity.ChatMessageEntity
import com.workhubui.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepository(private val dao: ChatMessageDao) {

    suspend fun getRecentMessages(): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            dao.getRecentMessages().map { it.toModel() }
        }

    // Giữ lại hàm getAllMessages nếu bạn có nhu cầu lấy tất cả tin nhắn ở đâu đó
    fun getAllMessages(): Flow<List<ChatMessage>> =
        dao.getAllMessages().map { list ->
            list.map { it.toModel() }
        }

    // Hàm mới để lấy tin nhắn giữa hai người dùng cụ thể
    fun getMessagesBetweenUsers(user1: String, user2: String): Flow<List<ChatMessage>> =
        dao.getMessagesBetweenUsers(user1, user2).map { list ->
            list.map { it.toModel() }
        }

    suspend fun sendMessage(chat: ChatMessage) =
        withContext(Dispatchers.IO) {
            dao.insertMessage(chat.toEntity())
            // TODO: gửi FCM tại đây (sau khi config FCMService) [cite: 19]
        }
}

// Extension mapping (giữ nguyên từ code gốc của bạn)
private fun ChatMessageEntity.toModel(): ChatMessage =
    ChatMessage(sender, receiver, content, timestamp)

private fun ChatMessage.toEntity(): ChatMessageEntity =
    ChatMessageEntity(sender = sender, receiver = receiver, content = content, timestamp = timestamp)