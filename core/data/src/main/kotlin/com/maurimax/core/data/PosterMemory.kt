package com.maurimax.core.data

import android.content.Context

/**
 * Poster URLs kept from the last time the catalogue loaded.
 *
 * The sign-in screen wants to be built out of the service's own artwork, but it
 * runs before there is a session to fetch any. Remembering a handful from the
 * previous visit solves that: the first launch on a device is plain, and every
 * launch after it is a wall of the catalogue the customer is signing in to.
 */
object PosterMemory {

    private const val PREFS = "maurimax.posters"
    private const val KEY = "urls"
    private const val KEEP = 15

    fun save(context: Context, urls: List<String>) {
        val kept = urls.filter { it.isNotBlank() }.distinct().take(KEEP)
        if (kept.isEmpty()) return
        prefs(context).edit().putString(KEY, kept.joinToString("\n")).apply()
    }

    fun load(context: Context): List<String> =
        prefs(context).getString(KEY, null)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
