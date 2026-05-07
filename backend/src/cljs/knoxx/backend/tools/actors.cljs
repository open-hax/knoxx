(ns knoxx.backend.tools.actors
  "Actor-to-actor messaging tools."
  (:require [clojure.string :as str]
            [knoxx.backend.actor-mailbox :as actor-mailbox]
            [knoxx.backend.agent-context :as agent-context]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.session-store :as session-store]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.shared :refer [create-tool-obj json-parse live-config maybe-tool-update!]]))

(def send-message-params
  [:map
   [:target {:description "Target address: parent, self, conversation:<id>, conversation-id:<id>, session:<id>, session-id:<id>, actor:<id>, actor-id:<id>, or a raw conversation/session id when target_type is provided."} :string]
   [:content {:description "Message content to deliver."} :string]
   [:mode {:optional true :description "Delivery mode: message (default follow-up), steer, follow-up, or event."} :string]
   [:target_type {:optional true :description "Optional hint for raw target: parent, self, conversation, session, actor, or event."} :string]
   [:conversation_id {:optional true :description "Explicit conversation id override."} :string]
   [:session_id {:optional true :description "Explicit session id override."} :string]
   [:run_id {:optional true :description "Optional target/current run id for audit linkage."} :string]
   [:metadata_json {:optional true :description "Optional JSON object with lineage/audit metadata, e.g. parentRunId or subAgentId."} :string]])

(defn- self-headers
  [config]
  (let [api-key (:knoxx-api-key (live-config config))]
    (cond-> #js {"Content-Type" "application/json"
                 "x-knoxx-user-email" "system-admin@open-hax.local"}
      (not (str/blank? (str api-key)))
      (aset "X-API-Key" api-key))))

(defn- api-base
  [config]
  (or (:knoxx-base-url (live-config config))
      "http://127.0.0.1:8000"))

(defn- fetch-json!
  [config method path body]
  (-> (js/fetch (str (api-base config) path)
                #js {:method method
                     :headers (self-headers config)
                     :body (when body (.stringify js/JSON (clj->js body)))})
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str "HTTP " (.-status resp) ": " text)))))))))))

