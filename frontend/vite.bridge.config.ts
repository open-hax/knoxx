import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  esbuild: {
    jsxDev: false,
  },
  build: {
    // Do not emit source maps for shadow-consumed bridge files. shadow-cljs
    // follows source map metadata when file-resolving ESM and can rehydrate
    // dev JSX (`jsxDEV`) into the optimized bundle, which crashes under the
    // production React runtime.
    sourcemap: false,
    lib: {
      entry: path.resolve(__dirname, "src/bridge/index.ts"),
      name: "knoxx-frontend-bridge",
      fileName: (format) => `knoxx-frontend-bridge.${format}.js`,
      formats: ["es"],
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
        "react-router-dom",
      ],
      output: {
        globals: {
          react: "React",
          "react-dom": "ReactDOM",
          "react-dom/client": "ReactDOMClient",
        },
      },
    },
    outDir: "dist/bridge",
    // This directory is shared with other build outputs (e.g. knoxx-app-bridge).
    // In watch mode, emptying it will delete sibling artifacts and break shadow's
    // file-resolved modules at runtime.
    emptyOutDir: false,
  },
});
