import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Backend URL for Vite dev server proxy
// Use VITE_KNOXX_BACKEND_URL env var, or default to container service name
const VITE_BACKEND_URL =
  process.env.VITE_KNOXX_BACKEND_URL || "http://knoxx-backend:8000";

export default defineConfig({
  plugins: [react()],
  server: {
    allowedHosts: ["err-stealth-16-ai-studio-a1vgg.tailbe888a.ts.net"],
    host: "0.0.0.0",
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: VITE_BACKEND_URL,
        changeOrigin: true,
      },
      "/ws": {
        target: VITE_BACKEND_URL.replace("http://", "ws://").replace(
          "https://",
          "wss://",
        ),
        ws: true,
      },
      "/health": {
        target: VITE_BACKEND_URL,
        changeOrigin: true,
      },
    },
  },
});
