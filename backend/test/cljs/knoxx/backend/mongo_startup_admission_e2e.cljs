(ns knoxx.backend.mongo-startup-admission-e2e
  "Real standalone Mongo startup compensation, ambiguous acknowledgment and fencing proof."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.thread-store :as thread-domain]
            [knoxx.backend.extern.mongo-run-native-fixture :as native]
            [knoxx.backend.extern.mongo-startup-admission-fixture :as fixture]
            [knoxx.backend.infra.stores.mongo-run-store :as run-store]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.infra.stores.mongo-thread-store :as thread-store]
            [knoxx.backend.shape.session-persistence :as runs]
            [knoxx.backend.shape.startup-admission :as startup]
            [knoxx.backend.shape.thread-store :as threads]
            [knoxx.backend.startup-admission-provider-proof :as proof]))

(defn- run-provider [db]
  (run-store/create-mongo-run-store db {:clock! (constantly proof/at) :instance-id "startup-proof"}))

(defn- ^:async initialize! [db]
  (await (run-store/setup-indexes! db))
  (await (thread-store/setup-indexes! db)))

(defn- port [db kind]
  (if (= :run kind)
    {:provider (run-provider db) :collection "knoxx_runs" :id-field :run_id :read! runs/get-run}
    {:provider (thread-store/create-store db) :collection "knoxx_threads"
     :id-field :session_id :read! threads/read-thread}))

(deftest ^:async native-startup-provider-ownership-and-fifo-laws
  (let [owned (await (native/open!))]
    (try
      (await (initialize! (:db owned)))
      (await (proof/check-run-ownership! (run-provider (:db owned)) "ownership"))
      (await (proof/check-thread-retry! (thread-store/create-store (:db owned)) "retry"))
      (await (proof/check-thread-replacement! (thread-store/create-store (:db owned)) "replacement"))
      (finally (await (native/close! owned))))))

(deftest ^:async native-startup-view-refuses-invalid-ids-before-placeholder-write
  (let [owned (await (native/open!)) db (:db owned)]
    (try
      (await (initialize! db))
      (doseq [kind [:run :thread] id [nil "" "   "]]
        (let [{:keys [provider collection id-field]} (port db kind)
              result (await (proof/outcome! (startup/startup-view provider id)))]
          (is (= 400 (get-in result [:error :status])))
          (is (nil? (await (fixture/raw-document db collection id-field id)))
              "Invalid startup-view IDs cannot create even an inert native row")))
      (finally (await (native/close! owned))))))

(defn- ^:async check-fault! [db kind mode]
  (let [{:keys [provider collection id-field read!]} (port db kind)
        record (proof/record (str (name kind) "-" (name mode))) id (id-field record)
        view (await (startup/startup-view provider id))
        instrument (fixture/intercept db collection mode)
        fault-provider (:provider (port (:db instrument) kind))
        work (proof/outcome! (startup/claim-startup! fault-provider record view))]
    (await (:entered instrument))
    (if (= :delay mode)
      (do
        (try (await (startup/settle-startup! provider record view))
             (finally ((:release! instrument))))
        (is (= 409 (get-in (await work) [:error :status]))
            "A delayed DB publication loses to the durable failure fence"))
      (do
        (is (= "fixture_startup_ack_lost" (get-in (await work) [:error :code])))
        (is (= "running" (:status (await (read! provider id))))
            "The fault happened after Mongo actually accepted the claim")
        (await (startup/settle-startup! provider record view))))
    (is (= "failed" (:status (await (read! provider id)))))
    (is (= (:startup_token record) (:startup_token (await (read! provider id)))))
    (when (= :thread kind)
      (is (:can-send (thread-domain/session-can-send? (await (read! provider id))))))
    (is (seq @(:calls instrument)))
    (is (every? #(= {:w "majority" :j true} (get-in % [:options :writeConcern]))
                @(:calls instrument)))))

