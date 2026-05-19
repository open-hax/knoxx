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

(defn- default-actor-for-source
  [kind]
  (case (keyword kind)
    :discord "discord_automation"
    nil))

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
        target (nonblank (:trigger/target trigger))]
    {:trigger/id (or (nonblank (:contract/id trigger))
                     (nonblank (:trigger/id trigger)))
     :trigger/kind kind
     :trigger/enabled? (not (false? (:enabled trigger)))
     :trigger/events (trigger-event-types trigger)
     :trigger/emitter (or (nonblank (:trigger/emitter trigger))
                          (default-actor-for-source src-kind))
     :trigger/listener (or (nonblank (:trigger/listener trigger))
                           (first (:contract/actors trigger))
                           (default-actor-for-source src-kind))
     :trigger/predicate (or (:trigger/predicate trigger)
                            (get-in trigger [:data :filters])
                            {})
     :trigger/action (or (:trigger/action trigger)
                         (when target :actions/start-agent-session))
     :trigger/agent (or (nonblank (:trigger/agent trigger))
                        target)
     :trigger/context (or (get-in trigger [:data :context]) {})
     :trigger/source-kind src-kind
     :trigger/raw trigger}))
