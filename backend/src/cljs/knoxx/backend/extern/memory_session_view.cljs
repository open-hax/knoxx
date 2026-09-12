(ns knoxx.backend.extern.memory-session-view
  "Memory session native response enrichment, live rows and title warming."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.extern.memory-session-cache :as cache]
            [knoxx.backend.extern.memory-session-pages :as pages]
            [knoxx.backend.infra.auth.authz :refer [ctx-membership-id ctx-org-id ctx-permitted? ctx-user-id system-admin?]]
            [knoxx.backend.infra.core-memory :refer [fetch-openplanner-session-rows!]]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]
            [knoxx.backend.infra.stores.session-titles :refer [session-titles* session-title-promises* session-title-seed-text heuristic-session-title resolve-session-title! normalize-session-title cache-session-title!]]
            [knoxx.backend.shape.memory-sessions :as memory-shape]))

(defn ^:async run-warm-title-cache!
  "Memory session operation: run-warm-title-cache!."
  [session-id config runtime]
  (try
    (let [title-rows (await (fetch-openplanner-session-rows! config session-id))
          seed-text (session-title-seed-text title-rows)
          fallback-title (heuristic-session-title seed-text)]
      (try
        (let [entry (await (resolve-session-title! config seed-text))]
          (cache-session-title! runtime config session-id
                                (or (normalize-session-title (:title entry) fallback-title)
                                    fallback-title)
                                (:title_model entry)))
        (catch :default _
          (cache-session-title! runtime config session-id fallback-title nil))))
    (catch :default _
      (cache-session-title! runtime config session-id "Untitled session" nil))))

