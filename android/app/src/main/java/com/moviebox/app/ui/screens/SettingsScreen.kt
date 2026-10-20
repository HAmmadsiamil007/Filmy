package com.moviebox.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moviebox.app.data.Api
import com.moviebox.app.data.Config
import com.moviebox.app.ui.theme.Divider
import com.moviebox.app.ui.theme.Panel
import com.moviebox.app.ui.theme.TextDim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface ProbeState {
    data object Idle : ProbeState
    data object Checking : ProbeState
    data class Ok(val version: String?) : ProbeState
    data class Fail(val error: String) : ProbeState
}

/**
 * Backend settings — the Android mirror of web/src/Settings.tsx.
 *
 * Needed because backend hosting is remote/self-hosted (ADR-005): the client
 * ships thin and the user supplies an API base URL.
 *
 * Probe safety mirrors the web client: `test()` applies the draft, probes
 * /health, and REVERTS to the previous value on failure so a bad host is never
 * persisted. `save()` only commits on explicit tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    // Draft starts from the effective base (stored or build-time default).
    var draft by remember { mutableStateOf(Config.apiBase()) }
    var probe by remember { mutableStateOf<ProbeState>(ProbeState.Idle) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun test() {
        probe = ProbeState.Checking
        val previous = Config.apiBase()
        Config.setApiBase(draft)
        scope.launch {
            try {
                val health = withContext(Dispatchers.IO) { Api.getHealth() }
                probe = ProbeState.Ok(health.version)
            } catch (e: Exception) {
                // Revert: never leave a host persisted that failed the probe.
                Config.setApiBase(previous)
                probe = ProbeState.Fail(e.message ?: "Connection failed")
            }
        }
    }

    fun save() {
        Config.setApiBase(draft)
        saved = true
        probe = ProbeState.Ok(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backend") },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Point this client at your self-hosted MovieBox API. " +
                    "Leave blank to fall back to the build-time default.",
                color = TextDim,
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    saved = false
                    probe = ProbeState.Idle
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API base URL") },
                placeholder = { Text("http://127.0.0.1:8000") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Divider,
                    focusedContainerColor = Panel,
                    unfocusedContainerColor = Panel,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = TextDim,
                ),
                shape = RoundedCornerShape(10.dp),
            )

            // ---- Probe status line ------------------------------------
            when (val p = probe) {
                ProbeState.Idle -> if (saved) {
                    StatusLine("✓ Saved", androidx.compose.ui.graphics.Color(0xFF4ADE80))
                }

                ProbeState.Checking -> StatusLine("Checking…", TextDim)
                is ProbeState.Ok -> StatusLine(
                    "✓ Connected" + (p.version?.let { " · v$it" } ?: ""),
                    androidx.compose.ui.graphics.Color(0xFF4ADE80),
                )
                is ProbeState.Fail -> StatusLine("✗ ${p.error}", MaterialTheme.colorScheme.error)
            }

            // ---- Current effective base (readout) ----------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(10.dp))
                    .padding(14.dp),
            ) {
                Text(
                    "Effective base",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextDim,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Config.apiBase().ifEmpty { "(none — set one above)" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            // ---- Actions ----------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { test() },
                    enabled = probe != ProbeState.Checking,
                    modifier = Modifier.weight(1f),
                ) {
                    if (probe == ProbeState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Test")
                    }
                }
                Button(
                    onClick = { save() },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            if (text.startsWith("✓")) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = color,
        )
        Text(text, color = color, style = MaterialTheme.typography.bodySmall)
    }
}
