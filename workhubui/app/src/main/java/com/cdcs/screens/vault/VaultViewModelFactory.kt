package com.cdcs.screens.vault

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cdcs.data.local.AppDatabase
import com.cdcs.data.repository.VaultRepository // Corrected: Import from the new package
import com.cdcs.security.CryptoManager

/**
 * Factory for creating [VaultViewModel] instances.
 * This allows passing dependencies (Application, VaultRepository, CryptoManager) to the ViewModel.
 */
class VaultViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {
            // Initialize dependencies for VaultViewModel
            val vaultFileDao = AppDatabase.getInstance(application).vaultFileDao()
            val vaultRepository = VaultRepository(vaultFileDao) // Use the correctly imported VaultRepository
            val cryptoManager = CryptoManager() // Instantiate CryptoManager

            @Suppress("UNCHECKED_CAST")
            return VaultViewModel(application, vaultRepository, cryptoManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
