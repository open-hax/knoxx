(ns knoxx.backend.mongo-event-chunks-e2e
  "Native bounded event records, interrupted publication and exact legacy migration."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.mongo-run-store :as snapshot]
            [knoxx.backend.extern.mongo-event-chunks-fixture :as fixture]
            [knoxx.backend.extern.mongo-run-events :as native-events]
            [knoxx.backend.extern.mongo-run-store :as native]
            [knoxx.backend.extern.provider-recovery-fixture :as concurrent]
            [knoxx.backend.law.mongo-run-events :as law]
            [knoxx.backend.infra.stores.mongo-run-store :as mongo]
            [knoxx.backend.shape.session-persistence :as protocol]))

(def at "2026-09-12T12:00:00.000Z")
(def run {:run_id "run" :session_id "session" :conversation_id "conversation"
          :org_id "org" :user_id "user" :status "running" :created_at at :updated_at at})
(def event {:run_id "run" :session_id "session" :conversation_id "conversation"
            :event_id "event" :type "tool_update" :at at :payload {:review/state :review/accepted}})

(defn- open
  ([fixture] (open fixture (atom at)))
  ([fixture clock]
   (mongo/create-mongo-run-store (:db fixture) {:clock! #(deref clock) :instance-id "native"})))

(defn- ^:async refusal [f]
  (try (await (f)) nil (catch :default cause (or (ex-data cause) {:message (str cause)}))))

(defn- interrupted! []
  (throw (ex-info "Injected loss after actual native write" {:status 503 :code "injected_lost_ack"})))

(defn- ^:async seed-v1! [fixture provider run events]
  (let [id (:run_id run)]
    (await (protocol/put-run! provider run))
    (let [head (await (fixture/head (:db fixture) id))
          state (assoc (select-keys head [:runs :bindings]) :events {id events})]
      (await (fixture/replace! (:db fixture) native/collection-name {:run_id id}
                              {:run_id id :persistence_revision 1 :run_state_edn (pr-str state)})))))

(deftest ^:async near-limit-metadata-remains-writable-and-oversize-refuses-before-publication
  (let [fixture (await (fixture/open!)) provider (open fixture)]
    (try
      (await (mongo/setup-indexes! (:db fixture)))
      (await (protocol/put-run! provider (assoc run :answer (apply str (repeat (* 9 1024 1024) "a")))))
      (is (= "completed" (:status (await (protocol/patch-run! provider "run" {:status "completed"})))))
      (is (= (* 9 1024 1024) (count (:answer (await (protocol/get-run provider "run"))))))
      (let [before (await (fixture/document (:db fixture) native/collection-name {:run_id "run"}))]
        (is (= "run_store_record_too_large"
               (:code (await (refusal #(protocol/patch-run! provider "run" {:answer (apply str (repeat (* 16 1024 1024) "b"))}))))))
        (is (= before (await (fixture/document (:db fixture) native/collection-name {:run_id "run"})))))
      (is (= "run_store_record_too_large"
             (:code (await (refusal #(protocol/put-run! provider (assoc run :run_id "oversize" :answer (apply str (repeat (* 16 1024 1024) "b")))))))))
      (is (nil? (await (protocol/get-run provider "oversize"))))
      (catch :default cause
        (is false (str "Native near-limit replacement refused: " cause)))
      (finally (await (fixture/close! fixture))))))

(deftest ^:async accepted-history-exceeds-the-old-document-ceiling-with-bounded-fresh-writes
  (let [fixture (atom (await (fixture/open!))) provider (open @fixture)
        small (apply str (repeat 17000 "x"))]
    (try
      (await (mongo/setup-indexes! (:db @fixture)))
      (await (protocol/put-run! provider run))
      (doseq [i (range 1001)]
        (when (= i 1000) (reset! (:metrics @fixture) {}))
        (await (protocol/append-event! provider (assoc event :event_id (str "event-" i) :payload {:text small}))))
      (let [metrics @(:metrics @fixture)]
        (is (= 1 (get-in metrics [native/collection-name :records])))
        (is (= 1 (get-in metrics [native-events/header-collection :records])))
        (is (= 1 (get-in metrics [native-events/fragment-collection :records])))
        (is (< (get-in metrics [native/collection-name :max-command-bytes]) 4096))
        (is (<= (get-in metrics [native-events/fragment-collection :max-record-bytes]) law/fragment-record-bytes))
        (is (zero? (get metrics :non-durable-writes 0)))
        (println "Bounded append after 1000 accepted events:" (pr-str metrics)))
      (let [history (await (protocol/events-since provider "run" nil))]
        (is (= (vec (range 1 1002)) (mapv :sequence history)))
        (is (> (fixture/utf8-bytes (pr-str history)) (* 16 1024 1024))))
      (reset! fixture (await (fixture/restart! @fixture)))
      (let [reopened (open @fixture)]
        (is (= [1000 1001] (mapv :sequence (await (protocol/events-since reopened "run" 999)))))
        (is (= 1 (:sequence (await (protocol/append-event! reopened (assoc event :event_id "event-0" :payload {:text small}))))))
        (is (= 409 (:status (await (refusal #(protocol/append-event! reopened (assoc event :event_id "event-0"))))))))
      (catch :default cause (is false (str "Native bounded history: " cause)))
      (finally (await (fixture/close! @fixture))))))

(deftest ^:async one-event-larger-than-sixteen-mib-is-lossless-across-fragments-and-restart
  (let [fixture (atom (await (fixture/open!))) provider (open @fixture)
        text (str "\uD800" (apply str (repeat (* 17 1024 1024) "z")) "🌱é\uDC00")
        large (assoc event :payload {:text text :review/state :review/accepted})]
    (try
      (await (mongo/setup-indexes! (:db @fixture)))
      (await (protocol/put-run! provider run))
      (is (> (fixture/utf8-bytes (pr-str large)) (* 16 1024 1024)))
      (is (= 1 (:sequence (await (protocol/append-event! provider large)))))
      (is (> (await (fixture/count-documents (:db @fixture) native-events/fragment-collection)) 500))
      (is (<= (get-in @(:metrics @fixture) [native-events/fragment-collection :max-record-bytes]) law/fragment-record-bytes))
      (is (<= (get-in @(:metrics @fixture) [native-events/header-collection :max-record-bytes]) law/header-record-bytes))
      (reset! fixture (await (fixture/restart! @fixture)))
      (let [replayed (first (await (protocol/events-since (open @fixture) "run" nil)))]
        (is (= (fixture/fingerprint text) (fixture/fingerprint (get-in replayed [:payload :text]))))
        (is (= :review/accepted (get-in replayed [:payload :review/state])))
        (is (= 1 (:sequence replayed))))
      (catch :default cause (is false (str "Native fragmented large event: " cause)))
      (finally (await (fixture/close! @fixture))))))

(deftest ^:async incomplete-or-acknowledgment-lost-preparations-never-reserve-an-event-id
  (let [fixture (await (fixture/open!)) provider (open fixture) actual-insert native-events/insert-prepared!
        payload (apply str (repeat 80000 "x"))]
    (try
      (await (mongo/setup-indexes! (:db fixture)))
      (doseq [stage [:fragment-prefix :header-ack]]
        (let [id (name stage) attempted (assoc event :run_id id :payload {:text payload}) writes (atom 0)]
          (await (protocol/put-run! provider (assoc run :run_id id)))
          (with-redefs [native-events/insert-prepared!
                        (^:async fn [db collection doc limit]
                          (let [result (await (actual-insert db collection doc limit))]
                            (when (or (and (= stage :fragment-prefix) (= collection native-events/fragment-collection)
                                           (= 2 (swap! writes inc)))
                                      (and (= stage :header-ack) (= collection native-events/header-collection)))
                              (interrupted!)) result))]
            (is (= "injected_lost_ack" (:code (await (refusal #(protocol/append-event! provider attempted)))))))
          (is (= [] (await (protocol/events-since provider id nil))))
          (is (= 1 (:sequence (await (protocol/append-event! provider (assoc attempted :payload {:changed :unaccepted}))))))
          (is (= [:unaccepted] (mapv #(get-in % [:payload :changed]) (await (protocol/events-since provider id nil)))))))
      (is (> (await (fixture/count-documents (:db fixture) native-events/fragment-collection)) 4))
      (is (zero? (get @(:metrics fixture) :non-durable-writes 0)))
      (catch :default cause (is false (str "Native preparation acknowledgment: " cause)))
      (finally (await (fixture/close! fixture))))))

(deftest ^:async publication-before-and-after-lost-ack-preserves-exact-retry-and-metadata
  (let [fixture (await (fixture/open!)) clock (atom at) provider (open fixture clock)
        actual-cas native/compare-and-swap!]
    (try
      (await (mongo/setup-indexes! (:db fixture)))
      (await (protocol/put-run! provider run))
      (with-redefs [native/compare-and-swap! (^:async fn [_ _ _ _ _] (interrupted!))]
        (is (= "injected_lost_ack" (:code (await (refusal #(protocol/append-event! provider event)))))))
      (is (= [] (await (protocol/events-since provider "run" nil))))
      (with-redefs [native/compare-and-swap! (^:async fn [db id previous state chain]
                                              (await (actual-cas db id previous state chain)) (interrupted!))]
        (is (= "injected_lost_ack" (:code (await (refusal #(protocol/append-event! provider event)))))))
      (reset! clock "2026-09-12T12:10:00.000Z")
      (let [results (await (concurrent/settled [(protocol/patch-run! provider "run" {:answer "patch survives"})
                                               (protocol/append-event! provider (assoc event :event_id "later"))]))]
        (is (every? #(= :fulfilled (:status %)) results)))
      (let [before (await (fixture/document (:db fixture) native/collection-name {:run_id "run"}))]
        (reset! clock "2026-09-12T12:20:00.000Z")
        (is (= 1 (:sequence (await (protocol/append-event! provider event)))))
        (let [after (await (fixture/document (:db fixture) native/collection-name {:run_id "run"}))]
          (is (= (inc (:persistence_revision before)) (:persistence_revision after)))
          (is (not= (:persistence_token before) (:persistence_token after)))
          (is (= (:run_state_edn before) (:run_state_edn after)))))
      (is (= "patch survives" (:answer (await (protocol/get-run provider "run")))))
      (await (protocol/delete-run! provider "run"))
      (is (= 404 (:status (await (refusal #(protocol/append-event! provider event))))))
      (await (protocol/put-run! provider run))
      (is (= [1 2] (mapv :sequence (await (protocol/events-since provider "run" nil)))))
      (catch :default cause (is false (str "Native head acknowledgment: " cause)))
      (finally (await (fixture/close! fixture))))))

(deftest ^:async canonical-migration-preserves-events-and-retries-after-an-old-writer-removes-the-fence
  (let [fixture (await (fixture/open!)) provider (open fixture)
        history [(assoc event :sequence 1) (assoc event :event_id "second" :sequence 2)]
        actual-fence native/fence-canonical! once? (atom true)]
    (try
      (await (mongo/setup-indexes! (:db fixture)))
      (await (seed-v1! fixture provider run history))
      (with-redefs [native/fence-canonical!
                    (^:async fn [db id previous]
                      (let [fenced (await (actual-fence db id previous))]
                        (when (compare-and-set! once? true false)
                          (await (fixture/replace! db native/collection-name {:run_id id}
                                                  {:run_id id :persistence_revision (inc (:revision fenced))
                                                   :run_state_edn (pr-str (assoc-in (:state previous) [:runs id :run :answer]
                                                                                  "old canonical writer survives"))})))
                        fenced))]
        (is (= 3 (:sequence (await (protocol/append-event! provider (assoc event :event_id "new")))))))
      (is (= "old canonical writer survives" (:answer (await (protocol/get-run provider "run")))))
      (let [events (await (protocol/events-since provider "run" nil))]
        (is (= [1 2 3] (mapv :sequence events)))
        (is (= history (subvec events 0 2))))
      (is (> (await (fixture/count-documents (:db fixture) native-events/header-collection)) 3)
          "Losing migration candidates remain unaccepted and never replace the winning chain")
      (catch :default cause (is false (str "Native canonical migration: " cause)))
      (finally (await (fixture/close! fixture))))))

(deftest ^:async lost-canonical-stamp-ack-can-retry-large-metadata-without-inventing-a-new-fact
  (let [fixture (await (fixture/open!)) provider (open fixture) actual-fence native/fence-canonical!
        history [(assoc event :sequence 1)] large-run (assoc run :answer (apply str (repeat (* 9 1024 1024) "a")))]
    (try
      (await (mongo/setup-indexes! (:db fixture)))
      (await (seed-v1! fixture provider large-run history))
      (with-redefs [native/fence-canonical! (^:async fn [db id previous]
                                             (await (actual-fence db id previous)) (interrupted!))]
        (is (= "injected_lost_ack" (:code (await (refusal #(protocol/append-event! provider (assoc event :event_id "new"))))))))
      (is (= history (await (protocol/events-since provider "run" nil))))
      (is (= 2 (:sequence (await (protocol/append-event! provider (assoc event :event_id "new"))))))
      (is (= [1 2] (mapv :sequence (await (protocol/events-since provider "run" nil)))))
      (is (= (* 9 1024 1024) (count (:answer (await (protocol/get-run provider "run"))))))
      (catch :default cause (is false (str "Native canonical stamp acknowledgment: " cause)))
      (finally (await (fixture/close! fixture))))))

(deftest ^:async revisionless-native-adoption-fences-complete-bson-and-refuses-oversized-replacement
  (let [fixture (await (fixture/open!)) provider (open fixture) db (:db fixture)]
    (try
      (await (mongo/setup-indexes! db))
      (await (fixture/insert! db native/collection-name (assoc run :expiresAt (fixture/date "2026-09-12T14:00:00.000Z"))))
      (let [previous (await (native/read! db "run")) state (snapshot/restore previous "run")]
        (await (fixture/change! db native/collection-name {:run_id "run"} {:answer "concurrent raw writer"}))
        (is (false? (await (native/compare-and-swap! db "run" previous state law/empty-chain))))
        (is (= "concurrent raw writer" (:answer (await (protocol/get-run provider "run")))))
        (await (protocol/patch-run! provider "run" {:status "completed"}))
        (is (= :knoxx.run/v2 (:format (await (fixture/head db "run"))))))
      (await (fixture/insert! db native/collection-name
                             (assoc run :run_id "large-legacy" :answer (apply str (repeat (* 9 1024 1024) "a"))
                                    :expiresAt (fixture/date "2026-09-12T14:00:00.000Z"))))
      (let [before (await (fixture/document-fingerprint db native/collection-name {:run_id "large-legacy"}))]
        (is (= "run_store_legacy_record_too_large"
               (:code (await (refusal #(protocol/patch-run! provider "large-legacy" {:status "completed"}))))))
        (is (= before (await (fixture/document-fingerprint db native/collection-name {:run_id "large-legacy"})))))
      (catch :default cause (is false (str "Native revisionless migration: " cause)))
      (finally (await (fixture/close! fixture))))))

(deftest ^:async missing-accepted-fragment-refuses-replay-instead-of-returning-a-partial-history
  (let [fixture (await (fixture/open!)) provider (open fixture) db (:db fixture)]
    (try
      (await (mongo/setup-indexes! db))
      (await (protocol/put-run! provider run))
      (await (protocol/append-event! provider event))
      (await (protocol/append-event! provider (assoc event :event_id "later")))
      (let [head (await (fixture/head db "run"))
            tail (await (native-events/read-header! db (get-in head [:event-chain :tail]) (native-events/identity-key "run") 2))
            first-header (await (native-events/read-header! db (:previous tail) (native-events/identity-key "run") 1))]
        (await (fixture/remove! db native-events/fragment-collection {:_id (:first-fragment first-header)})))
      (is (= "run_store_corrupt" (:code (await (refusal #(protocol/events-since provider "run" nil))))))
      (is (= [2] (mapv :sequence (await (protocol/events-since provider "run" 1)))))
      (is (= "run_store_corrupt" (:code (await (refusal #(protocol/append-event! provider event))))))
      (catch :default cause (is false (str "Native corrupt fragment: " cause)))
      (finally (await (fixture/close! fixture))))))
