package com.workhubui.data.repository

import com.workhubui.data.local.dao.UserDao
import com.workhubui.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepository(private val dao: UserDao) {

    // Lấy thông tin người dùng theo UID
    suspend fun getUserByUid(uid: String): UserEntity? =
        withContext(Dispatchers.IO) {
            dao.getUserByUid(uid)
        }

    // Lấy thông tin người dùng theo Email
    suspend fun getUserByEmail(email: String): UserEntity? =
        withContext(Dispatchers.IO) {
            dao.getUserByEmail(email)
        }

    // Lấy tất cả người dùng (danh sách bạn bè)
    fun getAllUsers(): Flow<List<UserEntity>> =
        dao.getAllUsers()

    // Chèn hoặc cập nhật thông tin người dùng
    suspend fun insertUser(user: UserEntity) =
        withContext(Dispatchers.IO) {
            dao.insertUser(user)
        }
}