(defn- warm-title-cache! [session-id config runtime]
  (let [session-id (str (or session-id ""))]
    (when (and (not (str/blank? session-id))
               (not (contains? @session-titles* session-id))
               (not (contains? @session-title-promises* session-id)))
      (let [title-promise (run-warm-title-cache! session-id config runtime)]
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

(defn- active-session-actor-id
  [session agent-spec]
  (or (agent-spec-value agent-spec [:actor_id :actor-id :actorId])
      (some-> (or (:actor_id session) (:actor-id session) (:actorId session))
              str
              str/trim
              not-empty)))

(defn- active-session-event-types
  [agent-spec]
  (let [values (or (:event_types agent-spec)
                   (:event-types agent-spec)
                   (:eventTypes agent-spec))]
    (when (sequential? values)
      (->> values
           (map str)
           (remove str/blank?)
           distinct
           vec))))

(defn- active-session-synthetic-scope
  [session agent-spec]
  (let [actor-id (active-session-actor-id session agent-spec)
        event-types (active-session-event-types agent-spec)]
    (cond-> {}
      actor-id (assoc :actor_id actor-id)
      (agent-spec-value agent-spec [:contract_id :contract-id :contractId]) (assoc :contract_id (agent-spec-value agent-spec [:contract_id :contract-id :contractId]))
      (agent-spec-value agent-spec [:sub_agent_id :sub-agent-id :subAgentId]) (assoc :sub_agent_id (agent-spec-value agent-spec [:sub_agent_id :sub-agent-id :subAgentId]))
      (agent-spec-value agent-spec [:parent_agent_id :parent-agent-id :parentAgentId]) (assoc :parent_agent_id (agent-spec-value agent-spec [:parent_agent_id :parent-agent-id :parentAgentId]))
      (agent-spec-value agent-spec [:parent_run_id :parent-run-id :parentRunId]) (assoc :parent_run_id (agent-spec-value agent-spec [:parent_run_id :parent-run-id :parentRunId]))
      (agent-spec-value agent-spec [:spawn_kind :spawn-kind :spawnKind]) (assoc :spawn_kind (agent-spec-value agent-spec [:spawn_kind :spawn-kind :spawnKind]))
      (agent-spec-value agent-spec [:trigger_id :trigger-id :triggerId]) (assoc :trigger_id (agent-spec-value agent-spec [:trigger_id :trigger-id :triggerId]))
      (agent-spec-value agent-spec [:event_type :event-type :eventType]) (assoc :event_type (agent-spec-value agent-spec [:event_type :event-type :eventType]))
      (seq event-types) (assoc :event_types event-types)
      (agent-spec-value agent-spec [:event_id :event-id :eventId]) (assoc :event_id (agent-spec-value agent-spec [:event_id :event-id :eventId]))
      (agent-spec-value agent-spec [:event_scope_id :event-scope-id :eventScopeId]) (assoc :event_scope_id (agent-spec-value agent-spec [:event_scope_id :event-scope-id :eventScopeId]))
      (agent-spec-value agent-spec [:schedule_id :schedule-id :scheduleId]) (assoc :schedule_id (agent-spec-value agent-spec [:schedule_id :schedule-id :scheduleId])))))

(defn- active-session-synthetic-row
  [session]
  (let [agent-spec (:agent_spec session)]
    (merge {:session (:conversation_id session)
            :is_active true
            :active_status (:status session)
            :has_active_stream (boolean (:has_active_stream session))
            :title (str "Running · " (or (:run_id session) (:conversation_id session)))
            :event_count 0
            :last_ts (:updated_at session)}
           (active-session-synthetic-scope session agent-spec))))

(defn ^:async enrich-row
  "Memory session operation: enrich-row." [row]
  (let [session-id (str (:session row))
        titled-row (if-let [title-entry (get @session-titles* session-id)]
                     (assoc row
                            :title (:title title-entry)
                            :title_model (:title_model title-entry))
                     row)]
    (try
      (let [active-session-id (await (session-store/get-conversation-active-session session-id))]
        (if (str/blank? (str active-session-id))
          (inactive-row titled-row)
          (try
            (let [active-session (await (session-store/get-session active-session-id))
                  status (or (:status active-session) "inactive")
                  is-active (contains? #{"running" "waiting_input"} status)]
              (assoc titled-row
                     :active_session_id active-session-id
                     :is_active is-active
                     :active_status status
                      :has_active_stream (boolean (:has_active_stream active-session))))
              (catch :default _
                (inactive-row titled-row)))))
        (catch :default _
          (inactive-row titled-row)))))

(defn memory-sessions-request-options
  "Memory session operation: memory-sessions-request-options."
  [config ctx request]
  (let [{:keys [limit offset actor-id exclude-actor-ids contract-id] :as opts}
        (memory-shape/query-options (aget request "query"))]
    (assoc opts
           :cache-key (cache/memory-sessions-cache-key {:config config
                                                  :ctx ctx
                                                  :limit limit
                                                  :offset offset
                                                  :actor-id actor-id
                                                  :exclude-actor-ids exclude-actor-ids
                                                  :contract-id contract-id}))))

(defn fetch-memory-sessions-source!
  "Memory session operation: fetch-memory-sessions-source!."
  [config ctx opts authorized-session-ids! fetch-openplanner-session-rows! session-matches-page-actor-filter?]
  (cache/cached-memory-sessions-source!
   (:cache-key opts)
   (fn []
     (pages/fetch-authorized-session-pages! config ctx (:actor-id opts) (:exclude-actor-ids opts)
                                        (:contract-id opts) authorized-session-ids!
                                        fetch-openplanner-session-rows!
                                        session-matches-page-actor-filter?
                                        (:upstream-page-size opts) 0 [] (:needed-count opts)))))



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

(defn ^:async send-memory-session-rows!
  "Memory session operation: send-memory-session-rows!."
  [{:keys [config runtime error-response! reply] :as env} page-state rows]
  (warm-memory-session-title-rows! rows config runtime)
  (try
    (let [enriched-rows (await (.all js/Promise (clj->js (mapv enrich-row rows))))]
      (send-enriched-memory-sessions! env page-state enriched-rows))
    (catch :default err
      (error-response! reply err 502)
      nil)))

(defn- synthetic-active-session-rows
  [ctx live page-rows actor-id exclude-actor-ids contract-id]
  (let [op-ids (set (map #(str (:session %)) page-rows))]
    (->> live
         (filter #(and (= (ctx-org-id ctx) (:org_id %))
                       (or (system-admin? ctx) (ctx-permitted? ctx "agent.memory.cross_session")
                           (and (some? (ctx-user-id ctx)) (= (ctx-user-id ctx) (:user_id %)))
                           (and (some? (ctx-membership-id ctx)) (= (ctx-membership-id ctx) (:membership_id %))))
                       (:conversation_id %)
                       (not (op-ids (str (:conversation_id %))))
                       (contains? #{"running" "waiting_input"} (:status %))
                       (active-session-matches-actor-filter? % actor-id exclude-actor-ids)
                       (active-session-matches-contract? % contract-id)))
         (map active-session-synthetic-row)
         vec)))

(defn ^:async send-memory-sessions-live-ids!
  "Memory session operation: send-memory-sessions-live-ids!."
  [{:keys [ctx error-response! reply] :as env}
   {:keys [page-rows actor-id exclude-actor-ids contract-id] :as page-state}
   live-ids]
  (try
    (let [live-js (await (.all js/Promise
                               (clj->js (mapv #(session-store/get-session %)
                                               (vec live-ids)))))
          live (vec (js->clj live-js :keywordize-keys true))
          synthetic (synthetic-active-session-rows ctx live page-rows actor-id exclude-actor-ids contract-id)]
      (await (send-memory-session-rows! env page-state (vec (concat synthetic page-rows)))))
    (catch :default err
      (error-response! reply err 502)
      nil)))

(defn ^:async send-memory-sessions-result!
  "Memory session operation: send-memory-sessions-result!."
  [env {:keys [page-rows] :as page-state}]
  (try
    (let [live-ids (await (session-store/list-active-session-ids))]
      (if (seq live-ids)
        (await (send-memory-sessions-live-ids! env page-state live-ids))
        (await (send-memory-session-rows! env page-state page-rows))))
    (catch :default _
      (await (send-memory-session-rows! env page-state page-rows)))))

