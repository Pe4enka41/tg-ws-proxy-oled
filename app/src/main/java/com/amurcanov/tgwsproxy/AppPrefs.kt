package com.amurcanov.tgwsproxy

import android.content.Context

/**
 * Language is kept outside DataStore because it is needed in
 * Activity.attachBaseContext(), before any coroutine or DataStore access.
 */
object AppPrefs {
    private const val PREFS = "lang_prefs"
    private const val KEY_LANGUAGE = "app_language"

    const val SYSTEM = "system"
    const val RU = "ru"
    const val EN = "en"

    fun language(context: Context): String {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SYSTEM)
        return stored ?: SYSTEM
    }

    fun saveLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }
}
