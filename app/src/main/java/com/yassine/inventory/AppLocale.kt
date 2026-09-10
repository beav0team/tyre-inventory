package com.yassine.inventory

import android.content.Context
import java.util.Locale

object AppLocale {
    private const val PREFS = "settings"
    private const val KEY_LANG = "lang"

    fun locales(context: Context): List<Locale> {
        val tag = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "")
            .orEmpty()
        if (tag.isBlank()) return emptyList()
        return Locale.forLanguageTag(tag).let { listOf(it) }
    }

    fun currentTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "")
            .orEmpty()

    fun set(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, tag)
            .apply()
    }
}