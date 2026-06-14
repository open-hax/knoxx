(ns knoxx.backend.domain.action.anonymous-test
  "Tests for anonymous :action/fn compilation and interpretation."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.anonymous :as anonymous]))

(deftest function-values-pass-through
  (testing "real function values are returned untouched"
    (let [f (fn [_ctx _action] {:ok true})]
      (is (identical? f (anonymous/compile-action-fn f))))))

(deftest non-fn-forms-compile-to-nil
  (is (nil? (anonymous/compile-action-fn nil)))
  (is (nil? (anonymous/compile-action-fn "string")))
  (is (nil? (anonymous/compile-action-fn {:not :a-fn})))
  (is (nil? (anonymous/compile-action-fn '(let [x 1] x)))))

(deftest edn-fn-form-compiles-and-runs
  (testing "(fn [ctx action] ...) list forms interpret against ctx/action"
    (let [handler (anonymous/compile-action-fn
                   '(fn [ctx action]
                      {:ok true
                       :actor (:actor/id ctx)
                       :task (get-in action [:action/with :task])}))]
      (is (fn? handler))
      (is (= {:ok true :actor "pi" :task "hello"}
             (handler {:actor/id "pi"}
                      {:action/with {:task "hello"}}))))))

(deftest edn-fn-supports-let-and-destructuring
  (let [handler (anonymous/compile-action-fn
                 '(fn [ctx action]
                    (let [{:keys [agent-id task] :as with} (:with action)
                          shout (str/upper-case task)]
                      {:agent agent-id :shout shout :with with})))]
    (is (= {:agent "a1" :shout "GO" :with {:agent-id "a1" :task "go"}}
           (handler {} {:with {:agent-id "a1" :task "go"}})))))

(deftest edn-fn-can-call-scope-functions
  (testing "scope fns destructured from ctx are callable"
    (let [calls (atom [])
          handler (anonymous/compile-action-fn
                   '(fn [ctx action]
                      (let [send-message (:eta-mu/send-message (:scope ctx))]
                        (send-message {:agent-id "x"}))))]
      (handler {:scope {:eta-mu/send-message (fn [args] (swap! calls conj args))}} {})
      (is (= [{:agent-id "x"}] @calls)))))

(deftest edn-fn-collection-pipeline
  (let [handler (anonymous/compile-action-fn
                 '(fn [ctx action]
                    (filterv (fn [m] (> (:n m) 1))
                             (get-in action [:action/with :items]))))]
    (is (= [{:n 2} {:n 3}]
           (handler {} {:action/with {:items [{:n 1} {:n 2} {:n 3}]}})))))

(deftest edn-fn-conditionals-and-defaults
  (let [handler (anonymous/compile-action-fn
                 '(fn [ctx action]
                    (if (:loud action)
                      (str/upper-case (or (:msg action) "default"))
                      (or (:msg action) "default"))))]
    (is (= "HI" (handler {} {:loud true :msg "hi"})))
    (is (= "default" (handler {} {})))))

(deftest unbound-symbols-fail-closed
  (let [handler (anonymous/compile-action-fn
                 '(fn [ctx action] (js/eval "1")))]
    (is (thrown? js/Error (handler {} {})))))

(deftest unknown-functions-fail-closed
  (let [handler (anonymous/compile-action-fn
                 '(fn [ctx action] (slurp "/etc/passwd")))]
    (is (thrown? js/Error (handler {} {})))))

(deftest vector-destructuring-in-params
  (let [handler (anonymous/compile-action-fn
                 '(fn [ctx action]
                    (let [[a b] (:pair action)]
                      (+ a b))))]
    (is (= 3 (handler {} {:pair [1 2]})))))
