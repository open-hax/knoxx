(ns knoxx.backend.infra.routes.memory
  "Memory route registration and compatibility exports."
  (:require-macros [knoxx.backend.macros :as routes])
  (:require ["node:crypto" :as crypto]
            [clojure.string :as str]
            [knoxx.backend.domain.graph.expansion-policy :as expansion-policy]
            [knoxx.backend.domain.graph.policy-registry :as policy-registry]
            [knoxx.backend.domain.realtime :as realtime]
            [knoxx.backend.domain.time :as time]
            [knoxx.backend.extern.memory-session-cache :as cache]
            [knoxx.backend.extern.memory-session-pages :as pages]
            [knoxx.backend.extern.memory-session-view :as view]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.core-memory :as core-memory]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.infra.openplanner.memory :as planner-memory]
            [knoxx.backend.infra.openplanner.scope :as planner-scope]
            [knoxx.backend.infra.stores.session-titles :as titles]
            [knoxx.backend.shape.memory-sessions :as memory-shape]
            [knoxx.backend.shape.parse :as parse]))

(def interactive-session-id?
  "Compatibility export for interactive-session-id?."
  pages/interactive-session-id?)

(def memory-sessions-cache-ttl-seconds
  "Compatibility export for memory-sessions-cache-ttl-seconds."
  cache/memory-sessions-cache-ttl-seconds)

(def clear-memory-sessions-cache!
  "Compatibility export for clear-memory-sessions-cache!."
  cache/clear-memory-sessions-cache!)

(def memory-sessions-auth-scope
  "Compatibility export for memory-sessions-auth-scope."
  cache/memory-sessions-auth-scope)

(def memory-sessions-cache-key
  "Compatibility export for memory-sessions-cache-key."
  cache/memory-sessions-cache-key)

(def write-memory-sessions-cache-to-mongo!
  "Compatibility export for write-memory-sessions-cache-to-mongo!."
  cache/write-memory-sessions-cache-to-mongo!)

(def mongo-memory-sessions-hit!
  "Compatibility export for mongo-memory-sessions-hit!."
  cache/mongo-memory-sessions-hit!)

(def fetch-and-cache-memory-sessions!
  "Compatibility export for fetch-and-cache-memory-sessions!."
  cache/fetch-and-cache-memory-sessions!)

(def cached-memory-sessions-source!
  "Compatibility export for cached-memory-sessions-source!."
  cache/cached-memory-sessions-source!)

(def filter-page-actor-row!
  "Compatibility export for filter-page-actor-row!."
  pages/filter-page-actor-row!)

(def filter-page-actor-rows!
  "Compatibility export for filter-page-actor-rows!."
  pages/filter-page-actor-rows!)

(def fetch-contract-session-pages-from-mongo!
  "Compatibility export for fetch-contract-session-pages-from-mongo!."
  pages/fetch-contract-session-pages-from-mongo!)

(def fetch-authorized-session-pages!
  "Compatibility export for fetch-authorized-session-pages!."
  pages/fetch-authorized-session-pages!)

(def search-hit-session-visibility!
  "Compatibility export for search-hit-session-visibility!."
  pages/search-hit-session-visibility!)

(def filter-search-hits-by-actor!
  "Compatibility export for filter-search-hits-by-actor!."
  pages/filter-search-hits-by-actor!)

(def run-warm-title-cache!
  "Compatibility export for run-warm-title-cache!."
  view/run-warm-title-cache!)

(def enrich-row
  "Compatibility export for enrich-row."
  view/enrich-row)

(def memory-sessions-request-options
  "Compatibility export for memory-sessions-request-options."
  view/memory-sessions-request-options)

(def send-memory-session-rows!
  "Compatibility export for send-memory-session-rows!."
  view/send-memory-session-rows!)

(def send-memory-sessions-live-ids!
  "Compatibility export for send-memory-sessions-live-ids!."
  view/send-memory-sessions-live-ids!)

(def send-memory-sessions-result!
  "Compatibility export for send-memory-sessions-result!."
  view/send-memory-sessions-result!)

(defn- openplanner-ready?
  [config]
  (openplanner-client/enabled? (openplanner-client/client config)))

