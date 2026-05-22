(ns knoxx.backend.domain.trigger.normalize
  "Normalize trigger contracts into the event/action runtime shape."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.event.normalize :as event-normalize]))

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn trigger-event-types
  [trigger]
  (->> (or (:trigger/events trigger)
           (:trigger/event-types trigger)
           (:trigger/event-kinds trigger)
           (get-in trigger [:trigger :eventKinds])
           [])
       (keep event-normalize/event-type)
       distinct
       vec))

(defn- trigger-participants
  [trigger]
  (let [explicit-actor (nonblank (:trigger/actor trigger))
        contract-actors (when (sequential? (:contract/actors trigger))
                          (first (:contract/actors trigger)))]
    {:actor explicit-actor
     :emitter (or (nonblank (:trigger/emitter trigger)) explicit-actor)
     :listener (or (nonblank (:trigger/listener trigger)) explicit-actor contract-actors)}))

(defn normalize-trigger
  [trigger]
  (let [target (nonblank (:trigger/target trigger))
        {:keys [actor emitter listener]} (trigger-participants trigger)]
    {:trigger/id (or (nonblank (:contract/id trigger))
                     (nonblank (:trigger/id trigger)))
     :trigger/kind :event
     :trigger/enabled? (not (false? (:enabled trigger)))
     :trigger/events (trigger-event-types trigger)
     :trigger/actor actor
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
     :trigger/raw trigger}))
