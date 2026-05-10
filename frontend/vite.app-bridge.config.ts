import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// Stable app-surface exports for shadow-cljs.
//
// This is NOT the SPA entrypoint build. shadow-cljs owns routing and mounting.
export default defineConfig({
  plugins: [react()],
  build: {
    // Emit .map files so browser stack traces map back to TS sources.
    sourcemap: true,
    lib: {
      entry: path.resolve(__dirname, "src/bridge/app.ts"),
      name: "knoxx-app-bridge",
      fileName: (format) => `knoxx-app-bridge.${format}.js`,
      formats: ["es"],
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
        "react-router-dom",
      ],
      output: {
        // shadow-cljs consumes this file via :js-options :resolve {:target :file ...}.
        // That resolver cannot follow additional relative chunk imports.
        // Keep this bridge as a single self-contained ESM file.
        inlineDynamicImports: true,
      },
    },
    outDir: "dist/bridge",
    emptyOutDir: false,
  },
});
