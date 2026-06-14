(ns knoxx.backend.domain.trigger.normalize
  "Normalize trigger contracts into the event/action runtime shape.

   :trigger/with is the sole argument mechanism: everything the action needs
   travels in that map and reaches the action as :action/with. Legacy
   :trigger/agent and :trigger/task fields fold into it during normalization
   (explicit :trigger/with keys win) so unmigrated contracts keep working."
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

(defn- legacy-trigger-task
  [trigger]
  (or (nonblank (:trigger/task trigger))
      (nonblank (:trigger/task-prompt trigger))
      (nonblank (:trigger/message-template trigger))
      (nonblank (get-in trigger [:data :task]))
      (nonblank (get-in trigger [:data :message-template]))
      (nonblank (get-in trigger [:data :context :task]))))

(defn- trigger-with
  "Build the trigger argument map: explicit :trigger/with keys over folded
   legacy :trigger/agent and :trigger/task fields."
  [trigger target]
  (let [agent-id (or (nonblank (:trigger/agent trigger)) target)
        task (legacy-trigger-task trigger)]
    (merge (when agent-id {:agent-id agent-id})
           (when task {:task task})
           (:trigger/with trigger))))

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
     :trigger/condition (or (:trigger/condition trigger)
                            (get-in trigger [:data :condition]))
     :trigger/action (or (:trigger/action trigger)
                         (when target :actions/start-agent-session))
     :trigger/with (trigger-with trigger target)
     :trigger/context (or (:trigger/context trigger)
                          (get-in trigger [:data :context])
                          {})
     :trigger/raw trigger}))
