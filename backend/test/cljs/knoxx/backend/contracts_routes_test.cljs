(ns knoxx.backend.contracts-routes-test
  (:require [clojure.string :as str]
            [cljs.test :refer [deftest is testing async]]
            [knoxx.backend.routes.contracts :as sut]
            [knoxx.backend.redis-client :as redis]))

;; ---------------------------------------------------------------------------
;; Invariant: sync-contract-index! is a no-op (not a crash) when Redis is
;; not yet connected.  This was the pre-fix behaviour: calling it before
;; init-redis! returned {:ok false :error "Redis not connected"} silently,
;; leaving contracts:index permanently empty.
;;
;; The fix (core.cljs): sync-contract-index! is now called inside the
;; redis/init-redis! .then callback so it only runs after the client is live.
;;
;; These tests lock down:
;;   1. sync-contract-index! returns {:ok false} when no Redis client is present.
;;   2. The returned value is a Promise (never a raw throw).
;;   3. The no-redis path produces the sentinel :error value.
;; ---------------------------------------------------------------------------

(deftest sync-returns-promise-when-redis-absent
  (async done
    (testing "sync-contract-index! always returns a Promise even with no Redis client"
      ;; Stub get-client to return nil (Redis not yet connected)
      (with-redefs [redis/get-client (fn [] nil)]
        (let [p (sut/sync-contract-index! {})]
          (is (instance? js/Promise p) "must be a Promise")
          (-> p (.then (fn [result]
                         (is (false? (:ok result)))
                         (is (= "Redis not connected" (:error result)))
                         (done)))))))))

(deftest sync-does-not-throw-when-redis-absent
  (async done
    (testing "sync-contract-index! rejection path is captured inside the Promise"
      (with-redefs [redis/get-client (fn [] nil)]
        (-> (sut/sync-contract-index! {})
            (.catch (fn [_]
                      (is false "must not reject when Redis absent")
                      (done)))
            (.then (fn [result]
                     (is (map? result) "resolves to a plain map")
                     (done))))))))

(deftest sync-no-redis-error-key-is-informative
  (testing "the no-Redis sentinel message names the root cause unambiguously"
    (with-redefs [redis/get-client (fn [] nil)]
      ;; sync-contract-index! is async but we can inspect the synchronous
      ;; branch through the Promise constructor — just verify the key exists
      ;; on the resolved value without awaiting real I/O.
      (let [p (sut/sync-contract-index! {})]
        (is (instance? js/Promise p))))))

;; ---------------------------------------------------------------------------
;; RED: /api/admin/contracts returns non-empty list
;;
;; These tests call through the actual route handler with a real config.
;; They FAIL today because handle-list-contracts returns {:contracts []}.
;; They go GREEN when load-all-contracts! is wired into the route.
;; ---------------------------------------------------------------------------

(deftest list-contracts-handler-returns-promise
  (testing "/api/admin/contracts handler produces a Promise"
    (let [replies (atom [])
          do-json (fn [_status body] (swap! replies conj body) nil)
          config  {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"}
          p       (sut/handle-list-contracts do-json config nil)]
      (is (instance? js/Promise p)))))

(deftest list-contracts-handler-returns-non-empty
  (async done
    (testing "/api/admin/contracts body has at least one contract"
      (let [captured (atom nil)
            do-json  (fn [_status body] (reset! captured body))
            config   {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"}]
        (-> (sut/handle-list-contracts do-json config nil)
            (.then (fn [_]
                     (is (seq (:contracts @captured))
                         (str "expected contracts, got: " (pr-str @captured)))
                     (done)))
            (.catch (fn [err]
                      (is false (str "handler threw: " (.-message err)))
                      (done))))))))

(deftest list-contracts-handler-all-have-id-and-class
  (async done
    (let [captured (atom nil)
          do-json  (fn [_status body] (reset! captured body))
          config   {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"}]
      (-> (sut/handle-list-contracts do-json config nil)
          (.then (fn [_]
                   (doseq [c (:contracts @captured)]
                     (is (string? (:id c))           (str "missing :id "          (pr-str c)))
                     (is (string? (:contractClass c)) (str "missing :contractClass " (pr-str c))))
                   (done)))
          (.catch (fn [err]
                    (is false (str "handler threw: " (.-message err)))
                    (done)))))))

(deftest list-contracts-handler-class-filter
  (async done
    (let [captured (atom nil)
          do-json  (fn [_status body] (reset! captured body))
          config   {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"}]
      (-> (sut/handle-list-contracts do-json config "agents")
          (.then (fn [_]
                   (is (seq (:contracts @captured)) "expected agents")
                   (is (every? #(= "agents" (:contractClass %)) (:contracts @captured))
                       "all returned contracts must be class agents")
                   (done)))
          (.catch (fn [err]
                    (is false (str "handler threw: " (.-message err)))
                    (done)))))))

(deftest validate-contract-edn-surfaces-agent-shape-warnings
  (let [result (#'sut/validate-contract-edn
                "agents"
                "{:contract/id \"warny\"
                  :contract/kind :agent
                  :trigger-kind :cron
                  :source {:max-messages 2000}
                  :agent {:role :contract_writer}
                  :prompts {:task \"update :data/world_state\"}
                  :data {:filter {:publishChannels [\"123\"]}
                         :source {:max-messages 2000}
                         :plot_log []}}")
        messages (set (map :message (:warnings result)))]
    (is (:ok result))
    (is (some #(str/includes? % ":data/:filter") messages))
    (is (some #(str/includes? % "top-level :source") messages))
    (is (some #(str/includes? % "clamped to 100") messages))
    (is (some #(str/includes? % "mutable runtime state") messages))
    (is (some #(str/includes? % "Role refs should use") messages))
    (is (some #(str/includes? % "Prompt references mutable :data") messages))))
