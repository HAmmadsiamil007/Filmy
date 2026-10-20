package com.filmy.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Data models mirroring web/src/api.ts exactly.
//
// Two contract notes carried over from the web client:
//   1. Wire format is snake_case, so every field carries an explicit
//      @SerialName rather than relying on a naming strategy. This keeps the
//      mapping visible and identical to the TS interfaces, and avoids any
//      dependency on experimental kotlinx-serialization APIs.
//   2. Nullable fields default to null so BOTH an absent key and an explicit
//      JSON null decode cleanly (the backend emits null for many of these).

@Serializable
data class SearchResult(
    @SerialName("subject_id") val subjectId: String,
    val title: String,
    /** CLASSIFIED label (Movie/Series/Anime/ShortDrama/Promo), not raw type. */
    val type: String,
    @SerialName("subject_type") val subjectType: Int,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("imdb_rating") val imdbRating: String? = null,
    val genre: String? = null,
    @SerialName("is_adult") val isAdult: Boolean = false,
    val cover: String? = null,
    @SerialName("detail_path") val detailPath: String? = null,
    val language: String? = null,
) {
    /** Branch on this, not raw subjectType (6/7/8 are NOT plain series). */
    val isMovie: Boolean get() = type == "Movie"
}

@Serializable
data class Dub(
    @SerialName("subject_id") val subjectId: String,
    @SerialName("lang_name") val langName: String,
    val language: String,
)

@Serializable
data class Detail(
    @SerialName("subject_id") val subjectId: String,
    val title: String,
    @SerialName("subject_type") val subjectType: Int,
    val type: String,
    val description: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val duration: String? = null,
    @SerialName("imdb_rating") val imdbRating: String? = null,
    val genre: String? = null,
    @SerialName("is_adult") val isAdult: Boolean = false,
    val country: String? = null,
    val language: String? = null,
    val cover: String? = null,
    val dubs: List<Dub> = emptyList(),
) {
    val isMovie: Boolean get() = type == "Movie"
}

@Serializable
data class ResolutionInfo(
    val resolution: Int,
    @SerialName("epNum") val epNum: Int,
)

@Serializable
data class Season(
    val season: Int,
    @SerialName("episode_count") val episodeCount: Int,
    val resolutions: List<ResolutionInfo> = emptyList(),
)

@Serializable
data class Stream(
    val source: String,
    /** "MP4" or "DASH" — but upstream MISLABELS some DASH fragments as MP4. */
    val format: String,
    val quality: String,
    @SerialName("quality_value") val qualityValue: Int,
    @SerialName("resolution_tag") val resolutionTag: String,
    val url: String,
    @SerialName("size_mb") val sizeMb: Double,
    val codec: String,
    @SerialName("duration_sec") val durationSec: Double? = null,
    val cookie: String? = null,
    /** Referer/UA/Origin required by the CDN — injected by /proxy/stream. */
    val headers: Map<String, String> = emptyMap(),
) {
    val referer: String? get() = headers["Referer"]
    val isDash: Boolean get() = format.equals("DASH", ignoreCase = true)
    val isHevc: Boolean get() = codec.contains("hevc", ignoreCase = true) ||
        codec.contains("hvc1", ignoreCase = true)
}

@Serializable
data class Caption(
    val id: String,
    val language: String,
    val code: String,
    val url: String,
    val size: Long? = null,
)

// ---- Envelope types (the backend wraps arrays in keyed objects) ----------

@Serializable
data class SearchEnvelope(val results: List<SearchResult> = emptyList())

@Serializable
data class SeasonsEnvelope(val seasons: List<Season> = emptyList())

@Serializable
data class StreamsEnvelope(val streams: List<Stream> = emptyList())

@Serializable
data class CaptionsEnvelope(val captions: List<Caption> = emptyList())

@Serializable
data class HealthResponse(
    val status: String? = null,
    val version: String? = null,
)
