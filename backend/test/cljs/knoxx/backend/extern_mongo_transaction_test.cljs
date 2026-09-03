(ns knoxx.backend.extern-mongo-transaction-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.mongo :as extern-mongo]))

(deftest duplicate-key-classification-stays-inside-the-mongo-boundary
  (let [numeric (doto (js/Error. "write collision")
                  (aset "code" 11000))
        string-code (doto (js/Error. "write collision")
                      (aset "code" "11000"))
        message-only (js/Error. "E11000 duplicate key error")
        wrapped (ex-info "duplicate key" {:code 11000})]
    (is (true? (extern-mongo/duplicate-key-error? numeric)))
    (is (true? (extern-mongo/duplicate-key-error? string-code)))
    (is (true? (extern-mongo/duplicate-key-error? message-only)))
    (is (true? (extern-mongo/duplicate-key-error? wrapped)))
    (is (false? (extern-mongo/duplicate-key-error?
                 (js/Error. "connection reset"))))))

(deftest ^:async transaction-topology-boundary-test
  (let [commands* (atom [])
        replica-db
        #js {:command (fn [command]
                        (swap! commands* conj
                               (js->clj command :keywordize-keys true))
                        (js/Promise.resolve #js {:setName "rs0"}))}
        sharded-db
        #js {:command (fn [_]
                        (js/Promise.resolve #js {:msg "isdbgrid"}))}
        standalone-db
        #js {:command (fn [_]
                        (js/Promise.resolve #js {:isWritablePrimary true}))}]
    (is (= {:kind :replica-set :set-name "rs0"}
           (await (extern-mongo/require-transaction-capable-topology!
                   replica-db))))
    (is (= [{:hello 1}] @commands*))
    (is (= {:kind :sharded-cluster}
           (await (extern-mongo/require-transaction-capable-topology!
                   sharded-db))))
    (try
      (await (extern-mongo/require-transaction-capable-topology!
              standalone-db))
      (is false "a standalone Mongo deployment must be rejected")
      (catch :default err
        (is (= :standalone (:mongo/topology (ex-data err))))
        (is (false? (:mongo/transaction-capable (ex-data err))))))))

(deftest ^:async transaction-boundary-hides-native-session-test
  (let [events* (atom [])
        session
        #js {:withTransaction
             (fn [callback options]
               (swap! events* conj
                      [:transaction-options
                       (.-level (.-readConcern options))
                       (.-w (.-writeConcern options))])
               (callback))
             :endSession
             (fn []
               (swap! events* conj [:end-session])
               (js/Promise.resolve nil))}
        collection
        #js {:updateOne
             (fn [query update options]
               (swap! events* conj
                      [:update-one
                       (js->clj query :keywordize-keys true)
                       (js->clj update :keywordize-keys true)
                       {:upsert (.-upsert options)
                        :write-concern (.-w (.-writeConcern options))
                        :collation-locale (.-locale (.-collation options))
                        :collation-strength (.-strength (.-collation options))
                        :session? (= session (.-session options))}])
               (js/Promise.resolve
                #js {:matchedCount 1 :modifiedCount 1 :upsertedCount 0}))
             :updateMany
             (fn [_query _update options]
               (swap! events* conj
                      [:update-many :session? (= session (.-session options))])
               (js/Promise.resolve
                #js {:matchedCount 2 :modifiedCount 2 :upsertedCount 0}))}
        client #js {:startSession (fn [] session)}
        result
        (await
         (extern-mongo/with-transaction!
          client
          (^:async fn [{:keys [update-one! update-many!]}]
            {:one (await (update-one!
                          collection
                          {:credential_id "credential"}
                          {:$set {:status "active"}}
                          {:upsert true
                           :write-concern "majority"
                           :case-insensitive? true}))
             :many (await (update-many!
                           collection
                           {:org_id "org"}
                           {:$set {:status "inactive"}}
                           {}))})))]
    (is (= {:one {:matched-count 1 :modified-count 1 :upserted-count 0}
            :many {:matched-count 2 :modified-count 2 :upserted-count 0}}
           result))
    (is (= [:transaction-options "snapshot" "majority"] (first @events*)))
    (is (= [:update-one
            {:credential_id "credential"}
            {:$set {:status "active"}}
            {:upsert true
             :write-concern "majority"
             :collation-locale "en"
             :collation-strength 2
             :session? true}]
           (second @events*)))
    (is (= [:update-many :session? true] (nth @events* 2)))
    (is (= [:end-session] (last @events*)))))

(deftest ^:async transaction-boundary-closes-session-after-error-test
  (let [closed?* (atom false)
        session #js {:withTransaction (fn [callback _options] (callback))
                     :endSession (fn []
                                   (reset! closed?* true)
                                   (js/Promise.resolve nil))}
        client #js {:startSession (fn [] session)}]
    (try
      (await
       (extern-mongo/with-transaction!
        client
        (fn [_operations]
          (js/Promise.reject (js/Error. "transaction failed")))))
      (is false "transaction error must propagate")
      (catch js/Error err
        (is (= "transaction failed" (.-message err)))))
    (is @closed?* "session is closed after an aborted transaction")))

(deftest ^:async transaction-boundary-preserves-native-retry-labels-test
  (let [attempts* (atom 0)
        closed?* (atom false)
        transient-error (js/Error. "write conflict")
        _ (aset transient-error "hasErrorLabel"
                (fn [label] (= "TransientTransactionError" label)))
        collection
        #js {:updateOne
             (fn [& _]
               (if (= 1 (swap! attempts* inc))
                 (js/Promise.reject transient-error)
                 (js/Promise.resolve
                  #js {:matchedCount 1 :modifiedCount 1 :upsertedCount 0})))}
        session
        #js {:withTransaction
             (fn [callback _options]
               (letfn [(run! []
                         (.catch
                          (callback)
                          (fn [err]
                            (let [has-error-label (aget err "hasErrorLabel")]
                              (if (and has-error-label
                                       (.call has-error-label err
                                              "TransientTransactionError"))
                                (run!)
                                (js/Promise.reject err))))))]
                 (run!)))
             :endSession
             (fn []
               (reset! closed?* true)
               (js/Promise.resolve nil))}
        client #js {:startSession (fn [] session)}
        result
        (await
         (extern-mongo/with-transaction!
          client
          (^:async fn [{:keys [update-one!]}]
            (await (update-one!
                    collection
                    {:_id "lock"}
                    {:$set {:updated_at 1}}
                    {})))))]
    (is (= 2 @attempts*) "the driver can retry the original labeled error")
    (is (= 1 (:matched-count result)))
    (is @closed?* "the retried transaction still closes its session")))

(deftest ^:async transaction-boundary-translates-native-write-error-after-driver-test
  (let [closed?* (atom false)
        native-error (js/Error. "simulated write failure")
        collection #js {:updateOne (fn [& _]
                                     (js/Promise.reject native-error))}
        session #js {:withTransaction (fn [callback _options] (callback))
                     :endSession (fn []
                                   (reset! closed?* true)
                                   (js/Promise.resolve nil))}
        client #js {:startSession (fn [] session)}]
    (try
      (await
       (extern-mongo/with-transaction!
        client
        (^:async fn [{:keys [update-one!]}]
          (await (update-one!
                  collection
                  {:credential_id "credential"}
                  {:$set {:status "active"}}
                  {})))))
      (is false "the failed native write must reject the transaction")
      (catch :default err
        (is (= "Mongo updateOne failed" (.-message err)))
        (is (identical? native-error (ex-cause err))
            "translation retains the original native Mongo error")))
    (is @closed?* "the failed transaction still closes its session")))

(deftest ^:async mutation-boundary-rejects-unsafe-shapes-test
  (let [calls* (atom 0)
        collection
        #js {:updateOne
             (fn [& _]
               (swap! calls* inc)
               (js/Promise.resolve #js {:matchedCount 0}))}]
    (try
      (await (extern-mongo/update-one!
              collection {} {:$set {:status "inactive"}} {}))
      (is false "an empty mutation query must be rejected")
      (catch js/Error err
        (is (re-find #"unsafe Mongo mutation query" (.-message err)))))
    (try
      (await (extern-mongo/update-one!
              collection {:credential_id "credential"} {} {}))
      (is false "an empty mutation document must be rejected")
      (catch js/Error err
        (is (re-find #"unsafe Mongo mutation document" (.-message err)))))
    (try
      (await (extern-mongo/update-one!
              collection
              {:credential_id "credential"}
              {:$rename {:status :old_status}}
              {}))
      (is false "an unapproved mutation operator must be rejected")
      (catch js/Error err
        (is (re-find #"unsafe Mongo mutation document" (.-message err)))))
    (is (zero? @calls*) "invalid mutations never reach the driver")))

(deftest ^:async mutation-boundary-encodes-declared-bson-dates-test
  (let [calls* (atom [])
        timestamp 1700000000000
        collection
        #js {:updateOne
             (fn [_query update _options]
               (let [native-date (aget (aget update "$set") "updated_at")]
                 (swap! calls* conj
                        {:native-date? (instance? js/Date native-date)
                         :epoch-ms (when (instance? js/Date native-date)
                                     (.getTime native-date))}))
               (js/Promise.resolve
                #js {:matchedCount 1 :modifiedCount 1 :upsertedCount 0}))}]
    (is (= {:matched-count 1 :modified-count 1 :upserted-count 0}
           (await (extern-mongo/update-one!
                   collection
                   {:_id "lock"}
                   {:$set {:updated_at timestamp}}
                   {:bson-date-fields #{:updated_at}}))))
    (is (= [{:native-date? true :epoch-ms timestamp}] @calls*))
    (doseq [[update fields]
            [[{:$set {:updated_at "not-an-instant"}} #{:updated_at}]
             [{:$set {:updated_at timestamp}} #{:created_at}]]]
      (try
        (await (extern-mongo/update-one!
                collection {:_id "lock"} update
                {:bson-date-fields fields}))
        (is false "invalid BSON Date encoding must be rejected")
        (catch js/Error err
          (is (re-find #"unsafe Mongo BSON Date encoding" (.-message err))))))
    (is (= 1 (count @calls*))
        "invalid or misspelled Date fields never reach the driver")))
