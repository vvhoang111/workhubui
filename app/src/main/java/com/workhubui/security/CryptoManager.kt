// app/src/main/java/com/workhubui/security/CryptoManager.kt
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
            // CẢNH BÁO BẢO MẬT: `setUserAuthenticationRequired(false)` làm giảm bảo mật.
            // Trong môi trường production, NÊN đặt là `true` để yêu cầu xác thực người dùng
            // (ví dụ: vân tay, PIN) khi sử dụng khóa này, đặc biệt cho các khóa nhạy cảm.
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    private fun getCipher() = Cipher.getInstance("AES/CBC/PKCS7Padding")

    // Sử dụng một alias cố định cho đơn giản. Trong ứng dụng thực tế,
    // bạn có thể dùng alias riêng cho từng người dùng hoặc phiên bản khóa.
    private val vaultKeyAlias = "WorkHubVaultKey"

    /**
     * Mã hóa một tệp và lưu nó vào một tệp mới được mã hóa.
     * @param file Tệp gốc không mã hóa.
     * @param application Context của ứng dụng để truy cập cacheDir.
     * @return Tệp đã được mã hóa hoặc null nếu có lỗi.
     */
    fun encryptFile(file: File, application: Application): File? {
        return try {
            val secretKey = getSecretKey(vaultKeyAlias)
            val cipher = getCipher()
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv // Lấy IV sau khi khởi tạo cho mã hóa
            val encryptedFile = File(application.cacheDir, "enc_${file.name}")

            FileOutputStream(encryptedFile).use { fos ->
                // Ghi IV (Initialization Vector) vào đầu tệp
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

    /**
     * Giải mã một tệp đã mã hóa và lưu nó vào một tệp tạm thời không mã hóa.
     * @param encryptedFile Tệp đã mã hóa.
     * @param application Context của ứng dụng để truy cập cacheDir.
     * @return Tệp đã giải mã hoặc null nếu có lỗi.
     */
    fun decryptFile(encryptedFile: File, application: Application): File? {
        return try {
            val secretKey = getSecretKey(vaultKeyAlias)
            val cipher = getCipher()

            FileInputStream(encryptedFile).use { fis ->
                // Đọc IV từ đầu tệp
                val iv = ByteArray(16) // Kích thước khối AES
                fis.read(iv)
                val ivParameterSpec = IvParameterSpec(iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

                // Tạo tệp giải mã tạm thời
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

    /**
     * Xóa tất cả các tệp tạm thời đã giải mã (thường có tiền tố "dec_") khỏi thư mục cache.
     * Nên gọi hàm này khi ứng dụng khởi động hoặc định kỳ để đảm bảo dọn dẹp.
     */
    fun cleanupDecryptedTempFiles(application: Application) {
        application.cacheDir.listFiles { file -> file.name.startsWith("dec_") }?.forEach {
            it.delete()
        }
    }
}