// Typed client for the Moviebox FastAPI backend.
//
// Three decisions baked in here that all four clients depend on:
//   1. Video always plays through /proxy/stream (the CDN is Referer-locked and
//      a <video> tag cannot send a Referer header).
//   2. Adult content is UNFILTERED by default (include_adult=true), matching
//      the product decision. Pass false explicitly to hide it.
//   3. The API base is RUNTIME-configurable (remote/self-hosted per ADR). An
//      empty base means same-origin. See ./config.ts.
import { apiUrl } from "./config";

export interface SearchResult {
  subject_id: string;
  title: string;
  type: "Movie" | "Series" | "Anime" | "ShortDrama" | "Promo" | string;
  subject_type: number;
  release_date: string | null;
  imdb_rating: string | null;
  genre: string | null;
  is_adult: boolean;
  cover: string | null;
  detail_path: string | null;
  language: string | null;
}

export interface Detail {
  subject_id: string;
  title: string;
  subject_type: number;
  type: string;
  description: string | null;
  release_date: string | null;
  duration: string | null;
  imdb_rating: string | null;
  genre: string | null;
  is_adult: boolean;
  country: string | null;
  language: string | null;
  cover: string | null;
  dubs: { subject_id: string; lang_name: string; language: string }[];
}

export interface Season {
  season: number;
  episode_count: number;
  resolutions: { resolution: number; epNum: number }[];
}

export interface StreamHeaders {
  "User-Agent"?: string;
  Referer?: string;
  Origin?: string;
  [k: string]: string | undefined;
}

export interface Stream {
  source: string;
  format: "MP4" | "DASH" | string;
  quality: string;
  quality_value: number;
  resolution_tag: string;
  url: string;
  size_mb: number;
  codec: string;
  duration_sec: number | null;
  cookie?: string;
  headers: StreamHeaders;
}

export interface Caption {
  id: string;
  language: string;
  code: string;
  url: string;
  size: number | null;
}

async function getJSON<T>(path: string): Promise<T> {
  const res = await fetch(apiUrl(path));
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`${res.status} ${res.statusText}${body ? ` — ${body.slice(0, 200)}` : ""}`);
  }
  return res.json() as Promise<T>;
}

export async function search(query: string, opts: { includeAdult?: boolean } = {}): Promise<SearchResult[]> {
  const adult = opts.includeAdult ?? true; // unfiltered by default
  const data = await getJSON<{ results: SearchResult[] }>(
    `/search?q=${encodeURIComponent(query)}&include_adult=${adult}`
  );
  return data.results;
}

export async function getDetail(id: string): Promise<Detail> {
  return getJSON<Detail>(`/detail/${id}`);
}

export async function getEpisodes(id: string): Promise<Season[]> {
  const data = await getJSON<{ seasons: Season[] }>(`/episodes/${id}`);
  return data.seasons;
}

export async function getStreams(id: string, se = 0, ep = 0): Promise<Stream[]> {
  const data = await getJSON<{ streams: Stream[] }>(`/api/stream/${id}?se=${se}&ep=${ep}`);
  return data.streams;
}

export async function getCaptions(id: string, se = 0, ep = 0): Promise<Caption[]> {
  const data = await getJSON<{ captions: Caption[] }>(`/api/stream/${id}/captions?se=${se}&ep=${ep}`);
  return data.captions;
}

/**
 * Wrap a stream URL for playback.
 *
 * MP4 goes straight through the proxy with a plain <video> src.
 * DASH needs the manifest fetched through the proxy too, then handed to hls.js
 * (which handles .mpd fine via its own loader once the bytes are CORS-clean).
 *
 * NOTE: the proxy target must follow the configured API base, not
 * window.location.origin — with a self-hosted/remote backend the video bytes
 * come from the API host. The backend emits Access-Control-Allow-Origin: * on
 * /proxy/stream, so cross-origin playback works.
 */
export function proxiedUrl(url: string, referer?: string): string {
  const target = new URL(apiUrl("/proxy/stream"), window.location.origin);
  target.searchParams.set("url", url);
  if (referer) target.searchParams.set("referer", referer);
  return target.toString();
}
