(ns knoxx.backend.domain.schedule.runtime
  "Runtime for schedule resources.

   Schedules own temporal rules. When a rule ticks, the schedule emits an event
   through the event dispatcher; it never invokes trigger actions directly."
  (:require [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]))

(defonce running?* (atom false))
(defonce schedule-id->interval* (atom {}))
(defonce schedule-id->inflight* (atom {}))

(defn rule->ms
  "Naive temporal rule-to-ms: supports cron-like '*/N * * * *' and
   'N * * * *' forms. Falls back to 5 minutes and never returns < 1 minute."
  [rule]
  (let [rule-text (str rule)
        ms (cond
             (re-find #"/(\d+)" rule-text)
             (let [n (-> (re-find #"/(\d+)" rule-text) second js/parseInt)]
               (* 60 1000 (max (or n 5) 1)))

             (re-find #"^\d+\s+\d+\s+\*\s+\*\s+\*$" rule-text)
             (* 24 60 60 1000)

             (re-find #"^(\d+)\s+\*" rule-text)
             (let [n (-> (re-find #"^(\d+)\s+\*" rule-text) second js/parseInt)]
               (* 60 1000 (max n 1)))

             :else (* 5 60 1000))]
    (max ms (* 60 1000))))

(defn- schedule-resource-id
  [schedule]
  (or (:schedule/id schedule)
      (:contract/id schedule)))

(defn schedule->event
  [schedule]
  (let [event (or (:schedule/event schedule) {})
        schedule-id (schedule-resource-id schedule)
        generator (or (:schedule/generator schedule)
                      (get-in schedule [:schedule/generator :kind])
                      :schedule)]
    (assoc event
           :event/generator (cond-> {:kind generator}
                              schedule-id
                              (assoc :schedule/id schedule-id))
           :event/payload (merge {:schedule/id schedule-id}
                                 (:event/payload event)
                                 (:payload event)))))

(defn- load-schedule-sync
  [config schedule-id]
  (resources/resource-sync config :schedule schedule-id))

(defn- schedule-rule
  [schedule]
  (or (:schedule/rule schedule)
      (:schedule/cron schedule)))

(defn- ^:async emit!
  [config schedule-id schedule]
  (swap! schedule-id->inflight* assoc schedule-id true)
  (try
    (let [result (await (event-dispatch/dispatch! config (schedule->event schedule)))]
      (js/console.log "[schedule-domain] emitted" schedule-id
                      "matched" (count (:matchedTriggers result)) "triggers"))
    (catch :default err
      (js/console.error "[schedule-domain] emission failed" schedule-id err))
    (finally
      (swap! schedule-id->inflight* dissoc schedule-id))))

(defn- arm-rule!
  [config schedule-id rule]
  (let [ms (rule->ms rule)
        tick! (fn ^:async schedule-tick []
                (when @running?*
                  (when-not (get @schedule-id->inflight* schedule-id)
                    (when-let [schedule (load-schedule-sync config schedule-id)]
                      (when (:enabled schedule)
                        (await (emit! config schedule-id schedule)))))))
        interval-id (js/setInterval tick! ms)]
    (swap! schedule-id->interval* assoc schedule-id interval-id)
    (js/console.log "[schedule-domain] armed" schedule-id "every" ms "ms")))

(defn start!
  [config]
  (when-not @running?*
    (reset! running?* true)
    (doseq [schedule-id (resources/list-resource-ids-sync config :schedule)]
      (when-let [schedule (load-schedule-sync config schedule-id)]
        (when (:enabled schedule)
          (if-let [rule (schedule-rule schedule)]
            (arm-rule! config schedule-id rule)
            (js/console.warn "[schedule-domain] ignored missing rule" schedule-id)))))))

(defn stop!
  []
  (doseq [[schedule-id interval-id] @schedule-id->interval*]
    (js/clearInterval interval-id)
    (js/console.log "[schedule-domain] stopped" schedule-id))
  (reset! schedule-id->interval* {})
  (reset! schedule-id->inflight* {})
  (reset! running?* false))

(defn status
  [config]
  {:running @running?*
   :schedules (->> (resources/list-resource-ids-sync config :schedule)
                   (mapv (fn [id]
                           (when-let [schedule (load-schedule-sync config id)]
                             (select-keys schedule
                                          [:contract/id
                                           :schedule/id
                                           :schedule/rule
                                           :schedule/cron
                                           :schedule/event
                                           :enabled])))))})
