(ns knoxx.backend.infra.translation-event-writer
  "Runtime projection of durable translation facts into OpenPlanner events.

  A receipt and candidate set are already immutable and the pure builder
  produces stable event ids. OpenPlanner's event collection does not yet
  enforce uniqueness for those ids, so this boundary serializes each complete
  process-local check/append/repair operation and appends only missing events.
  Equal `save_translation` replay in one Knoxx process is therefore a repair
  path rather than a source of duplicate event rows."
  (:require [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-event :as event]))

(def ^:private durable-translation-extra-keys
  [:source_lang :target_lang :source_text :mt_model :status])

(defonce ^:private candidate-event-tail* (atom nil))

(defn- ^:async run-after-candidate-event!
  [previous task-fn]
  (when previous
    (try
      (await previous)
      ;; knoxx-lint/allow-silent-catch — an earlier queue failure must not poison later writes.
      (catch :default _
        nil)))
  (await (task-fn)))

(defn- ^:async recover-candidate-event-tail!
  [task]
  (try
    (await task)
    ;; knoxx-lint/allow-silent-catch — only the stored recovery tail consumes this rejection.
    (catch :default _
      nil)))

(defn- enqueue-candidate-event!
  [task-fn]
  (let [task (run-after-candidate-event! @candidate-event-tail* task-fn)]
    (reset! candidate-event-tail* (recover-candidate-event-tail! task))
    task))

(defn- ^:async ensure-durable-event-extra!
  [client events]
  (loop [remaining events
         results []]
    (if-let [event (first remaining)]
      (let [required (select-keys (:extra event)
                                  durable-translation-extra-keys)]
        (when-not (= (count durable-translation-extra-keys) (count required))
          (throw (ex-info "translation event lacks durable query metadata"
                          {:status 500
                           :code "translation_event_extra_incomplete"
                           :event-id (:id event)})))
        (recur (next remaining)
               (conj results
                     (await (openplanner-client/ensure-event-extra-fields!
                             client (:id event) required)))))
      results)))

(defn- ^:async emit-candidate-events-once!
  [client {:keys [receipt turn candidate-set]}]
  (let [events (event/candidate-events crypto/sha256-hex receipt turn candidate-set)
        ids (mapv :id events)
        existing (await
                  (openplanner-client/mongo-query!
                   client
                   {:collection "events"
                    :filter {:id {:$in ids}}
                    :projection {:id 1}
                    :limit (count ids)}))
        existing-id-set (into #{} (keep :id) (:rows existing))
        existing-ids (filterv #(contains? existing-id-set %) ids)
        missing (filterv #(not (contains? existing-id-set (:id %))) events)
        result (if (empty? missing)
                 {:ok true :count 0 :ids ids :existing true}
                 (await
                  (openplanner-client/ingest-events-awaiting-projections!
                   client missing)))
        extra-results (await (ensure-durable-event-extra! client events))
        ;; Ensure the full id set after append so both a new detached embedding
        ;; and an idempotent replay of an incomplete projection are repaired.
        indexed (await (openplanner-client/ensure-event-vectors! client ids))]
    {:translation/event-ids (mapv :id events)
     :translation/event-existing-ids existing-ids
     :translation/event-recorded-ids (mapv :id missing)
     :translation/event-result result
     :translation/event-extra-results extra-results
     :translation/event-index-result indexed}))

(defn ^:async emit-candidate-events!
  "Append each stable candidate event only when its id is not already durable.

  A failure-recovering process-wide tail covers the complete stable-event
  operation so live sinks and admission repair cannot race their read-before-
  append checks within one Knoxx process. Multi-process uniqueness still
  requires an OpenPlanner-owned data migration or atomic append contract.

  The selected client must expose event-projection repair. REST currently does
  not, so it is rejected before any query or append instead of silently mixing
  its event store with the embedded SDK store."
  [client completion]
  (openplanner-client/assert-event-projection-repair-supported! client)
  (await (enqueue-candidate-event!
          #(emit-candidate-events-once! client completion))))

(defn- candidate-backed-receipts
  [receipts]
  (filterv #(some? (:translation/candidate-set-id %)) receipts))

(defn- missing-lineage!
  [kind receipt]
  (throw (ex-info
          (str "completed translation cannot repair events: " kind " is missing")
          {:status 500
           :code "translation_event_repair_lineage_missing"
           :translation-event/missing kind
           :translation/candidate-set-id
           (:translation/candidate-set-id receipt)
           :translation/revision (:translation/revision receipt)})))

(defn- assert-repair-stores!
  [evidence split]
  (when-not evidence
    (throw (ex-info "translation event repair requires an evidence store"
                    {:status 503
                     :code "translation_event_repair_evidence_store_missing"})))
  (when-not split
    (throw (ex-info "translation event repair requires a split store"
                    {:status 503
                     :code "translation_event_repair_split_store_missing"}))))

(defn- ^:async repair-receipt!
  [client split receipt]
  (let [candidate-set-id (:translation/candidate-set-id receipt)
        candidate-set (or (await (split-store/candidate-set-by-id!
                                  split candidate-set-id))
                          (missing-lineage! "candidate set" receipt))
        turn (or (await (split-store/turn-for-candidate-set!
                         split candidate-set-id))
                 (missing-lineage! "owning turn" receipt))
        result (await (emit-candidate-events!
                       client
                       {:receipt receipt :turn turn :candidate-set candidate-set}))]
    {:translation/candidate-set-id candidate-set-id
     :translation/event-ids (:translation/event-ids result)
     :translation/event-existing-ids (:translation/event-existing-ids result)
     :translation/event-recorded-ids (:translation/event-recorded-ids result)}))

(defn- repair-summary
  [receipt-count skipped-count results]
  {:translation/event-repair-receipt-count receipt-count
   :translation/event-repair-skipped-count skipped-count
   :translation/event-repair-recorded-count
   (reduce + 0 (map #(count (:translation/event-recorded-ids %)) results))
   :translation/event-repair-existing-count
   (reduce + 0 (map #(count (:translation/event-existing-ids %)) results))
   :translation/event-repair-results results})

(defn ^:async repair-completed-event-projections!
  "Rebuild missing candidate events from durable completed receipts.

  This seam is intentionally independent of the agent tool replay path. A
  later admission/dispatch pass can call it with the tenant-scoped evidence and
  split stores plus `{:org-id ... :project ...}`. Legacy worker receipts without
  split lineage are reported as skipped; every split-backed receipt must still
  resolve its immutable candidate set and owning turn or repair fails visibly."
  [{:keys [evidence-store split-store] :as dependencies} scope]
  (assert-repair-stores! evidence-store split-store)
  (let [client (:openplanner-client dependencies)]
    (openplanner-client/assert-event-projection-repair-supported! client)
    (let [receipts (vec (await (evidence-store/completed-translations!
                                evidence-store scope)))
          repairable (candidate-backed-receipts receipts)]
      (loop [remaining repairable
             results []]
        (if-let [receipt (first remaining)]
          (recur (next remaining)
                 (conj results
                       (await (repair-receipt!
                               client split-store receipt))))
          (repair-summary (count repairable)
                          (- (count receipts) (count repairable))
                          results))))))
