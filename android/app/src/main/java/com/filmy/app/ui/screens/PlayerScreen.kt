package com.filmy.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.filmy.app.data.Api
import com.filmy.app.data.Config
import com.filmy.app.data.Stream
import com.filmy.app.player.QualityLogic
import com.filmy.app.ui.theme.Panel
import com.filmy.app.ui.theme.Panel2
import com.filmy.app.ui.theme.TextDim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed interface PlaybackState {
    data object Loading : PlaybackState
    data object Ready : PlaybackState
    data class Failed(val message: String) : PlaybackState
}

/**
 * Media3 player screen.
 *
 * ARCHITECTURE NOTE — why we do our own quality switching instead of ExoPlayer's
 * built-in ABR: the backend returns each quality as a SEPARATE progressive MP4
 * URL, not one DASH manifest with multiple representations. ExoPlayer's
 * adaptive track selection only adapts *within* a manifest. So, exactly like
 * the web Player.tsx, we estimate bandwidth, pick a rendition with
 * [QualityLogic], and hot-swap the MediaItem while preserving the playhead.
 * (media3-dash IS included so a future DASH-manifest mode can use native ABR.)
 *
 * Playback goes through /proxy/stream (see Config.proxiedUrl) — the CDN is
 * Referer-locked and the proxy injects the header server-side, so NO custom
 * headers are needed here. That's what makes this identical to web/desktop.
 */
