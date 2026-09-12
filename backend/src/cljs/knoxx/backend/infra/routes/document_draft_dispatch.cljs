(ns knoxx.backend.infra.routes.document-draft-dispatch
  "Draft-turn ownership, retained settlement and indexed-event dispatch.")

(defn- ^:async settle-draft-generation!
  [draft-complete? release-indexed-event! item _settlement]
  (let [event-id (get-in item [:runtime-event :event/id])]
    (try
      (when-not (await (draft-complete? (:draft/policy item)))
        (await (release-indexed-event! event-id)))
      true
      (catch :default err
        ;; The runner retains the rejected settlement for redelivery. Release
        ;; its dispatch claim now so that retained result can actually be
        ;; retried instead of remaining pinned behind an in-flight event.
        (await (release-indexed-event! event-id))
        (throw err)))))

(defn- ^:async register-draft-terminal-owner!
  [runtime item]
  (when (:draft/needs-generation? item)
    (let [event-id (get-in item [:runtime-event :event/id])
          registration
          (await
           ((:register-turn-settler! runtime)
            event-id
            (fn [settlement]
              (settle-draft-generation!
               (:draft-complete? runtime) (:release-indexed-event! runtime)
               item settlement))))]
      {:event-id event-id
       :registration registration})))

(defn- unregister-draft-terminal-owner!
  [runtime event-id]
  (when event-id
    ((:unregister-turn-settler! runtime) event-id)))

(defn- current-draft-owner-state
  [runtime event-id]
  ((:draft-event-owner-state runtime) event-id))

(defn- current-indexed-event-state
  [runtime event-id]
  ((:indexed-event-state runtime) event-id))

(defn- ^:async release-stale-completed-draft!
  [runtime item event-id owner-state dispatch-state]
  (when (and (:draft/needs-generation? item)
             (= :completed dispatch-state)
             (nil? owner-state))
    (await ((:release-indexed-event! runtime) event-id))))

(defn- pending-settlement-redelivery?
  [owner-state registration]
  (and (= :settled owner-state)
       (true? (:event-turn/redelivered? registration))
       (false? (:event-turn/redelivery-accepted? registration))))

(defn- live-draft-owner-result
  []
  {:matchedTriggers []
   :skipped true
   :dedup/status :in-flight
   :draft-owner/existing? true})

(defn- ^:async prepare-draft-owner!
  [runtime item]
  (let [event-id (get-in item [:runtime-event :event/id])
        generation? (:draft/needs-generation? item)
        owner-state (when generation?
                      (current-draft-owner-state runtime event-id))
        dispatch-state (when generation?
                         (current-indexed-event-state runtime event-id))]
    (await (release-stale-completed-draft!
            runtime item event-id owner-state dispatch-state))
    (if (= :in-flight owner-state)
      {:result (live-draft-owner-result)}
      (let [{:keys [registration] :as owner}
            (await (register-draft-terminal-owner! runtime item))]
        (if (pending-settlement-redelivery? owner-state registration)
          ;; The prior turn is terminal and no replacement was enqueued. Keep
          ;; its cached settlement for a later retry, but fail this admission;
          ;; calling it in-flight would let deployment pass with no draft owner.
          (throw
           (ex-info "publication post draft settlement could not be reconciled"
                    {:status 503
                     :code "document_post_draft_settlement_redelivery_failed"
                     :document/id (get-in item [:document :document/id])
                     :event/id event-id}))
          owner)))))

(defn- ^:async emit-runtime-indexed-event!
  [runtime item document-id event-id]
  (try
    (await ((:emit-indexed! runtime) (:runtime-event item)))
    (catch :default err
      (unregister-draft-terminal-owner! runtime event-id)
      (throw
       (ex-info "publication document indexed event dispatch failed"
                {:status 500
                 :code "document_indexed_dispatch_failed"
                 :document/id document-id
                 :event/id (get-in item [:indexed-event :id])}
                err)))))

(defn- assert-draft-dispatch!
  [runtime item document-id event-id result]
  (when (and (:draft/needs-generation? item)
             (empty? (:matchedTriggers result))
             (not (and (:skipped result)
                       (= :in-flight (:dedup/status result)))))
    (unregister-draft-terminal-owner! runtime event-id)
    (throw
     (ex-info (if (:skipped result)
                "publication post draft dispatch is stale"
                "publication post draft trigger is not enabled")
              {:status 503
               :code (if (:skipped result)
                       "document_post_draft_dispatch_stale"
                       "document_post_draft_trigger_missing")
               :document/id document-id
               :event/id (get-in item [:indexed-event :id])
               :dedup/status (:dedup/status result)})))
  result)

(defn ^:async emit-indexed-event!
  "Own draft settlement and dispatch the indexed event once its owner is ready."
  [runtime item document-id]
  (let [{:keys [event-id result]} (await (prepare-draft-owner! runtime item))
        result (or result
                   (await (emit-runtime-indexed-event!
                           runtime item document-id event-id)))]
    (assert-draft-dispatch! runtime item document-id event-id result)))
