# Identity & naming (project renamed to Filmy)

## The rule: "moviebox" means two different things

Our product is **Filmy**. The service we scrape is still **MovieBox**. The old
word therefore appears in two classes of text, and only one class may ever be
rewritten:

| Class | Examples | Action |
| --- | --- | --- |
| **Upstream contract** | `apig.inmoviebox.com`, `moviebox.ph`, `inmoviebox.com`, `movieboxonline.net`, `movieboxhd.net`, `movie-box.co`, `aoneroom.com`, `h5-api.*`, `wefeed-*`, `x-source: moviebox`, `PROXY_ALLOWED_HOST_SUFFIXES`, `WEB_DOMAINS`, `Referer: https://moviebox.ph/`, `host=moviebox.ph` | **Never touch.** Renaming these breaks bootstrap, playback and SSRF protection. |
| **Stray local dir** | `.gitignore` anchored `/moviebox/`, `scripts/check_wikilinks.py` ignore entry | **Never touch.** It is a real folder on disk, not branding. |
| **Gitignore history** | `rules.md`, `.gitignore`, `Git Workflow.md`, `Status.md`, `git_hygiene.md` examples showing `moviebox/` matching `.../com/moviebox/app/` | **Never touch.** They quote the PRE-rename path as evidence; rewriting makes the example untrue. |
| **Our brand** | package/identifier/title/class/storage-key strings | Renamed to `filmy` / `Filmy` on 2026-10-20. |

Upstream-facing **prose** still says MovieBox on purpose - `AGENTS.md`,
`docs/Project/Overview.md`, ADR-002, ADR-004 and `mem:core` describe *the catalog
we scrape*, not us.

## What was renamed (any of these changing changes on-disk state)

| Surface | Value |
| --- | --- |
| npm package | `filmy-web` |
| Tauri | `productName` `Filmy`, `identifier` `com.filmy.desktop`, `title` `Filmy` |
| Rust | crate `filmy-desktop`, lib `filmy_desktop_lib` (`main.rs` calls `filmy_desktop_lib::run()`) |
| Android | `namespace` + `applicationId` `com.filmy.app`, Gradle root `Filmy`, source under `java/com/filmy/app/` |
| Android symbols | `FilmyApp` (manifest `android:name=".FilmyApp"`), `FilmyTheme`, `Theme.Filmy`, proguard rules |
| Backend | `FilmyEngine`, OpenAPI `title`/`description`, both HTML dashboard headings |
| Settings key (both clients) | `filmy.apiBase` |
| Android prefs file | `filmy` |
| Serena | `project.yml` `project_name: "filmy"` |

## Migrations - why the settings keys were not just overwritten

Renaming a storage key silently reverts every existing user's configured
backend URL to the default, which looks like "my Settings reset". So both
clients keep a read-fallback to the pre-rename key:

- `web/src/config.ts` - `LEGACY_STORAGE_KEY = "moviebox.apiBase"`; on a miss it
  adopts the value into the new key and deletes the old one.
- `android/.../data/Config.kt` - `LEGACY_PREFS = "moviebox"` /
  `LEGACY_KEY_API_BASE`; `init()` calls `adoptLegacy()`, a no-op on fresh
  installs. Uses `SharedPreferences.contains()`, which is real Android API.

Safe to delete both once no pre-rename build still ships.

## How the rename was done (reproducible pattern)

1. Classify every occurrence into protected vs ours **before** replacing.
2. Phase 1: replace only unambiguous tokens (`com.moviebox.app`,
   `MovieBoxTheme`, `moviebox.apiBase`, ... ) that cannot occur inside an
   upstream literal - so no line guard was needed.
3. Phase 2: the bare word `MovieBox` is semantic, so files were split into
   allowlist / explicit-with-count-assertions / never-touched. A count mismatch
   aborts instead of corrupting an upstream reference.
4. Verify protected literals byte-identical afterwards, then run the full gate.

Two traps hit along the way, both worth remembering:

- **A broad pattern misses variants.** `movie-box.co` (hyphenated) and
  `Moviebox` (lowercase b) were invisible to a literal `moviebox` search; a
  `movie.?box` case-insensitive sweep found them.
- **`-SimpleMatch` + `[regex]::Escape()` double-escapes**, so a "protected
  literals intact" check silently reports 0 for everything and looks like
  success. Probe the check with a control string containing no metacharacters.

## Related
- Root project files: `mem:project/root_files`
- gitignore traps: `mem:project/git_hygiene`
- Runtime config + settings UI: `mem:frontend/settings`
- Android build status (UNCOMPILED): `mem:architecture/android_build`
- Desktop/Tauri: `mem:architecture/desktop_tauri`
