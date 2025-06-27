package com.cdcs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cdcs.data.local.entity.VaultFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files ORDER BY uploadDate DESC")
    fun getAllVaultFiles(): Flow<List<VaultFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultFile(file: VaultFileEntity): Long // Return Long for the inserted ID

    @Query("SELECT * FROM vault_files WHERE id = :id")
    suspend fun getVaultFileById(id: Long): VaultFileEntity?

    @Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteVaultFileById(id: Long)

    @Query("DELETE FROM vault_files")
    suspend fun clearAllVaultFiles()
}
