# Filmy Android

Native Kotlin client — **codebase #2** of the 2-codebase strategy (ADR-003).
Covers the phone APK; the Android TV variant reuses these sources with
Compose-for-TV UI (see `docs/Roadmap/Android.md`).

| Concern | Choice | Why |
| --- | --- | --- |
| UI | Jetpack Compose + Material3 | Native, no WebView (WebView video is poor here) |
| Playback | Media3 / ExoPlayer | Handles MP4 + DASH + signed URLs + seeking |
| HTTP | OkHttp | Mirrors `fetch()` in `web/src/api.ts` |
| JSON | kotlinx-serialization | Snake_case via explicit `@SerialName` |
| Images | Coil | Async poster loading |

## Status — read this before building

> [!warning]
> **These sources have never been compiled.** The environment used to author
> them has no Android SDK / Gradle / `ANDROID_HOME` (verified 2026-10-20).
> Files were written against the Media3/Compose APIs and manually reviewed for
> unresolved references, but the **first `./gradlew assembleDebug` is the real
> test** and will likely surface a few compile errors. That is expected.

Already caught and fixed during authoring (by review, not compilation):
- `PlaybackException.errorMessage` → does not exist; corrected to `message`/`errorCode`.
- `AnalyticsListener.onPlaybackStatsUpdate(EventSender, ...)` → phantom signature; the whole
  byte-counting sampler was removed in favor of **buffer-driven adaptation** using only
  stable `Player.bufferedPosition`/`currentPosition`.
- `network_security_config.xml` CIDR ranges → Android does not accept CIDR; replaced
  with a documented `base-config cleartextTrafficPermitted="true"`.
- Missing imports: `background`, `mutableLongStateOf`; unresolved `Color_Error`.
- Invalid Kotlin: a fully-qualified call written as a method chain (`.androidx...`).

## Prerequisites (all currently MISSING)

1. **Android Studio** (recommended — it generates the Gradle wrapper on first open) **or** Gradle 8.9+ on PATH.
2. **JDK 17** — present (OpenJDK 21.0.12.1 works).
3. **Android SDK** with **API 35** platform + build-tools; set `ANDROID_HOME`.
4. **Gradle wrapper JAR** — `gradle/wrapper/gradle-wrapper.properties` is committed, but
   the binary `gradle-wrapper.jar` is not. Generate once with `gradle wrapper`, or just
   open the project in Android Studio.

## Build

```powershell
cd android

# one-time, if you have Gradle on PATH:
gradle wrapper

.\gradlew.bat assembleDebug      # -> app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat testDebugUnitTest  # runs QualityLogicTest (pure JVM, no device)
.\gradlew.bat lint
```

Point the APK at a LAN backend at build time (optional — Settings overrides it):

```powershell
.\gradlew.bat assembleDebug -PdefaultApiBase=http://192.168.1.10:8000
```

## Layout

```
app/src/main/java/com/filmy/app/
├── FilmyApp.kt          # Application; inits Config once
├── MainActivity.kt         # single-activity host
├── data/
│   ├── Config.kt           # runtime API base  (mirrors web/src/config.ts)
│   ├── Models.kt           # @Serializable DTOs (mirrors web/src/api.ts)
│   └── Api.kt              # OkHttp endpoints (mirrors web/src/api.ts)
├── player/
│   └── QualityLogic.kt     # pure adaptive/manual logic (mirrors Player.tsx)
└── ui/
    ├── nav/Nav.kt          # Home -> Detail -> Player, + Settings
    ├── theme/              # Color / Theme (brand-synced to styles.css)
    └── screens/            # Home, Detail, Player, Settings
app/src/test/.../QualityLogicTest.kt   # 17 tests, no Android deps
```

## Invariants (do not break)

1. **Video always goes through `/proxy/stream`** — `Config.proxiedUrl()`. The CDN is
   Referer-locked; the proxy injects the header server-side so ExoPlayer needs **no
   custom headers**. Same URL shape as web/desktop.
2. **Never persist stream URLs.** They carry `&t=<epoch>` and expire (~2 min TTL).
   Resolve at play time only.
3. **Branch on the classified `type`, never raw `subject_type`** (6/7/8 are
   Anime/ShortDrama/Promo, not plain series).
4. **Dedupe before showing the quality picker** — `QualityLogic.dedupe()`. Upstream
   returns a ~0.8 MB hevc fragment mislabelled `MP4` beside the real h264 file, both
   `quality_value=1080`. Skipping this yields duplicate rows + a stream that dies at ~1s.
5. **Step-UP is gated on `GOOD_BUFFER_SEC`** — the missing gate that made the original
   web player oscillate.

## Backend hosting

Remote / self-hosted (ADR-005): thin client, no bundled Python. Base URL lives in
`SharedPreferences` (`filmy.apiBase` — same key as web) with a Settings screen
whose **Test** probe reverts on failure so a bad host is never persisted.
