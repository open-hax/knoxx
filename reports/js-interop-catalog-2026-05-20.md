# JS/CLJS interop catalog outside extern

Generated: 2026-05-20

## Scope

- Included: `backend/src/cljs/**/*.{cljs,cljc,clj}`
- Excluded: `backend/src/cljs/knoxx/backend/extern/**`
- Scanner ignores semicolon comments and string literals. It does not execute/parse macros; there were no `#_` reader-comment hits for these tokens in scope.
- Searched tokens: `clj->js`, `js->clj`, `js->cljs`, and `#js`. Exact `js->cljs` does not occur; `js->clj` is included because it is the actual ClojureScript inverse conversion present in the codebase.

## Totals

| Token | Count |
|---|---:|
| `#js` | 383 |
| `clj->js` | 296 |
| `js->clj` | 126 |
| `js->cljs` | 0 |
| **Total** | **805** |

Files with hits: **88**

## File counts

| File | #js | clj->js | js->clj | js->cljs | Total | Note |
|---|---:|---:|---:|---:|---:|---|
| `backend/src/cljs/knoxx/backend/infra/eta_mu_session_ingester.cljs` | 59 | 1 | 0 | 0 | 60 | high-thought event ingestion: raw event/session JS shapes are constructed and batched before HTTP. |
| `backend/src/cljs/knoxx/backend/domain/bluesky/bluesky.cljs` | 54 | 3 | 0 | 0 | 57 | high-thought third-party API: ATProto wire shapes and rich-text facets leak through domain logic. |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 36 | 8 | 3 | 0 | 47 | high-thought Discord API/gateway: SDK events/payloads/voice data cross several functions before normalization. |
| `backend/src/cljs/knoxx/backend/domain/discord/voice_tools.cljs` | 21 | 19 | 3 | 0 | 43 | high-thought Discord API/gateway: SDK events/payloads/voice data cross several functions before normalization. |
| `backend/src/cljs/knoxx/backend/domain/discord/tools.cljs` | 20 | 19 | 0 | 0 | 39 | high-thought Discord API/gateway: SDK events/payloads/voice data cross several functions before normalization. |
| `backend/src/cljs/knoxx/backend/infra/auth/session.cljs` | 9 | 18 | 0 | 0 | 27 | auth/session boundary: cookies, Redis, policy DB, OAuth payloads; mixed immediate and propagated shapes. |
| `backend/src/cljs/knoxx/backend/infra/routes/auth.cljs` | 0 | 25 | 0 | 0 | 25 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs` | 0 | 10 | 8 | 0 | 18 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/domain/discord/source.cljs` | 5 | 5 | 6 | 0 | 16 | high-thought Discord API/gateway: SDK events/payloads/voice data cross several functions before normalization. |
| `backend/src/cljs/knoxx/backend/domain/music.cljs` | 8 | 2 | 5 | 0 | 15 | media/node/fetch boundary: mostly immediate options/arrays, some provider payloads. |
| `backend/src/cljs/knoxx/backend/infra/routes/mcp.cljs` | 0 | 15 | 0 | 0 | 15 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs` | 0 | 7 | 8 | 0 | 15 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/domain/sandbox_container.cljs` | 6 | 4 | 4 | 0 | 14 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 9 | 4 | 1 | 0 | 14 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/mcp/mcp_bridge.cljs` | 1 | 10 | 2 | 0 | 13 | MCP wire boundary: tool schemas/session/token payloads need dedicated extern wrappers. |
| `backend/src/cljs/knoxx/backend/domain/session_mycology.cljs` | 12 | 1 | 0 | 0 | 13 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 0 | 7 | 6 | 0 | 13 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/routes/tools.cljs` | 0 | 7 | 6 | 0 | 13 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/domain/media/blaze.cljs` | 6 | 4 | 2 | 0 | 12 | media/node/fetch boundary: mostly immediate options/arrays, some provider payloads. |
| `backend/src/cljs/knoxx/backend/infra/db/policy.cljs` | 0 | 6 | 6 | 0 | 12 | DB adapter boundary: JS row/param/result shapes; likely extern policy/pg adapter. |
| `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs` | 6 | 5 | 1 | 0 | 12 | OpenPlanner API/wire boundary; request/response normalization belongs in extern/openplanner. |
| `backend/src/cljs/knoxx/backend/infra/agent/resume.cljs` | 6 | 5 | 0 | 0 | 11 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/openplanner/tools.cljs` | 6 | 5 | 0 | 0 | 11 | OpenPlanner API/wire boundary; request/response normalization belongs in extern/openplanner. |
| `backend/src/cljs/knoxx/backend/domain/label/audio.cljs` | 2 | 4 | 4 | 0 | 10 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/voice/tools.cljs` | 6 | 2 | 2 | 0 | 10 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs` | 0 | 8 | 2 | 0 | 10 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/bootstrap.cljs` | 9 | 0 | 0 | 0 | 9 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/contracts/tools.cljs` | 7 | 1 | 1 | 0 | 9 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/realtime.cljs` | 7 | 1 | 0 | 0 | 8 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/tools.cljs` | 4 | 3 | 1 | 0 | 8 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/redis_client.cljs` | 2 | 3 | 3 | 0 | 8 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/routes/documents.cljs` | 0 | 2 | 6 | 0 | 8 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs` | 0 | 7 | 1 | 0 | 8 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/routes/voice.cljs` | 0 | 4 | 4 | 0 | 8 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/shape/app_shapes.cljs` | 3 | 0 | 5 | 0 | 8 | shape/schema boundary: JS schema objects likely deliberate; decide if shape namespace is an allowed exception. |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 2 | 2 | 3 | 0 | 7 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/nrepl.cljs` | 1 | 5 | 1 | 0 | 7 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/document_state.cljs` | 3 | 4 | 0 | 0 | 7 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/http_server.cljs` | 7 | 0 | 0 | 0 | 7 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 0 | 4 | 3 | 0 | 7 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/source/opencode_session_ingester.cljs` | 7 | 0 | 0 | 0 | 7 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/actor/tools.cljs` | 2 | 3 | 1 | 0 | 6 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/contracts/loader.cljs` | 4 | 2 | 0 | 0 | 6 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/extension_runtime.cljs` | 6 | 0 | 0 | 0 | 6 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/mcp/mcp_expose.cljs` | 2 | 3 | 1 | 0 | 6 | MCP wire boundary: tool schemas/session/token payloads need dedicated extern wrappers. |
| `backend/src/cljs/knoxx/backend/domain/media/workspace.cljs` | 4 | 2 | 0 | 0 | 6 | media/node/fetch boundary: mostly immediate options/arrays, some provider payloads. |
| `backend/src/cljs/knoxx/backend/domain/text.cljs` | 3 | 1 | 2 | 0 | 6 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/core.cljs` | 4 | 2 | 0 | 0 | 6 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/actor/mailbox.cljs` | 0 | 2 | 3 | 0 | 5 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/media.cljs` | 4 | 1 | 0 | 0 | 5 | media/node/fetch boundary: mostly immediate options/arrays, some provider payloads. |
| `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs` | 0 | 4 | 1 | 0 | 5 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/svg_render.cljs` | 5 | 0 | 0 | 0 | 5 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/action/invoke_sub_agent.cljs` | 0 | 4 | 0 | 0 | 4 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/discord/discord_io.cljs` | 4 | 0 | 0 | 0 | 4 | high-thought Discord API/gateway: SDK events/payloads/voice data cross several functions before normalization. |
| `backend/src/cljs/knoxx/backend/domain/policy/sql_adapter.cljs` | 1 | 2 | 1 | 0 | 4 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/agent/recovery.cljs` | 2 | 1 | 1 | 0 | 4 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/graceful_shutdown.cljs` | 3 | 1 | 0 | 0 | 4 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/routes/contracts.cljs` | 0 | 3 | 1 | 0 | 4 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 0 | 4 | 0 | 0 | 4 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/stores/session_store.cljs` | 0 | 2 | 2 | 0 | 4 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/node/fs.cljs` | 3 | 0 | 0 | 0 | 3 | node wrapper layer: acceptable-ish adjacent boundary, but outside extern by requested scope. |
| `backend/src/cljs/knoxx/backend/infra/agent/hydration.cljs` | 0 | 2 | 1 | 0 | 3 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/agent/provider.cljs` | 3 | 0 | 0 | 0 | 3 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/agent/stream.cljs` | 1 | 0 | 2 | 0 | 3 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/auth/authz.cljs` | 1 | 0 | 2 | 0 | 3 | auth/session boundary: cookies, Redis, policy DB, OAuth payloads; mixed immediate and propagated shapes. |
| `backend/src/cljs/knoxx/backend/infra/routes/actors.cljs` | 0 | 1 | 2 | 0 | 3 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/routes/workspace_media.cljs` | 0 | 3 | 0 | 0 | 3 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/domain/agent/agent_templates.cljs` | 2 | 0 | 0 | 0 | 2 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/event/dispatch.cljs` | 0 | 1 | 1 | 0 | 2 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/node/path.cljs` | 0 | 2 | 0 | 0 | 2 | node wrapper layer: acceptable-ish adjacent boundary, but outside extern by requested scope. |
| `backend/src/cljs/knoxx/backend/domain/openutau/tools.cljs` | 2 | 0 | 0 | 0 | 2 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/policy/edn_adapter.cljs` | 2 | 0 | 0 | 0 | 2 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/core_memory.cljs` | 0 | 1 | 1 | 0 | 2 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/routes/multimodal.cljs` | 0 | 2 | 0 | 0 | 2 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/stores/redis_session_store.cljs` | 0 | 1 | 1 | 0 | 2 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/action/run_state.cljs` | 0 | 0 | 1 | 0 | 1 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/actor/credentials.cljs` | 0 | 0 | 1 | 0 | 1 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/agent/content.cljs` | 0 | 0 | 1 | 0 | 1 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/domain/media/multimodal.cljs` | 0 | 1 | 0 | 0 | 1 | media/node/fetch boundary: mostly immediate options/arrays, some provider payloads. |
| `backend/src/cljs/knoxx/backend/domain/twitch.cljs` | 0 | 1 | 0 | 0 | 1 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/agent/policy.cljs` | 0 | 0 | 1 | 0 | 1 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/agent/tools.cljs` | 0 | 0 | 1 | 0 | 1 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/agent/transcript.cljs` | 0 | 0 | 1 | 0 | 1 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/openplanner/semantic.cljs` | 0 | 1 | 0 | 0 | 1 | OpenPlanner API/wire boundary; request/response normalization belongs in extern/openplanner. |
| `backend/src/cljs/knoxx/backend/infra/routes/models.cljs` | 0 | 1 | 0 | 0 | 1 | route/HTTP boundary: Fastify request/reply, fetch opts, DB args; likely migrate behind extern.fastify/http/db helpers. |
| `backend/src/cljs/knoxx/backend/infra/stores/composite_session_store.cljs` | 0 | 1 | 0 | 0 | 1 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/infra/tooling.cljs` | 0 | 1 | 0 | 0 | 1 | mixed boundary; inspect occurrence before mechanical replacement. |
| `backend/src/cljs/knoxx/backend/tools/mcp.cljs` | 1 | 0 | 0 | 0 | 1 | mixed boundary; inspect occurrence before mechanical replacement. |

## High-thought clusters

These are the places most likely to need design work rather than a local wrapper swap, because JS data is built/read and then passed across multiple functions or persisted/batched before normalization.

- **Eta-mu session ingestion** — 60 hits (`#js` 59, `clj->js` 1, `js->clj` 0).
- **Bluesky/ATProto** — 57 hits (`#js` 54, `clj->js` 3, `js->clj` 0).
- **Discord gateway/tools/voice/source** — 149 hits (`#js` 86, `clj->js` 51, `js->clj` 12).
- **Policy DB adapter** — 12 hits (`#js` 0, `clj->js` 6, `js->clj` 6).
- **MCP bridge/routes** — 19 hits (`#js` 3, `clj->js` 13, `js->clj` 3).
- **MCP route** — 15 hits (`#js` 0, `clj->js` 15, `js->clj` 0).
- **Auth/session** — 27 hits (`#js` 9, `clj->js` 18, `js->clj` 0).
- **Route layer bulk** — 162 hits (`#js` 0, `clj->js` 114, `js->clj` 48).

## Full occurrence catalog

