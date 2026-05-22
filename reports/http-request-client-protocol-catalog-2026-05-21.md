# HTTP request catalog and client protocol map

Generated: 2026-05-21
Updated: 2026-05-21 after OpenCode client migration

## Scope

- Included: `backend/src/cljs/knoxx/backend/**/*.cljs`
- Excluded from request-source scan: `backend/src/cljs/knoxx/backend/extern/**`
- Included request forms: direct `js/fetch`, compatibility `fetch-json` / `fetch-json!` / `fetch-with-timeout`, `forward-knoxx-request!`, OpenPlanner proxy compatibility calls `v1-json!` / `forward-v1!`, and Discord SDK `(.fetch ...)` calls.
- Excluded require/import lines, function definitions, and dependency-injection map entries that merely pass request helpers as values.

## Totals

- Total request-shaped call sites: **59**
- Files with request-shaped call sites: **10**
- OpenPlanner shim calls: **0**
- Direct `js/fetch` in `backend/src/cljs/knoxx/backend/infra/source/opencode_session_ingester.cljs`: **0**
- Direct non-extern `js/fetch`: **2**

| Request form | Count |
|---|---:|
| `fetch-json-call` | 24 |
| `discord-sdk-fetch` | 15 |
| `fetch-with-timeout-call` | 13 |
| `openplanner-client-compat-call` | 5 |
| `direct-js-fetch` | 2 |

## Protocol target counts

| Target protocol | Count |
|---|---:|
| `IDiscordGatewayClient (needed)` | 15 |
| `IIngestionClient / IHealthProbeClient / IGraphClient` | 14 |
| `IIngestionClient` | 9 |
| `IKnoxxControlClient` | 7 |
| `IOpenPlannerClient` | 5 |
| `IHttpClient` | 3 |
| `TBD dedicated client` | 3 |
| `IMcpHttpClient` | 1 |
| `IRemoteMediaClient` | 1 |
| `IRemoteMediaClient / IOpenPlannerClient` | 1 |

## Protocol artifacts created

- `backend/src/cljs/knoxx/backend/extern/fetch.cljs` — `IHttpClient`; owns native fetch/AbortController/Response parsing.
- `backend/src/cljs/knoxx/backend/infra/clients/opencode.cljs` — `IOpenCodeClient`; implemented with `extern.fetch` for OpenCode health, session listing, and session message lookups.

## Full request call-site map

| File | Line | Form | Target protocol | Protocol file | Snippet |
|---|---:|---|---|---|---|
| `backend/src/cljs/knoxx/backend/domain/actor/tools.cljs` | 172 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(fetch-json! config "POST" "/api/admin/config/events/dispatch"` |
| `backend/src/cljs/knoxx/backend/domain/actor/tools.cljs` | 188 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(fetch-json! config "POST" (control-path mode)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 294 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (if (.-partial reaction) (.fetch reaction) (js/Promise.resolve reaction))` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 297 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (if (and message (.-partial message)) (.fetch message) (js/Promise.resolve message))` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 419 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. guild -channels))` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 456 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. active-client -channels) channel-id)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 460 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. channel -messages)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 474 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. active-client -users) user-id)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 478 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. dm -messages)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 503 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetchDmMessages this-fn (aget opts "userId")` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 511 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetchChannelMessages this-fn (aget opts "channelId")` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 527 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. active-client -channels) channel-id)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 566 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. active-client -channels) channel-id)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 635 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. active-client -guilds) guild-id)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 639 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. guild -channels) channel-id)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 1094 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(.fetchChannelMessages manager channel-id opts)))` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 1100 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(.fetchDmMessages manager user-id opts)))` |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 39 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (fetch-json! config "GET" "/api/admin/config/events" nil)` |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 45 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (fetch-json! config "POST" "/api/admin/config/events/dispatch"` |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 71 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (fetch-json! config "PUT" "/api/admin/config/events" next-control)` |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 104 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (fetch-json! config "POST" (str "/api/admin/config/events/jobs/" (js/encodeURIComponent job-id) "/run") nil)` |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 130 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (fetch-json! config "POST" "/api/knoxx/direct/start" {:message message` |
| `backend/src/cljs/knoxx/backend/domain/mcp/mcp_bridge.cljs` | 98 | `fetch-with-timeout-call` | `IMcpHttpClient` | `backend/src/cljs/knoxx/backend/domain/mcp/client.cljs` | `(-> (http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/agent/content_codec.cljs` | 15 | `direct-js-fetch` | `IRemoteMediaClient` | `backend/src/cljs/knoxx/backend/domain/media/remote_client.cljs` | `(-> (js/fetch url)` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 89 | `fetch-with-timeout-call` | `IHttpClient` | `backend/src/cljs/knoxx/backend/extern/fetch.cljs` | `([url opts] (fetch-with-timeout url opts 30000))` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 102 | `fetch-json-call` | `IHttpClient` | `backend/src/cljs/knoxx/backend/extern/fetch.cljs` | `(fetch-json url opts default-fetch-timeout-ms))` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 166 | `fetch-with-timeout-call` | `IHttpClient` | `backend/src/cljs/knoxx/backend/extern/fetch.cljs` | `(fetch-with-timeout target-url` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/tools.cljs` | 74 | `direct-js-fetch` | `IRemoteMediaClient / IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/domain/media/remote_client.cljs` | `(-> (js/fetch url #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0"}})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 876 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 885 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 911 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json (str ingestion-base "/api/ingestion/sources") {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 920 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 929 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "POST"` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 941 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 951 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "POST"` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 962 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "DELETE"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 972 | `openplanner-client-compat-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-client/v1-json! (openplanner-client/client config) "GET" (str path qs) nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 980 | `openplanner-client-compat-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-client/v1-json! (openplanner-client/client config) "POST" path body)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 987 | `openplanner-client-compat-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-client/v1-json! (openplanner-client/client config) "DELETE" path nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 995 | `openplanner-client-compat-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-client/v1-json! (openplanner-client/client config) "PATCH" path body)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1004 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json url {:headers (or headers {}) :method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1123 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1134 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json (str ingestion-base "/api/ingestion/file?path=" (js/encodeURIComponent path)) nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1142 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json gw-url` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1151 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json "http://127.0.0.1:8796/api/status" {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1395 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json (str (:shibboleth-base-url config) "/api/chat/import")` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 99 | `fetch-with-timeout-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(let [fetch-promise (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 140 | `fetch-with-timeout-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `kms-p (-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 153 | `fetch-with-timeout-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 197 | `fetch-with-timeout-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 209 | `fetch-with-timeout-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 235 | `fetch-with-timeout-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `kms-p (-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 248 | `fetch-with-timeout-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 300 | `fetch-with-timeout-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 312 | `fetch-with-timeout-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 369 | `openplanner-client-compat-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-client/forward-v1! (openplanner-client/client config) request*)` |
| `backend/src/cljs/knoxx/backend/infra/routes/voice.cljs` | 26 | `fetch-json-call` | `TBD dedicated client` | `TBD` | `(http/fetch-json (str base-url suffix) opts))` |
| `backend/src/cljs/knoxx/backend/infra/routes/voice.cljs` | 247 | `fetch-json-call` | `TBD dedicated client` | `TBD` | `(-> (http/fetch-json (voxx-v1-url config "/voices")` |
| `backend/src/cljs/knoxx/backend/infra/routes/voice.cljs` | 341 | `fetch-with-timeout-call` | `TBD dedicated client` | `TBD` | `(-> (http/fetch-with-timeout url opts 30000)` |
