import { useCallback, useEffect, useState } from "react";
import type { Detail, SearchResult, Season } from "./api";
import { getDetail, getEpisodes, getStreams, search } from "./api";
import { getApiBase } from "./config";
import Player from "./Player";
import Settings from "./Settings";

type View = { kind: "home" } | { kind: "detail"; id: string; title: string };

export default function App() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResult[]>([]);
  const [view, setView] = useState<View>({ kind: "home" });
  const [detail, setDetail] = useState<Detail | null>(null);
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [se, setSe] = useState(1);
  const [ep, setEp] = useState(1);
  const [streams, setStreams] = useState<Awaited<ReturnType<typeof getStreams>>>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [apiBase, setApiBaseState] = useState(() => getApiBase());

  const doSearch = useCallback(async (q: string) => {
    if (!q.trim()) return;
    setLoading(true);
    setError(null);
    setView({ kind: "home" });
    try {
      setResults(await search(q));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Search failed");
      setResults([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const openDetail = useCallback(async (item: SearchResult) => {
    setLoading(true);
    setError(null);
    setView({ kind: "detail", id: item.subject_id, title: item.title });
    setDetail(null);
    setStreams([]);
    try {
      const d = await getDetail(item.subject_id);
      setDetail(d);
      if (d.type === "Movie") {
        setStreams(await getStreams(item.subject_id));
      } else {
        const s = await getEpisodes(item.subject_id);
        setSeasons(s);
        if (s.length) {
          setSe(s[0].season);
          setEp(1);
          setStreams(await getStreams(item.subject_id, s[0].season, 1));
        }
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load title");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadEpisode = useCallback(
    async (season: number, episode: number) => {
      if (view.kind !== "detail") return;
      setSe(season);
      setEp(episode);
      setLoading(true);
      try {
        setStreams(await getStreams(view.id, season, episode));
      } catch (e) {
        setError(e instanceof Error ? e.message : "Failed to load episode");
      } finally {
        setLoading(false);
      }
    },
    [view]
  );

  useEffect(() => {
    document.title = view.kind === "detail" ? `${view.title} · MovieBox` : "MovieBox";
  }, [view]);

  const back = () => {
    setView({ kind: "home" });
    setDetail(null);
    setStreams([]);
    setSeasons([]);
  };

  return (
    <div className="app">
      <header className="topbar">
        <button className="brand" onClick={back}>
          MovieBox
        </button>
        <form
          className="search"
          onSubmit={(e) => {
            e.preventDefault();
            void doSearch(query);
          }}
        >
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search movies, series, anime…"
            aria-label="Search"
          />
          <button type="submit">Search</button>
        </form>
        <button
          className="icon-btn"
          onClick={() => setSettingsOpen(true)}
          title={apiBase ? `API: ${apiBase}` : "API: same origin"}
          aria-label="Backend settings"
        >
          ⚙
        </button>
      </header>

      <Settings
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        onApplied={() => {
          setApiBaseState(getApiBase());
          setSettingsOpen(false);
          setError(null);
          // Host may have changed — drop stale results and re-run the query.
          setResults([]);
          setStreams([]);
          setDetail(null);
          setView({ kind: "home" });
          if (query.trim()) void doSearch(query);
        }}
      />

      {error && <div className="banner error">{error}</div>}
      {loading && <div className="banner">Loading…</div>}

      {view.kind === "home" ? (
        <section className="grid">
          {results.length === 0 && !loading && (
            <p className="hint">Search for a title to get started.</p>
          )}
          {results.map((r) => (
            <button key={r.subject_id} className="card" onClick={() => void openDetail(r)}>
              {r.cover ? (
                <img src={r.cover} alt="" loading="lazy" />
              ) : (
                <div className="cover-fallback">{r.title}</div>
              )}
              <div className="card-meta">
                <span className="card-title">{r.title}</span>
                <span className="card-sub">
                  {r.type}
                  {r.release_date ? ` · ${r.release_date.slice(0, 4)}` : ""}
                  {r.imdb_rating ? ` · ★ ${r.imdb_rating}` : ""}
                </span>
              </div>
            </button>
          ))}
        </section>
      ) : (
        <section className="detail">
          {detail && (
            <div className="detail-head">
              {detail.cover && <img className="detail-cover" src={detail.cover} alt="" />}
              <div className="detail-info">
                <h1>{detail.title}</h1>
                <p className="detail-meta">
                  {[detail.type, detail.release_date, detail.country, detail.language]
                    .filter(Boolean)
                    .join(" · ")}
                </p>
                {detail.genre && <p className="detail-genre">{detail.genre}</p>}
                {detail.description && <p className="detail-desc">{detail.description}</p>}
              </div>
            </div>
          )}

          {seasons.length > 0 && (
            <div className="episodes">
              <div className="season-row">
                {seasons.map((s) => (
                  <button
                    key={s.season}
                    className={`season-btn ${se === s.season ? "is-active" : ""}`}
                    onClick={() => void loadEpisode(s.season, 1)}
                  >
                    S{s.season}
                  </button>
                ))}
              </div>
              <div className="ep-row">
                {Array.from(
                  { length: seasons.find((s) => s.season === se)?.episode_count ?? 0 },
                  (_, i) => i + 1
                ).map((n) => (
                  <button
                    key={n}
                    className={`ep-btn ${ep === n ? "is-active" : ""}`}
                    onClick={() => void loadEpisode(se, n)}
                  >
                    E{n}
                  </button>
                ))}
              </div>
            </div>
          )}

          {streams.length > 0 && (
            <Player
              streams={streams}
              title={detail?.title ?? ""}
              subtitle={detail?.type === "Movie" ? undefined : `S${se} E${ep}`}
            />
          )}
        </section>
      )}
    </div>
  );
}
