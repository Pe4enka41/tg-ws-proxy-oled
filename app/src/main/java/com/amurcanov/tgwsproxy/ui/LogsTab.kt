package com.amurcanov.tgwsproxy.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amurcanov.tgwsproxy.LogEntry
import com.amurcanov.tgwsproxy.LogManager
import com.amurcanov.tgwsproxy.R
import com.amurcanov.tgwsproxy.SettingsStore
import kotlinx.coroutines.launch

@Composable
fun LogsScreen(settingsStore: SettingsStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs by LogManager.logs.collectAsStateWithLifecycle()

    val info by settingsStore.logShowInfo.collectAsStateWithLifecycle(initialValue = true)
    val errors by settingsStore.logShowError.collectAsStateWithLifecycle(initialValue = true)
    val debug by settingsStore.logShowDebug.collectAsStateWithLifecycle(initialValue = false)

    val filtered = remember(logs, info, errors, debug) {
        logs.filter { entry ->
            when {
                entry.isEssential -> true
                entry.priority >= 6 -> errors
                entry.priority == 5 -> errors
                entry.priority == 3 -> debug
                else -> info
            }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(filtered.size) {
        if (filtered.isNotEmpty()) {
            listState.animateScrollToItem(filtered.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 12.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_logs),
                style = MaterialTheme.typography.headlineSmall,
                color = Ink.Text
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                val text = filtered.joinToString("\n") { "${it.message} (x${it.count})" }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("proxy log", text))
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.copy),
                    tint = Ink.TextDim,
                    modifier = Modifier.size(19.dp)
                )
            }
            IconButton(onClick = { LogManager.clearLogs() }) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.clear),
                    tint = Ink.TextDim,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LogFilter(
                label = stringResource(R.string.logs_info),
                selected = info,
                onClick = { scope.launch { settingsStore.saveLogFilters(debug, !info, errors, false) } }
            )
            LogFilter(
                label = stringResource(R.string.logs_errors),
                selected = errors,
                onClick = { scope.launch { settingsStore.saveLogFilters(debug, info, !errors, false) } }
            )
            LogFilter(
                label = stringResource(R.string.logs_debug),
                selected = debug,
                onClick = { scope.launch { settingsStore.saveLogFilters(!debug, info, errors, false) } }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink.Black)
        ) {
            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(R.string.logs_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.TextFaint,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 22.dp, vertical = 18.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 22.dp, end = 18.dp, top = 6.dp, bottom = 24.dp)
                ) {
                    items(items = filtered, key = { it.key }) { entry ->
                        LogLine(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Ink.Text else Ink.TextFaint
        )
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .width(if (selected) 18.dp else 0.dp)
                .height(1.dp)
                .background(if (selected) Ink.Signal else androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}

@Composable
private fun LogLine(entry: LogEntry) {
    val color = when (entry.priority) {
        6 -> Ink.Danger
        5 -> Ink.Warning
        3 -> Ink.TextFaint
        else -> Ink.TextDim
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(4.dp)
                .background(color)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = if (entry.count > 1) "${entry.message}  ×${entry.count}" else entry.message,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
