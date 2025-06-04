//package com.workhubui.data.repository
//
//import com.workhubui.data.local.dao.ChatMessageDao
//import com.workhubui.data.local.entity.ChatMessageEntity
//import com.workhubui.model.ChatMessage
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.withContext
//
//class ChatRepository(private val dao: ChatMessageDao) {
//
//    suspend fun getRecentMessages(): List<ChatMessage> =
//        withContext(Dispatchers.IO) {
//            dao.getRecentMessages().map { it.toModel() }
//        }
//
//    // Giữ lại hàm getAllMessages nếu bạn có nhu cầu lấy tất cả tin nhắn ở đâu đó
//    fun getAllMessages(): Flow<List<ChatMessage>> =
//        dao.getAllMessages().map { list ->
//            list.map { it.toModel() }
//        }
//
//    // Hàm mới để lấy tin nhắn giữa hai người dùng cụ thể
//    fun getMessagesBetweenUsers(user1: String, user2: String): Flow<List<ChatMessage>> =
//        dao.getMessagesBetweenUsers(user1, user2).map { list ->
//            list.map { it.toModel() }
//        }
//
//    suspend fun sendMessage(chat: ChatMessage) =
//        withContext(Dispatchers.IO) {
//            dao.insertMessage(chat.toEntity())
//            // TODO: gửi FCM tại đây (sau khi config FCMService) [cite: 19]
//        }
//}
//
//// Extension mapping (giữ nguyên từ code gốc của bạn)
//private fun ChatMessageEntity.toModel(): ChatMessage =
//    ChatMessage(sender, receiver, content, timestamp)
//
//private fun ChatMessage.toEntity(): ChatMessageEntity =
//    ChatMessageEntity(sender = sender, receiver = receiver, content = content, timestamp = timestamp)
package com.workhubui.data.repository

import com.workhubui.data.local.dao.ChatMessageDao
import com.workhubui.model.ChatMessage // Sử dụng ChatMessage làm Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepository(private val dao: ChatMessageDao) {

    // Lấy 3 tin nhắn gần nhất (sử dụng cho Recent Chats)
    // Hàm này không cần map toModel() nữa vì ChatMessage đã là model
    suspend fun getRecentMessages(): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            dao.getRecentMessages()
        }

    // Lấy tất cả tin nhắn (có thể dùng để lọc theo cuộc trò chuyện)
    // Hàm này không cần map toModel() nữa vì ChatMessage đã là model
    fun getAllMessages(): Flow<List<ChatMessage>> =
        dao.getAllMessages()

    // Hàm mới để lấy tin nhắn giữa hai người dùng cụ thể
    // Hàm này không cần map toModel() nữa vì ChatMessage đã là model
    fun getMessagesBetweenUsers(user1: String, user2: String): Flow<List<ChatMessage>> =
        dao.getMessagesBetweenUsers(user1, user2)

    // Gửi tin nhắn và lưu vào database cục bộ
    // Hàm này không cần map toEntity() nữa vì ChatMessage đã là entity
    suspend fun sendMessage(chat: ChatMessage) =
        withContext(Dispatchers.IO) {
            dao.insertMessage(chat)
            // TODO: gửi FCM tại đây (sau khi config FCMService)
            // Firebase chỉ truyền dữ liệu, không lưu trữ nội dung chat.
            // Nội dung chat (text, hình ảnh, video) sẽ được lưu cục bộ.
            // FCM chỉ nên truyền metadata (ai gửi, loại tin nhắn, ID tin nhắn)
            // để người nhận biết có tin nhắn mới và tải về từ Room.
        }
}