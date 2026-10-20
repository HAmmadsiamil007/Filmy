package com.moviebox.app.data

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP client — the Android mirror of web/src/api.ts.
 *
 * Endpoints, query params and envelope shapes match the TS client exactly, so
 * a backend change that breaks one breaks the other (they can be diffed
 * side by side).
 */
object Api {

    private val json = Json {
        // Upstream/catalog responses carry keys we don't model; ignore them.
        ignoreUnknownKeys = true
        // Some catalog entries ship empty strings where we expect nulls.
        isLenient = true
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // Large progressive MP4s over a slow link can idle > 15s between reads.
        .readTimeout(60, TimeUnit.SECONDS)
        // Retry idempotent GETs on transient upstream hiccups. The backend
        // already backs off on 429s; this only covers connection-level blips.
        .retryOnConnectionFailure(true)
        .build()

    // ---- Core -------------------------------------------------------------

    private class ApiException(val code: Int, message: String) : IOException(message)

    /**
     * GET [path] against the configured API base and decode [T].
     *
     * @throws IllegalStateException when no API base is configured.
     * @throws ApiException on non-2xx (message carries status + body excerpt).
     * @throws IOException on transport failure.
     */
    private inline fun <reified T> getJson(path: String): T {
        val url = Config.apiUrl(path)
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                val body = resp.body?.string().orEmpty().take(200)
                throw ApiException(
                    resp.code,
                    "${resp.code} ${resp.message}${if (body.isNotEmpty()) " — $body" else ""}"
                )
            }
            val text = resp.body?.string()
                ?: throw IOException("Empty response body from $path")
            return json.decodeFromString(text)
        }
    }

    // ---- Endpoints --------------------------------------------------------

    /**
     * @param includeAdult unfiltered by default, matching the product decision
     *                      and the web client's search().
     */
    fun search(query: String, includeAdult: Boolean = true): List<SearchResult> {
        val httpUrl = Config.apiUrl("/search").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("include_adult", includeAdult.toString())
            ?.build()
            ?: throw IllegalStateException("No API base configured. Set it in Settings.")

        val request = Request.Builder().url(httpUrl).get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                val body = resp.body?.string().orEmpty().take(200)
                throw ApiException(resp.code, "${resp.code} ${resp.message} — $body")
            }
            val text = resp.body?.string() ?: throw IOException("Empty search response")
            return json.decodeFromString<SearchEnvelope>(text).results
        }
    }

    fun getDetail(id: String): Detail = getJson("/detail/$id")

    fun getEpisodes(id: String): List<Season> = getJson("/episodes/$id").seasons

    fun getStreams(id: String, season: Int = 0, episode: Int = 0): List<Stream> =
        getJson("/api/stream/$id?se=$season&ep=$episode").streams

    fun getCaptions(id: String, season: Int = 0, episode: Int = 0): List<Caption> =
        getJson("/api/stream/$id/captions?se=$season&ep=$episode").captions

    fun getHealth(): HealthResponse = getJson("/health")

    // NOTE: rendition dedupe/selection lives in player/QualityLogic.kt
    // (QualityLogic.dedupe). It was briefly duplicated here as selectPlayable()
    // and removed — two copies of the same rule would drift. Callers must use
    // QualityLogic.dedupe(), which is the version covered by QualityLogicTest.
}
