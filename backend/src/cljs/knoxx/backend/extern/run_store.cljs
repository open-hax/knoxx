(ns knoxx.backend.extern.run-store
  "Clock normalization and content identity at the runtime event boundary."
  (:require [clio.extern.js.crypto :as crypto]
            [clio.shape.canonical :as canonical]
            [knoxx.backend.law.run-store :as law]))

(defn stamp
  "Normalize one real instant and a bounded two-hour expiry before admission."
  [instant instance-id]
  (law/require! law/Instant instant)
  (let [at-ms (.parse js/Date instant)]
    (law/require! law/Milliseconds at-ms)
    (when-not (= instant (.toISOString (js/Date. at-ms)))
      (throw (ex-info "Noncanonical run clock" {:status 400 :code "run_store_invalid_clock"})))
    (law/require! law/Stamp {:at instant :at-ms at-ms :expires-ms (+ at-ms law/ttl-ms)
                             :instance-id instance-id})))

(defn event-id
  "Preserve an explicit event ID or identify the complete immutable runtime payload."
  [event]
  (law/require! law/Event event)
  (law/require! law/NonBlank
                (or (:event_id event) (:id event)
                    (str "event_" (crypto/sha256 (canonical/canonical-edn event))))))
