(ns knoxx.backend.extern.actor-mailbox
  "Decode mailbox HTTP/tool wire values before shared command validation."
  (:require [clojure.string :as str] [knoxx.backend.law.mailbox-store :as law]))
(defn- metadata-value [body]
  (if-let [encoded (or (:metadata_json body) (:metadataJson body))]
    (try (js->clj (js/JSON.parse encoded) :keywordize-keys true)
         (catch :default _ (law/refuse! 400 "mailbox_metadata_invalid" "Mailbox metadata must be a JSON object")))
    (or (:metadata body) {})))
(defn decode-send
  "Normalize documented wire aliases; arbitrary fields cannot become authority."
  [operation-id raw lineage]
  (let [body (js->clj (or raw #js {}) :keywordize-keys true)
        result {:operation-id (or operation-id (:operation_id body) (:operationId body))
                :target (:target body) :content (:content body) :metadata (metadata-value body) :lineage (or lineage {})}]
    (reduce (fn [result [key aliases]] (if-let [value (some #(get body %) aliases)] (assoc result key value) result))
            (cond-> result (:mode body) (assoc :mode (if (string? (:mode body)) (str/replace (:mode body) "_" "-") (:mode body))))
            {:target-type [:target_type :targetType] :conversation-id [:conversation_id :conversationId]
             :session-id [:session_id :sessionId] :run-id [:run_id :runId]})))
(defn request-send [request] (decode-send nil (aget request "body") nil))
(defn request-id "Read the mailbox route identifier." [request] (aget request "params" "mailboxId"))
(defn- present-values [data] (into {} (remove (comp nil? val)) data))
(defn- integer-value [value] (if (and (string? value) (re-matches #"\d+" value)) (js/Number value) value))
(defn request-filters "Decode only the named mailbox filters." [request]
  (let [query (js->clj (or (aget request "query") #js {}) :keywordize-keys true)]
    (present-values {:status (:status query) :limit (some-> (:limit query) integer-value)
                     :target-actor-id (or (:target_actor_id query) (:targetActorId query) (:actor_id query) (:actorId query))
                     :target-session-id (or (:target_session_id query) (:targetSessionId query) (:session_id query) (:sessionId query))
                     :source-actor-id (or (:source_actor_id query) (:sourceActorId query))
                     :source-run-id (or (:source_run_id query) (:sourceRunId query) (:run_id query) (:runId query))})))
(defn request-box "Only inbox and outbox are supported." [request]
  (let [box (or (aget request "query" "box") "inbox")]
    (when-not (contains? #{"inbox" "outbox"} box) (law/refuse! 400 "mailbox_box_invalid" "Select inbox or outbox")) box))
(defn request-retry "Decode delivery retry options without admitting caller authority." [request]
  (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true) statuses (or (:statuses body) (:status body))]
    {:dispatch? (not= false (:dispatch_events body))
     :options (present-values {:mailbox-id (or (:mailbox_id body) (:mailboxId body))
                               :operation-id (or (:operation_id body) (:operationId body))
                               :statuses (if (string? statuses) [statuses] statuses)
                               :max-attempts (or (:max_attempts body) (:maxAttempts body))
                               :limit (:limit body) :delay-seconds (or (:delay_seconds body) (:delaySeconds body))})}))
(defn entry-wire "Keep qualified persistence keys distinct in JSON." [entry]
  (cond-> {:id (:mailbox/id entry) :orgId (:mailbox/org-id entry) :kind (:mailbox/kind entry) :status (:mailbox/status entry)
           :source (:mailbox/source entry) :target (:mailbox/target entry) :delivery (:mailbox/delivery entry)
           :contentRef (:mailbox/content-ref entry) :metadata (:mailbox/metadata entry) :preview (:mailbox/preview entry)
           :lastError (:mailbox/last-error entry) :durable (true? (:mailbox/durable? entry))
           :createdAt (:mailbox/created-at entry) :updatedAt (:mailbox/updated-at entry)
           :deliveredAt (:mailbox/delivered-at entry) :acknowledgedAt (:mailbox/acknowledged-at entry) :expiresAt (:mailbox/expires-at entry)}
    (contains? entry :mailbox/content) (assoc :content (:mailbox/content entry))))
(defn command-wire "Project one command result without losing qualified identifiers." [result]
  (cond-> result (:entry result) (update :entry entry-wire)))
(defn result-wire "Project list and retry receipts." [result]
  (cond-> (-> result (assoc :entries (mapv entry-wire (:entries result)) :durable (boolean (:durable? result))) (dissoc :durable?))
    (:deliveries result) (update :deliveries #(mapv command-wire %))))
