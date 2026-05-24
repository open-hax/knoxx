(ns knoxx.backend.domain.source.runtime
  "Runtime lifecycle for source resource instances.

   A source gives us events by binding one actor-owned resource to one
   code-level driver implementation. The driver observes its system and calls
   dispatch-source-event! (or receives :dispatch! in start-source!) with a plain
   event map. This namespace attaches source/driver provenance and forwards the
   normalized event into the generic event dispatcher."
  (:require [knoxx.backend.domain.driver.builtin :as driver-builtin]
            [knoxx.backend.domain.driver.registry :as driver-registry]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]))

(defonce source-status* (atom {}))

(defn- resource-definition
  [record]
  (or (:resource/definition record)
      (:contract record)))

(defn- source-record?
  [record]
  (or (= :source (:resource/kind record))
      (= "sources" (:resource/class record))
      (= "sources" (:contractClass record))))

(defn- enabled-source?
  [source]
  (and (not (false? (:enabled source)))
       (not (false? (:source/enabled? source)))))

(defn event-source?
  [source]
  (and (enabled-source? source)
       (or (= :event-generator (:source/type source))
           (= "event-generator" (:source/type source))
           (seq (:source/listens source)))))

(defn source-resources
  "Load enabled event source resources from EDN."
  [config]
  (driver-builtin/register-built-in-drivers!)
  (->> (resources/load-all-resources-sync config)
       (filter source-record?)
       (map resource-definition)
       (filter event-source?)
       vec))

(defn- event-entry
  [event]
  (or (:event/type event)
      (:eventType event)
      (:eventKind event)
      (:event-kind event)))

(defn source-listens-to?
  [source event-type]
  (contains? (set (driver-registry/source-listens source))
             (driver-registry/event-type event-type)))

(defn matching-source
  "Find the source resource that admits driver-id/actor-id/event-type.

   This is the multiple-bot seam: several source resources may use the same
   driver, but actor-owned credentials choose which one receives events."
  [config driver-id actor-id event-type]
  (let [wanted-driver (driver-registry/normalize-driver-id driver-id)
        exact-actor? (fn [source]
                       (= (str (:source/actor source)) (str actor-id)))
        listens? (fn [source]
                   (and (= wanted-driver
                           (driver-registry/normalize-driver-id (:source/driver source)))
                        (source-listens-to? source event-type)))
        candidates (filter listens? (source-resources config))]
    (or (some #(when (exact-actor? %) %) candidates)
        (when-not actor-id
          (first candidates)))))

(defn source-event
  "Attach source actor and driver/source provenance to an event."
  [source event]
  (when-let [driver (driver-registry/driver (:source/driver source))]
    (driver-registry/source-event driver source event)))

(defn ^:async dispatch-source-event!
  "Dispatch one event through a concrete source resource if it selected the event.

   Returns a skipped result instead of dispatching when the source's driver is not
   registered or the event type is outside :source/listens."
  [config source event]
  (driver-builtin/register-built-in-drivers!)
  (let [event-type (event-entry event)
        driver (:source/driver source)]
    (cond
      (not (driver-registry/registered-driver? driver))
      {:skipped true
       :reason :unknown-driver
       :driver driver
       :source/id (:source/id source)}

      (not (source-listens-to? source event-type))
      {:skipped true
       :reason :source-not-listening
       :driver driver
       :source/id (:source/id source)
       :event/type (driver-registry/event-type event-type)}

      :else
      (await (event-dispatch/dispatch! config (source-event source event))))))

(defn ^:async dispatch-driver-event!
  "Dispatch an event emitted by driver-id for actor-id.

   The event is admitted only if an enabled source resource for that actor uses
   the driver and lists the event type in :source/listens."
  [config driver-id actor-id event]
  (if-let [source (matching-source config driver-id actor-id (event-entry event))]
    (await (dispatch-source-event! config source event))
    {:skipped true
     :reason :no-matching-source
     :driver (driver-registry/normalize-driver-id driver-id)
     :actor/id actor-id
     :event/type (driver-registry/event-type (event-entry event))}))

(defn- source-start-context
  [config source]
  {:config config
   :source source
   :dispatch! (fn [event]
                (dispatch-source-event! config source event))})

(defn ^:async start-source!
  [config source]
  (driver-builtin/register-built-in-drivers!)
  (let [driver-id (:source/driver source)]
    (if-let [driver (driver-registry/driver driver-id)]
      (let [result (await (driver-registry/start-source! driver (source-start-context config source)))
            status (merge {:driver/id (driver-registry/driver-id driver)
                           :source/id (:source/id source)
                           :source/actor (:source/actor source)}
                          (when (map? result) result))]
        (swap! source-status* assoc (:source/id source) status)
        status)
      (let [status {:started? false
                    :reason :unknown-driver
                    :driver/id driver-id
                    :source/id (:source/id source)
                    :source/actor (:source/actor source)}]
        (swap! source-status* assoc (:source/id source) status)
        status))))

(defn ^:async start!
  "Start/bind all enabled event source resources."
  [config]
  (let [results (await (js/Promise.all
                        (clj->js
                         (mapv #(start-source! config %) (source-resources config)))))]
    (js->clj results :keywordize-keys true)))

(defn stop!
  []
  (reset! source-status* {})
  nil)

(defn status
  []
  @source-status*)
