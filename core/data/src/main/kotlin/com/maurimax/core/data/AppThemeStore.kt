package com.maurimax.core.data

import android.content.Context

/**
 * Light or dark.
 *
 * Dark is the default. Light was tried first, but this portal's channel logos
 * are authored as light marks baked onto black, so on a pale ground every
 * unlogo'd rail became a row of black rectangles. Dark is also what a video
 * catalogue wants: the artwork is the only lit thing on screen. The customer
 * can still switch, and the choice is remembered.
 */
enum class ThemeMode { LIGHT, DARK }

object AppThemeStore {

    private const val PREFS = "maurimax.appearance"
    private const val KEY = "theme"

    fun load(context: Context): ThemeMode =
        when (prefs(context).getString(KEY, null)) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            else -> ThemeMode.DARK
        }

    fun save(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY, mode.name).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
