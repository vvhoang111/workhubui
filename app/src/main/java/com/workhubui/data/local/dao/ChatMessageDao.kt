package com.workhubui.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.workhubui.model.ChatMessage // Sử dụng ChatMessage làm Entity
import kotlinx.coroutines.flow.Flow // << THÊM IMPORT NÀY

@Dao
interface ChatMessageDao {
    /**
     * Lấy 3 tin nhắn gần nhất.
     * @return Danh sách các ChatMessage gần nhất.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 3")
    suspend fun getRecentMessages(): List<ChatMessage>

    /**
     * Lấy tất cả tin nhắn, sắp xếp theo thời gian.
     * Room sẽ tự động cập nhật Flow này khi có thay đổi trong bảng chat_messages.
     * @return Flow chứa danh sách tất cả ChatMessage.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    /**
     * Lấy tất cả tin nhắn giữa hai người dùng cụ thể, sắp xếp theo thời gian.
     * Room sẽ tự động cập nhật Flow này khi có thay đổi liên quan đến hai người dùng này.
     * @param user1 Email hoặc ID của người dùng thứ nhất.
     * @param user2 Email hoặc ID của người dùng thứ hai.
     * @return Flow chứa danh sách ChatMessage giữa hai người dùng.
     */
    @Query("SELECT * FROM chat_messages WHERE (sender = :user1 AND receiver = :user2) OR (sender = :user2 AND receiver = :user1) ORDER BY timestamp ASC")
    fun getMessagesBetweenUsers(user1: String, user2: String): Flow<List<ChatMessage>>

    /**
     * Chèn một tin nhắn mới vào cơ sở dữ liệu.
     * Nếu tin nhắn đã tồn tại (dựa trên khóa chính), nó sẽ được thay thế.
     * @param message Đối tượng ChatMessage cần chèn.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)
}
