package com.maurimax.core.data

import android.content.Context

/** Light or dark. Light is the product default; the customer can switch. */
enum class ThemeMode { LIGHT, DARK }

object AppThemeStore {

    private const val PREFS = "maurimax.appearance"
    private const val KEY = "theme"

    fun load(context: Context): ThemeMode =
        when (prefs(context).getString(KEY, null)) {
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.LIGHT
        }

    fun save(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY, mode.name).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
