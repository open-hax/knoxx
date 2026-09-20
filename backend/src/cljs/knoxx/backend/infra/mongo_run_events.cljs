(ns knoxx.backend.infra.mongo-run-events
  "Resolve event authority only through the currently committed single-run head."
  (:require [knoxx.backend.extern.mongo-run-events :as mongo]
            [knoxx.backend.law.mongo-run-events :as law]
            [knoxx.backend.law.run-store :as run]))

(defn- ^:async accepted-event! [db binding sequence header]
  (law/stored-event! binding sequence (await (mongo/read-event! db header))))

(defn ^:async existing!
  "Use indexed absence for new IDs; a candidate hit requires accepted-chain membership proof."
  [db record state run-id event-id]
  (if-let [chain (:event-chain record)]
    (let [run-key (mongo/identity-key run-id) event-key (mongo/identity-key event-id)
          binding (get-in state [:bindings run-id])]
      (when (pos? (:last-sequence chain))
        (await (mongo/read-header! db (:tail chain) run-key (:last-sequence chain))))
      (when (await (mongo/candidate-exists? db run-key event-key))
        (loop [reference (:tail chain) sequence (:last-sequence chain)]
          (when (pos? sequence)
            (let [header (await (mongo/read-header! db reference run-key sequence))
                  candidate (when (= event-key (:event-key header))
                              (await (accepted-event! db binding sequence header)))]
              (if (= event-id (:event_id candidate)) candidate
                (recur (:previous header) (dec sequence))))))))
    (some #(when (= event-id (:event_id %)) %) (get-in state [:events run-id]))))

(defn ^:async prepare-history!
  "Preserve v2 references or prepare fully validated old ordered events before the head CAS."
  [db record state run-id]
  (if-let [chain (:event-chain record)] chain
    (loop [history (seq (get-in state [:events run-id])) chain law/empty-chain]
      (if-let [event (first history)]
        (recur (next history) (await (mongo/prepare! db event (:tail chain))))
        chain))))

(defn ^:async replay!
  "Read only accepted links in exact descending sequence, then return the requested chronological view.
   Numeric cursors stop at their suffix; timestamp cursors may inspect the complete history."
  [db binding chain since]
  (run/require! [:or :nil [:int {:min 0}] run/Instant] since)
  (law/chain! chain)
  (let [run-key (mongo/identity-key (:run_id binding))]
    (loop [reference (:tail chain) sequence (:last-sequence chain) result []]
      (if (or (zero? sequence) (and (number? since) (<= sequence since)))
        (vec (rseq result))
        (let [header (await (mongo/read-header! db reference run-key sequence))
              event (await (accepted-event! db binding sequence header))]
          (recur (:previous header) (dec sequence)
                 (cond-> result
                   (or (nil? since) (number? since) (pos? (compare (:at event) since))) (conj event))))))))
