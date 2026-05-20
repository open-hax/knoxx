(ns knoxx.backend.infra.routes.app
  (:require-macros [knoxx.backend.macros :refer [defroute]])
  (:require [clojure.string :as str]
            [knoxx.backend.infra.routes.admin :as admin-routes]
            [knoxx.backend.infra.routes.actors :as actor-routes]
            [knoxx.backend.infra.agent.hydration :refer [ensure-settings! settings-state*]]
            [knoxx.backend.infra.agent.runtime :refer [resolve-workspace-path queue-agent-control!]]
            [knoxx.backend.infra.agent.session :refer [active-agent-session]]
            [knoxx.backend.shape.agent :refer [streaming? current-turn]]
            [knoxx.backend.infra.agent.policy :refer [validate-chat-policy!]]
            [knoxx.backend.infra.agent.recovery :refer [resume-recovered-session!]]
            [knoxx.backend.infra.agent.turn :refer [send-agent-turn! ensure-conversation-access! ensure-session-id]]
            [knoxx.backend.shape.app-shapes :refer [normalize-chat-body normalize-control-body route!]]
            [knoxx.backend.infra.auth.authz :refer [policy-db policy-db-enabled? policy-db-promise with-request-context! ensure-permission! ensure-tool! ensure-any-permission! ensure-org-scope! primary-context-role ctx-permitted? system-admin? ctx-user-id ctx-user-email ctx-org-id run-visible?]]
            [knoxx.backend.infra.core-memory :refer [fetch-openplanner-session-rows! session-visible? session-matches-page-actor-filter? filter-authorized-memory-hits! authorized-session-ids!]]
            [knoxx.backend.infra.routes.contracts :as contracts-routes]
            [knoxx.backend.domain.contracts.sources :as contract-sources]
            [knoxx.backend.infra.document-state :refer [normalize-relative-path]]
            [knoxx.backend.infra.routes.documents :as document-routes]
            [knoxx.backend.law.guards :as guards]
            [knoxx.backend.infra.http :refer [forward-knoxx-request! json-response! rewrite-localhost-url with-query-param bearer-headers fetch-json openplanner-enabled? openplanner-request! openplanner-url openplanner-headers openai-auth-error send-fetch-response! request-query-string http-error error-response! js-array-seq]]
            [knoxx.backend.infra.routes.memory :as memory-routes]
            [knoxx.backend.infra.routes.models :as model-routes]
            [knoxx.backend.infra.openplanner.memory :refer [openplanner-memory-search! openplanner-graph-export!]]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.domain.realtime :refer [broadcast-ws!]]
            [knoxx.backend.domain.action.run-state :as run-state :refer [runs* run-order*]]
            [knoxx.backend.shape.parse :refer [parse-positive-int truthy-param?]]
            [knoxx.backend.domain.time :refer [now-iso]]
            [knoxx.backend.infra.stores.session-store :as session-store]
            [knoxx.backend.infra.stores.session-titles :refer [start-session-title-backfill! session-title-backfill* session-titles* get-cached-session-title! session-title-seed-text heuristic-session-title stored-session-title-entry cache-session-title-entry! resolve-session-title! cache-session-title! normalize-session-title]]
            [knoxx.backend.domain.text :refer [count-occurrences replace-first clip-text]]
            [knoxx.backend.infra.routes.tools :as tool-routes]
            [knoxx.backend.infra.tooling :refer [tool-catalog ensure-role-can-use! email-enabled? effective-agent-contract agent-contract-catalog actor-catalog default-agent-contract-id default-actor-id]]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.infra.routes.voice :as voice-routes]
            [knoxx.backend.infra.routes.workspace-media :as workspace-media-routes]
            [knoxx.backend.infra.routes.studio :as studio-routes]
            [knoxx.backend.infra.routes.translation :as translation-routes]
            [shadow.cljs.modern :refer (js-await)]
            ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]
            ))


(defn- queue-chat-start!
  [runtime config reply agent-ctx policy-model body accepted-response]
  (js-await [validated (validate-chat-policy! agent-ctx policy-model)]
            (js-await [sent (send-agent-turn! runtime config body)]
                      (json-response! reply 202 accepted-response))))


(defn- compact-agent-spec-overrides
  [agent-spec]
  (into {}
        (remove (fn [[_ value]]
                  (or (nil? value)
                      (and (string? value) (str/blank? value))
                      (and (sequential? value) (empty? value)))))
        agent-spec))

(defn- merged-agent-spec
  [config parsed]
  (let [requested (compact-agent-spec-overrides (or (:agent-spec parsed) {}))
        requested-actor-id (or (get requested :actor-id)
                               (default-actor-id config))
        requested-contract-id (or (get requested :contract-id)
                                  (default-agent-contract-id config requested-actor-id))
        resolved (effective-agent-contract config requested-contract-id requested-actor-id)
        resolved-id (:id resolved)
        merged (merge (select-keys resolved [:role :model :system-prompt :task-prompt :thinking-level :tool-policies :contract-actor-ids :memory-hydration :context-policy :sources])
                      requested)
        runtime-sources (contract-sources/compose-source-refs config
                                                              (:sources resolved)
                                                              (:sources requested))]
    (cond-> (assoc merged :sources runtime-sources)
      requested-actor-id (assoc :actor-id requested-actor-id)
      (seq (:contract-actor-ids resolved)) (assoc :contract-actors (:contract-actor-ids resolved))
      resolved-id (assoc :contract-id resolved-id))))

(defn- requested-role
  [parsed]
  (or (get-in parsed [:agent-spec :role])
      (some-> (:auth-context parsed) :role str str/trim not-empty)
      (some->> (get-in parsed [:auth-context :roleSlugs]) seq first str str/trim not-empty)))

(defn- allow-policy?
  [policy]
  (= "allow" (some-> (:effect policy) str str/lower-case)))

(defn- requested-tool-policies
  [parsed]
  (let [from-spec (vec (or (get-in parsed [:agent-spec :tool-policies]) []))
        from-auth (vec (or (get-in parsed [:auth-context :toolPolicies]) []))]
    (cond
      (seq from-spec) from-spec
      (seq from-auth) from-auth
      :else [])))

