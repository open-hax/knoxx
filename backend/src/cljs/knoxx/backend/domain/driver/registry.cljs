(ns knoxx.backend.domain.driver.registry
  "Code-level registry for event drivers.

   Drivers are not resources. A driver is ClojureScript code that knows how to
   normalize events from one system (Discord, sessions, translations, etc.) and
   dispatch them into the event runtime. Source resources declare which driver
   implementation they use, which actor owns credentials, and which driver event
   types they care about."
  (:require [clojure.string :as str]))

(defprotocol EventDriver
  (driver-id [driver]
    "Stable keyword id for the driver implementation, e.g. :driver/discord.")
  (driver-kind [driver]
    "Coarse event system, e.g. :discord or :sessions.")
  (driver-event-specs [driver]
    "Event specs this driver implementation may emit.")
  (start-source! [driver context]
    "Start or bind one source instance for this driver.

     context contains at least:
     - :config runtime config
     - :source source resource map
     - :dispatch! fn that accepts a normalized event map"))

(defrecord StaticEventDriver [id kind event-specs start-fn]
  EventDriver
  (driver-id [_] id)
  (driver-kind [_] kind)
  (driver-event-specs [_] event-specs)
  (start-source! [_ context]
    (if start-fn
      (start-fn context)
      (js/Promise.resolve {:started? false
                           :reason :no-start-hook
                           :driver/id id
                           :source/id (get-in context [:source :source/id])}))))

(defonce drivers* (atom {}))

(defn normalize-driver-id
  "Normalize driver ids to namespaced :driver/* keywords when possible."
  [value]
  (let [raw (some-> value str (str/replace #"^:" "") str/trim not-empty)]
    (cond
      (keyword? value) value
      (nil? raw) nil
      (str/includes? raw "/") (keyword raw)
      :else (keyword "driver" raw))))

(defn event-type
  "Extract an event type keyword from a driver/listener event entry."
  [entry]
  (let [value (if (map? entry) (:event/type entry) entry)
        raw (some-> value str (str/replace #"^:" "") str/trim not-empty)]
    (cond
      (keyword? value) value
      (nil? raw) nil
      (str/includes? raw "/") (keyword raw)
      :else (keyword raw))))

(defn driver-event-types
  "Return event type keywords emitted by a driver implementation."
  [driver]
  (->> (driver-event-specs driver)
       (keep event-type)
       distinct
       vec))

(defn make-static-driver
  "Construct a data-backed driver implementation for systems whose startup hook
   is supplied elsewhere or not wired yet."
  [{:keys [id kind emits start!]}]
  (->StaticEventDriver (normalize-driver-id id) kind (vec emits) start!))

(defn register-driver!
  "Register one code-level driver implementation."
  [driver]
  (swap! drivers* assoc (driver-id driver) driver)
  driver)

(defn register-drivers!
  "Register multiple code-level driver implementations."
  [drivers]
  (doseq [driver drivers]
    (register-driver! driver))
  @drivers*)

(defn unregister-driver!
  [driver-id]
  (swap! drivers* dissoc (normalize-driver-id driver-id)))

(defn clear-drivers!
  "Test helper: clear the process-local driver registry."
  []
  (reset! drivers* {}))

(defn driver
  "Return a registered driver implementation by id, or nil."
  [driver-id]
  (get @drivers* (normalize-driver-id driver-id)))

(defn registered-driver?
  "True when driver-id names a registered code-level driver implementation."
  [driver-id]
  (contains? @drivers* (normalize-driver-id driver-id)))

(defn registered-driver-ids
  []
  (-> @drivers* keys sort vec))

(defn emitted-event-types
  "Return event types emitted by the registered driver id."
  [driver-id]
  (if-let [registered (driver driver-id)]
    (driver-event-types registered)
    []))

(defn source-listens
  "Return event type keywords selected by a source resource."
  [source]
  (->> (:source/listens source)
       (keep event-type)
       distinct
       vec))

(defn listened-by-driver?
  "True when every source/listens event is emitted by its selected driver."
  [source]
  (let [driver-types (set (emitted-event-types (:source/driver source)))]
    (every? driver-types (source-listens source))))

(defn source-event
  "Attach driver/source provenance to an event before dispatch.

   This is how a source instance gives us an event: the driver implementation
   observes a system signal, calls this helper with its source resource, then
   passes the resulting map to the runtime dispatcher."
  [driver source event]
  (let [driver-key (driver-id driver)
        generator (merge {:kind (driver-kind driver)
                          :driver driver-key}
                         (when-let [source-id (:source/id source)]
                           {:source source-id}))]
    (-> event
        (assoc :event/generator generator)
        (cond-> (:source/actor source)
          (assoc :event/actor (:source/actor source))))))
