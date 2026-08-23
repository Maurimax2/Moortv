package com.maurimax.core.data

import com.maurimax.core.model.ContentRow
import kotlinx.coroutines.flow.Flow

/**
 * The single seam between the UI and wherever titles actually come from.
 *
 * Everything in the app talks to this interface, never to a network or database
 * directly. Swapping [FakeContentRepository] for a real backend, an IPTV playlist
 * parser, or a metadata provider is then a one-line change in the app modules.
 */
interface ContentRepository {
    fun homeRows(): Flow<List<ContentRow>>
}
