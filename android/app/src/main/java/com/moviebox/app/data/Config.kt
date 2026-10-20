package com.moviebox.app.data

import android.content.Context
import android.content.SharedPreferences
import com.moviebox.app.BuildConfig

/**
 * Runtime-configurable API endpoint — the Android mirror of web/src/config.ts.
 *
 * Backend hosting decision (ADR-005): clients ship THIN and point at a
 * user-supplied API base URL. This is persisted to SharedPreferences, the
 * Android equivalent of localStorage.
 *
 * PLATFORM DIFFERENCE vs the web client — this matters:
 *   On the web, an empty base means "same origin", which the Vite dev proxy
 *   and any reverse proxy handle. Android HAS no origin, so an empty base is
 *   meaningless. Here an empty stored value falls back to
 *   [BuildConfig.DEFAULT_API_BASE] (settable at build time via
 *   -PdefaultApiBase=...), and if BOTH are blank we surface an explicit
 *   "configure it in Settings" error instead of silently building a broken URL.
 */
object Config {

    private const val PREFS = "moviebox"
    private const val KEY_API_BASE = "moviebox.apiBase" // same key as web

    @Volatile
    private var cached: String? = null

    private var prefs: SharedPreferences? = null

    /** Call once from Application so the object has a Context. */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** Strip trailing slashes so path joining never yields "//". */
    private fun normalize(raw: String): String = raw.trim().trimEnd('/')

    /**
     * The effective API base, or an empty string when nothing is configured.
     * Empty means "not configured" on Android — check [isConfigured].
     */
    fun apiBase(): String {
        cached?.let { return it }
        val stored = normalize(prefs?.getString(KEY_API_BASE, "") ?: "")
        val resolved = if (stored.isNotEmpty()) stored else normalize(BuildConfig.DEFAULT_API_BASE)
        cached = resolved
        return resolved
    }

    /** True when a usable API base exists (stored or baked in at build time). */
    fun isConfigured(): Boolean = apiBase().isNotEmpty()

    /** Persist a new base. Blank clears it (back to build-time default). */
    fun setApiBase(raw: String): String {
        val value = normalize(raw)
        cached = value
        val p = prefs ?: return value
        if (value.isEmpty()) p.edit().remove(KEY_API_BASE).apply()
        else p.edit().putString(KEY_API_BASE, value).apply()
        return value
    }

    /** Drop the in-memory cache after an external prefs change. */
    fun invalidate() {
        cached = null
    }

    /**
     * Resolve an API path to an absolute URL.
     *
     * @throws IllegalStateException when no base is configured — callers turn
     *         this into a UI error rather than firing a request at a bad host.
     */
    fun apiUrl(path: String): String {
        val base = apiBase()
        require(base.isNotEmpty()) {
            "No API base configured. Set it in Settings."
        }
        return base + if (path.startsWith("/")) path else "/$path"
    }

    /**
     * Wrap a stream URL for playback — the mirror of proxiedUrl() in api.ts.
     *
     * Video MUST go through /proxy/stream because the CDN is Referer-locked
     * and ExoPlayer setting headers itself would only fix THIS platform; the
     * proxy fixes all four clients at once (ADR-001).
     *
     * The target follows [apiBase], never the app's own location — with a
     * remote backend the bytes come from the API host.
     */
    fun proxiedUrl(url: String, referer: String? = null): String {
        val target = apiUrl("/proxy/stream")
        val encoded = buildString {
            append(target)
            append(if (target.contains("?")) "&" else "?")
            append("url=")
            append(encodeQuery(url))
            if (!referer.isNullOrBlank()) {
                append("&referer=")
                append(encodeQuery(referer))
            }
        }
        return encoded
    }

    /**
     * Percent-encode for a query component. Kept dependency-free rather than
     * pulling in OkHttp's HttpUrl just for this (it IS still used elsewhere).
     */
    private fun encodeQuery(value: String): String {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c.isLetterOrDigit() || c in "-_.~") {
                sb.append(c)
            } else {
                sb.append('%')
                sb.append(((b.toInt() shr 4) and 0xF).toString(16).uppercase())
                sb.append((b.toInt() and 0xF).toString(16).uppercase())
            }
        }
        return sb.toString()
    }
}
