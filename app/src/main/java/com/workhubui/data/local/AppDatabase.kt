//package com.workhubui.data.local
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import androidx.room.migration.Migration
//import androidx.sqlite.db.SupportSQLiteDatabase
//import com.workhubui.data.local.dao.ChatMessageDao
//import com.workhubui.data.local.dao.ScheduleDao
//import com.workhubui.data.local.dao.UserDao
//import com.workhubui.data.local.entity.ChatMessageEntity
//import com.workhubui.data.local.entity.ScheduleItemEntity
//import com.workhubui.data.local.entity.UserEntity // << Đảm bảo import này
//import net.sqlcipher.database.SQLiteDatabase
//import net.sqlcipher.database.SupportFactory
//import java.nio.charset.StandardCharsets
//
//@Database(
//    entities = [
//        ChatMessageEntity::class,
//        ScheduleItemEntity::class,
//        UserEntity::class // << Đảm bảo UserEntity có ở đây
//    ],
//    version = 2, // << Đảm bảo version đã được tăng
//    exportSchema = true // Nên là true và cấu hình schemaLocation
//)
//abstract class AppDatabase : RoomDatabase() {
//
//    abstract fun chatMessageDao(): ChatMessageDao
//    abstract fun scheduleDao(): ScheduleDao
//    abstract fun userDao(): UserDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                // Room sẽ tự động tạo bảng 'users' dựa trên UserEntity.
//                // Nếu bạn muốn chắc chắn, có thể thêm:
//                // db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`uid` TEXT NOT NULL, `email` TEXT, `displayName` TEXT, `photoUrl` TEXT, PRIMARY KEY(`uid`))")
//            }
//        }
//
//        fun getInstance(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val passphraseString = "your-very-secure-and-randomly-generated-passphrase-for-workhub"
//                val passphrase: ByteArray = passphraseString.toByteArray(StandardCharsets.UTF_8)
//                val factory = SupportFactory(passphrase)
//
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AppDatabase::class.java,
//                    "workhubui_encrypted.db"
//                )
//                    .openHelperFactory(factory)
//                    .addMigrations(MIGRATION_1_2) // Sử dụng migration
//                    // .fallbackToDestructiveMigration() // Chỉ dùng khi thực sự muốn xóa dữ liệu cũ khi nâng cấp
//                    .build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
//}
package com.workhubui.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.workhubui.data.local.dao.ChatMessageDao
import com.workhubui.data.local.dao.ScheduleDao
import com.workhubui.data.local.dao.UserDao
import com.workhubui.model.ChatMessage // Sử dụng ChatMessage làm Entity
import com.workhubui.data.local.entity.ScheduleItemEntity
import com.workhubui.data.local.entity.UserEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.nio.charset.StandardCharsets

// Định nghĩa cơ sở dữ liệu Room.
// entities: Danh sách các Entity (bảng) trong database.
// version: Phiên bản database (cần tăng khi có thay đổi schema).
// exportSchema: Nên là true và cấu hình schemaLocation trong build.gradle để xuất schema.
@Database(
    entities = [
        ChatMessage::class, // ChatMessage giờ là Entity
        ScheduleItemEntity::class,
        UserEntity::class // Thêm UserEntity vào danh sách entities
    ],
    version = 3, // Tăng version database vì đã thêm UserEntity và có thể thay đổi ScheduleItemEntity
    exportSchema = true // Nên là true để kiểm soát migration
)
abstract class AppDatabase : RoomDatabase() {

    // Khai báo các DAO (Data Access Objects)
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun userDao(): UserDao // Thêm DAO cho UserEntity

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Định nghĩa các Migration cho database.
        // MIGRATION_1_2: Nếu bạn có database version 1 và nâng cấp lên 2 (thêm ScheduleItemEntity).
        // MIGRATION_2_3: Nếu bạn có database version 2 và nâng cấp lên 3 (thêm UserEntity).
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Nếu ScheduleItemEntity được thêm vào ở version 2, Room sẽ tự tạo bảng
                // nếu nó chưa tồn tại và được khai báo trong @Database.
                // Nếu có thay đổi cấu trúc bảng cũ, cần viết SQL ALTER TABLE ở đây.
                // Ví dụ: db.execSQL("ALTER TABLE `schedule` ADD COLUMN `detail` TEXT")
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Room sẽ tự động tạo bảng 'users' dựa trên UserEntity
                // nếu nó chưa tồn tại và được khai báo trong @Database.
                // Nếu bạn muốn chắc chắn, có thể thêm câu lệnh SQL CREATE TABLE ở đây:
                db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`uid` TEXT NOT NULL, `email` TEXT, `displayName` TEXT, `photoUrl` TEXT, PRIMARY KEY(`uid`))")
            }
        }

        // Lấy instance singleton của AppDatabase với SQLCipher hỗ trợ mã hóa
        @JvmStatic // Đảm bảo phương thức này có thể được gọi tĩnh từ Java
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Passphrase để mã hóa DB. RẤT QUAN TRỌNG: Không hardcode trong production!
                // Nên lấy từ một nguồn an toàn hơn (ví dụ: Android Keystore)
                val passphraseString = "your-very-secure-and-randomly-generated-passphrase-for-workhub-2025"
                val passphrase: ByteArray = passphraseString.toByteArray(StandardCharsets.UTF_8)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workhubui_encrypted.db" // Tên file database mã hóa
                )
                    .openHelperFactory(factory) // Bật mã hóa SQLCipher
                    // Thêm tất cả các migration theo thứ tự
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // .fallbackToDestructiveMigration() // CHỈ DÙNG KHI THỰC SỰ MUỐN XÓA DỮ LIỆU CŨ KHI NÂNG CẤP VÀ KHÔNG CÓ MIGRATION
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}