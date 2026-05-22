(ns knoxx.backend.domain.trigger.runtime
  "Runtime for trigger resources.

   Triggers are event responders. Manual trigger exercise dispatches an event;
   it does not call actions directly, so all listeners use the same path."
  (:require [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]))

(defonce running?* (atom false))

(defn- load-trigger-sync
  [config trigger-id]
  (resources/resource-sync config :trigger trigger-id))

(defn- event-trigger?
  [trigger]
  (= :event (:trigger/kind trigger)))

(defn start!
  [config]
  (when-not @running?*
    (reset! running?* true)
    (doseq [trigger-id (resources/list-resource-ids-sync config :trigger)]
      (when-let [trigger (load-trigger-sync config trigger-id)]
        (if (event-trigger? trigger)
          (js/console.log "[trigger-domain] registered" trigger-id)
          (js/console.warn "[trigger-domain] ignored non-event trigger"
                           trigger-id
                           (:trigger/kind trigger)))))))

(defn stop!
  []
  (reset! running?* false))

(defn ^:async fire!
  "Dispatch one of a trigger's observed events for manual testing."
  ([config trigger-id] (fire! config trigger-id {}))
  ([config trigger-id payload]
   (let [trigger (load-trigger-sync config trigger-id)]
     (if trigger
       (let [event-type (first (:trigger/events trigger))]
         (when-not (event-trigger? trigger)
           (throw (js/Error. (str "Trigger is not an event trigger: " trigger-id))))
         (when-not event-type
           (throw (js/Error. (str "Trigger has no observed events: " trigger-id))))
         (await (event-dispatch/dispatch! config
                                          {:event/type event-type
                                           :event/generator {:kind :manual
                                                             :trigger/id trigger-id}
                                           :event/payload (merge (get-in trigger [:data :context])
                                                                 payload)})))
       (throw (js/Error. (str "Unknown trigger: " trigger-id)))))))

(defn status
  [config]
  {:running @running?*
   :triggers (->> (resources/list-resource-ids-sync config :trigger)
                  (mapv (fn [id]
                          (when-let [trigger (load-trigger-sync config id)]
                            (select-keys trigger
                                         [:contract/id
                                          :trigger/id
                                          :trigger/kind
                                          :trigger/events
                                          :trigger/action
                                          :enabled])))))})
