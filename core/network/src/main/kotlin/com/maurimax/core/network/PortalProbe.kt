package com.maurimax.core.network

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Callable
import java.util.concurrent.Executors
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

    /**
     * Clients a panel is most likely to have an opinion about.
     *
     * Panels do refuse unknown clients, and some do it with a 404 rather than a
     * 403 so the endpoint looks absent rather than guarded. Safari is here
     * because a plain phone browser is the one client we have seen served.
     */
    private val AGENTS = listOf(
        "default" to null,
        "chrome" to "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36",
        "safari" to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
        "vlc" to "VLC/3.0.20 LibVLC/3.0.20",
        "exo" to "ExoPlayerLib/2.19.1",
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

        val lines = mutableListOf("PANEL $base", addresses(base))

        for ((label, agent) in AGENTS) {
            lines += runCatching { attempt(url, label, agent, http) }
                .getOrElse { error ->
                    // A failure to connect at all is as much an answer as a
                    // body is, and it is the one people misread most.
                    "$label: ${error.javaClass.simpleName}: ${error.message.orEmpty().take(80)}"
                }
        }

        // The two families asked separately. A host behind a CDN can answer on
        // one and not the other, and the resolver's order decides which one a
        // client happens to get — which looks like the app being refused.
        for ((label, client) in listOf("ipv4" to overV4, "ipv6" to overV6)) {
            lines += runCatching { attempt(url, label, null, client) }
                .getOrElse { error ->
                    "$label: ${error.javaClass.simpleName}: ${error.message.orEmpty().take(60)}"
                }
        }

        return lines.joinToString("\n")
    }

    /**
     * Which machine the name points at, from this device.
     *
     * A host with more than one address can serve the panel on one and
     * something else on another, and a phone and a browser do not have to pick
     * the same one — which looks exactly like a panel refusing the app.
     */
    private fun addresses(base: String): String {
        val host = base.substringAfter("://", base).substringBefore('/').substringBefore(':')
        return runCatching {
            val ips = java.net.InetAddress.getAllByName(host).map { it.hostAddress }
            "DNS $host -> ${ips.joinToString(", ")}"
        }.getOrElse { "DNS $host -> ${it.javaClass.simpleName}" }
    }

    /** Only one address family, so each can be judged on its own. */
    private fun family(v4: Boolean) = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .dns { hostname ->
            Dns.SYSTEM.lookup(hostname)
                .filter { (it is java.net.Inet4Address) == v4 }
                .ifEmpty { throw java.net.UnknownHostException("no ${if (v4) "IPv4" else "IPv6"} address") }
        }
        .build()

    private val overV4 by lazy { family(v4 = true) }
    private val overV6 by lazy { family(v4 = false) }

    private fun attempt(url: String, label: String, agent: String?, client: OkHttpClient): String {
        val request = Request.Builder().url(url).apply {
            if (agent != null) header("User-Agent", agent)
        }.build()

        client.newCall(request).execute().use { response ->
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

            // cf-ray only comes back from Cloudflare, so its presence names
            // what is answering without having to guess from an error page.
            val server = listOfNotNull(
                response.header("Server"),
                response.header("cf-ray")?.let { "cf" },
            ).joinToString("/").take(24)
            return "$label: HTTP ${response.code} $type $server → $verdict | $head"
        }
    }

    /** How much of the body is worth looking at. */
    private const val PEEK = 200L

    /**
     * Looks for the panel somewhere other than where we were told it is.
     *
     * A reseller moving a panel to another port is ordinary, and it leaves the
     * domain answering — nginx serves a 404 and the app reports something it
     * cannot act on. Rather than guessing one alternative at a time over a
     * chat, this tries the ports and paths panels actually use and says which
     * ones answer with the API.
     *
     * A diagnostic, run only when the configured address has already failed.
     * Nothing here changes where the app connects: that stays compiled in.
     */
    fun locate(portalUrl: String, username: String, password: String): String {
        val host = portalUrl
            .substringAfter("://", portalUrl)
            .substringBefore('/')
            .substringBefore(':')
            .trim()
        if (host.isBlank()) return "no host to search"

        val candidates = buildList {
            for (port in HTTP_PORTS) for (path in PATHS) add("http://$host:$port/$path")
            for (port in HTTPS_PORTS) for (path in PATHS) add("https://$host:$port/$path")
        }

        val pool = Executors.newFixedThreadPool(8)
        val found = try {
            pool.invokeAll(
                candidates.map { url ->
                    Callable { if (answersWithJson(url, username, password)) url else null }
                },
                40, TimeUnit.SECONDS,
            ).mapNotNull { runCatching { it.get() }.getOrNull() }
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            emptyList()
        } finally {
            pool.shutdownNow()
        }

        return if (found.isEmpty()) {
            "searched ${candidates.size} addresses on $host, none answered with the API"
        } else {
            "FOUND on $host:\n" + found.take(6).joinToString("\n")
        }
    }

    private fun answersWithJson(url: String, username: String, password: String): Boolean {
        val full = "$url?username=${username.enc()}&password=${password.enc()}"
        return runCatching {
            sweeper.newCall(Request.Builder().url(full).build()).execute().use { response ->
                response.isSuccessful &&
                    response.peekBody(64).string().trimStart().startsWith("{")
            }
        }.getOrDefault(false)
    }

    /** Where panels are found when they are not on 80. */
    private val HTTP_PORTS = listOf(80, 8080, 8000, 2052, 2082, 2086, 2095, 8880, 25461)
    private val HTTPS_PORTS = listOf(443, 2053, 2083, 2087, 2096, 8443)

    /** Both spellings the Xtream API has shipped under. */
    private val PATHS = listOf("player_api.php", "panel_api.php")

    /** Short budgets: this is thirty attempts, and most of them will not answer. */
    private val sweeper = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun String.enc(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
