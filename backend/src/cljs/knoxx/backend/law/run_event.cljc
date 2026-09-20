(ns knoxx.backend.law.run-event
  "Provider-independent admission for immutable sequenced runtime events."
  (:require [knoxx.backend.law.run-store :as run]))

(def max-sequence 9007199254740991)
(def Sequence [:int {:min 1 :max max-sequence}])

(defn admit
  "Require the visible run's coordinates; exact retries keep their accepted sequence."
  [snapshot event event-id existing last-sequence]
  (run/require! run/Event event)
  (run/require! run/NonBlank event-id)
  (run/require! [:int {:min 0 :max max-sequence}] last-sequence)
  (when-not (= (select-keys snapshot [:run_id :session_id :conversation_id])
               (select-keys event [:run_id :session_id :conversation_id]))
    (run/conflict! "Run event has a different conversation or session"))
  (when (or (contains? event :sequence) (contains? event :run_events))
    (run/conflict! "Event sequence is assigned by the durable provider"))
  (if existing
    (if (= (dissoc existing :sequence) (assoc event :event_id event-id))
      {:existing? true :event existing}
      (run/conflict! "Run event ID is already bound to another payload"))
    (do (when (= max-sequence last-sequence)
          (run/conflict! "Run event sequence is exhausted"))
        {:existing? false :event (assoc event :event_id event-id :sequence (inc last-sequence))})))
