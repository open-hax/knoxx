import { defineConfig } from "vite";

const backend = "http://127.0.0.1:8003";

export default defineConfig({
  // A built CLJS app and its CSS are static inputs to this local preview.
  publicDir: "dist",
  server: {
    host: "127.0.0.1",
    port: 5176,
    strictPort: true,
    proxy: {
      "/api": { target: backend, changeOrigin: true },
      "/ws": { target: backend, changeOrigin: true, ws: true },
      "/health": { target: backend, changeOrigin: true },
    },
  },
});