(deftest ^:async native-claims-lost-acknowledgment-and-late-publication
  (let [owned (await (native/open!))]
    (try
      (await (initialize! (:db owned)))
      (doseq [kind [:run :thread] mode [:lost-ack :delay]]
        (await (check-fault! (:db owned) kind mode)))
      (finally (await (native/close! owned))))))

(deftest ^:async native-delayed-thread-claim-cannot-resurrect-a-retired-fence
  (let [owned (await (native/open!)) db (:db owned)
        record (proof/record "retired-fence") id (:session_id record)]
    (try
      (await (initialize! db))
      (let [provider (thread-store/create-store db)
            view (await (startup/startup-view provider id))
            instrument (fixture/intercept db "knoxx_threads" :delay)
            work (proof/outcome! (startup/claim-startup! (thread-store/create-store (:db instrument)) record view))]
        (await (:entered instrument))
        (try
          (await (startup/settle-startup! provider record view))
          (is (await (fixture/retire-failed-row! db record)))
          (finally ((:release! instrument))))
        (is (= 409 (get-in (await work) [:error :status]))
            "Retiring a fence must not make the original absent preimage current again")
        (is (nil? (await (threads/read-thread provider id)))
            "A delayed rejected attempt must not resurrect a running session"))
      (finally (await (native/close! owned))))))

(deftest ^:async native-retired-fence-successor-is-not-overwritten-by-a-delayed-claim
  (let [owned (await (native/open!)) db (:db owned)
        record (proof/record "recreated-fence") id (:session_id record)]
    (try
      (await (initialize! db))
      (let [provider (thread-store/create-store db)
            view (await (startup/startup-view provider id))
            instrument (fixture/intercept db "knoxx_threads" :delay)
            work (proof/outcome! (startup/claim-startup! (thread-store/create-store (:db instrument)) record view))
            successor (assoc record :run_id "recreated-successor-run" :startup_token "recreated-successor-token")]
        (await (:entered instrument))
        (try
          (await (startup/settle-startup! provider record view))
          (is (await (fixture/retire-failed-row! db record)))
          (await (startup/claim-startup! provider successor (await (startup/startup-view provider id))))
          (finally ((:release! instrument))))
        (let [expected (await (threads/read-thread provider id))]
          (is (= 409 (get-in (await work) [:error :status])))
          (await (startup/settle-startup! provider record view))
          (is (= expected (await (threads/read-thread provider id))))
          (is (= "recreated-successor-token" (:startup_token expected)))))
      (finally (await (native/close! owned))))))

(defn- ^:async lose-settlement-ack! [db kind record]
  (let [{:keys [provider collection id-field read!]} (port db kind)
        id (id-field record) view (await (startup/startup-view provider id))]
    (await (startup/claim-startup! provider record view))
    (let [instrument (fixture/intercept db collection :lost-ack)
          fault-provider (:provider (port (:db instrument) kind))
          result (await (proof/outcome! (startup/settle-startup! fault-provider record view)))]
      (is (= "fixture_startup_ack_lost" (get-in result [:error :code])))
      (is (= "failed" (:status (await (read! provider id)))))
      (let [token-field (if (= :run kind) "persistence_token" "startup_cas_token")
            before (fixture/field (await (fixture/raw-document db collection id-field id)) token-field)]
        (await (startup/settle-startup! provider record view))
        (is (string? before))
        (is (not= before (fixture/field (await (fixture/raw-document db collection id-field id)) token-field))
            "Acknowledgment recovery publishes a fresh native version, never a read-only/no-op success"))
      (is (= "failed" (:status (await (read! provider id))))))))

