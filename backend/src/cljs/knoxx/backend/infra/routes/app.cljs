(ns knoxx.backend.infra.routes.app
  "Public application route API and ordered composition."
  (:require [knoxx.backend.extern.app-health :as app-health]
            [knoxx.backend.extern.app-http :as app-http]
            [knoxx.backend.infra.agent.hydration :as agent-hydration]
            [knoxx.backend.infra.routes.app.administration :as app-administration]
            [knoxx.backend.infra.routes.app.chat-start :as app-chat-start]
            [knoxx.backend.infra.routes.app.data :as app-data]
            [knoxx.backend.infra.routes.app.dependencies :as app-dependencies]
            [knoxx.backend.infra.routes.app.ingestion :as app-ingestion]
            [knoxx.backend.infra.routes.app.resources :as app-resources]
            [knoxx.backend.infra.routes.app.runs :as app-runs]
            [knoxx.backend.infra.routes.app.session-recovery :as app-session-recovery]
            [knoxx.backend.infra.routes.app.session-views :as app-session-views]
            [knoxx.backend.infra.routes.app.status :as app-status]))

(def send-agent-turn-best-effort!
  "Compatibility entrypoint for app-chat-start/send-agent-turn-best-effort!." app-chat-start/send-agent-turn-best-effort!)

(def queue-chat-start!
  "Compatibility entrypoint for app-chat-start/queue-chat-start!." app-chat-start/queue-chat-start!)

(def queue-direct-start!
  "Compatibility entrypoint for app-chat-start/queue-direct-start!." app-chat-start/queue-direct-start!)

(def live-active-agent-summaries!
  "Compatibility entrypoint for app-session-views/live-active-agent-summaries!." app-session-views/live-active-agent-summaries!)

(def SESSION_RECOVERY_STALE_MS
  "Compatibility entrypoint for app-session-recovery/SESSION_RECOVERY_STALE_MS." app-session-recovery/SESSION_RECOVERY_STALE_MS)

(def latest-run-event!
  "Compatibility entrypoint for app-session-recovery/latest-run-event!." app-session-recovery/latest-run-event!)

(def send-knoxx-proxy!
  "Compatibility entrypoint for app-http/send-knoxx-proxy!." app-http/send-knoxx-proxy!)

(def send-fetch-json!
  "Compatibility entrypoint for app-http/send-fetch-json!." app-http/send-fetch-json!)

(def send-fetch-json-detail!
  "Compatibility entrypoint for app-http/send-fetch-json-detail!." app-http/send-fetch-json-detail!)

(def send-openplanner-v1-json!
  "Compatibility entrypoint for app-http/send-openplanner-v1-json!." app-http/send-openplanner-v1-json!)

(def send-json-promise!
  "Compatibility entrypoint for app-http/send-json-promise!." app-http/send-json-promise!)

(def send-mongo-collections!
  "Compatibility entrypoint for app-http/send-mongo-collections!." app-http/send-mongo-collections!)

(def send-data-browse!
  "Compatibility entrypoint for app-http/send-data-browse!." app-http/send-data-browse!)

(def write-ingestion-file!
  "Compatibility entrypoint for app-http/write-ingestion-file!." app-http/write-ingestion-file!)

(def service-health-check!
  "Compatibility entrypoint for app-health/service-health-check!." app-health/service-health-check!)

(def openplanner-health-check!
  "Compatibility entrypoint for app-health/openplanner-health-check!." app-health/openplanner-health-check!)

(def proxx-health-check!
  "Compatibility entrypoint for app-health/proxx-health-check!." app-health/proxx-health-check!)

(def ollama-health-check!
  "Compatibility entrypoint for app-health/ollama-health-check!." app-health/ollama-health-check!)

(def send-data-health!
  "Compatibility entrypoint for app-health/send-data-health!." app-health/send-data-health!)

(def send-knoxx-health!
  "Compatibility entrypoint for app-health/send-knoxx-health!." app-health/send-knoxx-health!)

(def deps
  "Compatibility entrypoint for app-dependencies/deps." app-dependencies/deps)

(def detect-zombies
  "Compatibility entrypoint for app-session-recovery/detect-zombies." app-session-recovery/detect-zombies)

(def handle-chat-start
  "Compatibility entrypoint for app-chat-start/handle-chat-start." app-chat-start/handle-chat-start)

(def handle-direct-start
  "Compatibility entrypoint for app-chat-start/handle-direct-start." app-chat-start/handle-direct-start)

