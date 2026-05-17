import React from "react";
import ReactDOM from "react-dom/client";

// Expose the *same* React instances to the shadow-cljs bundle during the
// Vite→shadow migration, so Helix hooks use the renderer dispatcher from this
// React root (prevents: resolveDispatcher() is null).
(globalThis as unknown as { React?: unknown; ReactDOMClient?: unknown }).React = React;
(globalThis as unknown as { React?: unknown; ReactDOMClient?: unknown }).ReactDOMClient = ReactDOM;

// Load the shadow-cljs bundle *after* React is on globalThis.
// This prevents Helix hooks from seeing a different React instance and failing
// with `resolveDispatcher() is null` / "Invalid hook call".
if (!(globalThis as unknown as { __knoxxShadowLoaded?: boolean }).__knoxxShadowLoaded) {
  (globalThis as unknown as { __knoxxShadowLoaded?: boolean }).__knoxxShadowLoaded = true;
  const script = document.createElement("script");
  script.src = "/cljs/app.js";
  script.async = true;
  document.head.appendChild(script);
}
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <App />
    </BrowserRouter>
  </React.StrictMode>
);
