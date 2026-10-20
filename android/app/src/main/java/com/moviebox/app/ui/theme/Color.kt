package com.moviebox.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette, synced to web/src/styles.css:
//   --bg #0b0d12 · --panel #141821 · --panel-2 #1b2130
//   --text #e7ecf3 · --text-dim #8b94a7
//   --accent #e50914 · --accent-2 #3b82f6
//
// Keeping these as hex constants (not read from resources) means the theme
// compiles without a Resources lookup and stays obviously parallel to the CSS.

val Bg = Color(0xFF0B0D12)
val Panel = Color(0xFF141821)
val Panel2 = Color(0xFF1B2130)
val Text = Color(0xFFE7ECF3)
val TextDim = Color(0xFF8B94A7)
val Accent = Color(0xFFE50914)
val Accent2 = Color(0xFF3B82F6)

// Derived surfaces for elevation/scrims so we don't hand-roll alpha everywhere.
val Scrim = Color(0x99000000)
val Divider = Color(0xFF262D3D)
val OnAccent = Color(0xFFFFFFFF)

// Destructive / failure states (playback error, probe failure). Not in the
// web palette as a var, but matches its #ff8a8a error text feel.
val ErrorRed = Color(0xFFEF4444)
