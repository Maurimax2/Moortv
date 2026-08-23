package com.maurimax.core.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Language selection for the app.
 *
 * Arabic is the product's language, not a translation of an English original,
 * so it is the default rather than a fallback. Two things have to be true for
 * that to actually hold:
 *
 *  1. Arabic strings live in `values/`, so they are what any device resolves to
 *     unless it asks for French specifically.
 *  2. The *layout direction* comes from the configuration locale, not from the
 *     strings that happen to be loaded. Without overriding the context, an
 *     English-locale phone renders Arabic text in a left-to-right layout —
 *     right-aligned copy in a mirrored shell is the difference between a
 *     localised product and a translated one.
 */
object AppLocale {

    const val ARABIC = "ar"
    const val FRENCH = "fr"

    private const val PREFS = "maurimax.locale"
    private const val KEY = "language"

    /** Languages the catalogue ships, in order of precedence. */
    val supported = listOf(ARABIC, FRENCH)

    /**
     * Wraps a base context in the chosen language. Call from
     * `Activity.attachBaseContext` so resources and layout direction are both
     * resolved before any view or composable is created.
     */
    fun wrap(base: Context): Context {
        val locale = Locale.forLanguageTag(resolve(base))
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /** The language in force: an explicit choice, else French on a French device, else Arabic. */
    fun resolve(context: Context): String {
        stored(context)?.let { return it }

        val device = context.resources.configuration.locales
            .takeIf { !it.isEmpty }
            ?.get(0)
            ?.language
        return if (device == FRENCH) FRENCH else ARABIC
    }

    fun stored(context: Context): String? =
        prefs(context).getString(KEY, null)?.takeIf { it in supported }

    /** Remembers a choice. The caller recreates the activity to apply it. */
    fun set(context: Context, language: String) {
        require(language in supported) { "unsupported language: $language" }
        prefs(context).edit().putString(KEY, language).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
