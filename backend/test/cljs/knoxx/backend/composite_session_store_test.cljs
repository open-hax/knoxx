(ns knoxx.backend.composite-session-store-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.shape.session-persistence :refer [ISessionStore get-run put-run! complete-run! delete-run! patch-run!]]
            [knoxx.backend.infra.stores.composite-session-store :refer [->CompositeSessionStore]]))

;; ─── In-memory ISessionStore for testing ────────────────────────────────────

(defrecord MemorySessionStore [runs*]
  ISessionStore

  (put-run! [_ run]
    (swap! runs* assoc (:run_id run) run)
    (js/Promise.resolve run))

  (get-run [_ run-id]
    (js/Promise.resolve (get @runs* run-id)))

  (patch-run! [store run-id patch]
    (-> (get-run store run-id)
        (.then (fn [current]
                 (let [updated (merge (or current {:run_id run-id}) patch)]
                   (put-run! store updated))))))

  (list-active-runs [_ session-id]
    (js/Promise.resolve
     (->> (vals @runs*)
          (filter #(and (= (:session_id %) session-id)
                        (contains? #{"running" "queued" "waiting_input"} (:status %))))
          vec)))

  (complete-run! [store run-id opts]
    (patch-run! store run-id
                (merge {:status "completed" :has_active_stream false}
                       (select-keys opts [:status :answer :error :trace_blocks :messages]))))

  (delete-run! [_ run-id]
    (swap! runs* dissoc run-id)
    (js/Promise.resolve true)))

(defn- memory-store [] (->MemorySessionStore (atom {})))

(defn- base-run []
  {:run_id "run-1"
   :session_id "sess-1"
   :conversation_id "conv-1"
   :status "running"
   :created_at "2026-01-01T00:00:00.000Z"
   :updated_at "2026-01-01T00:00:00.000Z"})

;; ─── put-run! ───────────────────────────────────────────────────────────────

(deftest ^:async put-run-live-writes-to-primary-only
  ;; Live runs (running/queued/waiting_input) go to Redis only — OpenPlanner is
  ;; not the authority for in-flight state.
  (let [a (memory-store)
        b (memory-store)
        composite (->CompositeSessionStore a b)
        run (base-run)              ; status "running"
        result (await (put-run! composite run))]
    (testing "returns the run"
      (is (= "run-1" (:run_id result))))
    (testing "written to primary store"
      (is (= run (get @(:runs* a) "run-1"))))
    (testing "not written to secondary store while live"
      (is (nil? (get @(:runs* b) "run-1"))))))

(deftest ^:async put-run-terminal-writes-to-both-stores
  ;; Terminal runs (completed/failed/cancelled) are archived to both stores.
  (let [a (memory-store)
        b (memory-store)
        composite (->CompositeSessionStore a b)
        run (assoc (base-run) :status "completed" :answer "ok")
        result (await (put-run! composite run))]
    (testing "returns the run"
      (is (= "run-1" (:run_id result))))
    (testing "written to primary store"
      (is (= "completed" (:status (get @(:runs* a) "run-1")))))
    (testing "written to secondary store"
      (is (= "completed" (:status (get @(:runs* b) "run-1")))))))

;; ─── get-run ────────────────────────────────────────────────────────────────

(deftest ^:async get-run-returns-primary-hit
  (let [a (memory-store)
        b (memory-store)
        composite (->CompositeSessionStore a b)
        run (base-run)]
    (swap! (:runs* a) assoc "run-1" run)
    (testing "returns run from primary when present"
      (is (= "run-1" (:run_id (await (get-run composite "run-1"))))))))

(deftest ^:async get-run-falls-back-to-secondary-when-primary-misses
  (let [a (memory-store)
        b (memory-store)
        composite (->CompositeSessionStore a b)
        run (base-run)]
    (swap! (:runs* b) assoc "run-1" run)
    (testing "falls back to secondary when primary returns nil"
      (is (= "run-1" (:run_id (await (get-run composite "run-1"))))))))

(deftest ^:async get-run-returns-nil-when-both-miss
  (let [composite (->CompositeSessionStore (memory-store) (memory-store))]
    (testing "returns nil when neither store has the run"
      (is (nil? (await (get-run composite "no-such-run")))))))

;; ─── complete-run! ──────────────────────────────────────────────────────────

(deftest ^:async complete-run-writes-completion-to-both-stores
  (let [a (memory-store)
        b (memory-store)
        composite (->CompositeSessionStore a b)
        run (base-run)]
    (swap! (:runs* a) assoc "run-1" run)
    (swap! (:runs* b) assoc "run-1" run)
    (await (complete-run! composite "run-1" {:status "completed" :answer "42"}))
    (testing "primary store reflects completed status"
      (is (= "completed" (:status (get @(:runs* a) "run-1")))))
    (testing "secondary store reflects completed status"
      (is (= "completed" (:status (get @(:runs* b) "run-1")))))
    (testing "answer is persisted in both stores"
      (is (= "42" (:answer (get @(:runs* a) "run-1"))))
      (is (= "42" (:answer (get @(:runs* b) "run-1")))))))

(deftest ^:async complete-run-archives-primary-result-to-secondary
  ;; complete-run! finalises the run in the primary store, then puts the
  ;; primary result into the secondary — secondary's prior state is irrelevant.
  (let [a (memory-store)
        b (memory-store)
        composite (->CompositeSessionStore a b)
        run (base-run)]
    (swap! (:runs* a) assoc "run-1" run)
    (await (complete-run! composite "run-1" {:status "completed" :answer "yes"}))
    (testing "secondary receives the finalised run from primary"
      (is (= "completed" (:status (get @(:runs* b) "run-1"))))
      (is (= "yes" (:answer (get @(:runs* b) "run-1")))))))

;; ─── delete-run! ────────────────────────────────────────────────────────────

(deftest ^:async delete-run-removes-from-both-stores
  (let [a (memory-store)
        b (memory-store)
        composite (->CompositeSessionStore a b)
        run (base-run)]
    (swap! (:runs* a) assoc "run-1" run)
    (swap! (:runs* b) assoc "run-1" run)
    (await (delete-run! composite "run-1"))
    (testing "removed from primary store"
      (is (nil? (get @(:runs* a) "run-1"))))
    (testing "removed from secondary store"
      (is (nil? (get @(:runs* b) "run-1"))))))
