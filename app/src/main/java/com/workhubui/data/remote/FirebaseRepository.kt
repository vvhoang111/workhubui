package com.workhubui.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.workhubui.data.local.entity.UserEntity
import kotlinx.coroutines.tasks.await
import android.util.Log

// FirebaseRepository để tương tác với Firebase Realtime Database hoặc Firestore.
class FirebaseRepository {

    private val realtimeDb = FirebaseDatabase.getInstance()
    private val firestoreDb = FirebaseFirestore.getInstance()

    // Hàm để lưu thông tin người dùng lên Firebase (ví dụ: Firestore)
    suspend fun saveUserToFirestore(user: UserEntity) {
        try {
            firestoreDb.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            Log.d("FirebaseRepo", "User ${user.email} saved to Firestore successfully.")
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error saving user to Firestore: ${e.message}", e)
            throw e
        }
    }

    // Hàm để lấy thông tin người dùng từ Firestore (ví dụ: khi tìm kiếm bạn bè)
    suspend fun getUserByEmailFromFirestore(email: String): UserEntity? {
        return try {
            val querySnapshot = firestoreDb.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
            querySnapshot.documents.firstOrNull()?.toObject(UserEntity::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error getting user by email from Firestore: ${e.message}", e)
            null
        }
    }

    // TODO: Thêm các hàm khác để tương tác với Realtime DB hoặc Storage nếu cần.
    // Ví dụ: gửi/nhận metadata tin nhắn qua Realtime DB, upload/download file từ Storage.
}