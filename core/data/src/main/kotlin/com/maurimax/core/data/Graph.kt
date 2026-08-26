package com.maurimax.core.data

import android.content.Context
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.Credentials
import com.maurimax.core.model.MediaItem
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
    val authRepository: AuthRepository by lazy {
        AuthRepository(api, credentialStore, portalUrl)
    }

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

    // ---- accounts ---------------------------------------------------------

    /**
     * Whose lists to read. Every saved list belongs to one account, so this is
     * the key to all of them; blank while signed out, when there is nothing
     * personal to show anyway.
     */
    val activeUser: String get() = if (ready) credentialStore.load()?.username.orEmpty() else ""

    fun savedAccounts(): List<Credentials> = if (ready) credentialStore.all() else emptyList()

    /** Removes an account and everything it saved. */
    fun forgetAccount(username: String) {
        if (!ready) return
        credentialStore.forget(username)
        Library.erase(appContext, username)
        CatalogCache.erase(appContext, username)
    }

    // ---- catalogue cache --------------------------------------------------

    fun cachedRows(tab: CatalogTab): List<ContentRow> =
        if (ready) CatalogCache.load(appContext, activeUser, tab) else emptyList()

    fun cacheRows(tab: CatalogTab, rows: List<ContentRow>) {
        if (ready) CatalogCache.save(appContext, activeUser, tab, rows)
    }

    // ---- library ----------------------------------------------------------
    // Thin passthroughs so the UI never has to hold a Context of its own.

    fun continueWatching(): List<SavedItem> =
        if (ready) Library.continueWatching(appContext, activeUser) else emptyList()

    fun favourites(): List<SavedItem> =
        if (ready) Library.favourites(appContext, activeUser) else emptyList()

    fun isFavourite(itemId: String): Boolean =
        if (ready) Library.isFavourite(appContext, activeUser, itemId) else false

    fun toggleFavourite(item: SavedItem): Boolean =
        if (ready) Library.toggleFavourite(appContext, activeUser, item) else false

    fun forgetProgress(itemId: String) {
        if (ready) Library.forget(appContext, activeUser, itemId)
    }

    fun resumePosition(itemId: String): Long =
        if (ready) Library.resumePosition(appContext, activeUser, itemId) else 0L

    fun recordProgress(item: SavedItem) {
        if (ready) Library.recordProgress(appContext, activeUser, item)
    }

    // ---- downloads --------------------------------------------------------

    fun downloads(): List<Download> =
        if (ready) Downloads.all(appContext, activeUser) else emptyList()

    fun startDownload(item: MediaItem) {
        if (ready) Downloads.start(appContext, activeUser, item)
    }

    fun removeDownload(itemId: String) {
        if (ready) Downloads.remove(appContext, activeUser, itemId)
    }

    fun isDownloaded(itemId: String): Boolean =
        if (ready) Downloads.isDownloaded(appContext, activeUser, itemId) else false

    /**
     * The copy on this device, if there is one. Playback asks for this first,
     * so a downloaded film costs nothing to watch and works with no connection.
     */
    fun localUrl(itemId: String): String? =
        if (ready) Downloads.localUrl(appContext, activeUser, itemId) else null

    fun contentRepository(credentials: Credentials): XtreamContentRepository =
        XtreamContentRepository(api = api, urls = urls, credentials = credentials)
}
