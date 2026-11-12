package com.apz.manager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.apz.manager.domain.RiskAnalyzer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun APZManagerScreen(vm: AppViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    var language by remember { mutableStateOf("فارسی") }
    val isFa = language == "فارسی"
    val filtered = state.apps.filter {
        it.label.contains(state.query, true) || it.packageName.contains(state.query, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("APZ Manager | مدیر APZ") },
                actions = {
                    TextButton(onClick = { language = if (isFa) "English" else "فارسی" }) { Text(language) }
                    IconButton(onClick = vm::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(if (isFa) "مدیریت و ممیزی برنامه‌های نصب‌شده" else "Installed app management and audit",
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.query, onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isFa) "جستجوی برنامه یا بسته" else "Search apps or packages") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            Text("${filtered.size} ${if (isFa) "برنامه" else "apps"}")
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.packageName }) { app ->
                    val risk = RiskAnalyzer.score(app)
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(app.label, style = MaterialTheme.typography.titleMedium)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(onClick = {}, label = { Text(if (app.isSystem) "System" else "User app") })
                                AssistChip(onClick = {}, label = { Text("Risk ${risk.score}/100 · ${risk.level}") })
                                AssistChip(onClick = {}, label = { Text("${app.permissions.size} permissions") })
                            }
                            if (risk.reasons.isNotEmpty()) {
                                Text("Sensitive permissions: ${risk.reasons.joinToString()}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
