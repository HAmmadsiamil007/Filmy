package com.filmy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.filmy.app.data.Api
import com.filmy.app.data.SearchResult
import com.filmy.app.ui.theme.Divider
import com.filmy.app.ui.theme.Panel
import com.filmy.app.ui.theme.TextDim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Loaded(val results: List<SearchResult>) : SearchState
    data class Failed(val message: String) : SearchState
}

/**
 * Home = search field + poster grid. Mirrors the web App.tsx flow: search →
 * tap a result → detail. No catalog browse yet (web has none either); the
 * /movies and /tv-series endpoints exist on the backend and are a backlog item.
 */
@Composable
fun HomeScreen(
    onOpenDetail: (id: String, title: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }

    val runSearch: (String) -> Unit = { q ->
        if (q.isBlank()) {
            state = SearchState.Idle
        } else {
            state = SearchState.Loading
            // Kick off; result assignment happens on the main thread below.
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- Top bar ------------------------------------------------------
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    // Live-search like the web client (submit also works).
                    runSearch(it)
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search movies, series, anime…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Divider,
                    focusedContainerColor = Panel,
                    unfocusedContainerColor = Panel,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                ),
                shape = RoundedCornerShape(10.dp),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Backend settings",
                    tint = TextDim,
                )
            }
        }

        // ---- Body ---------------------------------------------------------
        Box(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                SearchState.Idle -> EmptyHint()

                SearchState.Loading -> Loading()

                is SearchState.Failed -> ErrorState(s.message) {
                    state = SearchState.Idle
                }

                is SearchState.Loaded -> {
                    if (s.results.isEmpty()) {
                        EmptyHint("No results for “$query”")
                    } else {
                        ResultsGrid(s.results, onOpenDetail)
                    }
                }
            }
        }
    }

    // Single async entry point for searches. Runs off the main thread so a slow
    // backend can't jank the search field mid-typing.
    androidx.compose.runtime.LaunchedEffect(query) {
        val q = query
        if (q.isBlank()) return@LaunchedEffect
        state = SearchState.Loading
        try {
            val results = withContext(Dispatchers.IO) { Api.search(q) }
            // Guard: query may have changed while this was in flight.
            if (q == query) state = SearchState.Loaded(results)
        } catch (e: Exception) {
            if (q == query) {
                state = SearchState.Failed(e.message ?: "Something went wrong.")
            }
        }
    }
}

@Composable
private fun EmptyHint(message: String = "Search for something to get started.") {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Movie,
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.TextButton(onClick = onRetry) {
                Text("Retry", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ResultsGrid(
    results: List<SearchResult>,
    onOpenDetail: (String, String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(results, key = { it.subjectId }) { item ->
            PosterCard(item = item, onClick = { onOpenDetail(item.subjectId, item.title) })
        }
    }
}

@Composable
private fun PosterCard(item: SearchResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.cover,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f) // poster ratio, matches web grid
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
        )
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(
                item.type.takeIf { it.isNotBlank() },
                item.releaseDate?.take(4)?.takeIf { it.isNotBlank() },
                item.imdbRating?.takeIf { it.isNotBlank() }?.let { "★ $it" },
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
