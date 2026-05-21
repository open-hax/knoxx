(ns knoxx.backend.infra.stores.redis-session-store
  (:require [shadow.cljs.modern :refer [js-await]]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.shape.session-persistence :refer [ISessionStore assert-run!]]
            [knoxx.backend.domain.time :as time]))

(def ^:private RUN_PREFIX "knoxx:run:")
(def ^:private RUN_TTL 7200)
(def ^:private SESS_RUNS "knoxx:session_runs:")

(defn- run-key [run-id] (str RUN_PREFIX run-id))
(defn- sess-key [session-id] (str SESS_RUNS session-id))

(defrecord RedisSessionStore [client]
  ISessionStore

  (put-run! [_ run]
    (assert-run! run "RedisSessionStore/put-run!")
    (js-await [_ (redis/set-json client (run-key (:run_id run)) run RUN_TTL)]
      (redis/sadd client (sess-key (:session_id run)) (:run_id run))
      run))

  (get-run [_ run-id]
    (redis/get-json client (run-key run-id)))

  (patch-run! [store run-id patch]
    (js-await [current (.get-run store run-id)]
      (when-let [base (or current
                           (throw (ex-info "patch-run! on unknown run"
                                           {:run-id run-id :patch-keys (keys patch)})))]
        (let [updated (merge base patch {:updated_at (time/now-iso)})]
          (.put-run! store updated)))))

  (list-active-runs [store session-id]
    (js-await [run-ids (redis/smembers client (sess-key session-id))]
      (js-await [runs (js/Promise.all
                        (clj->js (mapv #(.get-run store %) run-ids)))]
        (->> (js->clj runs :keywordize-keys true)
             (remove nil?)
             (filter #(contains? #{"running" "queued" "waiting_input"}
                                  (:status %)))
             vec))))

  (complete-run! [store run-id opts]
    (.patch-run! store run-id
                 (merge {:status "completed"
                         :has_active_stream false
                         :updated_at (time/now-iso)}
                        (select-keys opts [:status :answer :error
                                           :trace_blocks :messages]))))

  (delete-run! [_ run-id]
    (js-await [_ (redis/del client (run-key run-id))]
      true)))