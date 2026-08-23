package com.maurimax.core.data

import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow

/**
 * The single seam between the UI and the portal.
 *
 * One call per tab: a panel returns its whole catalog for a section in two
 * requests, so paging per row would be slower, not faster.
 */
interface ContentRepository {
    suspend fun rows(tab: CatalogTab): List<ContentRow>
}