(defn- normalize-mode
  [mode]
  (let [mode* (some-> mode str str/trim str/lower-case (str/replace #"_" "-"))]
    (case mode*
      "steer" "steer"
      "follow-up" "follow-up"
      "followup" "follow-up"
      "event" "event"
      "message" "message"
      "message")))

(defn- normalize-target-type
  [target-type]
  (some-> target-type str str/trim str/lower-case (str/replace #"_" "-") not-empty))

(defn- parse-metadata
  [metadata-json]
  (if (str/blank? (str metadata-json))
    {}
    (json-parse (str metadata-json))))

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn- prefixed-target
  [target]
  (let [t (str/trim (str target))
        idx (.indexOf t ":")]
    (when (pos? idx)
      {:type (normalize-target-type (.slice t 0 idx))
       :id (nonblank (.slice t (inc idx)))})))

(defn- current-context
  []
  (or (agent-context/get-context) {}))

(defn- parent-context
  [ctx metadata]
  (let [agent-spec (:agent-spec ctx)]
    {:conversation-id (or (nonblank (:parentConversationId metadata))
                         (nonblank (:parent-conversation-id metadata))
                         (nonblank (:parent_conversation_id metadata))
                         (nonblank (:parentConversationId agent-spec))
                         (nonblank (:parent-conversation-id agent-spec))
                         (nonblank (:parent_conversation_id agent-spec)))
     :session-id (or (nonblank (:parentSessionId metadata))
                     (nonblank (:parent-session-id metadata))
                     (nonblank (:parent_session_id metadata))
                     (nonblank (:parentSessionId agent-spec))
                     (nonblank (:parent-session-id agent-spec))
                     (nonblank (:parent_session_id agent-spec)))
     :run-id (or (nonblank (:parentRunId metadata))
                 (nonblank (:parent-run-id metadata))
                 (nonblank (:parent_run_id metadata))
                 (nonblank (:parentRunId agent-spec))
                 (nonblank (:parent-run-id agent-spec))
                 (nonblank (:parent_run_id agent-spec)))}))

(defn- resolve-target-sync
  [params metadata]
  (let [ctx (current-context)
        target (nonblank (or (aget params "target") "self"))
        type-hint (normalize-target-type (aget params "target_type"))
        parsed (prefixed-target target)
        target-type (or (:type parsed) type-hint)
        target-id (or (:id parsed) (when-not (#{"parent" "self" "event"} target) target))
        explicit-conversation-id (nonblank (or (aget params "conversation_id")
                                               (aget params "conversationId")))
        explicit-session-id (nonblank (or (aget params "session_id")
                                          (aget params "sessionId")))
        explicit-run-id (nonblank (or (aget params "run_id")
                                      (aget params "runId")))
        parent (parent-context ctx metadata)]
    (cond-> {:target target
             :target-type target-type
             :conversation-id explicit-conversation-id
             :session-id explicit-session-id
             :run-id (or explicit-run-id (:run-id ctx))}
      (= target "self")
      (merge {:conversation-id (or explicit-conversation-id (:conversation-id ctx))
              :session-id (or explicit-session-id (:session-id ctx))
              :run-id (or explicit-run-id (:run-id ctx))})

      (= target "parent")
      (merge {:conversation-id (or explicit-conversation-id (:conversation-id parent))
              :session-id (or explicit-session-id (:session-id parent))
              :run-id (or explicit-run-id (:run-id parent))})

      (#{"conversation" "conversation-id" "conversationid"} target-type)
      (merge {:conversation-id (or explicit-conversation-id target-id)})

      (#{"session" "session-id" "sessionid"} target-type)
      (merge {:session-id (or explicit-session-id target-id)})

      (#{"actor" "actor-id" "actorid"} target-type)
      (merge {:actor-id target-id})

      (= "event" target-type)
      (merge {:event-target target-id}))))

(defn- resolve-session-conversation!
  [target]
  (let [session-id (:session-id target)
        conversation-id (:conversation-id target)]
    (if (or conversation-id (str/blank? (str session-id)))
      (js/Promise.resolve target)
      (-> (session-store/get-session (redis/get-client) session-id)
          (.then (fn [session]
                   (assoc target :conversation-id (or (:conversation_id session)
                                                      (:conversation-id session)))))))))

(defn- delivery-mode
  [mode]
  (if (= "message" mode) "follow-up" mode))

(defn- control-path
  [mode]
  (case (delivery-mode mode)
    "steer" "/api/knoxx/steer"
    "follow-up" "/api/knoxx/follow-up"
    "/api/knoxx/follow-up"))

(defn- event-payload
  [target content metadata current mailbox-id]
  (cond-> {:sourceKind "actor"
           :eventKind "actors.message"
           :payload {:target (:target target)
                     :targetType (:target-type target)
                     :actorId (:actor-id target)
                     :conversationId (:conversation-id target)
                     :sessionId (:session-id target)
                     :runId (:run-id target)
                     :sourceConversationId (:conversation-id current)
                     :sourceSessionId (:session-id current)
                     :sourceRunId (:run-id current)
                     :mailboxId mailbox-id
                     :content content
                     :metadata metadata}}
    (not (str/blank? (str mailbox-id)))
    (assoc :id (actor-mailbox/mailbox-event-id mailbox-id))))

(defn- send-event!
  [config target content metadata mailbox-id]
  (fetch-json! config "POST" "/api/admin/config/events/dispatch"
               (event-payload target content metadata (current-context) mailbox-id)))

(defn- send-control!
  [config target mode content metadata]
  (let [conversation-id (:conversation-id target)
        session-id (:session-id target)
        run-id (:run-id target)]
    (cond
      (str/blank? (str conversation-id))
      (js/Promise.reject (js/Error. "conversation_id is required for steer/follow-up actor messages"))

      (str/blank? (str session-id))
      (js/Promise.reject (js/Error. "session_id is required for steer/follow-up actor messages"))

      :else
      (fetch-json! config "POST" (control-path mode)
                   {:message content
                    :conversation_id conversation-id
                    :session_id session-id
                    :run_id run-id
                    :metadata metadata}))))

(defn- resolve-actor-route!
  [runtime target]
  (if (and (:actor-id target)
           (str/blank? (str (:conversation-id target)))
           (str/blank? (str (:session-id target))))
    (-> (actor-mailbox/resolve-actor-session! runtime (:actor-id target))
        (.then (fn [route]
                 (if route
                   (merge target {:conversation-id (:conversation-id route)
                                  :session-id (:session-id route)
                                  :run-id (or (:run-id target) (:run-id route))
                                  :resolved-actor-route route})
                   target))))
    (js/Promise.resolve target)))

(defn- mailbox-delivery-mode
  [mode]
  (if (= "event" mode)
    "event"
    (delivery-mode mode)))

(defn- create-mailbox-entry!
  [runtime target mode content metadata]
  (let [current (current-context)]
    (actor-mailbox/create-entry!
     runtime
     {:kind "actor-message"
      :status "pending"
      :source (actor-mailbox/source-from-context current)
      :target target
      :delivery-mode (mailbox-delivery-mode mode)
      :content-ref (cond-> {}
                     (:run-id target) (assoc :target-run-id (:run-id target))
                     (:run-id current) (assoc :source-run-id (:run-id current)))
      :metadata metadata
      :preview content})))

(defn- mailbox-id
  [entry]
  (:mailbox/id entry))

(defn- delivery-content-ref
  [mode result mailbox-id]
  (cond-> {:delivery-mode (mailbox-delivery-mode mode)}
    (= "event" mode) (assoc :event-id (actor-mailbox/mailbox-event-id mailbox-id))
    (map? result) (assoc :result result)))

(defn- mark-failed-and-rethrow!
  [runtime mailbox-id err]
  (-> (actor-mailbox/mark-failed! runtime mailbox-id err)
      (.catch (fn [_] nil))
      (.then (fn [_]
               (throw err)))))

(defn actors-send-message-execute
  [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        content (or (aget params "content") "")
        mode (normalize-mode (aget params "mode"))
        metadata (parse-metadata (or (aget params "metadata_json")
                                     (aget params "metadataJson")))
        target (resolve-target-sync params metadata)]
    (when (str/blank? (str content))
      (throw (js/Error. "content is required")))
    (maybe-tool-update! on-update (str "Sending actor message via " mode "…"))
    (-> (resolve-session-conversation! target)
        (.then (fn [session-target]
                 (resolve-actor-route! runtime session-target)))
        (.then (fn [resolved-target]
                 (-> (create-mailbox-entry! runtime resolved-target mode content metadata)
                     (.then (fn [entry]
                              (let [mailbox-id (mailbox-id entry)
                                    metadata* (assoc metadata :mailboxId mailbox-id)]
                                (-> (if (= "event" mode)
                                      (send-event! config resolved-target content metadata* mailbox-id)
                                      (send-control! config resolved-target mode content metadata*))
                                    (.then (fn [result]
                                             (let [result* (js->clj result :keywordize-keys true)]
                                               (-> (actor-mailbox/mark-delivered!
                                                    runtime mailbox-id
                                                    (delivery-content-ref mode result* mailbox-id))
                                                   (.catch (fn [_] nil))
                                                   (.then (fn [_]
                                                            {:entry entry
                                                             :target resolved-target
                                                             :result result*}))))))
                                    (.catch (fn [err]
                                              (mark-failed-and-rethrow! runtime mailbox-id err))))))))))
        (.then (fn [{:keys [entry target result]}]
                 (let [summary (str "Sent actor message to " (:target target)
                                    " via " (if (= "message" mode) "follow-up" mode))]
                   (tool-text-result summary
                                     {:ok true
                                      :tool "actors.send-message"
                                      :mode mode
                                      :mailbox_id (mailbox-id entry)
                                      :mailbox_durable (boolean (:mailbox/durable? entry))
                                      :target target
                                      :result result})))))))

(def actors-send-message-tool
  (partial create-tool-obj
           "actors.send-message"
           "Actors Send Message"
           "Send an actor-to-actor message to a parent, sibling, session, conversation, actor target, or event queue."
           "Route asynchronous child-agent progress or results to another actor/session as steer, follow-up, or event while preserving lineage metadata."
           ["Use target=parent from sub-agents when parentConversationId/parentSessionId is present in metadata or agent context."
            "Use mode=follow-up for busy targets; use mode=steer only for immediate interruption/steering."
            "Use mode=event when no live session target is available but an auditable mailbox-style event should be emitted."
            "Pass metadata_json with parentRunId, subAgentId, resultKey, or other lineage labels for auditability."]
           send-message-params
           actors-send-message-execute))

(defn create-actors-custom-tools
  ([runtime config] (create-actors-custom-tools runtime config nil))
  ([runtime config auth-context]
   (if (or (nil? auth-context)
           (ctx-tool-allowed? auth-context "actors.send-message"))
     (clj->js [(actors-send-message-tool runtime config)])
     (clj->js []))))
