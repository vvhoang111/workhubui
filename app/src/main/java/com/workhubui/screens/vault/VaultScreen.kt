//package com.workhubui.screens.vault
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Folder
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//
//@Composable
//fun VaultScreen() {
//    Column(modifier = Modifier.padding(16.dp)) {
//        Text("Vault", style = MaterialTheme.typography.headlineSmall)
//        Spacer(Modifier.height(8.dp))
//        val items = listOf("Project Files", "Documents", "Design Assets", "Meeting Notes", "Reports")
//        items.forEach {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 8.dp)
//                    .clickable { },
//                horizontalArrangement = Arrangement.Start
//            ) {
//                Icon(Icons.Default.Folder, contentDescription = it)
//                Spacer(Modifier.width(12.dp))
//                Text(it, style = MaterialTheme.typography.bodyLarge)
//            }
//        }
//    }
//}
package com.workhubui.screens.vault

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.workhubui.data.local.AppDatabase
import com.workhubui.data.local.dao.VaultFileDao // Import DAO
import com.workhubui.data.local.entity.VaultFileEntity // Import Entity
import com.workhubui.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// VaultFile (Model) - Đại diện cho một file trong Vault
data class VaultFile(
    val id: Long,
    val fileName: String,
    val filePath: String, // Đường dẫn cục bộ của file đã mã hóa
    val originalSize: Long, // Kích thước gốc của file
    val encryptedSize: Long, // Kích thước file đã mã hóa
    val uploadDate: Long // Thời gian upload
)

// Extension functions để chuyển đổi giữa Entity và Model
fun VaultFileEntity.toModel(): VaultFile = VaultFile(id, fileName, filePath, originalSize, encryptedSize, uploadDate)
fun VaultFile.toEntity(): VaultFileEntity = VaultFileEntity(id, fileName, filePath, originalSize, encryptedSize, uploadDate)

// VaultFileDao (DAO cho VaultFileEntity)
@androidx.room.Dao
interface VaultFileDao {
    @androidx.room.Query("SELECT * FROM vault_files ORDER BY uploadDate DESC")
    fun getAllVaultFiles(): Flow<List<VaultFileEntity>>

    @androidx.room.Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultFile(file: VaultFileEntity)

    @androidx.room.Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteVaultFile(id: Long)
}

// VaultRepository
class VaultRepository(private val dao: VaultFileDao) {
    fun getAllFiles(): Flow<List<VaultFile>> = dao.getAllVaultFiles().map { list -> list.map { it.toModel() } }
    suspend fun insertFile(file: VaultFile) = dao.insertVaultFile(file.toEntity())
    suspend fun deleteFile(id: Long) = dao.deleteVaultFile(id)
}

// VaultViewModel
class VaultViewModel(
    application: Application,
    private val vaultRepository: VaultRepository,
    private val cryptoManager: CryptoManager
) : AndroidViewModel(application) {

    private val _vaultFiles = MutableStateFlow<List<VaultFile>>(emptyList())
    val vaultFiles: StateFlow<List<VaultFile>> = _vaultFiles.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadVaultFiles()
    }

    private fun loadVaultFiles() {
        viewModelScope.launch {
            vaultRepository.getAllFiles()
                .catch { e -> _errorMessage.value = "Lỗi tải file: ${e.message}" }
                .collectLatest {
                    _vaultFiles.value = it
                }
        }
    }

    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val context = getApplication<Application>().applicationContext

                // Tạo một file tạm thời từ URI để mã hóa
                val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: run {
                    _errorMessage.value = "Không thể đọc file từ URI."
                    return@launch
                }

                // Bước 1: Mã hóa file cục bộ
                val encryptedFile = cryptoManager.encryptFile(tempFile, getApplication())
                tempFile.delete() // Xóa file tạm thời
                if (encryptedFile == null) {
                    _errorMessage.value = "Lỗi mã hóa file."
                    return@launch
                }

                // Bước 2: Lưu thông tin file đã mã hóa vào Room
                val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: "unknown_file"

                val vaultFile = VaultFile(
                    fileName = fileName,
                    filePath = encryptedFile.absolutePath,
                    originalSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L,
                    encryptedSize = encryptedFile.length(),
                    uploadDate = System.currentTimeMillis()
                )
                vaultRepository.insertFile(vaultFile)

                // TODO: Bước 3: Upload file đã mã hóa lên Cloud Storage (Firebase Storage)
                // Đây là phần phức tạp, cần Firebase Storage SDK và xử lý upload.
                Toast.makeText(getApplication(), "File đã mã hóa và lưu cục bộ. Cần tích hợp upload lên Cloud.", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                _errorMessage.value = "Lỗi upload/mã hóa: ${e.message}"
            }
        }
    }

    fun deleteFile(file: VaultFile) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                // Xóa file cục bộ
                val localFile = File(file.filePath)
                if (localFile.exists()) {
                    localFile.delete()
                }

                // Xóa khỏi database
                vaultRepository.deleteFile(file.id)

                // TODO: Xóa file trên Cloud Storage nếu có
                Toast.makeText(getApplication(), "File đã xóa cục bộ. Cần tích hợp xóa trên Cloud nếu có.", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                _errorMessage.value = "Lỗi xóa file: ${e.message}"
            }
        }
    }

    fun openFile(file: VaultFile) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val encryptedFile = File(file.filePath)
                if (!encryptedFile.exists()) {
                    _errorMessage.value = "File mã hóa không tồn tại."
                    return@launch
                }

                // Giải mã file vào một file tạm thời
                val decryptedFile = cryptoManager.decryptFile(encryptedFile, getApplication())
                if (decryptedFile == null) {
                    _errorMessage.value = "Lỗi giải mã file."
                    return@launch
                }

                // Mở file đã giải mã bằng Intent
                val context = getApplication<Application>().applicationContext
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", decryptedFile)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, context.contentResolver.getType(uri))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Quan trọng cho context không phải activity
                context.startActivity(intent)

            } catch (e: Exception) {
                _errorMessage.value = "Lỗi mở file: ${e.message}"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: VaultViewModel = viewModel(factory = VaultViewModelFactory(application))

    val vaultFiles by viewModel.vaultFiles.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    // Launcher để chọn file từ bộ nhớ thiết bị
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(it)
        }
    }

    // Display error messages using Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            // viewModel.clearErrorMessage() // Clear error after showing (if you add this function)
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
                pickFileLauncher.launch("*/*") // Mở trình chọn file, cho phép tất cả các loại file
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Tải lên file mới")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Các file đã mã hóa", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            if (vaultFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có file nào trong Vault.")
                }
            } else {
                LazyColumn {
                    items(vaultFiles, key = { it.id }) { file ->
                        VaultFileItem(file = file,
                            onOpenClick = { viewModel.openFile(it) },
                            onDeleteClick = { viewModel.deleteFile(it) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun VaultFileItem(file: VaultFile, onOpenClick: (VaultFile) -> Unit, onDeleteClick: (VaultFile) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenClick(file) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Folder, contentDescription = "File", modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(file.fileName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
        }
        IconButton(onClick = { onDeleteClick(file) }) {
            Icon(Icons.Default.Delete, contentDescription = "Xóa file", tint = MaterialTheme.colorScheme.error)
        }
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}