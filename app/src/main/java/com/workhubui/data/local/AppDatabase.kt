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
import com.workhubui.data.local.dao.VaultFileDao // Import VaultFileDao
import com.workhubui.model.ChatMessage
import com.workhubui.data.local.entity.ScheduleItemEntity
import com.workhubui.data.local.entity.UserEntity
import com.workhubui.data.local.entity.VaultFileEntity // Import VaultFileEntity
import net.sqlcipher.database.SupportFactory // SQLCipher support
import java.nio.charset.StandardCharsets

@Database(
    entities = [
        ChatMessage::class,
        ScheduleItemEntity::class,
        UserEntity::class,
        VaultFileEntity::class // Added VaultFileEntity
    ],
    version = 4, // Incremented version for the new table
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun userDao(): UserDao
    abstract fun vaultFileDao(): VaultFileDao // Added DAO for VaultFile

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from 1 to 2 (if you had it before for ScheduleItemEntity)
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQL for migrating from version 1 to 2
                // e.g., db.execSQL("ALTER TABLE `schedule` ADD COLUMN `detail` TEXT")
            }
        }

        // Migration from 2 to 3 (for UserEntity)
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`uid` TEXT NOT NULL, `email` TEXT, `displayName` TEXT, `photoUrl` TEXT, PRIMARY KEY(`uid`))")
            }
        }

        // Migration from 3 to 4 (for VaultFileEntity)
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `vault_files` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`fileName` TEXT NOT NULL, " +
                            "`filePath` TEXT NOT NULL, " +
                            "`originalSize` INTEGER NOT NULL, " +
                            "`encryptedSize` INTEGER NOT NULL, " +
                            "`uploadDate` INTEGER NOT NULL" +
                            ")"
                )
            }
        }

        @JvmStatic
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphraseString = "your-very-secure-and-randomly-generated-passphrase-for-workhub-2025"
                val passphrase: ByteArray = passphraseString.toByteArray(StandardCharsets.UTF_8)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workhubui_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // Added new migration
                    // .fallbackToDestructiveMigration() // Avoid if possible, use migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
