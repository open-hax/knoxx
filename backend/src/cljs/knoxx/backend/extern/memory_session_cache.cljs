(ns knoxx.backend.extern.memory-session-cache
  "Session-page cache native clock, canonical hash and persistence boundary."
  (:require ["node:crypto" :as crypto]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.stores.mongo-memory-sessions :as mongo-memory-sessions]))

(def memory-sessions-cache-ttl-seconds
  "Memory session operation: memory-sessions-cache-ttl-seconds." 10)
(def ^:private memory-sessions-cache-ttl-ms (* memory-sessions-cache-ttl-seconds 1000))
(def ^:private memory-sessions-cache-max-entries 256)
(defonce ^:private memory-sessions-cache* (atom {}))
(defonce ^:private memory-sessions-cache-promises* (atom {}))

(defn clear-memory-sessions-cache!
  "Memory session operation: clear-memory-sessions-cache!."
  []
  (reset! memory-sessions-cache* {})
  (reset! memory-sessions-cache-promises* {})
  true)

(defn- now-ms [] (.now js/Date))

(defn- sha256
  [value]
  (-> (.createHash crypto "sha256")
      (.update (str value))
      (.digest "hex")))

(defn- stable-json
  [value]
  (.stringify js/JSON (clj->js value)))

(defn memory-sessions-auth-scope
  "Memory session operation: memory-sessions-auth-scope."
  [ctx]
  {:system-admin? (boolean (authz/system-admin? ctx))
   :cross-session? (boolean (authz/ctx-permitted? ctx "agent.memory.cross_session"))
   :org-id (str (or (authz/ctx-org-id ctx) ""))
   :membership-id (str (or (authz/ctx-membership-id ctx) ""))
   :user-id (str (or (authz/ctx-user-id ctx) ""))
   :actor-id (str (or (authz/ctx-actor-id ctx) ""))})

(defn memory-sessions-cache-key
  "Memory session operation: memory-sessions-cache-key."
  [{:keys [config ctx limit offset actor-id exclude-actor-ids contract-id]}]
  (sha256
   (stable-json
    {:v 1
     :project (str (or (:session-project-name config) ""))
     :limit (max 1 (or limit 12))
     :offset (max 0 (or offset 0))
     :actor-id (str (or actor-id ""))
     :exclude-actor-ids (sort (map str (or exclude-actor-ids [])))
     :contract-id (str (or contract-id ""))
     :auth (memory-sessions-auth-scope ctx)})))

(defn- evict-memory-sessions-cache!
  []
  (let [ts (now-ms)]
    (swap! memory-sessions-cache*
           (fn [entries]
             (let [live (->> entries
                             (filter (fn [[_ entry]]
                                       (> (:expires-at entry 0) ts)))
                             (sort-by (fn [[_ entry]] (:cached-at entry 0)))
                             (take-last memory-sessions-cache-max-entries))]
               (into {} live))))))

(defn- memory-sessions-cache-entry
  [value]
  (let [ts (now-ms)]
    {:value value
     :cached-at ts
     :expires-at (+ ts memory-sessions-cache-ttl-ms)}))

(defn- memory-sessions-local-hit
  [cache-key]
  (when-let [entry (get @memory-sessions-cache* cache-key)]
    (let [ts (now-ms)]
      (if (> (:expires-at entry 0) ts)
        {:value (:value entry)
         :cache {:hit true
                 :tier "memory"
                 :stale false
                 :age_ms (max 0 (- ts (:cached-at entry ts)))}}
        (do
          (swap! memory-sessions-cache* dissoc cache-key)
          nil)))))

(defn- remember-memory-sessions-cache!
  [cache-key entry]
  (swap! memory-sessions-cache* assoc cache-key entry)
  (evict-memory-sessions-cache!)
  entry)

(defn ^:async write-memory-sessions-cache-to-mongo!
  "Memory session operation: write-memory-sessions-cache-to-mongo!."
  [cache-key entry]
  (try
    (await (mongo-memory-sessions/set-cache-entry! cache-key entry))
    (catch :default _
      nil)))

(defn- write-memory-sessions-cache!
  [cache-key value]
  (let [entry (memory-sessions-cache-entry value)]
    (remember-memory-sessions-cache! cache-key entry)
    (write-memory-sessions-cache-to-mongo! cache-key entry)
    entry))

(defn ^:async mongo-memory-sessions-hit!
  "Memory session operation: mongo-memory-sessions-hit!."
  [cache-key]
  (try
    (let [entry (await (mongo-memory-sessions/get-cache-entry! cache-key))
          ts (now-ms)]
      (when (and entry (> (:expires-at entry 0) ts))
        (remember-memory-sessions-cache! cache-key entry)
        {:value (:value entry)
         :cache {:hit true
                 :tier "mongo"
                 :stale false
                 :age_ms (max 0 (- ts (:cached-at entry ts)))}}))
    (catch :default _
      nil)))

(defn ^:async fetch-and-cache-memory-sessions!
  "Memory session operation: fetch-and-cache-memory-sessions!."
  [cache-key fetch-fn]
  (try
    (let [value (await (fetch-fn))]
      (write-memory-sessions-cache! cache-key value)
      {:value value
       :cache {:hit false
               :tier "miss"
               :stale false
               :age_ms 0}})
    (finally
      (swap! memory-sessions-cache-promises* dissoc cache-key))))

(defn ^:async cached-memory-sessions-source!
  "Memory session operation: cached-memory-sessions-source!."
  [cache-key fetch-fn]
  (if-let [hit (memory-sessions-local-hit cache-key)]
    hit
    (if-let [hit (await (mongo-memory-sessions-hit! cache-key))]
      hit
      (if-let [pending (get @memory-sessions-cache-promises* cache-key)]
        (await pending)
        (let [promise (fetch-and-cache-memory-sessions! cache-key fetch-fn)]
          (swap! memory-sessions-cache-promises* assoc cache-key promise)
          (await promise))))))


