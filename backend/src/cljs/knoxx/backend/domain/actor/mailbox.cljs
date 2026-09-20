(ns knoxx.backend.domain.actor.mailbox
  "Pure actor mailbox addressing and message-reference vocabulary."
  (:require [clojure.string :as str] [knoxx.backend.law.mailbox-store :as law]))
(def mailbox-statuses #{"pending" "delivered" "failed" "expired" "superseded" "acknowledged"})
(def mailbox-delivery-modes #{"steer" "follow-up" "event" "inbox-only" "direct-run"})
(defn nonblank [value] (some-> value str str/trim not-empty))
(defn normalize-status [status]
  (let [status* (some-> status str str/trim str/lower-case (str/replace #"_" "-"))]
    (if (contains? mailbox-statuses status*) status* "pending")))
(defn normalize-delivery-mode [mode]
  (let [mode* (some-> mode str str/trim str/lower-case (str/replace #"_" "-"))]
    (case mode* "message" "follow-up" "followup" "follow-up"
      (if (contains? mailbox-delivery-modes mode*) mode* "follow-up"))))
(defn preview-text [content]
  (let [text (str (or content ""))] (if (> (count text) 240) (str (subs text 0 240) "…") text)))
(defn mailbox-event-id [mailbox-id] (str "actor-mailbox-" mailbox-id))
(defn source-from-context [ctx]
  (let [agent-spec (:agent-spec ctx)
        actor-id (or (nonblank (:actor-id ctx)) (nonblank (:actor-id agent-spec)) (nonblank (:actorId agent-spec)) (nonblank (:actor_id agent-spec)))
        contract-id (or (nonblank (:contract-id ctx)) (nonblank (:contract-id agent-spec)) (nonblank (:contractId agent-spec)) (nonblank (:contract_id agent-spec)))]
    (cond-> {} actor-id (assoc :actor-id actor-id)
      (nonblank (:session-id ctx)) (assoc :session-id (nonblank (:session-id ctx)))
      (nonblank (:conversation-id ctx)) (assoc :conversation-id (nonblank (:conversation-id ctx)))
      (nonblank (:run-id ctx)) (assoc :run-id (nonblank (:run-id ctx))) contract-id (assoc :contract-id contract-id))))
(defn normalize-target-map [target]
  (let [kind (or (nonblank (:target-type target))
                 (cond (nonblank (:actor-id target)) "actor" (nonblank (:session-id target)) "session"
                       (nonblank (:conversation-id target)) "conversation" :else nil)
                 (nonblank (:target target)) "unknown")]
    (cond-> {:kind kind}
      (nonblank (:actor-id target)) (assoc :actor-id (nonblank (:actor-id target)))
      (nonblank (:session-id target)) (assoc :session-id (nonblank (:session-id target)))
      (nonblank (:conversation-id target)) (assoc :conversation-id (nonblank (:conversation-id target)))
      (nonblank (:run-id target)) (assoc :run-id (nonblank (:run-id target)))
      (nonblank (:target target)) (assoc :address (nonblank (:target target))))))
(defn mailbox-entry
  [{:keys [id kind status source target delivery-mode content-ref metadata preview attempts next-at expires-at]}]
  (let [entry-id (or (nonblank id) (law/refuse! 400 "mailbox_id_required" "A mailbox identity must be supplied"))]
    (cond-> {:mailbox/id entry-id :mailbox/kind (or (nonblank kind) "actor-message") :mailbox/status (normalize-status status)
             :mailbox/source (or source {}) :mailbox/target (normalize-target-map (or target {}))
             :mailbox/delivery {:mode (normalize-delivery-mode delivery-mode) :attempts (or attempts 0)}
             :mailbox/content-ref (or content-ref {}) :mailbox/metadata (or metadata {})}
      (nonblank preview) (assoc :mailbox/preview (preview-text preview))
      (nonblank next-at) (assoc-in [:mailbox/delivery :next-at] (nonblank next-at))
      (nonblank expires-at) (assoc :mailbox/expires-at (nonblank expires-at)))))
(defn retry-request-event
  "A stable delivery attempt references canonical content instead of copying a transcript."
  [entry]
  {:id (str (mailbox-event-id (:mailbox/id entry)) "-retry-"
            (or (get-in entry [:mailbox/delivery :claim-id]) (get-in entry [:mailbox/delivery :attempts])))
   :sourceKind "actor" :eventKind "actors.mailbox.retry-requested" :orgId (:mailbox/org-id entry)
   :payload {:mailboxId (:mailbox/id entry) :status (:mailbox/status entry) :target (:mailbox/target entry)
             :source (:mailbox/source entry) :delivery (:mailbox/delivery entry)
             :contentRef (:mailbox/content-ref entry) :metadata (:mailbox/metadata entry) :preview (:mailbox/preview entry)}})
