package com.cdcs.data.repository

import com.cdcs.data.local.dao.UserDao
import com.cdcs.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepository(private val dao: UserDao) {

    suspend fun getUserByUid(uid: String): UserEntity? =
        withContext(Dispatchers.IO) {
            dao.getUserByUid(uid)
        }

    suspend fun getUserByEmail(email: String): UserEntity? =
        withContext(Dispatchers.IO) {
            dao.getUserByEmail(email)
        }

    fun getAllUsers(): Flow<List<UserEntity>> =
        dao.getAllUsers()

    suspend fun insertUser(user: UserEntity) =
        withContext(Dispatchers.IO) {
            dao.insertUser(user)
        }

    // << THÊM MỚI >>
    suspend fun insertUsers(users: List<UserEntity>) =
        withContext(Dispatchers.IO) {
            dao.insertUsers(users)
        }

    // << THÊM MỚI >>
    suspend fun clearUsers() =
        withContext(Dispatchers.IO) {
            dao.clearAllUsers()
        }

    suspend fun getUsers(uids: List<String>): List<UserEntity> {
        // Logic để lấy danh sách user từ Room dựa trên list uids
        // Đây là ví dụ, bạn cần implement query tương ứng trong UserDao
        return uids.mapNotNull { dao.getUserByUid(it) }
    }
}
