// vite.bridge.config.ts
import { defineConfig } from "file:///home/err/devel/orgs/open-hax/openplanner/node_modules/.pnpm/vite@5.4.21_@types+node@24.12.2/node_modules/vite/dist/node/index.js";
import react from "file:///home/err/devel/orgs/open-hax/openplanner/node_modules/.pnpm/@vitejs+plugin-react@4.7.0_vite@5.4.21_@types+node@24.12.2_/node_modules/@vitejs/plugin-react/dist/index.js";
import path from "path";
var __vite_injected_original_dirname = "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/frontend";
var vite_bridge_config_default = defineConfig({
  plugins: [react()],
  build: {
    // Emit .map files so browser stack traces map back to TS sources.
    sourcemap: true,
    lib: {
      entry: path.resolve(__vite_injected_original_dirname, "src/bridge/index.ts"),
      name: "knoxx-frontend-bridge",
      fileName: (format) => `knoxx-frontend-bridge.${format}.js`,
      formats: ["es"]
    },
    rollupOptions: {
      external: [
        "react",
        "react-dom",
        "react-dom/client",
        "react/jsx-runtime",
        // Keep router libs external to avoid duplicating router context.
        "@remix-run/router",
        "react-router",
        "react-router-dom"
      ],
      output: {
        globals: {
          react: "React",
          "react-dom": "ReactDOM",
          "react-dom/client": "ReactDOMClient"
        }
      }
    },
    outDir: "dist/bridge",
    // This directory is shared with other build outputs (e.g. knoxx-app-bridge).
    // In watch mode, emptying it will delete sibling artifacts and break shadow's
    // file-resolved modules at runtime.
    emptyOutDir: false
  }
});
export {
  vite_bridge_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5icmlkZ2UuY29uZmlnLnRzIl0sCiAgInNvdXJjZXNDb250ZW50IjogWyJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiL2hvbWUvZXJyL2RldmVsL29yZ3Mvb3Blbi1oYXgvb3BlbnBsYW5uZXIvcGFja2FnZXMvYWdlbnRzL2tub3h4L2Zyb250ZW5kXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCIvaG9tZS9lcnIvZGV2ZWwvb3Jncy9vcGVuLWhheC9vcGVucGxhbm5lci9wYWNrYWdlcy9hZ2VudHMva25veHgvZnJvbnRlbmQvdml0ZS5icmlkZ2UuY29uZmlnLnRzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9ob21lL2Vyci9kZXZlbC9vcmdzL29wZW4taGF4L29wZW5wbGFubmVyL3BhY2thZ2VzL2FnZW50cy9rbm94eC9mcm9udGVuZC92aXRlLmJyaWRnZS5jb25maWcudHNcIjtpbXBvcnQgeyBkZWZpbmVDb25maWcgfSBmcm9tIFwidml0ZVwiO1xuaW1wb3J0IHJlYWN0IGZyb20gXCJAdml0ZWpzL3BsdWdpbi1yZWFjdFwiO1xuaW1wb3J0IHBhdGggZnJvbSBcInBhdGhcIjtcblxuZXhwb3J0IGRlZmF1bHQgZGVmaW5lQ29uZmlnKHtcbiAgcGx1Z2luczogW3JlYWN0KCldLFxuICBidWlsZDoge1xuICAgIC8vIEVtaXQgLm1hcCBmaWxlcyBzbyBicm93c2VyIHN0YWNrIHRyYWNlcyBtYXAgYmFjayB0byBUUyBzb3VyY2VzLlxuICAgIHNvdXJjZW1hcDogdHJ1ZSxcbiAgICBsaWI6IHtcbiAgICAgIGVudHJ5OiBwYXRoLnJlc29sdmUoX19kaXJuYW1lLCBcInNyYy9icmlkZ2UvaW5kZXgudHNcIiksXG4gICAgICBuYW1lOiBcImtub3h4LWZyb250ZW5kLWJyaWRnZVwiLFxuICAgICAgZmlsZU5hbWU6IChmb3JtYXQpID0+IGBrbm94eC1mcm9udGVuZC1icmlkZ2UuJHtmb3JtYXR9LmpzYCxcbiAgICAgIGZvcm1hdHM6IFtcImVzXCJdLFxuICAgIH0sXG4gICAgcm9sbHVwT3B0aW9uczoge1xuICAgICAgZXh0ZXJuYWw6IFtcbiAgICAgICAgXCJyZWFjdFwiLFxuICAgICAgICBcInJlYWN0LWRvbVwiLFxuICAgICAgICBcInJlYWN0LWRvbS9jbGllbnRcIixcbiAgICAgICAgXCJyZWFjdC9qc3gtcnVudGltZVwiLFxuICAgICAgICAvLyBLZWVwIHJvdXRlciBsaWJzIGV4dGVybmFsIHRvIGF2b2lkIGR1cGxpY2F0aW5nIHJvdXRlciBjb250ZXh0LlxuICAgICAgICBcIkByZW1peC1ydW4vcm91dGVyXCIsXG4gICAgICAgIFwicmVhY3Qtcm91dGVyXCIsXG4gICAgICAgIFwicmVhY3Qtcm91dGVyLWRvbVwiLFxuICAgICAgXSxcbiAgICAgIG91dHB1dDoge1xuICAgICAgICBnbG9iYWxzOiB7XG4gICAgICAgICAgcmVhY3Q6IFwiUmVhY3RcIixcbiAgICAgICAgICBcInJlYWN0LWRvbVwiOiBcIlJlYWN0RE9NXCIsXG4gICAgICAgICAgXCJyZWFjdC1kb20vY2xpZW50XCI6IFwiUmVhY3RET01DbGllbnRcIixcbiAgICAgICAgfSxcbiAgICAgIH0sXG4gICAgfSxcbiAgICBvdXREaXI6IFwiZGlzdC9icmlkZ2VcIixcbiAgICAvLyBUaGlzIGRpcmVjdG9yeSBpcyBzaGFyZWQgd2l0aCBvdGhlciBidWlsZCBvdXRwdXRzIChlLmcuIGtub3h4LWFwcC1icmlkZ2UpLlxuICAgIC8vIEluIHdhdGNoIG1vZGUsIGVtcHR5aW5nIGl0IHdpbGwgZGVsZXRlIHNpYmxpbmcgYXJ0aWZhY3RzIGFuZCBicmVhayBzaGFkb3cnc1xuICAgIC8vIGZpbGUtcmVzb2x2ZWQgbW9kdWxlcyBhdCBydW50aW1lLlxuICAgIGVtcHR5T3V0RGlyOiBmYWxzZSxcbiAgfSxcbn0pOyJdLAogICJtYXBwaW5ncyI6ICI7QUFBd1osU0FBUyxvQkFBb0I7QUFDcmIsT0FBTyxXQUFXO0FBQ2xCLE9BQU8sVUFBVTtBQUZqQixJQUFNLG1DQUFtQztBQUl6QyxJQUFPLDZCQUFRLGFBQWE7QUFBQSxFQUMxQixTQUFTLENBQUMsTUFBTSxDQUFDO0FBQUEsRUFDakIsT0FBTztBQUFBO0FBQUEsSUFFTCxXQUFXO0FBQUEsSUFDWCxLQUFLO0FBQUEsTUFDSCxPQUFPLEtBQUssUUFBUSxrQ0FBVyxxQkFBcUI7QUFBQSxNQUNwRCxNQUFNO0FBQUEsTUFDTixVQUFVLENBQUMsV0FBVyx5QkFBeUIsTUFBTTtBQUFBLE1BQ3JELFNBQVMsQ0FBQyxJQUFJO0FBQUEsSUFDaEI7QUFBQSxJQUNBLGVBQWU7QUFBQSxNQUNiLFVBQVU7QUFBQSxRQUNSO0FBQUEsUUFDQTtBQUFBLFFBQ0E7QUFBQSxRQUNBO0FBQUE7QUFBQSxRQUVBO0FBQUEsUUFDQTtBQUFBLFFBQ0E7QUFBQSxNQUNGO0FBQUEsTUFDQSxRQUFRO0FBQUEsUUFDTixTQUFTO0FBQUEsVUFDUCxPQUFPO0FBQUEsVUFDUCxhQUFhO0FBQUEsVUFDYixvQkFBb0I7QUFBQSxRQUN0QjtBQUFBLE1BQ0Y7QUFBQSxJQUNGO0FBQUEsSUFDQSxRQUFRO0FBQUE7QUFBQTtBQUFBO0FBQUEsSUFJUixhQUFhO0FBQUEsRUFDZjtBQUNGLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
