```kotlin
package com.apz.manager.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.apz.manager.data.model.Report
import com.apz.manager.python.PythonBridge
import com.apz.manager.ui.components.RiskBadge
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(sha256: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var report by remember { mutableStateOf<Report?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sha256) {
        try {
            report = PythonBridge.getReport(sha256)
        } catch (t: Throwable) {
            error = t.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("گزارش تحلیل") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    report?.let { r ->
                        IconButton(onClick = {
                            val json = Gson().toJson(r)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            ctx.startActivity(Intent.createChooser(send, "اشتراک‌گذاری"))
                        }) {
                            Icon(Icons.Filled.Share, null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        val r = report
        when {
            error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { Text("خطا: $error") }

            r == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { HeaderCard(r) }
                item { SummaryCard(r) }

                if (r.ai?.summary != null || r.ai?.recommendations != null) {
                    item { AiCard(r) }
                }

                if (r.findings.isNotEmpty()) {
                    item { SectionTitle("یافته‌ها") }
                    items(r.findings) { FindingCard(it) }
                }

                if (r.permissions.isNotEmpty()) {
                    item { SectionTitle("مجوزها (${r.permissions.size})") }
                    items(r.permissions) { p ->
                        Text("• $p", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (r.threat.yara.isNotEmpty()) {
                    item { SectionTitle("YARA (${r.threat.yara.size})") }
                    items(r.threat.yara) { y ->
                        Text("• ${y.rule} [${y.severity}]",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (r.threat.mitre.isNotEmpty()) {
                    item { SectionTitle("MITRE ATT&CK") }
                    items(r.threat.mitre) { m ->
                        Text("• ${m.id} — ${m.name}",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }

                r.threat.iocs.forEach { (k, v) ->
                    if (v.isNotEmpty()) {
                        item { SectionTitle("IOC – $k") }
                        items(v.take(20)) { ioc ->
                            Text("• $ioc", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (r.limitations.isNotEmpty()) {
                    item { SectionTitle("محدودیت‌ها") }
                    items(r.limitations) {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun HeaderCard(r: Report) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(r.fileName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("SHA-256: ${r.sha256.take(16)}…",
                style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RiskBadge(r.level)
                Text("امتیاز: ${r.riskScore}/100",
                    style = MaterialTheme.typography.bodyMedium)
                Text("اطمینان: ${r.confidence}%",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SummaryCard(r: Report) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("خلاصه", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("حجم: ${r.sizeBytes / 1024} KB",
                style = MaterialTheme.typography.bodyMedium)
            Text("پکیج: ${r.packageName.ifEmpty { "—" }}",
                style = MaterialTheme.typography.bodyMedium)
            Text("نسخه: ${r.versionName.ifEmpty { "—" }}",
                style = MaterialTheme.typography.bodyMedium)
            Text("min/target SDK: ${r.minSdk} / ${r.targetSdk}",
                style = MaterialTheme.typography.bodyMedium)
            Text("زمان تحلیل: ${r.analysisTimeSeconds}s",
                style = MaterialTheme.typography.bodyMedium)

            if (r.explanation.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("دلایل وزن‌دهی:", style = MaterialTheme.typography.labelSmall)
                r.explanation.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AiCard(r: Report) {
    val ai = r.ai ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("🤖 تحلیل AI", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ai.summary?.takeIf { it.isNotBlank() }?.let {
                Text("خلاصه:", style = MaterialTheme.typography.labelSmall)
                Text(it, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }
            ai.riskExplanation?.takeIf { it.isNotBlank() }?.let {
                Text("توضیح ریسک:", style = MaterialTheme.typography.labelSmall)
                Text(it, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }
            ai.recommendations?.takeIf { it.isNotBlank() }?.let {
                Text("توصیه‌ها:", style = MaterialTheme.typography.labelSmall)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FindingCard(f: com.apz.manager.data.model.Finding) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(f.titleFa, style = MaterialTheme.typography.titleMedium)
            Text("وزن: ${f.weight}", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(f.detailFa, style = MaterialTheme.typography.bodyMedium)
            if (f.evidence.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                f.evidence.take(8).forEach {
                    Text("• $it", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp)
    )
}
```
