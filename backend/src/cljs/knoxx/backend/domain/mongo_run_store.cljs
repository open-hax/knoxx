(ns knoxx.backend.domain.mongo-run-store
  "Validate durable Mongo snapshots and refuse ambiguous legacy event histories."
  (:require [knoxx.backend.domain.run-store :as run]
            [knoxx.backend.law.run-store :as law]
            [knoxx.backend.shape.session-persistence :as contract]))

(defn- corrupt! []
  (throw (ex-info "Invalid durable Mongo run state"
                  {:status 503 :code "run_store_corrupt"})))

(defn- validate-events! [events binding]
  (when-not (vector? events) (corrupt!))
  (reduce (fn [ids [index event]]
            (law/require! law/Event event)
            (law/require! law/NonBlank (:event_id event))
            (when-not (and (= (inc index) (:sequence event))
                            (not (contains? ids (:event_id event)))
                            (= (select-keys binding [:run_id :session_id :conversation_id])
                               (select-keys event [:run_id :session_id :conversation_id])))
              (corrupt!))
            (conj ids (:event_id event))) #{} (map-indexed vector events)))

(defn validate-state!
  "Require one run's immutable binding, contiguous sequence and unique event identities."
  [state run-id]
  (when-not (and (= #{:runs :events :bindings} (set (keys state)))
                  (every? map? (vals state))
                  (every? #(every? #{run-id} (keys %)) (vals state)))
    (corrupt!))
  (let [binding (get-in state [:bindings run-id])
        entry (get-in state [:runs run-id])]
    (when binding
      (doseq [key [:run_id :session_id :conversation_id]]
        (law/require! law/NonBlank (get binding key)))
      (when-not (= run-id (:run_id binding)) (corrupt!)))
    (when entry
      (contract/assert-run! (:run entry) "MongoRunStore/replay")
      (law/require! law/Milliseconds (:expires-ms entry))
      (when-not (= binding (select-keys (:run entry) run/identity-fields)) (corrupt!)))
    (validate-events! (get-in state [:events run-id] []) binding))
  state)

(defn restore
  "Decode a validated snapshot or migrate an empty legacy event history without guessing order."
  [record run-id]
  (let [state (cond
                (nil? record) run/empty-state
                (:state record) (:state record)
                :else
                (let [legacy (:legacy-run record)]
                  (when (or (seq (:run_events legacy)) (seq (:events legacy)))
                    (throw (ex-info "Legacy Mongo events require explicit ordered-history migration"
                                    {:status 503 :code "run_events_migration_required"})))
                  (let [payload (dissoc legacy :run_events :events :sequence)]
                    {:runs {run-id {:run payload :expires-ms (:expires-ms record)}}
                     :events {} :bindings {run-id (select-keys payload run/identity-fields)}})))]
    (validate-state! state run-id)))
