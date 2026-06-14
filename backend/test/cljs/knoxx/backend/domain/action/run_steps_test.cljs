(ns knoxx.backend.domain.action.run-steps-test
  "Tests for the :actions/run-steps action."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.registry :as registry]
            [knoxx.backend.infra.temp-memory :as temp-memory]))

;; ── helpers ───────────────────────────────────────────────────────────

(defn- make-snoop-action
  "Return a handler that records invocations in an atom."
  [calls-atom label]
  (fn [ctx action]
    (swap! calls-atom conj {:label label
                            :with (:action/with action)
                            :ctx-keys (keys ctx)})
    (js/Promise.resolve {:ok true :label label :action/kind label})))

(defn- make-failing-action
  "Return a handler that always fails."
  [label error-msg]
  (fn [_ctx _action]
    (js/Promise.resolve {:ok false :error error-msg :action/kind label})))

;; ── basic invocation ─────────────────────────────────────────────────

(deftest ^:async run-steps-invokes-actions-in-order
  (testing "run-steps calls actions in step order"
    (let [calls (atom [])]
      (registry/register-action!
       ::step-a
       {:action/description "step a"}
       (make-snoop-action calls :a))
      (registry/register-action!
       ::step-b
       {:action/description "step b"}
       (make-snoop-action calls :b))
      (registry/register-action!
       ::step-c
       {:action/description "step c"}
       (make-snoop-action calls :c))
      (let [ctx {:scope {::step-a (fn [ctx action]
                                     ((make-snoop-action calls :a) ctx action))
                         ::step-b (fn [ctx action]
                                     ((make-snoop-action calls :b) ctx action))
                         ::step-c (fn [ctx action]
                                     ((make-snoop-action calls :c) ctx action))}
                 :event {}}
            action {:action/kind :actions/run-steps
                    :action/with {:steps [{:action ::step-a :with {:x 1}}
                                          {:action ::step-b :with {:x 2}}
                                          {:action ::step-c :with {:x 3}}]}}
            result (await (registry/run-action! ctx action))]
        (is (true? (:ok result)))
        (is (= 3 (:steps-run result)))
        (is (= [:a :b :c] (mapv :label @calls)))
        (is (= {:x 1} (:with (nth @calls 0))))
        (is (= {:x 2} (:with (nth @calls 1))))
        (is (= {:x 3} (:with (nth @calls 2))))))))

(deftest ^:async run-steps-single-step
  (testing "run-steps works with a single step"
    (let [calls (atom [])]
      (let [ctx {:scope {::solo (fn [ctx action]
                                   ((make-snoop-action calls :solo) ctx action))}
                 :event {}}
            action {:action/kind :actions/run-steps
                    :action/with {:steps [{:action ::solo :with {:val "only"}}]}}
            result (await (registry/run-action! ctx action))]
        (is (true? (:ok result)))
        (is (= 1 (:steps-run result)))
        (is (= [:solo] (mapv :label @calls)))))))

(deftest ^:async run-steps-empty-steps
  (testing "run-steps with no steps succeeds immediately"
    (let [ctx {:scope {} :event {}}
          action {:action/kind :actions/run-steps
                  :action/with {:steps []}}
          result (await (registry/run-action! ctx action))]
      (is (true? (:ok result)))
      (is (= 0 (:steps-run result))))))

;; ── error handling ────────────────────────────────────────────────────

(deftest ^:async run-steps-error-stops-subsequent-steps
  (testing "error in step 2 prevents step 3 from running"
    (let [calls (atom [])]
      (let [ctx {:scope {::good-a (fn [ctx action]
                                     ((make-snoop-action calls :a) ctx action))
                         ::bad-b  (fn [ctx action]
                                     ((make-failing-action :b "step b failed") ctx action))
                         ::good-c (fn [ctx action]
                                     ((make-snoop-action calls :c) ctx action))}
                 :event {}}
            action {:action/kind :actions/run-steps
                    :action/with {:steps [{:action ::good-a :with {}}
                                          {:action ::bad-b  :with {}}
                                          {:action ::good-c :with {}}]}}
            result (await (registry/run-action! ctx action))]
        (is (false? (:ok result)))
        (is (= "step b failed" (:error result)))
        (is (= 1 (:failed-step result)))
        (is (= ::bad-b (:failed-action result)))
        (is (= [:a] (mapv :label @calls))
            "Only step A should have run; step C was blocked")))))

