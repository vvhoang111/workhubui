package com.workhubui.data.repository

import com.workhubui.data.local.dao.VaultFileDao
import com.workhubui.data.local.entity.VaultFileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.workhubui.screens.vault.VaultFile // Assuming VaultFile model is in screens.vault
import com.workhubui.screens.vault.toModel // Assuming toModel extension is in screens.vault

/**
 * Repository for managing VaultFile data.
 * It abstracts data operations between ViewModels and data sources (local Room DB).
 * @param vaultFileDao Data Access Object for VaultFileEntity.
 */
class VaultRepository(private val vaultFileDao: VaultFileDao) {

    /**
     * Retrieves all vault files from the local database as a Flow of VaultFileEntity.
     * @return Flow<List<VaultFileEntity>> A flow emitting a list of vault file entities.
     */
    fun getAllFileEntities(): Flow<List<VaultFileEntity>> = vaultFileDao.getAllVaultFiles()

    /**
     * Retrieves all vault files and maps them to the VaultFile model.
     * This is useful if the ViewModel prefers to work with model objects.
     * @return Flow<List<VaultFile>> A flow emitting a list of vault file models.
     */
    fun getAllFilesAsModels(): Flow<List<VaultFile>> {
        return vaultFileDao.getAllVaultFiles().map { entities ->
            entities.map { it.toModel() } // Convert each entity to model
        }
    }

    /**
     * Inserts a new vault file entity into the database.
     * @param fileEntity The VaultFileEntity to insert.
     * @return The row ID of the newly inserted file.
     */
    suspend fun insertFile(fileEntity: VaultFileEntity): Long {
        return vaultFileDao.insertVaultFile(fileEntity)
    }

    /**
     * Retrieves a specific vault file entity by its ID.
     * @param id The ID of the vault file.
     * @return The VaultFileEntity if found, otherwise null.
     */
    suspend fun getFileById(id: Long): VaultFileEntity? {
        return vaultFileDao.getVaultFileById(id)
    }

    /**
     * Deletes a specific vault file entity by its ID.
     * @param id The ID of the vault file to delete.
     */
    suspend fun deleteFileById(id: Long) {
        vaultFileDao.deleteVaultFileById(id)
    }

    /**
     * Clears all vault file entities from the database.
     */
    suspend fun clearAllFiles() {
        vaultFileDao.clearAllVaultFiles()
    }
}
