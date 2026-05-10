// vite.app-bridge.config.ts
import { defineConfig } from "file:///home/err/devel/orgs/open-hax/openplanner/node_modules/.pnpm/vite@5.4.21_@types+node@24.12.2/node_modules/vite/dist/node/index.js";
import react from "file:///home/err/devel/orgs/open-hax/openplanner/node_modules/.pnpm/@vitejs+plugin-react@4.7.0_vite@5.4.21_@types+node@24.12.2_/node_modules/@vitejs/plugin-react/dist/index.js";
import path from "path";
var __vite_injected_original_dirname = "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/frontend";
var vite_app_bridge_config_default = defineConfig({
  plugins: [react()],
  build: {
    // Emit .map files so browser stack traces map back to TS sources.
    sourcemap: true,
    lib: {
      entry: path.resolve(__vite_injected_original_dirname, "src/bridge/app.ts"),
      name: "knoxx-app-bridge",
      fileName: (format) => `knoxx-app-bridge.${format}.js`,
      formats: ["es"]
    },
    rollupOptions: {
      external: [
        "react",
        "react-dom",
        "react-dom/client",
        "react/jsx-runtime",
        // IMPORTANT: keep router libs external so shadow-cljs and the app-bridge
        // share ONE router context instance. Bundling react-router(-dom) into the
        // bridge causes hooks (useLocation, etc.) to see a different context and
        // throw "may be used only in the context of a <Router>".
        "@remix-run/router",
        "react-router",
        "react-router-dom"
      ],
      output: {
        // shadow-cljs consumes this file via :js-options :resolve {:target :file ...}.
        // That resolver cannot follow additional relative chunk imports.
        // Keep this bridge as a single self-contained ESM file.
        inlineDynamicImports: true
      }
    },
    outDir: "dist/bridge",
    emptyOutDir: false
  }
});
export {
  vite_app_bridge_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5hcHAtYnJpZGdlLmNvbmZpZy50cyJdLAogICJzb3VyY2VzQ29udGVudCI6IFsiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIi9ob21lL2Vyci9kZXZlbC9vcmdzL29wZW4taGF4L29wZW5wbGFubmVyL3BhY2thZ2VzL2FnZW50cy9rbm94eC9mcm9udGVuZFwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL2hvbWUvZXJyL2RldmVsL29yZ3Mvb3Blbi1oYXgvb3BlbnBsYW5uZXIvcGFja2FnZXMvYWdlbnRzL2tub3h4L2Zyb250ZW5kL3ZpdGUuYXBwLWJyaWRnZS5jb25maWcudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL2hvbWUvZXJyL2RldmVsL29yZ3Mvb3Blbi1oYXgvb3BlbnBsYW5uZXIvcGFja2FnZXMvYWdlbnRzL2tub3h4L2Zyb250ZW5kL3ZpdGUuYXBwLWJyaWRnZS5jb25maWcudHNcIjtpbXBvcnQgeyBkZWZpbmVDb25maWcgfSBmcm9tIFwidml0ZVwiO1xuaW1wb3J0IHJlYWN0IGZyb20gXCJAdml0ZWpzL3BsdWdpbi1yZWFjdFwiO1xuaW1wb3J0IHBhdGggZnJvbSBcInBhdGhcIjtcblxuLy8gU3RhYmxlIGFwcC1zdXJmYWNlIGV4cG9ydHMgZm9yIHNoYWRvdy1jbGpzLlxuLy9cbi8vIFRoaXMgaXMgTk9UIHRoZSBTUEEgZW50cnlwb2ludCBidWlsZC4gc2hhZG93LWNsanMgb3ducyByb3V0aW5nIGFuZCBtb3VudGluZy5cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XG4gIHBsdWdpbnM6IFtyZWFjdCgpXSxcbiAgYnVpbGQ6IHtcbiAgICAvLyBFbWl0IC5tYXAgZmlsZXMgc28gYnJvd3NlciBzdGFjayB0cmFjZXMgbWFwIGJhY2sgdG8gVFMgc291cmNlcy5cbiAgICBzb3VyY2VtYXA6IHRydWUsXG4gICAgbGliOiB7XG4gICAgICBlbnRyeTogcGF0aC5yZXNvbHZlKF9fZGlybmFtZSwgXCJzcmMvYnJpZGdlL2FwcC50c1wiKSxcbiAgICAgIG5hbWU6IFwia25veHgtYXBwLWJyaWRnZVwiLFxuICAgICAgZmlsZU5hbWU6IChmb3JtYXQpID0+IGBrbm94eC1hcHAtYnJpZGdlLiR7Zm9ybWF0fS5qc2AsXG4gICAgICBmb3JtYXRzOiBbXCJlc1wiXSxcbiAgICB9LFxuICAgIHJvbGx1cE9wdGlvbnM6IHtcbiAgICAgIGV4dGVybmFsOiBbXG4gICAgICAgIFwicmVhY3RcIixcbiAgICAgICAgXCJyZWFjdC1kb21cIixcbiAgICAgICAgXCJyZWFjdC1kb20vY2xpZW50XCIsXG4gICAgICAgIFwicmVhY3QvanN4LXJ1bnRpbWVcIixcbiAgICAgICAgLy8gSU1QT1JUQU5UOiBrZWVwIHJvdXRlciBsaWJzIGV4dGVybmFsIHNvIHNoYWRvdy1jbGpzIGFuZCB0aGUgYXBwLWJyaWRnZVxuICAgICAgICAvLyBzaGFyZSBPTkUgcm91dGVyIGNvbnRleHQgaW5zdGFuY2UuIEJ1bmRsaW5nIHJlYWN0LXJvdXRlcigtZG9tKSBpbnRvIHRoZVxuICAgICAgICAvLyBicmlkZ2UgY2F1c2VzIGhvb2tzICh1c2VMb2NhdGlvbiwgZXRjLikgdG8gc2VlIGEgZGlmZmVyZW50IGNvbnRleHQgYW5kXG4gICAgICAgIC8vIHRocm93IFwibWF5IGJlIHVzZWQgb25seSBpbiB0aGUgY29udGV4dCBvZiBhIDxSb3V0ZXI+XCIuXG4gICAgICAgIFwiQHJlbWl4LXJ1bi9yb3V0ZXJcIixcbiAgICAgICAgXCJyZWFjdC1yb3V0ZXJcIixcbiAgICAgICAgXCJyZWFjdC1yb3V0ZXItZG9tXCIsXG4gICAgICBdLFxuICAgICAgb3V0cHV0OiB7XG4gICAgICAgIC8vIHNoYWRvdy1jbGpzIGNvbnN1bWVzIHRoaXMgZmlsZSB2aWEgOmpzLW9wdGlvbnMgOnJlc29sdmUgezp0YXJnZXQgOmZpbGUgLi4ufS5cbiAgICAgICAgLy8gVGhhdCByZXNvbHZlciBjYW5ub3QgZm9sbG93IGFkZGl0aW9uYWwgcmVsYXRpdmUgY2h1bmsgaW1wb3J0cy5cbiAgICAgICAgLy8gS2VlcCB0aGlzIGJyaWRnZSBhcyBhIHNpbmdsZSBzZWxmLWNvbnRhaW5lZCBFU00gZmlsZS5cbiAgICAgICAgaW5saW5lRHluYW1pY0ltcG9ydHM6IHRydWUsXG4gICAgICB9LFxuICAgIH0sXG4gICAgb3V0RGlyOiBcImRpc3QvYnJpZGdlXCIsXG4gICAgZW1wdHlPdXREaXI6IGZhbHNlLFxuICB9LFxufSk7XG4iXSwKICAibWFwcGluZ3MiOiAiO0FBQWdhLFNBQVMsb0JBQW9CO0FBQzdiLE9BQU8sV0FBVztBQUNsQixPQUFPLFVBQVU7QUFGakIsSUFBTSxtQ0FBbUM7QUFPekMsSUFBTyxpQ0FBUSxhQUFhO0FBQUEsRUFDMUIsU0FBUyxDQUFDLE1BQU0sQ0FBQztBQUFBLEVBQ2pCLE9BQU87QUFBQTtBQUFBLElBRUwsV0FBVztBQUFBLElBQ1gsS0FBSztBQUFBLE1BQ0gsT0FBTyxLQUFLLFFBQVEsa0NBQVcsbUJBQW1CO0FBQUEsTUFDbEQsTUFBTTtBQUFBLE1BQ04sVUFBVSxDQUFDLFdBQVcsb0JBQW9CLE1BQU07QUFBQSxNQUNoRCxTQUFTLENBQUMsSUFBSTtBQUFBLElBQ2hCO0FBQUEsSUFDQSxlQUFlO0FBQUEsTUFDYixVQUFVO0FBQUEsUUFDUjtBQUFBLFFBQ0E7QUFBQSxRQUNBO0FBQUEsUUFDQTtBQUFBO0FBQUE7QUFBQTtBQUFBO0FBQUEsUUFLQTtBQUFBLFFBQ0E7QUFBQSxRQUNBO0FBQUEsTUFDRjtBQUFBLE1BQ0EsUUFBUTtBQUFBO0FBQUE7QUFBQTtBQUFBLFFBSU4sc0JBQXNCO0FBQUEsTUFDeEI7QUFBQSxJQUNGO0FBQUEsSUFDQSxRQUFRO0FBQUEsSUFDUixhQUFhO0FBQUEsRUFDZjtBQUNGLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
