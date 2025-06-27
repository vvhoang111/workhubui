package com.cdcs.utils

import com.cdcs.data.local.entity.VaultFileEntity
import com.cdcs.model.VaultFile // Import VaultFile model

/**
 * Converts a [VaultFileEntity] (Room entity) to a [VaultFile] (UI model).
 */
fun VaultFileEntity.toModel(): VaultFile = VaultFile(
    id = this.id,
    fileName = this.fileName,
    filePath = this.filePath,
    originalSize = this.originalSize,
    encryptedSize = this.encryptedSize,
    uploadDate = this.uploadDate
)

/**
 * Converts a [VaultFile] (UI model) to a [VaultFileEntity] (Room entity).
 */
fun VaultFile.toEntity(): VaultFileEntity = VaultFileEntity(
    id = this.id, // Pass ID for updates, Room handles 0 for inserts
    fileName = this.fileName,
    filePath = this.filePath,
    originalSize = this.originalSize,
    encryptedSize = this.encryptedSize,
    uploadDate = this.uploadDate
)