(deftest ^:async native-settlement-reacknowledgment-and-history-survive-restart
  (let [owned* (atom (await (native/open!)))
        record (proof/record "restart")]
    (try
      (await (initialize! (:db @owned*)))
      (let [provider (run-provider (:db @owned*))
            view (await (startup/startup-view provider (:run_id record)))]
        (await (startup/claim-startup! provider record view))
        (await (runs/append-event! provider (proof/event record)))
        (await (startup/settle-startup! provider record view)))
      (doseq [kind [:run :thread]]
        (await (lose-settlement-ack! (:db @owned*) kind (proof/record (str "settle-" (name kind))))))
      (reset! owned* (await (native/restart! @owned*)))
      (let [provider (run-provider (:db @owned*))
            events (await (runs/events-since provider (:run_id record) nil))]
        (is (= "failed" (:status (await (runs/get-run provider (:run_id record))))))
        (is (= [(assoc (proof/event record) :sequence 1)] events))
        (is (= (first events) (await (runs/append-event! provider (proof/event record))))))
      (doseq [kind [:run :thread]]
        (let [{:keys [provider id-field read!]} (port (:db @owned*) kind)
              attempt (proof/record (str "settle-" (name kind)))]
          (is (= "failed" (:status (await (read! provider (id-field attempt))))))))
      (finally (await (native/close! @owned*))))))

(deftest ^:async native-thread-preimage-compares-bson-types
  (let [owned (await (native/open!)) record (proof/record "bson") id (:session_id record)]
    (try
      (await (initialize! (:db owned)))
      (let [provider (thread-store/create-store (:db owned))]
        (await (threads/put-thread! provider (assoc (dissoc record :startup_token) :status "completed")))
        (let [view (await (startup/startup-view provider id))]
          (await (fixture/replace-date-type! (:db owned) id))
          (let [before (fixture/fingerprint (await (fixture/raw-document (:db owned) "knoxx_threads" :session_id id)))
                result (await (proof/outcome! (startup/claim-startup! provider record view)))]
            (is (= 409 (get-in result [:error :status])) "Equal JSON is not an exact BSON preimage")
            (await (startup/settle-startup! provider record view))
            (is (= before (fixture/fingerprint (await (fixture/raw-document (:db owned) "knoxx_threads" :session_id id))))))))
      (finally (await (native/close! owned))))))

(deftest ^:async native-new-thread-must-fit-its-future-compensation
  (let [owned (await (native/open!))
        record (assoc (proof/record "large-new") :large_payload (fixture/large-text))
        id (:session_id record)]
    (try
      (await (initialize! (:db owned)))
      (let [provider (thread-store/create-store (:db owned))
            view (await (startup/startup-view provider id))
            result (await (proof/outcome! (startup/claim-startup! provider record view)))]
        (is (= 413 (get-in result [:error :status]))
            "A claim cannot fit admission but exceed the required later compensation command")
        (is (nil? (await (threads/read-thread provider id)))
            "Oversized fresh admission may reserve a generation but never a busy session")
        (await (proof/outcome! (startup/settle-startup! provider record view)))
        (is (nil? (await (threads/read-thread provider id)))
            "Failure compensation must not publish the oversized active record either"))
      (finally (await (native/close! owned))))))

(deftest ^:async native-generation-placeholder-is-invisible-to-public-reads-and-recovery
  (let [owned (await (native/open!)) db (:db owned)
        record (proof/record "placeholder") id (:session_id record)
        prior @sessions/provider*]
    (try
      (await (initialize! db))
      (let [provider (thread-store/create-store db)]
        (sessions/install! provider)
        (await (startup/startup-view provider id))
        (is (true? (fixture/field (await (fixture/raw-document db "knoxx_threads" :session_id id)) "startup_placeholder")))
        (is (nil? (await (threads/read-thread provider id))))
        (is (nil? (await (threads/conversation-thread provider (:conversation_id record)))))
        (is (= [] (await (threads/active-threads provider))))
        (is (nil? (await (sessions/get-session id))))
        (is (nil? (sessions/get-session-sync id)))
        (is (= [] (await (sessions/recover-sessions!))))
        (is (= [] (sessions/active-session-snapshots)))
        (await (threads/put-thread! provider record))
        (is (not (true? (fixture/field (await (fixture/raw-document db "knoxx_threads" :session_id id)) "startup_placeholder"))))
        (is (= (:run_id record) (:run_id (await (sessions/get-session id)))))
        (is (= (:run_id record) (:run_id (sessions/get-session-sync id)))))
      (finally (sessions/install! prior) (await (native/close! owned))))))

