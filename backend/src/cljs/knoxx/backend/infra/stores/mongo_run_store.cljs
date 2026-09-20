(ns knoxx.backend.infra.stores.mongo-run-store
  "Atomic Mongo run state and ordered events using the same finite transitions as Clio."
  (:require [knoxx.backend.domain.mongo-run-store :as snapshot]
            [knoxx.backend.domain.run-directory :as directory]
            [knoxx.backend.domain.run-store :as domain]
            [knoxx.backend.extern.clock :as clock]
            [knoxx.backend.extern.mongo-run-store :as mongo]
            [knoxx.backend.extern.run-store :as host]
            [knoxx.backend.infra.system-instance :as instance]
            [knoxx.backend.law.run-store :as law]
            [knoxx.backend.shape.run-directory :as directory-port]
            [knoxx.backend.shape.session-persistence :as protocol]))

(defn- sample [store] (host/stamp ((:clock! store)) (:instance-id store)))

(defn- ^:async read-state! [store id]
  (snapshot/restore (await (mongo/read! (:db store) id)) id))

(defn- ^:async mutate! [store operation]
  (let [id (:run-id operation)]
    (loop [attempt 0]
      (when (>= attempt 32)
        (throw (ex-info "Concurrent Mongo run writers exceeded bounded admission retries"
                        {:status 409 :code "run_store_concurrent_write"})))
      (let [previous (await (mongo/read! (:db store) id))
            state (snapshot/restore previous id)
            [next-state result] (domain/transition state (assoc operation :stamp (sample store)))]
        ;; Even exact retries rewrite the same state with a new storage revision:
        ;; a previous write may be visible after its journal acknowledgement failed.
        (if (or (and (nil? previous) (= state next-state))
                (await (mongo/compare-and-swap! (:db store) id previous next-state)))
          result
          (recur (inc attempt)))))))

(defn- ^:async visible! [store id]
  (domain/visible-run (await (read-state! store id)) id (:at-ms (sample store))))

(defn- ^:async active! [store session-id]
  (let [records (await (mongo/active! (:db store) session-id))
        at (:at-ms (sample store))]
    (->> records
         (keep (fn [{:keys [run-id record]}]
                 (domain/visible-run (snapshot/restore record run-id) run-id at)))
         (sort-by :run_id) vec)))

(defn- ^:async events! [store run-id since]
  (domain/events-since (await (read-state! store run-id)) run-id since (:at-ms (sample store))))

(defn- ^:async directory! [store scope]
  (let [records (await (mongo/directory! (:db store) scope))
        at (:at-ms (sample store))]
    (directory/selected
     (keep (fn [{:keys [run-id record]}]
             (domain/visible-run (snapshot/restore record run-id) run-id at)) records) scope)))

(defrecord MongoRunStore [db clock! instance-id]
  protocol/ISessionStore
  (put-run! [store run] (mutate! store {:kind :put :run-id (:run_id run) :run run}))
  (get-run [store id] (visible! store id))
  (patch-run! [store id patch] (mutate! store {:kind :patch :run-id id :patch patch}))
  (list-active-runs [store id] (active! store id))
  (complete-run! [store id opts]
    (protocol/patch-run! store id (merge {:status "completed" :has_active_stream false}
                                         (select-keys opts [:status :answer :error :trace_blocks :messages]))))
  (delete-run! [store id] (mutate! store {:kind :delete :run-id id}))
  protocol/IRunEventStore
  (append-event! [store event]
    (mutate! store {:kind :event :run-id (:run_id event) :event event :event-id (host/event-id event)}))
  (events-since [store id since] (events! store id since))
  directory-port/IRunDirectoryStore
  (list-runs [store scope] (directory! store scope)))

(defn create-mongo-run-store
  "Create the explicit Mongo provider. Bootstrap must await setup-indexes! before installation."
  ([db] (create-mongo-run-store db {}))
  ([db {:keys [clock! instance-id] :or {clock! clock/instant-iso}}]
   (when-not (and db (fn? clock!))
     (throw (ex-info "Mongo run provider requires a database and clock"
                     {:status 503 :code "run_store_invalid_provider"})))
   (let [owner (or instance-id (instance/current-id))]
     (host/stamp (clock!) owner)
     (->MongoRunStore db clock! owner))))

(defn ^:async append-run-event!
  "Compatibility entrypoint; callers must supply the complete runtime event and stable ID."
  [db run-id event]
  (law/require! law/NonBlank run-id)
  (when-not (= run-id (:run_id event)) (law/conflict! "Run event identity differs from requested run"))
  (await (protocol/append-event! (create-mongo-run-store db) event)))

(defn setup-indexes!
  "Create the actual provider's unique run identity and active-session indexes."
  [db]
  (mongo/setup-indexes! db))
