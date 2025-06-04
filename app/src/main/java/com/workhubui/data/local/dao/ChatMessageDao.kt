//package com.workhubui.data.local.dao
//
//import androidx.room.Dao
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import com.workhubui.data.local.entity.ChatMessageEntity
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface ChatMessageDao {
//    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 3")
//    suspend fun getRecentMessages(): List<ChatMessageEntity>
//
//    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
//    fun getAllMessages(): Flow<List<ChatMessageEntity>>
//
//    @Query("SELECT * FROM chat_messages WHERE (sender = :user1 AND receiver = :user2) OR (sender = :user2 AND receiver = :user1) ORDER BY timestamp ASC") //
//    fun getMessagesBetweenUsers(user1: String, user2: String): Flow<List<ChatMessageEntity>> //
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertMessage(message: ChatMessageEntity)
//}
package com.workhubui.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.workhubui.model.ChatMessage // Sử dụng ChatMessage làm Entity

@Dao
interface ChatMessageDao {
    // Lấy 3 tin nhắn gần nhất (có thể dùng cho Recent Chats)
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 3")
    suspend fun getRecentMessages(): List<ChatMessage>

    // Lấy tất cả tin nhắn (có thể dùng để lọc theo cuộc trò chuyện)
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    // Lấy tin nhắn giữa hai người dùng cụ thể
    // Sắp xếp theo timestamp để đảm bảo thứ tự
    @Query("SELECT * FROM chat_messages WHERE (sender = :user1 AND receiver = :user2) OR (sender = :user2 AND receiver = :user1) ORDER BY timestamp ASC")
    fun getMessagesBetweenUsers(user1: String, user2: String): Flow<List<ChatMessage>>

    // Chèn tin nhắn mới vào database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)
}