(deftest ^:async native-lost-placeholder-acknowledgment-admits-no-thread
  (let [owned (await (native/open!)) db (:db owned)
        record (proof/record "placeholder-ack") id (:session_id record)]
    (try
      (await (initialize! db))
      (let [instrument (fixture/intercept db "knoxx_threads" :lost-ack)
            fault-provider (thread-store/create-store (:db instrument))
            result (await (proof/outcome! (startup/startup-view fault-provider id)))
            provider (thread-store/create-store db)]
        (is (= "fixture_startup_ack_lost" (get-in result [:error :code])))
        (is (nil? (await (threads/read-thread provider id))))
        (is (= [] (await (threads/active-threads provider))))
        (let [view (await (startup/startup-view provider id))]
          (await (startup/claim-startup! provider record view))
          (is (= "running" (:status (await (threads/read-thread provider id)))))
          (await (startup/settle-startup! provider record view))
          (is (= "failed" (:status (await (threads/read-thread provider id)))))))
      (finally (await (native/close! owned))))))

(defn- ^:async check-run-retirement! [db retirement]
  (let [clock* (atom proof/at)
        make-provider #(run-store/create-mongo-run-store % {:clock! (fn [] @clock*) :instance-id "retirement-proof"})
        provider (make-provider db) record (proof/record (str "run-" (name retirement)))
        id (:run_id record)]
    (await (startup/claim-startup! provider (assoc record :status "queued") (await (startup/startup-view provider id))))
    (let [accepted (await (runs/append-event! provider (proof/event record)))
          view (await (startup/startup-view provider id))
          instrument (fixture/intercept db "knoxx_runs" :delay)
          work (proof/outcome! (startup/claim-startup! (make-provider (:db instrument)) record view))]
      (await (:entered instrument))
      (try
        (await (startup/settle-startup! provider record view))
        (if (= retirement :delete) (await (runs/delete-run! provider id))
            (reset! clock* "2030-01-01T00:00:00.000Z"))
        (is (nil? (await (runs/get-run provider id))))
        (is (some? (await (fixture/raw-document db "knoxx_runs" :run_id id))) "Expiry/delete retain the binding and head fence")
        (finally ((:release! instrument))))
      (is (= 409 (get-in (await work) [:error :status])))
      (is (nil? (await (runs/get-run provider id))))
      (await (runs/put-run! provider (assoc record :status "failed")))
      (is (= [accepted] (await (runs/events-since provider id nil)))))))

(deftest ^:async native-run-delete-and-expiry-retain-authority-against-delayed-claims
  (let [owned (await (native/open!))]
    (try
      (await (initialize! (:db owned)))
      (doseq [retirement [:delete :expiry]] (await (check-run-retirement! (:db owned) retirement)))
      (finally (await (native/close! owned))))))

(deftest ^:async native-large-thread-preimage-refuses-without-changing-owner
  (let [owned (await (native/open!)) record (proof/record "large") id (:session_id record)]
    (try
      (await (initialize! (:db owned)))
      (let [provider (thread-store/create-store (:db owned))]
        (await (threads/put-thread! provider (assoc (dissoc record :startup_token)
                                                  :status "completed" :large_payload (fixture/large-text))))
        (let [view (await (startup/startup-view provider id))
              before (fixture/fingerprint (await (fixture/raw-document (:db owned) "knoxx_threads" :session_id id)))
              result (await (proof/outcome! (startup/claim-startup! provider record view)))]
          (is (= 413 (get-in result [:error :status])) "Unsafe BSON command geometry is classified before publication")
          (is (= before (fixture/fingerprint (await (fixture/raw-document (:db owned) "knoxx_threads" :session_id id)))))
          (is (:can-send (thread-domain/session-can-send? (await (threads/read-thread provider id)))))))
      (finally (await (native/close! owned))))))
