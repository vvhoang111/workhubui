package com.workhubui.model

/**
 * Represents a file displayed in the Vault UI.
 * This is a UI-specific model, distinct from the Room Entity.
 */
data class VaultFile(
    val id: Long = 0L, // Default for new files, Room will auto-generate
    val fileName: String,
    val filePath: String, // Local path of the encrypted file
    val originalSize: Long,
    val encryptedSize: Long,
    val uploadDate: Long // Timestamp of upload
)
