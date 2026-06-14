(ns knoxx.backend.infra.store-test
  "Tests for the IStore protocol: schema guard, memory backend, registry, and
   the MongoCollection record against a stubbed collection handle."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.store.memory :as memory]
            [knoxx.backend.infra.store.mongo :as mongo]
            [knoxx.backend.infra.store.protocol :as store]
            [knoxx.backend.infra.store.registry :as store-registry]
            [knoxx.backend.domain.resources.loader :as resources]
            [open-hax.contract-runtime.store.law :as store-law]))

(defn- build-test-deps
  "Build contract-runtime deps for tests."
  []
  {:load-resources (fn [config] (resources/load-all-resources-sync config))})

(def fixture-config
  {:contracts-dir "test/fixtures/interpreter-contracts"
   :contract-runtime/deps (build-test-deps)})

;; ── schema guard ─────────────────────────────────────────────────────

(deftest schema-guard-passes-valid-docs
  (let [guard (store-law/compile-schema-guard [:map [:id :string]])]
    (is (= {:id "a"} (guard {:id "a"})))))

(deftest schema-guard-throws-on-invalid-docs
  (let [guard (store-law/compile-schema-guard [:map [:id :string]])]
    (is (thrown? js/Error (guard {:id 42})))))

(deftest nil-schema-guard-passes-everything
  (let [guard (store-law/compile-schema-guard nil)]
    (is (= {:anything true} (guard {:anything true})))))

;; ── memory collection ────────────────────────────────────────────────

(deftest ^:async memory-collection-insert-and-find
  (let [coll (memory/memory-collection {:store/id :t
                                        :store/schema [:map [:message-id :string]]})]
    (await (store/insert! coll {:message-id "m1" :who "a"}))
    (await (store/insert! coll {:message-id "m2" :who "b"}))
    (testing "field-equality query"
      (is (= [{:message-id "m1" :who "a"}]
             (await (store/find-docs coll {:who "a"})))))
    (testing "empty query returns everything in insertion order"
      (is (= ["m1" "m2"] (mapv :message-id (await (store/find-docs coll {}))))))
    (testing ":limit caps results"
      (is (= 1 (count (await (store/find-docs coll {:limit 1}))))))))

(deftest ^:async memory-collection-guards-inserts
  (let [coll (memory/memory-collection {:store/id :t
                                        :store/schema [:map [:message-id :string]]})]
    (let [err (await (-> (store/insert! coll {:message-id 7})
                         (.then (fn [_] nil))
                         (.catch (fn [e] e))))]
      (is (some? err) "invalid doc must reject"))
    (is (= [] (await (store/find-docs coll {}))) "invalid doc must not persist")))

(deftest ^:async memory-collection-is-callable
  (testing "(store query) is shorthand for find-docs"
    (let [coll (memory/memory-collection {:store/id :t :store/schema nil})]
      (await (store/insert! coll {:id "x"}))
      (is (= [{:id "x"}] (await (coll {:id "x"})))))))

;; ── mongo collection (stub handle) ───────────────────────────────────

(defn- stub-mongo-handle
  [inserted* find-result]
  #js {:insertOne (fn [doc]
                    (swap! inserted* conj (js->clj doc :keywordize-keys true))
                    (js/Promise.resolve #js {:acknowledged true}))
       :find (fn [query]
               (let [limited* (atom nil)]
                 #js {:limit (fn [n] (reset! limited* n)
                               #js {:toArray (fn [] (js/Promise.resolve (clj->js (take @limited* find-result))))})
                      :toArray (fn [] (js/Promise.resolve (clj->js find-result)))}))})

(deftest ^:async mongo-collection-inserts-guarded-docs
  (let [inserted* (atom [])
        coll (mongo/mongo-collection {:store/id :m
                                      :store/schema [:map [:id :string]]}
                                     (stub-mongo-handle inserted* []))]
    (is (= {:id "a"} (await (store/insert! coll {:id "a"}))))
    (is (= [{:id "a"}] @inserted*))))

(deftest ^:async mongo-collection-finds-and-limits
  (let [coll (mongo/mongo-collection {:store/id :m :store/schema nil}
                                     (stub-mongo-handle (atom []) [{:id "a"} {:id "b"}]))]
    (is (= [{:id "a"} {:id "b"}] (await (store/find-docs coll {}))))
    (is (= [{:id "a"}] (await (store/find-docs coll {:limit 1}))))))

;; ── store registry ───────────────────────────────────────────────────

(deftest ^:async store-registry-instantiates-from-resource-definition
  (store-registry/reset-stores!)
  (let [coll (store-registry/get-store! fixture-config :testns/events-seen)]
    (is (some? coll) "store resource should instantiate")
    (is (identical? coll (store-registry/get-store! fixture-config :testns/events-seen))
        "instances are cached")
    (await (store/insert! coll {:id "e1"}))
    (is (= [{:id "e1"}] (await (store/find-docs coll {}))))))

(deftest store-registry-returns-nil-for-unknown-store
  (store-registry/reset-stores!)
  (is (nil? (store-registry/get-store! fixture-config :testns/never-declared))))

(deftest store-registry-explicit-registration-wins
  (store-registry/reset-stores!)
  (let [custom (memory/memory-collection {:store/id :custom :store/schema nil})]
    (store-registry/register-store! :testns/events-seen custom)
    (is (identical? custom (store-registry/get-store! fixture-config :testns/events-seen)))
    (store-registry/reset-stores!)))