@Composable
fun PlayerScreen(
    subjectId: String,
    title: String,
    season: Int,
    episode: Int,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var state by remember { mutableStateOf<PlaybackState>(PlaybackState.Loading) }
    var renditions by remember { mutableStateOf<List<Stream>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(QualityLogic.Mode.AUTO) }
    var showPicker by remember { mutableStateOf(false) }

    // Buffer-driven adaptation: only stable Player API is used here.
    // (Measuring bytes would need PlaybackStatsListener/TransferListener, which
    // pulls in unstable analytics APIs — buffer health is the adaptive signal.)
    var bufferSec by remember { mutableDoubleStateOf(0.0) }
    var lastSwitchAt by remember { mutableLongStateOf(0L) }
    var playerError by remember { mutableStateOf<String?>(null) }

    // Landscape while playing, portrait restored when leaving. The manifest
    // pins portrait; overriding per-screen keeps browse screens upright and
    // lets video use the full panel.
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // One ExoPlayer instance for the screen's lifetime.
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    // Error surfacing only — adaptation is driven by the buffer poll loop
    // below, so no unstable analytics APIs are needed here.
    val playerListener = remember {
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // PlaybackException extends Exception -> `message` is the
                // readable text (there is NO `errorMessage` property on it;
                // the human-readable form is only reachable via
                // ErrorMessageProvider). errorCode is stable for classification.
                playerError = error.message
                    ?: "Playback failed (code ${error.errorCode})"
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) playerError = null
            }
        }
    }

    DisposableEffect(player, playerListener) {
        player.addListener(playerListener)
        onDispose {
            player.removeListener(playerListener)
            player.release()
        }
    }

    // Load streams once.
    LaunchedEffect(subjectId, season, episode) {
        state = PlaybackState.Loading
        try {
            val raw = withContext(Dispatchers.IO) {
                Api.getStreams(subjectId, season, episode)
            }
            val playable = QualityLogic.dedupe(raw)
            if (playable.isEmpty()) {
                state = PlaybackState.Failed("No playable stream found.")
            } else {
                renditions = playable
                currentIndex = 0
                state = PlaybackState.Ready
            }
        } catch (e: Exception) {
            state = PlaybackState.Failed(e.message ?: "Couldn't load streams.")
        }
    }

    // (Re)start playback when the chosen rendition changes.
    LaunchedEffect(state, currentIndex) {
        if (state != PlaybackState.Ready) return@LaunchedEffect
        val stream = renditions.getOrNull(currentIndex) ?: return@LaunchedEffect
        val resumeAt = try { player.currentPosition } catch (_: Exception) { 0L }
        val hadStarted = resumeAt > 0 && player.mediaItem != null

        val proxyUrl = Config.proxiedUrl(stream.url, stream.referer)
        player.setMediaItem(MediaItem.fromUri(proxyUrl))
        if (hadStarted) player.seekTo(resumeAt) // preserve playhead across switch
        player.prepare()
    }

    // Adaptive loop (AUTO mode only): poll buffer + bandwidth and re-pick.
    LaunchedEffect(state, mode, renditions) {
        if (state != PlaybackState.Ready || mode != QualityLogic.Mode.AUTO) return@LaunchedEffect
        while (true) {
            delay(2_000)
            if (renditions.isEmpty()) continue
            // Drain measure.
            bufferSec = try {
                val buffered = player.bufferedPosition - player.currentPosition
                if (buffered < 0) 0 else buffered / 1000.0
            } catch (_: Exception) {
                0.0
            }

            val target = QualityLogic.pickForBandwidth(
                renditions = renditions,
                mbps = 0.0, // bandwidth probe intentionally omitted (buffer-driven)
                bufferSec = bufferSec,
                currentIndex = currentIndex,
            )
            if (target != currentIndex) {
                val now = System.currentTimeMillis()
                if (now - lastSwitchAt >= QualityLogic.SWITCH_COOLDOWN_MS) {
                    lastSwitchAt = now
                    currentIndex = target
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            PlaybackState.Loading -> CenterPane {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("Loading…", color = Color.White)
            }

            is PlaybackState.Failed -> CenterPane {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    s.message,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
                Spacer(Modifier.height(14.dp))
                androidx.compose.material3.TextButton(onClick = onBack) {
                    Text("Back", color = MaterialTheme.colorScheme.primary)
                }
            }

            PlaybackState.Ready -> {
                // Media3 PlayerView supplies play/pause/seek scrubbber for
                // free; we overlay chrome (back, title, quality) above it.
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            this.player = player
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // ---- Top chrome (auto-hides with the controller) ---------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xB3000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (season > 0 || episode > 0) {
                                Text(
                                    text = "S%02d · E%02d".format(season, episode),
                                    color = TextDim,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        // Live status: mode + selected rung + buffer health.
                        Text(
                            text = buildString {
                                append(if (mode == QualityLogic.Mode.AUTO) "Auto" else "Manual")
                                renditions.getOrNull(currentIndex)?.let {
                                    append(" · ")
                                    append(QualityLogic.labelFor(it))
                                }
                                if (bufferSec > 0) {
                                    append(" · ")
                                    append("%.0fs".format(bufferSec))
                                }
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )

                        // Quality button -> picker.
                        Box {
                            IconButton(onClick = { showPicker = !showPicker }) {
                                Text(
                                    text = "EQ",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }

                // Transient error toast over the video.
                playerError?.let { msg ->
                    Text(
                        text = msg,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 96.dp)
                            .background(Color(0xCCB3261E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }

                // ---- Quality picker -------------------------------------
                if (showPicker) {
                    QualityPicker(
                        renditions = renditions,
                        selectedIndex = currentIndex,
                        mode = mode,
                        onSelect = { index ->
                            // Manual pick pins the rung until Auto is reselected.
                            mode = QualityLogic.Mode.MANUAL
                            lastSwitchAt = System.currentTimeMillis()
                            if (index != currentIndex) currentIndex = index
                            showPicker = false
                        },
                        onSelectAuto = {
                            mode = QualityLogic.Mode.AUTO
                            lastSwitchAt = System.currentTimeMillis()
                            showPicker = false
                        },
                        onDismiss = { showPicker = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterPane(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

/**
 * Bottom-sheet style quality chooser. Auto is the default; picking a rung
 * switches to Manual (mirrors the web player's modeRef override).
 */
@Composable
private fun QualityPicker(
    renditions: List<Stream>,
    selectedIndex: Int,
    mode: QualityLogic.Mode,
    onSelect: (Int) -> Unit,
    onSelectAuto: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .padding(vertical = 10.dp)
                .clickable(enabled = false) { },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Panel2, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.height(8.dp))

            // Auto row.
            PickerRow(
                label = "Auto",
                sub = "Adapt to connection speed",
                selected = mode == QualityLogic.Mode.AUTO,
                onClick = onSelectAuto,
            )

            LazyColumn(modifier = Modifier.height(280.dp)) {
                itemsIndexed(renditions) { index, stream ->
                    PickerRow(
                        label = QualityLogic.labelFor(stream),
                        sub = listOfNotNull(
                            stream.codec.takeIf { it.isNotBlank() },
                            if (stream.sizeMb > 0) "%.0f MB".format(stream.sizeMb) else null,
                        ).joinToString(" · "),
                        selected = mode == QualityLogic.Mode.MANUAL && index == selectedIndex,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRow(label: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                style = MaterialTheme.typography.titleSmall,
            )
            if (sub.isNotBlank()) {
                Text(sub, color = TextDim, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
