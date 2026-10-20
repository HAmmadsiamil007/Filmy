# Android Project (phone APK)

WRITTEN 2026-10-20 at android/. NEVER COMPILED - no Android SDK/Gradle/ANDROID_HOME in this env.

# Status
14 .kt files (87.9 KB) + full Gradle config + resources + 17 unit tests.
Package declarations verified to match file paths. Import sweep clean.

# CRITICAL: unverified, first build WILL likely surface compile errors
The authoring env has no toolchain, so this code was reviewed by hand, not compiled.
android/README.md has a Status section listing every bug caught during authoring.
Bugs already caught by review (each was real):
- PlaybackException.errorMessage does not exist -> use message / errorCode.
- AnalyticsListener.onPlaybackStatsUpdate phantom signature (extra EventSender param,
  and the method is not reliably on that interface). Whole byte-counting sampler REMOVED.
- network_security_config.xml CIDR ranges (192.168.0.0/16) are INVALID on Android -> replaced
  with base-config cleartextTrafficPermitted=true plus a documented rationale.
- Missing imports: background, mutableLongStateOf. Unresolved symbol Color_Error.
- Invalid Kotlin: a fully-qualified call written as a method chain (.androidx.compose...).
- selectPlayable() duplicated QualityLogic.dedupe() and was dead -> removed.

# Architecture decisions
- Buffer-driven adaptation, NOT bandwidth probing. Uses only stable Player.bufferedPosition/
  currentPosition. Measuring bytes would need PlaybackStatsListener/TransferListener (unstable
  analytics APIs) - that path was tried and abandoned. mbps param still exists, pass 0.0.
- QualityLogic.kt is PURE (no Android imports) so it is unit-testable and diffable vs web Player.tsx.
- media3-dash IS a dependency even though we play progressive MP4, so a future DASH-manifest
  mode gets native ABR for free.
- Single-activity Compose + NavHost. Player forces landscape via activity.requestedOrientation
  in a DisposableEffect; manifest pins portrait otherwise.

# Files
- data/Config.kt - mirrors web/src/config.ts. SharedPreferences key moviebox.apiBase.
  DIFFERENCE vs web: empty base is meaningless on Android (no origin) so it falls back to
  BuildConfig.DEFAULT_API_BASE (-PdefaultApiBase=...), then errors with an actionable message.
- data/Models.kt - @Serializable with EXPLICIT @SerialName on every snake_case field (no naming
  strategy - avoids experimental kotlinx-serialization APIs). Nullable fields default null so
  both absent-key and JSON-null decode.
- data/Api.kt - OkHttp. search/getDetail/getEpisodes/getStreams/getCaptions/getHealth.
- ui/screens/{Home,Detail,Player,Settings}Screen.kt, ui/nav/Nav.kt, ui/theme/
- app/src/test/.../QualityLogicTest.kt - 17 tests, pure JVM, run via testDebugUnitTest.

# Verification performed
- Assertions traced against an independent JS reimplementation of pickForBandwidth: 9/9 pass.
- package-vs-path check: all match.
- import sweep across all .kt: clean.

# Related
mem:architecture/clients, mem:frontend/player_quality (web twin), mem:architecture/remote_hosting,
mem:legal_risks, mem:architecture/desktop_tauri