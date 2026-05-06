# Route File Migration Audit

**Date**: 2026-01-26
**Scope**: Move all route files into `knoxx.backend.routes.*` namespace

## Audit Complete

### Files Still Using `route!` (15 files)

| File | Uses | Notes |
|------|------|-------|
| `app_shapes.cljs:230` | **Defines** `route!` | Source of truth — stays |
| `app_routes.cljs` | Calls via helpers map (×8) | Passes to sub-route fns |
| `admin_routes.cljs` | Direct calls (×11) | `register-admin-routes!` |
| `contracts_routes.cljs` | Destructures from helpers (×2) | `do-route` wrapper |
| `document_routes.cljs` | Uses `defroute` macro | Already migrated |
| `memory_routes.cljs` | Uses `defroute` macro | Already migrated |
| `model_routes.cljs` | Direct calls (×13) | `register-model-routes!` |
| `multimodal_routes.cljs` | Direct calls (×5) | `register-multimodal-routes!` |
| `tool_routes.cljs` | Uses `defroute` macro | Already migrated |
| `voice_routes.cljs` | Direct calls (×4) | `register-voice-routes!` |
| `workspace_media_routes.cljs` | Direct calls (×4) | `register-workspace-media-routes!` |
| `translation_routes.cljs` | Direct calls (×14) | `register-translation-routes!` |
| `mcp_http.cljs` | Direct calls + `defroute` | Mixed usage |
| `users/admin.cljs` | Direct calls (×8) | `register-admin-user-routes!` |
| `studio_routes.cljs` | Uses `defroute` macro | Already migrated |
| `tools/proxy_routes.cljs` | Unknown | Needs inspection |

### Files to Move into `knoxx.backend.routes.*`

Current → Target:

```
knoxx.backend.app-routes          → knoxx.backend.routes.app
knoxx.backend.admin-routes        → knoxx.backend.routes.admin
knoxx.backend.contracts-routes    → knoxx.backend.routes.contracts
knoxx.backend.document-routes     → knoxx.backend.routes.documents
knoxx.backend.memory-routes       → knoxx.backend.routes.memory
knoxx.backend.model-routes        → knoxx.backend.routes.models
knoxx.backend.multimodal-routes   → knoxx.backend.routes.multimodal
knoxx.backend.tool-routes         → knoxx.backend.routes.tools
knoxx.backend.voice-routes        → knoxx.backend.routes.voice
knoxx.backend.workspace-media-routes → knoxx.backend.routes.workspace-media
knoxx.backend.translation-routes  → knoxx.backend.routes.translation
knoxx.backend.studio-routes       → knoxx.backend.routes.studio
knoxx.backend.mcp-http            → knoxx.backend.routes.mcp (partial)
knoxx.backend.users/admin         → knoxx.backend.routes.users.admin
```

### Migration Strategy

1. **Create new namespace structure** under `routes/`
2. **Move each file** with namespace rename
3. **Update all require/require-macros** across the codebase
4. **Keep `app_shapes.cljs`** where it is (defines `route!`)
5. **Update shadow-cljs build config** if paths are hardcoded

This is a large mechanical refactoring touching ~20 files.
