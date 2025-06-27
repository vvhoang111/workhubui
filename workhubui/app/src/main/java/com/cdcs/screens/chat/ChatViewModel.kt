package com.cdcs.screens.chat

import android.app.Application
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdcs.data.remote.FirebaseRepository
import com.cdcs.data.repository.ChatRepository
import com.cdcs.model.ChatMessage
import com.cdcs.model.FirestoreChatMessage
import com.cdcs.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

class ChatViewModel(
    application: Application,
    private val chatRepository: ChatRepository,
    private val firebaseRepository: FirebaseRepository,
    private val cryptoManager: CryptoManager,
    private val currentUserUid: String,
    private val friendUid: String
) : AndroidViewModel(application) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _friendPublicKey = MutableStateFlow<PublicKey?>(null)
    private val _chatRoomId = MutableStateFlow("")

    init {
        _chatRoomId.value = if (currentUserUid > friendUid) {
            "$currentUserUid-$friendUid"
        } else {
            "$friendUid-$currentUserUid"
        }

        viewModelScope.launch {
            loadFriendPublicKey()
            listenForRealtimeMessages()
        }
    }

    private suspend fun loadFriendPublicKey() {
        val keyString = firebaseRepository.getFriendPublicKey(friendUid)
        if (keyString != null) {
            try {
                val decodedKey = Base64.decode(keyString, Base64.DEFAULT)
                val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
                val keySpec = X509EncodedKeySpec(decodedKey)
                _friendPublicKey.value = keyFactory.generatePublic(keySpec)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to decode public key", e)
            }
        }
    }

    private suspend fun listenForRealtimeMessages() {
        chatRepository.getRealtimeMessages(_chatRoomId.value)
            .catch { e -> Log.e("ChatViewModel", "Error listening to messages", e) }
            .collect { firestoreMessages ->
                val decryptedMessages = firestoreMessages.mapNotNull { decryptMessage(it) }
                _messages.value = decryptedMessages
            }
    }

    fun sendMessage(plainTextContent: String) {
        val friendPubKey = _friendPublicKey.value ?: return
        if (plainTextContent.isBlank()) return

        viewModelScope.launch {
            try {
                val sessionKey = cryptoManager.generateAesSessionKey()
                val (iv, encryptedContent) = cryptoManager.encryptWithAesKey(plainTextContent.toByteArray(), sessionKey)

                val currentUserKeyPair = cryptoManager.getOrCreateUserKeyPair(currentUserUid)
                val encryptedSessionKeyForSender = cryptoManager.encryptWithRsaPublicKey(sessionKey.encoded, currentUserKeyPair.public)

                val encryptedSessionKeyForReceiver = cryptoManager.encryptWithRsaPublicKey(sessionKey.encoded, friendPubKey)

                val firestoreMessage = FirestoreChatMessage(
                    messageId = UUID.randomUUID().toString(),
                    senderId = currentUserUid,
                    receiverId = friendUid,
                    encryptedContent = Base64.encodeToString(encryptedContent, Base64.DEFAULT),
                    contentIv = Base64.encodeToString(iv, Base64.DEFAULT),
                    encryptedSessionKeyForSender = Base64.encodeToString(encryptedSessionKeyForSender, Base64.DEFAULT),
                    encryptedSessionKeyForReceiver = Base64.encodeToString(encryptedSessionKeyForReceiver, Base64.DEFAULT)
                    // timestamp sẽ là null, để server tự điền
                )

                chatRepository.sendMessage(_chatRoomId.value, firestoreMessage, plainTextContent)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to encrypt and send message", e)
            }
        }
    }

    private fun decryptMessage(msg: FirestoreChatMessage): ChatMessage? {
        try {
            val encryptedSessionKeyString = if (msg.senderId == currentUserUid) {
                msg.encryptedSessionKeyForSender
            } else {
                msg.encryptedSessionKeyForReceiver
            }

            val encryptedSessionKey = Base64.decode(encryptedSessionKeyString, Base64.DEFAULT)

            val decryptedSessionKeyBytes = cryptoManager.decryptWithRsaPrivateKey(encryptedSessionKey, currentUserUid)

            if (decryptedSessionKeyBytes == null) {
                Log.w("ChatViewModel", "Không thể giải mã khóa session cho tin nhắn ${msg.messageId}")
                return ChatMessage(
                    id = msg.messageId.hashCode().toLong(), sender = msg.senderId, receiver = msg.receiverId,
                    content = "[Tin nhắn không thể giải mã (khóa không hợp lệ)]",
                    timestamp = msg.timestamp?.time ?: System.currentTimeMillis()
                )
            }

            val sessionKey = SecretKeySpec(decryptedSessionKeyBytes, "AES")

            val iv = Base64.decode(msg.contentIv, Base64.DEFAULT)
            val encryptedContent = Base64.decode(msg.encryptedContent, Base64.DEFAULT)
            val decryptedContentBytes = cryptoManager.decryptWithAesKey(encryptedContent, iv, sessionKey)
            val plainTextContent = String(decryptedContentBytes)

            return ChatMessage(
                id = msg.messageId.hashCode().toLong(),
                sender = msg.senderId,
                receiver = msg.receiverId,
                content = plainTextContent,
                timestamp = msg.timestamp?.time ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to decrypt message ${msg.messageId}", e)
            return ChatMessage(
                id = msg.messageId.hashCode().toLong(), sender = msg.senderId, receiver = msg.receiverId,
                content = "[Tin nhắn không thể giải mã]",
                timestamp = msg.timestamp?.time ?: System.currentTimeMillis()
            )
        }
    }
}
