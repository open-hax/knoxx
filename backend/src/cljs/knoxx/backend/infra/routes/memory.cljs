(ns knoxx.backend.infra.routes.memory
  (:require-macros [knoxx.backend.macros :refer [defroute]])
  (:require [clojure.string :as str]
            [knoxx.backend.infra.http :refer [json-response! error-response! http-error]]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.core-memory :as core-memory :refer [fetch-openplanner-session-rows!
                                               session-visible?
                                               session-matches-page-actor-filter?
                                               session-matches-contract-filter?
                                               session-summary-scope-from-rows
                                               filter-authorized-memory-hits!
                                               authorized-session-ids!]]
            [knoxx.backend.infra.openplanner.memory :refer [openplanner-memory-search!]]
            [knoxx.backend.domain.realtime :refer [broadcast-ws!]]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.infra.stores.session-store :as session-store]
            [knoxx.backend.infra.stores.session-titles :refer [session-titles*
                                                  session-title-promises*
                                                  session-title-backfill*
                                                  session-title-seed-text
                                                  heuristic-session-title
                                                  resolve-session-title!
                                                  normalize-session-title
                                                  cache-session-title!
                                                  start-session-title-backfill!]]
            [knoxx.backend.infra.auth.authz :refer [ctx-actor-id
                                         ctx-membership-id
                                         ctx-org-id
                                         ctx-permitted?
                                         ctx-user-id
                                         system-admin?
                                         ensure-permission!]]
            [knoxx.backend.shape.memory-sessions :as memory-shape]
            [knoxx.backend.shape.parse :refer [parse-positive-int truthy-param?]]
            [knoxx.backend.domain.time :refer [now-iso]]
            [shadow.cljs.modern :refer [js-await]]
            ["node:crypto" :as crypto]))

(defn interactive-session-id?
  [session-id]
  (not (str/starts-with? (str session-id) "translation-")))


(defn- openplanner-ready?
  [config]
  (openplanner-client/enabled? (openplanner-client/client config)))

(def memory-sessions-cache-ttl-seconds 10)
(def ^:private memory-sessions-cache-ttl-ms (* memory-sessions-cache-ttl-seconds 1000))
(def ^:private memory-sessions-cache-max-entries 256)
(defonce ^:private memory-sessions-cache* (atom {}))
(defonce ^:private memory-sessions-cache-promises* (atom {}))

(defn clear-memory-sessions-cache!
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
  [ctx]
  {:system-admin? (boolean (system-admin? ctx))
   :cross-session? (boolean (ctx-permitted? ctx "agent.memory.cross_session"))
   :org-id (str (or (ctx-org-id ctx) ""))
   :membership-id (str (or (ctx-membership-id ctx) ""))
   :user-id (str (or (ctx-user-id ctx) ""))
   :actor-id (str (or (ctx-actor-id ctx) ""))})