(def health!
  "Compatibility entrypoint for app-status/health!." app-status/health!)

(def dev-hmr!
  "Compatibility entrypoint for app-status/dev-hmr!." app-status/dev-hmr!)

(def config!
  "Compatibility entrypoint for app-status/config!." app-status/config!)

(def api-knoxx-agents-catalog!
  "Compatibility entrypoint for app-status/api-knoxx-agents-catalog!." app-status/api-knoxx-agents-catalog!)

(def api-auth-context!
  "Compatibility entrypoint for app-status/api-auth-context!." app-status/api-auth-context!)

(def api-knoxx-proxy-get!
  "Compatibility entrypoint for app-ingestion/api-knoxx-proxy-get!." app-ingestion/api-knoxx-proxy-get!)

(def api-knoxx-proxy-post!
  "Compatibility entrypoint for app-ingestion/api-knoxx-proxy-post!." app-ingestion/api-knoxx-proxy-post!)

(def api-knoxx-proxy-put!
  "Compatibility entrypoint for app-ingestion/api-knoxx-proxy-put!." app-ingestion/api-knoxx-proxy-put!)

(def api-knoxx-proxy-patch!
  "Compatibility entrypoint for app-ingestion/api-knoxx-proxy-patch!." app-ingestion/api-knoxx-proxy-patch!)

(def api-knoxx-proxy-delete!
  "Compatibility entrypoint for app-ingestion/api-knoxx-proxy-delete!." app-ingestion/api-knoxx-proxy-delete!)

(def api-ingestion-browse!
  "Compatibility entrypoint for app-ingestion/api-ingestion-browse!." app-ingestion/api-ingestion-browse!)

(def api-ingestion-file!
  "Compatibility entrypoint for app-ingestion/api-ingestion-file!." app-ingestion/api-ingestion-file!)

(def api-ingestion-file-put!
  "Compatibility entrypoint for app-ingestion/api-ingestion-file-put!." app-ingestion/api-ingestion-file-put!)

(def api-ingestion-sources!
  "Compatibility entrypoint for app-ingestion/api-ingestion-sources!." app-ingestion/api-ingestion-sources!)

(def api-ingestion-jobs-get!
  "Compatibility entrypoint for app-ingestion/api-ingestion-jobs-get!." app-ingestion/api-ingestion-jobs-get!)

(def api-ingestion-jobs-post!
  "Compatibility entrypoint for app-ingestion/api-ingestion-jobs-post!." app-ingestion/api-ingestion-jobs-post!)

(def api-ingestion-proxy-get!
  "Compatibility entrypoint for app-ingestion/api-ingestion-proxy-get!." app-ingestion/api-ingestion-proxy-get!)

(def api-ingestion-proxy-post!
  "Compatibility entrypoint for app-ingestion/api-ingestion-proxy-post!." app-ingestion/api-ingestion-proxy-post!)

(def api-ingestion-proxy-delete!
  "Compatibility entrypoint for app-ingestion/api-ingestion-proxy-delete!." app-ingestion/api-ingestion-proxy-delete!)

(def api-data-op-get!
  "Compatibility entrypoint for app-data/api-data-op-get!." app-data/api-data-op-get!)

(def authorize-openplanner-proxy-post!
  "Compatibility entrypoint for app-data/authorize-openplanner-proxy-post!." app-data/authorize-openplanner-proxy-post!)

(def api-data-op-post!
  "Compatibility entrypoint for app-data/api-data-op-post!." app-data/api-data-op-post!)

(def api-data-op-delete!
  "Compatibility entrypoint for app-data/api-data-op-delete!." app-data/api-data-op-delete!)

(def api-data-op-patch!
  "Compatibility entrypoint for app-data/api-data-op-patch!." app-data/api-data-op-patch!)

(def api-data-health!
  "Compatibility entrypoint for app-data/api-data-health!." app-data/api-data-health!)

(def api-data-mongo-collections!
  "Compatibility entrypoint for app-data/api-data-mongo-collections!." app-data/api-data-mongo-collections!)

(def api-data-mongo-list!
  "Compatibility entrypoint for app-data/api-data-mongo-list!." app-data/api-data-mongo-list!)

(def data-mongo-read-permission
  "Compatibility entrypoint for app-data/data-mongo-read-permission." app-data/data-mongo-read-permission)

(def authorize-mongo-query!
  "Compatibility entrypoint for app-data/authorize-mongo-query!." app-data/authorize-mongo-query!)

