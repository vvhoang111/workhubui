package com.workhubui.data.repository

import com.google.firebase.firestore.ListenerRegistration
import com.workhubui.data.local.dao.ChatMessageDao
import com.workhubui.data.remote.FirebaseRepository
import com.workhubui.model.ChatMessage
import com.workhubui.model.FirestoreChatMessage
import com.workhubui.model.ChatRoomMetadata // Thêm import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class ChatRepository(
    private val dao: ChatMessageDao,
    private val firebaseRepository: FirebaseRepository
) {

    suspend fun getRecentMessages(): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            dao.getRecentMessages()
        }

    fun getMessagesBetweenUsers(user1: String, user2: String): Flow<List<ChatMessage>> =
        dao.getMessagesBetweenUsers(user1, user2)

    // << THAY ĐỔI: Thêm tham số lastMessageContent >>
    suspend fun sendMessage(chatRoomId: String, firestoreMessage: FirestoreChatMessage, lastMessageContent: String) {
        // Gửi tin nhắn và cập nhật metadata phòng chat
        firebaseRepository.sendMessageToFirestore(chatRoomId, firestoreMessage, lastMessageContent)
    }

    fun getRealtimeMessages(chatRoomId: String): Flow<List<FirestoreChatMessage>> {
        return callbackFlow {
            var listener: ListenerRegistration? = null
            try {
                listener = firebaseRepository.getChatMessagesListener(chatRoomId) { messages ->
                    trySend(messages).isSuccess
                }
            } catch (e: Exception) {
                close(e)
            }
            awaitClose {
                listener?.remove()
            }
        }
    }

    // << THÊM MỚI: Lắng nghe danh sách phòng chat từ Firestore >>
    fun listenToChatRooms(userUid: String): Flow<List<ChatRoomMetadata>> {
        return callbackFlow {
            var listener: ListenerRegistration? = null
            try {
                listener = firebaseRepository.getChatRoomsListener(userUid) { rooms ->
                    trySend(rooms).isSuccess
                }
            } catch (e: Exception) {
                close(e)
            }
            awaitClose {
                listener?.remove()
            }
        }
    }

    suspend fun clearChatMessages() {
        withContext(Dispatchers.IO) {
            dao.clearAllMessages()
        }
    }
}
