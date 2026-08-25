package com.maurimax.core.data

import android.content.Context
import com.maurimax.core.model.Credentials
import com.maurimax.core.network.XtreamApi
import com.maurimax.core.network.XtreamClient
import com.maurimax.core.network.XtreamUrls

/**
 * Hand-rolled object graph. Small enough not to need a DI framework yet; when
 * it stops being small, this is the single place Hilt would replace.
 */
object Graph {

    /** The one portal this build talks to. Never entered by a customer. */
    val portalUrl: String get() = BuildConfig.PORTAL_URL

    private lateinit var appContext: Context

    val api: XtreamApi by lazy { XtreamClient.create(portalUrl) }
    val urls: XtreamUrls by lazy { XtreamUrls(portalUrl) }
    val credentialStore: CredentialStore by lazy { EncryptedCredentialStore(appContext) }
    val authRepository: AuthRepository by lazy { AuthRepository(api, credentialStore) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** False in unit tests, where nothing has initialised the graph. */
    private val ready: Boolean get() = ::appContext.isInitialized

    /** Feeds the sign-in backdrop with artwork from the catalogue just loaded. */
    fun rememberPosters(urls: List<String>) {
        if (ready) PosterMemory.save(appContext, urls)
    }

    fun rememberedPosters(): List<String> =
        if (ready) PosterMemory.load(appContext) else emptyList()

    // ---- library ----------------------------------------------------------
    // Thin passthroughs so the UI never has to hold a Context of its own.

    fun continueWatching(): List<SavedItem> =
        if (ready) Library.continueWatching(appContext) else emptyList()

    fun favourites(): List<SavedItem> =
        if (ready) Library.favourites(appContext) else emptyList()

    fun isFavourite(itemId: String): Boolean =
        if (ready) Library.isFavourite(appContext, itemId) else false

    fun toggleFavourite(item: SavedItem): Boolean =
        if (ready) Library.toggleFavourite(appContext, item) else false

    fun forgetProgress(itemId: String) {
        if (ready) Library.forget(appContext, itemId)
    }

    fun contentRepository(credentials: Credentials): XtreamContentRepository =
        XtreamContentRepository(api = api, urls = urls, credentials = credentials)
}
