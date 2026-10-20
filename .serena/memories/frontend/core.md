# Frontend Core (web/)

Read `mem:core` first. This is client #1 of 2 codebases (web + native Android).

## Files
- src/api.ts - typed client. ALL video goes through proxiedUrl() -> /proxy/stream. Adult filtering is OPTED OUT (include_adult=true) by explicit product decision; backend default is the opposite.
- src/App.tsx - search -> detail -> seasons/episodes -> player. Local useState, no router.
- src/Player.tsx - adaptive + manual quality. See `mem:frontend/player_quality`.
- src/styles.css - dark theme, -- vars in :root.
- vite.config.ts - dev proxy maps /search /detail /episodes /movies /tv-series /home /api /proxy /health to 127.0.0.1:8000.

## Flow
1. doSearch(q) -> search() -> results grid.
2. openDetail(item) -> getDetail(). If type Movie, getStreams(id). Else getEpisodes(id), then getStreams(id, season, 1).
3. loadEpisode(se, ep) -> getStreams(id, se, ep).
4. <Player streams=...> picks MP4 renditions.

## Gotchas
- d.type is the CLASSIFIED label (Movie/Series/Anime/ShortDrama/Promo), not the raw subject_type. Branch on d.type === "Movie" to decide whether to fetch episodes.
- hls.js is installed but unwired. DASH manifests are currently ignored; only format === "MP4" is playable.
- getEpisodes() returns [] for movies and for short-dramas with no season data - the UI must tolerate that.
- Backend is NOT hot-reloading; restart it after api.py edits or the client will serve stale shapes.