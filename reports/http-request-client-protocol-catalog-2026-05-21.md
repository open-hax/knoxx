# HTTP request catalog and client protocol map

Generated: 2026-05-21

## Scope

- Included: `backend/src/cljs/knoxx/backend/**/*.cljs`
- Excluded from request-source scan: `backend/src/cljs/knoxx/backend/extern/**`
- Included request forms: direct `js/fetch`, compatibility `fetch-json` / `fetch-with-timeout`, `openplanner-request!`, `forward-knoxx-request!`, and Discord SDK `(.fetch ...)` calls.
- Excluded require/import lines and dependency-injection map entries that merely pass `fetch-json` as a function value.
- This is a request-boundary catalog, not a data-boundary catalog; see `reports/js-interop-catalog-2026-05-20.md` for `#js`/`clj->js`/`js->clj`.

## Totals

- Total request-shaped call sites: **120**
- Files with request-shaped call sites: **23**

| Request form | Count |
|---|---:|
| `openplanner-request-call` | 43 |
| `fetch-json-call` | 35 |
| `direct-js-fetch` | 13 |
| `fetch-with-timeout-call` | 12 |
| `discord-sdk-fetch` | 11 |
| `forward-knoxx-request-call` | 6 |

## Protocol target counts

| Target protocol | Count |
|---|---:|
| `IOpenPlannerClient` | 54 |
| `IKnoxxControlClient` | 23 |
| `IDiscordGatewayClient (needed)` | 11 |
| `IIngestionClient` | 10 |
| `IBlazeClient` | 3 |
| `IAudDClient / IRemoteMediaClient` | 3 |
| `IHttpClient` | 3 |
| `IRemoteMediaClient` | 2 |
| `ITtsClient` | 1 |
| `IGitHubClient` | 2 |
| `IGraphClient` | 2 |
| `ISttClient` | 2 |
| `IMcpHttpClient` | 1 |
| `IHealthProbeClient` | 1 |
| `IShibbolethClient` | 1 |
| `IOpenCodeClient` | 1 |

## Protocol artifacts created

