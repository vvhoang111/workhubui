package com.workhubui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TaskboardScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Taskboard", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        val tasks = listOf(
            "Create wireframes", "Review requirements",
            "Write documentation", "Fix bugs", "Plan roadmap"
        )

        val checkedStates = remember { mutableStateListOf(false, true, true, false, false) }

        tasks.forEachIndexed { index, task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .toggleable(
                        value = checkedStates[index],
                        onValueChange = { checkedStates[index] = it }
                    )
            ) {
                Checkbox(
                    checked = checkedStates[index],
                    onCheckedChange = null
                )
                Spacer(Modifier.width(8.dp))
                Text(task)
            }
        }
    }
}