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
import com.workhubui.data.local.entity.ChatMessageEntity
import com.workhubui.data.local.entity.ScheduleItemEntity
import com.workhubui.data.local.entity.UserEntity // << Đảm bảo import này
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.nio.charset.StandardCharsets

@Database(
    entities = [
        ChatMessageEntity::class,
        ScheduleItemEntity::class,
        UserEntity::class // << Đảm bảo UserEntity có ở đây
    ],
    version = 2, // << Đảm bảo version đã được tăng
    exportSchema = true // Nên là true và cấu hình schemaLocation
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Room sẽ tự động tạo bảng 'users' dựa trên UserEntity.
                // Nếu bạn muốn chắc chắn, có thể thêm:
                // db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`uid` TEXT NOT NULL, `email` TEXT, `displayName` TEXT, `photoUrl` TEXT, PRIMARY KEY(`uid`))")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphraseString = "your-very-secure-and-randomly-generated-passphrase-for-workhub"
                val passphrase: ByteArray = passphraseString.toByteArray(StandardCharsets.UTF_8)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workhubui_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2) // Sử dụng migration
                    // .fallbackToDestructiveMigration() // Chỉ dùng khi thực sự muốn xóa dữ liệu cũ khi nâng cấp
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}