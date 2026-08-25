package com.maurimax.feature.home

import androidx.annotation.StringRes
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.MediaKind

/**
 * Domain enums carry no words, so the mapping to translated copy lives here —
 * one place to change when a fourth tab or kind appears.
 */
@get:StringRes
internal val CatalogTab.labelRes: Int
    get() = when (this) {
        CatalogTab.LIVE -> R.string.tab_live
        CatalogTab.SPORTS -> R.string.tab_sports
        CatalogTab.MOVIES -> R.string.tab_movies
        CatalogTab.SERIES -> R.string.tab_series
    }

@get:StringRes
internal val MediaKind.labelRes: Int
    get() = when (this) {
        MediaKind.LIVE -> R.string.kind_live
        MediaKind.CATCH_UP -> R.string.kind_catch_up
        MediaKind.MOVIE -> R.string.kind_movie
        MediaKind.SERIES -> R.string.kind_series
    }
