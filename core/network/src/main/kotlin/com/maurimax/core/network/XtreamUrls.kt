package com.maurimax.core.network

/**
 * Builds playable stream URLs for a panel. These are not API calls — the panel
 * serves media straight off path-encoded credentials, which is why the exact
 * shape matters and is unit tested.
 */
class XtreamUrls(portalUrl: String) {

    /** Normalised once so callers never have to worry about a trailing slash. */
    private val base: String = portalUrl.trimEnd('/')

    /**
     * Live TV. `.m3u8` is HLS and what Media3 handles best; `.ts` is the raw
     * MPEG-TS some panels prefer. Both are valid on a standard panel.
     */
    fun live(username: String, password: String, streamId: Int, extension: String = "m3u8"): String =
        "$base/live/${username.enc()}/${password.enc()}/$streamId.$extension"

    /** A film. The extension comes from the panel's own `container_extension`. */
    fun movie(username: String, password: String, streamId: Int, containerExtension: String): String =
        "$base/movie/${username.enc()}/${password.enc()}/$streamId.${containerExtension.ifBlank { "mp4" }}"

    /** One episode. Note the id is the episode id, not the series id. */
    fun episode(username: String, password: String, episodeId: Int, containerExtension: String): String =
        "$base/series/${username.enc()}/${password.enc()}/$episodeId.${containerExtension.ifBlank { "mp4" }}"

    /** The full XMLTV guide for the account. */
    fun xmltv(username: String, password: String): String =
        "$base/xmltv.php?username=${username.enc()}&password=${password.enc()}"

    private fun String.enc(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
