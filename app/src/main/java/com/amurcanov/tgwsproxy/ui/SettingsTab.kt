package com.amurcanov.tgwsproxy.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amurcanov.tgwsproxy.AppPrefs
import com.amurcanov.tgwsproxy.BuildConfig
import com.amurcanov.tgwsproxy.ProxyService
import com.amurcanov.tgwsproxy.R
import com.amurcanov.tgwsproxy.SettingsStore
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onLanguageChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isRunning by ProxyService.isRunning.collectAsStateWithLifecycle()

    val port by settingsStore.port.collectAsStateWithLifecycle(initialValue = "1445")
    val bindIp by settingsStore.bindIp.collectAsStateWithLifecycle(initialValue = "127.0.0.1")
    val poolSize by settingsStore.poolSize.collectAsStateWithLifecycle(initialValue = 4)
    val cfEnabled by settingsStore.cfproxyEnabled.collectAsStateWithLifecycle(initialValue = true)
    val customDomainEnabled by settingsStore.customCfDomainEnabled.collectAsStateWithLifecycle(initialValue = false)
    val customDomain by settingsStore.customCfDomain.collectAsStateWithLifecycle(initialValue = "")
    val autoStart by settingsStore.autoStartOnBoot.collectAsStateWithLifecycle(initialValue = false)
    val autoApply by settingsStore.autoApplyOnStart.collectAsStateWithLifecycle(initialValue = false)
    val applyMode by settingsStore.applyMode.collectAsStateWithLifecycle(initialValue = "packages")
    val secret by settingsStore.secretKey.collectAsStateWithLifecycle(initialValue = "")

    var showPort by rememberSaveable { mutableStateOf(false) }
    var showBindIp by rememberSaveable { mutableStateOf(false) }
    var showDomain by rememberSaveable { mutableStateOf(false) }
    var showSecret by rememberSaveable { mutableStateOf(false) }

    val lockHint = stringResource(R.string.settings_stop_first)
    val locked = { if (isRunning) Toast.makeText(context, lockHint, Toast.LENGTH_SHORT).show() }
    val language = remember { AppPrefs.language(context) }
    val languageLabel = when (language) {
        AppPrefs.RU -> stringResource(R.string.lang_ru)
        AppPrefs.EN -> stringResource(R.string.lang_en)
        else -> stringResource(R.string.lang_system)
    }
    val applyClientsLabel = stringResource(R.string.apply_clients)
    val applyLinkLabel = stringResource(R.string.apply_link)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineSmall,
            color = Ink.Text,
            modifier = Modifier.padding(top = 14.dp)
        )

        if (isRunning) {
            Text(
                text = stringResource(R.string.settings_running_banner),
                style = MaterialTheme.typography.bodySmall,
                color = Ink.Signal,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(22.dp))

        SectionLabel(stringResource(R.string.settings_connection))
        SettingsGroup {
            SettingRow(
                title = stringResource(R.string.setting_port),
                value = port,
                enabled = !isRunning,
                onClick = { if (isRunning) locked() else showPort = true }
            )
            SettingRow(
                title = stringResource(R.string.setting_bind_ip),
                value = bindIp,
                enabled = !isRunning,
                onClick = { if (isRunning) locked() else showBindIp = true }
            )
            SettingRow(
                title = stringResource(R.string.setting_cloudflare),
                subtitle = stringResource(R.string.setting_cloudflare_desc),
                enabled = !isRunning,
                trailing = {
                    OledSwitch(
                        checked = cfEnabled,
                        enabled = !isRunning,
                        onCheckedChange = {
                            scope.launch { settingsStore.saveCfProxyEnabled(it) }
                        }
                    )
                }
            )
            if (cfEnabled) {
                SettingRow(
                    title = stringResource(R.string.setting_cf_domain),
                    value = if (customDomainEnabled && customDomain.isNotBlank()) customDomain else stringResource(R.string.setting_default),
                    enabled = !isRunning,
                    onClick = { if (isRunning) locked() else showDomain = true }
                )
            }
            SettingRow(
                title = stringResource(R.string.setting_pool),
                subtitle = stringResource(R.string.setting_pool_desc),
                trailing = {
                    Segmented(
                        options = listOf("2", "4", "6"),
                        selected = poolSize.toString(),
                        enabled = !isRunning,
                        onSelect = { scope.launch { settingsStore.savePoolSize(it.toInt()) } }
                    )
                },
                showDivider = false
            )
        }

        SectionLabel(stringResource(R.string.settings_behavior))
        SettingsGroup {
            ApplyModeBlock(
                title = stringResource(R.string.setting_apply_mode),
                subtitle = if (applyMode == "packages") {
                    stringResource(R.string.apply_clients_desc)
                } else {
                    stringResource(R.string.apply_link_desc)
                },
                options = listOf(applyClientsLabel, applyLinkLabel),
                selected = if (applyMode == "packages") applyClientsLabel else applyLinkLabel,
                onSelect = { label ->
                    val mode = if (label == applyClientsLabel) "packages" else "link"
                    scope.launch { settingsStore.saveApplyMode(mode) }
                }
            )
            SettingRow(
                title = stringResource(R.string.setting_auto_apply),
                subtitle = stringResource(R.string.setting_auto_apply_desc),
                trailing = {
                    OledSwitch(
                        checked = autoApply,
                        onCheckedChange = { scope.launch { settingsStore.saveAutoApplyOnStart(it) } }
                    )
                }
            )
            SettingRow(
                title = stringResource(R.string.setting_autostart),
                subtitle = stringResource(R.string.setting_autostart_desc),
                trailing = {
                    OledSwitch(
                        checked = autoStart,
                        onCheckedChange = { scope.launch { settingsStore.saveAutoStartOnBoot(it) } }
                    )
                }
            )
            SettingRow(
                title = stringResource(R.string.setting_language),
                value = languageLabel,
                onClick = {
                    val next = when (language) {
                        AppPrefs.SYSTEM -> AppPrefs.RU
                        AppPrefs.RU -> AppPrefs.EN
                        else -> AppPrefs.SYSTEM
                    }
                    onLanguageChanged(next)
                },
                showDivider = false
            )
        }

        SectionLabel(stringResource(R.string.settings_security))
        SettingsGroup {
            SettingRow(
                title = stringResource(R.string.setting_secret),
                value = if (secret.length >= 8) "dd…${secret.takeLast(6)}" else "dd…",
                onClick = { showSecret = true },
                showDivider = false
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WS Proxy ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = Ink.TextFaint
            )
            Text(
                text = "GitHub",
                style = MaterialTheme.typography.labelSmall,
                color = Ink.TextDim,
                modifier = Modifier.clickable {
                    openUrlInBrowser(context, "https://github.com/Pe4enka41/tg-ws-proxy-oled")
                }
            )
        }
    }

    if (showPort) {
        InputDialog(
            title = stringResource(R.string.setting_port),
            initial = port,
            keyboardType = KeyboardType.Number,
            filter = { it.filter(Char::isDigit).take(5) },
            onDismiss = { showPort = false },
            onSave = {
                val value = it.toIntOrNull()
                if (value == null || value !in 1..65535) {
                    Toast.makeText(context, R.string.invalid_port, Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch { settingsStore.savePort(it) }
                    showPort = false
                }
            }
        )
    }

    if (showBindIp) {
        InputDialog(
            title = stringResource(R.string.setting_bind_ip),
            initial = bindIp,
            keyboardType = KeyboardType.Uri,
            filter = { it.filter { ch -> ch.isDigit() || ch == '.' } },
            onDismiss = { showBindIp = false },
            onSave = {
                if (it.count { ch -> ch == '.' } == 3) {
                    scope.launch { settingsStore.saveBindIp(it) }
                    showBindIp = false
                } else {
                    Toast.makeText(context, R.string.invalid_ip, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showDomain) {
        InputDialog(
            title = stringResource(R.string.setting_cf_domain),
            initial = customDomain,
            keyboardType = KeyboardType.Uri,
            filter = { it.filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '-' } },
            onDismiss = { showDomain = false },
            onSave = {
                scope.launch { settingsStore.saveCustomCfDomain(it.isNotBlank(), it.trim()) }
                showDomain = false
            }
        )
    }

    if (showSecret) {
        SecretDialog(
            secret = secret,
            canRegenerate = !isRunning,
            onRegenerate = {
                val bytes = ByteArray(16)
                java.security.SecureRandom().nextBytes(bytes)
                val generated = bytes.joinToString("") { "%02x".format(it) }
                scope.launch { settingsStore.saveSecretKey(generated) }
            },
            onDismiss = { showSecret = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Ink.TextFaint,
        modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .background(Ink.Surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp)
    ) {
        content()
    }
}

@Composable
private fun ApplyModeBlock(
    title: String,
    subtitle: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink.Text
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Ink.TextFaint,
            modifier = Modifier.padding(top = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Segmented(
                options = options,
                selected = selected,
                onSelect = onSelect
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Ink.LineSoft)
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (subtitle == null) 54.dp else 66.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink.Text
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink.TextFaint,
                        modifier = Modifier.padding(top = 2.dp, end = 12.dp)
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.TextDim,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 12.dp)
                )
                if (onClick != null) {
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink.TextFaint,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else if (onClick != null) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink.TextFaint
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Ink.LineSoft)
            )
        }
    }
}

@Composable
private fun OledSwitch(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Ink.Black,
            checkedTrackColor = Ink.Signal,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Ink.TextDim,
            uncheckedTrackColor = Ink.SurfaceRaised,
            uncheckedBorderColor = Ink.Line
        )
    )
}

