package com.cdcs.screens.vault

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdcs.data.repository.VaultRepository
import com.cdcs.model.VaultFile
import com.cdcs.security.CryptoManager
import com.cdcs.utils.toEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class VaultViewModel(
    application: Application,
    private val vaultRepository: VaultRepository,
    private val cryptoManager: CryptoManager
) : AndroidViewModel(application) {

    private val _vaultFiles = MutableStateFlow<List<VaultFile>>(emptyList())
    val vaultFiles: StateFlow<List<VaultFile>> = _vaultFiles.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadVaultFiles()
        cryptoManager.cleanupDecryptedTempFiles(application)
    }

    private fun loadVaultFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            vaultRepository.getAllFilesAsModels()
                .catch { e ->
                    _errorMessage.value = "Lỗi tải danh sách file: ${e.message}"
                    _isLoading.value = false
                }
                .collectLatest { files ->
                    _vaultFiles.value = files
                    _isLoading.value = false
                }
        }
    }

    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val context = getApplication<Application>().applicationContext

            try {
                var fileName = "unknown_file"
                var originalSize = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) originalSize = cursor.getLong(sizeIndex)
                }

                val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}_${fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: run {
                    _errorMessage.value = "Không thể đọc file từ URI."; _isLoading.value = false; return@launch
                }

                val encryptedFile = cryptoManager.encryptFile(tempFile, getApplication())
                tempFile.delete()

                if (encryptedFile == null) {
                    _errorMessage.value = "Lỗi mã hóa file."; _isLoading.value = false; return@launch
                }

                val vaultFileModel = VaultFile(
                    fileName = fileName,
                    filePath = encryptedFile.absolutePath,
                    originalSize = originalSize,
                    encryptedSize = encryptedFile.length(),
                    uploadDate = System.currentTimeMillis()
                )
                vaultRepository.insertFile(vaultFileModel.toEntity())

                Toast.makeText(context, "File '${fileName}' đã được mã hóa và lưu.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi upload/mã hóa: ${e.localizedMessage}"; e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFile(file: VaultFile) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val localEncryptedFile = File(file.filePath)
                if (localEncryptedFile.exists()) {
                    if (!localEncryptedFile.delete()) _errorMessage.value = "Không thể xóa file cục bộ."
                }
                vaultRepository.deleteFileById(file.id)
                Toast.makeText(getApplication(), "File '${file.fileName}' đã xóa.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi xóa file: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openFile(file: VaultFile) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val context = getApplication<Application>().applicationContext
            try {
                cryptoManager.cleanupDecryptedTempFiles(getApplication())

                val encryptedFile = File(file.filePath)
                if (!encryptedFile.exists()) {
                    _errorMessage.value = "File mã hóa không tồn tại."; _isLoading.value = false; return@launch
                }

                val decryptedFile = cryptoManager.decryptFile(encryptedFile, getApplication())
                if (decryptedFile == null || !decryptedFile.exists()) {
                    _errorMessage.value = "Lỗi giải mã file hoặc file giải mã không tồn tại."; decryptedFile?.delete(); _isLoading.value = false; return@launch
                }

                val authority = "${context.packageName}.fileprovider"
                val decryptedFileUri = FileProvider.getUriForFile(context, authority, decryptedFile)

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(decryptedFileUri, context.contentResolver.getType(decryptedFileUri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    _errorMessage.value = "Không tìm thấy ứng dụng để mở loại file này."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi mở file: ${e.localizedMessage}"; e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
