package com.workhubui.screens.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VaultScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Vault", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        val items = listOf("Project Files", "Documents", "Design Assets", "Meeting Notes", "Reports")
        items.forEach {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { },
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(Icons.Default.Folder, contentDescription = it)
                Spacer(Modifier.width(12.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}