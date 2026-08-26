package com.maurimax.core.data

import com.maurimax.core.model.ContentRow

/**
 * The live channels the player is allowed to switch between.
 *
 * Handed over in memory rather than through the intent that starts playback:
 * a line of this size is tens of thousands of channels, and an intent that
 * large is refused outright by the system. Both screens run in one process, so
 * a shared reference costs nothing and copies nothing.
 *
 * Empty is a legitimate state — after the process is rebuilt from a
 * notification, say. The player then simply plays what it was given and offers
 * no channel list, rather than failing.
 */
object LiveQueue {

    @Volatile
    private var groups: List<ContentRow> = emptyList()

    /** Called as playback starts, with the live catalogue as it stands. */
    fun set(rows: List<ContentRow>) {
        groups = rows
    }

    fun groups(): List<ContentRow> = groups

    /** Every channel, in the panel's own order, across all groups. */
    fun all(): List<com.maurimax.core.model.MediaItem> = groups.flatMap { it.items }

    fun clear() {
        groups = emptyList()
    }
}
