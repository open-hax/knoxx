(ns knoxx.backend.domain.event.dispatch
  "Contract-native event dispatcher."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [knoxx.backend.domain.contracts.loader :as loader]
            [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.action.start-agent-session]
            [knoxx.backend.domain.action.run-pipeline]
            [knoxx.backend.domain.condition.registry :as condition-registry]
            [knoxx.backend.domain.event.normalize :as event-normalize]
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

(defn- payload-value
  [event k]
  (let [payload (:event/payload event)]
    (or (get payload k)
        (get payload (keyword (name k))))))

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

(defn- load-trigger-contracts
  [config]
  (->> (loader/load-all-contracts-sync config)
       (filter #(= "triggers" (:contractClass %)))
       (map :contract)
       (remove nil?)
       vec))

(defn- channel-matches?
  [predicate event]
  (let [channels (seq (:channels predicate))]
    (or (nil? channels)
        (contains? (set (map str channels))
                   (str (or (payload-value event :channelId)
                            (payload-value event :channel-id)
                            ""))))))

(defn- keyword-matches?
  [predicate event]
  (let [keywords (->> (or (:keywords predicate) [])
                      (map #(some-> % str str/trim str/lower-case not-empty))
                      (remove nil?))
        content (str/lower-case (str (or (payload-value event :content) "")))]
    (or (empty? keywords)
        (some #(str/includes? content %) keywords))))


(defn- source-matches?
  [trigger event]
  (let [required (some-> (:trigger/source-kind trigger) keyword)
        actual (some-> (get-in event [:event/source :kind]) keyword)]
    (or (nil? required)
        (= required actual))))

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
       (source-matches? trigger event)
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

(defn dispatch!
  ([event]
   (dispatch! (cfg) event))
  ([config event]
   (let [event' (event-normalize/normalize-event event)
         event-id (:event/id event')]
     (append-recent-event! event')
     (if-not (mark-event-dispatched! event-id)
       (js/Promise.resolve {:matchedTriggers []
                            :event event'
                            :skipped true})
       (let [matching-triggers (->> (load-trigger-contracts config)
                                    (map trigger-normalize/normalize-trigger)
                                    (filter #(trigger-matches? % event'))
                                    vec)]
         (-> (js/Promise.all
              (clj->js
               (mapv (fn [trigger]
                       (action-registry/run-action!
                        (actor-context config trigger event')
                        (action-registry/action-map trigger)))
                     matching-triggers)))
             (.then (fn [results]
                      {:matchedTriggers (mapv :trigger/id matching-triggers)
                       :event event'
                       :results (js->clj results :keywordize-keys true)}))))))))

(defn status-snapshot
  [config]
  (let [triggers (->> (load-trigger-contracts config)
                      (map trigger-normalize/normalize-trigger)
                      vec)]
    {:running true
     :configured true
     :sources {:recentEvents @recent-events*}
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
