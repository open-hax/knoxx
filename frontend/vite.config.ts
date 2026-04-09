import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    host: "0.0.0.0",
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8001",
        changeOrigin: true
      },
      "/ws": {
        target: "ws://127.0.0.1:8001",
        ws: true
      },
      "/health": {
        target: "http://127.0.0.1:8001",
        changeOrigin: true
      }
    }
  }
});