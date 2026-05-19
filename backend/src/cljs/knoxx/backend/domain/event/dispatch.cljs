(ns knoxx.backend.domain.event.dispatch
  "Contract-native event dispatcher."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [knoxx.backend.contracts.loader :as loader]
            [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.action.start-agent-session]
            [knoxx.backend.domain.event.normalize :as event-normalize]
            [knoxx.backend.domain.trigger.normalize :as trigger-normalize]
            [knoxx.backend.runtime.config :as runtime-config]
            [knoxx.backend.runtime.models :as runtime-models]))

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

(defn- predicate-matches?
  [trigger event]
  (let [predicate (:trigger/predicate trigger)]
    (and (channel-matches? predicate event)
         (keyword-matches? predicate event))))

(defn- source-matches?
  [trigger event]
  (let [required (some-> (:trigger/source-kind trigger) keyword)
        actual (some-> (get-in event [:event/source :kind]) keyword)]
    (or (nil? required)
        (= required actual))))

(defn- event-type-matches?
  [trigger event]
  (let [trigger-types (set (:trigger/events trigger))]
    (and (seq trigger-types)
         (seq (set/intersection trigger-types (set (:event/types event)))))))

(defn- trigger-matches?
  [trigger event]
  (and (:trigger/enabled? trigger)
       (= :event (:trigger/kind trigger))
       (event-type-matches? trigger event)
       (source-matches? trigger event)
       (predicate-matches? trigger event)))

(defn- action-map
  [trigger]
  {:action/id (name (:trigger/action trigger))
   :action/kind (:trigger/action trigger)
   :action/with {:agent-id (:trigger/agent trigger)}})

(defn- actor-context
  [config trigger event]
  {:config config
   :event event
   :trigger trigger
   :actor/id (:trigger/listener trigger)
   :agent/id (:trigger/agent trigger)})

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
                        (action-map trigger)))
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
