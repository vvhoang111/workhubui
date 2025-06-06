//package com.workhubui.data.local.dao
//
//import androidx.room.Dao
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import com.workhubui.data.local.entity.UserEntity
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface UserDao {
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertUser(user: UserEntity)
//
//    @Query("SELECT * FROM users WHERE uid = :uid")
//    suspend fun getUserByUid(uid: String): UserEntity? // Room sẽ tự chuyển đổi Cursor sang UserEntity
//
//    @Query("SELECT * FROM users WHERE email = :email")
//    suspend fun getUserByEmail(email: String): UserEntity? // Room sẽ tự chuyển đổi Cursor sang UserEntity
//
//    @Query("SELECT * FROM users")
//    fun getAllUsers(): Flow<List<UserEntity>> // Room sẽ tự chuyển đổi Cursor sang Flow<List<UserEntity>>
//}
package com.workhubui.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.workhubui.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // Chèn hoặc cập nhật thông tin người dùng
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Lấy thông tin người dùng theo UID
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserByUid(uid: String): UserEntity?

    // Lấy thông tin người dùng theo email
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    // Lấy tất cả người dùng (có thể dùng cho danh sách bạn bè)
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
}