### `backend/src/cljs/knoxx/backend/bootstrap.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 70 | 3 | `#js` | `#js {:connectionString (or (aget js/process.env "KNOXX_POLICY_DATABASE_URL")` |
| 93 | 27 | `#js` | `#js {:pid (.-pid js/process)` |
| 112 | 56 | `#js` | `(auth-routes/register-auth-routes app #js {:policyDb policyDb` |
| 158 | 50 | `#js` | `(discord-gateway/createDiscordGatewayManager #js {:log js/console})` |
| 163 | 32 | `#js` | `(let [runtime #js {}]` |
| 173 | 9 | `#js` | `#js {:pid (.-pid js/process)` |
| 178 | 22 | `#js` | `#js {:pid (.-pid js/process)` |
| 187 | 9 | `#js` | `#js {:pid (.-pid js/process)` |
| 194 | 26 | `#js` | `#js {:pid (.-pid js/process)` |
### `backend/src/cljs/knoxx/backend/domain/action/invoke_sub_agent.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 307 | 25 | `clj->js` | `(-> (js/Promise.race (clj->js [(spawn-once! ctx payload)` |
| 336 | 24 | `clj->js` | `(-> (js/Promise.all (clj->js (mapv #(run-payload! ctx %) payloads)))` |
| 374 | 28 | `clj->js` | `(-> (js/Promise.all (clj->js (mapv #(payload-for-id! ctx action-with shared-config %) sub-agent-ids)))` |
| 379 | 45 | `clj->js` | `(-> (js/Promise.all (clj->js (mapv #(fire-and-forget! ctx %) payloads)))` |
### `backend/src/cljs/knoxx/backend/domain/action/run_state.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 52 | 21 | `js->clj` | `(array? value) (js->clj value :keywordize-keys true)` |
### `backend/src/cljs/knoxx/backend/domain/actor/credentials.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 28 | 8 | `js->clj` | `(js->clj credential :keywordize-keys true))))` |
### `backend/src/cljs/knoxx/backend/domain/actor/mailbox.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 117 | 22 | `clj->js` | `(query-fn sql (clj->js params))` |
| 123 | 24 | `clj->js` | `(.stringify js/JSON (clj->js (or value {}))))` |
| 268 | 27 | `js->clj` | `(string? value) (try (js->clj (.parse js/JSON value) :keywordize-keys true)` |
| 270 | 21 | `js->clj` | `(array? value) (js->clj value :keywordize-keys true)` |
| 271 | 12 | `js->clj` | `:else (js->clj value :keywordize-keys true)))` |
### `backend/src/cljs/knoxx/backend/domain/actor/tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 26 | 17 | `#js` | `headers #js {"Content-Type" "application/json"` |
| 40 | 17 | `#js` | `#js {:method method` |
| 42 | 60 | `clj->js` | `:body (when body (.stringify js/JSON (clj->js body)))})` |
| 294 | 61 | `js->clj` | `(let [result* (js->clj result :keywordize-keys true)]` |
| 335 | 7 | `clj->js` | `(clj->js [(actors-send-message-tool runtime config)])` |
| 336 | 7 | `clj->js` | `(clj->js []))))` |
### `backend/src/cljs/knoxx/backend/domain/agent/agent_templates.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 16 | 31 | `#js` | `(def ^:private lookup-missing #js {})` |
| 93 | 28 | `#js` | `(def ^:private env-missing #js {})` |
### `backend/src/cljs/knoxx/backend/domain/agent/content.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 299 | 51 | `js->clj` | `(let [part (if (map? part) part (js->clj part :keywordize-keys true))` |
### `backend/src/cljs/knoxx/backend/domain/bluesky/bluesky.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 116 | 17 | `#js` | `#js {:method "POST"` |
| 117 | 31 | `#js` | `:headers #js {"Content-Type" "application/json"` |
| 119 | 48 | `#js` | `:body (.stringify js/JSON #js {:identifier identifier :password password})}` |
| 124 | 17 | `#js` | `#js {:method "POST"` |
| 125 | 31 | `#js` | `:headers #js {"Content-Type" mime-type` |
| 138 | 4 | `#js` | `#js {:method "POST"` |
| 139 | 18 | `#js` | `:headers #js {"Content-Type" "application/json"` |
| 143 | 27 | `#js` | `#js {:repo (aget session "did")` |
| 151 | 4 | `#js` | `#js {:method "POST"` |
| 152 | 18 | `#js` | `:headers #js {"Content-Type" "application/json"` |
| 156 | 27 | `#js` | `#js {:repo (aget session "did")` |
| 173 | 8 | `#js` | `#js {:method "GET"` |
| 174 | 22 | `#js` | `:headers #js {"Accept" "application/json"` |
| 214 | 19 | `#js` | `#js {:method "GET"` |
| 215 | 33 | `#js` | `:headers #js {"Accept" "application/json"` |
| 259 | 19 | `#js` | `#js {:method "GET"` |
| 260 | 33 | `#js` | `:headers #js {"Accept" "application/json"` |
| 278 | 8 | `#js` | `#js {:method "GET"` |
| 279 | 22 | `#js` | `:headers #js {"Accept" "application/json"` |
| 305 | 17 | `#js` | `#js {:method "GET"` |
| 306 | 31 | `#js` | `:headers #js {"Accept" "application/json"` |
| 344 | 40 | `#js` | `(conj acc #js {"$type" "app.bsky.richtext.facet"` |
| 345 | 52 | `#js` | `:index #js {:byteStart start-byte :byteEnd end-byte}` |
| 346 | 55 | `#js` | `:features #js [#js {"$type" "app.bsky.richtext.facet#tag"` |
| 346 | 60 | `#js` | `:features #js [#js {"$type" "app.bsky.richtext.facet#tag"` |
| 348 | 26 | `clj->js` | `(when (seq matches) (clj->js matches))))` |
| 353 | 11 | `clj->js` | `(clj->js` |
| 362 | 29 | `#js` | `#js {:alt alt :image blob})))))` |
| 365 | 18 | `#js` | `#js {"$type" "app.bsky.embed.images" :images uploaded})))` |
| 373 | 18 | `#js` | `#js {:parent #js {:uri (:uri parent) :cid (:cid parent)}` |
| 373 | 31 | `#js` | `#js {:parent #js {:uri (:uri parent) :cid (:cid parent)}` |
| 374 | 29 | `#js` | `:root #js {:uri (:uri parent) :cid (:cid parent)}})))))` |
| 383 | 42 | `#js` | `record #js {"$type" "app.bsky.feed.post"` |
| 474 | 30 | `#js` | `#js {"$type" "app.bsky.feed.repost"` |
| 475 | 44 | `#js` | `:subject #js {:uri (:uri post) :cid (:cid post)}` |
| 485 | 30 | `#js` | `#js {"$type" "app.bsky.feed.like"` |
| 486 | 44 | `#js` | `:subject #js {:uri (:uri post) :cid (:cid post)}` |
| 504 | 30 | `#js` | `#js {"$type" "app.bsky.graph.follow"` |
| 557 | 8 | `#js` | `#js {:method "GET"` |
| 558 | 22 | `#js` | `:headers #js {"Accept" "application/json"` |
| 584 | 17 | `#js` | `#js {:method "GET"` |
| 585 | 31 | `#js` | `:headers #js {"Accept" "application/json"` |
| 594 | 8 | `#js` | `#js {:method "GET"` |
| 595 | 22 | `#js` | `:headers #js {"Accept" "application/json"` |
| 613 | 8 | `#js` | `#js {:method "GET"` |
| 614 | 22 | `#js` | `:headers #js {"Accept" "application/json"` |
| 637 | 4 | `#js` | `#js {:method method` |
| 638 | 18 | `#js` | `:headers #js {"Accept" "application/json"` |
| 656 | 26 | `#js` | `(let [msg #js {"$type" "chat.bsky.convo.defs#messageInput"` |
| 659 | 40 | `#js` | `(aset msg "replyTo" #js {"$type" "chat.bsky.convo.defs#messageRef"` |
| 663 | 19 | `#js` | `#js {:method "POST"` |
| 664 | 33 | `#js` | `:headers #js {"Content-Type" "application/json"` |
| 667 | 50 | `#js` | `:body (.stringify js/JSON #js {:convoId convo-id :message msg})}` |
| 675 | 17 | `#js` | `#js {:method "POST"` |
| 676 | 31 | `#js` | `:headers #js {"Content-Type" "application/json"` |
| 680 | 40 | `#js` | `#js {:convoId convo-id` |
| 1104 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/contracts/loader.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 127 | 25 | `#js` | `(-> (.readdir fs root #js {:withFileTypes true :recursive true})` |
| 231 | 37 | `#js` | `(->> (.readdirSync node-fs root #js {:withFileTypes true :recursive true})` |
| 297 | 26 | `clj->js` | `(-> (js/Promise.all (clj->js (map discover-contract-files! roots)))` |
| 304 | 35 | `clj->js` | `(js/Promise.all (clj->js (map read-contract-file! files)))))` |
| 342 | 65 | `#js` | `(let [entries (.readdirSync node-fs (.join path root klass) #js {:withFileTypes true :recursive true})]` |
| 375 | 18 | `#js` | `(.mkdir fs dir #js {:recursive true}))` |
### `backend/src/cljs/knoxx/backend/domain/contracts/tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 70 | 28 | `#js` | `#js {:method "GET"})` |
| 86 | 28 | `#js` | `#js {:method "GET"})` |
| 105 | 28 | `#js` | `#js {:method "PUT"` |
| 106 | 42 | `#js` | `:headers #js {"Content-Type" "text/plain; charset=utf-8"}` |
| 123 | 19 | `#js` | `#js {:method "POST"` |
| 124 | 33 | `#js` | `:headers #js {"Content-Type" "application/json"}` |
| 125 | 50 | `#js` | `:body (.stringify js/JSON #js {:edn_text edn-text :contract_class klass})})` |
| 128 | 27 | `js->clj` | `(let [r (js->clj result :keywordize-keys true)` |
| 213 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/discord/discord_io.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 16 | 3 | `#js` | `#js {"Authorization" (str "Bot " token)` |
| 53 | 10 | `#js` | `#js {:method "GET"` |
| 79 | 22 | `#js` | `#js {:method "GET"` |
| 92 | 22 | `#js` | `#js {:method "GET"` |
### `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 108 | 9 | `#js` | `#js []))` |
| 110 | 7 | `#js` | `#js [])))` |
| 118 | 5 | `#js` | `#js {:id (.-id message)` |
| 130 | 26 | `#js` | `#js {:id (.-id att)` |
| 137 | 21 | `#js` | `#js {:title (or (.-title embed) nil)` |
| 161 | 7 | `#js` | `#js [normalized]` |
| 162 | 25 | `#js` | `(let [parts (atom #js [])` |
| 174 | 45 | `#js` | `(swap! parts (fn [p] (.concat p #js [(.trimEnd (.slice r 0 split-at))])))` |
| 177 | 43 | `#js` | `(swap! parts (fn [p] (.concat p #js [@remaining]))))` |
| 195 | 5 | `#js` | `#js {:attachment buffer` |
| 236 | 16 | `#js` | `mapped #js {:emoji (or (.-name emoji) "")` |
| 264 | 16 | `#js` | `mapped #js {:action action` |
| 323 | 26 | `clj->js` | `(clj->js {:intents [(.-Guilds GatewayIntentBits)` |
| 391 | 13 | `#js` | `(cond-> #js {:started (some? c)` |
| 408 | 15 | `#js` | `#js {:id (.-id guild)` |
| 426 | 44 | `#js` | `#js {:id (.-id ch)` |
| 436 | 37 | `#js` | `(let [promises (atom #js [])]` |
| 439 | 50 | `#js` | `(.concat ps #js [(-> (collect guild)` |
| 443 | 69 | `#js` | `#js [])))]))))` |
| 446 | 44 | `#js` | `(let [flat (atom #js [])]` |
| 461 | 40 | `clj->js` | `(clj->js {:limit (max 1 (min 100 (or (aget opts "limit") 50)))` |
| 479 | 51 | `clj->js` | `(clj->js {:limit (max 1 (min 100 (or (aget opts "limit") 50)))` |
| 482 | 51 | `#js` | `#js {:dmChannelId (.-id dm)` |
| 504 | 30 | `clj->js` | `(clj->js {:limit 100 :before (aget opts "before")}))` |
| 507 | 22 | `#js` | `#js {:dmChannelId (aget result "dmChannelId")` |
| 512 | 35 | `clj->js` | `(clj->js {:limit 100` |
| 517 | 22 | `#js` | `#js {:channelId (aget opts "channelId")` |
| 540 | 68 | `clj->js` | `(let [payload (clj->js {:content chunk})]` |
| 542 | 80 | `clj->js` | `(aset payload "reply" (clj->js {:messageReference reply-to})))` |
| 549 | 42 | `#js` | `#js {:channelId channel-id` |
| 574 | 41 | `#js` | `#js {:channelId channel-id` |
| 610 | 34 | `#js` | `stream (new Readable #js {:read (fn [])})` |
| 614 | 23 | `#js` | `#js {:inputType (.-Arbitrary (aget voice "StreamType"))})]` |
| 625 | 54 | `#js` | `opus-stream (.subscribe receiver user-id #js {:mode "opus"})]` |
| 647 | 49 | `#js` | `#js {:userId (.-id user)` |
| 684 | 34 | `#js` | `(new OpusDecoder #js {:rate 48000 :channels 2 :frameSize 960})))` |
| 733 | 52 | `#js` | `(swap! pcm-buffers assoc uid #js [keep-pcm])` |
| 756 | 50 | `#js` | `(swap! pcm-buffers assoc uid #js []))` |
| 870 | 19 | `#js` | `#js {:start (fn [token] (gw-start client-state ready-promise current-token listeners log this-stop build-client token))` |
| 894 | 49 | `#js` | `#js {:guildId guild-id :channelId channel-id :joined true}))))` |
| 897 | 38 | `#js` | `#js {:guildId guild-id :left true})` |
| 980 | 52 | `#js` | `(let [manager (createDiscordGatewayManager #js {:log js/console :setDefault false})]` |
| 990 | 28 | `js->clj` | `:status (js->clj (.status manager) :keywordize-keys true)})))))` |
| 994 | 15 | `js->clj` | `(let [rows (js->clj (or credentials #js []) :keywordize-keys true)` |
| 994 | 39 | `#js` | `(let [rows (js->clj (or credentials #js []) :keywordize-keys true)` |
| 1011 | 11 | `clj->js` | `(clj->js` |
| 1019 | 19 | `js->clj` | `(js->clj results :keywordize-keys true))))))` |
### `backend/src/cljs/knoxx/backend/domain/discord/source.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 52 | 3 | `#js` | `#js {"Authorization" (str "Bot " token)` |
| 137 | 66 | `clj->js` | `(-> (fetch-channel-from-gateway-entries! entries channel-id (clj->js {:limit (max 1 (min 100 (or limit 25)))}))` |
| 139 | 24 | `js->clj` | `(->> (js->clj messages :keywordize-keys true)` |
| 149 | 14 | `#js` | `#js {:method "GET"` |
| 164 | 13 | `clj->js` | `(clj->js` |
| 167 | 41 | `#js` | `(.catch (fn [_] #js []))))` |
| 172 | 32 | `js->clj` | `(map #(js->clj % :keywordize-keys true))` |
| 176 | 25 | `clj->js` | `clj->js))))` |
| 178 | 31 | `#js` | `(js/Promise.resolve #js [])))))` |
| 186 | 30 | `js->clj` | `(let [rows (js->clj channels :keywordize-keys true)` |
| 220 | 9 | `clj->js` | `(clj->js` |
| 281 | 28 | `clj->js` | `(-> (js/Promise.all (clj->js (mapv fetch-row! channel-ids)))` |
| 283 | 40 | `js->clj` | `(dispatch-summary! (js->clj results :keywordize-keys true))))))))` |
| 293 | 76 | `#js` | `(dg/start-actor-gateways! (or (aget result "credentials") #js []))))` |
| 305 | 65 | `js->clj` | `(assoc (js->clj mapped :keywordize-keys true)` |
| 311 | 77 | `js->clj` | `(assoc (js->clj mapped :keywordize-keys true)` |
### `backend/src/cljs/knoxx/backend/domain/discord/tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 38 | 3 | `#js` | `#js {"Authorization" (str "Bot " token)` |
| 199 | 25 | `#js` | `#js {:method "GET"` |
| 225 | 17 | `#js` | `#js {:method "POST"` |
| 227 | 48 | `#js` | `:body (.stringify js/JSON #js {:recipient_id user-id})})))))` |
| 360 | 28 | `#js` | `(-> (js/fetch source #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0"}})` |
| 360 | 42 | `#js` | `(-> (js/fetch source #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0"}})` |
| 413 | 24 | `#js` | `(js/Blob. #js [(:buffer file)] #js {:type (:mimeType file)})` |
| 413 | 45 | `#js` | `(js/Blob. #js [(:buffer file)] #js {:type (:mimeType file)})` |
| 421 | 55 | `clj->js` | `(.append form "payload_json" (.stringify js/JSON (clj->js payload)))` |
| 424 | 19 | `#js` | `#js {:method "POST"` |
| 425 | 33 | `#js` | `:headers #js {"Authorization" (str "Bot " token)}` |
| 463 | 11 | `clj->js` | `(clj->js (map-indexed (fn [idx url]` |
| 495 | 32 | `#js` | `#js {:method "PUT"` |
| 496 | 46 | `#js` | `:headers #js {"Authorization" (str "Bot " token)}})` |
| 517 | 15 | `clj->js` | `body (clj->js {:name name` |
| 523 | 28 | `#js` | `#js {:method "POST"` |
| 544 | 37 | `#js` | `#js {:method "GET"` |
| 564 | 37 | `#js` | `#js {:method "GET"` |
| 583 | 24 | `clj->js` | `(clj->js (mapv (fn [server]` |
| 606 | 34 | `#js` | `#js [])` |
| 610 | 30 | `clj->js` | `clj->js)]` |
| 626 | 28 | `clj->js` | `(clj->js ["Use discord.publish or discord.send to share results in a Discord channel."` |
| 665 | 38 | `clj->js` | `(clj->js ["Use this when you know the channel id and need exact message history."` |
| 692 | 36 | `clj->js` | `(clj->js ["Use discord.channel.scroll as sugar over discord.channel.messages before=oldest_seen_id."` |
| 719 | 33 | `clj->js` | `(clj->js ["Use this when the relevant conversation is in DMs rather than a guild channel."` |
| 756 | 28 | `clj->js` | `(clj->js ["Use scope=channel with channel_id for guild channels or scope=dm with user_id for DMs."` |
| 780 | 34 | `clj->js` | `(clj->js ["Use this before discord.list.channels when you need discovery."` |
| 788 | 28 | `clj->js` | `(clj->js ["Alias for discord.list.servers."` |
| 811 | 35 | `clj->js` | `(clj->js ["If guild_id is omitted, returns channels across all visible guilds."` |
| 822 | 26 | `#js` | `#js {:guild_id (aget params "guildId")}` |
| 828 | 30 | `clj->js` | `(clj->js ["Alias for discord.list.channels."` |
| 852 | 27 | `clj->js` | `(clj->js ["Use discord.react to add emoji reactions to messages."` |
| 879 | 35 | `clj->js` | `(clj->js ["Use discord.thread.create to start a thread from a message or in a channel."` |
| 899 | 25 | `#js` | `#js {:channel_id (or (aget params "channel_id") (aget params "channelId") "")` |
| 901 | 114 | `#js` | `:attachment_urls (or (aget params "attachment_urls") (aget params "attachmentUrls") #js [])}` |
| 907 | 29 | `clj->js` | `(clj->js ["Use discord.publish or discord.send to share results in a Discord channel."` |
| 921 | 29 | `#js` | `#js {:channel_id (or (aget params "channel_id") (aget params "channelId") "")` |
| 928 | 26 | `clj->js` | `(clj->js ["Use discord.read as a simple alias for discord.channel.messages."` |
| 943 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/discord/voice_tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 55 | 17 | `#js` | `headers #js {"Content-Type" "application/json"` |
| 71 | 19 | `#js` | `#js {:method "POST"` |
| 72 | 33 | `#js` | `:headers #js {"Authorization" (str "Bearer " api-key) "Content-Type" "application/json" "Accept" "audio/mpeg"}` |
| 73 | 51 | `clj->js` | `:body (.stringify js/JSON (clj->js body))})` |
| 106 | 9 | `#js` | `#js {:text merged-text` |
| 129 | 17 | `#js` | `#js {:method "POST"` |
| 130 | 31 | `#js` | `:headers #js {"Content-Type" "audio/wav"` |
| 157 | 19 | `#js` | `#js {:method "POST"` |
| 159 | 51 | `clj->js` | `:body (.stringify js/JSON (clj->js body))})` |
| 174 | 17 | `#js` | `#js {:method "POST"` |
| 176 | 49 | `clj->js` | `:body (.stringify js/JSON (clj->js body))})` |
| 196 | 56 | `clj->js` | `(js/JSON.stringify (clj->js (select-keys agent-spec [:contractId :contract-id :actorId :actor-id :role :model])))))` |
| 235 | 39 | `#js` | `(aset m "__voiceSessionContext" #js {:sessionId sid :conversationId cid}))` |
| 240 | 94 | `js->clj` | `(tool-text-result (str "Joined voice " ch " in guild " (aget r "guildId")) (js->clj r :keywordize-keys true)))))))` |
| 245 | 32 | `clj->js` | `(clj->js ["Use discord.voice.join to connect to a voice channel."` |
| 267 | 74 | `js->clj` | `(.then (fn [r] (tool-text-result (str "Left voice in guild " g) (js->clj r :keywordize-keys true)))))))` |
| 272 | 33 | `clj->js` | `(clj->js ["Use discord.voice.leave to disconnect from a voice channel."` |
| 309 | 31 | `clj->js` | `(clj->js ["Use discord.voice.say to speak in a voice channel."` |
| 329 | 95 | `#js` | `(.-length (or (aget agent-loop "audioWindows") #js [])))}))))` |
| 334 | 34 | `clj->js` | `(clj->js ["Use discord.voice.status to check if the bot is in a voice channel."` |
| 372 | 78 | `clj->js` | `"agent-context:" (when agent-context (js/JSON.stringify (clj->js agent-context)))` |
| 406 | 32 | `#js` | `(aset user-buf "texts" #js [])` |
| 435 | 19 | `#js` | `#js {:method "POST"` |
| 437 | 51 | `clj->js` | `:body (.stringify js/JSON (clj->js body))})` |
| 448 | 52 | `#js` | `(let [queue (or (aget loop-state "audioWindows") #js [])]` |
| 508 | 35 | `#js` | `(let [loop-state #js {:guildId g` |
| 515 | 55 | `#js` | `:audioWindows #js []` |
| 549 | 72 | `#js` | `(aset m "__voiceTranscriptionBuffer" #js {}))` |
| 551 | 63 | `#js` | `user-buf (or (aget buf-obj uid) #js {:texts #js [] :timer nil})]` |
| 551 | 75 | `#js` | `user-buf (or (aget buf-obj uid) #js {:texts #js [] :timer nil})]` |
| 577 | 37 | `#js` | `(aset m "__voiceSessionContext" #js {:sessionId sid :conversationId cid})` |
| 579 | 44 | `#js` | `(aset m "__voiceTranscriptionBuffer" #js {}))` |
| 594 | 39 | `#js` | `(aset m "__voiceSessionContext" #js {:sessionId sid :conversationId cid}))` |
| 602 | 72 | `clj->js` | `(voice-listen-execute runtime config _tool-call-id (clj->js {:guild_id guild-id` |
| 614 | 13 | `clj->js` | `(clj->js ["Use discord.voice.connect as the default voice entrypoint."` |
| 623 | 34 | `clj->js` | `(clj->js ["Use discord.voice.listen only when already connected via discord.voice.join."` |
| 667 | 55 | `clj->js` | `(clj->js {:guild_id guild-id` |
| 680 | 13 | `clj->js` | `(clj->js ["This is for contracts that explicitly grant :cap/voice-audio-event."` |
| 691 | 13 | `clj->js` | `(clj->js ["Use only after joining voice."` |
| 713 | 39 | `clj->js` | `(clj->js ["Use discord.voice.stop_listen to stop voice transcription."` |
| 734 | 28 | `js->clj` | `(let [ms (js->clj members :keywordize-keys true)` |
| 743 | 40 | `clj->js` | `(clj->js ["Use discord.voice.list_members to see who is in a voice channel."` |
| 756 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/event/dispatch.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 139 | 16 | `clj->js` | `(clj->js` |
| 148 | 34 | `js->clj` | `:results (js->clj results :keywordize-keys true)}))))))))` |
### `backend/src/cljs/knoxx/backend/domain/event/tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 35 | 17 | `#js` | `headers #js {"Content-Type" "application/json"` |
| 49 | 17 | `#js` | `#js {:method method` |
| 51 | 60 | `clj->js` | `:body (when body (.stringify js/JSON (clj->js body)))})` |
| 63 | 17 | `js->clj` | `(js->clj result :keywordize-keys true)))))` |
| 69 | 28 | `js->clj` | `(.then (fn [result] (js->clj result :keywordize-keys true)))))` |
| 156 | 33 | `js->clj` | `(let [result* (js->clj result :keywordize-keys true)]` |
| 261 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/extension_runtime.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 128 | 27 | `#js` | `(js/Promise.resolve #js {:handled false})` |
| 137 | 37 | `#js` | `(.then result (fn [r] #js {:handled true :result r}))` |
| 138 | 35 | `#js` | `(js/Promise.resolve #js {:handled true :result result})))` |
| 139 | 31 | `#js` | `(js/Promise.resolve #js {:handled false}))))))` |
| 159 | 3 | `#js` | `#js {:cwd (or (:workspace-root config) (.. js/process cwd))` |
| 161 | 17 | `#js` | `#js {:provider "proxx" :id model-id})` |
### `backend/src/cljs/knoxx/backend/domain/label/audio.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 18 | 23 | `js->clj` | `(js->clj :keywordize-keys true))))` |
| 25 | 34 | `clj->js` | `data (js/JSON.stringify (clj->js labels) nil 2)]` |
| 112 | 30 | `#js` | `(-> (.mkdir fs label-dir #js {:recursive true})` |
| 120 | 37 | `clj->js` | `(js/Promise.all (clj->js (map create-link files)))))))))` |
| 127 | 69 | `js->clj` | `(let [dir-paths (map (fn [d] (str audio-dir "/" d)) (js->clj dirs))` |
| 133 | 76 | `js->clj` | `(.then (fn [f] (count (js->clj f))))` |
| 137 | 39 | `clj->js` | `(-> (js/Promise.all (clj->js (map count-dir dir-paths)))` |
| 139 | 44 | `js->clj` | `(reduce + 0 (js->clj counts))))))))` |
| 157 | 43 | `#js` | `(-> (.mkdir fs audio-dir #js {:recursive true})` |
| 163 | 50 | `clj->js` | `(js/Promise.all (clj->js (map process-label all-labels))))))` |
### `backend/src/cljs/knoxx/backend/domain/mcp/mcp_bridge.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 89 | 20 | `clj->js` | `(clj->js {:jsonrpc "2.0"` |
| 100 | 15 | `clj->js` | `(clj->js {:method "POST"` |
| 123 | 21 | `clj->js` | `(clj->js {:protocolVersion PROTOCOL-VERSION` |
| 131 | 65 | `#js` | `(let [js-tools (or (aget tools-result "tools") #js [])` |
| 136 | 55 | `js->clj` | `:input-schema (js->clj (or (aget tool "inputSchema") {}) :keywordize-keys true)})))]` |
| 250 | 62 | `clj->js` | `"with args:" (subs (js/JSON.stringify (clj->js (or args {}))) 0 200))` |
| 254 | 27 | `clj->js` | `(clj->js {:name tool-name :arguments (or args {})}))` |
| 264 | 8 | `clj->js` | `(clj->js` |
| 272 | 47 | `js->clj` | `args (js->clj tool-args :keywordize-keys true)]` |
| 274 | 51 | `clj->js` | `(on-update (clj->js {:content [{:type "text"` |
| 278 | 51 | `clj->js` | `(clj->js {:content [{:type "text"` |
| 281 | 52 | `clj->js` | `(clj->js {:content [{:type "text"` |
| 292 | 32 | `clj->js` | `:parameters (clj->js (or input-schema {}))` |
### `backend/src/cljs/knoxx/backend/domain/mcp/mcp_expose.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 108 | 42 | `#js` | `(let [meta (or (aget tool "_meta") #js {})]` |
| 109 | 39 | `clj->js` | `(aset meta "knoxx/subAgents" (clj->js catalog))` |
| 110 | 41 | `clj->js` | `(aset meta "knoxx/subAgentIds" (clj->js ids))` |
| 112 | 38 | `clj->js` | `(aset tool "knoxxSubAgents" (clj->js catalog))` |
| 120 | 39 | `#js` | `(doseq [tool (array-seq (or tools #js []))]` |
| 142 | 27 | `js->clj` | `(let [ctx (when ctx-js (js->clj ctx-js :keywordize-keys true))` |
### `backend/src/cljs/knoxx/backend/domain/media/blaze.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 42 | 26 | `clj->js` | `(.stringify js/JSON (clj->js value))` |
| 227 | 21 | `js->clj` | `(let [parsed (js->clj (.parse js/JSON text) :keywordize-keys true)]` |
| 359 | 17 | `#js` | `headers #js {"Authorization" (str "Bearer " api-key)` |
| 367 | 31 | `#js` | `(p/let [res (js/fetch url #js {:method "POST"` |
| 370 | 63 | `clj->js` | `:body (.stringify js/JSON (clj->js body))})` |
| 384 | 26 | `js->clj` | `payload-clj (js->clj payload :keywordize-keys true)` |
| 486 | 29 | `#js` | `(p/let [res (js/fetch url #js {:headers #js {"Accept" "image/*,audio/*,video/*,*/*"}` |
| 486 | 43 | `#js` | `(p/let [res (js/fetch url #js {:headers #js {"Accept" "image/*,audio/*,video/*,*/*"}` |
| 522 | 52 | `#js` | `(.mkdir fs-promises (.dirname path absolute) #js {:recursive true})` |
| 532 | 34 | `clj->js` | `(let [raw (.stringify js/JSON (clj->js payload) nil 2)]` |
| 642 | 39 | `#js` | `(let [next-params (js/Object.assign #js {} params)]` |
| 775 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/media/multimodal.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 53 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/media/workspace.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 45 | 20 | `#js` | `#js {:content #js [#js {:type "text"` |
| 45 | 34 | `#js` | `#js {:content #js [#js {:type "text"` |
| 45 | 39 | `#js` | `#js {:content #js [#js {:type "text"` |
| 47 | 34 | `#js` | `:details #js {:path relative` |
| 49 | 55 | `clj->js` | `:content_parts (clj->js [part])}}))))))` |
| 70 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/media.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 268 | 27 | `#js` | `(-> (fs-mkdir! fs dir #js {:recursive true})` |
| 321 | 36 | `#js` | `(let [headers #js {"User-Agent" "Knoxx-Agent/1.0"}]` |
| 324 | 41 | `#js` | `(js/fetch source #js {:headers headers}))))` |
| 416 | 33 | `clj->js` | `args (clj->js ["-y"` |
| 421 | 58 | `#js` | `(-> (exec-file-async "ffmpeg" args #js {:timeout 120000 :maxBuffer 1048576})` |
### `backend/src/cljs/knoxx/backend/domain/music.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 29 | 41 | `#js` | `(js/Blob. #js [(:buffer media)] #js {:type (:mime-type media)})` |
| 29 | 63 | `#js` | `(js/Blob. #js [(:buffer media)] #js {:type (:mime-type media)})` |
| 31 | 59 | `#js` | `(-> (js/fetch "https://api.audd.io/" #js {:method "POST" :body form})` |
| 44 | 60 | `js->clj` | `:result (when result (js->clj result :keywordize-keys true))})))))))))))` |
| 66 | 32 | `js->clj` | `:result (js->clj result :keywordize-keys true)}))))))))` |
| 79 | 32 | `#js` | `(js/fetch url #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0 (discord bot)"}})))` |
| 79 | 46 | `#js` | `(js/fetch url #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0 (discord bot)"}})))` |
| 87 | 28 | `js->clj` | `:result (js->clj result :keywordize-keys true)})))))` |
| 117 | 97 | `#js` | `(-> (media/fs-mkdir! fs (media/path-resolve path absolute "..") #js {:recursive true})` |
| 119 | 74 | `#js` | `(-> (exec-file-async "node" #js [script-path spec-path absolute]` |
| 120 | 66 | `#js` | `#js {:timeout 120000 :maxBuffer 1048576})` |
| 122 | 73 | `js->clj` | `(let [result (js->clj (.parse js/JSON stdout) :keywordize-keys true)]` |
| 151 | 46 | `clj->js` | `(clj->js value)))` |
| 233 | 23 | `js->clj` | `(js->clj (.parse js/JSON audio-data-json) :keywordize-keys true)` |
| 366 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/node/fs.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 48 | 22 | `#js` | `(.mkdir fs (str p) #js {:recursive true}))` |
| 85 | 32 | `#js` | `(-> (.readdir fs (str root) #js {:withFileTypes true :recursive true})` |
| 99 | 11 | `#js` | `#js {:recursive true}` |
### `backend/src/cljs/knoxx/backend/domain/node/path.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 14 | 41 | `clj->js` | `(.apply (.-join node-path) node-path (clj->js (keep #(when % (str %)) parts))))` |
| 19 | 44 | `clj->js` | `(.apply (.-resolve node-path) node-path (clj->js (keep #(when % (str %)) parts))))` |
### `backend/src/cljs/knoxx/backend/domain/nrepl.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 52 | 27 | `clj->js` | `(.concat js/Buffer (clj->js [(.from js/Buffer (str (.-length b) ":") "utf8") b])))` |
| 59 | 27 | `clj->js` | `(.concat js/Buffer (clj->js (into [(.from js/Buffer "l" "utf8")] (concat items [(.from js/Buffer "e" "utf8")])))))` |
| 64 | 22 | `js->clj` | `:else (js->clj v))` |
| 70 | 17 | `clj->js` | `(clj->js (into [(.from js/Buffer "d" "utf8")] (concat encoded [(.from js/Buffer "e" "utf8")])))))` |
| 145 | 42 | `#js` | `(let [socket (.createConnection net #js {:host (nrepl-host)` |
| 166 | 52 | `clj->js` | `(let [buf (.concat js/Buffer (clj->js [(:buf @state) chunk]))` |
| 344 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/openutau/tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 35 | 41 | `#js` | `(js-await [result (exec-file script #js [ustx-path output-wav-path]` |
| 36 | 34 | `#js` | `#js {:timeout 600000 :maxBuffer 4194304})]` |
### `backend/src/cljs/knoxx/backend/domain/policy/edn_adapter.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 68 | 32 | `#js` | `(->> (.readdirSync fs root #js {:withFileTypes true :recursive true})` |
| 129 | 48 | `#js` | `(.mkdirSync fs (.dirname path file-path) #js {:recursive true})` |
### `backend/src/cljs/knoxx/backend/domain/policy/sql_adapter.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 176 | 19 | `js->clj` | `:secretJson (js->clj (or (aget row "secret_json") {}) :keywordize-keys true)` |
| 230 | 50 | `#js` | `:role-ids #js []` |
| 310 | 73 | `clj->js` | `:secret-json (js/JSON.stringify (clj->js (or (:secretJson credential) {})))` |
| 316 | 26 | `clj->js` | `(-> (js/Promise.all (clj->js (mapv #(project-actor! store %) actors)))` |
### `backend/src/cljs/knoxx/backend/domain/realtime.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 23 | 40 | `clj->js` | `(.send socket (.stringify js/JSON (clj->js payload)))))` |
| 26 | 3 | `#js` | `#js ["--query-gpu=index,name,utilization.gpu,utilization.memory,memory.used,memory.total,temperature.gpu,power.draw"` |
| 55 | 59 | `#js` | `(-> (exec-file-async "nvidia-smi" nvidia-smi-query-args #js {:timeout 1200})` |
| 159 | 11 | `#js` | `#js {:method "GET"` |
| 164 | 38 | `#js` | `(.send #js {:error "WebSocket upgrade required"})))` |
| 177 | 66 | `#js` | `(swap! ws-clients* assoc client-id #js {:socket ws :sessionId session-id :conversationId conversation-id})` |
| 186 | 100 | `#js` | `(fn [c] (when c (js/Object.assign #js {} c #js {:conversationId new-cid})))))))` |
| 186 | 109 | `#js` | `(fn [c] (when c (js/Object.assign #js {} c #js {:conversationId new-cid})))))))` |
### `backend/src/cljs/knoxx/backend/domain/sandbox_container.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 133 | 29 | `clj->js` | `(-> (exec-file-async bin (clj->js args) (clj->js (merge {:maxBuffer sandbox-max-buffer-bytes} opts)))` |
| 133 | 44 | `clj->js` | `(-> (exec-file-async bin (clj->js args) (clj->js (merge {:maxBuffer sandbox-max-buffer-bytes} opts)))` |
| 155 | 19 | `js->clj` | `(js->clj (.parse js/JSON (str text)) :keywordize-keys true)))` |
| 161 | 63 | `clj->js` | `(-> (fs-write-file! fs metadata-path (.stringify js/JSON (clj->js metadata) nil 2) "utf8")` |
| 215 | 34 | `js->clj` | `labels (js->clj (or (aget item "Config" "Labels") #js {}) :keywordize-keys false)` |
| 215 | 76 | `#js` | `labels (js->clj (or (aget item "Config" "Labels") #js {}) :keywordize-keys false)` |
| 216 | 33 | `js->clj` | `state (js->clj (or (aget item "State") #js {}) :keywordize-keys true)` |
| 216 | 65 | `#js` | `state (js->clj (or (aget item "State") #js {}) :keywordize-keys true)` |
| 217 | 34 | `js->clj` | `mounts (js->clj (or (aget item "Mounts") #js []) :keywordize-keys true)` |
| 217 | 67 | `#js` | `mounts (js->clj (or (aget item "Mounts") #js []) :keywordize-keys true)` |
| 242 | 35 | `#js` | `(.rm fs host-dir #js {:recursive true :force true})))` |
| 244 | 36 | `#js` | `(.rm fs host-dir #js {:recursive true :force true})))` |
| 283 | 41 | `#js` | `(fs-mkdir! fs host-dir #js {:recursive true})))` |
| 509 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/session_mycology.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 73 | 58 | `#js` | `(-> (fs-promises/mkdir (node-path.dirname file-path) #js {:recursive true})` |
| 85 | 25 | `#js` | `(.catch (fn [_] #js [])))))` |
| 159 | 27 | `#js` | `(js/Promise.resolve #js {:promoted false :eligible false})` |
| 165 | 36 | `#js` | `(-> (fs-promises/mkdir dir #js {:recursive true})` |
| 179 | 38 | `#js` | `#js {:ts (now-iso) :slug (aget spore "slug") :name (aget spore "name")` |
| 185 | 36 | `#js` | `#js {:promoted (or created-skill created-contract) :eligible true` |
| 213 | 60 | `#js` | `(-> (fs-promises/mkdir (node-path.dirname file-path) #js {:recursive true})` |
| 239 | 42 | `#js` | `#js {:spores spores}))))` |
| 245 | 28 | `#js` | `(let [reflection #js {:ts (now-iso) :cwd cwd :sessionFile session-file :model model-label` |
| 266 | 32 | `#js` | `#js {:reflection reflection}))` |
| 272 | 57 | `#js` | `spore #js {:ts (now-iso) :name name :slug slug :description description` |
| 298 | 61 | `#js` | `#js {:reflection reflection` |
| 328 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/text.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 91 | 3 | `#js` | `#js {:content #js [#js {:type "text" :text text}]` |
| 91 | 17 | `#js` | `#js {:content #js [#js {:type "text" :text text}]` |
| 91 | 22 | `#js` | `#js {:content #js [#js {:type "text" :text text}]` |
| 92 | 18 | `clj->js` | `:details (clj->js details)})` |
| 256 | 23 | `js->clj` | `(let [parsed (js->clj (.parse js/JSON trimmed) :keywordize-keys true)` |
| 436 | 35 | `js->clj` | `(let [clj-value (js->clj value :keywordize-keys true)]` |
### `backend/src/cljs/knoxx/backend/domain/tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 13 | 4 | `clj->js` | `(clj->js (mjs/transform schema)))` |
| 16 | 3 | `#js` | `#js {:name name` |
| 20 | 27 | `clj->js` | `:promptGuidelines (clj->js prompt-guidelines)` |
| 27 | 8 | `#js` | `(f #js {:content #js [#js {:type "text" :text text}]})))` |
| 27 | 22 | `#js` | `(f #js {:content #js [#js {:type "text" :text text}]})))` |
| 27 | 27 | `#js` | `(f #js {:content #js [#js {:type "text" :text text}]})))` |
| 41 | 6 | `clj->js` | `(clj->js` |
| 91 | 6 | `js->clj` | `(js->clj (.parse js/JSON text) :keywordize-keys true)` |
### `backend/src/cljs/knoxx/backend/domain/twitch.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 171 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/domain/voice/tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 112 | 29 | `#js` | `(p/let [res (js/fetch url #js {:method "POST"` |
| 113 | 42 | `#js` | `:headers #js {"Authorization" (str "Bearer " api-key)` |
| 116 | 63 | `clj->js` | `:body (.stringify js/JSON (clj->js body))})` |
| 125 | 55 | `#js` | `(.mkdir fs-promises (.dirname node-path absolute) #js {:recursive true})` |
| 254 | 49 | `js->clj` | `notes (openutau/normalize-notes (js->clj (or (aget params "notes") #js []) :keywordize-keys true))` |
| 254 | 83 | `#js` | `notes (openutau/normalize-notes (js->clj (or (aget params "notes") #js []) :keywordize-keys true))` |
| 257 | 64 | `js->clj` | `:time_signature (js->clj (or (aget params "time_signature") #js {}) :keywordize-keys true)` |
| 257 | 107 | `#js` | `:time_signature (js->clj (or (aget params "time_signature") #js {}) :keywordize-keys true)` |
| 274 | 38 | `#js` | `(.mkdir fs-promises output-dir #js {:recursive true})` |
| 380 | 7 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/infra/agent/hydration.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 174 | 8 | `clj->js` | `(clj->js (into base-parts` |
| 176 | 67 | `js->clj` | `(let [p (if (map? part) part (js->clj part :keywordize-keys true))` |
| 196 | 8 | `clj->js` | `(clj->js base-parts))))` |
### `backend/src/cljs/knoxx/backend/infra/agent/policy.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 14 | 65 | `js->clj` | `(and constraints (= "object" (goog/typeOf constraints))) (js->clj constraints :keywordize-keys true)` |
### `backend/src/cljs/knoxx/backend/infra/agent/provider.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 26 | 25 | `#js` | `(-> (js/fetch url #js {:headers #js {"Authorization" (str "Bearer " token)` |
| 26 | 39 | `#js` | `(-> (js/fetch url #js {:headers #js {"Authorization" (str "Bearer " token)` |
| 33 | 72 | `#js` | `(let [items (js-array-seq (or (aget payload "data") #js []))` |
### `backend/src/cljs/knoxx/backend/infra/agent/recovery.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 109 | 45 | `#js` | `#js {:sessionId session-id` |
| 167 | 49 | `#js` | `#js {:sessionId session-id` |
| 192 | 42 | `clj->js` | `(-> (.all js/Promise (clj->js (mapv #(resume-recovered-session! runtime config %) items)))` |
| 194 | 39 | `js->clj` | `(vec (js->clj results :keywordize-keys true)))))` |
### `backend/src/cljs/knoxx/backend/infra/agent/resume.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 143 | 39 | `#js` | `#js {:sessionId session-id` |
| 149 | 39 | `#js` | `#js {:sessionId session-id` |
| 183 | 29 | `clj->js` | `(-> (.all js/Promise (clj->js (repeatedly worker-count worker)))` |
| 202 | 15 | `#js` | `#js [(-> (.all js/Promise (clj->js (mapv abort-stale-session! stale)))` |
| 202 | 42 | `clj->js` | `#js [(-> (.all js/Promise (clj->js (mapv abort-stale-session! stale)))` |
| 298 | 30 | `#js` | `#js [(-> (.all js/Promise (clj->js (mapv abort-stale-session! stale)))` |
| 298 | 57 | `clj->js` | `#js [(-> (.all js/Promise (clj->js (mapv abort-stale-session! stale)))` |
| 302 | 57 | `clj->js` | `(-> (.all js/Promise (clj->js (mapv #(agent-recovery/resume-recovered-session!` |
| 349 | 18 | `clj->js` | `(clj->js` |
| 380 | 51 | `#js` | `(js/setTimeout #(resolve #js {:timed_out false :remaining 0})` |
| 384 | 31 | `#js` | `(resolve #js {:timed_out true :remaining remaining})` |
### `backend/src/cljs/knoxx/backend/infra/agent/stream.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 288 | 30 | `js->clj` | `(js->clj raw-args :keywordize-keys true)` |
| 378 | 31 | `js->clj` | `(js->clj raw-result :keywordize-keys true)` |
| 412 | 53 | `#js` | `(let [tool-results (or (aget event "toolResults") #js [])` |
### `backend/src/cljs/knoxx/backend/infra/agent/tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 22 | 19 | `js->clj` | `(js->clj raw-args :keywordize-keys true)` |
### `backend/src/cljs/knoxx/backend/infra/agent/transcript.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 41 | 33 | `js->clj` | `(js->clj raw-usage :keywordize-keys true))` |
### `backend/src/cljs/knoxx/backend/infra/auth/authz.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 43 | 53 | `js->clj` | `(http/json-response! reply status (js->clj result :keywordize-keys true))` |
| 55 | 50 | `#js` | `(let [headers (or (aget request "headers") #js {})` |
| 65 | 37 | `js->clj` | `(let [clj-ctx (js->clj ctx :keywordize-keys true)]` |
### `backend/src/cljs/knoxx/backend/infra/auth/session.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 25 | 34 | `clj->js` | `data (js/JSON.stringify (clj->js payload))` |
| 106 | 20 | `clj->js` | `payload (clj->js {:token token` |
| 126 | 23 | `clj->js` | `(clj->js {:id (aget s "id")` |
| 147 | 52 | `clj->js` | `client (.createClient redis (clj->js {:url url}))]` |
| 175 | 54 | `clj->js` | `(js/JSON.stringify (clj->js data))` |
| 176 | 35 | `clj->js` | `(clj->js {:EX ttl})))))))))))` |
| 206 | 18 | `clj->js` | `(clj->js {:method "POST"` |
| 210 | 36 | `clj->js` | `(clj->js {:client_id client-id` |
| 227 | 22 | `clj->js` | `(-> (js/fetch url (clj->js {:headers {:Authorization (str "Bearer " access-token)` |
| 264 | 18 | `clj->js` | `(clj->js {:path "/"` |
| 274 | 20 | `clj->js` | `(clj->js {:path "/"` |
| 334 | 8 | `clj->js` | `(clj->js ["system_admin"])` |
| 337 | 8 | `clj->js` | `(clj->js (if (seq allowlist-role-slugs)` |
| 342 | 8 | `clj->js` | `(clj->js ["knowledge_worker"]))))` |
| 373 | 33 | `clj->js` | `(clj->js {:orgId org-id` |
| 376 | 54 | `clj->js` | `(.resolveRequestContext policyDb (clj->js {"x-knoxx-membership-id" membership-id})))))` |
| 383 | 25 | `clj->js` | `(clj->js {"x-knoxx-user-email" normalized-email}))` |
| 388 | 45 | `clj->js` | `(sync-user-from-actor-contract (clj->js {:email normalized-email` |
| 461 | 34 | `#js` | `(let [headers #js {"x-knoxx-user-email" (aget session-data "email")` |
| 474 | 39 | `#js` | `raw-token (sign-token #js {:sid session-id})` |
| 475 | 30 | `#js` | `session-data #js {:membershipId (some-> fresh-ctx (aget "membership") (aget "id"))` |
| 500 | 31 | `#js` | `raw-token (sign-token #js {:sid session-id})` |
| 501 | 22 | `#js` | `session-data #js {:membershipId (some-> ctx (aget "membership") (aget "id"))` |
| 520 | 18 | `#js` | `#js {:ok true` |
| 608 | 45 | `#js` | `#js {:host smtp-host` |
| 611 | 56 | `#js` | `:auth #js {:user smtp-user` |
| 617 | 22 | `#js` | `#js {:from from` |
### `backend/src/cljs/knoxx/backend/infra/core.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 41 | 16 | `#js` | `(.listen app #js {:host host :port port}))` |
| 50 | 4 | `clj->js` | `(clj->js (runtime-models/enrich-config (runtime-config/cfg))))` |
| 141 | 26 | `clj->js` | `(js/Promise.resolve (clj->js resolved-config))))` |
| 147 | 24 | `#js` | `app (Fastify #js {:logger #js {:stream (.-stderr js/process)}})]` |
| 147 | 37 | `#js` | `app (Fastify #js {:logger #js {:stream (.-stderr js/process)}})]` |
| 153 | 52 | `#js` | `(.then (fn [] (.register app fastifyCors #js {:origin true})))` |
### `backend/src/cljs/knoxx/backend/infra/core_memory.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 15 | 24 | `js->clj` | `(js->clj (.parse js/JSON value) :keywordize-keys true)` |
| 250 | 16 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/infra/db/policy.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 226 | 48 | `clj->js` | `(js/JSON.stringify (clj->js (:constraints p))))))))))` |
| 308 | 48 | `clj->js` | `(js/JSON.stringify (clj->js (:constraints p))))))))))` |
| 348 | 40 | `js->clj` | `:constraints (js->clj (or constraints_json (js-obj)) :keywordize-keys true)}))` |
| 385 | 40 | `js->clj` | `:constraints (js->clj (or constraints_json (js-obj)) :keywordize-keys true)}))` |
| 630 | 59 | `clj->js` | `:before-json (when before (js/JSON.stringify (clj->js before)))` |
| 631 | 59 | `clj->js` | `:after-json (when after (js/JSON.stringify (clj->js after)))))))` |
| 938 | 53 | `js->clj` | `:config (js->clj (or config_json (js-obj)) :keywordize-keys true)` |
| 950 | 43 | `clj->js` | `config-json (js/JSON.stringify (clj->js (or config {})))]` |
| 1052 | 75 | `clj->js` | `:role-slugs-json (js/JSON.stringify (clj->js slugs))` |
| 1108 | 76 | `js->clj` | `(string? role_slugs) (js->clj (js/JSON.parse role_slugs))` |
| 1109 | 76 | `js->clj` | `:else (js->clj role_slugs))` |
| 1137 | 23 | `js->clj` | `(js->clj options :keywordize-keys true))` |
### `backend/src/cljs/knoxx/backend/infra/document_state.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 100 | 23 | `#js` | `(.mkdir fs dir-path #js {:recursive true}))` |
| 167 | 44 | `#js` | `(let [read-promise (.readdir fs dir-path #js {:withFileTypes true})]` |
| 171 | 27 | `clj->js` | `(clj->js` |
| 176 | 53 | `#js` | `(js/Promise.resolve #js [full-path]))))))` |
| 215 | 25 | `clj->js` | `(clj->js (map #(document-entry! runtime config profile db-id %) paths)))` |
| 314 | 27 | `clj->js` | `(clj->js` |
| 427 | 11 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/infra/eta_mu_session_ingester.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 33 | 31 | `#js` | `(.mkdir fs INGEST-STATE-DIR #js {:recursive true}))` |
| 39 | 23 | `#js` | `(.catch (fn [_] #js {:sessions (js/Object.create nil)}))))` |
| 73 | 44 | `#js` | `#js {:dir dir` |
| 80 | 38 | `#js` | `(.catch (fn [_] #js [])))))` |
| 88 | 25 | `#js` | `(.catch (fn [_] #js [])))))` |
| 134 | 13 | `#js` | `(let [evt #js {:schema "openplanner.event.v1"` |
| 139 | 30 | `#js` | `:source_ref #js {:project ETA-MU-SESSION-PROJECT :session session-id}` |
| 149 | 5 | `#js` | `#js [(-> (make-event session-id "session_start" (.-timestamp eta-mu-event)` |
| 151 | 26 | `#js` | `#js {:role "system" :author "eta-mu"` |
| 154 | 26 | `#js` | `#js {:id id` |
| 163 | 3 | `#js` | `#js [(make-event session-id "model_change" (obj-get eta-mu-event "timestamp")` |
| 165 | 20 | `#js` | `#js {:role "system" :author "eta-mu"}` |
| 166 | 20 | `#js` | `#js {:id (obj-get eta-mu-event "id")` |
| 175 | 7 | `#js` | `#js []` |
| 176 | 7 | `#js` | `#js [(make-event session-id "compaction" (.-timestamp eta-mu-event)` |
| 178 | 24 | `#js` | `#js {:role "system" :author "eta-mu"}` |
| 179 | 24 | `#js` | `#js {:id (.-id eta-mu-event)` |
| 188 | 7 | `#js` | `#js []` |
| 189 | 7 | `#js` | `#js [(make-event session-id (str "custom." (or (obj-get eta-mu-event "customType") "unknown"))` |
| 192 | 24 | `#js` | `#js {:role "system" :author "eta-mu"}` |
| 193 | 24 | `#js` | `#js {:id (obj-get eta-mu-event "id")` |
| 202 | 7 | `#js` | `#js []` |
| 203 | 7 | `#js` | `#js [(make-event session-id "message" (obj-get eta-mu-event "timestamp")` |
| 205 | 24 | `#js` | `#js {:role "user" :author "user"}` |
| 206 | 24 | `#js` | `#js {:id (obj-get eta-mu-event "id")` |
| 218 | 16 | `#js` | `events #js []]` |
| 223 | 33 | `#js` | `#js {:role "assistant" :author "eta-mu" :model model}` |
| 224 | 33 | `#js` | `#js {:id (obj-get eta-mu-event "id")` |
| 236 | 33 | `#js` | `#js {:role "system" :author "eta-mu" :model model}` |
| 237 | 33 | `#js` | `#js {:id (str (obj-get eta-mu-event "id") "-thinking")` |
| 246 | 35 | `#js` | `#js {:role "system" :author "eta-mu" :model model}` |
| 247 | 35 | `#js` | `#js {:id (or (obj-get tc "id") (obj-get eta-mu-event "id"))` |
| 262 | 7 | `#js` | `#js []` |
| 271 | 7 | `#js` | `#js []` |
| 280 | 54 | `#js` | `(let [msg (or (obj-get eta-mu-event "message") #js {})` |
| 289 | 17 | `#js` | `:else #js []))` |
| 291 | 13 | `#js` | `:else #js [])))` |
| 300 | 23 | `#js` | `events #js []` |
| 301 | 29 | `#js` | `session-meta #js {:sessionId "unknown" :cwd "/unknown"}]` |
| 312 | 12 | `#js` | `#js {:events events :sessionMeta session-meta})))))` |
| 325 | 31 | `#js` | `#js []` |
| 328 | 14 | `#js` | `#js {:sessionId (.-sessionId session-meta) :eventsIngested 0 :batches 0}` |
| 330 | 29 | `#js` | `(let [promises #js []` |
| 337 | 77 | `#js` | `(-> (openplanner-request-fn "POST" "/v1/events" #js {:events batch})` |
| 349 | 29 | `#js` | `#js {:sessionId (.-sessionId session-meta)` |
| 361 | 31 | `#js` | `(js/Promise.resolve #js {:sessions (js/Object.create nil)})` |
| 382 | 23 | `#js` | `#js {:ok true` |
| 388 | 48 | `#js` | `(let [results-atom (atom #js [])]` |
| 398 | 52 | `#js` | `#js {:mtime (.-mtime file)` |
| 410 | 53 | `#js` | `#js {:sessionId (.-sessionId file)` |
| 430 | 41 | `#js` | `#js {:ok true` |
| 440 | 12 | `#js` | `#js {:ok false :error (.-message err)})))` |
| 470 | 36 | `#js` | `#js {:sessionId id` |
| 474 | 12 | `#js` | `#js {:ok true` |
| 482 | 34 | `clj->js` | `:recentIngested (clj->js recent)})))))` |
| 506 | 31 | `#js` | `#js {:sessionId (.-sessionId f) :workspace (.-dir f)` |
| 513 | 35 | `#js` | `#js {:sessionId (or (.-id header) (.-sessionId f))` |
| 522 | 35 | `#js` | `#js {:sessionId (.-sessionId f) :workspace (.-dir f)` |
| 527 | 27 | `#js` | `#js {:sessionId (.-sessionId f) :workspace (.-dir f)` |
| 534 | 21 | `#js` | `#js {:ok true` |
### `backend/src/cljs/knoxx/backend/infra/graceful_shutdown.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 68 | 41 | `#js` | `(.all js/Promise #js [(close-server!)` |
| 77 | 43 | `#js` | `#js {:count count}))))` |
| 78 | 48 | `#js` | `(js/Promise.resolve #js {:count 0})))))` |
| 81 | 32 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/infra/http.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 20 | 17 | `clj->js` | `(.send (clj->js body)))))` |
| 95 | 39 | `#js` | `js-opts (js/Object.assign #js {:signal (.-signal controller)}` |
| 96 | 56 | `clj->js` | `(if (map? opts) (clj->js opts) (or opts #js {})))]` |
| 96 | 79 | `#js` | `(if (map? opts) (clj->js opts) (or opts #js {})))]` |
| 115 | 46 | `js->clj` | `(js->clj (.parse js/JSON text) :keywordize-keys true)` |
| 149 | 58 | `clj->js` | `body (assoc :body (.stringify js/JSON (clj->js body))))]` |
| 213 | 42 | `#js` | `query (or (aget request "query") #js {})]` |
| 227 | 45 | `#js` | `source (or (aget request "headers") #js {})]` |
| 258 | 42 | `#js` | `(contains? #{"GET" "HEAD"} method) #js {}` |
| 259 | 20 | `#js` | `(some? body) #js {:body body}` |
| 260 | 58 | `#js` | `(str/includes? content-type "multipart/form-data") #js {:body (aget request "raw")` |
| 262 | 13 | `#js` | `:else #js {})))` |
| 267 | 14 | `#js` | `base #js {:method method` |
| 270 | 63 | `clj->js` | `(js/fetch target-url (.assign js/Object base stream-opts (clj->js extra)))))` |
### `backend/src/cljs/knoxx/backend/infra/http_server.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 12 | 12 | `#js` | `(Fastify #js {:logger true` |
| 28 | 26 | `#js` | `#js {:parseAs "string"}` |
| 31 | 56 | `#js` | `(done nil (if (= body "") #js {} (js/JSON.parse body)))` |
| 41 | 34 | `#js` | `(-> (.register app fastifyCors #js {:origin true})` |
| 45 | 33 | `#js` | `#js {:limits #js {:fileSize (* 50 1024 1024)` |
| 45 | 46 | `#js` | `#js {:limits #js {:fileSize (* 50 1024 1024)` |
| 52 | 16 | `#js` | `(.listen app #js {:host host :port port}))` |
### `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 100 | 39 | `clj->js` | `(clj->js` |
| 143 | 24 | `#js` | `(let [text-block #js {:type "text" :text text}` |
| 145 | 28 | `clj->js` | `(clj->js (into [text-block]` |
| 148 | 52 | `#js` | `#js {:type "image_url"` |
| 149 | 68 | `#js` | `:image_url #js {:url (or (:url p)` |
| 151 | 52 | `#js` | `#js {:type "text" :text (or (:text p) "")}))` |
| 153 | 27 | `#js` | `#js [text-block])]` |
| 154 | 9 | `#js` | `#js {:role role` |
| 263 | 46 | `clj->js` | `(let [results (await (.all js/Promise (clj->js (map #(fetch-session-summary! config %) session-ids))))]` |
| 264 | 15 | `js->clj` | `(->> (js->clj results :keywordize-keys true)` |
| 291 | 57 | `clj->js` | `(js/JSON.parse (js/JSON.stringify (clj->js (str/split node-type #",")))))` |
| 293 | 52 | `clj->js` | `(js/JSON.parse (js/JSON.stringify (clj->js (str/split lake #",")))))]` |
### `backend/src/cljs/knoxx/backend/infra/openplanner/semantic.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 52 | 5 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/infra/openplanner/tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 72 | 21 | `#js` | `(-> (js/fetch url #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0"}})` |
| 72 | 35 | `#js` | `(-> (js/fetch url #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0"}})` |
| 129 | 50 | `clj->js` | `(aset "rows" (clj->js rows)))]` |
| 155 | 62 | `clj->js` | `:body (.stringify js/JSON (clj->js {:query query` |
| 191 | 38 | `#js` | `(-> (media/fs-mkdir! fs parent #js {:recursive true})` |
| 201 | 49 | `#js` | `evidence (or (aget params "evidence") #js [])` |
| 211 | 76 | `clj->js` | `:extra {:claim claim :evidence (clj->js evidence) :p p :src source}})]` |
| 253 | 25 | `#js` | `(-> (js/fetch url #js {:method "POST"` |
| 254 | 39 | `#js` | `:headers #js {"Content-Type" "application/json"` |
| 256 | 57 | `clj->js` | `:body (.stringify js/JSON (clj->js segment))})` |
| 355 | 5 | `clj->js` | `(clj->js` |
### `backend/src/cljs/knoxx/backend/infra/redis_client.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 24 | 25 | `clj->js` | `(js/JSON.stringify (clj->js value))` |
| 43 | 41 | `#js` | `(let [client (.createClient redis #js {:url redis-url})]` |
| 109 | 37 | `#js` | `(.set client key' value' #js {:EX ttl})` |
| 120 | 50 | `clj->js` | `(.set (redis-arg key) (js/JSON.stringify (clj->js value)))` |
| 134 | 19 | `js->clj` | `(js->clj (js/JSON.parse value) :keywordize-keys true))))` |
| 169 | 17 | `js->clj` | `(js->clj members)))` |
| 194 | 51 | `clj->js` | `(.lPush (redis-arg key) (js/JSON.stringify (clj->js value)))` |
| 221 | 34 | `js->clj` | `(js->clj (js/JSON.parse item) :keywordize-keys true)` |
### `backend/src/cljs/knoxx/backend/infra/routes/actors.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 18 | 4 | `js->clj` | `(js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true))` |
| 62 | 10 | `clj->js` | `(clj->js` |
| 134 | 73 | `js->clj` | `:dispatches (js->clj dispatch-results :keywordize-keys true))))))` |
### `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 69 | 74 | `clj->js` | `(policy-db-promise runtime reply 200 (.listRoles db (clj->js {:orgId org-id}))))))` |
| 77 | 67 | `clj->js` | `payload (.assign js/Object (js/Object.) body (clj->js {:orgId org-id}))]` |
| 93 | 65 | `js->clj` | `(let [role (js->clj (aget result "role") :keywordize-keys true)]` |
| 107 | 78 | `clj->js` | `(policy-db-promise runtime reply 200 (.listDataLakes db (clj->js {:orgId org-id}))))))` |
| 115 | 67 | `clj->js` | `payload (.assign js/Object (js/Object.) body (clj->js {:orgId org-id}))]` |
### `backend/src/cljs/knoxx/backend/infra/routes/app.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 230 | 36 | `clj->js` | `(-> (.all js/Promise (clj->js (mapv #(session-store/get-session redis-client %) (vec ids))))` |
| 234 | 52 | `js->clj` | `session-items (->> (vec (js->clj sessions-js :keywordize-keys true))` |
| 330 | 15 | `js->clj` | `(let [rows (js->clj (aget result "rows") :keywordize-keys true)]` |
| 334 | 15 | `js->clj` | `(let [rows (js->clj (aget result "rows") :keywordize-keys true)]` |
| 379 | 12 | `js->clj` | `(let [r (js->clj results :keywordize-keys true)]` |
| 409 | 12 | `js->clj` | `(let [r (js->clj results :keywordize-keys true)]` |
| 463 | 67 | `clj->js` | `(js/JSON.stringify (clj->js (:body resp)))))}))` |
| 902 | 33 | `clj->js` | `(-> (.mkdir fs dir (clj->js {:recursive true}))` |
| 1117 | 48 | `clj->js` | `:body (js/JSON.stringify (clj->js {:k k :minSimilarity min-sim}))})` |
| 1144 | 38 | `clj->js` | `(-> (query-fn final-sql (clj->js []))` |
| 1156 | 32 | `clj->js` | `(-> (query-fn sql-str (clj->js []))` |
| 1437 | 61 | `js->clj` | `:items (or (some-> (aget body "items") js->clj) [])}]` |
| 1441 | 52 | `clj->js` | `:body (js/JSON.stringify (clj->js payload))})` |
### `backend/src/cljs/knoxx/backend/infra/routes/auth.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 23 | 27 | `clj->js` | `(.send reply (clj->js {:githubEnabled github-enabled` |
| 30 | 42 | `clj->js` | `(.send (.code reply 503) (clj->js {:error "Knoxx policy database is not configured"}))` |
| 37 | 46 | `clj->js` | `(.send (.code reply 400) (clj->js {:error "email is required"}))` |
| 44 | 53 | `clj->js` | `(clj->js {:email email` |
| 54 | 73 | `clj->js` | `(clj->js {"x-knoxx-user-email" email` |
| 58 | 92 | `clj->js` | `(clj->js {:email email` |
| 65 | 42 | `clj->js` | `(clj->js {:error (or (.-message err) "Signup failed")})))))))))))` |
| 70 | 41 | `clj->js` | `(.send (.code reply 503) (clj->js {:error "GitHub OAuth not configured"}))` |
| 84 | 41 | `clj->js` | `(.send (.code reply 503) (clj->js {:error "GitHub OAuth not configured"}))` |
| 88 | 45 | `clj->js` | `(.send (.code reply 400) (clj->js {:error "Missing code or state"}))` |
| 91 | 47 | `clj->js` | `(.send (.code reply 400) (clj->js {:error "Invalid or expired state parameter"}))))))))` |
| 101 | 30 | `clj->js` | `(.send reply (clj->js {:ok true})))))` |
| 107 | 44 | `clj->js` | `(.send (.code reply 400) (clj->js {:error "Invite code is required"}))` |
| 116 | 48 | `clj->js` | `(.send (.code reply 401) (clj->js {:error "Not authenticated"}))` |
| 119 | 49 | `clj->js` | `(.send reply (clj->js {:ok true` |
| 124 | 44 | `clj->js` | `(clj->js {:error (or (.-message err) "Invite redemption failed")})))))))))))` |
| 133 | 95 | `clj->js` | `role-slugs (or (some-> req (aget "body") (aget "roleSlugs")) (clj->js ["knowledge_worker"]))]` |
| 135 | 57 | `clj->js` | `(.send (.code reply 400) (clj->js {:error "email is required"}))` |
| 137 | 51 | `clj->js` | `(clj->js {:orgId org-id` |
| 146 | 58 | `clj->js` | `(.send reply (clj->js {:ok true` |
| 150 | 53 | `clj->js` | `(clj->js {:error (or (.-message err) "Invite creation failed")}))))))))` |
| 153 | 36 | `clj->js` | `(clj->js {:error (or (.-message err) "Unauthorized")})))))))` |
| 162 | 56 | `clj->js` | `(-> (.listInvites policyDb (clj->js (cond-> {:orgId org-id}` |
| 168 | 50 | `clj->js` | `(clj->js {:error (.-message err)}))))))))` |
| 171 | 35 | `clj->js` | `(clj->js {:error (or (.-message err) "Unauthorized")}))))))))))` |
### `backend/src/cljs/knoxx/backend/infra/routes/contracts.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 393 | 56 | `clj->js` | `(-> (js/Promise.all (clj->js ops))` |
| 460 | 35 | `clj->js` | `(clj->js {:recursive true})` |
| 618 | 44 | `clj->js` | `(.end reply (.status reply status) text (clj->js {"Content-Type" "text/plain; charset=utf-8"})))` |
| 622 | 4 | `js->clj` | `(js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true))` |
### `backend/src/cljs/knoxx/backend/infra/routes/documents.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 80 | 41 | `clj->js` | `(js-await [written (js/Promise.all (clj->js promises))]` |
| 113 | 32 | `clj->js` | `(-> (fs-rm! fs abs-path (clj->js {:force true}))` |
| 124 | 15 | `js->clj` | `body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)]` |
| 133 | 15 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
| 191 | 15 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
| 247 | 15 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
| 288 | 15 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
| 304 | 15 | `js->clj` | `body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
### `backend/src/cljs/knoxx/backend/infra/routes/mcp.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 108 | 36 | `clj->js` | `(-> (.code reply status) (.send (clj->js payload))))` |
| 155 | 33 | `clj->js` | `(aset raw "rawHeaders" (clj->js (conj filtered "accept" accept-value)))))` |
| 484 | 83 | `clj->js` | `(-> (redis-set! redis (str "knoxx:mcp:client:" client-id) (js/JSON.stringify (clj->js client)) js/undefined)` |
| 527 | 93 | `clj->js` | `(-> (redis-set! redis (str "knoxx:mcp:code:" code) (js/JSON.stringify (clj->js payload)) (clj->js {:EX code-ttl}))` |
| 527 | 112 | `clj->js` | `(-> (redis-set! redis (str "knoxx:mcp:code:" code) (js/JSON.stringify (clj->js payload)) (clj->js {:EX code-ttl}))` |
| 543 | 85 | `clj->js` | `(-> (redis-set! redis (str "knoxx:mcp:token:" access-token) (js/JSON.stringify (clj->js token-value)) (clj->js {:EX token-ttl}))` |
| 543 | 108 | `clj->js` | `(-> (redis-set! redis (str "knoxx:mcp:token:" access-token) (js/JSON.stringify (clj->js token-value)) (clj->js {:EX token-ttl}))` |
| 587 | 20 | `clj->js` | `(clj->js (for [tid (array-seq token-ids)]` |
| 643 | 36 | `clj->js` | `(do (.writeHead raw-res 401 (clj->js {"WWW-Authenticate" (www-authenticate-challenge base)` |
| 650 | 45 | `clj->js` | `(do (.writeHead raw-res 401 (clj->js {"WWW-Authenticate" (www-authenticate-challenge base)` |
| 661 | 55 | `clj->js` | `server (new McpServer (clj->js {:name "knoxx" :version "0.1.0"}))` |
| 663 | 44 | `clj->js` | `(clj->js {:sessionIdGenerator js/undefined}))]` |
| 668 | 50 | `clj->js` | `(let [tool-config (clj->js {:description (str (or (aget tool "description") (aget tool "label") n))` |
| 686 | 48 | `clj->js` | `(.writeHead raw-res 500 (clj->js {"Content-Type" "application/json"}))` |
| 687 | 57 | `clj->js` | `(.end raw-res (js/JSON.stringify (clj->js {:error "mcp_post_failed"` |
### `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 77 | 24 | `clj->js` | `(.stringify js/JSON (clj->js value)))` |
| 205 | 33 | `js->clj` | `(array? value) (js->clj value)` |
| 220 | 16 | `clj->js` | `(clj->js` |
| 232 | 24 | `js->clj` | `(->> (js->clj results :keywordize-keys true)` |
| 296 | 20 | `clj->js` | `(clj->js` |
| 307 | 51 | `js->clj` | `(let [allowed-sessions (->> (js->clj results :keywordize-keys true)` |
| 506 | 44 | `clj->js` | `(-> (.all js/Promise (clj->js enrich-promises))` |
| 510 | 72 | `js->clj` | `:rows (vec (js->clj enriched-rows :keywordize-keys true))` |
| 521 | 47 | `clj->js` | `(-> (.all js/Promise (clj->js (mapv #(session-store/get-session redis-client %) (vec live-ids))))` |
| 524 | 49 | `js->clj` | `(let [live (vec (js->clj live-js :keywordize-keys true))` |
| 535 | 56 | `clj->js` | `(-> (.all js/Promise (clj->js (mapv (partial enrich-row redis-client) all-rows)))` |
| 539 | 84 | `js->clj` | `:rows (vec (js->clj enriched-rows :keywordize-keys true))` |
| 550 | 54 | `clj->js` | `(-> (.all js/Promise (clj->js enrich-promises))` |
| 554 | 82 | `js->clj` | `:rows (vec (js->clj enriched-rows :keywordize-keys true))` |
| 592 | 17 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)))` |
### `backend/src/cljs/knoxx/backend/infra/routes/models.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 259 | 59 | `clj->js` | `:body (.stringify js/JSON (clj->js payload))})` |
### `backend/src/cljs/knoxx/backend/infra/routes/multimodal.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 60 | 33 | `clj->js` | `(fs-mkdir! fs upload-path (clj->js {:recursive true}))` |
| 152 | 44 | `clj->js` | `(js/Promise.all (clj->js upload-promises))` |
### `backend/src/cljs/knoxx/backend/infra/routes/studio/discord_scan.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 31 | 19 | `clj->js` | `(clj->js {:method "GET"` |
| 326 | 61 | `clj->js` | `(-> (media/fs-mkdir! fs dir-absolute (clj->js {:recursive true}))` |
| 329 | 96 | `clj->js` | `(media/fs-write-file! fs meta-absolute (.stringify js/JSON (clj->js metadata) nil 2) "utf8")))` |
| 357 | 61 | `clj->js` | `(-> (media/fs-mkdir! fs dir-absolute (clj->js {:recursive true}))` |
| 360 | 96 | `clj->js` | `(media/fs-write-file! fs meta-absolute (.stringify js/JSON (clj->js metadata) nil 2) "utf8")))` |
| 372 | 43 | `clj->js` | `(-> (media/fs-mkdir! fs dir-absolute (clj->js {:recursive true}))` |
| 374 | 78 | `clj->js` | `(media/fs-write-file! fs file-absolute (.stringify js/JSON (clj->js manifest) nil 2) "utf8")))` |
| 386 | 55 | `js->clj` | `(let [channel-ids (vec (remove str/blank? (map str (js->clj (or (aget body "channel_ids") (js/Array.))))))` |
### `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 44 | 37 | `clj->js` | `(-> (.readdir node-fs root-dir (clj->js {:withFileTypes true}))` |
| 82 | 114 | `clj->js` | `(-> (.query db "SELECT state_json FROM studio_state WHERE user_id = $1 AND org_id = $2 AND kind = $3" (clj->js [user-id org-id kind]))` |
| 85 | 78 | `js->clj` | `(json-response! reply 200 {:ok true :state (if row (js->clj (.-state_json row) :keywordize-keys true) {})}))))` |
| 99 | 20 | `js->clj` | `state (js->clj (or (aget body "state") (js/Object.)) :keywordize-keys true)]` |
| 101 | 224 | `clj->js` | `(-> (.query db "INSERT INTO studio_state (user_id,org_id,kind,state_json) VALUES ($1,$2,$3,$4::jsonb) ON CONFLICT (user_id,org_id,kind) DO UPDATE SET state_j...` |
| 101 | 274 | `clj->js` | `(-> (.query db "INSERT INTO studio_state (user_id,org_id,kind,state_json) VALUES ($1,$2,$3,$4::jsonb) ON CONFLICT (user_id,org_id,kind) DO UPDATE SET state_j...` |
| 115 | 116 | `clj->js` | `(-> (.query db "SELECT state_json FROM studio_state WHERE user_id=$1 AND org_id=$2 AND kind='playlist'" (clj->js [user-id org-id]))` |
| 118 | 45 | `js->clj` | `state (if row (js->clj (.-state_json row) :keywordize-keys true) {})]` |
| 132 | 20 | `js->clj` | `items (js->clj (or (aget body "items") (js/Array.)) :keywordize-keys true)]` |
| 134 | 220 | `clj->js` | `(-> (.query db "INSERT INTO studio_state (user_id,org_id,kind,state_json) VALUES ($1,$2,'playlist',$3::jsonb) ON CONFLICT (user_id,org_id,kind) DO UPDATE SET...` |
| 134 | 265 | `clj->js` | `(-> (.query db "INSERT INTO studio_state (user_id,org_id,kind,state_json) VALUES ($1,$2,'playlist',$3::jsonb) ON CONFLICT (user_id,org_id,kind) DO UPDATE SET...` |
| 166 | 16 | `js->clj` | `items (js->clj (or (aget body "items") (js/Array.)) :keywordize-keys true)` |
| 181 | 30 | `clj->js` | `(-> (.mkdir fs absolute (clj->js {:recursive true}))` |
| 190 | 16 | `js->clj` | `items (js->clj (or (aget body "items") (js/Array.)) :keywordize-keys true)` |
| 251 | 40 | `js->clj` | `(let [m3u-files (->> (js->clj files)` |
| 336 | 136 | `clj->js` | `(-> (.query db "SELECT image_data, mime_type, width, height FROM studio_audio_assets WHERE audio_path = $1 AND asset_type = $2" (clj->js [audio-path asset-ty...` |
| 339 | 40 | `js->clj` | `(let [row (first (js->clj (.-rows res)))]` |
| 364 | 22 | `clj->js` | `(clj->js [audio-path asset-type buffer mime-type width height]))` |
### `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 212 | 58 | `clj->js` | `:body (js/JSON.stringify (clj->js {:source_id (aget eta-mu-source "source_id")` |
| 315 | 58 | `clj->js` | `:body (js/JSON.stringify (clj->js {:source_id (aget opencode-source "source_id")` |
| 350 | 36 | `clj->js` | `(clj->js (map #(enrich-session-summary! config %) (vec (or (:rows body) [])))))]` |
| 351 | 29 | `clj->js` | `(.send reply (clj->js (assoc body :rows (vec (array-seq enriched)))))))))` |
### `backend/src/cljs/knoxx/backend/infra/routes/tools.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 30 | 44 | `clj->js` | `(clj->js {:host "smtp.gmail.com"` |
| 35 | 21 | `clj->js` | `(clj->js {:from email` |
| 153 | 61 | `clj->js` | `(clj->js {:query query` |
| 181 | 49 | `clj->js` | `(-> (.readdir fs path-str (clj->js {:withFileTypes true}))` |
| 227 | 46 | `clj->js` | `(.mkdir fs parent (clj->js {:recursive true}))` |
| 279 | 30 | `clj->js` | `(clj->js ["-lc" (or (aget body "command") "")])` |
| 280 | 30 | `clj->js` | `(clj->js {:cwd workdir` |
| 354 | 24 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
| 386 | 17 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)]` |
| 445 | 24 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
| 477 | 17 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)]` |
| 536 | 24 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
| 611 | 20 | `js->clj` | `args (js->clj (or (aget body "arguments") (js/Object.)) :keywordize-keys true)]` |
### `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 77 | 25 | `js->clj` | `body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
| 84 | 22 | `clj->js` | `(clj->js body-with-auth))` |
| 117 | 22 | `clj->js` | `(clj->js {:method "GET" :headers (openplanner-headers config)}))` |
| 133 | 25 | `js->clj` | `(let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
| 135 | 90 | `clj->js` | `(-> (openplanner-request! config "POST" "/v1/translations/segments/batch" (clj->js body-with-auth))` |
| 182 | 25 | `js->clj` | `body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)` |
| 188 | 22 | `clj->js` | `(clj->js body-with-auth))` |
### `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 25 | 74 | `clj->js` | `(policy-db-promise runtime reply 200 (.listUsers db (clj->js {:orgId org-id}))))))` |
| 48 | 74 | `clj->js` | `(policy-db-promise runtime reply 200 (.listUsers db (clj->js {:orgId org-id}))))))` |
| 61 | 69 | `clj->js` | `(.listUsers db (clj->js {:orgId org-id})))))))))` |
| 69 | 67 | `clj->js` | `payload (.assign js/Object (js/Object.) body (clj->js {:orgId org-id}))]` |
| 81 | 67 | `clj->js` | `payload (.assign js/Object (js/Object.) body (clj->js {:orgId org-id}))]` |
| 123 | 67 | `clj->js` | `payload (.assign js/Object (js/Object.) body (clj->js {:provider provider}))]` |
| 139 | 67 | `clj->js` | `payload (.assign js/Object (js/Object.) body (clj->js {:provider provider}))]` |
| 155 | 80 | `clj->js` | `(policy-db-promise runtime reply 200 (.listMemberships db (clj->js {:orgId org-id}))))))` |
| 167 | 71 | `js->clj` | `(let [membership (js->clj (aget result "membership") :keywordize-keys true)]` |
| 183 | 71 | `js->clj` | `(let [membership (js->clj (aget result "membership") :keywordize-keys true)]` |
### `backend/src/cljs/knoxx/backend/infra/routes/voice.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 120 | 40 | `clj->js` | `(.send socket (.stringify js/JSON (clj->js payload)))))` |
| 148 | 42 | `js->clj` | `:alignment (js->clj (aget payload "alignment") :keywordize-keys true)` |
| 149 | 53 | `js->clj` | `:normalized_alignment (js->clj (aget payload "normalizedAlignment") :keywordize-keys true)})` |
| 156 | 40 | `js->clj` | `:payload (js->clj payload :keywordize-keys true)}))))` |
| 161 | 16 | `clj->js` | `(clj->js {:method "GET"` |
| 166 | 48 | `clj->js` | `(.send (clj->js {:error "WebSocket upgrade required"}))))` |
| 347 | 38 | `clj->js` | `(let [payload (clj->js` |
| 362 | 64 | `js->clj` | `(assoc :voice_settings (js->clj voice-settings))))` |
### `backend/src/cljs/knoxx/backend/infra/routes/workspace_media.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 111 | 88 | `clj->js` | `stream (.createReadStream node-fs absolute (clj->js {:start start :end end}))]` |
| 147 | 32 | `clj->js` | `(-> (.readdir fs root-dir (clj->js {:withFileTypes true}))` |
| 245 | 50 | `clj->js` | `(-> (.mkdir fs absolute (clj->js {:recursive true}))` |
### `backend/src/cljs/knoxx/backend/infra/source/opencode_session_ingester.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 19 | 17 | `#js` | `(let [headers #js {"accept" "application/json"}` |
| 53 | 14 | `#js` | `#js {:ok true` |
| 63 | 19 | `#js` | `#js {:headers (auth-headers)` |
| 70 | 8 | `#js` | `#js [(fetch-json-response "/global/health" {})` |
| 77 | 12 | `#js` | `#js {:ok true` |
| 84 | 10 | `#js` | `#js {:ok false` |
| 101 | 12 | `#js` | `#js {:ok true` |
### `backend/src/cljs/knoxx/backend/infra/stores/composite_session_store.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 52 | 52 | `clj->js` | `(clj->js (ex-data` |
### `backend/src/cljs/knoxx/backend/infra/stores/redis_session_store.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 37 | 26 | `clj->js` | `(clj->js (mapv #(.get-run store %) run-ids)))]` |
| 38 | 15 | `js->clj` | `(->> (js->clj runs :keywordize-keys true)` |
### `backend/src/cljs/knoxx/backend/infra/stores/session_store.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 164 | 37 | `js->clj` | `current (if (array? raw) (js->clj raw :keywordize-keys true) raw)` |
| 241 | 44 | `clj->js` | `(-> (.all js/Promise (clj->js (mapv #(get-session redis-client %) ids)))` |
| 243 | 56 | `js->clj` | `(let [sessions (vec (js->clj results :keywordize-keys true))` |
| 331 | 37 | `clj->js` | `(js/console.log "Session cache:" (clj->js @session-cache*)))` |
### `backend/src/cljs/knoxx/backend/infra/svg_render.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 47 | 14 | `#js` | `(let [opts #js {:args #js ["--no-sandbox"` |
| 47 | 25 | `#js` | `(let [opts #js {:args #js ["--no-sandbox"` |
| 91 | 41 | `#js` | `(js-await [_ (.setViewport page #js {:width width :height height})]` |
| 93 | 70 | `#js` | `(js-await [_ (.setContent page (svg-document svg-string) #js {:waitUntil "networkidle0"})]` |
| 97 | 53 | `#js` | `(js-await [png (.screenshot element #js {:type "png"` |
### `backend/src/cljs/knoxx/backend/infra/tooling.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 122 | 37 | `clj->js` | `(clj->js {:contract-id agent-contract-id` |
### `backend/src/cljs/knoxx/backend/shape/app_shapes.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 80 | 29 | `js->clj` | `(let [spec (some-> value (js->clj :keywordize-keys true))` |
| 201 | 30 | `js->clj` | `(let [parts (some-> value (js->clj :keywordize-keys true))]` |
| 230 | 33 | `js->clj` | `(js->clj :keywordize-keys true))` |
| 236 | 29 | `js->clj` | `(js->clj :keywordize-keys true))}))` |
| 242 | 22 | `#js` | `#js {})]` |
| 256 | 17 | `js->clj` | `:metadata (js->clj metadata :keywordize-keys true)}))` |
| 267 | 17 | `#js` | `(.route app #js {:method method` |
| 271 | 26 | `#js` | `#js {:method method :url url}` |
### `backend/src/cljs/knoxx/backend/tools/mcp.cljs`

| Line | Col | Token | Snippet |
|---:|---:|---|---|
| 24 | 6 | `#js` | `#js [])))` |

