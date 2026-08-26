package com.maurimax.core.data

/**
 * What the panel said, the last time it said something we could not use.
 *
 * "The server could not be understood" is honest and useless on its own: it
 * covers a block page, a parked domain, a moved endpoint and a panel that
 * serves one client and refuses another. Those need four different fixes, and
 * none of them can be told apart from a phone in Nouakchott.
 *
 * So the failure keeps its receipt, and the sign-in screen can show it.
 * Cleared on the next success, because a stale receipt is worse than none.
 */
object PortalDiagnostics {

    @Volatile
    var last: String? = null
        private set

    fun record(detail: String) {
        last = detail.trim().ifBlank { null }
    }

    fun clear() {
        last = null
    }
}
