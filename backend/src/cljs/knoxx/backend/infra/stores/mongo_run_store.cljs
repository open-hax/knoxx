(ns knoxx.backend.infra.stores.mongo-run-store
  "Atomic Mongo run state and ordered events using the same finite transitions as Clio."
  (:require [knoxx.backend.domain.mongo-run-store :as snapshot]
            [knoxx.backend.domain.run-directory :as directory]
            [knoxx.backend.domain.run-store :as domain]
            [knoxx.backend.extern.clock :as clock]
            [knoxx.backend.extern.mongo-run-events :as native-events]
            [knoxx.backend.extern.mongo-run-store :as mongo]
            [knoxx.backend.extern.run-store :as host]
            [knoxx.backend.infra.mongo-run-events :as events]
            [knoxx.backend.infra.system-instance :as instance]
            [knoxx.backend.law.run-event :as event-law]
            [knoxx.backend.law.run-store :as law]
            [knoxx.backend.shape.run-directory :as directory-port]
            [knoxx.backend.shape.session-persistence :as protocol]))

(defn- sample [store] (host/stamp ((:clock! store)) (:instance-id store)))

(defn- ^:async read-state! [store id]
  (snapshot/restore (await (mongo/read! (:db store) id)) id))

(defn- current-run! [state run-id stamp]
  (or (domain/visible-run state run-id (:at-ms stamp))
      (throw (ex-info "Run is absent or expired" {:status 404 :code "run_store_not_found"}))))

(defn- ^:async event-transition! [store previous state {:keys [run-id event event-id]}]
  ;; An old retry after expiry/delete is still an append and must refuse.
  (current-run! state run-id (sample store))
  (let [existing (await (events/existing! (:db store) previous state run-id event-id))
        stamp (sample store)
        last-sequence (or (get-in previous [:event-chain :last-sequence])
                          (count (get-in state [:events run-id])))
        admitted (event-law/admit (current-run! state run-id stamp) event event-id existing last-sequence)]
    [(if (:existing? admitted) state (assoc-in state [:runs run-id :expires-ms] (:expires-ms stamp)))
     (:event admitted) (not (:existing? admitted))]))

(defn- ^:async mutate! [store operation]
  (let [id (:run-id operation)]
    (loop [attempt 0]
      (when (>= attempt 32)
        (throw (ex-info "Concurrent Mongo run writers exceeded bounded admission retries"
                        {:status 409 :code "run_store_concurrent_write"})))
      (let [previous (await (mongo/read! (:db store) id))
            state (snapshot/restore previous id)
            [next-state result append?] (if (= :event (:kind operation))
                                         (await (event-transition! store previous state operation))
                                         (domain/transition state (assoc operation :stamp (sample store))))]
        (if (and (nil? previous) (= state next-state)) result
          (let [chain (await (events/prepare-history! (:db store) previous state id))
                next-chain (if append? (await (native-events/prepare! (:db store) result (:tail chain))) chain)]
            ;; Even exact retries force a revision write: visible history alone
            ;; cannot repair a prior ambiguous journal acknowledgement.
            (if (await (mongo/compare-and-swap! (:db store) id previous next-state next-chain))
              result
              (recur (inc attempt)))))))))

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
  (law/require! [:or :nil [:int {:min 0}] law/Instant] since)
  (let [record (await (mongo/read! (:db store) run-id))
        state (snapshot/restore record run-id)]
    (if-let [chain (:event-chain record)]
      (if (domain/visible-run state run-id (:at-ms (sample store)))
        (await (events/replay! (:db store) (get-in state [:bindings run-id]) chain since))
        [])
      (domain/events-since state run-id since (:at-ms (sample store))))))

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

(defn ^:async setup-indexes!
  "Create the actual provider's unique run identity and active-session indexes."
  [db]
  (await (mongo/setup-indexes! db))
  (await (native-events/setup-indexes! db)))
