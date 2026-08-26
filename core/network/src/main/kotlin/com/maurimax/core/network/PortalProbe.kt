package com.maurimax.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Asks the panel the login question in the plainest way possible and reports
 * exactly what came back.
 *
 * "The server could not be understood" is true but useless: it covers an HTML
 * block page, a parked domain, a 404 from a moved endpoint and a panel that
 * refuses one client while serving another. Those need four different fixes,
 * and nobody can tell them apart from a phone.
 *
 * So this repeats the call with no parsing at all and writes down the status,
 * the content type and the first line of the body. It also repeats it under a
 * couple of different client names, because panels do reject some and serve
 * others — and if one of them works, that alone is the answer.
 *
 * The password never appears in the result.
 */
object PortalProbe {

    /** Clients a panel is most likely to have an opinion about. */
    private val AGENTS = listOf(
        "default" to null,
        "browser" to "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36",
        "player" to "VLC/3.0.20 LibVLC/3.0.20",
    )

    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Runs the probe. Blocking, so callers put it on a background dispatcher.
     *
     * Returns lines meant to be read by a person and pasted into a message —
     * short enough to fit on a phone, specific enough to act on.
     */
    fun run(portalUrl: String, username: String, password: String): String {
        val base = portalUrl.trimEnd('/')
        val url = "$base/player_api.php" +
            "?username=${username.enc()}&password=${password.enc()}"

        val lines = mutableListOf("PANEL $base")

        for ((label, agent) in AGENTS) {
            lines += runCatching { attempt(url, label, agent) }
                .getOrElse { error ->
                    // A failure to connect at all is as much an answer as a
                    // body is, and it is the one people misread most.
                    "$label: ${error.javaClass.simpleName}: ${error.message.orEmpty().take(80)}"
                }
        }

        return lines.joinToString("\n")
    }

    private fun attempt(url: String, label: String, agent: String?): String {
        val request = Request.Builder().url(url).apply {
            if (agent != null) header("User-Agent", agent)
        }.build()

        http.newCall(request).execute().use { response ->
            val type = response.header("Content-Type").orEmpty().substringBefore(';')
            // Only the head of the body: enough to tell JSON from an error
            // page, and it leaves the rest of a large answer unread.
            val head = runCatching { response.peekBody(PEEK).string() }
                .getOrDefault("")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(120)

            val verdict = when {
                head.startsWith("{") || head.startsWith("[") -> "JSON"
                head.isBlank() -> "empty"
                else -> "not JSON"
            }

            return "$label: HTTP ${response.code} $type → $verdict | $head"
        }
    }

    /** How much of the body is worth looking at. */
    private const val PEEK = 200L

    private fun String.enc(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
