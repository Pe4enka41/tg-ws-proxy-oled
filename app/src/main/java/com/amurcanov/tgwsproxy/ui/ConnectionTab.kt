package com.amurcanov.tgwsproxy.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amurcanov.tgwsproxy.BuildConfig
import com.amurcanov.tgwsproxy.ProxyController
import com.amurcanov.tgwsproxy.ProxyService
import com.amurcanov.tgwsproxy.R
import com.amurcanov.tgwsproxy.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(settingsStore: SettingsStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isRunning by ProxyService.isRunning.collectAsStateWithLifecycle()
    val isVerifiedRunning by ProxyService.isVerifiedRunning.collectAsStateWithLifecycle()
    val stats by ProxyService.stats.collectAsStateWithLifecycle()
    val startedAt by ProxyService.startedAt.collectAsStateWithLifecycle()
    val lastError by ProxyService.lastError.collectAsStateWithLifecycle()

    val port by settingsStore.port.collectAsStateWithLifecycle(initialValue = "1445")
    val bindIp by settingsStore.bindIp.collectAsStateWithLifecycle(initialValue = "127.0.0.1")
    val cfEnabled by settingsStore.cfproxyEnabled.collectAsStateWithLifecycle(initialValue = true)
    val secretKey by settingsStore.secretKey.collectAsStateWithLifecycle(initialValue = "")
    val applyMode by settingsStore.applyMode.collectAsStateWithLifecycle(initialValue = "packages")
    val autoApply by settingsStore.autoApplyOnStart.collectAsStateWithLifecycle(initialValue = false)

    var isStarting by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRunning, isVerifiedRunning) {
        if (isVerifiedRunning || !isRunning) isStarting = false
    }
    LaunchedEffect(lastError) {
        if (lastError != null) isStarting = false
    }
    LaunchedEffect(isStarting) {
        if (isStarting) {
            delay(10_000)
            if (!ProxyService.isRunning.value && !ProxyService.isVerifiedRunning.value) {
                isStarting = false
            }
        }
    }

    // Keep a stable key for the local proxy, generated once and reused.
    LaunchedEffect(secretKey) {
        if (secretKey.isEmpty()) {
            val bytes = ByteArray(16)
            java.security.SecureRandom().nextBytes(bytes)
            settingsStore.saveSecretKey(bytes.joinToString("") { "%02x".format(it) })
        }
    }

    LaunchedEffect(isVerifiedRunning, startedAt) {
        while (isVerifiedRunning) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val proxyUrl = remember(bindIp, port, secretKey) {
        val secret = secretKey.ifBlank { "00000000000000000000000000000000" }
        "https://t.me/proxy?server=$bindIp&port=$port&secret=dd$secret"
    }

    var autoApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isVerifiedRunning) {
        if (!isVerifiedRunning) {
            autoApplied = false
        } else if (autoApply && !autoApplied) {
            autoApplied = true
            applyProxyToTelegram(context, proxyUrl, applyMode)
        }
    }

    // "Open in Telegram" also works while the proxy is off: start it first
    // and apply as soon as the engine reports ready.
    var pendingApply by remember { mutableStateOf(false) }
    LaunchedEffect(lastError) {
        if (lastError != null) pendingApply = false
    }
    LaunchedEffect(isVerifiedRunning, pendingApply) {
        if (isVerifiedRunning && pendingApply) {
            pendingApply = false
            applyProxyToTelegram(context, proxyUrl, applyMode)
        }
    }

    val connect = {
        if (!isRunning && !isStarting) {
            isStarting = true
            scope.launch {
                val started = ProxyController.startFromSavedSettings(context, showInvalidPortToast = true)
                if (!started) isStarting = false
            }
        }
    }
    val disconnect = {
        if (isRunning || isStarting) {
            isStarting = false
            ProxyController.stop(context)
        }
    }

    val active = isRunning || isStarting
    val statusText = when {
        isVerifiedRunning -> stringResource(R.string.status_on)
        active -> stringResource(R.string.status_starting)
        else -> stringResource(R.string.status_off)
    }
    val statusColor by animateColorAsState(
        targetValue = when {
            isVerifiedRunning -> Ink.Signal
            active -> Ink.Warning
            else -> Ink.TextFaint
        },
        animationSpec = tween(350),
        label = "status_color"
    )
    val modeLabel = if (cfEnabled) stringResource(R.string.mode_cloudflare) else stringResource(R.string.mode_direct)
    val hintText = when {
        isVerifiedRunning -> stringResource(R.string.hint_on)
        active -> stringResource(R.string.hint_starting)
        else -> stringResource(R.string.hint_off)
    }
    val elapsed = if (isVerifiedRunning && startedAt > 0) (now - startedAt).coerceAtLeast(0) else 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plane),
                contentDescription = null,
                tint = Ink.Signal,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(R.string.app_name_short),
                style = MaterialTheme.typography.titleMedium,
                color = Ink.Text
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = Ink.TextFaint
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineMedium,
                color = Ink.Text
            )
        }

        Text(
            text = "$bindIp:$port · $modeLabel",
            style = MaterialTheme.typography.bodySmall,
            color = Ink.TextDim,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = hintText,
            style = MaterialTheme.typography.bodySmall,
            color = Ink.TextFaint,
            modifier = Modifier.padding(top = 4.dp, end = 24.dp)
        )

        if (isVerifiedRunning && stats.activeSessions > 0) {
            Text(
                text = stringResource(R.string.sessions_count, stats.activeSessions),
                style = MaterialTheme.typography.bodySmall,
                color = Ink.TextFaint,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        Spacer(Modifier.height(26.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Ink.Surface)
                .padding(vertical = 16.dp)
        ) {
            Metric(
                label = stringResource(R.string.stat_up),
                value = formatBytes(stats.upBytes),
                modifier = Modifier.weight(1f)
            )
            MetricDivider()
            Metric(
                label = stringResource(R.string.stat_down),
                value = formatBytes(stats.downBytes),
                modifier = Modifier.weight(1f)
            )
            MetricDivider()
            Metric(
                label = stringResource(R.string.stat_time),
                value = formatDuration(elapsed),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { if (active) disconnect() else connect() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) Ink.SurfaceRaised else Ink.Text,
                contentColor = if (active) Ink.Text else Ink.Black
            )
        ) {
            Text(
                text = if (active) stringResource(R.string.action_disconnect) else stringResource(R.string.action_connect),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(10.dp))

        val telegramOnClick = {
            if (isVerifiedRunning) {
                applyProxyToTelegram(context, proxyUrl, applyMode)
            } else {
                pendingApply = true
                connect()
            }
        }
        val telegramLabel = if (isVerifiedRunning) {
            stringResource(R.string.action_open_telegram)
        } else {
            stringResource(R.string.action_start_and_open)
        }
        val telegramContent: @Composable () -> Unit = {
            Icon(
                painter = painterResource(R.drawable.ic_plane),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = telegramLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
        if (isVerifiedRunning) {
            Button(
                onClick = telegramOnClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink.Signal,
                    contentColor = Ink.Black
                )
            ) {
                telegramContent()
            }
        } else {
            OutlinedButton(
                onClick = telegramOnClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink.Signal)
            ) {
                telegramContent()
            }
        }

        TextButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("proxy", proxyUrl))
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            Text(
                text = stringResource(R.string.action_copy_link),
                style = MaterialTheme.typography.labelLarge,
                color = Ink.TextDim
            )
        }

        lastError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = Ink.Danger,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = Ink.Text,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Ink.TextFaint,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Ink.Line)
    )
}
