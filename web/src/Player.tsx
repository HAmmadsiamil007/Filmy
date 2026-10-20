import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { Stream } from "./api";
import { proxiedUrl } from "./api";

type QualityMode = "auto" | number;

export interface QualityOption {
  label: string;
  value: QualityMode;
}

const MIN_BUFFER_SEC = 12;   // below this, consider stepping DOWN
const GOOD_BUFFER_SEC = 30;  // above this, consider stepping UP
const SWITCH_COOLDOWN_MS = 8000;
const HEADROOM = 1.35;       // need 35% spare bandwidth to step up

interface Props {
  streams: Stream[];
  title: string;
  subtitle?: string;
  onEnded?: () => void;
}

/**
 * Adaptive + manual quality player.
 *
 * Auto mode estimates available bandwidth from the video element's own
 * download progress (bytes received over wall time) plus the browser's
 * Network Information API, then picks the highest rendition whose bitrate
 * fits inside that estimate with HEADROOM to spare. It also watches buffer
 * health: if the buffer drains below MIN_BUFFER_SEC it steps down
 * immediately, and only steps back up once GOOD_BUFFER_SEC is recovered.
 *
 * Any manual selection overrides Auto and pins the rendition until the user
 * returns to Auto. Switches preserve the playhead.
 */
