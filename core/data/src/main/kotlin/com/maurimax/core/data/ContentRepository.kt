package com.maurimax.core.data

import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.Season
import kotlinx.coroutines.flow.Flow

/**
 * The single seam between the UI and the portal.
 *
 * Rails arrive one at a time rather than all at once. A panel this size cannot
 * hand over its whole catalogue in a request anybody is willing to wait for, so
 * the screen fills as each category lands instead of staying empty until the
 * last one does.
 */
interface ContentRepository {

    /** Emits a growing list of rails. The last emission is the finished tab. */
    fun rows(tab: CatalogTab): Flow<List<ContentRow>>

    /**
     * The episodes of one series, in order.
     *
     * Separate from [rows] because it is the one thing the panel will not hand
     * over in bulk: the series list carries no episodes at all, so this is a
     * request per series and can only be made when a customer opens one.
     *
     * Empty for anything that is not a series.
     */
    suspend fun seasons(item: MediaItem): List<Season> = emptyList()
}