(routes/defroute ^{:doc "Register the memory-sessions-route! HTTP handler."}
  memory-sessions-route! [authorized-session-ids!
                                  fetch-openplanner-session-rows!
                                  session-matches-page-actor-filter?]
  "GET" "/api/memory/sessions"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (try
      (let [config (planner-scope/scoped-config config ctx)
            opts (view/memory-sessions-request-options config ctx request)
            env {:config config :ctx ctx
                 :runtime runtime
                 :reply reply
                 :json-response! json-response!
                 :error-response! error-response!}
            {:keys [value cache]} (await (view/fetch-memory-sessions-source! config ctx opts
                                                                         authorized-session-ids!
                                                                         fetch-openplanner-session-rows!
                                                                         session-matches-page-actor-filter?))]
        (await (view/send-memory-sessions-result!
                env
                (memory-shape/page-state value opts cache))))
      (catch :default err
        (error-response! reply err 502)
        nil))))

(routes/defroute ^{:doc "Register the memory-session-titles-status-route! HTTP handler."}
  memory-session-titles-status-route! []
  "GET" "/api/memory/session-titles/status"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (json-response! reply 200 {:ok true
                               :status @titles/session-title-backfill*
                               :cached_count (count @titles/session-titles*)})))

(routes/defroute ^{:doc "Register the memory-backfill-titles-route! HTTP handler."}
  memory-backfill-titles-route! [fetch-openplanner-session-rows!]
  "POST" "/api/memory/sessions/backfill-titles"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (try
      (let [body (or (aget request "body") (js/Object.))
            limit (or (parse/parse-positive-int (aget body "limit"))
                      (parse/parse-positive-int (aget request "query" "limit")))
            force? (or (parse/truthy-param? (aget body "force"))
                       (parse/truthy-param? (aget request "query" "force")))
            status (await (titles/start-session-title-backfill! runtime (planner-scope/scoped-config config ctx) {:force force? :limit limit} fetch-openplanner-session-rows!))]
        (json-response! reply 202 {:ok true
                                   :status status
                                   :cached_count (count @titles/session-titles*)}))
      (catch :default err
        (error-response! reply err 502)))))

(routes/defroute ^{:doc "Register the memory-import-titles-route! HTTP handler."}
  memory-import-titles-route! []
  "POST" "/api/memory/sessions/import-titles"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (let [body (js->clj (or (aget request "body") (js/Object.)))
          titles (or (get body "titles") {})
          updated (reduce-kv (fn [total session-id entry]
                               (let [session-id (str session-id)
                                     raw-title (if (map? entry) (or (get entry "title") (get entry :title)) entry)
                                     title-model (when (map? entry)
                                                   (or (get entry "title_model")
                                                       (get entry "title-model")
                                                       (get entry "model")
                                                       (:title_model entry)
                                                       (:title-model entry)
                                                       (:model entry)))
                                     normalized (titles/normalize-session-title raw-title)]
                                 (if (or (str/blank? session-id) (nil? normalized))
                                   total
                                   (do
                                     (titles/cache-session-title! runtime config session-id normalized (or title-model "retro:heuristic"))
                                     (inc total)))))
                             0
                             titles)]
      (json-response! reply 200 {:ok true
                                 :updated updated
                                 :cached_count (count @titles/session-titles*)}))))

(routes/defroute ^{:doc "Register the memory-session-by-id-route! HTTP handler."}
  memory-session-by-id-route! [fetch-openplanner-session-rows!]
  "GET" "/api/memory/sessions/:sessionId"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (try
      (let [config (planner-scope/scoped-config config ctx)
            session-id (or (aget request "params" "sessionId") "")
            requested-limit (parse/parse-positive-int (aget request "query" "limit"))
            preview-limit (when requested-limit
                            (:limit (expansion-policy/bounded-preview-params
                                     (policy-registry/get-policy)
                                     {:limit requested-limit})))]
        (if (str/blank? session-id)
          (json-response! reply 400 {:detail "sessionId is required"})
          (let [rows (await (fetch-openplanner-session-rows! config session-id))]
            (if (core-memory/session-visible? ctx rows)
              (json-response! reply 200 {:ok true
                                         :session session-id
                                         :rows (if preview-limit
                                                 (vec (take preview-limit rows))
                                                 rows)})
              (error-response! reply (http/http-error 403 "memory_scope_denied" "Session is outside the current Knoxx scope"))))))
      (catch :default err
        (error-response! reply err 502)))))

(defn- require-memory-read! [ctx]
  (authz/ensure-permission! ctx "agent.memory.read"))

