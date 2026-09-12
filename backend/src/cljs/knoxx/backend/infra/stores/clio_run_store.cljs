(ns knoxx.backend.infra.stores.clio-run-store
  "Durable finite run and ordered event protocols over canonical Clio."
  (:require [knoxx.backend.domain.run-store :as domain]
            [knoxx.backend.extern.clock :as clock]
            [knoxx.backend.extern.local-policy :as locks]
            [knoxx.backend.extern.run-store :as host]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.system-instance :as instance]
            [knoxx.backend.shape.session-persistence :as protocol]))

(defn- projection []
  (let [state (atom domain/empty-state)] {:store state :snapshot #(deref state)}))

(defn- admit! [state operation]
  (let [[next-state result] (domain/transition @state operation)] (reset! state next-state) result))

(defn- sample [store] (host/stamp ((:clock! store)) (:instance-id store)))

(defn- ^:async mutate! [store operation]
  (await (locks/with-lock! (str (:directory store) "/run-store.lock")
          (fn [] (clio/write! (:engine store) :run/admit [(assoc operation :stamp (sample store))])))))

(defrecord ClioRunStore [engine directory clock! instance-id]
  protocol/ISessionStore
  (put-run! [store run] (mutate! store {:kind :put :run-id (:run_id run) :run run}))
  (get-run [store run-id] (clio/read! engine :run/read [run-id (:at-ms (sample store))]))
  (patch-run! [store run-id patch] (mutate! store {:kind :patch :run-id run-id :patch patch}))
  (list-active-runs [store session-id] (clio/read! engine :run/active [session-id (:at-ms (sample store))]))
  (complete-run! [store run-id opts]
    (protocol/patch-run! store run-id (merge {:status "completed" :has_active_stream false}
                                            (select-keys opts [:status :answer :error :trace_blocks :messages]))))
  (delete-run! [store run-id] (mutate! store {:kind :delete :run-id run-id}))
  protocol/IRunEventStore
  (append-event! [store event]
    (mutate! store {:kind :event :run-id (:run_id event) :event event :event-id (host/event-id event)}))
  (events-since [store run-id since]
    (clio/read! engine :run/events [run-id since (:at-ms (sample store))])))

(defn open!
  "Open a required directory with validated clock and instance identity before touching disk."
  [{:keys [directory clock! instance-id] :or {clock! clock/instant-iso}}]
  (when-not (fn? clock!)
    (throw (ex-info "Run clock must be a function" {:status 400 :code "run_store_invalid_clock"})))
  (let [instance-id (or instance-id (instance/current-id))
        _ (host/stamp (clock!) instance-id)
        engine (clio/open! {:directory directory :stream "knoxx/runs" :projection projection
                            :reads {:run/read (fn [state id at] (domain/visible-run @state id at))
                                    :run/active (fn [state id at] (domain/active-runs @state id at))
                                    :run/events (fn [state id since at] (domain/events-since @state id since at))}
                            :writes {:run/admit admit!}})]
    (->ClioRunStore engine (:directory engine) clock! instance-id)))
