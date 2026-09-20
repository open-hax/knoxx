(ns knoxx.backend.mongo-recovery-handoff-e2e
  "Actual Mongo recovery release and real turn admission, with deterministic native races."
  (:require [cljs.test :as test]
            [knoxx.backend.extern.mongo-run-native-fixture :as native]
            [knoxx.backend.extern.mongo-startup-admission-fixture :as faults]
            [knoxx.backend.extern.recovery-handoff-fixture :as fixture]
            [knoxx.backend.infra.stores.mongo-run-store :as runs]
            [knoxx.backend.infra.stores.mongo-thread-store :as threads]
            [knoxx.backend.shape.thread-recovery :as recovery]
            [knoxx.backend.shape.thread-store :as thread]))

(defn- ^:async with-store! [operation]
  (let [owned (await (native/open!)) db (:db owned)]
    (try
      (await (runs/setup-indexes! db)) (await (threads/setup-indexes! db))
      (await (operation db (threads/create-store db) (runs/create-mongo-run-store db)))
      (finally (await (native/close! owned))))))

(defn- ^:async outcome! [promise]
  (try {:value (await promise)} (catch :default error {:error (ex-data error)})))

(test/deftest ^:async actual-mongo-recovery-entrypoint-and-historical-run-policy
  (await (with-store!
          (^:async fn [_ provider run-provider]
            (doseq [legacy? [false true] scan? [false true :cache]]
              (await (fixture/success! provider provider run-provider (str "native-" legacy? "-" scan?) legacy? scan?)))))))

(test/deftest ^:async native-recovery-receipt-rejects-identical-wire-changed-bson
  (await (with-store!
          (^:async fn [db provider run-provider]
            (await (fixture/refused!
                    provider provider run-provider "native-bson"
                    (^:async fn [_ value]
                      (await (faults/replace-date-type! db (:session_id value))) value)))))))

(defn- ^:async fingerprint! [db id]
  (when-let [raw (await (faults/raw-document db "knoxx_threads" :session_id id))]
    (faults/fingerprint raw)))

(defn- ^:async delayed-release! [db provider recreate?]
  (let [value (fixture/record (str "native-delay-" recreate?)) id (:session_id value)]
    (await (fixture/seed! provider value))
    (let [snapshot (await (thread/read-thread provider id))
          instrument (faults/intercept db "knoxx_threads" :delay)
          work (outcome! (recovery/release-recovery! (threads/create-store (:db instrument)) snapshot))]
      (await (:entered instrument))
      (try
        (await (thread/delete-thread! provider id))
        (when recreate?
          (await (thread/put-thread! provider (assoc value :run_id "successor" :startup_token "successor"))))
        (let [before (await (fingerprint! db id))]
          ((:release! instrument))
          (test/is (= "thread_recovery_conflict" (get-in (await work) [:error :code])))
          (test/is (= before (await (fingerprint! db id))))
          (test/is (= (when recreate? "successor") (:run_id (await (thread/read-thread provider id))))))
        (finally ((:release! instrument)) (await work))))))

(test/deftest ^:async native-late-release-cannot-resurrect-or-overwrite-a-recreated-successor
  (await (with-store!
          (^:async fn [db provider _]
            (doseq [recreate? [false true]]
              (await (delayed-release! db provider recreate?)))))))

(test/deftest ^:async native-lost-release-acknowledgment-is-idle-and-never-retried-as-an-owner
  (await (with-store!
          (^:async fn [db provider _]
            (let [value (fixture/record "native-lost") id (:session_id value)]
              (await (fixture/seed! provider value))
              (let [snapshot (await (thread/read-thread provider id))
                    instrument (faults/intercept db "knoxx_threads" :lost-ack)
                    result (await (outcome! (recovery/release-recovery! (threads/create-store (:db instrument)) snapshot)))]
                (test/is (= "fixture_startup_ack_lost" (get-in result [:error :code])))
                (test/is (= "waiting_input" (:status (await (thread/read-thread provider id)))))
                (test/is (= "thread_recovery_conflict"
                            (get-in (await (outcome! (recovery/release-recovery! provider snapshot))) [:error :code])))
                (test/is (every? #(= {:w "majority" :j true} (get-in % [:options :writeConcern]))
                                 @(:calls instrument)))))))))
