import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const VITE_BACKEND_URL =
  process.env.VITE_KNOXX_BACKEND_URL || "http://knoxx-backend:8000";

export default defineConfig({
  plugins: [react()],
  resolve: {
    dedupe: ["react", "react-dom", "react/jsx-runtime", "react/jsx-dev-runtime"],
  },

  // During the migration, Vite is only responsible for producing a build output
  // into ./dist (optionally in --watch mode). shadow-cljs serves /dist and owns
  // the dev port.
  build: {
    outDir: "dist",
    // shadow-cljs also writes into dist/cljs and Vite bridge builds write into
    // dist/bridge. Don't wipe those on each rebuild.
    emptyOutDir: false,
  },

  // Kept for `pnpm preview` / ad-hoc Vite dev usage, but the default `pnpm dev`
  // script no longer runs Vite in server mode.
  server: {
    allowedHosts: true,
    host: "0.0.0.0",
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: VITE_BACKEND_URL,
        changeOrigin: true,
      },
      "/ws": {
        target: VITE_BACKEND_URL,
        changeOrigin: true,
        ws: true,
      },
      "/health": {
        target: VITE_BACKEND_URL,
        changeOrigin: true,
      },
    },
  },
});
