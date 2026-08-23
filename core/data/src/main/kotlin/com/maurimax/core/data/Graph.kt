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

    fun contentRepository(credentials: Credentials): XtreamContentRepository =
        XtreamContentRepository(api = api, urls = urls, credentials = credentials)
}
