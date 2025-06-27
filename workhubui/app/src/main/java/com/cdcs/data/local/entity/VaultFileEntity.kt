package com.cdcs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String, // Path to the encrypted file on local storage
    val originalSize: Long,
    val encryptedSize: Long,
    val uploadDate: Long // Timestamp of upload
)
