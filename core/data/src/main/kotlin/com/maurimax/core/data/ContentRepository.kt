package com.maurimax.core.data

import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.Season

/**
 * The single seam between the UI and the portal.
 *
 * One call per tab: a panel returns its whole catalog for a section in two
 * requests, so paging per row would be slower, not faster.
 */
interface ContentRepository {
    suspend fun rows(tab: CatalogTab): List<ContentRow>

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
