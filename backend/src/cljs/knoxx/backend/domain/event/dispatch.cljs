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

(defn release-exact-event!
  "Release one exact event id whose trigger execution is retriable.

   The set is both the in-flight claim and the completed-event dedup ledger. A
   throw cannot remain in that ledger: doing so turns a transient action failure
   into a permanent false success on every retry. Equal callers that arrive
   while the owner is still running remain deduplicated; only the owner that
   observes the failure releases its claim.

   Terminal effect owners may also call this with their own deterministic event
   id when the asynchronously queued work settles without producing its durable
   success fact. This intentionally has no bulk or predicate form."
  [event-id]
  (let [event-id (nonblank event-id)]
    (when event-id
      (swap! dispatched-event-ids* disj event-id))
    true))

(defn- load-trigger-resources
  [config]
  (let [all-resources (resources/load-all-resources-sync config)
        trigger-resources (filter #(= :trigger (:resource/kind %)) all-resources)
        trigger-defs (map :resource/definition trigger-resources)]
    (js/console.log "[event-dispatch] load-trigger-resources: all=" (count all-resources)
                    " triggers=" (count trigger-resources)
                    " defs=" (count trigger-defs)
                    " kinds=" (pr-str (take 5 (map :resource/kind all-resources)))
                    " sample=" (pr-str (take 1 (map :resource/id trigger-resources))))
    (vec (remove nil? trigger-defs))))

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
  [config trigger event trusted?]
  {:config config
   :event event
   ;; Dispatch provenance is deliberately outside the normalized event. A
   ;; caller can forge every event field, but cannot change which entry point
   ;; admitted it.
   :event/trusted? trusted?
   :trigger trigger
   :actor/id (or (nonblank (:trigger/actor trigger))
                 (nonblank (:trigger/listener trigger)))
   :agent/id (get-in trigger [:trigger/with :agent-id])
   :trigger-ctx (merge (get-in trigger [:data :context]) {}
                       (get-in event [:event/payload]) {})})

(defn- matching-triggers
  [config event]
  (let [all-triggers (load-trigger-resources config)]
    (js/console.log "[event-dispatch] loaded triggers:" (count all-triggers))
    (->> all-triggers
         (map trigger-normalize/normalize-trigger)
         (filter #(trigger-matches? % event))
         vec)))

(defn- ^:async run-trigger!
  [config event trigger trusted?]
  (try
    (await (action-registry/run-action!
            (actor-context config trigger event trusted?)
            (action-registry/action-map trigger)))
    (catch :default err
      (js/console.error "[event-dispatch] action failed for trigger"
                        (:trigger/id trigger) ":" (.-message err))
      (throw err))))

(defn- ^:async dispatch-with-provenance!
  [config event trusted?]
   (let [event' (event-normalize/normalize-event event)
         event-id (:event/id event')]
      (append-recent-event! event')
      (if-not (mark-event-dispatched! event-id)
        (do (js/console.log "[event-dispatch] event deduplicated:" event-id)
            {:matchedTriggers []
             :event event'
             :skipped true})
        (try
          (let [matched (matching-triggers config event')
                _ (js/console.log "[event-dispatch] matching triggers:" (count matched) "for event" (pr-str (:event/type event')))
                results (await (js/Promise.all
                                (clj->js
                                 (mapv #(run-trigger! config event' % trusted?)
                                       matched))))]
            ;; Nothing was dispatched when no enabled trigger matched. Keeping
            ;; the id would make enabling the missing trigger ineffective: the
            ;; corrected retry would be skipped before matching was attempted.
            (when (empty? matched)
              (release-exact-event! event-id))
            {:matchedTriggers (mapv :trigger/id matched)
             :event event'
             :results (js->clj results :keywordize-keys true)})
          (catch :default err
            (release-exact-event! event-id)
            (throw err))))))

(defn ^:async dispatch!
  "Dispatch an event produced by an in-process source, schedule, or domain.

  Trust here is capability provenance, not a claim made inside `event`."
  ([event]
   (dispatch! (cfg) event))
  ([config event]
   (await (dispatch-with-provenance! config event true))))

(defn ^:async dispatch-external!
  "Dispatch an operator-supplied event without event-policy authority."
  ([event]
   (dispatch-external! (cfg) event))
  ([config event]
   (await (dispatch-with-provenance! config event false))))

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
                        :agent (get-in trigger [:trigger/with :agent-id])
                        :listener (:trigger/listener trigger)
                        :emitter (:trigger/emitter trigger)
                        :condition (some? (:trigger/condition trigger))
                        :resourcePoliciesFromEvent
                        (true? (get-in trigger
                                       [:trigger/with
                                        :resource-policies-from-event]))
                        :executionSnapshotFromEvent
                        (true? (get-in trigger
                                       [:trigger/with
                                        :execution-snapshot-from-event]))})
                     triggers)}))

(defn reset-dedup!
  []
  (reset! dispatched-event-ids* #{})
  (reset! recent-events* []))
