(ns knoxx.backend.domain.trigger.normalize
  "Normalize trigger contracts into the event/action runtime shape."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.event.normalize :as event-normalize]))

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn- source-kind
  [trigger]
  (or (get-in trigger [:trigger/source :kind])
      (:source-kind trigger)
      (:sourceKind trigger)))


(defn trigger-event-types
  [trigger]
  (->> (or (:trigger/events trigger)
           (:trigger/event-types trigger)
           (:trigger/event-kinds trigger)
           (get-in trigger [:trigger/source :events])
           (get-in trigger [:trigger :eventKinds])
           [])
       (keep event-normalize/event-type)
       distinct
       vec))

(defn normalize-trigger
  [trigger]
  (let [kind (or (:trigger/kind trigger) :event)
        src-kind (source-kind trigger)
        target (nonblank (:trigger/target trigger))
        explicit-actor (nonblank (:trigger/actor trigger))
        explicit-emitter (nonblank (:trigger/emitter trigger))
        explicit-listener (nonblank (:trigger/listener trigger))
        contract-actors (when (sequential? (:contract/actors trigger))
                          (first (:contract/actors trigger)))
        emitter (or explicit-emitter explicit-actor )
        listener (or explicit-listener explicit-actor contract-actors )]
    {:trigger/id (or (nonblank (:contract/id trigger))
                     (nonblank (:trigger/id trigger)))
     :trigger/kind kind
     :trigger/enabled? (not (false? (:enabled trigger)))
     :trigger/events (trigger-event-types trigger)
     :trigger/actor explicit-actor
     :trigger/emitter emitter
     :trigger/listener listener
     :trigger/predicate (or (:trigger/predicate trigger)
                            (get-in trigger [:data :filters])
                            {})
     :trigger/condition (or (:trigger/condition trigger)
                            (get-in trigger [:data :condition]))
     :trigger/action (or (:trigger/action trigger)
                         (when target :actions/start-agent-session))
     :trigger/agent (or (nonblank (:trigger/agent trigger))
                        target)
     :trigger/with (:trigger/with trigger)
     :trigger/context (or (get-in trigger [:data :context]) {})
     :trigger/source-kind src-kind
     :trigger/raw trigger}))