(deftest ^:async run-steps-error-in-first-step
  (testing "error in step 0 blocks all subsequent steps"
    (let [calls (atom [])]
      (let [ctx {:scope {::bad-a  (fn [_ctx _action]
                                     (js/Promise.resolve {:ok false :error "fail early"}))
                         ::good-b (fn [ctx action]
                                     ((make-snoop-action calls :b) ctx action))}
                 :event {}}
            action {:action/kind :actions/run-steps
                    :action/with {:steps [{:action ::bad-a  :with {}}
                                          {:action ::good-b :with {}}]}}
            result (await (registry/run-action! ctx action))]
        (is (false? (:ok result)))
        (is (= 0 (:failed-step result)))
        (is (= [] (mapv :label @calls))
            "No steps should have run")))))

(deftest ^:async run-steps-action-not-in-scope
  (testing "step with action not in scope returns error"
    (let [ctx {:scope {} :event {}}
          action {:action/kind :actions/run-steps
                  :action/with {:steps [{:action ::nonexistent :with {}}]}}
          result (await (registry/run-action! ctx action))]
      (is (false? (:ok result)))
      (is (= 0 (:failed-step result)))
      (is (clojure.string/includes? (:error result) "not found in scope")))))

;; ── temp-memory interpolation ─────────────────────────────────────────

(deftest ^:async run-steps-interpolates-temp-memory
  (testing "step :with maps get temp-memory placeholders resolved"
    (let [calls (atom [])]
      (await (temp-memory/mem-set! "interpolated-key" "resolved-value" {:ttl 60}))
      (let [ctx {:scope {::interp (fn [ctx action]
                                     (swap! calls conj (:action/with action))
                                     (js/Promise.resolve {:ok true}))}
                 :event {}}
            action {:action/kind :actions/run-steps
                    :action/with {:steps [{:action ::interp
                                          :with {:msg "{{memory.temp:interpolated-key}}"}}]}}
            result (await (registry/run-action! ctx action))]
        (is (true? (:ok result)))
        (is (= "resolved-value" (get-in (first @calls) [:msg]))
            "Placeholder should be replaced with resolved temp memory value")))))

(deftest ^:async run-steps-interpolates-nested-maps
  (testing "temp-memory interpolation works in nested maps"
    (let [calls (atom [])]
      (await (temp-memory/mem-set! "nested-key" "nested-val" {:ttl 60}))
      (let [ctx {:scope {::nested (fn [ctx action]
                                     (swap! calls conj (:action/with action))
                                     (js/Promise.resolve {:ok true}))}
                 :event {}}
            action {:action/kind :actions/run-steps
                    :action/with {:steps [{:action ::nested
                                          :with {:outer {:inner "{{memory.temp:nested-key}}"}}}]}}
            result (await (registry/run-action! ctx action))]
        (is (true? (:ok result)))
        (is (= "nested-val" (get-in (first @calls) [:outer :inner]))
            "Nested placeholder should be resolved")))))

;; ── output to temp memory ─────────────────────────────────────────────

(deftest ^:async run-steps-output-writes-to-temp-memory
  (testing ":output key writes final result to temp memory"
    (await (temp-memory/mem-del! "output-test-key"))
    (let [ctx {:scope {::out (fn [_ctx _action]
                                (js/Promise.resolve {:ok true :data "final"}))}
                 :event {}}
          action {:action/kind :actions/run-steps
                  :action/with {:steps [{:action ::out :with {}}]
                                :output {:key "output-test-key" :ttl 60}}}
          result (await (registry/run-action! ctx action))]
      (is (true? (:ok result)))
      (let [stored (await (temp-memory/mem-get "output-test-key"))]
        (is (some? stored))
        (is (= "final" (:data stored))
            "Final step result should be written to temp memory")))))

(deftest ^:async run-steps-no-output-key
  (testing "run-steps without :output does not write to temp memory"
    (await (temp-memory/mem-del! "no-output-key"))
    (let [ctx {:scope {::no-out (fn [_ctx _action]
                                   (js/Promise.resolve {:ok true :data "ignored"}))}
                 :event {}}
          action {:action/kind :actions/run-steps
                  :action/with {:steps [{:action ::no-out :with {}}]}}
          result (await (registry/run-action! ctx action))]
      (is (true? (:ok result)))
      (let [stored (await (temp-memory/mem-get "no-output-key"))]
        (is (nil? stored)
            "Nothing should be written when :output is absent")))))

;; ── registration metadata ─────────────────────────────────────────────

(deftest run-steps-is-registered
  (testing ":actions/run-steps is registered with correct metadata"
    (let [record (registry/get-action :actions/run-steps)]
      (is (some? record) "run-steps should be registered")
      (is (= "Execute a sequence of actions from scope."
             (get-in record [:metadata :action/description])))
      (is (= "run.steps" (get-in record [:metadata :action/tool :name])))
      (is (= "medium" (get-in record [:metadata :action/tool :risk-level])))
      (is (= :actions.run-steps/request (get-in record [:metadata :action/events :input])))
      (is (= :actions.run-steps/complete (get-in record [:metadata :action/events :output]))))))
