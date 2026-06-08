(ns knoxx.backend.infra.stores.mongo-policy-store-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-policy-store :as store]))

(defn- matches-query? [doc query]
  (every? (fn [[k v]]
            (let [actual (get doc k)]
              (if (map? v)
                (cond
                  (contains? v :$gt) (and actual (> (.getTime actual) (.getTime (:$gt v))))
                  (contains? v :$lt) (and actual (< (.getTime actual) (.getTime (:$lt v))))
                  :else (= actual v))
                (= actual v))))
          query))

(defn- mock-update-one [docs]
  (fn [query update opts]
    (let [q (js->clj query :keywordize-keys true)
          set-doc (js->clj (.-$set update) :keywordize-keys true)
          existing (first (filter #(matches-query? % q) @docs))]
      (cond
        existing
        (swap! docs (fn [ds] (mapv #(if (matches-query? % q) (merge % set-doc) %) ds)))

        (and opts (.-upsert opts))
        (swap! docs conj (merge q set-doc)))
      (js/Promise.resolve #js {}))))

(defn- mock-delete-many [docs]
  (fn [query]
    (let [q (js->clj query :keywordize-keys true)
          before (count @docs)]
      (swap! docs (fn [ds] (vec (remove #(matches-query? % q) ds))))
      (js/Promise.resolve #js {:deletedCount (- before (count @docs))}))))

(defn- mock-collection [docs]
  #js {:insertOne (fn [doc]
                    (swap! docs conj (js->clj doc :keywordize-keys true))
                    (js/Promise.resolve #js {}))
       :findOne (fn [query]
                  (let [q (js->clj query :keywordize-keys true)]
                    (js/Promise.resolve
                     (clj->js (first (filter #(matches-query? % q) @docs))))))
       :find (fn [query]
               (let [q (js->clj query :keywordize-keys true)
                     hits (filter #(matches-query? % q) @docs)]
                 #js {:toArray (fn [] (js/Promise.resolve (clj->js hits)))}))
       :updateOne (mock-update-one docs)
       :findOneAndUpdate (fn [query update opts]
                           (let [q (js->clj query :keywordize-keys true)
                                 soi (js->clj (.-$setOnInsert update) :keywordize-keys true)
                                 existing (first (filter #(matches-query? % q) @docs))]
                             (cond
                               existing (js/Promise.resolve (clj->js existing))
                               (and opts (.-upsert opts))
                               (let [doc (merge q soi)]
                                 (swap! docs conj doc)
                                 (js/Promise.resolve (clj->js doc)))
                               :else (js/Promise.resolve nil))))
       :deleteOne (fn [query]
                    (let [q (js->clj query :keywordize-keys true)]
                      (swap! docs (fn [ds] (vec (remove #(matches-query? % q) ds))))
                      (js/Promise.resolve #js {:deletedCount 1})))
       :deleteMany (mock-delete-many docs)
       :createIndex (fn [_keys _opts] (js/Promise.resolve "ok"))})

(defn- mock-db []
  (let [collections (atom {})]
    #js {:collection (fn [name]
                       (let [docs (or (get @collections name)
                                      (let [d (atom [])]
                                        (swap! collections assoc name d)
                                        d))]
                         (mock-collection docs)))}))

(deftest ^:async create-and-get-session-test
  (testing "round-trips a session by bearer token with hash verification"
    (let [db (mock-db)
          _ (await (store/create-session! db {:token "tok-abc" :user-id "u1"
                                              :org-id "o1" :email "a@b.c"
                                              :display-name "A"}))
          result (await (store/get-session-by-token! db "tok-abc"))]
      (is (some? (:session result)))
      (is (= "u1" (get-in result [:session :user-id])))
      (is (= "a@b.c" (get-in result [:session :email])))
      (is (some? (get-in result [:session :id]))))))

(deftest ^:async wrong-token-test
  (testing "a non-matching token resolves no session"
    (let [db (mock-db)]
      (await (store/create-session! db {:token "tok-real" :user-id "u1"}))
      (is (nil? (await (store/get-session-by-token! db "tok-fake")))))))

(deftest ^:async blank-token-create-test
  (testing "blank token is rejected"
    (let [db (mock-db)]
      (is (thrown? js/Error (await (store/create-session! db {:token "" :user-id "u1"})))))))

(deftest ^:async delete-session-test
  (testing "delete-by-token removes the session"
    (let [db (mock-db)]
      (await (store/create-session! db {:token "tok-del" :user-id "u1"}))
      (let [deleted (await (store/delete-session-by-token! db "tok-del"))]
        (is (some? (:session deleted))))
      (is (nil? (await (store/get-session-by-token! db "tok-del")))))))

(deftest ^:async cleanup-expired-test
  (testing "cleanup removes only expired sessions"
    (let [db (mock-db)
          coll (.collection db store/SESSIONS_COLLECTION)]
      (await (store/create-session! db {:token "tok-live" :user-id "u1"}))
      (await (.insertOne coll (clj->js {:session_id "expired-1"
                                        :token_prefix "deadbeef0000"
                                        :expires_at (js/Date. 0)})))
      (let [n (await (store/cleanup-expired-sessions! db))]
        (is (= 1 n)))
      (is (some? (await (store/get-session-by-token! db "tok-live")))))))

(deftest ^:async session-secret-test
  (testing "generates once, then recovers the same secret"
    (let [db (mock-db)
          first-secret (await (store/recover-session-secret! db nil))
          second-secret (await (store/recover-session-secret! db nil))]
      (is (string? first-secret))
      (is (= 64 (count first-secret)))
      (is (= first-secret second-secret))))
  (testing "adopts a fallback secret (PG cutover) instead of generating"
    (let [db (mock-db)
          secret (await (store/recover-session-secret! db "pg-secret-value"))]
      (is (= "pg-secret-value" secret))
      (is (= "pg-secret-value" (await (store/recover-session-secret! db nil)))))))

(deftest ^:async init-config-first-writer-wins-test
  (testing "init-config-value! never clobbers an existing value"
    (let [db (mock-db)]
      (is (= "first" (await (store/init-config-value! db "race-key" "first"))))
      (is (= "first" (await (store/init-config-value! db "race-key" "second"))))
      (is (= "first" (await (store/get-config-value! db "race-key")))))))

(deftest ^:async config-value-test
  (testing "config values upsert and read back"
    (let [db (mock-db)]
      (is (nil? (await (store/get-config-value! db "missing"))))
      (await (store/set-config-value! db "k1" "v1"))
      (is (= "v1" (await (store/get-config-value! db "k1"))))
      (await (store/set-config-value! db "k1" "v2"))
      (is (= "v2" (await (store/get-config-value! db "k1")))))))
