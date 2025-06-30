package com.cdcs.security

import android.app.Application
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CryptoManager {

    private val androidKeyStoreProvider = "AndroidKeyStore"
    private val rsaTransformation = "RSA/ECB/PKCS1Padding"
    private val aesTransformation = "AES/CBC/PKCS7Padding"

    private val keyStore = KeyStore.getInstance(androidKeyStoreProvider).apply {
        load(null)
    }

    private fun getRsaKeyPair(alias: String): KeyPair? {
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        return entry?.let { KeyPair(it.certificate.publicKey, it.privateKey) }
    }

    private fun generateRsaKeyPair(alias: String): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, androidKeyStoreProvider)
        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(2048)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .build()
        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    fun getOrCreateUserKeyPair(userUid: String): KeyPair {
        val alias = "user_key_$userUid"
        return getRsaKeyPair(alias) ?: generateRsaKeyPair(alias)
    }

    fun encryptWithRsaPublicKey(data: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance(rsaTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    fun decryptWithRsaPrivateKey(encryptedData: ByteArray, userUid: String): ByteArray? {
        return try {
            val alias = "user_key_$userUid"
            val privateKeyEntry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            if (privateKeyEntry == null) {
                Log.e("CryptoManager", "Không tìm thấy khóa riêng tư cho người dùng $userUid trên thiết bị này.")
                return null
            }
            val privateKey = privateKeyEntry.privateKey
            val cipher = Cipher.getInstance(rsaTransformation)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            cipher.doFinal(encryptedData)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Lỗi giải mã RSA", e)
            null
        }
    }

    fun generateAesSessionKey(): SecretKey {
        return KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }

    fun encryptWithAesKey(data: ByteArray, secretKey: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(aesTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return Pair(cipher.iv, cipher.doFinal(data))
    }

    fun decryptWithAesKey(encryptedData: ByteArray, iv: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(aesTransformation)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher.doFinal(encryptedData)
    }

    private fun getVaultSecretKey(): SecretKey {
        val alias = "WorkHubVaultKey"
        val existingKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: generateVaultSecretKey(alias)
    }

    private fun generateVaultSecretKey(alias: String): SecretKey {
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStoreProvider).apply {
            val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()
            init(spec)
        }.generateKey()
    }

    fun encryptFile(file: File, application: Application): File? {
        return try {
            val cipher = Cipher.getInstance(aesTransformation)
            cipher.init(Cipher.ENCRYPT_MODE, getVaultSecretKey())
            val iv = cipher.iv
            val encryptedFile = File(application.cacheDir, "enc_${file.name}")
            FileOutputStream(encryptedFile).use { fos ->
                fos.write(iv)
                FileInputStream(file).use { fis ->
                    CipherOutputStream(fos, cipher).use { cos -> fis.copyTo(cos) }
                }
            }
            encryptedFile
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun decryptFile(encryptedFile: File, application: Application): File? {
        return try {
            val decryptedFile = File(application.cacheDir, "dec_${encryptedFile.name.removePrefix("enc_")}")
            FileInputStream(encryptedFile).use { fis ->
                val iv = ByteArray(16)
                fis.read(iv)
                val cipher = Cipher.getInstance(aesTransformation)
                cipher.init(Cipher.DECRYPT_MODE, getVaultSecretKey(), IvParameterSpec(iv))
                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(decryptedFile).use { fos -> cis.copyTo(fos) }
                }
            }
            decryptedFile
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun cleanupDecryptedTempFiles(application: Application) {
        application.cacheDir.listFiles { _, name -> name.startsWith("dec_") }?.forEach { it.delete() }
    }
}