(defn- effective-tool-policies
  [ctx parsed]
  (let [requested (requested-tool-policies parsed)]
    (cond
      (and (nil? ctx) (seq requested)) requested
      (and (nil? ctx) (:auth-context parsed)) (vec (or (get-in parsed [:auth-context :toolPolicies]) []))
      (empty? requested) (vec (or (:toolPolicies ctx) []))
      (system-admin? ctx) requested
      :else (let [allowed (->> (:toolPolicies ctx)
                               (filter allow-policy?)
                               (map :toolId)
                               set)]
              (->> requested
                   (filter #(contains? allowed (:toolId %)))
                   vec)))))

(defn- effective-auth-context
  [ctx parsed]
  (let [base (or ctx (:auth-context parsed))
        requested-actor-id (some-> (get-in parsed [:agent-spec :actor-id]) str str/trim not-empty)
        requested-role-slug (requested-role parsed)
        role-slugs (cond
                     (and (nil? base) requested-role-slug) [requested-role-slug]
                     (and requested-role-slug (or (system-admin? ctx)
                                                  (contains? (into #{} (or (:roleSlugs base) [])) requested-role-slug)))
                     [requested-role-slug]
                     :else (vec (or (:roleSlugs base) [])))
        tool-policies (effective-tool-policies ctx parsed)
        resource-policies (or (get-in parsed [:agent-spec :resource-policies])
                              (get-in parsed [:auth-context :resourcePolicies])
                              (:resourcePolicies base))]
    (when (or base requested-actor-id requested-role-slug (seq tool-policies) resource-policies)
      (cond-> (or base {})
        requested-actor-id (assoc :actorId requested-actor-id)
        (seq role-slugs) (assoc :roleSlugs role-slugs)
        (or (seq tool-policies) (some? base)) (assoc :toolPolicies tool-policies)
        resource-policies (assoc :resourcePolicies resource-policies)))))

(defn- auth-context-with-actor
  [ctx actor-id]
  (if-let [actor-id* (some-> actor-id str str/trim not-empty)]
    (assoc (or ctx {}) :actorId actor-id*)
    ctx))

(defn- active-run-summary
  [run session]
  (let [messages   (vec (or (:request_messages run) []))
        user-msg   (some #(when (= "user" (some-> (:role %) str/lower-case)) %) (reverse messages))
        conversation-id (:conversation_id run)]
    {:run_id (:run_id run)
     :session_id (:session_id run)
     :conversation_id (:conversation_id run)
     :status (:status run)
     :model (:model run)
     :created_at (:created_at run)
     :updated_at (:updated_at run)
     :ttft_ms (:ttft_ms run)
     :total_time_ms (:total_time_ms run)
     :input_tokens (:input_tokens run)
     :output_tokens (:output_tokens run)
     :tokens_per_s (:tokens_per_s run)
     :error (:error run)
     :event_count (count (or (:events run) []))
     :tool_receipt_count (count (or (:tool_receipts run) []))
     :has_active_stream (boolean (:has_active_stream session))
     :active_turn_registered (boolean (and conversation-id (turn-control/active-turn conversation-id)))
     :agent_spec (get-in run [:settings :agentSpec])
     :resource_policies (get-in run [:resources :agentResourcePolicies])
     :latest_user_message (:content user-msg)
     :content_parts (mapv (fn [p]
                            (-> p
                                (dissoc :data)
                                (select-keys [:type :url :mimeType :filename :text])))
                          (or (:content-parts user-msg) []))
     :tool_receipts (mapv (fn [r]
                            (select-keys r [:id :tool_name :status
                                            :input_preview :result_preview
                                            :started_at :ended_at]))
                          (or (:tool_receipts run) []))
     :trace_blocks (mapv (fn [b]
                           (select-keys b [:id :kind :status :toolName
                                           :toolCallId :content :at
                                           :inputPreview :outputPreview :isError]))
                         (or (:trace_blocks run) []))
     :latest_event (some-> (:events run) last (select-keys [:type :status :tool_name :preview :at]))}))

(defn- active-session-summary
  [session]
  (let [messages (vec (or (:messages session) []))
        user-msg (some #(when (= "user" (some-> (:role %) str/lower-case)) %) (reverse messages))
        conversation-id (:conversation_id session)]
    {:run_id (:run_id session)
     :session_id (:session_id session)
     :conversation_id conversation-id
     :status (:status session)
     :model (:model session)
     :created_at (:created_at session)
     :updated_at (:updated_at session)
     :error (:error session)
     :event_count 0
     :tool_receipt_count 0
     :has_active_stream (boolean (:has_active_stream session))
     :active_turn_registered (boolean (and conversation-id (turn-control/active-turn conversation-id)))
     :agent_spec (or (:agent_spec session) (:agentSpec session))
     :resource_policies (:resource_policies session)
     :latest_user_message (:content user-msg)
     :latest_event nil}))

(defn- live-active-agent-summaries!
  [limit include-all?]
  (let [limit (max 1 (or limit 25))
        redis-client (redis/get-client)
        sessions-by-id (into {}
                             (map (fn [session]
                                    [(:session_id session) session]))
                             (session-store/active-session-snapshots))
        run-items (->> @run-order*
                       (map #(get @runs* %))
                       (filter some?)
                       (filter #(contains? #{"queued" "running" "waiting_input"} (:status %)))
                       (map (fn [run]
                              (active-run-summary run (get sessions-by-id (:session_id run)))))
                       vec)]
    (if-not redis-client
      (js/Promise.resolve (take limit run-items))
      (-> (session-store/list-active-sessions redis-client)
          (.then
           (fn [ids]
             (-> (.all js/Promise (clj->js (mapv #(session-store/get-session redis-client %) (vec ids))))
                 (.then
                  (fn [sessions-js]
                    (let [run-session-ids (set (keep :session_id run-items))
                          session-items (->> (vec (js->clj sessions-js :keywordize-keys true))
                                             (filter some?)
                                             (filter #(or include-all?
                                                          (contains? #{"queued" "running" "waiting_input"} (:status %))))
                                             (remove #(contains? run-session-ids (:session_id %)))
                                             (map active-session-summary)
                                             vec)]
                      (->> (concat run-items session-items)
                           (sort-by #(or (:updated_at %) (:created_at %) "") #(compare %2 %1))
                           (take limit)
                           vec)))))))))))

(def SESSION_RECOVERY_STALE_MS 60000)

(defn- clear-ghost-turn!
  "If turn-control has an entry for conversation-id but the underlying Proxx
   session shows no active streaming or current turn, the entry is a ghost
   from a previous hung run. Unregister it so zombie recovery can proceed."
  [conversation-id]
  (let [agent-session (active-agent-session conversation-id)
        active-streaming? (and agent-session (streaming? agent-session))
        active-turn? (and agent-session
                          (try
                            (some? (current-turn agent-session))
                            (catch js/Error _ false)))]
    (when (and (not active-streaming?) (not active-turn?))
      (turn-control/unregister-active-turn! conversation-id))))

(defn- runtime-processing-session?
  [conversation-id]
  (let [agent-session (active-agent-session conversation-id)
        active-streaming? (and agent-session (streaming? agent-session))
        active-turn? (and agent-session
                          (try
                            (some? (current-turn agent-session))
                            (catch js/Error _ false)))
        registered-turn? (some? (turn-control/active-turn conversation-id))]
    (or active-streaming? active-turn? registered-turn?)))

(defn- parse-iso-ms
  [value]
  (let [parsed (js/Date.parse (str (or value "")))]
    (when-not (js/isNaN parsed)
      parsed)))

(defn- latest-run-event!
  [run-id]
  (let [run-id (str (or run-id ""))]
    (cond
      (str/blank? run-id)
      (js/Promise.resolve nil)

      (seq (get-in @runs* [run-id :events]))
      (js/Promise.resolve (last (get-in @runs* [run-id :events])))

      (nil? (redis/get-client))
      (js/Promise.resolve nil)

      :else
      (-> (redis/lrange-json (redis/get-client) (run-state/run-events-key run-id) 0 0)
          (.then first)
          (.catch (constantly nil))))))

(defn- stale-running-session?
  [session latest-event]
  (let [stamp (or (:at latest-event)
                  (:updated_at session)
                  (:created_at session))
        stamp-ms (parse-iso-ms stamp)]
    (or (nil? stamp-ms)
        (> (- (.now js/Date) stamp-ms) SESSION_RECOVERY_STALE_MS))))

(defn- dev-hmr-response
  []
  {:ok true
   :version "v2"
   :at (now-iso)})

;; ── Common promise handlers ───────────────────────────────────────────

(defn- proxy-ok [reply resp]
  (send-fetch-response! reply resp))

(defn- proxy-err [reply prefix err]
  (json-response! reply 502 {:detail (str prefix err)}))

(defn- fetch-json-ok [reply resp]
  (json-response! reply (or (aget resp "status") 200) (aget resp "body")))

(defn- fetch-json-err [reply err]
  (json-response! reply 502 {:error (.-message err)}))

(defn- fetch-json-err-detail [reply prefix err]
  (json-response! reply 502 {:detail (str prefix err)}))

(defn- pg-query-ok [reply result]
  (let [rows (js->clj (aget result "rows") :keywordize-keys true)]
    (json-response! reply 200 {:ok true :rows rows :count (count rows)})))

(defn- pg-query-table-ok [reply table result]
  (let [rows (js->clj (aget result "rows") :keywordize-keys true)]
    (json-response! reply 200 {:ok true :table table :rows rows :count (count rows)})))

(defn- pg-query-err [reply err]
  (json-response! reply 400 {:error (.-message err)}))

(defn- zombie-recovered [queue-turn! _]
  (queue-turn! "Async direct agent chat failed (recovered from zombie)"))

(defn- zombie-recovery-failed [reply err]
  (.error js/console "Failed to abort zombie session" err)
  (json-response! reply 409 {:ok false
                             :error (str "Agent is already processing. Zombie recovery failed: " err)
                             :code "agent_already_processing"
                             :has_active_stream false
                             :can_send false}))

(defn- session-check-failed [queue-turn! err]
  (.error js/console "Session status check failed" err)
  (queue-turn! "Async agent chat failed"))

(defn- health-deps-ok [reply proxx-configured openplanner-configured parts]
  (let [proxx-res (aget parts 0)
        openplanner-res (aget parts 1)
        proxx-ok (and proxx-configured (aget proxx-res "ok"))
        openplanner-ok (and openplanner-configured (aget openplanner-res "ok"))
        healthy (and proxx-ok openplanner-ok)]
    (json-response!
     reply
     (if healthy 200 503)
     {:status (if healthy "ok" "unhealthy")
      :service "knoxx-backend-cljs"
      :dependencies {:proxx {:configured proxx-configured
                             :reachable (boolean proxx-ok)
                             :status_code (aget proxx-res "status")
                             :detail (js->clj (aget proxx-res "body") :keywordize-keys true)}
                     :openplanner {:configured openplanner-configured
                                   :reachable (boolean openplanner-ok)
                                   :status_code (aget openplanner-res "status")
                                   :detail (js->clj (aget openplanner-res "body") :keywordize-keys true)}}})))

(defn- health-deps-err [reply err]
  (json-response! reply 503 {:status "unhealthy"
                             :service "knoxx-backend-cljs"
                             :error (str err)}))

(defn- data-health-ok [reply results]
  (let [r (js->clj results :keywordize-keys true)]
    (json-response! reply 200
                    {:ok true
                     :services {:openplanner (nth r 0)
                                :proxx (nth r 1)
                                :ingestion (nth r 2)
                                :graph-weaver (nth r 3)
                                :shuvcrawl (nth r 4)
                                :vexx (nth r 5)
                                :eros-eris-field-app (nth r 6)
                                :myrmex (nth r 7)}})))

(defn- data-health-err [reply err]
  (json-response! reply 500 {:error (.-message err)}))

(def deps
  {:route! route!
   :json-response! json-response!
   :error-response! error-response!
   :ensure-permission! ensure-permission!
   :clip-text clip-text
   :with-request-context! with-request-context!
   :send-fetch-response! send-fetch-response!
   :bearer-headers bearer-headers
   :fetch-json fetch-json
   :request-query-string request-query-string
   :session-guard nil
   :optional-session-guard nil})

(defn- mongo-collections-ok [reply results]
  (let [r (js->clj results :keywordize-keys true)]
    (json-response! reply 200
                    {:ok true :documents (aget (nth r 0) "body") :graph (aget (nth r 1) "body")})))

(defn- undo-session-ok [reply session-id conversation-id removed-count rewound-messages]
  (json-response! reply 200 {:ok true
                             :session_id session-id
                             :conversation_id conversation-id
                             :removed_count removed-count
                             :remaining_messages (count rewound-messages)}))

(defn- undo-session-err [reply err]
  (json-response! reply 500 {:ok false :error (str err)}))

(defn- agents-active-ok [reply items]
  (json-response! reply 200 {:runs items :count (count items)}))

(defn- agents-active-err [reply err]
  (error-response! reply err 502))

(defn- abort-admin-ok [reply abort-result conversation-id resolved-session-id resolved-run-id]
  (json-response! reply 200
                  (assoc abort-result
                         :ok true
                         :conversation_id conversation-id
                         :session_id resolved-session-id
                         :run_id resolved-run-id
                         :marked_aborted true)))

(defn- run-events-ok [reply run-id events]
  (json-response! reply 200 {:run_id run-id :events events :count (count events)}))

(defn- run-events-err [reply err]
  (json-response! reply 500 {:error (str err)}))

(defn- shibboleth-ok [reply request body data]
  (let [session (or (aget data "session") #js {})
        session-id (str (or (aget session "id") ""))
        ui-url (if (and (not (str/blank? session-id))
                        (not (str/blank? (:shibboleth-ui-url config))))
                 (with-query-param (rewrite-localhost-url (:shibboleth-ui-url config) request)
                   "session"
                   session-id)
                 "")]
    (if (str/blank? session-id)
      (json-response! reply 502 {:detail "Shibboleth import did not return a session id"})
      (json-response! reply 200 {:ok true
                                 :session_id session-id
                                 :ui_url ui-url
                                 :imported_item_count (count (js-array-seq (aget body "items")))}))))

(defn- shibboleth-import-failed [reply resp]
  (json-response! reply 502 {:detail (str "Shibboleth import failed: "
                                          (or (aget (aget resp "body") "raw")
                                              (js/JSON.stringify (aget resp "body"))))}))

(defn- shibboleth-unreachable [reply err]
  (json-response! reply 502 {:detail (str "Shibboleth is unreachable: " err)}))

(defn- detect-zombies [conversation-id session session-id queue-turn! can-send-result reply latest-event]
  (clear-ghost-turn! conversation-id)
  (let [stalled? (and (= "running" (:status session))
                      (not (runtime-processing-session? conversation-id))
                      (stale-running-session? session latest-event))]
    (if stalled?
      (-> (session-store/complete-session! (redis/get-client)
                                           session-id
                                           conversation-id
                                           {:status "failed"
                                            :error "Session was stale/zombie; auto-aborted before new turn."
                                            :messages (:messages session)})
          (.then (fn [_]
                   (queue-turn! "Async direct agent chat failed (recovered from zombie)")))
          (.catch (fn [err]
                    (.error js/console "Failed to abort zombie session" err)
                    (json-response! reply 409 {:ok false
                                               :error (str "Agent is already processing. Zombie recovery failed: " err)
                                               :code "agent_already_processing"
                                               :has_active_stream false
                                               :can_send false}))))
      (json-response! reply 409 {:ok false
                                 :error (str "Agent is already processing. " (or (:reason can-send-result) ""))
                                 :code "agent_already_processing"
                                 :has_active_stream (boolean (:has_active_stream session))
                                 :can_send false}))))

(defn- handle-chat-start [runtime config reply ctx request]
  (let [node-crypto crypto
        parsed0 (normalize-chat-body (or (aget request "body") #js {}))
        parsed (assoc parsed0 :agent-spec (merged-agent-spec config parsed0))
        agent-ctx (effective-auth-context ctx parsed)
        policy-model (or (:model parsed)
                         (get-in parsed [:agent-spec :model])
                         (:llmModel @settings-state*))
        provided-session-id (:session-id parsed)
        session-id (ensure-session-id node-crypto provided-session-id)
        conversation-id (or (:conversation-id parsed)
                            (.randomUUID node-crypto))
        run-id (or (:run-id parsed)
                   (.randomUUID node-crypto))
        body (assoc parsed
                    :session-id session-id
                    :conversation-id conversation-id
                    :run-id run-id
                    :mode "rag"
                    :auth-context agent-ctx)
        accepted-response {:ok true
                           :queued true
                           :run_id run-id
                           :runId run-id
                           :conversation_id conversation-id
                           :conversationId conversation-id
                           :session_id session-id
                           :sessionId session-id
                           :model (or (:model body)
                                      (get-in body [:agent-spec :model])
                                      (:llmModel @settings-state*))}
        queue-turn! (fn [_log-label]
                      (queue-chat-start! runtime config reply agent-ctx policy-model body accepted-response))]
    (if-not provided-session-id
      (queue-turn! "Async agent chat failed")
      (-> (session-store/get-session (redis/get-client) session-id)
          (.then (fn [session]
                   (let [can-send-result (session-store/session-can-send? session)]
                     (if (:can-send can-send-result)
                       (let [agent-session (active-agent-session conversation-id)
                             actively-streaming? (and agent-session (streaming? agent-session))]
                         (if actively-streaming?
                           (json-response! reply 409
                                           {:ok false
                                            :error "Agent is already processing. Specify streamingBehavior steer or followUp to queue the message."
                                            :code "agent-already-processing"
                                            :has-active-stream true
                                            :can-send false})
                           (queue-turn! "Async agent chat failed")))
                       (-> (if (= "running" (:status session))
                             (latest-run-event! (:run_id session))
                             (js/Promise.resolve nil))
                           (.then (partial detect-zombies conversation-id session session-id queue-turn! can-send-result reply)))))))
          (.catch (fn [err]
                    (.error js/console "Session status check failed" err)
                    (queue-turn! "Async agent chat failed")))))))

(defn- handle-direct-start [runtime config reply ctx request]
  (let [node-crypto crypto
        parsed0 (normalize-chat-body (or (aget request "body") #js {}))
        parsed (assoc parsed0 :agent-spec (merged-agent-spec config parsed0))
        agent-ctx (effective-auth-context ctx parsed)
        policy-model (or (:model parsed)
                         (get-in parsed [:agent-spec :model])
                         (:llmModel @settings-state*))
        provided-session-id (:session-id parsed)
        session-id (ensure-session-id node-crypto provided-session-id)
        conversation-id (or (:conversation-id parsed) (.randomUUID node-crypto))
        run-id (or (:run-id parsed) (.randomUUID node-crypto))
        body (assoc parsed :session-id session-id :conversation-id conversation-id :run-id run-id :mode "direct" :auth-context agent-ctx)
        accepted-response {:ok true
                           :queued true
                           :run_id run-id
                           :conversation_id conversation-id
                           :session_id (:session-id body)
                           :model (or (:model body)
                                      (get-in body [:agent-spec :model])
                                      (:llmModel @settings-state*))}
        queue-turn! (fn [log-label]
                      (-> (validate-chat-policy! agent-ctx policy-model)
                          (.then (fn [_]
                                   (-> (send-agent-turn! runtime config body)
                                       (.then (fn [_] nil))
                                       (.catch (fn [err]
                                                 (.error js/console log-label err))))
                                   (json-response! reply 202 accepted-response)))
                          (.catch (fn [err]
                                    (error-response! reply err 429)))))]
    (if-not provided-session-id
      (queue-turn! "Async direct agent chat failed")
      (.then (session-store/get-session (redis/get-client) session-id)
             (fn [session]
               (let [can-send-result (session-store/session-can-send? session)]
                 (if (:can-send can-send-result)
                   (let [agent-session (active-agent-session conversation-id)
                         actively-streaming? (and agent-session (streaming? agent-session))]
                     (if actively-streaming?
                       (json-response! reply 409 {:ok false
                                                  :error "Agent is already processing. Specify streamingBehavior ('steer' or 'followUp') to queue the message."
                                                  :code "agent_already_processing"
                                                  :has_active_stream true
                                                  :can_send false})
                       (queue-turn! "Async direct agent chat failed")))
                   (.then (if (= "running" (:status session))
                            (latest-run-event! (:run_id session))
                            (js/Promise.resolve nil))
                          (partial detect-zombies conversation-id session session-id queue-turn! can-send-result reply)))))
             (fn [err]
               (.error js/console "Session status check failed" err)
               (queue-turn! "Async direct agent chat failed"))))))

(defn- handle-admin-abort [reply ctx request]
  (let [raw (or (aget request "body") #js {})
        requested-conversation-id (str (or (aget raw "conversation_id")
                                           (aget raw "conversationId")
                                           ""))
        requested-session-id (str (or (aget raw "session_id")
                                      (aget raw "sessionId")
                                      ""))
        requested-run-id (str (or (aget raw "run_id")
                                  (aget raw "runId")
                                  ""))
        reason (str (or (aget raw "reason") "operator_abort"))
        redis-client (redis/get-client)
        run (when-not (str/blank? requested-run-id)
              (get @runs* requested-run-id))
        session-id (or (some-> requested-session-id not-empty)
                       (:session_id run))]
    (-> (if (and redis-client (not (str/blank? (str session-id))))
          (session-store/get-session redis-client session-id)
          (js/Promise.resolve nil))
        (.then
         (fn [session]
           (let [conversation-id (str (or (some-> requested-conversation-id not-empty)
                                          (:conversation_id run)
                                          (:conversation_id session)
                                          ""))
                 resolved-session-id (str (or session-id (:session_id session) ""))
                 resolved-run-id (str (or requested-run-id (:run_id run) (:run_id session) ""))]
             (if (str/blank? conversation-id)
               (json-response! reply 400 {:ok false
                                          :error "conversation_id, session_id, or run_id is required"})
               (-> (turn-control/abort-active-turn! conversation-id reason)
                   (.then
                    (fn [abort-result]
                      (when-not (str/blank? resolved-run-id)
                        (run-state/update-run! resolved-run-id
                                               (fn [r]
                                                 (-> r
                                                     (assoc :status "aborted"
                                                            :error reason
                                                            :updated_at (now-iso))))))
                      (-> (if (and redis-client (not (str/blank? resolved-session-id)))
                            (session-store/update-session! redis-client resolved-session-id
                                                           {:status "aborted"
                                                            :error reason
                                                            :has_active_stream false})
                            (js/Promise.resolve nil))
                          (.then
                           (fn [_]
                             (json-response! reply 200
                                             (assoc abort-result
                                                    :ok true
                                                    :conversation_id conversation-id
                                                    :session_id resolved-session-id
                                                    :run_id resolved-run-id
                                                    :marked_aborted true))))))))))))
        (.catch (fn [err]
                  (error-response! reply err 409))))))

(defn- handle-session-status [runtime config reply request]
  (let [session-id (or (aget request "query" "session_id")
                       (aget request "query" "sessionId")
                       "")
        conversation-id (or (aget request "query" "conversation_id")
                            (aget request "query" "conversationId")
                            "")]
    (cond
      (str/blank? session-id)
      (json-response! reply 400 {:error "session_id is required"})

      :else
      (-> (session-store/get-session (redis/get-client) session-id)
          (.then (fn [session]
                   (if session
                     (let [conversation-id' (str (or (:conversation_id session) conversation-id ""))
                           runtime-active? (runtime-processing-session? conversation-id')
                           can-send (session-store/session-can-send? session)]
                       (-> (if (= "running" (:status session))
                             (latest-run-event! (:run_id session))
                             (js/Promise.resolve nil))
                           (.then (fn [latest-event]
                                    (let [stalled? (and (= "running" (:status session))
                                                        (not runtime-active?)
                                                        (stale-running-session? session latest-event))]
                                      (when stalled?
                                        (-> (resume-recovered-session! runtime config session)
                                            (.catch (fn [err]
                                                      (js/console.error "On-demand session recovery failed" err)))))
                                      (json-response! reply 200
                                                      {:session_id session-id
                                                       :conversation_id (:conversation_id session)
                                                       :run_id (:run_id session)
                                                       :status (:status session)
                                                       :has_active_stream (boolean (or (:has_active_stream session)
                                                                                       runtime-active?))
                                                       :can_send (if stalled? false (:can-send can-send))
                                                       :reason (cond
                                                                 stalled? "Session looked stalled after restart; recovery requested."
                                                                 runtime-active? "Session is already processing. Use steer, follow-up, abort, or wait."
                                                                 :else (:reason can-send))
                                                       :model (:model session)
                                                       :updated_at (:updated_at session)
                                                       :latest_event_at (:at latest-event)
                                                       :recovery_requested stalled?}))))))
                     ;; No session in Redis - trust in-memory runtime if it still has a live turn.
                     (if (runtime-processing-session? conversation-id)
                       (json-response! reply 200
                                       {:session_id session-id
                                        :conversation_id conversation-id
                                        :status "running"
                                        :has_active_stream true
                                        :can_send false
                                        :reason "Session is already processing. Use steer, follow-up, abort, or wait."})
                       (json-response! reply 200
                                       {:session_id session-id
                                        :conversation_id conversation-id
                                        :status "not_found"
                                        :has_active_stream false
                                        :can_send true
                                        :reason "No session state found. Ready for new turn."})))))
          (.catch (fn [err]
                    (js/console.error "Session status check failed" err)
                    (json-response! reply 500 {:error (str err)})))))))

(defroute health! []
  "GET" "/health"
  (let [proxx-configured (and (not (str/blank? (:proxx-base-url config)))
                              (not (str/blank? (:proxx-auth-token config))))
        openplanner-configured (openplanner-enabled? config)
        proxx-promise (if proxx-configured
                        (fetch-json (str (:proxx-base-url config) "/health")
                                    #js {:headers (bearer-headers (:proxx-auth-token config))})
                        (js/Promise.resolve #js {:ok false
                                                 :status 503
                                                 :body #js {:detail "Proxx is not configured"}}))
        openplanner-promise (if openplanner-configured
                              (fetch-json (openplanner-url config "/v1/health")
                                          #js {:headers (openplanner-headers config)})
                              (js/Promise.resolve #js {:ok false
                                                       :status 503
                                                       :body #js {:detail "OpenPlanner is not configured"}}))]
    (-> (js/Promise.all #js [proxx-promise openplanner-promise])
        (.then (partial health-deps-ok reply proxx-configured openplanner-configured))
        (.catch (partial health-deps-err reply)))))

;; Dev-only endpoint used to verify shadow-cljs hot-reload against a live
;; long-running Node runtime (shadow owns the process).
(defroute dev-hmr! []
  "GET" "/api/dev/hmr" []
  (json-response! reply 200 (dev-hmr-response)))

(defroute config! []
  "GET" "/api/config"
  (json-response!
   reply
   200
   {:knoxx_admin_url (rewrite-localhost-url (:knoxx-admin-url config) request)
    :knoxx_base_url (rewrite-localhost-url (:knoxx-base-url config) request)
    :knoxx_enabled true
    :stt_enabled (not (str/blank? (:stt-base-url config)))
    :stt_base_url (if (str/blank? (:stt-base-url config))
                    ""
                    (rewrite-localhost-url (:stt-base-url config) request))
    :tts_enabled (not (str/blank? (str/trim (or (:voxx-api-key config) ""))))
    :tts_provider (if (not (str/blank? (str/trim (or (:voxx-api-key config) ""))))
                    "voxx"
                    "")
    :tts_default_voice_id (or (:voxx-voice-id config) "af_jessica")
    :tts_default_model_id (or (:voxx-model-id config) "kokoro")
    :tts_default_speed (or (:voxx-default-speed config) "1.15")
    :tts_default_postprocess_enabled true
    :tts_default_postprocess_profile "sports-commentator-v1"
    :proxx_enabled (and (not (str/blank? (:proxx-base-url config)))
                        (not (str/blank? (:proxx-auth-token config))))
    :proxx_default_model (:llmModel @settings-state*)
    :shibboleth_ui_url (if (str/blank? (:shibboleth-ui-url config))
                         ""
                         (rewrite-localhost-url (:shibboleth-ui-url config) request))
    :shibboleth_enabled (and (not (str/blank? (:shibboleth-base-url config)))
                             (not (str/blank? (:shibboleth-ui-url config))))
    :default_role (:knoxx-default-role config)
    :default_actor_id (default-actor-id config)
    :default_agent_contract (default-agent-contract-id config (default-actor-id config))
    :email_enabled (email-enabled? config)
    :rbac_enabled (policy-db-enabled? runtime)}))

(defroute api-knoxx-agents-catalog! []
  "GET" "/api/knoxx/agents/catalog"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [actor-id (some-> (or (aget request "query" "actorId")
                             (aget request "query" "actor"))
                         str
                         str/trim
                         not-empty)
        effective-actor-id (or actor-id (default-actor-id config))
        agents (agent-contract-catalog config effective-actor-id)
        default-agent-id (default-agent-contract-id config effective-actor-id)
        default-agent (when default-agent-id
                        (effective-agent-contract config default-agent-id effective-actor-id))
        catalog (cond-> agents
                  (and default-agent
                       (not (some #(= (:id %) (:id default-agent)) agents)))
                  (conj default-agent))]
    (json-response! reply 200 {:actor_id effective-actor-id
                               :actors (mapv (fn [actor]
                                               {:id (:id actor)
                                                :kind (:kind actor)
                                                :defaultAgent (:default-agent actor)
                                                :roleSlugs (vec (or (:role-slugs actor) []))})
                                             (actor-catalog config))
                               :agents (vec (sort-by :id catalog))
                               :default_actor_id (default-actor-id config)
                               :default_agent_contract default-agent-id})))

(defroute api-auth-context! []
  "GET" "/api/auth/context"
  (if-not (policy-db-enabled? runtime)
    (json-response! reply 503 {:detail "Knoxx policy database is not configured"})
    (json-response! reply 200 {:user (:user ctx)
                               :actor (:actor ctx)
                               :org (:org ctx)
                               :membership (:membership ctx)
                               :roles (vec (or (:roles ctx) []))
                               :roleSlugs (vec (or (:roleSlugs ctx) []))
                               :permissions (vec (or (:permissions ctx) []))
                               :toolPolicies (vec (or (:toolPolicies ctx) []))
                               :membershipToolPolicies (vec (or (:membershipToolPolicies ctx) []))
                               :isSystemAdmin (boolean (:isSystemAdmin ctx))
                               :primaryRole (primary-context-role ctx)})))

(defroute api-knoxx-proxy-get! []
  "GET" "/api/knoxx/proxy/*"
  (let [path (aget request "params" "*")]
    (-> (forward-knoxx-request! config request "GET" path nil)
        (.then (partial proxy-ok reply))
        (.catch (partial proxy-err reply "Proxy request failed: ")))))

(defroute api-knoxx-proxy-post! []
  "POST" "/api/knoxx/proxy/*"
  (let [path (aget request "params" "*")]
    (-> (forward-knoxx-request! config request "POST" path nil)
        (.then (partial proxy-ok reply))
        (.catch (partial proxy-err reply "Proxy request failed: ")))))

(defroute api-knoxx-proxy-put! []
  "PUT" "/api/knoxx/proxy/*"
  (let [path (aget request "params" "*")]
    (-> (forward-knoxx-request! config request "PUT" path nil)
        (.then (partial proxy-ok reply))
        (.catch (partial proxy-err reply "Proxy request failed: ")))))

(defroute api-knoxx-proxy-patch! []
  "PATCH" "/api/knoxx/proxy/*"
  (let [path (aget request "params" "*")]
    (-> (forward-knoxx-request! config request "PATCH" path nil)
        (.then (partial proxy-ok reply))
        (.catch (partial proxy-err reply "Proxy request failed: ")))))

(defroute api-knoxx-proxy-delete! []
  "DELETE" "/api/knoxx/proxy/*"
  (let [path (aget request "params" "*")]
    (-> (forward-knoxx-request! config request "DELETE" path nil)
        (.then (partial proxy-ok reply))
        (.catch (partial proxy-err reply "Proxy request failed: ")))))

(defroute api-ingestion-browse! []
  "GET" "/api/ingestion/browse"
  (let [ingestion-base (:ingestion-base-url config)
        qs (request-query-string request)
        target-url (str ingestion-base "/api/ingestion/browse" qs)]
    (-> (fetch-json target-url #js {:method "GET"})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-ingestion-file! []
  "GET" "/api/ingestion/file"
  (let [ingestion-base (:ingestion-base-url config)
        qs (request-query-string request)
        target-url (str ingestion-base "/api/ingestion/file" qs)]
    (-> (fetch-json target-url #js {:method "GET"})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-ingestion-file-put! []
  "PUT" "/api/ingestion/file"
  (let [workspace-root (:workspace-root config)
        body (or (aget request "body") #js {})
        file-path (or (aget body "path") "")
        content (or (aget body "content") "")
        safe-path (normalize-relative-path file-path)]
    (if (or (str/blank? safe-path) (str/starts-with? safe-path ".."))
      (json-response! reply 400 {:detail "Invalid or unsafe file path"})
      (let [absolute-path (.resolve path workspace-root safe-path)]
        (if (not (str/starts-with? absolute-path workspace-root))
          (json-response! reply 400 {:detail "Path escapes workspace"})
          (let [dir (.dirname path absolute-path)]
            (-> (.mkdir fs dir #js {:recursive true})
                (.then (fn [] (.writeFile fs absolute-path content "utf8")))
                (.then (fn [] (json-response! reply 200 {:ok true :path safe-path})))
                (.catch (fn [err]
                          (json-response! reply 500 {:detail (str "Write failed: " err)}))))))))))

(defroute api-ingestion-sources! []
  "GET" "/api/ingestion/sources"
  (let [ingestion-base (:ingestion-base-url config)]
    (-> (fetch-json (str ingestion-base "/api/ingestion/sources") #js {:method "GET"})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-ingestion-jobs-get! []
  "GET" "/api/ingestion/jobs"
  (let [ingestion-base (:ingestion-base-url config)
        qs (request-query-string request)
        target-url (str ingestion-base "/api/ingestion/jobs" qs)]
    (-> (fetch-json target-url #js {:method "GET"})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-ingestion-jobs-post! []
  "POST" "/api/ingestion/jobs"
  (let [ingestion-base (:ingestion-base-url config)
        body (aget request "body")
        target-url (str ingestion-base "/api/ingestion/jobs")]
    (-> (fetch-json target-url #js {:method "POST"
                                    :headers #js {"Content-Type" "application/json"}
                                    :body (js/JSON.stringify (or body #js {}))})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-ingestion-proxy-get! []
  "GET" "/api/ingestion-proxy/*"
  (let [ingestion-base (:ingestion-base-url config)
        path (aget request "params" "*")
        qs (request-query-string request)
        target-url (str ingestion-base "/api/ingestion/" path qs)]
    (-> (fetch-json target-url #js {:method "GET"})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err-detail reply "Ingestion proxy failed: ")))))

(defroute api-ingestion-proxy-post! []
  "POST" "/api/ingestion-proxy/*"
  (let [ingestion-base (:ingestion-base-url config)
        path (aget request "params" "*")
        target-url (str ingestion-base "/api/ingestion/" path)
        body (aget request "body")]
    (-> (fetch-json target-url #js {:method "POST"
                                    :headers #js {"Content-Type" "application/json"}
                                    :body (js/JSON.stringify (or body #js {}))})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err-detail reply "Ingestion proxy failed: ")))))

(defroute api-ingestion-proxy-delete! []
  "DELETE" "/api/ingestion-proxy/*"
  (let [ingestion-base (:ingestion-base-url config)
        path (aget request "params" "*")
        target-url (str ingestion-base "/api/ingestion/" path)]
    (-> (fetch-json target-url #js {:method "DELETE"})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err-detail reply "Ingestion proxy failed: ")))))

(defroute api-data-op-get! []
  "GET" "/api/data/op/*"
  (let [path (aget request "params" "*")
        raw-url (aget request "raw" "url")
        query-idx (.indexOf raw-url "?")
        qs (if (>= query-idx 0) (subs raw-url query-idx) "")
        op-base (:openplanner-base-url config)
        op-key (:openplanner-api-key config)
        tenant-id (or (:session-project-name config) "knoxx-session")]
    (-> (fetch-json (str op-base "/v1/" path qs)
                    #js {:headers #js {"Authorization" (str "Bearer " op-key)
                                        "X-Tenant-ID" tenant-id}})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-data-op-post! []
  "POST" "/api/data/op/*"
  (let [path (aget request "params" "*")
        body (aget request "body")
        op-base (:openplanner-base-url config)
        op-key (:openplanner-api-key config)
        tenant-id (or (:session-project-name config) "knoxx-session")]
    (-> (fetch-json (str op-base "/v1/" path)
                    #js {:method "POST"
                         :headers #js {"Content-Type" "application/json"
                                       "Authorization" (str "Bearer " op-key)
                                       "X-Tenant-ID" tenant-id}
                         :body (js/JSON.stringify (or body #js {}))})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-data-op-delete! []
  "DELETE" "/api/data/op/*"
  (let [path (aget request "params" "*")
        op-base (:openplanner-base-url config)
        op-key (:openplanner-api-key config)
        tenant-id (or (:session-project-name config) "knoxx-session")]
    (-> (fetch-json (str op-base "/v1/" path)
                    #js {:method "DELETE"
                         :headers #js {"Authorization" (str "Bearer " op-key)
                                       "X-Tenant-ID" tenant-id}})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-data-op-patch! []
  "PATCH" "/api/data/op/*"
  (let [path (aget request "params" "*")
        body (aget request "body")
        op-base (:openplanner-base-url config)
        op-key (:openplanner-api-key config)
        tenant-id (or (:session-project-name config) "knoxx-session")]
    (-> (fetch-json (str op-base "/v1/" path)
                    #js {:method "PATCH"
                         :headers #js {"Content-Type" "application/json"
                                       "Authorization" (str "Bearer " op-key)
                                       "X-Tenant-ID" tenant-id}
                         :body (js/JSON.stringify (or body #js {}))})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-data-health! []
  "GET" "/api/data/health"
  (let [ingestion-base (:ingestion-base-url config)
        op-base (:openplanner-base-url config)
        op-key (:openplanner-api-key config)
        proxx-base (:proxx-base-url config)
        proxx-key (:proxx-auth-token config)
        check (fn [url headers]
                (-> (fetch-json url #js {:headers (or headers #js {}) :method "GET"})
                    (.then (fn [resp] {:ok (aget resp "ok")
                                       :status (aget resp "status")
                                       :url url
                                       :detail (aget resp "body")}))
                    (.catch (fn [err] {:ok false :error (.-message err) :url url}))))]
    (-> (js/Promise.all
         (into-array
           [(check (str op-base "/v1/health") #js {"Authorization" (str "Bearer " op-key)
                                                     "X-Tenant-ID" (or (:session-project-name config) "knoxx-session")})
            (check (str proxx-base "/health") #js {"Authorization" (str "Bearer " proxx-key)})
            (check (str ingestion-base "/health") nil)
           (check "http://127.0.0.1:8796/api/status" nil)
           (check "http://127.0.0.1:3777/health" nil)
           (check "http://127.0.0.1:8787/v1/health" nil)
           (check "http://127.0.0.1:8786/health" nil)
           (check "http://127.0.0.1:8801/health" nil)]))
        (.then (partial data-health-ok reply))
        (.catch (partial data-health-err reply)))))

(defroute api-data-mongo-collections! []
  "GET" "/api/data/mongo/collections"
  (let [op-base (:openplanner-base-url config)
        op-key (:openplanner-api-key config)
        tenant-id (or (:session-project-name config) "knoxx-session")]
    (-> (js/Promise.all
         (into-array
          [(fetch-json (str op-base "/v1/documents/stats")
                       #js {:headers #js {"Authorization" (str "Bearer " op-key)
                                           "X-Tenant-ID" tenant-id}})
           (fetch-json (str op-base "/v1/graph/monitoring")
                       #js {:headers #js {"Authorization" (str "Bearer " op-key)
                                           "X-Tenant-ID" tenant-id}})]))
        (.then (partial mongo-collections-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-data-mongo-list! []
  "GET" "/api/data/mongo/list"
  (let [op-base (:openplanner-base-url config)
        op-key (:openplanner-api-key config)
        tenant-id (or (:session-project-name config) "knoxx-session")]
    (-> (fetch-json (str op-base "/v1/mongo/collections")
                    #js {:headers #js {"Authorization" (str "Bearer " op-key)
                                        "X-Tenant-ID" tenant-id}})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-data-mongo-query! []
  "POST" "/api/data/mongo/query"
  (let [body (or (aget request "body") #js {})
        op-base (:openplanner-base-url config)
        op-key (:openplanner-api-key config)
        tenant-id (or (:session-project-name config) "knoxx-session")]
    (-> (fetch-json (str op-base "/v1/mongo/query")
                    #js {:method "POST"
                         :headers #js {"Content-Type" "application/json"
                                       "Authorization" (str "Bearer " op-key)
                                       "X-Tenant-ID" tenant-id}
                         :body (js/JSON.stringify body)})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-data-pg-tables! []
  "GET" "/api/data/pg/tables"
  (json-response! reply 200
                  {:ok true
                   :tables ["ingestion_sources" "ingestion_jobs" "ingestion_file_state"
                            "orgs" "users" "roles" "memberships" "data_lakes"
                            "sessions" "audit_events" "permissions"]}))

(defroute api-data-jobs-build-semantic-edges! []
  "POST" "/api/data/jobs/build-semantic-edges"
  (let [body (or (aget request "body") #js {})
        k (or (aget body "k") 8)
        min-sim (or (aget body "minSimilarity") 0.3)
        op-base (:openplanner-base-url config)
        op-key (:openplanner-api-key config)
        tenant-id (or (:session-project-name config) "knoxx-session")]
    (-> (fetch-json (str op-base "/v1/jobs/build-semantic-edges")
                    #js {:method "POST"
                         :headers #js {"Content-Type" "application/json"
                                       "Authorization" (str "Bearer " op-key)
                                       "X-Tenant-ID" tenant-id}
                         :body (js/JSON.stringify #js {:k k :minSimilarity min-sim})})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-data-pg-query! []
  "POST" "/api/data/pg/query"
  (let [body (or (aget request "body") #js {})
        raw-sql (or (aget body "sql") "")
        table (or (aget body "table") "")
        limit (or (aget body "limit") 50)
        db (policy-db runtime)
        query-fn (when db (aget db "query"))]
    (cond
      (nil? db)
      (json-response! reply 503 {:error "Policy database not configured"})

      (not (str/blank? raw-sql))
      ;; Raw SQL mode — SELECT only
      (let [trimmed (str/trim raw-sql)]
        (if-not (str/starts-with? (str/upper-case trimmed) "SELECT")
          (json-response! reply 400 {:error "Only SELECT queries are allowed"})
          (let [enforced-limit (min (max (js/parseInt (str limit) 10) 1) 500)
                ;; Inject LIMIT if none present
                has-limit (re-find #"(?i)\bLIMIT\b" trimmed)
                final-sql (if has-limit
                            trimmed
                            (str trimmed " LIMIT " enforced-limit))]
            (-> (query-fn final-sql #js [])
                (.then (partial pg-query-ok reply))
                (.catch (partial pg-query-err reply))))))

      ;; Table browse mode
      (or (str/blank? table)
          (re-find #"[^a-zA-Z0-9_]" table))
      (json-response! reply 400 {:error "Invalid table name"})

      :else
      (let [enforced-limit (min (max (js/parseInt (str limit) 10) 1) 500)
            sql-str (str "SELECT * FROM " table " LIMIT " enforced-limit)]
        (-> (query-fn sql-str #js [])
            (.then (partial pg-query-table-ok reply table))
            (.catch (partial pg-query-err reply)))))))

(defroute api-data-browse! []
  "GET" "/api/data/browse"
  (let [qs (aget request "query")
        path (or (aget qs "path") "")
        ingestion-base (:ingestion-base-url config)
        target-url (str ingestion-base "/api/ingestion/browse" (if (str/blank? path) "" (str "?path=" (js/encodeURIComponent path))))]
    (-> (fetch-json target-url nil)
        (.then (fn [resp]
                 (json-response! reply (or (aget resp "status") 200) (aget resp "body"))))
        (.catch (fn [err]
                  (json-response! reply 502 {:error (.-message err)}))))))

(defroute api-data-file! []
  "GET" "/api/data/file"
  (let [qs (aget request "query")
        path (or (aget qs "path") "")
        ingestion-base (:ingestion-base-url config)]
    (-> (fetch-json (str ingestion-base "/api/ingestion/file?path=" (js/encodeURIComponent path)) nil)
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-data-graphql! []
  "POST" "/api/data/graphql"
  (let [body (or (aget request "body") #js {})
        gw-url "http://127.0.0.1:8796/graphql"]
    (-> (fetch-json gw-url
                    #js {:method "POST"
                         :headers #js {"Content-Type" "application/json"}
                         :body (js/JSON.stringify body)})
        (.then (partial fetch-json-ok reply))
        (.catch (partial fetch-json-err reply)))))

(defroute api-data-graph-status! []
  "GET" "/api/data/graph/status"
  (-> (fetch-json "http://127.0.0.1:8796/api/status" #js {:method "GET"})
      (.then (partial fetch-json-ok reply))
      (.catch (partial fetch-json-err reply))))

;; Embed the graph-weaver WebGL view URL for the frontend
(defroute api-data-graph-view-url! []
  "GET" "/api/data/graph/view-url"
  (json-response! reply 200 {:url "http://127.0.0.1:8796"}))

(defroute api-knoxx-health! []
  "GET" "/api/knoxx/health"
  (json-response! reply 200 {:reachable true
                             :configured true
                             :base_url (:knoxx-base-url config)
                             :status_code 200
                             :details {:mode "shadow-cljs-eta-mu-sdk"
                                       :status "ok"
                                       :project (:project-name config)
                                       :collection {:name (:collection-name config)
                                                    :pointsCount nil}}}))

(defn- chat-turn-ok [reply resp]
  (json-response! reply 200 resp))

(defn- chat-turn-err [reply err]
  (error-response! reply err 502))

(defroute api-knoxx-chat! []
  "POST" "/api/knoxx/chat"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [parsed0 (normalize-chat-body (or (aget request "body") #js {}))
        parsed (assoc parsed0 :agent-spec (merged-agent-spec config parsed0))
        agent-ctx (effective-auth-context ctx parsed)
        body (assoc parsed
                    :mode "rag"
                    :auth-context agent-ctx)]
    (-> (send-agent-turn! runtime config body)
        (.then (partial chat-turn-ok reply))
        (.catch (partial chat-turn-err reply)))))

(defroute api-knoxx-chat-start! []
  "POST" "/api/knoxx/chat/start"
  (when ctx
    (ensure-permission! ctx "agent.chat.use"))
  (handle-chat-start runtime config reply ctx request))

(defroute api-knoxx-direct! []
  "POST" "/api/knoxx/direct"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [parsed0 (normalize-chat-body (or (aget request "body") #js {}))
        parsed (assoc parsed0 :agent-spec (merged-agent-spec config parsed0))
        agent-ctx (effective-auth-context ctx parsed)
        body (assoc parsed
                    :mode "direct"
                    :auth-context agent-ctx)]
    (-> (send-agent-turn! runtime config body)
        (.then (partial chat-turn-ok reply))
        (.catch (partial chat-turn-err reply)))))

(defroute api-knoxx-direct-start! []
  "POST" "/api/knoxx/direct/start"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (handle-direct-start runtime config reply ctx request))

(defn- steer-ok [reply resp]
  (json-response! reply 200 resp))

(defn- steer-err [reply err]
  (error-response! reply err 409))

(defroute api-knoxx-steer! []
  "POST" "/api/knoxx/steer"
  (when ctx (ensure-permission! ctx "agent.controls.steer"))
  (let [body (assoc (normalize-control-body (or (aget request "body") #js {})) :kind "steer")
        actor-ctx (auth-context-with-actor ctx (:actor-id body))]
    (ensure-conversation-access! actor-ctx (:conversation-id body))
    (-> (queue-agent-control! runtime config body)
        (.then (partial steer-ok reply))
        (.catch (partial steer-err reply)))))

(defroute api-knoxx-follow-up! []
  "POST" "/api/knoxx/follow-up"
  (when ctx (ensure-permission! ctx "agent.controls.follow_up"))
  (let [body (assoc (normalize-control-body (or (aget request "body") #js {})) :kind "follow_up")
        actor-ctx (auth-context-with-actor ctx (:actor-id body))]
    (ensure-conversation-access! actor-ctx (:conversation-id body))
    (-> (queue-agent-control! runtime config body)
        (.then (partial steer-ok reply))
        (.catch (partial steer-err reply)))))

(defn- abort-ok [reply resp]
  (json-response! reply (if (:ok resp) 200 409) resp))

;; Abort / interrupt the current running turn for a conversation.
;; This is stronger than steer(): it cancels the current operation immediately.
(defroute api-knoxx-abort! []
  "POST" "/api/knoxx/abort"
  (when ctx (ensure-permission! ctx "agent.controls.steer"))
  (let [raw (or (aget request "body") #js {})
        conversation-id (or (aget raw "conversation_id") (aget raw "conversationId") "")
        actor-id (or (aget raw "actor_id") (aget raw "actorId") (aget raw "actor-id"))
        actor-ctx (auth-context-with-actor ctx actor-id)
        reason (or (aget raw "reason") "aborted_by_user")]
    (if (str/blank? (str conversation-id))
      (json-response! reply 400 {:ok false :error "conversation_id is required"})
      (do
        (ensure-conversation-access! actor-ctx conversation-id)
        (-> (turn-control/abort-active-turn! conversation-id reason)
            (.then (partial abort-ok reply))
            (.catch (partial steer-err reply)))))))

(defroute api-knoxx-session-undo! []
  "POST" "/api/knoxx/session/undo"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [raw (or (aget request "body") #js {})
        session-id (str (or (aget raw "session_id")
                            (aget raw "sessionId")
                            ""))
        provided-conversation-id (str (or (aget raw "conversation_id")
                                          (aget raw "conversationId")
                                          ""))
        actor-id (or (aget raw "actor_id") (aget raw "actorId") (aget raw "actor-id"))
        actor-ctx (auth-context-with-actor ctx actor-id)
        turns-raw (or (aget raw "turns") 1)
        turns (let [parsed (js/parseInt (str turns-raw) 10)]
                (if (js/isNaN parsed) 1 (max 1 parsed)))]
    (if (str/blank? session-id)
      (json-response! reply 400 {:ok false :error "session_id is required"})
      (-> (session-store/get-session (redis/get-client) session-id)
          (.then
           (fn [session]
             (cond
               (nil? session)
               (json-response! reply 404 {:ok false :error "Session not found or expired"})

               (= "running" (:status session))
               (json-response! reply 409 {:ok false :error "Cannot undo while a turn is still running"})

               :else
               (let [conversation-id (str (or (:conversation_id session) provided-conversation-id ""))
                     current-messages (vec (or (:messages session) []))
                     rewound-messages (session-store/rewind-messages current-messages turns)
                     removed-count (- (count current-messages) (count rewound-messages))]
                 (when (and actor-ctx (not (str/blank? conversation-id)))
                   (ensure-conversation-access! actor-ctx conversation-id))
                 (if (zero? removed-count)
                   (json-response! reply 409 {:ok false :error "No user turns available to undo"})
                   (-> (session-store/undo-session-turns! (redis/get-client) session-id turns)
                       (.then
                        (fn [_]
                          (json-response! reply 200 {:ok true
                                                     :session_id session-id
                                                     :conversation_id conversation-id
                                                     :removed_count removed-count
                                                     :remaining_messages (count rewound-messages)})))))))))
          (.catch (partial undo-session-err reply))))))


(defn- build-active-runs [ctx limit]
  (let [sessions-by-id (into {}
                             (map (fn [session]
                                    [(:session_id session) session]))
                             (session-store/active-session-snapshots))]
    (->> @run-order*
         (map #(get @runs* %))
         (filter some?)
         (filter #(contains? #{"queued" "running" "waiting_input"} (:status %)))
         (filter #(run-visible? ctx %))
         (map (fn [run]
                (active-run-summary run (get sessions-by-id (:session_id run)))))
         (take limit)
         vec)))

(defroute api-knoxx-agents-active! []
  "GET" "/api/knoxx/agents/active"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [limit-raw (aget request "query" "limit")
        limit (if (string? limit-raw)
                (max 1 (js/parseInt limit-raw 10))
                25)
        items (build-active-runs ctx limit)]
    (agents-active-ok reply items)))

(defroute api-admin-agents-active! []
  "GET" "/api/admin/agents/active"
  (ensure-permission! ctx "org.events.control")
  (let [limit-raw (aget request "query" "limit")
        limit (or (parse-positive-int limit-raw) 200)]
    (-> (live-active-agent-summaries! limit false)
        (.then (partial agents-active-ok reply))
        (.catch (partial agents-active-err reply)))))

(defroute api-admin-agents-abort! []
  "POST" "/api/admin/agents/abort"
  (ensure-permission! ctx "org.events.control")
  (handle-admin-abort reply ctx request))

;; Session status endpoint for frontend resume detection
(defroute api-knoxx-session-status! []
  "GET" "/api/knoxx/session/status"
  (handle-session-status runtime config reply request))

;; Run event catch-up endpoint for WS reconnect recovery
(defroute api-knoxx-run-events! []
  "GET" "/api/knoxx/run/:runId/events"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [run-id (aget request "params" "runId")
        since (or (aget request "query" "since") "")]
    (if (str/blank? run-id)
      (json-response! reply 400 {:error "runId is required"})
      (-> (run-state/get-run-events-since (redis/get-client) run-id since)
          (.then (partial run-events-ok reply run-id))
          (.catch (partial run-events-err reply))))))

(defroute api-knoxx-run-get! []
  "GET" "/api/knoxx/runs/:runId"
  (when ctx (ensure-permission! ctx "agent.chat.use"))
  (let [run-id (str (or (aget request "params" "runId") ""))]
    (cond
      (str/blank? run-id)
      (json-response! reply 400 {:error "runId required"})

      (some? (get @runs* run-id))
      (if-let [filtered (run-visible? ctx (get @runs* run-id))]
        (json-response! reply 200
                        {:ok true :source "memory" :run filtered})
        (json-response! reply 403 {:error "Access denied"}))

      :else
      (json-response! reply 404 {:ok false :error "Run not found"
                                 :run_id run-id}))))

(defroute api-shibboleth-handoff! []
  "POST" "/api/shibboleth/handoff"
  (let [body (or (aget request "body") #js {})]
    (if (str/blank? (:shibboleth-base-url config))
      (json-response! reply 503 {:detail "SHIBBOLETH_BASE_URL is not configured"})
      (let [payload #js {:source_app "knoxx"
                         :model (aget body "model")
                         :system_prompt (aget body "system_prompt")
                         :provider (aget body "provider")
                         :conversation_id (aget body "conversation_id")
                         :fake_tools_enabled (boolean (aget body "fake_tools_enabled"))
                         :items (or (aget body "items") #js [])}]
        (-> (fetch-json (str (:shibboleth-base-url config) "/api/chat/import")
                        #js {:method "POST"
                             :headers #js {"Content-Type" "application/json"}
                             :body (.stringify js/JSON payload)})
            (.then (fn [resp]
                     (if (aget resp "ok")
                       (shibboleth-ok reply request body (aget resp "body"))
                       (shibboleth-import-failed reply resp))))
            (.catch (partial shibboleth-unreachable reply)))))))

(defn register-routes!
  [runtime app config lounge-messages*]
  (ensure-settings! config)

  (health! app runtime config deps)
  (dev-hmr! app runtime config deps)
  (config! app runtime config deps)
  (api-knoxx-agents-catalog! app runtime config deps)
  (api-auth-context! app runtime config deps)

  (admin-routes/register-admin-routes! app runtime
                                       {:route! route!
                                        :json-response! json-response!
                                        :with-request-context! with-request-context!
                                        :ensure-permission! ensure-permission!
                                        :ensure-any-permission! ensure-any-permission!
                                        :ensure-org-scope! ensure-org-scope!
                                        :policy-db policy-db
                                        :policy-db-promise policy-db-promise
                                        :http-error http-error})

  (memory-routes/register-memory-routes! app runtime config
                                         {:route! route!
                                          :json-response! json-response!
                                          :error-response! error-response!
                                          :with-request-context! with-request-context!
                                          :ensure-permission! ensure-permission!
                                          :parse-positive-int parse-positive-int
                                          :truthy-param? truthy-param?
                                          :start-session-title-backfill! start-session-title-backfill!
                                          :session-title-backfill* session-title-backfill*
                                          :session-titles* session-titles*
                                          :get-cached-session-title! get-cached-session-title!
                                          :openplanner-enabled? openplanner-enabled?
                                          :openplanner-request! openplanner-request!
                                          :fetch-openplanner-session-rows! fetch-openplanner-session-rows!
                                          :session-title-seed-text session-title-seed-text
                                          :heuristic-session-title heuristic-session-title
                                          :stored-session-title-entry stored-session-title-entry
                                          :cache-session-title-entry! cache-session-title-entry!
                                          :resolve-session-title! resolve-session-title!
                                          :cache-session-title! cache-session-title!
                                          :normalize-session-title normalize-session-title
                                          :session-visible? session-visible?
                                          :session-matches-page-actor-filter? session-matches-page-actor-filter?
                                          :openplanner-memory-search! openplanner-memory-search!
                                          :filter-authorized-memory-hits! filter-authorized-memory-hits!
                                          :ctx-permitted? ctx-permitted?
                                          :system-admin? system-admin?
                                          :http-error http-error
                                          :now-iso now-iso
                                          :broadcast-ws! broadcast-ws!
                                          :lounge-messages* lounge-messages*
                                          :authorized-session-ids! authorized-session-ids!})

  (let [session-guard          (guards/make-session-guard runtime)
        optional-session-guard (guards/make-optional-session-guard runtime)]
    (tool-routes/register-tool-routes! app runtime config
                                       {:route! route!
                                        :json-response! json-response!
                                        :error-response! error-response!
                                        :with-request-context! with-request-context!
                                        :ensure-permission! ensure-permission!
                                        :tool-catalog tool-catalog
                                        :ensure-role-can-use! ensure-role-can-use!
                                        :resolve-workspace-path resolve-workspace-path
                                        :count-occurrences count-occurrences
                                        :replace-first replace-first
                                        :clip-text clip-text
                                        :session-guard session-guard
                                        :optional-session-guard optional-session-guard})

    (actor-routes/register-actor-routes! app runtime config
                                         {:route! route!
                                          :json-response! json-response!
                                          :error-response! error-response!
                                          :with-request-context! with-request-context!
                                          :ensure-permission! ensure-permission!
                                          :session-guard session-guard}))

  (contracts-routes/register-contracts-routes! app runtime config
                                               {:route! route!
                                                :json-response! json-response!
                                                :error-response! error-response!
                                                :with-request-context! with-request-context!
                                                :ensure-permission! ensure-permission!})

  (model-routes/register-model-routes! app runtime config)

  (voice-routes/register-voice-routes! app runtime config
                                       {:route! route!
                                        :json-response! json-response!
                                        :with-request-context! with-request-context!
                                        :ensure-tool! ensure-tool!})

  (document-routes/register-document-routes! app runtime config
                                             {:route! route!
                                              :json-response! json-response!
                                              :error-response! error-response!
                                              :with-request-context! with-request-context!
                                              :ensure-permission! ensure-permission!
                                              :clip-text clip-text
                                              :openplanner-graph-export! openplanner-graph-export!
                                              :send-fetch-response! send-fetch-response!
                                              :bearer-headers bearer-headers
                                              :fetch-json fetch-json
                                              :openai-auth-error openai-auth-error
                                              :request-query-string request-query-string})

  (workspace-media-routes/register-workspace-media-routes! app runtime config
                                                           {:route! route!
                                                            :json-response! json-response!
                                                            :error-response! error-response!
                                                            :with-request-context! with-request-context!
                                                            :ensure-tool! ensure-tool!})

  ;; Broadcast studio routes (audio library, player state, playlist)
  (studio-routes/register-studio-routes! app runtime config
                                         {:route! route!
                                          :json-response! json-response!
                                          :error-response! error-response!
                                          :with-request-context! with-request-context!
                                          :ensure-tool! ensure-tool!
                                          :ensure-permission! ensure-permission!
                                          :policy-db policy-db
                                          :policy-db-promise policy-db-promise})

  (api-knoxx-proxy-get! app runtime config deps)
  (api-knoxx-proxy-post! app runtime config deps)
  (api-knoxx-proxy-put! app runtime config deps)
  (api-knoxx-proxy-patch! app runtime config deps)
  (api-knoxx-proxy-delete! app runtime config deps)
  (api-ingestion-browse! app runtime config deps)
  (api-ingestion-file! app runtime config deps)
  (api-ingestion-sources! app runtime config deps)
  (api-ingestion-jobs-get! app runtime config deps)
  (api-ingestion-jobs-post! app runtime config deps)
  (api-ingestion-proxy-get! app runtime config deps)
  (api-ingestion-proxy-post! app runtime config deps)
  (api-ingestion-proxy-delete! app runtime config deps)

  ;; ── Data explorer routes ─────────────────────────────────────────────
  ;; Service health aggregation
  ;; OpenPlanner API proxy for documents, search, etc.
  (api-data-op-get! app runtime config deps)
  (api-data-op-post! app runtime config deps)
  (api-data-op-patch! app runtime config deps)
  (api-data-op-delete! app runtime config deps)
  (api-data-health! app runtime config deps)
  (api-data-mongo-collections! app runtime config deps)
  (api-data-mongo-list! app runtime config deps)
  (api-data-mongo-query! app runtime config deps)
  (api-data-pg-tables! app runtime config deps)
  (api-data-jobs-build-semantic-edges! app runtime config deps)
  (api-data-pg-query! app runtime config deps)
  (api-data-browse! app runtime config deps)
  (api-data-file! app runtime config deps)
  (api-data-graphql! app runtime config deps)
  (api-data-graph-status! app runtime config deps)
  (api-data-graph-view-url! app runtime config deps)

  (api-knoxx-health! app runtime config deps)
  (api-knoxx-chat! app runtime config deps)
  (api-knoxx-chat-start! app runtime config deps)
  (api-knoxx-direct! app runtime config deps)
  (api-knoxx-direct-start! app runtime config deps)


  (api-knoxx-steer! app runtime config deps)
  (api-knoxx-follow-up! app runtime config deps)
  (api-knoxx-abort! app runtime config deps)
  (api-knoxx-session-undo! app runtime config deps)

  (api-knoxx-agents-active! app runtime config deps)
  (api-admin-agents-active! app runtime config deps)
  (api-admin-agents-abort! app runtime config deps)

  (api-knoxx-session-status! app runtime config deps)
  (api-knoxx-run-events! app runtime config deps)
  (api-knoxx-run-get! app runtime config deps)
  (api-shibboleth-handoff! app runtime config deps)

  ;; Translation routes
  (translation-routes/register-translation-routes! app runtime config
                                                   {:json-response! json-response!
                                                    :error-response! error-response!
                                                    :with-request-context! with-request-context!
                                                    :ensure-permission! ensure-permission!
                                                    :openplanner-enabled? openplanner-enabled?
                                                    :openplanner-request! openplanner-request!
                                                    :openplanner-url openplanner-url
                                                    :openplanner-headers openplanner-headers
                                                    :ctx-user-id ctx-user-id
                                                    :ctx-user-email ctx-user-email
                                                    :ctx-org-id ctx-org-id}))
