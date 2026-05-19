(ns knoxx.backend.domain.actor.mailbox
  "Durable actor mailbox delivery ledger/projection.

   OpenPlanner/session/run events remain the canonical content history. This
   namespace stores delivery state, routing metadata, and content references so
   actors can address live or later-resolved targets without creating a second
   message transcript."
  (:require [clojure.string :as str]
            [knoxx.backend.runtime.state :as runtime-state]
            ["node:crypto" :as crypto]))

(def mailbox-statuses
  #{"pending" "delivered" "failed" "expired" "superseded" "acknowledged"})

(def mailbox-delivery-modes
  #{"steer" "follow-up" "event" "inbox-only" "direct-run"})

(defn nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn normalize-status
  [status]
  (let [status* (some-> status str str/trim str/lower-case (str/replace #"_" "-"))]
    (if (contains? mailbox-statuses status*)
      status*
      "pending")))

(defn normalize-delivery-mode
  [mode]
  (let [mode* (some-> mode str str/trim str/lower-case (str/replace #"_" "-"))]
    (case mode*
      "message" "follow-up"
      "followup" "follow-up"
      (if (contains? mailbox-delivery-modes mode*) mode* "follow-up"))))

(defn preview-text
  [content]
  (let [text (str (or content ""))]
    (if (> (count text) 240)
      (str (subs text 0 240) "…")
      text)))

(defn mailbox-event-id
  [mailbox-id]
  (str "actor-mailbox-" mailbox-id))

(defn new-mailbox-id
  []
  (.randomUUID crypto))

(defn source-from-context
  [ctx]
  (let [agent-spec (:agent-spec ctx)
        actor-id (or (nonblank (:actor-id ctx))
                     (nonblank (:actor-id agent-spec))
                     (nonblank (:actorId agent-spec))
                     (nonblank (:actor_id agent-spec)))
        contract-id (or (nonblank (:contract-id ctx))
                        (nonblank (:contract-id agent-spec))
                        (nonblank (:contractId agent-spec))
                        (nonblank (:contract_id agent-spec)))]
    (cond-> {}
      actor-id (assoc :actor-id actor-id)
      (nonblank (:session-id ctx)) (assoc :session-id (nonblank (:session-id ctx)))
      (nonblank (:conversation-id ctx)) (assoc :conversation-id (nonblank (:conversation-id ctx)))
      (nonblank (:run-id ctx)) (assoc :run-id (nonblank (:run-id ctx)))
      contract-id (assoc :contract-id contract-id))))

(defn normalize-target-map
  [target]
  (let [kind (or (nonblank (:target-type target))
                 (cond
                   (nonblank (:actor-id target)) "actor"
                   (nonblank (:session-id target)) "session"
                   (nonblank (:conversation-id target)) "conversation"
                   :else nil)
                 (nonblank (:target target))
                 "unknown")]
    (cond-> {:kind kind}
      (nonblank (:actor-id target)) (assoc :actor-id (nonblank (:actor-id target)))
      (nonblank (:session-id target)) (assoc :session-id (nonblank (:session-id target)))
      (nonblank (:conversation-id target)) (assoc :conversation-id (nonblank (:conversation-id target)))
      (nonblank (:run-id target)) (assoc :run-id (nonblank (:run-id target)))
      (nonblank (:target target)) (assoc :address (nonblank (:target target))))))

(defn mailbox-entry
  [{:keys [id kind status source target delivery-mode content-ref metadata preview attempts next-at expires-at]}]
  (let [entry-id (or (nonblank id) (new-mailbox-id))]
    (cond-> {:mailbox/id entry-id
             :mailbox/kind (or (nonblank kind) "actor-message")
             :mailbox/status (normalize-status status)
             :mailbox/source (or source {})
             :mailbox/target (normalize-target-map (or target {}))
             :mailbox/delivery {:mode (normalize-delivery-mode delivery-mode)
                                :attempts (or attempts 0)}
             :mailbox/content-ref (or content-ref {})
             :mailbox/metadata (or metadata {})}
      (nonblank preview) (assoc :mailbox/preview (preview-text preview))
      (nonblank next-at) (assoc-in [:mailbox/delivery :next-at] (nonblank next-at))
      (nonblank expires-at) (assoc :mailbox/expires-at (nonblank expires-at)))))

(defn- policy-db
  [runtime]
  (or (when runtime (aget runtime "policyDb"))
      (runtime-state/current-policy-db)))

(defn database-enabled?
  [runtime]
  (boolean (and (policy-db runtime)
                (fn? (aget (policy-db runtime) "query")))))

(defn- query!
  [runtime sql params]
  (if-let [db (policy-db runtime)]
    (if-let [query-fn (aget db "query")]
      (query-fn sql (clj->js params))
      (js/Promise.resolve nil))
    (js/Promise.resolve nil)))

(defn- json-param
  [value]
  (.stringify js/JSON (clj->js (or value {}))))

(defn- rows
  [result]
  (let [rows* (when result (aget result "rows"))]
    (if (array? rows*)
      (array-seq rows*)
      [])))

(defn- row->route
  [row]
  (when row
    {:actor-id (aget row "actor_id")
     :conversation-id (aget row "conversation_id")
     :session-id (aget row "session_id")
     :run-id (aget row "run_id")
     :contract-id (aget row "contract_id")
     :status (aget row "status")
     :last-seen-at (aget row "last_seen_at")}))

(defn register-live-session!
  [runtime {:keys [actor-id conversation-id session-id run-id contract-id source expires-at]}]
  (if-let [actor-id* (nonblank actor-id)]
    (query! runtime
            "INSERT INTO actor_mailbox_routes (actor_id, conversation_id, session_id, run_id, contract_id, status, source_json, expires_at, last_seen_at)
             VALUES ($1, $2, $3, $4, $5, 'active', $6::jsonb, COALESCE($7::timestamptz, NOW() + interval '30 minutes'), NOW())
             ON CONFLICT (actor_id) DO UPDATE SET
               conversation_id = EXCLUDED.conversation_id,
               session_id = EXCLUDED.session_id,
               run_id = EXCLUDED.run_id,
               contract_id = EXCLUDED.contract_id,
               status = 'active',
               source_json = EXCLUDED.source_json,
               expires_at = EXCLUDED.expires_at,
               last_seen_at = NOW()
             RETURNING *"
            [actor-id* (nonblank conversation-id) (nonblank session-id) (nonblank run-id)
             (nonblank contract-id) (json-param source) (nonblank expires-at)])
    (js/Promise.resolve nil)))

(defn unregister-live-session!
  [runtime conversation-id]
  (if-let [conversation-id* (nonblank conversation-id)]
    (query! runtime
            "UPDATE actor_mailbox_routes
             SET status = 'inactive', last_seen_at = NOW()
             WHERE conversation_id = $1"
            [conversation-id*])
    (js/Promise.resolve nil)))

(defn resolve-actor-session!
  [runtime actor-id]
  (if-let [actor-id* (nonblank actor-id)]
    (-> (query! runtime
                "SELECT * FROM actor_mailbox_routes
                 WHERE actor_id = $1
                   AND status = 'active'
                   AND (expires_at IS NULL OR expires_at > NOW())
                 ORDER BY last_seen_at DESC
                 LIMIT 1"
                [actor-id*])
        (.then (fn [result]
                 (some-> (first (rows result)) row->route))))
    (js/Promise.resolve nil)))

(defn create-entry!
  [runtime raw-entry]
  (let [entry (mailbox-entry raw-entry)
        target (:mailbox/target entry)
        source (:mailbox/source entry)
        delivery (:mailbox/delivery entry)]
    (if-not (database-enabled? runtime)
      (js/Promise.resolve (assoc entry :mailbox/durable? false))
      (-> (query! runtime
                  "INSERT INTO actor_mailbox_entries
                   (id, kind, status,
                    source_actor_id, source_session_id, source_conversation_id, source_run_id, source_json,
                    target_kind, target_actor_id, target_session_id, target_conversation_id, target_run_id, target_json,
                    delivery_mode, attempts, next_at, expires_at,
                    content_ref_json, metadata_json, preview)
                   VALUES
                   ($1::uuid, $2, $3,
                    $4, $5, $6, $7, $8::jsonb,
                    $9, $10, $11, $12, $13, $14::jsonb,
                    $15, $16, $17::timestamptz, $18::timestamptz,
                    $19::jsonb, $20::jsonb, $21)
                   RETURNING *"
                  [(:mailbox/id entry)
                   (:mailbox/kind entry)
                   (:mailbox/status entry)
                   (:actor-id source)
                   (:session-id source)
                   (:conversation-id source)
                   (:run-id source)
                   (json-param source)
                   (:kind target)
                   (:actor-id target)
                   (:session-id target)
                   (:conversation-id target)
                   (:run-id target)
                   (json-param target)
                   (:mode delivery)
                   (:attempts delivery)
                   (:next-at delivery)
                   (:mailbox/expires-at entry)
                   (json-param (:mailbox/content-ref entry))
                   (json-param (:mailbox/metadata entry))
                   (:mailbox/preview entry)])
          (.then (fn [_]
                   (assoc entry :mailbox/durable? true)))))))

(defn mark-delivery!
  [runtime mailbox-id status {:keys [content-ref error attempts next-at]}]
  (if-let [mailbox-id* (nonblank mailbox-id)]
    (if-not (database-enabled? runtime)
      (js/Promise.resolve {:mailbox/id mailbox-id*
                           :mailbox/status (normalize-status status)
                           :mailbox/durable? false})
      (query! runtime
              "UPDATE actor_mailbox_entries
               SET status = $2,
                   attempts = COALESCE($3, attempts),
                   next_at = COALESCE($4::timestamptz, next_at),
                   delivered_at = CASE WHEN $2 = 'delivered' THEN NOW() ELSE delivered_at END,
                   acknowledged_at = CASE WHEN $2 = 'acknowledged' THEN NOW() ELSE acknowledged_at END,
                   content_ref_json = COALESCE($5::jsonb, content_ref_json),
                   last_error = $6,
                   updated_at = NOW()
               WHERE id = $1::uuid"
              [mailbox-id* (normalize-status status) attempts (nonblank next-at)
               (when content-ref (json-param content-ref)) (nonblank error)]))
    (js/Promise.resolve nil)))

(defn mark-delivered!
  [runtime mailbox-id content-ref]
  (mark-delivery! runtime mailbox-id "delivered" {:content-ref content-ref}))

(defn mark-failed!
  [runtime mailbox-id error]
  (mark-delivery! runtime mailbox-id "failed" {:error (str error)}))

(defn- json-value
  [value]
  (cond
    (nil? value) {}
    (string? value) (try (js->clj (.parse js/JSON value) :keywordize-keys true)
                         (catch :default _ {}))
    (array? value) (js->clj value :keywordize-keys true)
    :else (js->clj value :keywordize-keys true)))

(defn row->entry
  [row]
  (when row
    (let [source (json-value (aget row "source_json"))
          target (json-value (aget row "target_json"))
          delivery {:mode (aget row "delivery_mode")
                    :attempts (aget row "attempts")
                    :next-at (aget row "next_at")}
          content-ref (json-value (aget row "content_ref_json"))
          metadata (json-value (aget row "metadata_json"))]
      (cond-> {:mailbox/id (aget row "id")
               :mailbox/kind (aget row "kind")
               :mailbox/status (aget row "status")
               :mailbox/source source
               :mailbox/target target
               :mailbox/delivery delivery
               :mailbox/content-ref content-ref
               :mailbox/metadata metadata
               :mailbox/created-at (aget row "created_at")
               :mailbox/updated-at (aget row "updated_at")}
        (nonblank (aget row "preview")) (assoc :mailbox/preview (aget row "preview"))
        (nonblank (aget row "last_error")) (assoc :mailbox/last-error (aget row "last_error"))
        (nonblank (aget row "delivered_at")) (assoc :mailbox/delivered-at (aget row "delivered_at"))
        (nonblank (aget row "acknowledged_at")) (assoc :mailbox/acknowledged-at (aget row "acknowledged_at"))
        (nonblank (aget row "expires_at")) (assoc :mailbox/expires-at (aget row "expires_at"))))))

(defn- add-filter
  [{:keys [clauses params]} [clause-fn value]]
  (if-let [value* (nonblank value)]
    (let [idx (inc (count params))]
      {:clauses (conj clauses (clause-fn idx))
       :params (conj params value*)})
    {:clauses clauses :params params}))

(defn- positive-int
  [value fallback max-value]
  (let [parsed (js/parseInt (str (or value "")) 10)]
    (if (js/isNaN parsed)
      fallback
      (min max-value (max 1 parsed)))))

(defn list-entries!
  [runtime {:keys [status target-actor-id target-session-id source-actor-id source-run-id limit]}]
  (if-not (database-enabled? runtime)
    (js/Promise.resolve {:entries [] :durable? false})
    (let [{:keys [clauses params]}
          (reduce add-filter
                  {:clauses [] :params []}
                  [[(fn [idx] (str "status = $" idx)) status]
                   [(fn [idx] (str "target_actor_id = $" idx)) target-actor-id]
                   [(fn [idx] (str "target_session_id = $" idx)) target-session-id]
                   [(fn [idx] (str "source_actor_id = $" idx)) source-actor-id]
                   [(fn [idx] (str "source_run_id = $" idx)) source-run-id]])
          limit* (positive-int limit 50 500)
          limit-idx (inc (count params))
          where-sql (if (seq clauses)
                      (str " WHERE " (str/join " AND " clauses))
                      "")]
      (-> (query! runtime
                  (str "SELECT * FROM actor_mailbox_entries"
                       where-sql
                       " ORDER BY created_at DESC LIMIT $" limit-idx)
                  (conj params limit*))
          (.then (fn [result]
                   {:entries (mapv row->entry (rows result))
                    :durable? true}))))))

(defn acknowledge-entry!
  ([runtime mailbox-id]
   (acknowledge-entry! runtime mailbox-id nil))
  ([runtime mailbox-id target-actor-id]
   (if-let [mailbox-id* (nonblank mailbox-id)]
     (if-not (database-enabled? runtime)
       (js/Promise.resolve {:mailbox/id mailbox-id*
                            :mailbox/status "acknowledged"
                            :mailbox/durable? false})
       (-> (query! runtime
                   "UPDATE actor_mailbox_entries
                    SET status = 'acknowledged', acknowledged_at = NOW(), updated_at = NOW()
                    WHERE id = $1::uuid
                      AND ($2::text IS NULL OR target_actor_id = $2)
                    RETURNING *"
                   [mailbox-id* (nonblank target-actor-id)])
           (.then (fn [result]
                    (some-> (first (rows result)) row->entry)))))
     (js/Promise.reject (js/Error. "mailbox id is required")))))

(defn retry-request-event
  [entry]
  {:id (str (mailbox-event-id (:mailbox/id entry)) "-retry-" (.now js/Date))
   :sourceKind "actor"
   :eventKind "actors.mailbox.retry-requested"
   :payload {:mailboxId (:mailbox/id entry)
             :status (:mailbox/status entry)
             :target (:mailbox/target entry)
             :source (:mailbox/source entry)
             :delivery (:mailbox/delivery entry)
             :contentRef (:mailbox/content-ref entry)
             :metadata (:mailbox/metadata entry)
             :preview (:mailbox/preview entry)}})

(defn retry-eligible!
  [runtime {:keys [mailbox-id statuses max-attempts limit delay-seconds]}]
  (if-not (database-enabled? runtime)
    (js/Promise.resolve {:entries [] :durable? false})
    (let [statuses* (or (seq statuses) ["pending" "failed"])
          max-attempts* (positive-int max-attempts 5 100)
          limit* (positive-int limit 25 200)
          delay-seconds* (let [parsed (js/parseInt (str (or delay-seconds 0)) 10)]
                           (if (js/isNaN parsed) 0 (max 0 parsed)))]
      (-> (query! runtime
                  "WITH candidates AS (
                     SELECT id
                     FROM actor_mailbox_entries
                     WHERE status = ANY($1::text[])
                       AND attempts < $2
                       AND (next_at IS NULL OR next_at <= NOW())
                       AND (expires_at IS NULL OR expires_at > NOW())
                       AND ($5::uuid IS NULL OR id = $5::uuid)
                     ORDER BY created_at ASC
                     LIMIT $3
                     FOR UPDATE SKIP LOCKED
                   )
                   UPDATE actor_mailbox_entries m
                   SET status = 'pending',
                       attempts = m.attempts + 1,
                       next_at = CASE WHEN $4::int > 0 THEN NOW() + ($4::text || ' seconds')::interval ELSE NULL END,
                       last_error = NULL,
                       updated_at = NOW()
                   FROM candidates
                   WHERE m.id = candidates.id
                   RETURNING m.*"
                  [statuses* max-attempts* limit* delay-seconds* (nonblank mailbox-id)])
          (.then (fn [result]
                   {:entries (mapv row->entry (rows result))
                    :durable? true}))))))