@Composable
private fun Segmented(
    options: List<String>,
    selected: String,
    enabled: Boolean = true,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .background(Ink.Black, RoundedCornerShape(9.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val active = option == selected
            Text(
                text = option,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) Ink.Black else Ink.TextDim,
                modifier = Modifier
                    .background(
                        if (active) Ink.Text else Color.Transparent,
                        RoundedCornerShape(7.dp)
                    )
                    .clickable(enabled = enabled) { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun InputDialog(
    title: String,
    initial: String,
    keyboardType: KeyboardType,
    filter: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink.Surface, RoundedCornerShape(20.dp))
                .border(1.dp, Ink.Line, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Ink.Text
            )
            Spacer(Modifier.height(18.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = filter(it) },
                singleLine = true,
                textStyle = TextStyle(
                    color = Ink.Text,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start
                ),
                cursorBrush = SolidColor(Ink.Signal),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ink.Black, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = Ink.TextDim)
                }
                TextButton(onClick = { onSave(text.trim()) }) {
                    Text(stringResource(R.string.save), color = Ink.Signal)
                }
            }
        }
    }
}

@Composable
private fun SecretDialog(
    secret: String,
    canRegenerate: Boolean,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink.Surface, RoundedCornerShape(20.dp))
                .border(1.dp, Ink.Line, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.setting_secret),
                style = MaterialTheme.typography.titleLarge,
                color = Ink.Text
            )
            Text(
                text = stringResource(R.string.secret_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Ink.TextFaint,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "dd$secret",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = Ink.TextDim,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ink.Black, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("secret", "dd$secret"))
                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.copy), tint = Ink.TextDim, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onRegenerate,
                    enabled = canRegenerate
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.regenerate),
                        tint = if (canRegenerate) Ink.TextDim else Ink.Line,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close), color = Ink.TextDim)
                }
            }
        }
    }
}