- `backend/src/cljs/knoxx/backend/extern/fetch.cljs` — `IHttpClient`; owns native fetch/AbortController/Response parsing.
- `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` — `IOpenPlannerClient`; implemented with `extern.fetch`.
- `backend/src/cljs/knoxx/backend/domain/discord/rest_client.cljs` — `IDiscordRestClient`; implemented with `extern.fetch`; all non-voice Discord REST callers migrated.
- `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` — `IKnoxxControlClient`; implemented with `extern.fetch` for JSON control calls; preserves `KNOXX_BASE_URL`/`KNOXX_API_KEY` env fallback.
- `backend/src/cljs/knoxx/backend/domain/contracts/client.cljs` — `IContractLibrarianClient`; implemented with `extern.fetch`.
- `backend/src/cljs/knoxx/backend/infra/clients/proxx.cljs` — `IProxxClient`; implemented with `extern.fetch`; Proxx route/tool call sites migrated behind this client.
- `backend/src/cljs/knoxx/backend/infra/clients/github.cljs` — `IGitHubClient` protocol.
- `backend/src/cljs/knoxx/backend/infra/clients/opencode.cljs` — `IOpenCodeClient` protocol.
- `backend/src/cljs/knoxx/backend/infra/clients/provider_catalog.cljs` — `IProviderCatalogClient` protocol.
- `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` — `IIngestionClient` protocol.
- `backend/src/cljs/knoxx/backend/infra/clients/graph.cljs` — `IGraphClient` protocol.
- `backend/src/cljs/knoxx/backend/infra/clients/shibboleth.cljs` — `IShibbolethClient` protocol.
- `backend/src/cljs/knoxx/backend/infra/clients/health_probe.cljs` — `IHealthProbeClient` protocol.
- `backend/src/cljs/knoxx/backend/domain/bluesky/client.cljs` — `IBlueskyClient`; implemented with `extern.fetch`; Bluesky tool HTTP/XRPC calls migrated behind this client.
- `backend/src/cljs/knoxx/backend/domain/voice/client.cljs` — `ITtsClient` and `ISttClient`; implemented with `extern.fetch` for Discord voice TTS/STT and `voice.tts` synthesis flows.
- `backend/src/cljs/knoxx/backend/domain/media/remote_client.cljs` — `IRemoteMediaClient` protocol.
- `backend/src/cljs/knoxx/backend/domain/media/blaze_client.cljs` — `IBlazeClient` protocol.
- `backend/src/cljs/knoxx/backend/domain/music/audd_client.cljs` — `IAudDClient` protocol.
- `backend/src/cljs/knoxx/backend/domain/mcp/client.cljs` — `IMcpHttpClient` protocol.

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
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 527 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. active-client -channels) channel-id)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 566 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. active-client -channels) channel-id)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 635 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. active-client -guilds) guild-id)` |
| `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs` | 639 | `discord-sdk-fetch` | `IDiscordGatewayClient (needed)` | `backend/src/cljs/knoxx/backend/domain/discord/gateway.cljs adapter methods` | `(-> (.fetch (.. guild -channels) channel-id)` |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 39 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (fetch-json! config "GET" "/api/admin/config/events" nil)` |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 45 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (fetch-json! config "POST" "/api/admin/config/events/dispatch"` |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 71 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (fetch-json! config "PUT" "/api/admin/config/events" next-control)` |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 104 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (fetch-json! config "POST" (str "/api/admin/config/events/jobs/" (js/encodeURIComponent job-id) "/run") nil)` |
| `backend/src/cljs/knoxx/backend/domain/event/tools.cljs` | 130 | `fetch-json-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (fetch-json! config "POST" "/api/knoxx/direct/start" {:message message` |
| `backend/src/cljs/knoxx/backend/domain/mcp/mcp_bridge.cljs` | 98 | `fetch-with-timeout-call` | `IMcpHttpClient` | `backend/src/cljs/knoxx/backend/domain/mcp/client.cljs` | `(-> (http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/domain/media/blaze.cljs` | 367 | `direct-js-fetch` | `IBlazeClient` | `backend/src/cljs/knoxx/backend/domain/media/blaze_client.cljs` | `(p/let [res (js/fetch url #js {:method "POST"` |
| `backend/src/cljs/knoxx/backend/domain/media/blaze.cljs` | 486 | `direct-js-fetch` | `IBlazeClient` | `backend/src/cljs/knoxx/backend/domain/media/blaze_client.cljs` | `(p/let [res (js/fetch url #js {:headers #js {"Accept" "image/*,audio/*,video/*,*/*"}` |
| `backend/src/cljs/knoxx/backend/domain/media/blaze.cljs` | 566 | `fetch-json-call` | `IBlazeClient` | `backend/src/cljs/knoxx/backend/domain/media/blaze_client.cljs` | `(-> (fetch-json! (generation-url config modality) api-key body attempt-context)` |
| `backend/src/cljs/knoxx/backend/domain/media.cljs` | 324 | `direct-js-fetch` | `IRemoteMediaClient` | `backend/src/cljs/knoxx/backend/domain/media/remote_client.cljs` | `(js/fetch source #js {:headers headers}))))` |
| `backend/src/cljs/knoxx/backend/domain/music.cljs` | 31 | `direct-js-fetch` | `IAudDClient / IRemoteMediaClient` | `backend/src/cljs/knoxx/backend/domain/music/audd_client.cljs` | `(-> (js/fetch "https://api.audd.io/" #js {:method "POST" :body form})` |
| `backend/src/cljs/knoxx/backend/domain/music.cljs` | 57 | `direct-js-fetch` | `IAudDClient / IRemoteMediaClient` | `backend/src/cljs/knoxx/backend/domain/music/audd_client.cljs` | `(-> (js/fetch url)` |
| `backend/src/cljs/knoxx/backend/domain/music.cljs` | 79 | `direct-js-fetch` | `IAudDClient / IRemoteMediaClient` | `backend/src/cljs/knoxx/backend/domain/music/audd_client.cljs` | `(js/fetch url #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0 (discord bot)"}})))` |
| `backend/src/cljs/knoxx/backend/infra/agent/session.cljs` | 169 | `direct-js-fetch` | `IRemoteMediaClient` | `backend/src/cljs/knoxx/backend/domain/media/remote_client.cljs` | `(-> (js/fetch url)` |
| `backend/src/cljs/knoxx/backend/infra/agent/turn.cljs` | 128 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (http/openplanner-request! config "POST" "/v1/events"` |
| `backend/src/cljs/knoxx/backend/infra/auth/session.cljs` | 205 | `direct-js-fetch` | `IGitHubClient` | `backend/src/cljs/knoxx/backend/infra/clients/github.cljs` | `(-> (js/fetch "https://github.com/login/oauth/access_token"` |
| `backend/src/cljs/knoxx/backend/infra/auth/session.cljs` | 227 | `direct-js-fetch` | `IGitHubClient` | `backend/src/cljs/knoxx/backend/infra/clients/github.cljs` | `(-> (js/fetch url (clj->js {:headers {:Authorization (str "Bearer " access-token)` |
| `backend/src/cljs/knoxx/backend/infra/core_memory.cljs` | 234 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (backend-http/openplanner-request! config "GET" (str "/v1/sessions/" (js/encodeURIComponent (str session-id))` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 109 | `fetch-json-call` | `IHttpClient` | `backend/src/cljs/knoxx/backend/extern/fetch.cljs` | `(fetch-json url opts default-fetch-timeout-ms))` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 127 | `openplanner-request-call` | `IHttpClient` | `backend/src/cljs/knoxx/backend/extern/fetch.cljs` | `(defn openplanner-request!` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 128 | `openplanner-request-call` | `IHttpClient` | `backend/src/cljs/knoxx/backend/extern/fetch.cljs` | `([config method suffix] (openplanner-request! config method suffix nil))` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 236 | `forward-knoxx-request-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(defn forward-knoxx-request!` |
| `backend/src/cljs/knoxx/backend/infra/http.cljs` | 242 | `fetch-with-timeout-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(fetch-with-timeout target-url (.assign js/Object base stream-opts (clj->js extra)))))` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs` | 58 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (backend-http/openplanner-request! config "POST" "/v1/documents" payload)` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs` | 163 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (backend-http/openplanner-request! config "GET" (str "/v1/sessions/" conversation-id))` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs` | 240 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(let [rows (or (:rows (await (backend-http/openplanner-request! config "GET" (str "/v1/sessions/" session-id)))) [])` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs` | 252 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(let [session-ids (->> (or (:rows (await (backend-http/openplanner-request!` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs` | 280 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `:hits (default-memory-hits (vector-result-hits (:result (await (backend-http/openplanner-request! config "POST" "/v1/search/vector"` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs` | 294 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(backend-http/openplanner-request!` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs` | 311 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `:hits (vector-result-hits (:result (backend-http/openplanner-request! config "POST" "/v1/search/vector"` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs` | 319 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(backend-http/openplanner-request! config "GET" (str "/v1/graph/export" (backend-http/request-query-string request))))` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs` | 713 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (backend-http/openplanner-request! config "POST" "/v1/events" {:events all-events})` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/tools.cljs` | 72 | `direct-js-fetch` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (js/fetch url #js {:headers #js {"User-Agent" "Knoxx-Agent/1.0"}})` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/tools.cljs` | 213 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (backend-http/openplanner-request! config "POST" "/v1/events" {:events [event]})` |
| `backend/src/cljs/knoxx/backend/infra/openplanner/tools.cljs` | 253 | `direct-js-fetch` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (js/fetch url #js {:method "POST"` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 742 | `fetch-json-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(fetch-json (openplanner-url config "/v1/health")` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 839 | `forward-knoxx-request-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (forward-knoxx-request! config request "GET" path nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 846 | `forward-knoxx-request-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (forward-knoxx-request! config request "POST" path nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 853 | `forward-knoxx-request-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (forward-knoxx-request! config request "PUT" path nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 860 | `forward-knoxx-request-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (forward-knoxx-request! config request "PATCH" path nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 867 | `forward-knoxx-request-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (forward-knoxx-request! config request "DELETE" path nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 876 | `fetch-json-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 885 | `fetch-json-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 911 | `fetch-json-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (fetch-json (str ingestion-base "/api/ingestion/sources") {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 920 | `fetch-json-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 929 | `fetch-json-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (fetch-json target-url {:method "POST"` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 941 | `fetch-json-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (fetch-json target-url {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 951 | `fetch-json-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (fetch-json target-url {:method "POST"` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 962 | `fetch-json-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (fetch-json target-url {:method "DELETE"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 975 | `fetch-json-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (fetch-json (str op-base "/v1/" path qs)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 988 | `fetch-json-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (fetch-json (str op-base "/v1/" path)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1003 | `fetch-json-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (fetch-json (str op-base "/v1/" path)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1017 | `fetch-json-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (fetch-json (str op-base "/v1/" path)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1034 | `fetch-json-call` | `IHealthProbeClient` | `backend/src/cljs/knoxx/backend/infra/clients/health_probe.cljs` | `(-> (fetch-json url {:headers (or headers {}) :method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1061 | `fetch-json-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `[(fetch-json (str op-base "/v1/documents/stats")` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1064 | `fetch-json-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(fetch-json (str op-base "/v1/graph/monitoring")` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1075 | `fetch-json-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (fetch-json (str op-base "/v1/mongo/collections")` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1087 | `fetch-json-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (fetch-json (str op-base "/v1/mongo/query")` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1112 | `fetch-json-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (fetch-json (str op-base "/v1/jobs/build-semantic-edges")` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1166 | `fetch-json-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (fetch-json target-url nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1177 | `fetch-json-call` | `IIngestionClient` | `backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs` | `(-> (fetch-json (str ingestion-base "/api/ingestion/file?path=" (js/encodeURIComponent path)) nil)` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1185 | `fetch-json-call` | `IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients/graph.cljs` | `(-> (fetch-json gw-url` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1194 | `fetch-json-call` | `IGraphClient` | `backend/src/cljs/knoxx/backend/infra/clients/graph.cljs` | `(-> (fetch-json "http://127.0.0.1:8796/api/status" {:method "GET"})` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1438 | `fetch-json-call` | `IShibbolethClient` | `backend/src/cljs/knoxx/backend/infra/clients/shibboleth.cljs` | `(-> (fetch-json (str (:shibboleth-base-url config) "/api/chat/import")` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1482 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `:openplanner-request! openplanner-request!` |
| `backend/src/cljs/knoxx/backend/infra/routes/app.cljs` | 1637 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `:openplanner-request! openplanner-request!` |
| `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs` | 238 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `[config ctx actor-id exclude-actor-ids contract-id openplanner-request! authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter? upstream-page-size upstream-offset acc needed-count]` |
| `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs` | 239 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "GET"` |
| `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs` | 269 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(fetch-authorized-session-pages! config ctx actor-id exclude-actor-ids contract-id openplanner-request! authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter? upstream-page-size next-offset next-acc needed-count)` |
| `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs` | 457 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `openplanner-request!` |
| `backend/src/cljs/knoxx/backend/infra/routes/memory.cljs` | 494 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(fetch-authorized-session-pages! config ctx actor-id exclude-actor-ids contract-id openplanner-request! authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter? upstream-page-size 0 [] needed-count)))` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 98 | `fetch-with-timeout-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(let [fetch-promise (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 139 | `fetch-with-timeout-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `kms-p (-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 152 | `fetch-with-timeout-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 196 | `fetch-with-timeout-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 208 | `fetch-with-timeout-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 234 | `fetch-with-timeout-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `kms-p (-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 247 | `fetch-with-timeout-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 299 | `fetch-with-timeout-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 311 | `fetch-with-timeout-call` | `IKnoxxControlClient` | `backend/src/cljs/knoxx/backend/infra/clients/knoxx_control.cljs` | `(-> (backend-http/fetch-with-timeout` |
| `backend/src/cljs/knoxx/backend/infra/routes/tools/proxy.cljs` | 348 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(js-await [body (backend-http/openplanner-request! config "GET" (str "/v1/sessions" (request-query-string req)))]` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 25 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `openplanner-request!` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 53 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "GET" (str "/v1/translations/segments?" params))` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 65 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "GET" (str "/v1/translations/segments/" segment-id))` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 82 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "POST"` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 97 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "GET"` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 116 | `direct-js-fetch` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (js/fetch (openplanner-url config suffix)` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 135 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "POST" "/v1/translations/segments/batch" (clj->js body-with-auth))` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 156 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "GET" (str "/v1/translations/documents?" params))` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 169 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "GET" (str "/v1/translations/documents/" doc-id "/" target-lang))` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 186 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "POST"` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 201 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "POST" "/v1/translations/batches" body)` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 219 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "GET" (str "/v1/translations/batches?" params))` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 230 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "GET" "/v1/translations/batches/next")` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 242 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "GET" (str "/v1/translations/batches/" batch-id))` |
| `backend/src/cljs/knoxx/backend/infra/routes/translation.cljs` | 255 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (openplanner-request! config "POST"` |
| `backend/src/cljs/knoxx/backend/infra/routes/voice.cljs` | 23 | `fetch-json-call` | `ISttClient` | `backend/src/cljs/knoxx/backend/domain/voice/client.cljs` | `(http/fetch-json (str base-url suffix) opts))` |
| `backend/src/cljs/knoxx/backend/infra/routes/voice.cljs` | 272 | `fetch-json-call` | `ITtsClient` | `backend/src/cljs/knoxx/backend/domain/voice/client.cljs` | `(-> (http/fetch-json (voxx-v1-url config "/voices")` |
| `backend/src/cljs/knoxx/backend/infra/routes/voice.cljs` | 367 | `fetch-with-timeout-call` | `ISttClient` | `backend/src/cljs/knoxx/backend/domain/voice/client.cljs` | `(-> (http/fetch-with-timeout url opts 30000)` |
| `backend/src/cljs/knoxx/backend/infra/source/opencode_session_ingester.cljs` | 62 | `direct-js-fetch` | `IOpenCodeClient` | `backend/src/cljs/knoxx/backend/infra/clients/opencode.cljs` | `(-> (js/fetch (.toString url)` |
| `backend/src/cljs/knoxx/backend/infra/stores/openplanner_message_source.cljs` | 13 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (http/openplanner-request! config "GET" (str "/v1/sessions/" conversation-id))` |
| `backend/src/cljs/knoxx/backend/infra/stores/openplanner_session_store.cljs` | 87 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(js-await [_ (http/openplanner-request! config "POST" "/v1/events"` |
| `backend/src/cljs/knoxx/backend/infra/stores/openplanner_session_store.cljs` | 92 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(js-await [result (http/openplanner-request! config "POST" "/v1/search/vector"` |
| `backend/src/cljs/knoxx/backend/infra/stores/session_titles.cljs` | 270 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (backend-http/openplanner-request! config` |
| `backend/src/cljs/knoxx/backend/infra/stores/session_titles.cljs` | 286 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (backend-http/openplanner-request! config` |
| `backend/src/cljs/knoxx/backend/infra/stores/session_titles.cljs` | 305 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (backend-http/openplanner-request! config` |
| `backend/src/cljs/knoxx/backend/infra/stores/session_titles.cljs` | 439 | `openplanner-request-call` | `IOpenPlannerClient` | `backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs` | `(-> (backend-http/openplanner-request! config` |

## Migration rule

New code should not call `js/fetch`, `fetch-json`, `fetch-with-timeout`, `openplanner-request!`, or `forward-knoxx-request!` from work-performing namespaces. Route/domain functions should receive a domain client protocol, and concrete clients should use `extern.fetch` internally.
