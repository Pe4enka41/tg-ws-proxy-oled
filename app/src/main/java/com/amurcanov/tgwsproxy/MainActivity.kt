package com.amurcanov.tgwsproxy

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.amurcanov.tgwsproxy.ui.HomeScreen
import com.amurcanov.tgwsproxy.ui.Ink
import com.amurcanov.tgwsproxy.ui.LogsScreen
import com.amurcanov.tgwsproxy.ui.SettingsScreen
import com.amurcanov.tgwsproxy.ui.TgWsProxyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = AppPrefs.language(newBase)
        if (language == AppPrefs.SYSTEM) {
            super.attachBaseContext(newBase)
            return
        }
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            val settingsStore = remember { SettingsStore(context) }

            TgWsProxyTheme {
                MainContent(
                    settingsStore = settingsStore,
                    onLanguageChanged = {
                        AppPrefs.saveLanguage(context, it)
                        recreate()
                    }
                )
            }
        }
    }
}

private data class NavItem(
    val label: Int,
    val icon: ImageVector
)

@Composable
private fun MainContent(
    settingsStore: SettingsStore,
    onLanguageChanged: (String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val navItems = listOf(
        NavItem(R.string.nav_home, Icons.Outlined.PowerSettingsNew),
        NavItem(R.string.nav_logs, Icons.AutoMirrored.Outlined.Article),
        NavItem(R.string.nav_settings, Icons.Outlined.Tune)
    )

    DisposableEffect(Unit) {
        LogManager.startListening()
        onDispose { LogManager.stopListening() }
    }

    Scaffold(
        containerColor = Ink.Black,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        bottomBar = {
            OledNavBar(
                items = navItems,
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Ink.Black)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "screen"
            ) { screen ->
                when (screen) {
                    0 -> HomeScreen(settingsStore)
                    1 -> LogsScreen(settingsStore)
                    else -> SettingsScreen(settingsStore, onLanguageChanged = onLanguageChanged)
                }
            }
        }
    }
}

@Composable
private fun OledNavBar(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Ink.LineSoft)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(62.dp)
        ) {
            items.forEachIndexed { index, item ->
                val active = index == selected
                val color = if (active) Ink.Text else Ink.TextFaint
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        text = stringResource(item.label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        color = color,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Logcat reader with batched updates. Kept close to the original implementation:
 * it is already tuned to avoid GC churn from high-frequency engine logs.
 */
object LogManager {
    val logs = MutableStateFlow<List<LogEntry>>(emptyList())
    private var job: Job? = null
    private var logcatProcess: Process? = null
    private val nextKey = AtomicLong(0)
    private val logChannel = Channel<LogEntry>(capacity = Channel.BUFFERED)

    fun startListening() {
        if (job?.isActive == true) return
        job = CoroutineScope(Dispatchers.IO).launch {
            val readerJob = launch(Dispatchers.IO) {
                try {
                    val pid = android.os.Process.myPid()
                    val process = ProcessBuilder("logcat", "-v", "tag", "--pid", pid.toString())
                        .redirectErrorStream(true)
                        .start()
                    logcatProcess = process

                    process.inputStream.bufferedReader().use { reader ->
                        while (isActive) {
                            val line = try { reader.readLine() } catch (_: Exception) { null } ?: break
                            val entry = parseLine(line) ?: continue
                            logChannel.trySend(entry)
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    logcatProcess?.destroy()
                    logcatProcess = null
                }
            }

            launch {
                val pending = mutableListOf<LogEntry>()
                while (isActive) {
                    var received = logChannel.tryReceive()
                    while (received.isSuccess) {
                        pending.add(received.getOrThrow())
                        if (pending.size >= 20) break
                        received = logChannel.tryReceive()
                    }
                    if (pending.isNotEmpty()) {
                        logs.value = applyBatch(logs.value, pending)
                        pending.clear()
                    }
                    delay(150)
                }
            }

            readerJob.join()
        }
    }

    private fun applyBatch(current: List<LogEntry>, batch: List<LogEntry>): List<LogEntry> {
        val result = ArrayDeque(current)
        for (entry in batch) {
            var merged = false
            val depth = minOf(result.size, 10)
            for (i in result.indices.reversed().take(depth)) {
                if (result[i].message == entry.message) {
                    val existing = result.removeAt(i)
                    result.addLast(existing.copy(count = existing.count + 1))
                    merged = true
                    break
                }
            }
            if (!merged) result.addLast(entry)
        }
        while (result.size > 80) result.removeFirst()
        return result.toList()
    }

    fun stopListening() {
        job?.cancel()
        job = null
        logcatProcess?.destroy()
        logcatProcess = null
    }

    fun clearLogs() {
        logs.value = emptyList()
    }

    private fun parseLine(raw: String): LogEntry? {
        var message: String
        val isError: Boolean
        val priority: Int

        when {
            raw.contains("[ERROR]") -> {
                message = raw.substringAfter("[ERROR]").trim()
                isError = true
                priority = 6
            }
            raw.contains("[WARN]") -> {
                message = raw.substringAfter("[WARN]").trim()
                isError = false
                priority = 5
            }
            raw.contains("[DEBUG]") -> {
                message = raw.substringAfter("[DEBUG]").trim()
                isError = false
                priority = 3
            }
            raw.contains("TgWsProxy") -> {
                var msg = raw.substringAfter("TgWsProxy:").trim()
                if (msg.startsWith("[ERROR]") || msg.startsWith("[WARN]") || msg.startsWith("[DEBUG]")) {
                    return null
                }
                if (msg.contains("↑")) msg = msg.substringBefore("↑").trim()
                if (msg.contains("↓")) msg = msg.substringBefore("↓").trim()
                message = msg
                isError = false
                priority = 4
            }
            else -> return null
        }

        val emojiRegex = Regex(
            "[\\x{1F300}-\\x{1F5FF}\\x{1F900}-\\x{1F9FF}\\x{1F600}-\\x{1F64F}" +
                "\\x{1F680}-\\x{1F6FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}" +
                "\\x{1F1E6}-\\x{1F1FF}\\x{1F191}-\\x{1F251}\\x{1F004}\\x{1F0CF}" +
                "\\x{1F170}-\\x{1F171}\\x{1F17E}-\\x{1F17F}\\x{1F18E}\\x{3030}" +
                "\\x{2B50}\\x{2B55}\\x{2934}-\\x{2935}\\x{2B05}-\\x{2B07}" +
                "\\x{2B1B}-\\x{2B1C}\\x{3297}\\x{3299}\\x{303D}\\x{00A9}\\x{00AE}" +
                "\\x{2122}\\x{23F3}\\x{24C2}\\x{23E9}-\\x{23EF}\\x{25B6}" +
                "\\x{23F8}-\\x{23FA}⚠✅❌⚡🔥🔄🔗]"
        )
        message = message.replace(emojiRegex, "").trim()
        if (message.isEmpty()) return null

        val essential = listOf(
            "pool", "key:", "started", "address:", "error", "failed", "blocked",
            "Пул", "Ключ:", "запущен", "Адрес:", "ошибка", "провалены", "заблокирован"
        ).any { message.contains(it, ignoreCase = true) }

        return LogEntry(
            key = "log_${nextKey.getAndIncrement()}",
            message = message,
            count = 1,
            isError = isError,
            priority = priority,
            isEssential = essential
        )
    }
}
