package com.workhubui.security

import android.app.Application
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

// IMPORTANT: This is a simplified CryptoManager.
// For production, enhance security, error handling, and key management.
class CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private fun getSecretKey(alias: String): SecretKey {
        val existingKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: generateSecretKey(alias)
    }

    private fun generateSecretKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(false) // Consider true for higher security
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    private fun getCipher() = Cipher.getInstance("AES/CBC/PKCS7Padding")

    // Using a fixed alias for simplicity. In a real app, you might use user-specific aliases.
    private val vaultKeyAlias = "WorkHubVaultKey"

    fun encryptFile(file: File, application: Application): File? {
        return try {
            val secretKey = getSecretKey(vaultKeyAlias)
            val cipher = getCipher()
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv // Get the IV after initializing for encryption
            val encryptedFile = File(application.cacheDir, "enc_${file.name}")

            FileOutputStream(encryptedFile).use { fos ->
                // Write IV to the beginning of the file
                fos.write(iv)
                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val encryptedBytes = cipher.update(buffer, 0, bytesRead)
                        encryptedBytes?.let { fos.write(it) }
                    }
                    val finalBytes = cipher.doFinal()
                    finalBytes?.let { fos.write(it) }
                }
            }
            encryptedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decryptFile(encryptedFile: File, application: Application): File? {
        return try {
            val secretKey = getSecretKey(vaultKeyAlias)
            val cipher = getCipher()

            FileInputStream(encryptedFile).use { fis ->
                // Read IV from the beginning of the file
                val iv = ByteArray(16) // AES block size
                fis.read(iv)
                val ivParameterSpec = IvParameterSpec(iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

                val decryptedFile = File(application.cacheDir, "dec_${encryptedFile.name.removePrefix("enc_")}")
                FileOutputStream(decryptedFile).use { fos ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val decryptedBytes = cipher.update(buffer, 0, bytesRead)
                        decryptedBytes?.let { fos.write(it) }
                    }
                    val finalBytes = cipher.doFinal()
                    finalBytes?.let { fos.write(it) }
                }
                decryptedFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
