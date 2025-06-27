package com.cdcs.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cdcs.data.local.dao.ChatMessageDao
import com.cdcs.data.local.dao.ScheduleDao
import com.cdcs.data.local.dao.UserDao
import com.cdcs.data.local.dao.VaultFileDao
import com.cdcs.data.local.entity.ScheduleItemEntity
import com.cdcs.data.local.entity.UserEntity
import com.cdcs.data.local.entity.VaultFileEntity
import com.cdcs.model.ChatMessage
import net.sqlcipher.database.SupportFactory
import java.nio.charset.StandardCharsets

@Database(
    entities = [
        ChatMessage::class,
        ScheduleItemEntity::class,
        UserEntity::class,
        VaultFileEntity::class
    ],
    version = 7, // Tăng version lên để kích hoạt migration
    exportSchema = false // Tắt export schema cho đơn giản
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun userDao(): UserDao
    abstract fun vaultFileDao(): VaultFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphraseString = "12345"
                val passphrase: ByteArray = passphraseString.toByteArray(StandardCharsets.UTF_8)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workhubui_encrypted.db"
                )
                    .openHelperFactory(factory)
                    // << SỬA LỖI: Cách đơn giản nhất để áp dụng thay đổi cấu trúc DB >>
                    // Sẽ xóa và tạo lại CSDL nếu version thay đổi.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
