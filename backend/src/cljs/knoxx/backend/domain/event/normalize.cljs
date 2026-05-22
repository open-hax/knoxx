(ns knoxx.backend.domain.event.normalize
  "Canonical event shape for trigger/action dispatch."
  (:require [clojure.string :as str]))

(defn- now-iso
  []
  (.toISOString (js/Date.)))

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn event-type
  [value]
  (cond
    (keyword? value) value
    (nil? value) nil
    :else
    (let [raw (nonblank value)]
      (when raw
        (if (str/includes? raw "/")
          (keyword raw)
          (let [parts (str/split raw #"\.")]
            (if (> (count parts) 1)
              (keyword (str/join "." (butlast parts)) (last parts))
              (keyword raw))))))))

(defn event-types
  [event]
  (let [explicit (or (:event/types event)
                     (:eventTypes event)
                     (:eventKinds event)
                     (:event-kinds event))
        primary (or (:event/type event)
                    (:eventType event)
                    (:eventKind event)
                    (:event-kind event))]
    (->> (concat (when primary [primary]) explicit)
         (keep event-type)
         distinct
         vec)))

(defn normalize-event
  [event]
  (let [event (or event {})
        payload (or (:event/payload event)
                    (:payload event)
                    {})
        types (event-types event)
        generator-kind (or (get-in event [:event/generator :kind])
                           (:generatorKind event)
                           (:generator-kind event)
                           (:generator event))]
    {:event/id (or (nonblank (:event/id event))
                   (nonblank (:id event))
                   (str "evt_" (.now js/Date)))
     :event/type (or (first types) :manual/event)
     :event/types (if (seq types) types [:manual/event])
     :event/actor (or (nonblank (:event/actor event))
                      (nonblank (:actorId event))
                      (nonblank (:actor-id event)))
     :event/generator (cond-> (or (:event/generator event) {})
                        generator-kind (assoc :kind generator-kind))
     :event/payload payload
     :event/timestamp (or (nonblank (:event/timestamp event))
                          (nonblank (:timestamp event))
                          (now-iso))}))
