package com.workhubui.screens.vault

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
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
// import kotlinx.coroutines.flow.map // Not needed if repository returns models
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// --- Data Model for UI ---
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

// --- Extension Functions for Model-Entity Conversion ---
// These can be in a separate utility file if preferred.

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
                val encryptedFile = File(file.filePath)
                if (!encryptedFile.exists()) {
                    _errorMessage.value = "File mã hóa không tồn tại."
                    _isLoading.value = false
                    return@launch
                }

                // Decrypt the file to a temporary location
                val decryptedFile = cryptoManager.decryptFile(encryptedFile, getApplication())
                if (decryptedFile == null || !decryptedFile.exists()) {
                    _errorMessage.value = "Lỗi giải mã file hoặc file giải mã không tồn tại."
                    // Clean up if decryptedFile was created but is invalid
                    decryptedFile?.delete()
                    _isLoading.value = false
                    return@launch
                }

                // Get URI for the decrypted file using FileProvider
                val authority = "${context.packageName}.fileprovider" // Ensure this matches AndroidManifest
                val decryptedFileUri = FileProvider.getUriForFile(context, authority, decryptedFile)

                // Create an Intent to view the file
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(decryptedFileUri, context.contentResolver.getType(decryptedFileUri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant read permission to the receiving app
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Necessary if called from a non-Activity context
                }

                // Check if there's an app to handle this intent
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    _errorMessage.value = "Không tìm thấy ứng dụng để mở loại file này."
                }
                // Note: The decrypted temporary file should ideally be cleaned up after viewing.
                // This can be tricky as you don't know when the external app finishes.
                // One strategy is to clean up such temp files on app startup or periodically.

            } catch (e: Exception) {
                _errorMessage.value = "Lỗi mở file: ${e.localizedMessage}"
                e.printStackTrace() // Log for debugging
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}

// --- Composable UI for VaultScreen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen() {
    val application = LocalContext.current.applicationContext as Application
    // Initialize ViewModel using the factory
    val viewModel: VaultViewModel = viewModel(factory = VaultViewModelFactory(application))

    val vaultFiles by viewModel.vaultFiles.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // ActivityResultLauncher for picking a file from device storage
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(it) // Call ViewModel to handle the selected file URI
        }
    }

    // Snackbar host state for displaying error messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) { // Show snackbar when errorMessage changes
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearErrorMessage() // Clear the error message after showing
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kho lưu trữ an toàn") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (!isLoading) {
                    pickFileLauncher.launch("*/*") // Launch file picker, allow all file types
                } else {
                    Toast.makeText(context, "Đang xử lý, vui lòng đợi...", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Tải lên file mới")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) } // Host for snackbar messages
    ) { paddingValues ->
        Box( // Use Box to overlay CircularProgressIndicator when loading
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // Padding for the content column
            ) {
                Text("Các file đã mã hóa", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))

                if (vaultFiles.isEmpty() && !isLoading) {
                    // Display message if vault is empty and not loading
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có file nào trong Kho lưu trữ.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    // Display list of vault files
                    LazyColumn {
                        items(vaultFiles, key = { it.id }) { file -> // Use file.id as key for item stability
                            VaultFileItem(
                                file = file,
                                onOpenClick = { viewModel.openFile(it) },
                                onDeleteClick = { viewModel.deleteFile(it) }
                            )
                            HorizontalDivider() // Divider between items
                        }
                    }
                }
            }
            if (isLoading) { // Show loading indicator in the center of the screen
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

/**
 * Composable for displaying a single item in the vault file list.
 */
@Composable
fun VaultFileItem(
    file: VaultFile,
    onOpenClick: (VaultFile) -> Unit,
    onDeleteClick: (VaultFile) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenClick(file) } // Make the row clickable to open the file
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.FolderOpen, // Icon representing a file/folder
            contentDescription = "Biểu tượng file",
            modifier = Modifier.size(40.dp), // Slightly larger icon
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) { // Text content takes remaining space
            Text(file.fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Kích thước: ${formatFileSize(file.originalSize)} (Mã hóa: ${formatFileSize(file.encryptedSize)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Ngày tải lên: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(file.uploadDate))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Delete button for the file item
        IconButton(onClick = { onDeleteClick(file) }) {
            Icon(Icons.Default.Delete, contentDescription = "Xóa file", tint = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * Utility function to format file size from bytes to a human-readable string (KB, MB, GB).
 */
fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    // Ensure digitGroups is within the bounds of the units array
    val unitIndex = digitGroups.coerceIn(0, units.size - 1)
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, unitIndex.toDouble()), units[unitIndex])
}
