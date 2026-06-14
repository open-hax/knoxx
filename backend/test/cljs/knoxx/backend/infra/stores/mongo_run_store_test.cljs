(ns knoxx.backend.infra.stores.mongo-run-store-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-run-store :as run-store]
            [knoxx.backend.shape.session-persistence :as sp]))

(defn- valid-run [run-id session-id]
  {:run_id run-id
   :session_id session-id
   :conversation_id "conv-1"
   :status "running"
   :created_at "2026-06-05T00:00:00Z"
   :updated_at "2026-06-05T00:00:00Z"
   :model "gpt-4"
   :messages [{:role "user" :content "hello"}]})

(defn- mock-findOne [runs query]
  (let [run-id (aget query "run_id")
        session-id (aget query "session_id")
        status-in (when (.-status query) (.-$in (.-status query)))]
    (js/Promise.resolve
      (clj->js
        (cond
          run-id (first (filter #(= (:run_id %) run-id) (vals @runs)))
          session-id (first (filter #(= (:session_id %) session-id) (vals @runs)))
          status-in (first (filter #(contains? (set (js->clj status-in :keywordize-keys true)) (:status %)) (vals @runs)))
          :else nil)))))

(defn- mock-insertOne [runs doc]
  (let [run (js->clj doc :keywordize-keys true)]
    (swap! runs assoc (:run_id run) run)
    (js/Promise.resolve #js {})))

(defn- apply-push-update [existing push-obj]
  (if push-obj
    (let [run-events (get push-obj "run_events")
          each (get run-events "$each")
          new-event (js->clj (first each) :keywordize-keys true)
          slice (get run-events "$slice")]
      (vec (take (- slice) (conj (:run_events existing []) new-event))))
    (:run_events existing [])))

(defn- mock-findOneAndUpdate [runs query update _opts]
  (let [run-id (aget query "run_id")
        existing (get @runs run-id)
        set-obj (js->clj (.-$set update) :keywordize-keys true)
        push-obj (when (.-$push update) (js->clj (.-$push update)))
        events (apply-push-update existing push-obj)
        updated (merge existing set-obj {:run_events events})]
    (swap! runs assoc run-id updated)
    (js/Promise.resolve (clj->js updated))))

(defn- mock-deleteOne [runs query]
  (let [run-id (aget query "run_id")]
    (swap! runs dissoc run-id)
    (js/Promise.resolve #js {:deletedCount 1})))

(defn- mock-find [runs query]
  (let [session-id (aget query "session_id")
        statuses (when (.-status query) (js->clj (.-$in (.-status query)) :keywordize-keys true))
        filtered (filter #(and (= (:session_id %) session-id)
                               (contains? (set statuses) (:status %)))
                         (vals @runs))]
    #js {:toArray (fn [] (js/Promise.resolve (clj->js filtered)))}))

(defn- mock-collection [runs]
  #js {:findOne (partial mock-findOne runs)
       :insertOne (partial mock-insertOne runs)
       :findOneAndUpdate (partial mock-findOneAndUpdate runs)
       :deleteOne (partial mock-deleteOne runs)
       :find (partial mock-find runs)
       :createIndex (fn [_keys _opts] (js/Promise.resolve "ok"))})

(defn- mock-db []
  (let [runs (atom {})]
    #js {:collection (fn [_name] (mock-collection runs))}))

(deftest ^:async put-run-test
  (testing "Persists a valid run"
    (let [db (mock-db)
          store (run-store/create-mongo-run-store db)
          run (valid-run "r1" "s1")
          result (await (sp/put-run! store run))]
      (is (= "r1" (:run_id result)))
      (is (= "running" (:status result))))))

(deftest ^:async get-run-test
  (testing "Fetches a run by id"
    (let [db (mock-db)
          store (run-store/create-mongo-run-store db)
          run (valid-run "r2" "s1")]
      (await (sp/put-run! store run))
      (let [result (await (sp/get-run store "r2"))]
        (is (= "r2" (:run_id result)))))))

(deftest ^:async patch-run-test
  (testing "Patches an existing run"
    (let [db (mock-db)
          store (run-store/create-mongo-run-store db)
          run (valid-run "r3" "s1")]
      (await (sp/put-run! store run))
      (let [result (await (sp/patch-run! store "r3" {:status "completed"}))]
        (is (= "completed" (:status result)))))))

(deftest ^:async list-active-runs-test
  (testing "Lists active runs for a session"
    (let [db (mock-db)
          store (run-store/create-mongo-run-store db)]
      (await (sp/put-run! store (valid-run "r4" "s2")))
      (await (sp/put-run! store (assoc (valid-run "r5" "s2") :status "completed")))
      (let [result (await (sp/list-active-runs store "s2"))]
        (is (= 1 (count result)))
        (is (= "r4" (:run_id (first result))))))))

(deftest ^:async complete-run-test
  (testing "Completes a run"
    (let [db (mock-db)
          store (run-store/create-mongo-run-store db)
          run (valid-run "r6" "s1")]
      (await (sp/put-run! store run))
      (let [result (await (sp/complete-run! store "r6" {:answer "done"}))]
        (is (= "completed" (:status result)))
        (is (= "done" (:answer result)))))))

(deftest ^:async delete-run-test
  (testing "Deletes a run"
    (let [db (mock-db)
          store (run-store/create-mongo-run-store db)
          run (valid-run "r7" "s1")]
      (await (sp/put-run! store run))
      (let [result (await (sp/delete-run! store "r7"))]
        (is (= true result))
        (is (nil? (await (sp/get-run store "r7"))))))))

(deftest ^:async append-run-event-test
  (testing "Appends event to run_events array"
    (let [db (mock-db)
          store (run-store/create-mongo-run-store db)
          run (valid-run "r8" "s1")]
      (await (sp/put-run! store run))
      (let [raw (await (run-store/append-run-event! db "r8" {:kind :text :status "done"}))
            result (js->clj raw :keywordize-keys true)]
        (is (= 1 (count (:run_events result))))
        (is (= "text" (:kind (first (:run_events result)))))))))
