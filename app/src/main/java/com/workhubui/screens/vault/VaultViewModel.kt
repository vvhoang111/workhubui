package com.workhubui.screens.vault

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns // Để lấy tên/kích thước file từ Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen // Changed icon for better representation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workhubui.data.local.entity.VaultFileEntity
import com.workhubui.data.repository.VaultRepository // Corrected: Import from the new package
import com.workhubui.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.workhubui.model.VaultFile // Import VaultFile từ package model
import com.workhubui.utils.toEntity // Import toEntity extension từ utils
import com.workhubui.utils.toModel // Import toModel extension từ utils

// --- ViewModel for VaultScreen ---
class VaultViewModel(
    application: Application,
    private val vaultRepository: VaultRepository, // Uses the imported VaultRepository
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
        // Dọn dẹp các tệp đã giải mã tạm thời khi ViewModel khởi tạo (ứng dụng khởi động lại)
        cryptoManager.cleanupDecryptedTempFiles(application)
    }

    /**
     * Loads vault files from the repository and updates the UI state.
     */
    private fun loadVaultFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            // Assuming vaultRepository.getAllFilesAsModels() returns Flow<List<VaultFile>>
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

    /**
     * Handles the file upload process:
     * 1. Gets file metadata (name, size) from URI.
     * 2. Copies the file from URI to a temporary local file.
     * 3. Encrypts the temporary file.
     * 4. Saves the encrypted file's metadata to the Room database.
     * 5. Deletes the temporary unencrypted file.
     * (Cloud upload is a TODO).
     * @param uri The URI of the file selected by the user.
     */
    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val context = getApplication<Application>().applicationContext

            try {
                var fileName = "unknown_file"
                var originalSize = 0L

                // Get file name and original size from the content resolver
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) originalSize = cursor.getLong(sizeIndex)
                }

                // Create a temporary file to copy content from URI
                val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}_${fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}") // Sanitize file name
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: run {
                    _errorMessage.value = "Không thể đọc file từ URI."
                    _isLoading.value = false
                    return@launch
                }

                // Encrypt the temporary file
                val encryptedFile = cryptoManager.encryptFile(tempFile, getApplication())
                tempFile.delete() // Clean up the unencrypted temporary file

                if (encryptedFile == null) {
                    _errorMessage.value = "Lỗi mã hóa file."
                    _isLoading.value = false
                    return@launch
                }

                // Create VaultFileEntity to save to Room
                val vaultFileEntityToSave = VaultFileEntity(
                    // id will be auto-generated by Room for new entries
                    fileName = fileName,
                    filePath = encryptedFile.absolutePath,
                    originalSize = originalSize,
                    encryptedSize = encryptedFile.length(),
                    uploadDate = System.currentTimeMillis()
                )
                vaultRepository.insertFile(vaultFileEntityToSave) // Insert the entity

                Toast.makeText(context, "File '${fileName}' đã được mã hóa và lưu.", Toast.LENGTH_LONG).show()

                // TODO: Implement upload to Cloud Storage (e.g., Firebase Storage) here if needed.

            } catch (e: Exception) {
                _errorMessage.value = "Lỗi upload/mã hóa: ${e.localizedMessage}"
                e.printStackTrace() // Log the full stack trace for debugging
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a vault file from local storage and the database.
     * (Cloud deletion is a TODO).
     * @param file The VaultFile (UI model) to delete.
     */
    fun deleteFile(file: VaultFile) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Delete the local encrypted file
                val localEncryptedFile = File(file.filePath)
                if (localEncryptedFile.exists()) {
                    if (!localEncryptedFile.delete()) {
                        _errorMessage.value = "Không thể xóa file cục bộ."
                        // Potentially do not proceed with DB deletion if local file deletion fails
                    }
                }

                // Delete the record from Room database
                vaultRepository.deleteFileById(file.id)

                Toast.makeText(getApplication(), "File '${file.fileName}' đã xóa.", Toast.LENGTH_SHORT).show()

                // TODO: Implement deletion from Cloud Storage here if needed.

            } catch (e: Exception) {
                _errorMessage.value = "Lỗi xóa file: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Opens a vault file by:
     * 1. Decrypting it to a temporary location.
     * 2. Creating a content URI using FileProvider.
     * 3. Launching an Intent to view the file with an appropriate app.
     * @param file The VaultFile (UI model) to open.
     */
    fun openFile(file: VaultFile) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val context = getApplication<Application>().applicationContext
            try {
                // Đảm bảo dọn dẹp các tệp giải mã cũ trước khi tạo tệp mới
                cryptoManager.cleanupDecryptedTempFiles(getApplication())

                val encryptedFile = File(file.filePath)
                if (!encryptedFile.exists()) {
                    _errorMessage.value = "File mã hóa không tồn tại."
                    _isLoading.value = false
                    return@launch
                }

                // Giải mã tệp vào một vị trí tạm thời
                val decryptedFile = cryptoManager.decryptFile(encryptedFile, getApplication())
                if (decryptedFile == null || !decryptedFile.exists()) {
                    _errorMessage.value = "Lỗi giải mã file hoặc file giải mã không tồn tại."
                    // Dọn dẹp nếu decryptedFile được tạo nhưng không hợp lệ
                    decryptedFile?.delete()
                    _isLoading.value = false
                    return@launch
                }

                // Lấy URI cho tệp đã giải mã bằng FileProvider
                val authority = "${context.packageName}.fileprovider" // Đảm bảo khớp với AndroidManifest
                val decryptedFileUri = FileProvider.getUriForFile(context, authority, decryptedFile)

                // Tạo một Intent để xem tệp
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(decryptedFileUri, context.contentResolver.getType(decryptedFileUri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Cấp quyền đọc cho ứng dụng nhận
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Cần thiết nếu được gọi từ một Context không phải Activity
                }

                // Kiểm tra xem có ứng dụng nào để xử lý Intent này không
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    _errorMessage.value = "Không tìm thấy ứng dụng để mở loại file này."
                }
                // Lưu ý: Tệp tạm thời đã giải mã sẽ được dọn dẹp ở lần mở file tiếp theo hoặc khi app khởi động lại.

            } catch (e: Exception) {
                _errorMessage.value = "Lỗi mở file: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Xóa thông báo lỗi hiện tại.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
