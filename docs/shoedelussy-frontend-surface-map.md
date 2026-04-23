# Shoedelussy frontend surface map

This document gives Knoxx a grounded map of the current Shoedelussy frontend until the UI is decomposed into importable capability components.

## Source of truth

Local checkout:

- `/home/err/.knoxx/external/shoedelussy/ui`

Primary route files:

- `ui/src/App.tsx`
- `ui/src/pages/HomePage.tsx`
- `ui/src/pages/ProjectsPage.tsx`

## Current frontend routes

### `/`

Backed by `HomePage.tsx`.

This is the main live-coding DAW surface. It wires:

- `ProjectTopbar`
- `TransportBar`
- `EditorPanel`
- `ChatPanel`
- `DAWShell`
- `VisualizationSurface`
- `useChatOrchestrator`

Important query parameters already used by the frontend:

- `?project=<id>` — load a durable project
- `?template=demo` — open demo project
- `?template=empty` — open blank project
- `?share=<id>` — load a shared code snapshot into a local copy
- `?autoplay=1` — after the project/share loads and the editor is initialized, the page attempts playback automatically
- `?render=1` — currently a semantic flag for render/playback links; no dedicated render-only page exists yet
- `?export=wav` — when combined with active playback, the frontend attempts browser-side WAV capture/download
- `?duration_ms=<ms>` — approximate browser-side WAV capture duration for export links

### `/projects`

Backed by `ProjectsPage.tsx`.

This is the durable project browser. It shows project cards, search, open/delete actions, and new-project entry points.

## Playback coupling

The frontend is still tightly coupled to browser Strudel playback.

The real playback engine lives in:

- `ui/src/components/StrudelEditor.tsx`

Key facts:

- playback is browser-side via Strudel packages and Web Audio
- `StrudelEditor` exposes imperative bridge methods like `play`, `stop`, `evaluate`, `setCode`, and `getCode`
- `useChatOrchestrator` owns the editor bridge and drives most project/chat behavior
- the current `render_loop` MCP tool is honest browser-render plumbing: it creates a share URL with autoplay/render query params so a human can open the loop in the frontend and hear it
- the new `render_wav` MCP tool returns a browser URL that attempts WAV capture/download in this same shell; it is still frontend-coupled, not a headless worker export
- Knoxx can now drive that export URL through `backend/scripts/shoedelussy-capture-wav.mjs` using headless Chromium to save a real `.wav` artifact, but this is still frontend automation rather than an independent audio backend
- there is no true server-side wav/mp3 export path yet

## Export/share surface

The export/share affordances currently live in:

- `ui/src/components/ProjectTopbar.tsx`

Visible user actions:

- New Project
- Load Demo
- Export .txt
- Export .strudel
- Share

The share flow is still code-first, not audio-first:

- server creates a share id in `SHARES_KV`
- frontend rehydrates the code from `?share=<id>`
- browser playback happens after load

## Capability implications for Knoxx

Until components are imported directly into Knoxx, the safest mental model is:

1. Shoedelussy MCP manipulates code/project state.
2. Shoedelussy frontend is the human-facing playback/render shell.
3. `render_loop` returns a browser route into that shell, not a server audio file.
4. `render_wav` returns a browser route that attempts WAV capture/download inside that shell, but it still depends on frontend playback/browser permissions.
5. `backend/scripts/shoedelussy-capture-wav.mjs` lets Knoxx automate that same route with headless Chromium so the exported wave file lands back in the workspace.

## Candidate future decomposition

Natural capability/user-surface seams:

- project browser surface (`/projects`)
- live coding editor surface (`/` + `EditorPanel` + `StrudelEditor`)
- transport/playback surface (`TransportBar`)
- export/share surface (`ProjectTopbar` export tab)
- AI chat patch/review surface (`ChatPanel`)
- visualization surface (`VisualizationSurface`)

## Current limitation

Knoxx should treat Shoedelussy as a hybrid:

- backend tools for state mutation and links
- frontend routes/components for actual listening and human review

That is the coupling to preserve explicitly until the frontend is broken down into reusable imported surfaces.
