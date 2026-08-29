(ns knoxx.backend.infra.publication-target-registry-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.publication-plan :as plan]
            [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.infra.publication-target-memory :as memory]
            [knoxx.backend.infra.publication-target-registry :as registry]))

(def ^:private intent
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/target :knoxx.publication/memory
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :required
   :document/source-locale :en})

(def ^:private artifact
  {:artifact/content "<p>translated</p>"
   :artifact/media-type "text/html"
   :artifact/encoding "utf-8"
   :artifact/locale :es
   :artifact/revision "rev-1"})

(def ^:private declarations
  [{:publication-target/id :knoxx.publication/memory
    :publication-target/kind :publication-target/memory
    :publication-target/config {}
    :publication-target/enabled? true}])

(defn- memory-factory
  [declaration]
  (:target (memory/memory-target {:id (:publication-target/id declaration)})))

(defn- registry-with
  [target-declarations]
  (registry/make-registry target-declarations {:publication-target/memory memory-factory}))

(defn- publish-plan
  []
  {:op :publish
   :intent intent
   :desired {:materialized/revision "rev-1" :materialized/path "/probe"}
   :concrete-revision "rev-1"})

(deftest resource-declarations-select-a-deterministic-adapter
  (let [target (registry/resolve-target! (registry-with declarations)
                                         :knoxx.publication/memory)]
    (is (= :knoxx.publication/memory (effects/target-id target)))))

(deftest invalid-target-declarations-fail-before-adapter-construction
  (doseq [[label target-declarations]
          [["malformed" [(dissoc (first declarations) :publication-target/config)]]
           ["duplicate" (conj declarations (first declarations))]]]
    (testing label
      (is (thrown? js/Error (registry-with target-declarations)))))
  (let [calls (atom 0)
        disabled (assoc (first declarations) :publication-target/enabled? false)
        target-registry (registry/make-registry [disabled]
                                                {:publication-target/memory
                                                 (fn [_]
                                                   (swap! calls inc)
                                                   (memory-factory disabled))})]
    (is (thrown? js/Error (registry/resolve-target! target-registry
                                                    :knoxx.publication/memory)))
    (is (zero? @calls)))
  (is (thrown? js/Error (registry/resolve-target! (registry-with declarations)
                                                 :knoxx.publication/missing))))

(deftest ^:async registry-admits-only-catalog-approved-locales-before-effects
  (let [{:keys [store]} (memory/memory-store)
        target-registry (registry-with declarations)
        rejected? (atom false)]
    (try
      (await (registry/execute-plan! target-registry store {:request/id :unchanged}
                                     (publish-plan) artifact
                                     (fn [_ _ _] false)))
      (catch :default _ (reset! rejected? true)))
    (is @rejected?)
    (testing "without a locale-catalog admission, the adapter is never invoked"
      (is (thrown? js/Error
                   (await (registry/execute-plan! target-registry store {}
                                                 (publish-plan) artifact nil)))))))

(deftest ^:async registry-preserves-publication-context-for-the-selected-adapter
  (let [{:keys [store]} (memory/memory-store)
        seen-context (atom nil)
        context {:request/id :unchanged}
        target-registry
        (registry/make-registry
         declarations
         {:publication-target/memory
          (fn [declaration]
            (let [target (memory-factory declaration)]
              (reify effects/IPublicationTarget
                (target-id [_] (effects/target-id target))
                (publish! [_ adapter-context op]
                  (reset! seen-context adapter-context)
                  (effects/publish! target adapter-context op))
                (remove! [_ adapter-context publication-intent observed]
                  (effects/remove! target adapter-context publication-intent observed))
                (observe! [_ adapter-context publication-intent]
                  (effects/observe! target adapter-context publication-intent)))))
          })]
    (is (= :publication/materialized
           (:receipt/type
            (await (registry/execute-plan! target-registry store context
                                           (publish-plan) artifact
                                           (fn [_ _ _] true))))))
    (is (identical? context @seen-context))))

(deftest target-configuration-cannot-change-the-pure-plan
  (let [resource-index {:gardens {:knoxx.docs/promethean
                                  {:garden/status :active
                                   ;; The locale-catalog cross-check (#250)
                                   ;; merged after this fixture: a garden
                                   ;; without locales now blocks every
                                   ;; publish, so the fixture must declare
                                   ;; the intent's locale to keep testing
                                   ;; what it names.
                                   :garden/locales [:en :es]}}}
        facts {:current-source-revision (constantly "rev-1")
               :translated-revision? (constantly true)
               :approved? (constantly true)
               :source-revision-superseded? (constantly false)
               :materialized-publication (constantly nil)}]
    (is (= (plan/reconcile-plan resource-index intent facts)
           (plan/reconcile-plan resource-index intent facts)))
    (is (= :publish (:op (plan/reconcile-plan resource-index intent facts))))))
