package com.moviebox.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.moviebox.app.data.Api
import com.moviebox.app.data.Detail
import com.moviebox.app.data.Season
import com.moviebox.app.ui.theme.Divider
import com.moviebox.app.ui.theme.Panel
import com.moviebox.app.ui.theme.Panel2
import com.moviebox.app.ui.theme.TextDim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface DetailState {
    data object Loading : DetailState
    data class Loaded(val detail: Detail, val seasons: List<Season>) : DetailState
    data class Failed(val message: String) : DetailState
}

/**
 * Detail screen: poster + metadata + season/episode picker.
 *
 * Mirrors App.tsx: fetch detail, then — only when the classified type is NOT
 * "Movie" — fetch episodes. Branch on the classified `type`, never raw
 * subjectType (6/7/8 are Anime/ShortDrama/Promo, not plain series).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    subjectId: String,
    initialTitle: String,
    onBack: () -> Unit,
    onPlay: (season: Int, episode: Int, displayTitle: String) -> Unit,
) {
    var state by remember { mutableStateOf<DetailState>(DetailState.Loading) }
    var selectedSeason by remember { mutableIntStateOf(1) }

    androidx.compose.runtime.LaunchedEffect(subjectId) {
        state = DetailState.Loading
        try {
            val detail = withContext(Dispatchers.IO) { Api.getDetail(subjectId) }
            // Movies (and short-dramas with no season data) return [] here —
            // the web client tolerates that too, so don't treat it as an error.
            val seasons = if (detail.isMovie) {
                emptyList()
            } else {
                withContext(Dispatchers.IO) { Api.getEpisodes(subjectId) }
            }
            selectedSeason = seasons.firstOrNull()?.season ?: 1
            state = DetailState.Loaded(detail, seasons)
        } catch (e: Exception) {
            state = DetailState.Failed(e.message ?: "Something went wrong.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val s = state) {
                            is DetailState.Loaded -> s.detail.title
                            else -> initialTitle
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                DetailState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                is DetailState.Failed -> ErrorPane(s.message)

                is DetailState.Loaded -> DetailContent(
                    detail = s.detail,
                    seasons = s.seasons,
                    selectedSeason = selectedSeason,
                    onSelectSeason = { selectedSeason = it },
                    onPlay = onPlay,
                )
            }
        }
    }
}

@Composable
private fun ErrorPane(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(message, color = TextDim, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DetailContent(
    detail: Detail,
    seasons: List<Season>,
    selectedSeason: Int,
    onSelectSeason: (Int) -> Unit,
    onPlay: (Int, Int, String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---- Hero: poster + key facts ---------------------------------
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AsyncImage(
                    model = detail.cover,
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 118.dp, height = 177.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Panel),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = detail.title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    val facts = listOfNotNull(
                        detail.type.takeIf { it.isNotBlank() },
                        detail.releaseDate?.take(4)?.takeIf { it.isNotBlank() },
                        detail.duration?.takeIf { it.isNotBlank() },
                        detail.imdbRating?.takeIf { it.isNotBlank() }?.let { "★ $it" },
                    ).joinToString("  ·  ")
                    if (facts.isNotBlank()) {
                        Text(facts, color = TextDim, style = MaterialTheme.typography.bodySmall)
                    }
                    listOfNotNull(
                        detail.country?.takeIf { it.isNotBlank() },
                        detail.language?.takeIf { it.isNotBlank() },
                        detail.genre?.takeIf { it.isNotBlank() },
                    ).joinToString("  ·  ").takeIf { it.isNotBlank() }?.let {
                        Text(it, color = TextDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // ---- Primary action --------------------------------------------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                androidx.compose.material3.Button(
                    onClick = {
                        val ep = if (detail.isMovie) 0 else 1
                        onPlay(if (detail.isMovie) 0 else selectedSeason, ep, detail.title)
                    },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (detail.isMovie) "Play" else "Play S%02dE01".format(selectedSeason))
                }

                // Dub switcher: same title exists as a separate subject_id per
                // language; tapping swaps to that subject's detail/streams.
                detail.dubs.firstOrNull()?.let { primary ->
                    if (detail.dubs.size > 1) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { onPlay(0, 0, "${detail.title} [${primary.langName}]") },
                        ) {
                            Text("${detail.dubs.size} dubs", maxLines = 1)
                        }
                    }
                }
            }
        }

        // ---- Description -----------------------------------------------
        detail.description?.takeIf { it.isNotBlank() }?.let { desc ->
            item {
                Column {
                    Text(
                        "Synopsis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        desc,
                        color = TextDim,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // ---- Seasons + episodes (skip for movies) ----------------------
        if (seasons.isNotEmpty()) {
            item {
                Text(
                    "Episodes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            // Season chips.
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    seasons.forEach { season ->
                        FilterChip(
                            selected = season.season == selectedSeason,
                            onClick = { onSelectSeason(season.season) },
                            label = { Text("S%02d".format(season.season)) },
                        )
                    }
                }
            }
            // Episode list for the selected season.
            val current = seasons.firstOrNull { it.season == selectedSeason }
            items(
                (1..(current?.episodeCount ?: 0)).toList(),
                key = { "ep-$selectedSeason-$it" },
            ) { ep ->
                EpisodeRow(season = selectedSeason, episode = ep) {
                    onPlay(selectedSeason, ep, detail.title)
                }
            }
        } else if (!detail.isMovie) {
            // Series with no season data — tell the user instead of showing
            // an empty list (matches the web client's tolerance for []).
            item {
                Text(
                    "No episode data available for this title.",
                    color = TextDim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(season: Int, episode: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Panel2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column {
                Text(
                    "Episode %02d".format(episode),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "Season %02d".format(season),
                    color = TextDim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text("Play", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
    }
}
