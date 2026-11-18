package com.amurcanov.tgwsproxy.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.amurcanov.tgwsproxy.R

val telegramPackages = listOf(
    "org.telegram.messenger",
    "com.radolyn.ayugram",
    "com.exteragram.messenger",
    "org.telegram.plus",
    "ir.ilmili.telegraph",
    "org.telegram.BifToGram",
    "tw.nekomimi.nekogram",
    "xyz.nextalone.nagram",
    "uz.unnarsx.cherrygram",
    "org.telegram.mdgram",
    "org.forkclient.messenger.beta",
    "app.nicegram",
    "top.qwq2333.nullgram",
    "com.iMe.android",
    "ru.dahl.messenger",
    "com.scriptsaz.litegram",
    "org.thunderdog.challegram"
)

/**
 * "link" opens the system chooser, "packages" targets every installed Telegram
 * client directly, which skips the browser and lands in the proxy confirmation.
 */
fun applyProxyToTelegram(context: Context, url: String, mode: String) {
    if (mode == "link") {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.telegram_not_found, Toast.LENGTH_SHORT).show()
        }
        return
    }

    val pm = context.packageManager
    val available = telegramPackages.filter {
        try {
            pm.getPackageInfo(it, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    if (available.isEmpty()) {
        Toast.makeText(context, R.string.telegram_not_found, Toast.LENGTH_SHORT).show()
        return
    }

    val intents = available.map { pkg ->
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { setPackage(pkg) }
    }

    try {
        if (intents.size == 1) {
            context.startActivity(intents.first().apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } else {
            val chooser = Intent.createChooser(intents.first(), context.getString(R.string.choose_client))
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.drop(1).toTypedArray())
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    } catch (_: Exception) {
        Toast.makeText(context, R.string.telegram_open_failed, Toast.LENGTH_SHORT).show()
    }
}
