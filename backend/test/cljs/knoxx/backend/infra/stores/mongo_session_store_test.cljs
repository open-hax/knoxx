(ns knoxx.backend.infra.stores.mongo-session-store-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-session-store :as store]
            [knoxx.backend.infra.system-instance :as system-instance]))

(defn- mock-collection [sessions]
  #js {:findOne (fn [query]
                  (let [id (or (aget query "session_id")
                               (aget query "conversation_id"))]
                    (js/Promise.resolve
                      (clj->js (first (filter #(or (= (:session_id %) id)
                                                 (= (:conversation_id %) id))
                                              (vals @sessions)))))))
       :insertOne (fn [doc]
                    (let [session (js->clj doc :keywordize-keys true)]
                      (swap! sessions assoc (:session_id session) session)
                      (js/Promise.resolve #js {})))
       :findOneAndUpdate (fn [query update _opts]
                           (let [id (aget query "session_id")
                                  set-obj (js->clj (.-$set update) :keywordize-keys true)
                                 existing (get @sessions id)
                                 updated (merge existing set-obj)]
                             (swap! sessions assoc id updated)
                             (js/Promise.resolve (clj->js updated))))
       :deleteOne (fn [query]
                    (let [id (aget query "session_id")]
                      (swap! sessions dissoc id)
                      (js/Promise.resolve #js {:deletedCount 1})))
       :find (fn [query]
               (let [statuses (js->clj (.-$in (.-status query)) :keywordize-keys true)
                     filtered (filter #(contains? (set statuses) (:status %)) (vals @sessions))]
                 #js {:toArray (fn [] (js/Promise.resolve (clj->js filtered)))}))
       :createIndex (fn [_keys _opts] (js/Promise.resolve "ok"))})

(defn- mock-db []
  (let [sessions (atom {})]
    #js {:collection (fn [_name] (mock-collection sessions))}))

(deftest ^:async get-session-test
  (testing "Gets session from MongoDB"
    (let [db (mock-db)
          session {:session_id "s1" :status "running" :conversation_id "c1"}]
      (await (store/put-session! db session))
      (let [result (await (store/get-session db "s1"))]
        (is (= "s1" (:session_id result)))
        (is (= "running" (:status result)))))))

(deftest ^:async update-session-test
  (testing "Updates session state"
    (let [db (mock-db)
          session {:session_id "s3" :status "running"}]
      (await (store/put-session! db session))
      (let [result (await (store/update-session! db "s3" {:status "completed"}))]
        (is (= "completed" (:status result)))))))

(deftest ^:async put-session-stamps-system-instance-test
  (testing "put-session! stamps the current system instance id"
    (let [db (mock-db)
          session {:session_id "s-instance" :status "running"}]
      (await (store/put-session! db session))
      (let [result (await (store/get-session db "s-instance"))]
        (is (= (system-instance/current-id) (:system_instance_id result)))
        (is (system-instance/owned-by-current-instance? result))))))

(deftest ^:async remove-session-test
  (testing "Removes session from cache and MongoDB"
    (let [db (mock-db)
          session {:session_id "s4" :status "running"}]
      (await (store/put-session! db session))
      (let [result (await (store/remove-session! db "s4" nil))]
        (is (= true result))
        (is (nil? (await (store/get-session db "s4"))))
        ))))

(deftest ^:async recover-sessions-test
  (testing "Recovers active sessions"
    (let [db (mock-db)]
      (await (store/put-session! db {:session_id "s5" :status "running"}))
      (await (store/put-session! db {:session_id "s6" :status "completed"}))
      (let [result (await (store/recover-sessions! db))]
        (is (= 1 (count result)))
        (is (= "s5" (:session_id (first result))))))))

(deftest session-can-send-test
  (testing "Session can-send logic"
    (is (:can-send (store/session-can-send? nil)))
    (is (:can-send (store/session-can-send? {:status "waiting_input"})))
    (is (not (:can-send (store/session-can-send? {:status "running"}))))
    (is (:can-send (store/session-can-send? {:status "completed"})))
    (is (:can-send (store/session-can-send? {:status "failed"})))))
