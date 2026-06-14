(ns knoxx.backend.domain.action.interpreter-test
  "Tests for the action interpreter: inline :action/fn execution, scope
   resolution (actions + filters + stores), and action-resource expansion."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.interpreter :as interpreter]
            [knoxx.backend.domain.action.registry :as registry]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.filter.registry :as filter-registry]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.infra.store.protocol :as store]
            [knoxx.backend.infra.store.registry :as store-registry]))

(defn- build-test-deps
  "Build contract-runtime deps for tests."
  []
  {:run-action!    (fn [ctx action] (registry/run-action! ctx action))
   :get-action     (fn [kind] (registry/get-action kind))
   :get-scope-declaration (fn [kind] (registry/get-scope-declaration kind))
   :filter-fn      (fn [filter-id] (filter-registry/filter-fn filter-id))
   :load-resources (fn [config] (resources/load-all-resources-sync config))
   :get-store      (fn [config store-id] (store-registry/get-store! config store-id))
   :list-resource-ids (fn [config resource-kind] (resources/list-resource-ids-sync config resource-kind))
   :get-resource   (fn [config resource-kind resource-id] (resources/resource-record-sync config resource-kind resource-id))
   :resource-class (fn [resource-kind] (resources/resource-class resource-kind))})

(def fixture-config
  {:contracts-dir "test/fixtures/interpreter-contracts"
   :contract-runtime/deps (build-test-deps)})

(defn- ctx-with-deps
  "Create a test ctx with contract-runtime deps injected."
  [base-ctx]
  (assoc-in base-ctx [:config :contract-runtime/deps] (build-test-deps)))

;; ── inline :action/fn ────────────────────────────────────────────────

(deftest ^:async inline-action-fn-takes-precedence
  (testing ":action/fn executes directly, even for registered kinds"
    (registry/register-action!
     ::shadowed
     {}
     (fn [_ _] (js/Promise.resolve {:ok true :from :registered})))
    (let [result (await (interpreter/execute!
                         (ctx-with-deps {:config nil})
                         {:action/kind ::shadowed
                          :action/fn (fn [_ctx _action] {:ok true :from :inline})}))]
      (is (= :inline (:from result))))))

(deftest ^:async inline-action-fn-receives-scope
  (testing "inline actions get their :action/scope resolved into ctx"
    (registry/register-action!
     ::scope-probe
     {}
     (fn [_ _] (js/Promise.resolve {:ok true :from ::scope-probe})))
    (let [result (await (interpreter/execute!
                         (ctx-with-deps {:config nil})
                         {:action/fn (fn [ctx _action]
                                       {:ok true
                                        :scope-keys (vec (keys (:scope ctx)))})
                          :action/scope {:actions [::scope-probe]}}))]
      (is (= [::scope-probe] (:scope-keys result))))))

(deftest ^:async inline-edn-action-fn-runs
  (testing "EDN (fn ...) forms run through the anonymous interpreter"
    (let [result (await (interpreter/execute!
                         (ctx-with-deps {:config nil :actor/id "pi"})
                         {:action/fn '(fn [ctx action]
                                        {:ok true :actor (:actor/id ctx)})}))]
      (is (true? (:ok result)))
      (is (= "pi" (:actor result))))))

;; ── registered kind dispatch with scope ──────────────────────────────

(deftest ^:async registered-kind-gets-declared-scope
  (testing "actions, filters, and stores resolve into ctx :scope"
    (store-registry/reset-stores!)
    (registry/register-action!
     ::wants-scope
     {}
     (fn [ctx _action]
       (js/Promise.resolve {:ok true :scope-keys (set (keys (:scope ctx)))})))
    (let [result (await (interpreter/execute!
                         {:config fixture-config}
                         {:action/kind ::wants-scope
                          :action/scope {:actions [:actions/noop]
                                         :filters [:vector/exclude-shared]
                                         :stores [:testns/events-seen]}}))]
      (is (= #{:actions/noop :vector/exclude-shared :testns/events-seen}
             (:scope-keys result))))))

(deftest ^:async metadata-scope-used-when-action-has-none
  (testing "registered scope metadata applies when the action map declares none"
    (registry/register-action!
     ::meta-scoped
     {:action/scope {:actions [:actions/noop]}}
     (fn [ctx _action]
       (js/Promise.resolve {:ok true :scope-keys (vec (keys (:scope ctx)))})))
    (let [result (await (interpreter/execute!
                         (ctx-with-deps {:config nil})
                         {:action/kind ::meta-scoped}))]
      (is (= [:actions/noop] (:scope-keys result))))))

;; ── resolve-scope-decl ───────────────────────────────────────────────

(deftest resolve-scope-decl-skips-unknown-filters-and-stores
  (store-registry/reset-stores!)
  (let [scope (interpreter/resolve-scope-decl
               fixture-config
               {:actions [:actions/noop]
                :filters [:nonexistent/filter]
                :stores [:nonexistent/store]})]
    (is (= [:actions/noop] (vec (keys scope))))))

(deftest resolve-scope-decl-resolves-filters
  (let [scope (interpreter/resolve-scope-decl
               (assoc fixture-config :contract-runtime/deps (build-test-deps))
               {:filters [:vector/exclude-shared]})
        exclude-shared (get scope :vector/exclude-shared)]
    (is (fn? exclude-shared))
    (is (= [{:id 2}] (exclude-shared [{:id 1} {:id 2}] [{:id 1}])))))

(deftest ^:async resolve-scope-decl-resolves-stores
  (store-registry/reset-stores!)
  (let [scope (interpreter/resolve-scope-decl fixture-config {:stores [:testns/events-seen]})
        events-seen (get scope :testns/events-seen)]
    (is (some? events-seen))
    (await (store/insert! events-seen {:id "m1" :n 1}))
    (let [found (await (events-seen {:id "m1"}))]
      (is (= [{:id "m1" :n 1}] found)))))

;; ── action resource expansion ────────────────────────────────────────

(deftest ^:async action-resource-expands-by-action-id
  (testing "unknown kinds expand through enabled EDN action resources"
    (let [received (atom nil)]
      (registry/register-action!
       :interpreter-test/echo
       {}
       (fn [_ctx action]
         (reset! received action)
         (js/Promise.resolve {:ok true})))
      (let [result (await (interpreter/execute!
                           {:config fixture-config}
                           {:action/kind :testns/echo
                            :action/with {:extra 2}}))]
        (is (true? (:ok result)))
        (is (= :interpreter-test/echo (:action/kind @received))
            "resource :action/kind replaces the reference kind")
        (is (= {:source "resource" :base 1 :extra 2}
               (:action/with @received))
            "resource :action/with merges under the trigger's :action/with")))))

(deftest ^:async unknown-kind-still-falls-through
  (let [result (await (interpreter/execute!
                       {:config fixture-config}
                       {:action/kind :totally/unknown}))]
    (is (false? (:ok result)))))
