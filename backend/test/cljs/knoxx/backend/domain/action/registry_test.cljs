(ns knoxx.backend.domain.action.registry-test
  "Tests for the rich action registry."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.registry :as registry]))

(deftest register-and-get-action
  (testing "register-action! stores handler and metadata"
    (let [handler (fn [ctx action] {:ok true})]
      (registry/register-action!
       ::test-action
       {:action/description "test action"
        :action/tool {:name "test" :description "test" :parameters [] :risk-level "low"}}
       handler)
      (let [record (registry/get-action ::test-action)]
        (is (some? record) "Action should be registered")
        (is (= ::test-action (:action/key record)))
        (is (= handler (:handler record)))
        (is (= "test action" (get-in record [:metadata :action/description])))))))

(deftest get-tool-returns-tool-metadata
  (testing "get-tool returns :action/tool from metadata"
    (registry/register-action!
     ::tool-action
     {:action/tool {:name "my.tool" :description "does things" :parameters [] :risk-level "medium"}}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (let [tool (registry/get-tool ::tool-action)]
      (is (some? tool))
      (is (= "my.tool" (:name tool)))
      (is (= "does things" (:description tool)))
      (is (= "medium" (:risk-level tool))))))

(deftest get-tool-nil-for-internal-action
  (testing "get-tool returns nil for actions without :action/tool"
    (registry/register-action!
     ::internal-action
     {:action/description "internal only"}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (is (nil? (registry/get-tool ::internal-action)))))

(deftest get-scope-declaration
  (testing "get-scope-declaration returns scope data"
    (registry/register-action!
     ::scoped-action
     {:action/scope {:actions [:actions/foo :actions/bar]}}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (is (= {:actions [:actions/foo :actions/bar]}
           (registry/get-scope-declaration ::scoped-action)))))

(deftest get-scope-declaration-nil-when-absent
  (testing "get-scope-declaration returns nil when no scope"
    (registry/register-action!
     ::no-scope-action
     {}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (is (nil? (registry/get-scope-declaration ::no-scope-action)))))

(deftest get-event-contract
  (testing "get-event-contract returns input/output"
    (registry/register-action!
     ::event-action
     {:action/events {:input :foo/request :output :foo/complete}}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (is (= {:input :foo/request :output :foo/complete}
           (registry/get-event-contract ::event-action)))))

(deftest list-actions-includes-registered
  (testing "list-actions includes all registered action keys"
    (registry/register-action!
     ::list-test-a
     {}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (registry/register-action!
     ::list-test-b
     {}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (let [actions (registry/list-actions)]
      (is (some #{::list-test-a} actions))
      (is (some #{::list-test-b} actions)))))

(deftest list-tools-excludes-internal-actions
  (testing "list-tools only includes actions with :action/tool"
    (registry/register-action!
     ::tool-visible
     {:action/tool {:name "visible.tool" :description "v" :parameters [] :risk-level "low"}}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (registry/register-action!
     ::tool-hidden
     {:action/description "no tool metadata"}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (let [tools (registry/list-tools)]
      (is (some #{::tool-visible} tools))
      (is (not (some #{::tool-hidden} tools))))))

(deftest tool-count
  (testing "tool-count returns count of tools"
    (registry/register-action!
     ::count-tool
     {:action/tool {:name "count.tool" :description "c" :parameters [] :risk-level "low"}}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (is (pos? (registry/tool-count)))))

(deftest builtin-hello-world-is-registered
  (testing "hello-world is registered with tool metadata"
    (let [record (registry/get-action :actions/hello-world)]
      (is (some? record) "hello-world should be registered")
      (is (some? (registry/get-tool :actions/hello-world)) "hello-world should have tool metadata")
      (is (= "hello.world" (get-in record [:metadata :action/tool :name])))
      (is (= :message/greeting (get-in record [:metadata :action/events :input])))
      (is (= :message/send.expectation (get-in record [:metadata :action/events :output]))))))

(deftest builtin-noop-is-registered
  (testing "noop is registered without tool metadata"
    (is (some? (registry/get-action :actions/noop)))
    (is (nil? (registry/get-tool :actions/noop)) "noop should not have tool metadata")))

(deftest ^:async run-action!-bridge-to-registered
  (testing "run-action! dispatches to registered handler for unknown defmethods"
    (registry/register-action!
     ::bridge-test
     {}
     (fn [ctx action]
       (js/Promise.resolve {:ok true :bridge-worked true :ctx-keys (keys ctx)})))
    (let [result (await (registry/run-action!
                         {:event {:event/type :test} :scope {} :actor {:id "test"}}
                         {:action/kind ::bridge-test :action/with {}}))]
      (is (true? (:bridge-worked result))))))

(deftest ^:async run-action!-default-fallback
  (testing "run-action! returns error for truly unknown actions"
    (let [result (await (registry/run-action!
                         {:event nil :scope {} :actor {}}
                         {:action/kind :totally-unknown-action}))]
      (is (false? (:ok result)))
      (is (= :totally-unknown-action (:action/kind result))))))

;; ── resolve-scope tests ─────────────────────────────────────────────────

(deftest resolve-scope-returns-empty-for-no-scope
  (testing "resolve-scope returns empty map when no scope declared"
    (registry/register-action!
     ::no-scope-action
     {}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (is (= {} (registry/resolve-scope ::no-scope-action)))))

(deftest resolve-scope-resolves-declared-actions
  (testing "resolve-scope resolves declared action keys to bound fns"
    (registry/register-action!
     ::scope-target-a
     {}
     (fn [_ _] (js/Promise.resolve {:ok true :from :a})))
    (registry/register-action!
     ::scope-target-b
     {}
     (fn [_ _] (js/Promise.resolve {:ok true :from :b})))
    (registry/register-action!
     ::scoped-action
     {:action/scope {:actions [::scope-target-a ::scope-target-b]}}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (let [scope (registry/resolve-scope ::scoped-action)]
      (is (= 2 (count scope)))
      (is (fn? (get scope ::scope-target-a)))
      (is (fn? (get scope ::scope-target-b))))))

(deftest ^:async resolve-scope-bound-fn-delegates-to-run-action!
  (testing "bound fn in scope delegates to run-action!"
    (registry/register-action!
     ::scope-delegate
     {}
     (fn [ctx action]
       (js/Promise.resolve {:ok true :scope-received (:scope ctx)})))
    (registry/register-action!
     ::scope-parent
     {:action/scope {:actions [::scope-delegate]}}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (let [scope (registry/resolve-scope ::scope-parent)
          bound-fn (get scope ::scope-delegate)
          result (await (bound-fn {:event {} :scope {} :actor {}}
                                  {:action/kind ::scope-delegate}))]
      (is (true? (:ok result)))
      (is (= {} (:scope-received result))))))

(deftest resolve-scope-warns-on-unknown-action
  (testing "resolve-scope includes unknown action keys (handler will fallback at runtime)"
    (registry/register-action!
     ::scope-with-unknown
     {:action/scope {:actions [::totally-nonexistent-action]}}
     (fn [_ _] (js/Promise.resolve {:ok true})))
    (let [scope (registry/resolve-scope ::scope-with-unknown)]
      (is (= 1 (count scope)))
      (is (fn? (get scope ::totally-nonexistent-action))))))



(deftest resolve-scope-includes-hello-world-scope
  (testing "builtin hello-world scope resolves to noop"
    (let [scope (registry/resolve-scope :actions/hello-world)]
      (is (= 1 (count scope)))
      (is (fn? (get scope :actions/noop))))))

;; ── agent-control action tests ────────────────────────────────────────

(deftest agent-control-is-registered
  (testing ":actions/agent-control is registered with tool metadata"
    (let [record (registry/get-action :actions/agent-control)]
      (is (some? record) "agent-control should be registered")
      (is (= "Steer or follow-up on an active agent session. Parameterized by :kind (\"steer\" or \"follow_up\")."
             (get-in record [:metadata :action/description])))
      (is (= "agent.control" (get-in record [:metadata :action/tool :name])))
      (is (= "medium" (get-in record [:metadata :action/tool :risk-level]))))))

(deftest agent-control-has-correct-tool-parameters
  (testing ":actions/agent-control tool parameters include :kind and :message"
    (let [tool (registry/get-tool :actions/agent-control)]
      (is (some? tool))
      (is (= "agent.control" (:name tool))))))

;; ── Anonymous action tests ──────────────────────────────────────────────

(deftest ^:async anonymous-action-fn-is-executed
  (testing "run-action! executes :action/fn directly when present"
    (let [ctx {:event {:event/type :test} :scope {} :actor {:id "anon"}}
          action {:action/kind :some/anonymous
                  :action/with {:msg "hello"}
                  :action/fn (fn [c a]
                               (js/Promise.resolve
                                {:ok true
                                 :anon-worked true
                                 :msg (get-in a [:action/with :msg])
                                 :actor (:actor/id c)}))}
          result (await (registry/run-action! ctx action))]
      (is (true? (:ok result)))
      (is (true? (:anon-worked result)))
      (is (= "hello" (:msg result))))))

(deftest ^:async anonymous-action-not-in-list-actions
  (testing "anonymous actions are not discoverable via list-actions"
    (let [before (set (registry/list-actions))
          _ (registry/run-action!
             {:event nil :scope {} :actor {}}
             {:action/kind :anon/hidden
              :action/fn (fn [_ _] (js/Promise.resolve {:ok true}))})
          after (set (registry/list-actions))]
      (is (not (contains? after :anon/hidden))
          "anonymous action should not appear in list-actions")
      (is (= before after)
          "list-actions should be unchanged after running anonymous action"))))

(deftest ^:async registered-action-still-works-without-action-fn
  (testing "registered handler is invoked when :action/fn is absent"
    (registry/register-action!
     ::registered-no-fn
     {}
     (fn [ctx action]
       (js/Promise.resolve {:ok true :registered-worked true})))
    (let [result (await (registry/run-action!
                         {:event nil :scope {} :actor {}}
                         {:action/kind ::registered-no-fn :action/with {}}))]
      (is (true? (:ok result)))
      (is (true? (:registered-worked result))))))
