(ns knoxx.backend.mongo-thread-cache-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.mongo-cache :as cache]
            [knoxx.backend.extern.mongo-cache-fixture :as fixture]
            [knoxx.backend.extern.mongo-thread :as native]
            [knoxx.backend.extern.provider-recovery-fixture :as concurrent]
            [knoxx.backend.extern.thread-store :as clock]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.infra.stores.mongo-thread-store :as mongo]
            [knoxx.backend.law.thread-store :as law]
            [knoxx.backend.shape.thread-store :as protocol]
            [knoxx.backend.thread-identity-proof :as identity-proof]))

(def ^:private at 1000000000000)

(defn- rows []
  [{:session_id "live" :conversation_id "conversation-live" :status "running"
    :expiresAt (fixture/date (+ at 50)) :createdAt (fixture/date (- at 50))
    :updatedAt (fixture/date at) :_id (fixture/native-id)}
   {:session_id "expired" :conversation_id "conversation-expired" :status "queued" :expiresAt (fixture/date at)}
   {:session_id "missing" :conversation_id "conversation-missing" :status "waiting_input"}
   {:session_id "invalid" :conversation_id "conversation-invalid" :status "running" :expiresAt (fixture/invalid-date)}])

(deftest ^:async mongo-thread-reads-enforce-expiry-before-ttl-cleanup
  (await (fixture/with-clock! at
    (^:async fn []
      (let [{:keys [db queries]} (fixture/database {native/COLLECTION_NAME (rows)})
            live (await (native/find-session db "live"))]
        (is (= "live" (:session_id live)))
        (is (= (fixture/iso (+ at 50)) (:expiresAt live)))
        (is (law/edn-value? live))
        (is (not-any? #(contains? live %) [:_id :createdAt :updatedAt]))
        (doseq [id ["expired" "missing" "invalid"]]
          (is (nil? (await (native/find-session db id))))
          (is (nil? (await (native/find-session-by-conversation db (str "conversation-" id))))))
        (is (= ["live"] (mapv :session_id (await (native/fetch-active-sessions db)))))
        (is (every? #(= at (get-in % [:query :expiresAt :$gt])) @queries)
            "Every native read constrains expiry independently of the TTL monitor"))))))

(deftest ^:async mongo-thread-decoder-refuses-expired-stale-query-results
  (await (fixture/with-clock! at
    (^:async fn []
      (let [{:keys [db]} (fixture/database {native/COLLECTION_NAME (rows)} false)]
        (is (nil? (await (native/find-session db "expired"))))
        (is (nil? (await (native/find-session-by-conversation db "conversation-expired"))))
        (is (= ["live"] (mapv :session_id (await (native/fetch-active-sessions db))))))))))

(deftest ^:async mongo-admissions-return-portable-expiry-and-refresh-patch-time
  (let [{:keys [db]} (fixture/database {})
        provider (mongo/create-store db)]
    (await (fixture/with-clock! at
      (^:async fn []
        (doseq [id ["normal" "normal-sticky"]]
          (let [written (await (protocol/put-thread! provider {:session_id id :updated_at 1 :status "running"
                                                             :messages [{:role "user" :content "Hello"}]}))]
            (is (= (fixture/iso (+ at (law/ttl-ms id))) (:expiresAt written)))
            (is (= written (await (protocol/read-thread provider id))))
            (is (law/edn-value? written)))))))
    (await (fixture/with-clock! (+ at 100)
      (^:async fn []
        (let [patched (await (protocol/patch-thread! provider "normal" {:status "completed" :updated_at 2}))]
          (is (= (+ at 100) (:updated_at patched)))
          (is (= (fixture/iso (+ at 100 (law/ttl-ms "normal"))) (:expiresAt patched)))
          (is (= patched (await (protocol/read-thread provider "normal"))))))))
    (await (fixture/with-clock! (+ at 200)
      (^:async fn []
        (let [rewound (await (protocol/rewind-thread! provider "normal" 1))]
          (is (= [] (:messages rewound)))
          (is (= (+ at 200) (:updated_at rewound)))
          (is (= rewound (await (protocol/read-thread provider "normal"))))))))))

(deftest ^:async legacy-cache-values-strip-driver-metadata-and-convert-application-dates
  (await (fixture/with-clock! at
    (^:async fn []
      (let [{:keys [db]} (fixture/database
                          {"knoxx_thread_titles"
                           [{:session_id "title" :title "Portable title" :title_model "model"
                             :updated_at (fixture/date at) :created_at (fixture/date at)
                             :system_instance_id "old-instance" :expiresAt (fixture/date (+ at 1000))
                             :_id (fixture/native-id)}]
                           "knoxx_memory_threads"
                           [{:cache_key "memory" :value {:sessions [{:id "one"}]}
                             :cached-at (fixture/date at) :expires-at (fixture/date (+ at 1000))
                             :created_at (fixture/date at) :expiresAt (fixture/date (+ at 1000))
                             :_id (fixture/native-id)}]
                           "knoxx_temp_memory"
                           [{:key "temporary" :value {:text "portable"} :expiresAt (fixture/date (+ at 1000))}]})
            title (await (cache/read! db :titles "title"))
            memory (await (cache/read! db :memory-sessions "memory"))]
        (is (= {:title "Portable title" :title_model "model" :updated_at (fixture/iso at)} title))
        (is (= {:value {:sessions [{:id "one"}]} :cached-at at :expires-at (+ at 1000)} memory))
        (is (law/edn-value? title))
        (is (law/edn-value? memory))
        (is (= {:text "portable"} (await (cache/read! db :temp-memory "temporary")))))))))

(deftest ^:async cache-decoding-preserves-existing-portable-values-and-finite-wrappers
  (await (fixture/with-clock! at
    (^:async fn []
      (let [portable-title {:title "Already portable" :title_model "model" :updated_at (fixture/iso at)}
            portable-memory {:value {:sessions []} :cached-at at :expires-at (+ at 1000)}
            wrapped {:title "Finite value" :extra {:kept true}}
            {:keys [db]} (fixture/database
                           {"knoxx_thread_titles"
                            [(assoc portable-title :session_id "portable" :expiresAt (fixture/date (+ at 1000)))
                             {:session_id "wrapped" :value wrapped :cacheFormat "finite-v1"
                              :created_at (fixture/date at) :expiresAt (fixture/date (+ at 1000))}]
                            "knoxx_memory_threads"
                            [(assoc portable-memory :cache_key "portable" :expiresAt (fixture/date (+ at 1000)))]})]
        (is (= portable-title (await (cache/read! db :titles "portable"))))
        (is (= portable-memory (await (cache/read! db :memory-sessions "portable"))))
        (is (= wrapped (await (cache/read! db :titles "wrapped")))))))))

(defrecord FixtureThreadStore [state]
  protocol/IThreadStore
  (read-thread [_ id] (get @state id))
  (conversation-thread [_ _] nil)
  (put-thread! [_ value] (swap! state assoc (:session_id value) value) value)
  (patch-thread! [_ id patch] (get (swap! state update id merge patch) id))
  (rewind-thread! [_ _ _] nil)
  (delete-thread! [_ id] (swap! state dissoc id) true)
  (active-threads [_] (vec (vals @state))))

(deftest ^:async session-cache-is-bounded-sweeps-expiry-and-retains-provider-isolation
  (let [previous @sessions/provider*
        provider (->FixtureThreadStore (atom {}))
        now (atom at)]
    (try
      (sessions/install! provider)
      (with-redefs [clock/now-ms (fn [] @now)]
        (doseq [i (range 1000)]
          (swap! now inc)
          (await (sessions/put-session! {:session_id (str "cache-" i) :messages [{:role "user" :content "Transcript"}]
                                         :expiresAt (+ at 100000)})))
        (swap! now inc)
        (await (sessions/get-session "cache-0"))
        (swap! now inc)
        (await (sessions/put-session! {:session_id "cache-1000" :expiresAt (+ at 100000)}))
        (is (= 1000 (count @sessions/session-cache*)))
        (is (some? (sessions/get-session-sync "cache-0")) "Refreshing an entry retains it over the oldest untouched one")
        (is (nil? (sessions/get-session-sync "cache-1")))
        (let [before @sessions/session-cache*
              {:keys [db]} (fixture/database {})]
          (await (sessions/put-session! db {:session_id "foreign"}))
          (is (= before @sessions/session-cache*) "An explicit foreign database cannot populate or prune the selected cache"))
        (reset! now (+ at 100000))
        (await (sessions/put-session! {:session_id "fresh" :expiresAt (+ at 200000)}))
        (is (= 1 (count @sessions/session-cache*)) "Remembering one entry removes all expired transcripts")
        (is (contains? @sessions/session-cache* "fresh"))
        (reset! now (+ at 200000))
        (is (nil? (sessions/get-session-sync "fresh")))
        (is (zero? (count @sessions/session-cache*)) "Expired synchronous hits are removed, not merely hidden"))
      (finally (sessions/install! previous)))))

(deftest ^:async cached-mongo-thread-cannot-outlive-its-durable-expiry
  (let [previous @sessions/provider*
        {:keys [db]} (fixture/database {native/COLLECTION_NAME (rows)})
        provider (mongo/create-store db)]
    (try
      (sessions/install! provider)
      (await (fixture/with-clock! at
        (^:async fn []
          (is (some? (await (sessions/get-session "live"))))
          (is (some? (sessions/get-session-sync "live"))))))
      (await (fixture/with-clock! (+ at 50)
        (fn []
          (is (nil? (sessions/get-session-sync "live"))))))
      (finally (sessions/install! previous)))))

(deftest ^:async concurrent-thread-patches-retain-independent-fields
  (let [{:keys [db]} (fixture/database {})
        provider (mongo/create-store db)
        messages [{:role "user" :content "New transcript"}]]
    (await (protocol/put-thread! provider {:session_id "patch-race" :status "running"
                                         :messages [] :has_active_stream false :run_id "old"}))
    (let [results (await (concurrent/settled
                         [(protocol/patch-thread! provider "patch-race" {:messages messages :run_id "new"})
                          (protocol/patch-thread! provider "patch-race" {:has_active_stream true})]))
          stored (await (protocol/read-thread provider "patch-race"))]
      (is (every? #(= :fulfilled (:status %)) results))
      (is (= messages (:messages stored)))
      (is (= "new" (:run_id stored)))
      (is (true? (:has_active_stream stored))))))

(deftest ^:async concurrent-rewinds-remove-distinct-latest-turns
  (let [{:keys [db]} (fixture/database {})
        provider (mongo/create-store db)]
    (await (protocol/put-thread! provider {:session_id "rewind-race" :status "completed"
                                         :messages [{:role "system" :content "Keep"}
                                                    {:role "user" :content "First"}
                                                    {:role "assistant" :content "First reply"}
                                                    {:role "user" :content "Second"}]}))
    (let [results (await (concurrent/settled
                         [(protocol/rewind-thread! provider "rewind-race" 1)
                          (protocol/rewind-thread! provider "rewind-race" 1)]))]
      (is (every? #(= :fulfilled (:status %)) results))
      (is (= [{:role "system" :content "Keep"}]
             (:messages (await (protocol/read-thread provider "rewind-race"))))))))

(deftest ^:async invalid-legacy-thread-refuses-mutation-before-native-write
  (await (fixture/with-clock! at
    (^:async fn []
      (doseq [operation [:patch :rewind]]
        (let [messages [{:role "user" :content "Keep"}]
              {:keys [db]} (fixture/database
                             {native/COLLECTION_NAME
                              [{:session_id "opaque" :status "completed" :messages messages
                                :legacy_value (fixture/date at) :expiresAt (fixture/date (+ at 1000))}]})
              provider (mongo/create-store db)]
          (try
            (await (if (= operation :patch)
                     (protocol/patch-thread! provider "opaque" {:status "running"})
                     (protocol/rewind-thread! provider "opaque" 1)))
            (is false "Opaque legacy state must refuse before mutation")
            (catch :default error (is (= "thread_store_invalid" (:code (ex-data error))))))
          (let [stored (await (protocol/read-thread provider "opaque"))]
            (is (= "completed" (:status stored)))
            (is (= messages (:messages stored))))))))))

(deftest ^:async mongo-thread-identity-cannot-be-rebound
  (let [{:keys [db]} (fixture/database {})]
    (await (identity-proof/check-rebinding! (mongo/create-store db)))))

(deftest ^:async mongo-thread-initial-identity-assignment-is-atomic
  (let [{:keys [db]} (fixture/database {})]
    (await (identity-proof/check-initial-assignment! (mongo/create-store db)))))

(deftest ^:async mongo-thread-rejects-malformed-and-aliased-identities
  (let [{:keys [db]} (fixture/database {})]
    (await (identity-proof/check-invalid-identity! (mongo/create-store db)))))

(deftest ^:async expired-thread-readmission-does-not-adopt-private-history
  (let [{:keys [db]} (fixture/database {})
        provider (mongo/create-store db)]
    (await (fixture/with-clock! at
      (^:async fn []
        (await (protocol/put-thread! provider
                 {:session_id "expired-owner" :conversation_id "expired-conversation" :org_id "org-old"
                  :user_id "user-old" :messages [{:role "user" :content "Private old tenant"}]})))))
    (await (fixture/with-clock! (+ at (law/ttl-ms "expired-owner"))
      (^:async fn []
        (is (nil? (await (protocol/read-thread provider "expired-owner"))))
        (let [written (await (protocol/patch-thread! provider "expired-owner" {:org_id "org-new" :user_id "user-new"}))]
          (is (= "org-new" (:org_id written)))
          (is (= "user-new" (:user_id written)))
          (is (not (contains? written :messages)))
          (is (not (contains? written :conversation_id)))))))))

(deftest ^:async mongo-thread-compatible-owners-and-conversation-uniqueness
  (let [{:keys [db]} (fixture/database {})]
    (await (identity-proof/check-compatible-and-unique! (mongo/create-store db)))))

(deftest ^:async compatible-conversation-index-race-retries-instead-of-refusing
  (let [{:keys [db]} (fixture/database {})
        provider (mongo/create-store db)
        original native/upsert-session!
        calls (atom 0)
        thread {:session_id "same-admission" :conversation_id "same-conversation" :org_id "same-org"}]
    (with-redefs [native/upsert-session!
                  (^:async fn [handle fields observed]
                    (let [written (await (original handle fields observed))]
                      ;; A competing compatible writer has just won insertion;
                      ;; this admission receives the driver's other unique index.
                      (if (= 1 (swap! calls inc))
                        (throw (fixture/duplicate-error :conversation_id)) written)))]
      (try
        (is (= thread (select-keys (await (protocol/put-thread! provider thread)) (keys thread))))
        (catch :default _ (is false "A compatible conversation owner must retry"))))
    (is (= 2 @calls))
    (is (= thread (select-keys (await (protocol/read-thread provider "same-admission")) (keys thread))))))

(deftest ^:async thread-mutations-refuse-a-recreated-owner-after-observation
  (let [{:keys [db]} (fixture/database {})]
    (await (identity-proof/check-recreated-owner! (mongo/create-store db) db))))
