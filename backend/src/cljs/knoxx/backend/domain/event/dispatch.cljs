(ns knoxx.backend.domain.event.dispatch
  "Contract-native event dispatcher."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.action.start-agent-session]
            [knoxx.backend.domain.action.run-pipeline]
            [knoxx.backend.domain.condition.registry :as condition-registry]
            [knoxx.backend.domain.event.normalize :as event-normalize]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.trigger.normalize :as trigger-normalize]
            [knoxx.backend.infra.config :as runtime-config]
            [knoxx.backend.domain.models :as runtime-models]))

(defonce dispatched-event-ids* (atom #{}))
(defonce recent-events* (atom []))

(defn- cfg
  []
  (runtime-models/enrich-config (runtime-config/cfg)))

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn- append-recent-event!
  [event]
  (swap! recent-events*
         (fn [events]
           (->> (conj (vec events) event)
                (take-last 30)
                vec))))

(defn- mark-event-dispatched!
  [event-id]
  (let [[before] (swap-vals! dispatched-event-ids* conj event-id)]
    (not (contains? before event-id))))

(defn- load-trigger-resources
  [config]
  (->> (resources/load-all-resources-sync config)
       (filter #(= :trigger (:resource/kind %)))
       (map :resource/definition)
       (remove nil?)
       vec))

(defn- emitter-matches?
  "True if the trigger's emitter matches the event's actor."
  [trigger event]
  (let [trigger-emitter (nonblank (:trigger/emitter trigger))
        event-actor (nonblank (:event/actor event))]
    (or (nil? trigger-emitter)
        (= trigger-emitter event-actor))))

(defn- event-type-matches?
  [trigger event]
  (let [trigger-types (set (:trigger/events trigger))]
    (and (seq trigger-types)
         (seq (set/intersection trigger-types (set (:event/types event)))))))

(defn- condition-matches?
  "Evaluate the trigger's condition expression against the event.
  If no condition, then true.
  "
  [trigger event]
  (if-let [expr (:trigger/condition trigger)]
    (condition-registry/evaluate expr event nil trigger nil)
    true
    ))

(defn- trigger-matches?
  [trigger event]
  (and (:trigger/enabled? trigger)
       (= :event (:trigger/kind trigger))
       (event-type-matches? trigger event)
       (emitter-matches? trigger event)
       (condition-matches? trigger event)))

(defn- actor-context
  [config trigger event]
  {:config config
   :event event
   :trigger trigger
   :actor/id (or (nonblank (:trigger/actor trigger))
                 (nonblank (:trigger/listener trigger)))
   :agent/id (:trigger/agent trigger)
   :trigger-ctx (merge (get-in trigger [:data :context]) {}
                       (get-in event [:event/payload]) {})})

(defn ^:async dispatch!
  ([event]
   (dispatch! (cfg) event))
  ([config event]
   (let [event' (event-normalize/normalize-event event)
         event-id (:event/id event')]
     (append-recent-event! event')
     (if-not (mark-event-dispatched! event-id)
       {:matchedTriggers []
        :event event'
        :skipped true}
       (let [matching-triggers (->> (load-trigger-resources config)
                                    (map trigger-normalize/normalize-trigger)
                                    (filter #(trigger-matches? % event'))
                                    vec)
             results (await (js/Promise.all
                             (clj->js
                              (mapv (fn [trigger]
                                      (-> (action-registry/run-action!
                                           (actor-context config trigger event')
                                           (action-registry/action-map trigger))
                                          (.catch (fn [err]
                                                    (let [code (or (aget err "code") "action_error")]
                                                      (js/console.warn "[event-dispatch] trigger" (:trigger/id trigger)
                                                                       "skipped:" (.-message err))
                                                      #js {:skipped true :reason code
                                                           :trigger (:trigger/id trigger)})))))
                                    matching-triggers))))]
         {:matchedTriggers (mapv :trigger/id matching-triggers)
          :event event'
          :results (js->clj results :keywordize-keys true)})))))

(defn status-snapshot
  [config]
  (let [triggers (->> (load-trigger-resources config)
                      (map trigger-normalize/normalize-trigger)
                      vec)]
    {:running true
     :configured true
     :events {:recentEvents @recent-events*}
     :triggers (mapv (fn [trigger]
                       {:id (:trigger/id trigger)
                        :enabled (:trigger/enabled? trigger)
                        :kind (:trigger/kind trigger)
                        :events (:trigger/events trigger)
                        :action (:trigger/action trigger)
                        :agent (:trigger/agent trigger)
                        :listener (:trigger/listener trigger)})
                     triggers)}))

(defn reset-dedup!
  []
  (reset! dispatched-event-ids* #{})
  (reset! recent-events* []))
