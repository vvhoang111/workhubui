package com.cdcs.data.remote

import android.util.Log
import com.cdcs.data.local.entity.UserEntity
import com.cdcs.model.ChatRoomMetadata
import com.cdcs.model.FirestoreChatMessage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val firestoreDb = FirebaseFirestore.getInstance()
    private val usersCollection = firestoreDb.collection("users")
    private val chatsCollection = firestoreDb.collection("chats")
    private val chatRoomsCollection = firestoreDb.collection("chat_rooms")
    private val TAG = "FirebaseRepoDebug"

    suspend fun saveUserToFirestore(user: UserEntity) {
        try {
            usersCollection.document(user.uid).set(user, SetOptions.merge()).await()
            Log.d(TAG, "User ${user.email ?: user.uid} saved/updated in Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user to Firestore", e)
            throw e
        }
    }

    fun getChatRoomsListener(userUid: String, onUpdate: (List<ChatRoomMetadata>) -> Unit): ListenerRegistration {
        return chatRoomsCollection.whereArrayContains("participants", userUid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "ChatRooms listen failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    onUpdate(snapshot.toObjects(ChatRoomMetadata::class.java))
                }
            }
    }

    fun getChatMessagesListener(chatRoomId: String, onNewMessages: (List<FirestoreChatMessage>) -> Unit): ListenerRegistration {
        return chatsCollection.document(chatRoomId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    onNewMessages(snapshot.toObjects(FirestoreChatMessage::class.java))
                }
            }
    }

    suspend fun establishFriendshipAndCreateChatRoom(currentUserUid: String, friendUid: String) {
        val chatRoomId = if (currentUserUid > friendUid) "$currentUserUid-$friendUid" else "$friendUid-$currentUserUid"
        val chatRoomRef = chatRoomsCollection.document(chatRoomId)
        val userDocRef = usersCollection.document(currentUserUid)
        val friendDocRef = usersCollection.document(friendUid)

        try {
            firestoreDb.runBatch { batch ->
                batch.update(userDocRef, "friends", FieldValue.arrayUnion(friendUid))
                batch.update(friendDocRef, "friends", FieldValue.arrayUnion(currentUserUid))

                val metadata = ChatRoomMetadata(
                    participants = listOf(currentUserUid, friendUid),
                    lastMessage = "Cuộc trò chuyện đã bắt đầu!",
                    lastMessageSenderId = "",
                    lastMessageTimestamp = null
                )
                batch.set(chatRoomRef, metadata, SetOptions.merge())
            }.await()
            Log.d(TAG, "Friendship and chat room established for $chatRoomId")
        } catch (e: Exception) {
            Log.e(TAG, "Error establishing friendship", e)
            throw e
        }
    }

    suspend fun sendMessageToFirestore(chatRoomId: String, message: FirestoreChatMessage, lastMessageContent: String) {
        try {
            firestoreDb.runBatch { batch ->
                val messageRef = chatsCollection.document(chatRoomId).collection("messages").document(message.messageId)
                batch.set(messageRef, message)

                val chatRoomRef = chatRoomsCollection.document(chatRoomId)
                val metadataUpdate = mapOf(
                    "lastMessage" to lastMessageContent,
                    "lastMessageSenderId" to message.senderId,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp()
                )
                batch.set(chatRoomRef, metadataUpdate, SetOptions.merge())
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message and updating metadata", e)
            throw e
        }
    }

    suspend fun getUserProfile(uid: String): UserEntity? {
        return try {
            usersCollection.document(uid).get().await().toObject(UserEntity::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile for $uid", e)
            null
        }
    }

    // **HÀM CÒN THIẾU ĐƯỢC BỔ SUNG Ở ĐÂY**
    suspend fun getUserProfiles(uids: List<String>): List<UserEntity> {
        if (uids.isEmpty()) return emptyList()
        return try {
            usersCollection.whereIn("uid", uids).get().await().toObjects(UserEntity::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profiles", e)
            emptyList()
        }
    }

    suspend fun getUserByEmailFromFirestore(email: String): UserEntity? {
        return try {
            val querySnapshot = usersCollection.whereEqualTo("email", email).get().await()
            if (querySnapshot.isEmpty) { return null }
            querySnapshot.documents.firstOrNull()?.toObject(UserEntity::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by email", e)
            null
        }
    }

    suspend fun getFriendPublicKey(friendUid: String): String? {
        return try {
            usersCollection.document(friendUid).get().await().getString("publicKey")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting public key for $friendUid", e)
            null
        }
    }
}