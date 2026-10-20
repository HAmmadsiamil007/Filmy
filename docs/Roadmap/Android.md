---
tags: [roadmap, android, apk, tv]
status: in-progress
priority: medium
depends_on: [ADR-003, ADR-001, ADR-004, ADR-005]
verified: false
last_updated: 2026-10-20
---

# Android — phone APK + TV

> [!info] Codebase #2
> Kotlin + Jetpack Compose + **Media3/ExoPlayer**. TV shares this codebase via **Compose for TV** (Leanback). See [[Decisions/ADR-003 Two codebases]].

## Status — phone APK WRITTEN, NOT COMPILED (2026-10-20)

> [!warning] Never compiled
> The authoring environment has **no Android SDK / Gradle / `ANDROID_HOME`** (verified 2026-10-20). Everything below was written and hand-reviewed; **the first `assembleDebug` is the real test and will likely surface a few compile errors.** That's expected and documented.

**What exists** — `android/`, 14 Kotlin files (~88 KB) + Gradle config + resources + 17 unit tests.

| Area | State |
| --- | --- |
| Gradle (version catalog, wrapper pin, AGP 8.7.3) | ✅ written |
| Resources (manifest, themes, strings, adaptive + legacy icons) | ✅ written |
| Data layer (`Config`/`Models`/`Api`) | ✅ written, mirrors `api.ts` |
| `QualityLogic` (adaptive + manual) | ✅ written, **9/9 traced assertions pass** |
| Screens (Home/Detail/Player/Settings) + Nav | ✅ written |
| `QualityLogicTest` (17 tests, pure JVM) | ✅ written |
| **Compile + run** | ⬜ **blocked on Android SDK** |

### Bugs caught during authoring (by review, not compilation)

> [!failure] Each was a real defect
> 1. `PlaybackException.errorMessage` **doesn't exist** → corrected to `message` / `errorCode`.
> 2. `AnalyticsListener.onPlaybackStatsUpdate(EventSender, …)` → phantom signature; the whole byte-counting sampler was **removed** in favor of buffer-driven adaptation on stable `Player` API.
> 3. `network_security_config.xml` used **CIDR ranges** — Android rejects those → replaced with a documented `base-config`.
> 4. Missing imports (`background`, `mutableLongStateOf`) and unresolved `Color_Error`.
> 5. Invalid Kotlin: a fully-qualified call written as a method chain (`.androidx…`).
> 6. `selectPlayable()` duplicated `QualityLogic.dedupe()` and was dead → removed (drift risk).

### Key design choice — buffer-driven, not bandwidth-probed

Adaptation uses only `bufferedPosition`/`currentPosition`. Measuring bytes needs `PlaybackStatsListener`/`TransferListener` (unstable analytics APIs) — tried, abandoned. Buffer health is downstream of connection speed, so it's a valid adaptive signal. Full reasoning: `mem:architecture/android_build`.

### Prerequisites — all MISSING (verified 2026-10-20)

| Requirement | Status |
| --- | --- |
| Android SDK (API 35) + `ANDROID_HOME` | ❌ unset |
| Gradle (8.9+, matches AGP 8.7.3) | ❌ not on PATH |
| `gradle-wrapper.jar` binary | ❌ not committed (props file is) |
| JDK 17 | ✅ OpenJDK 21.0.12.1 |

**Recommended path:** open `android/` in **Android Studio** — it generates the wrapper and SDK on first open. Full setup: **`android/README.md`**.

## Why native, not WebView

User chose native Kotlin. Justified because WebView video is genuinely bad here:

- no hardware-accelerated DASH
- weak seeking
- poor battery behaviour
- no real TV focus handling

## Project shape (actual, on disk)

```
android/
├── settings.gradle.kts
├── build.gradle.kts (root)
├── gradle/
│   ├── libs.versions.toml        # version catalog
│   └── wrapper/gradle-wrapper.properties   # pins Gradle 8.9
├── README.md                     # setup + invariants + known gaps
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/filmy/app/
│       │   │   ├── FilmyApp.kt, MainActivity.kt
│       │   │   ├── data/     Config.kt, Models.kt, Api.kt   # mirrors api.ts
│       │   │   ├── player/   QualityLogic.kt                # pure, tested
│       │   │   └── ui/       nav/Nav.kt, theme/, screens/{Home,Detail,Player,Settings}
│       │   └── res/          values/, xml/, drawable/, mipmap-* (icons generated)
│       └── test/java/.../player/QualityLogicTest.kt        # 17 tests
```

## Playback

Use `Media3` `ExoPlayer` with the **proxy URL directly**:

> [!tip] The proxy is what makes this simple
> No custom headers needed — [[Architecture/Stream Proxy]] injects `Referer`/`Origin`/`UA` server-side and passes through `Range`. ExoPlayer, `<video>` and the Tauri webview all consume the **same URL identically**.

```kotlin
val player = ExoPlayer.Builder(context).build()
player.setMediaItem(MediaItem.fromUri(proxyUrl))   // no headers
player.prepare()
```

- Prefer the **h264** MP4 renditions — hevc won't decode on many devices.
- `MediaItem` with `ClippingConfiguration` if you need resume-from-position.

## TV-specific work

| Concern | Approach |
| --- | --- |
| D-pad navigation | Compose for TV `focusGroup()`, explicit `focusRequester`s |
| 10-foot UI | Larger type, higher contrast, 1.5× spacing scale |
| Poster rail | `LazyRow` with `TvLazyRow`-style focus scaling |
| Playback controls | Custom overlay, auto-hide, seek bar via D-pad |

This is **real UX work**, not a stylesheet — budget for it.

## Open dependencies

- [x] **Backend hosting decision** → resolved as **remote / self-hosted** ([[Decisions/ADR-005 Remote self-hosted backend]]). APK is a thin client; base URL lives in `SharedPreferences` (`filmy.apiBase`, same key as web) with a Settings screen whose **Test** probe reverts on failure.
- [x] `subject_type` labels correct server-side ([[Reference/Subject Types]]) — `Models.kt` exposes the **classified** `type`, with `isMovie` as the branch helper. Never branch on raw `subject_type`.
- [ ] **Android SDK + Gradle** installed — gates the first real build.
- [ ] First `assembleDebug` green (expected to need fixes).
- [ ] Dubs: `DetailScreen` renders the dub count but re-uses the current subject's streams rather than switching `subject_id` per language. Partial.
- [ ] Android TV variant (Compose for TV + D-pad focus) — not started.

## Risks

- Stream URLs expire — never persist to disk, resolve at play time.
- Upstream `429`s — surface a retry; backend already backs off.
- Catalog junk (music/jukebox/podcast) will look worse on a TV rail — consider client-side filtering. Tracked in [[Roadmap/Backlog]].

## Related

→ [[Decisions/ADR-003 Two codebases]] · [[Architecture/Client Strategy]] · [[Reference/API Endpoints]] · [[Risks and Compliance]]
