# HTTP request catalog and client protocol map

Generated: 2026-05-21
Updated: 2026-05-21 after Blaze client migration

## Scope

- Included: `backend/src/cljs/knoxx/backend/**/*.cljs`
- Excluded from request-source scan: `backend/src/cljs/knoxx/backend/extern/**`
- Included request forms: direct `js/fetch`, compatibility `fetch-json` / `fetch-json!` / `fetch-with-timeout`, `forward-knoxx-request!`, OpenPlanner proxy compatibility calls `v1-json!` / `forward-v1!`, and Discord SDK `(.fetch ...)` calls.
- Excluded require/import lines, function definitions, and dependency-injection map entries that merely pass request helpers as values.
- This is a request-boundary catalog, not a data-boundary catalog; see `reports/js-interop-catalog-2026-05-20.md` for `#js`/`clj->js`/`js->clj`.

## Totals

- Total request-shaped call sites: **67**
- Files with request-shaped call sites: **13**
- OpenPlanner shim calls (`openplanner-request!`, `openplanner-enabled?`, `openplanner-url`, `openplanner-headers`): **0**
- Direct `js/fetch` in `backend/src/cljs/knoxx/backend/domain/music.cljs`: **0**
- Direct `js/fetch` in `backend/src/cljs/knoxx/backend/domain/media/blaze.cljs`: **0**

| Request form | Count |
|---|---:|
| `fetch-json-call` | 28 |
| `discord-sdk-fetch` | 15 |
| `fetch-with-timeout-call` | 13 |
| `direct-js-fetch` | 6 |
| `openplanner-client-compat-call` | 5 |

## Protocol target counts

| Target protocol | Count |
|---|---:|
| `IDiscordGatewayClient (needed)` | 15 |
| `IIngestionClient / IHealthProbeClient / IGraphClient` | 14 |
| `IIngestionClient` | 9 |
| `IKnoxxControlClient` | 7 |
| `IOpenCodeClient` | 5 |
| `IOpenPlannerClient` | 5 |
| `TBD dedicated client` | 5 |
| `IHttpClient` | 3 |
| `IGitHubClient` | 2 |
| `IMcpHttpClient` | 1 |
| `IRemoteMediaClient` | 1 |

## Protocol artifacts created

- `backend/src/cljs/knoxx/backend/extern/fetch.cljs` — `IHttpClient`; owns native fetch/AbortController/Response parsing.
- `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` — `IOpenPlannerClient`; implemented with named OpenPlanner operations plus route-proxy compatibility methods.
- `backend/src/cljs/knoxx/backend/domain/music/audd_client.cljs` — `IAudDClient`; implemented with `extern.fetch` for AudD recognition, AcoustID lookup, and MusicBrainz recording lookup.
- `backend/src/cljs/knoxx/backend/domain/media/blaze_client.cljs` — `IBlazeClient`; implemented with `extern.fetch` for Proxx/Blaze generation requests and generated-media downloads.

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
| `backend/src/cljs/knoxx/backend/domain/media.cljs` | 324 | `direct-js-fetch` | `IRemoteMediaClient` | `backend/src/cljs/knoxx/backend/domain/media/remote_client.cljs` | `(js/fetch source #js {:headers headers}))))` |
| `backend/src/cljs/knoxx/backend/infra/agent/content_codec.cljs` | 15 | `direct-js-fetch` | `TBD dedicated client` | `TBD` | `(-> (js/fetch url)` |
| `backend/src/cljs/knoxx/backend/infra/auth/session.cljs` | 205 | `direct-js-fetch` | `IGitHubClient` | `backend/src/cljs/knoxx/backend/infra/clients/github.cljs` | `(-> (js/fetch "https://github.com/login/oauth/access_token"` |
| `backend/src/cljs/knoxx/backend/infra/auth/session.cljs` | 227 | `direct-js-fetch` | `IGitHubClient` | `backend/src/cljs/knoxx/backend/infra/clients/github.cljs` | `(-> (js/fetch url (clj->js {:headers {:Authorization (str "Bearer " access-token)` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 89 | `fetch-with-timeout-call` | `IHttpClient` | `backend/src/cljs/knoxx/backend/extern/fetch.cljs` | `([url opts] (fetch-with-timeout url opts 30000))` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 102 | `fetch-json-call` | `IHttpClient` | `backend/src/cljs/knoxx/backend/extern/fetch.cljs` | `(fetch-json url opts default-fetch-timeout-ms))` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 166 | `fetch-with-timeout-call` | `IHttpClient` | `backend/src/cljs/knoxx/backend/extern/fetch.cljs` | `(fetch-with-timeout target-url` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/tools.cljs` | 74 | `direct-js-fetch` | `TBD dedicated client` | `TBD` | `(-> (js/fetch url #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0"}})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 879 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 888 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 914 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json (str ingestion-base "/api/ingestion/sources") {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 923 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 932 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "POST"` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 944 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 954 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "POST"` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 965 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url {:method "DELETE"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 975 | `openplanner-client-compat-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-client/v1-json! (openplanner-client/client config) "GET" (str path qs) nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 983 | `openplanner-client-compat-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-client/v1-json! (openplanner-client/client config) "POST" path body)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 990 | `openplanner-client-compat-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-client/v1-json! (openplanner-client/client config) "DELETE" path nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 998 | `openplanner-client-compat-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-client/v1-json! (openplanner-client/client config) "PATCH" path body)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1007 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json url {:headers (or headers {}) :method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1129 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json target-url nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1140 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json (str ingestion-base "/api/ingestion/file?path=" (js/encodeURIComponent path)) nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1148 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json gw-url` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1157 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json "http://127.0.0.1:8796/api/status" {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1401 | `fetch-json-call` | `IIngestionClient / IHealthProbeClient / IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients` | `(-> (fetch-json (str (:shibboleth-base-url config) "/api/chat/import")` |
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
| `backend/src/cljs/knoxx/backend/infra/source/opencode_session_ingester.cljs` | 62 | `direct-js-fetch` | `IOpenCodeClient` | `backend/src/cljs/knoxx/backend/infra/clients/opencode.cljs` | `(-> (js/fetch (.toString url)` |
| `backend/src/cljs/knoxx/backend/infra/source/opencode_session_ingester.cljs` | 70 | `fetch-json-call` | `IOpenCodeClient` | `backend/src/cljs/knoxx/backend/infra/clients/opencode.cljs` | `#js [(fetch-json-response "/global/health" {})` |
| `backend/src/cljs/knoxx/backend/infra/source/opencode_session_ingester.cljs` | 71 | `fetch-json-call` | `IOpenCodeClient` | `backend/src/cljs/knoxx/backend/infra/clients/opencode.cljs` | `(fetch-json-response "/experimental/session" {:limit 20 :archived true})])` |
| `backend/src/cljs/knoxx/backend/infra/source/opencode_session_ingester.cljs` | 92 | `fetch-json-call` | `IOpenCodeClient` | `backend/src/cljs/knoxx/backend/infra/clients/opencode.cljs` | `(-> (fetch-json-response "/experimental/session"` |
| `backend/src/cljs/knoxx/backend/infra/source/opencode_session_ingester.cljs` | 109 | `fetch-json-call` | `IOpenCodeClient` | `backend/src/cljs/knoxx/backend/infra/clients/opencode.cljs` | `(fetch-json-response (str "/session/" (js/encodeURIComponent session-id) "/message") {}))` |