export default function Player({ streams, title, subtitle, onEnded }: Props) {
  // MP4 renditions only — DASH manifests need a separate MSE path.
  //
  // The upstream occasionally returns a tiny DASH/fragment artifact that is
  // mislabeled `format: "MP4"` (seen: a 0.8 MB hevc entry alongside the real
  // 4.9 GB h264 file, both tagged 1080p). Two consequences if we keep it:
  // the picker shows duplicate labels and React gets duplicate keys, and
  // auto-quality can pick it and die after ~1s. So we dedupe by
  // quality_value, keeping the largest real file, and prefer h264 since
  // hevc is not decodable in Chrome/Firefox.
  const renditions = useMemo(() => {
    const mp4 = streams.filter((s) => s.format === "MP4" && s.url);
    const byQuality = new Map<number, Stream>();
    for (const s of mp4) {
      const existing = byQuality.get(s.quality_value);
      if (!existing) {
        byQuality.set(s.quality_value, s);
        continue;
      }
      const preferNew =
        (s.size_mb ?? 0) > (existing.size_mb ?? 0) ||
        ((s.codec ?? "").toLowerCase() !== "hevc" &&
          (existing.codec ?? "").toLowerCase() === "hevc");
      if (preferNew) byQuality.set(s.quality_value, s);
    }
    return [...byQuality.values()].sort((a, b) => b.quality_value - a.quality_value);
  }, [streams]);

  const videoRef = useRef<HTMLVideoElement>(null);
  const [mode, setMode] = useState<QualityMode>("auto");
  const [activeIdx, setActiveIdx] = useState(0);
  const [autoNote, setAutoNote] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [showPicker, setShowPicker] = useState(false);

  // Refs for values the media-event handlers read, so we don't need to
  // re-create those handlers on every render (which would stall playback).
  const modeRef = useRef<QualityMode>("auto");
  const lastSwitchRef = useRef(0);
  const bytesRef = useRef(0);
  const windowStartRef = useRef(0);
  const stalledRef = useRef(false);

  const options: QualityOption[] = [
    { label: "Auto", value: "auto" },
    ...renditions.map((r) => ({ label: r.quality, value: r.quality_value as QualityMode })),
  ];

  const loadRendition = useCallback(
    (idx: number, resumeAt?: number) => {
      const video = videoRef.current;
      const stream = renditions[idx];
      if (!video || !stream) return;
      const wasPlaying = !video.paused;
      video.src = proxiedUrl(stream.url, stream.headers?.Referer);
      video.load();
      const onMeta = () => {
        if (typeof resumeAt === "number" && Number.isFinite(video.duration)) {
          video.currentTime = Math.min(resumeAt, video.duration - 1);
        }
        if (wasPlaying) void video.play().catch(() => {});
        video.removeEventListener("loadedmetadata", onMeta);
      };
      video.addEventListener("loadedmetadata", onMeta);
      setActiveIdx(idx);
    },
    [renditions]
  );

  // Kick off at the top rendition on mount.
  useEffect(() => {
    if (renditions.length) loadRendition(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [streams]);

  const pickForBandwidth = useCallback(
    (mbps: number, bufferSec: number, currentIdx: number): number => {
      // Prefer the highest rendition that fits the measured bandwidth.
      let chosen = renditions.length - 1;
      for (let i = 0; i < renditions.length; i++) {
        // quality_value doubles as a rough bitrate proxy; scale it so the
        // comparison is against Mbps rather than raw pixels.
        const neededMbps = renditions[i].quality_value / 180;
        if (neededMbps * HEADROOM <= mbps) {
          chosen = i;
        }
      }
      // If the buffer is already thin, cap one rung down to recover faster.
      if (bufferSec < MIN_BUFFER_SEC && chosen > 0) chosen -= 1;
      // Only allow stepping UP once the buffer is comfortably full. Without
      // this gate the player oscillates: it climbs on a good sample, the
      // buffer drains, it drops, and repeats. Requires GOOD_BUFFER_SEC.
      if (chosen < currentIdx && bufferSec < GOOD_BUFFER_SEC) {
        chosen = currentIdx;
      }
      return chosen;
    },
    [renditions]
  );

  const handleProgress = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    // Track cumulative bytes via buffered growth as a bandwidth proxy.
    const now = performance.now();
    if (!windowStartRef.current) {
      windowStartRef.current = now;
      return;
    }
    let bufferedBytes = 0;
    for (let i = 0; i < video.buffered.length; i++) {
      bufferedBytes += video.buffered.end(i) - video.buffered.start(i);
    }
    // Approximate bytes from buffered seconds * a nominal bitrate.
    const nominalBitrate = (renditions[activeIdx]?.quality_value ?? 720) * 1000;
    const approxBytes = bufferedBytes * (nominalBitrate / 8);
    const elapsed = (now - windowStartRef.current) / 1000;
    if (elapsed < 2) return;
    const mbps = (approxBytes - bytesRef.current) / 1e6 / elapsed;

    bytesRef.current = approxBytes;
    windowStartRef.current = now;

    if (modeRef.current !== "auto" || !renditions.length) return;

    const bufferSec = video.buffered.length
      ? video.buffered.end(video.buffered.length - 1) - video.currentTime
      : 0;

    const next = pickForBandwidth(mbps, bufferSec, activeIdx);
    const nowTs = Date.now();
    if (next !== activeIdx && nowTs - lastSwitchRef.current > SWITCH_COOLDOWN_MS) {
      lastSwitchRef.current = nowTs;
      loadRendition(next, video.currentTime);
      setAutoNote(`${renditions[next].quality} · ${mbps.toFixed(1)} Mbps`);
    }
  }, [activeIdx, loadRendition, pickForBandwidth, renditions]);

  const handleWaiting = useCallback(() => {
    if (stalledRef.current) return;
    stalledRef.current = true;
    if (modeRef.current !== "auto" || activeIdx >= renditions.length - 1) return;
    // Hard stall: drop a rung immediately, ignore cooldown.
    const next = activeIdx + 1; // renditions sorted desc, so +1 is lower
    lastSwitchRef.current = Date.now();
    loadRendition(next, videoRef.current?.currentTime);
    setAutoNote(`Stalled → ${renditions[next].quality}`);
  }, [activeIdx, loadRendition, renditions]);

  const handlePlaying = useCallback(() => {
    stalledRef.current = false;
  }, []);

  const changeMode = (m: QualityMode) => {
    modeRef.current = m;
    setMode(m);
    if (m === "auto") {
      setAutoNote("Auto");
    } else {
      const idx = renditions.findIndex((r) => r.quality_value === m);
      if (idx >= 0) {
        lastSwitchRef.current = Date.now();
        loadRendition(idx, videoRef.current?.currentTime);
        setAutoNote("");
      }
    }
    setShowPicker(false);
  };

  if (!renditions.length) {
    return (
      <div className="player-empty">
        <p>No directly playable MP4 renditions were returned for this title.</p>
      </div>
    );
  }

  const activeLabel = renditions[activeIdx]?.quality ?? "—";

  return (
    <div className="player">
      <video
        ref={videoRef}
        className="player-video"
        controls
        playsInline
        onProgress={handleProgress}
        onWaiting={handleWaiting}
        onPlaying={handlePlaying}
        onEnded={onEnded}
        onError={() => setError("Playback failed — the stream URL may have expired. Reload the title.")}
      />

      <div className="player-bar">
        <div className="player-meta">
          <span className="player-title">{title}</span>
          {subtitle && <span className="player-sub">{subtitle}</span>}
        </div>

        <div className="quality">
          <button
            className="quality-btn"
            onClick={() => setShowPicker((v) => !v)}
            aria-haspopup="listbox"
            aria-expanded={showPicker}
          >
            {mode === "auto" ? `Auto · ${activeLabel}` : activeLabel}
          </button>

          {showPicker && (
            <ul className="quality-list" role="listbox">
              {options.map((opt) => (
                <li key={String(opt.value)}>
                  <button
                    className={`quality-item ${mode === opt.value ? "is-active" : ""}`}
                    onClick={() => changeMode(opt.value)}
                    role="option"
                    aria-selected={mode === opt.value}
                  >
                    {opt.label}
                    {opt.value === "auto" && autoNote && (
                      <span className="quality-note">{autoNote}</span>
                    )}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {error && <div className="player-error">{error}</div>}
    </div>
  );
}