(def api-data-mongo-query!
  "Compatibility entrypoint for app-data/api-data-mongo-query!." app-data/api-data-mongo-query!)

(def api-data-pg-tables!
  "Compatibility entrypoint for app-data/api-data-pg-tables!." app-data/api-data-pg-tables!)

(def api-data-jobs-build-semantic-edges!
  "Compatibility entrypoint for app-data/api-data-jobs-build-semantic-edges!." app-data/api-data-jobs-build-semantic-edges!)

(def api-data-pg-query!
  "Compatibility entrypoint for app-data/api-data-pg-query!." app-data/api-data-pg-query!)

(def api-data-browse!
  "Compatibility entrypoint for app-data/api-data-browse!." app-data/api-data-browse!)

(def api-data-file!
  "Compatibility entrypoint for app-data/api-data-file!." app-data/api-data-file!)

(def api-data-graphql!
  "Compatibility entrypoint for app-data/api-data-graphql!." app-data/api-data-graphql!)

(def api-data-graph-status!
  "Compatibility entrypoint for app-data/api-data-graph-status!." app-data/api-data-graph-status!)

(def api-data-graph-view-url!
  "Compatibility entrypoint for app-data/api-data-graph-view-url!." app-data/api-data-graph-view-url!)

(def api-knoxx-health!
  "Compatibility entrypoint for app-runs/api-knoxx-health!." app-runs/api-knoxx-health!)

(def api-knoxx-chat!
  "Compatibility entrypoint for app-runs/api-knoxx-chat!." app-runs/api-knoxx-chat!)

(def api-knoxx-chat-start!
  "Compatibility entrypoint for app-runs/api-knoxx-chat-start!." app-runs/api-knoxx-chat-start!)

(def api-knoxx-direct!
  "Compatibility entrypoint for app-runs/api-knoxx-direct!." app-runs/api-knoxx-direct!)

(def api-knoxx-direct-start!
  "Compatibility entrypoint for app-runs/api-knoxx-direct-start!." app-runs/api-knoxx-direct-start!)

(def api-knoxx-steer!
  "Compatibility entrypoint for app-runs/api-knoxx-steer!." app-runs/api-knoxx-steer!)

(def api-knoxx-follow-up!
  "Compatibility entrypoint for app-runs/api-knoxx-follow-up!." app-runs/api-knoxx-follow-up!)

(def api-knoxx-abort!
  "Compatibility entrypoint for app-runs/api-knoxx-abort!." app-runs/api-knoxx-abort!)

(def api-knoxx-session-undo!
  "Compatibility entrypoint for app-runs/api-knoxx-session-undo!." app-runs/api-knoxx-session-undo!)

(def api-knoxx-agents-active!
  "Compatibility entrypoint for app-runs/api-knoxx-agents-active!." app-runs/api-knoxx-agents-active!)

(def api-admin-agents-active!
  "Compatibility entrypoint for app-runs/api-admin-agents-active!." app-runs/api-admin-agents-active!)

(def api-admin-agents-abort!
  "Compatibility entrypoint for app-runs/api-admin-agents-abort!." app-runs/api-admin-agents-abort!)

(def api-knoxx-session-status!
  "Compatibility entrypoint for app-runs/api-knoxx-session-status!." app-runs/api-knoxx-session-status!)

(def api-knoxx-run-events!
  "Compatibility entrypoint for app-runs/api-knoxx-run-events!." app-runs/api-knoxx-run-events!)

(def api-knoxx-run-get!
  "Compatibility entrypoint for app-runs/api-knoxx-run-get!." app-runs/api-knoxx-run-get!)

(def api-shibboleth-handoff!
  "Compatibility entrypoint for app-runs/api-shibboleth-handoff!." app-runs/api-shibboleth-handoff!)

(defn register-routes!
  "Initialize settings and register every application surface in its existing order."
  [runtime app config lounge-messages*]
  (agent-hydration/ensure-settings! config)
  (app-status/register-core-routes! app runtime config)
  (app-administration/register-admin-and-memory-routes! app runtime config lounge-messages*)
  (app-administration/register-tooling-route-groups! app runtime config)
  (app-resources/register-resource-and-media-routes! app runtime config)
  (app-ingestion/register-ingestion-routes! app runtime config)
  (app-data/register-data-routes! app runtime config)
  (app-runs/register-knoxx-run-routes! app runtime config)
  (app-resources/register-translation-route-group! app runtime config))
