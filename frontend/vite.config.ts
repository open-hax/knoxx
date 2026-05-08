import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const VITE_BACKEND_URL =
  process.env.VITE_KNOXX_BACKEND_URL || "http://knoxx-backend:8000";

export default defineConfig({
  plugins: [react()],
  resolve: {
    dedupe: ["react", "react-dom", "react/jsx-runtime", "react/jsx-dev-runtime"],
  },
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
