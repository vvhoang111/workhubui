package com.cdcs.screens.vault

import android.app.Application
import android.net.Uri
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
import androidx.lifecycle.viewmodel.compose.viewModel
// import kotlinx.coroutines.flow.map // Not needed if repository returns models
import java.text.SimpleDateFormat
import java.util.*
import com.cdcs.model.VaultFile // Import VaultFile từ package model

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

    // ActivityResultLauncher for picking a file from device storage
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(it) // Call ViewModel to handle the selected file URI
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