(defn memory-sessions-cache-key
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

(defn- memory-sessions-cache-redis-key
  [cache-key]
  (str "knoxx:memory:sessions:v1:" cache-key))

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

(defn- write-memory-sessions-cache!
  [redis-client cache-key value]
  (let [entry (memory-sessions-cache-entry value)]
    (remember-memory-sessions-cache! cache-key entry)
    (when redis-client
      (-> (redis/set-json redis-client (memory-sessions-cache-redis-key cache-key) entry memory-sessions-cache-ttl-seconds)
          (.catch (fn [_] nil))))
    entry))

(defn- redis-memory-sessions-hit!
  [redis-client cache-key]
  (if-not redis-client
    (js/Promise.resolve nil)
    (-> (redis/get-json redis-client (memory-sessions-cache-redis-key cache-key))
        (.then (fn [entry]
                 (let [entry (when (map? entry) entry)
                       ts (now-ms)]
                   (if (and entry (> (:expires-at entry 0) ts))
                     (do
                       (remember-memory-sessions-cache! cache-key entry)
                       {:value (:value entry)
                        :cache {:hit true
                                :tier "redis"
                                :stale false
                                :age_ms (max 0 (- ts (:cached-at entry ts)))}})
                     nil))))
        (.catch (fn [_] nil)))))

(defn cached-memory-sessions-source!
  [redis-client cache-key fetch-fn]
  (if-let [hit (memory-sessions-local-hit cache-key)]
    (js/Promise.resolve hit)
    (-> (redis-memory-sessions-hit! redis-client cache-key)
        (.then
         (fn [hit]
           (if hit
             hit
             (if-let [pending (get @memory-sessions-cache-promises* cache-key)]
               pending
               (let [promise (-> (fetch-fn)
                                 (.then (fn [value]
                                          (write-memory-sessions-cache! redis-client cache-key value)
                                          {:value value
                                           :cache {:hit false
                                                   :tier "miss"
                                                   :stale false
                                                   :age_ms 0}}))
                                 (.finally (fn []
                                             (swap! memory-sessions-cache-promises* dissoc cache-key))))]
                 (swap! memory-sessions-cache-promises* assoc cache-key promise)
                 promise))))))))


(defn- fetch-session-filter-rows!
  [fetch-openplanner-session-rows! config session-id]
  (if (identical? fetch-openplanner-session-rows! core-memory/fetch-openplanner-session-rows!)
    (core-memory/fetch-openplanner-session-visibility-rows! config session-id)
    (fetch-openplanner-session-rows! config session-id)))

(defn- filter-page-actor-rows!
  [config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids contract-id page-rows]
  (if (and (str/blank? (str (or actor-id "")))
           (empty? exclude-actor-ids)
           (str/blank? (str (or contract-id ""))))
    (js/Promise.resolve (vec page-rows))
    (-> (.all js/Promise
              (clj->js
               (mapv (fn [row]
                       (-> (fetch-session-filter-rows! fetch-openplanner-session-rows! config (:session row))
                           (.then (fn [rows]
                                    {:row (merge row (session-summary-scope-from-rows rows))
                                     :visible (and (session-matches-page-actor-filter? config rows actor-id exclude-actor-ids)
                                                   (session-matches-contract-filter? config rows contract-id))}))
                           (.catch (fn [_]
                                     {:row row
                                      :visible false}))))
                     page-rows)))
        (.then (fn [results]
                 (->> (js->clj results :keywordize-keys true)
                      (filter :visible)
                      (map :row)
                      vec))))))

(defn- grouped-session-rows
  [rows]
  (reduce (fn [state row]
            (let [session-id (str (:session row))]
              (if (or (str/blank? session-id)
                      (not (interactive-session-id? session-id)))
                state
                (-> state
                    (update :groups update session-id (fnil conj []) row)
                    (update :order (fn [order]
                                     (if (contains? (:seen state) session-id)
                                       order
                                       (conj order session-id))))
                    (update :seen conj session-id)))))
          {:groups {} :order [] :seen #{}}
          rows))

(defn- session-summary-title
  [session-id scope]
  (let [event-type (:event_type scope)
        trigger-id (:trigger_id scope)]
    (cond
      (and event-type trigger-id) (str "event: " event-type " trigger: " trigger-id)
      trigger-id (str "trigger: " trigger-id)
      event-type (str "event: " event-type)
      :else session-id)))

(defn- mongo-session-summary
  [session-id rows]
  (let [latest (first rows)
        scope (session-summary-scope-from-rows rows)]
    (merge {:session session-id
            :project (:project latest)
            :last_ts (:ts latest)
            :event_count (count rows)
            :title (session-summary-title session-id scope)}
           scope)))

(defn fetch-contract-session-pages-from-mongo!
  [config contract-id needed-count]
  (let [target (some-> contract-id str str/trim not-empty)
        query-limit 500]
    (if-not target
      (js/Promise.resolve nil)
      (-> (openplanner-client/mongo-query! (or (:openplanner-client config)
                                               (openplanner-client/client config))
                                           {:collection "events"
                                            :filter {"project" (:session-project-name config)
                                                     "session" {"$type" "string" "$ne" ""}
                                                     "extra.contract_id" target}
                                            :projection {"_id" 0
                                                         "project" 1
                                                         "session" 1
                                                         "ts" 1
                                                         "extra" 1}
                                            :sort {"ts" -1}
                                            :limit query-limit})
          (.then (fn [body]
                   (let [rows (vec (or (:rows body) []))
                         {:keys [groups order]} (grouped-session-rows rows)
                         summaries (mapv #(mongo-session-summary % (get groups %)) order)
                         wanted-count (max 1 (or needed-count 1))
                         selected (vec (take wanted-count summaries))
                         total-events (or (:total body) 0)
                         fetched-events (count rows)
                         more-events? (> total-events fetched-events)
                         more-sessions? (> (count summaries) wanted-count)]
                     {:rows selected
                      :has_more (or more-sessions? more-events?)})))))))

(defn fetch-authorized-session-pages!
  [config ctx actor-id exclude-actor-ids contract-id authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter? upstream-page-size upstream-offset acc needed-count]
  (-> (openplanner-client/sessions! (or (:openplanner-client config)
                                        (openplanner-client/client config))
                                    {:project (:session-project-name config)
                                     :limit upstream-page-size
                                     :offset upstream-offset})
      (.then
       (fn [body]
         (let [page-rows (vec (or (:rows body) []))
               fetched-count (count page-rows)
               next-offset (+ upstream-offset fetched-count)
               upstream-has-more (boolean (:has_more body))]
           (-> (authorized-session-ids! config ctx (map :session page-rows))
               (.then
                (fn [allowed]
                  (let [authorized-rows (->> page-rows
                                             (filter #(contains? allowed (str (:session %))))
                                             (filter #(interactive-session-id? (:session %)))
                                             vec)]
                    (-> (filter-page-actor-rows! config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids contract-id authorized-rows)
                        (.then
                         (fn [actor-visible-rows]
                           (let [next-acc (into acc actor-visible-rows)
                                 reached-target? (and (number? needed-count)
                                                      (>= (count next-acc) needed-count))]
                             (cond
                               reached-target?
                               (js/Promise.resolve {:rows (vec (take needed-count next-acc))
                                                    :has_more true})

                               (and upstream-has-more (pos? fetched-count))
                               (fetch-authorized-session-pages! config ctx actor-id exclude-actor-ids contract-id authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter? upstream-page-size next-offset next-acc needed-count)

                               :else
                               (js/Promise.resolve {:rows next-acc
                                                    :has_more false})))))))))))))))

(defn- hit-session-id
  [hit]
  (or (:session hit)
      (get-in hit [:metadata :session])
      (get-in hit [:extra :session])))

(defn- filter-search-hits-by-actor!
  [config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids hits]
  (let [actor-id (memory-shape/normalized-actor-id actor-id)
        exclude-actor-ids (memory-shape/normalized-actor-ids exclude-actor-ids)
        hits (vec hits)]
    (if (and (str/blank? (str (or actor-id "")))
             (empty? exclude-actor-ids))
      (js/Promise.resolve hits)
      (let [session-ids (->> hits
                             (keep hit-session-id)
                             (map str)
                             (remove str/blank?)
                             distinct
                             vec)]
        (-> (.all js/Promise
                  (clj->js
                   (mapv (fn [session-id]
                           (-> (fetch-session-filter-rows! fetch-openplanner-session-rows! config session-id)
                               (.then (fn [rows]
                                        {:session session-id
                                         :visible (session-matches-page-actor-filter? config rows actor-id exclude-actor-ids)}))
                               (.catch (fn [_]
                                         {:session session-id
                                          :visible false}))))
                         session-ids)))
            (.then (fn [results]
                     (let [allowed-sessions (->> (js->clj results :keywordize-keys true)
                                                 (filter :visible)
                                                 (map (comp str :session))
                                                 set)]
                       (->> hits
                            (filter (fn [hit]
                                      (contains? allowed-sessions (str (or (hit-session-id hit) "")))))
                            vec)))))))))

(defn- warm-title-cache! [session-id config runtime]
  (let [session-id (str (or session-id ""))]
    (when (and (not (str/blank? session-id))
               (not (contains? @session-titles* session-id))
               (not (contains? @session-title-promises* session-id)))
      (let [title-promise
            (-> (fetch-openplanner-session-rows! config session-id)
                (.then
                 (fn [title-rows]
                   (let [seed-text (session-title-seed-text title-rows)
                         fallback-title (heuristic-session-title seed-text)]
                     (-> (resolve-session-title! config seed-text)
                         (.then (fn [entry]
                                  (cache-session-title! runtime config session-id
                                                        (or (normalize-session-title (:title entry) fallback-title)
                                                            fallback-title)
                                                        (:title_model entry))))
                         (.catch (fn [_]
                                   (cache-session-title! runtime config session-id fallback-title nil)))))))
                (.catch (fn [_]
                          (cache-session-title! runtime config session-id "Untitled session" nil))))]
        (swap! session-title-promises* assoc session-id title-promise)
        title-promise))))

(defn- inactive-row [row]
  (assoc row
         :is_active false
         :active_status "inactive"
         :has_active_stream false))

(defn- agent-spec-value
  [agent-spec keys]
  (some (fn [k]
          (some-> (get agent-spec k)
                  str
                  str/trim
                  not-empty))
        keys))

(defn- active-session-actor-claims
  [session]
  (let [agent-spec (:agent_spec session)
        contract-actors (or (:contractActors agent-spec)
                            (:contract-actors agent-spec)
                            (:contract_actors agent-spec))]
    (actor-scope/normalize-actor-claims
     (concat [(or (:actor_id session)
                  (:actor-id session)
                  (:actorId session))
              (agent-spec-value agent-spec [:actor_id :actor-id :actorId])]
             (cond
               (set? contract-actors) contract-actors
               (sequential? contract-actors) contract-actors
               contract-actors [contract-actors]
               :else [])))))

(defn- active-session-matches-actor-filter?
  [session actor-id exclude-actor-ids]
  (let [include-actor-id (some-> actor-id str str/trim not-empty)
        exclude-actor-ids (->> (or exclude-actor-ids [])
                               (keep #(some-> % str str/trim not-empty))
                               distinct
                               vec)
        actors (active-session-actor-claims session)]
    (and (or (str/blank? (str (or include-actor-id "")))
             (actor-scope/actor-allowed? actors include-actor-id))
         (not-any? #(actor-scope/actor-allowed? actors %) exclude-actor-ids))))

(defn- actor-claim-includes?
  [actors actor-id]
  (let [wanted (actor-scope/normalize-actor-claim actor-id)]
    (and wanted
         (contains? (actor-scope/normalize-actor-claims actors) wanted))))

(defn- active-session-matches-contract?
  [session contract-id]
  (let [target (some-> contract-id str str/trim not-empty)
        agent-spec (:agent_spec session)]
    (or (nil? target)
        (= target (agent-spec-value agent-spec [:contract_id :contract-id :contractId]))
        (= target (agent-spec-value agent-spec [:sub_agent_id :sub-agent-id :subAgentId]))
        (= target (agent-spec-value agent-spec [:parent_agent_id :parent-agent-id :parentAgentId]))
        (= target (agent-spec-value agent-spec [:actor_id :actor-id :actorId]))
        (actor-claim-includes? (active-session-actor-claims session) target))))

(defn- active-session-synthetic-row
  [session]
  (let [agent-spec (:agent_spec session)
        contract-id (agent-spec-value agent-spec [:contract_id :contract-id :contractId])
        actor-id (or (agent-spec-value agent-spec [:actor_id :actor-id :actorId])
                     (some-> (or (:actor_id session)
                                 (:actor-id session)
                                 (:actorId session))
                             str
                             str/trim
                             not-empty))
        sub-agent-id (agent-spec-value agent-spec [:sub_agent_id :sub-agent-id :subAgentId])
        parent-agent-id (agent-spec-value agent-spec [:parent_agent_id :parent-agent-id :parentAgentId])
        parent-run-id (agent-spec-value agent-spec [:parent_run_id :parent-run-id :parentRunId])
        spawn-kind (agent-spec-value agent-spec [:spawn_kind :spawn-kind :spawnKind])
        trigger-id (agent-spec-value agent-spec [:trigger_id :trigger-id :triggerId])
        event-type (agent-spec-value agent-spec [:event_type :event-type :eventType])
        event-types (let [values (or (:event_types agent-spec)
                                     (:event-types agent-spec)
                                     (:eventTypes agent-spec))]
                      (when (sequential? values)
                        (->> values
                             (map str)
                             (remove str/blank?)
                             distinct
                             vec)))
        event-id (agent-spec-value agent-spec [:event_id :event-id :eventId])
        event-scope-id (agent-spec-value agent-spec [:event_scope_id :event-scope-id :eventScopeId])
        schedule-id (agent-spec-value agent-spec [:schedule_id :schedule-id :scheduleId])]
    (cond-> {:session (:conversation_id session)
             :is_active true
             :active_status (:status session)
             :has_active_stream (boolean (:has_active_stream session))
             :title (str "Running · " (or (:run_id session) (:conversation_id session)))
             :event_count 0
             :last_ts (:updated_at session)}
      actor-id (assoc :actor_id actor-id)
      contract-id (assoc :contract_id contract-id)
      sub-agent-id (assoc :sub_agent_id sub-agent-id)
      parent-agent-id (assoc :parent_agent_id parent-agent-id)
      parent-run-id (assoc :parent_run_id parent-run-id)
      spawn-kind (assoc :spawn_kind spawn-kind)
      trigger-id (assoc :trigger_id trigger-id)
      event-type (assoc :event_type event-type)
      (seq event-types) (assoc :event_types event-types)
      event-id (assoc :event_id event-id)
      event-scope-id (assoc :event_scope_id event-scope-id)
      schedule-id (assoc :schedule_id schedule-id))))

(defn- enrich-row  [redis-client row ]
  (let [session-id (str (:session row))
        titled-row (if-let [title-entry (get @session-titles* session-id)]
                     (assoc row
                            :title (:title title-entry)
                            :title_model (:title_model title-entry))
                     row)]
    (if-not redis-client
      (js/Promise.resolve (inactive-row titled-row))
      (-> (session-store/get-conversation-active-session redis-client session-id)
          (.then (fn [active-session-id]
                   (if (str/blank? (str active-session-id))
                     (inactive-row titled-row)
                     (-> (session-store/get-session redis-client active-session-id)
                         (.then (fn [active-session]
                                  (let [status (or (:status active-session) "inactive")
                                        is-active (contains? #{"running" "waiting_input"} status)]
                                    (assoc titled-row
                                           :active_session_id active-session-id
                                           :is_active is-active
                                           :active_status status
                                           :has_active_stream (boolean (:has_active_stream active-session))))))
                         (.catch (fn [_]
                                   (inactive-row titled-row)))))))
          (.catch (fn [_]
                    (inactive-row titled-row)))))))

(defn memory-sessions-request-options
  [config ctx request]
  (let [{:keys [limit offset actor-id exclude-actor-ids contract-id] :as opts}
        (memory-shape/query-options (aget request "query"))]
    (assoc opts
           :redis-client (redis/get-client)
           :cache-key (memory-sessions-cache-key {:config config
                                                  :ctx ctx
                                                  :limit limit
                                                  :offset offset
                                                  :actor-id actor-id
                                                  :exclude-actor-ids exclude-actor-ids
                                                  :contract-id contract-id}))))

(defn- contract-only-admin-fetch?
  [ctx {:keys [actor-id exclude-actor-ids contract-id]}]
  (and contract-id
       (str/blank? (str (or actor-id "")))
       (empty? exclude-actor-ids)
       (system-admin? ctx)))

(defn- fetch-memory-sessions-source!
  [config ctx opts authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter?]
  (cached-memory-sessions-source!
   (:redis-client opts)
   (:cache-key opts)
   (fn []
     (if (contract-only-admin-fetch? ctx opts)
       (fetch-contract-session-pages-from-mongo! config (:contract-id opts) (:needed-count opts))
       (fetch-authorized-session-pages! config ctx (:actor-id opts) (:exclude-actor-ids opts)
                                        (:contract-id opts) authorized-session-ids!
                                        fetch-openplanner-session-rows!
                                        session-matches-page-actor-filter?
                                        (:upstream-page-size opts) 0 [] (:needed-count opts))))))



(defn- warm-memory-session-title-rows!
  [rows config runtime]
  (doseq [row rows]
    (warm-title-cache! (str (:session row)) config runtime)))

(defn- send-enriched-memory-sessions!
  [{:keys [reply json-response!]} page-state enriched-rows]
  (json-response! reply 200
                  (memory-shape/response-payload
                   (assoc page-state
                          :rows (vec (js->clj enriched-rows :keywordize-keys true)))))
  nil)

(defn- send-memory-session-rows!
  [{:keys [redis-client config runtime error-response! reply] :as env} page-state rows]
  (warm-memory-session-title-rows! rows config runtime)
  (-> (.all js/Promise (clj->js (mapv (partial enrich-row redis-client) rows)))
      (.then (fn [enriched-rows]
               (send-enriched-memory-sessions! env page-state enriched-rows)))
      (.catch (fn [err]
                (error-response! reply err 502)
                nil))))

(defn- synthetic-active-session-rows
  [live page-rows actor-id exclude-actor-ids contract-id]
  (let [op-ids (set (map #(str (:session %)) page-rows))]
    (->> live
         (filter #(and (:conversation_id %)
                       (not (op-ids (str (:conversation_id %))))
                       (contains? #{"running" "waiting_input"} (:status %))
                       (active-session-matches-actor-filter? % actor-id exclude-actor-ids)
                       (active-session-matches-contract? % contract-id)))
         (map active-session-synthetic-row)
         vec)))

(defn- send-memory-sessions-live-ids!
  [{:keys [redis-client error-response! reply] :as env}
   {:keys [page-rows actor-id exclude-actor-ids contract-id] :as page-state}
   live-ids]
  (-> (.all js/Promise (clj->js (mapv #(session-store/get-session redis-client %) (vec live-ids))))
      (.then (fn [live-js]
               (let [live (vec (js->clj live-js :keywordize-keys true))
                     synthetic (synthetic-active-session-rows live page-rows actor-id exclude-actor-ids contract-id)]
                 (send-memory-session-rows! env page-state (vec (concat synthetic page-rows))))))
      (.catch (fn [err]
                (error-response! reply err 502)
                nil))))

(defn- send-memory-sessions-result!
  [{:keys [redis-client] :as env} {:keys [page-rows] :as page-state}]
  (if-not redis-client
    (send-memory-session-rows! env page-state page-rows)
    (-> (session-store/list-active-sessions redis-client)
        (.then (fn [live-ids]
                 (send-memory-sessions-live-ids! env page-state live-ids)))
        (.catch (fn [_]
                  (send-memory-session-rows! env page-state page-rows))))))

(defroute memory-sessions-route! [authorized-session-ids!
                                  fetch-openplanner-session-rows!
                                  session-matches-page-actor-filter?]
  "GET" "/api/memory/sessions"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (let [opts (memory-sessions-request-options config ctx request)
          env {:redis-client (:redis-client opts)
               :config config
               :runtime runtime
               :reply reply
               :json-response! json-response!
               :error-response! error-response!}]
      (-> (fetch-memory-sessions-source! config ctx opts
                                        authorized-session-ids!
                                        fetch-openplanner-session-rows!
                                        session-matches-page-actor-filter?)
          (.then (fn [{:keys [value cache]}]
                   (send-memory-sessions-result!
                    env
                    (memory-shape/page-state value opts cache))))
          (.catch (fn [err]
                    (error-response! reply err 502)
                    nil))))))

(defroute memory-session-titles-status-route! []
  "GET" "/api/memory/session-titles/status"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (json-response! reply 200 {:ok true
                               :status @session-title-backfill*
                               :cached_count (count @session-titles*)})))

(defroute memory-backfill-titles-route! [fetch-openplanner-session-rows!]
  "POST" "/api/memory/sessions/backfill-titles"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (let [body (or (aget request "body") (js/Object.))
          limit (or (parse-positive-int (aget body "limit"))
                    (parse-positive-int (aget request "query" "limit")))
          force? (or (truthy-param? (aget body "force"))
                     (truthy-param? (aget request "query" "force")))]
      (-> (start-session-title-backfill! runtime config {:force force? :limit limit} fetch-openplanner-session-rows!)
          (.then (fn [status]
                   (json-response! reply 202 {:ok true
                                              :status status
                                              :cached_count (count @session-titles*)})))
          (.catch (fn [err]
                    (error-response! reply err 502)))))))

(defroute memory-import-titles-route! []
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
                                     normalized (normalize-session-title raw-title)]
                                 (if (or (str/blank? session-id) (nil? normalized))
                                   total
                                   (do
                                     (cache-session-title! runtime config session-id normalized (or title-model "retro:heuristic"))
                                     (inc total)))))
                             0
                             titles)]
      (json-response! reply 200 {:ok true
                                 :updated updated
                                 :cached_count (count @session-titles*)}))))

(defroute memory-session-by-id-route! [fetch-openplanner-session-rows!]
  "GET" "/api/memory/sessions/:sessionId"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (let [session-id (or (aget request "params" "sessionId") "")]
      (if (str/blank? session-id)
        (json-response! reply 400 {:detail "sessionId is required"})
        (-> (fetch-openplanner-session-rows! config session-id)
            (.then (fn [rows]
                     (if (session-visible? ctx rows)
                       (json-response! reply 200 {:ok true
                                                  :session session-id
                                                  :rows rows})
                       (error-response! reply (http-error 403 "memory_scope_denied" "Session is outside the current Knoxx scope")))))
            (.catch (fn [err]
                      (error-response! reply err 502))))))))

(defroute memory-search-route! [fetch-openplanner-session-rows!
                                session-matches-page-actor-filter?]
  "POST" "/api/memory/search"
  (if-not (openplanner-ready? config)
    (json-response! reply 503 {:detail "OpenPlanner is not configured"})
    (let [body (or (aget request "body") (js/Object.))
          query (or (aget body "query") "")
          k (aget body "k")
          actor-id (memory-shape/normalized-actor-id (or (aget body "actorId")
                                                         (aget body "actor_id")
                                                         (aget body "actor")))
          exclude-actor-ids (memory-shape/normalized-actor-ids
                             (or (aget body "excludeActorIds")
                                 (aget body "exclude_actor_ids")
                                 (aget body "excludeActorId")
                                 (aget body "exclude_actor_id")))
          session-id (or (aget body "sessionId") (aget body "session_id") "")]
      (ensure-permission! ctx "agent.memory.read")
      (when (and (str/blank? (str session-id))
                 (not (ctx-permitted? ctx "agent.memory.cross_session"))
                 (not (system-admin? ctx)))
        (throw (http-error 403 "memory_scope_denied" "Cross-session memory search is outside the current Knoxx scope")))
      (-> (openplanner-memory-search! config {:query query
                                              :k k
                                              :session-id session-id})
          (.then (fn [result]
                   (-> (filter-authorized-memory-hits! config ctx (:hits result))
                       (.then (fn [hits]
                                (-> (filter-search-hits-by-actor! config fetch-openplanner-session-rows! session-matches-page-actor-filter? actor-id exclude-actor-ids hits)
                                    (.then (fn [filtered-hits]
                                             (json-response! reply 200 (assoc result :ok true :hits filtered-hits))))))))))
          (.catch (fn [err]
                    (error-response! reply err 502)))))))

(defroute lounge-messages-list-route! [lounge-messages*]
  "GET" "/api/lounge/messages"
  (json-response! reply 200 {:messages @lounge-messages*}))

(defroute lounge-messages-create-route! [lounge-messages*]
  "POST" "/api/lounge/messages"
  (let [body (or (aget request "body") (js/Object.))
        session-id (str (or (aget body "session_id") ""))
        alias (str/trim (str (or (aget body "alias") "anonymous")))
        text (str/trim (str (or (aget body "text") "")))]
    (cond
      (str/blank? session-id) (json-response! reply 400 {:detail "session_id is required"})
      (str/blank? text) (json-response! reply 400 {:detail "text is required"})
      :else (let [msg {:id (str (.randomUUID crypto))
                       :timestamp (now-iso)
                       :session_id session-id
                       :alias (if (str/blank? alias) "anonymous" alias)
                       :text text}]
              (swap! lounge-messages* #(->> (conj (vec %) msg) (take-last 100) vec))
              (broadcast-ws! "lounge" msg)
              (json-response! reply 200 {:ok true :message msg})))))

(defn register-memory-routes!
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