(defn ^:async send-memory-search!
  "Memory session operation: send-memory-search!."
  [{:keys [config ctx reply json-response!]}
   fetch-openplanner-session-rows!
   session-matches-page-actor-filter?
   {:keys [query bounded-k session-id actor-id exclude-actor-ids]}]
  (let [config (planner-scope/scoped-config config ctx)
        result (await (planner-memory/openplanner-memory-search! config {:query query
                                                          :k bounded-k
                                                          :session-id session-id}))
        hits (await (core-memory/filter-authorized-memory-hits! config ctx (:hits result)))
        filtered-hits (await (pages/filter-search-hits-by-actor! config
                                                           fetch-openplanner-session-rows!
                                                           session-matches-page-actor-filter?
                                                           actor-id
                                                           exclude-actor-ids
                                                           hits))]
    (json-response! reply 200 (assoc result :ok true :hits filtered-hits))))

(defn- memory-search-request-options [request]
  (let [body (or (aget request "body") (js/Object.))
        {bounded-k :k} (expansion-policy/bounded-search-params
                        (policy-registry/get-policy)
                        {:k (aget body "k")})]
    {:query (or (aget body "query") "")
     :bounded-k bounded-k
     :session-id (or (aget body "sessionId") (aget body "session_id") "")
     :actor-id (memory-shape/normalized-actor-id (or (aget body "actorId")
                                                     (aget body "actor_id")
                                                     (aget body "actor")))
     :exclude-actor-ids (memory-shape/normalized-actor-ids
                         (or (aget body "excludeActorIds")
                             (aget body "exclude_actor_ids")
                             (aget body "excludeActorId")
                             (aget body "exclude_actor_id")))}))

(defn- ensure-memory-search-scope! [ctx session-id]
  (require-memory-read! ctx)
  (when (and (str/blank? (str session-id))
             (not (authz/ctx-permitted? ctx "agent.memory.cross_session"))
             (not (authz/system-admin? ctx)))
    (throw (http/http-error 403 "memory_scope_denied" "Cross-session memory search is outside the current Knoxx scope"))))

(routes/defroute ^{:doc "Register the memory-search-route! HTTP handler."}
  memory-search-route! [fetch-openplanner-session-rows!
                                session-matches-page-actor-filter?]
  "POST" "/api/memory/search"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (try
      (let [{:keys [session-id] :as opts} (memory-search-request-options request)]
        (ensure-memory-search-scope! ctx session-id)
        (await (send-memory-search!
                {:config config :ctx ctx :reply reply :json-response! json-response!}
                fetch-openplanner-session-rows!
                session-matches-page-actor-filter?
                opts)))
      (catch :default err
        (error-response! reply err 502)))))

(routes/defroute ^{:doc "Register the lounge-messages-list-route! HTTP handler."}
  lounge-messages-list-route! [lounge-messages*]
  "GET" "/api/lounge/messages"
  (json-response! reply 200 {:messages @lounge-messages*}))

(routes/defroute ^{:doc "Register the lounge-messages-create-route! HTTP handler."}
  lounge-messages-create-route! [lounge-messages*]
  "POST" "/api/lounge/messages"
  (let [body (or (aget request "body") (js/Object.))
        session-id (str (or (aget body "session_id") ""))
        alias (str/trim (str (or (aget body "alias") "anonymous")))
        text (str/trim (str (or (aget body "text") "")))]
    (cond
      (str/blank? session-id) (json-response! reply 400 {:detail "session_id is required"})
      (str/blank? text) (json-response! reply 400 {:detail "text is required"})
      :else (let [msg {:id (str (.randomUUID crypto))
                       :timestamp (time/now-iso)
                       :session_id session-id
                       :alias (if (str/blank? alias) "anonymous" alias)
                       :text text}]
              (swap! lounge-messages* #(->> (conj (vec %) msg) (take-last 100) vec))
              (realtime/broadcast-ws! "lounge" msg)
              (json-response! reply 200 {:ok true :message msg})))))

(defn register-memory-routes!
  "Memory session operation: register-memory-routes!."
  [app runtime config deps]
  (js/console.log "memory-sessions-route! ="
                  (.-name memory-sessions-route!))
  (memory-sessions-route! app runtime config deps)
  (memory-session-titles-status-route! app runtime config deps)
  (memory-backfill-titles-route! app runtime config deps)
  (memory-import-titles-route! app runtime config deps)
  (memory-session-by-id-route! app runtime config deps)
  (memory-search-route! app runtime config deps)
  (lounge-messages-list-route! app runtime config deps)
  (lounge-messages-create-route! app runtime config deps))
