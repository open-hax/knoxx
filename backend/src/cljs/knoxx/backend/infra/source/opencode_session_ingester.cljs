(ns knoxx.backend.infra.source.opencode-session-ingester
  "Small OpenCode session API client used by Knoxx admin routes.

   The ingestion service owns durable ingestion. This namespace provides the
   lightweight status/list surface analogous to eta-mu-session-ingester.cljs."
  (:require [knoxx.backend.infra.clients.opencode :as opencode-client]))

(defn- opencode-client
  []
  (opencode-client/client {}))

(defn- opencode-server-url
  []
  (opencode-client/server-url {}))

(defn ^:async get-opencode-ingest-status
  []
  (try
    (let [parts (await (js/Promise.all
                        #js [(opencode-client/health! (opencode-client))
                             (opencode-client/sessions! (opencode-client) {:limit 20 :archived true})]))
          health (:body (aget parts 0))
          sessions (:body (aget parts 1))]
      #js {:ok true
           :opencodeServerUrl (opencode-server-url)
           :health (clj->js health)
           :recentSessionCount (count (or sessions []))
           :recentSessions (clj->js sessions)})
    (catch :default err
      #js {:ok false
           :opencodeServerUrl (opencode-server-url)
           :error (.-message err)})))

(defn ^:async list-opencode-sessions
  [{:keys [limit cursor directory search roots archived]
    :or {limit 50 archived true}}]
  (let [limit (min (or limit 50) 200)
        resp (await (opencode-client/sessions!
                     (opencode-client)
                     (cond-> {:limit limit
                              :archived archived}
                       cursor (assoc :cursor cursor)
                       directory (assoc :directory directory)
                       search (assoc :search search)
                       (some? roots) (assoc :roots roots))))]
    #js {:ok true
         :opencodeServerUrl (opencode-server-url)
         :sessions (clj->js (:body resp))
         :nextCursor (:nextCursor resp)
         :has_more (boolean (:nextCursor resp))}))

(defn get-opencode-session-messages
  [session-id]
  (opencode-client/session-messages! (opencode-client) session-id))
