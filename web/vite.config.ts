import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The dev server proxies to the local FastAPI backend so the browser
// sees same-origin requests (no CORS preflight on every API call).
// Video still streams through /proxy/stream, which is what makes the
// Referer-locked MP4s playable from a plain <video> tag.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/search": "http://127.0.0.1:8000",
      "/detail": "http://127.0.0.1:8000",
      "/episodes": "http://127.0.0.1:8000",
      "/movies": "http://127.0.0.1:8000",
      "/tv-series": "http://127.0.0.1:8000",
      "/home": "http://127.0.0.1:8000",
      "/api": "http://127.0.0.1:8000",
      "/proxy": "http://127.0.0.1:8000",
      "/health": "http://127.0.0.1:8000",
    },
  },
});
