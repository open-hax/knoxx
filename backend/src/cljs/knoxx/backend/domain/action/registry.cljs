(ns knoxx.backend.domain.action.registry
  "Rich action registry: dispatch table + metadata + tool surface.

   Actions are the universal unit of execution. Every registered action carries:
   - A handler function with signature (fn [ctx action] ...)
   - Optional :action/tool metadata (name, description, parameters, risk-level)
   - Optional :action/events input/output contracts
   - Optional :action/scope declarations (what other actions this action can call)

   The generic resource registry protocol (for EDN file discovery) lives in
   knoxx.backend.domain.registry.resource. This namespace owns the executable
   behavior and its metadata.

   Built-in actions:
     :actions/start-agent-session — spawn an agent session
     :actions/run-steps           — execute a sequence of actions from scope
     :actions/agent-control       — steer or follow-up an active agent session
     :actions/run-pipeline        — execute a pipeline resource (deprecated)
     :actions/hello-world         — produce a message/send expectation
     :actions/noop                — no-op, succeeds immediately"
  (:require [clojure.string :as str]
            [knoxx.backend.infra.agent.runtime :as agent-runtime]
            [knoxx.backend.infra.agent.session :as agent-session]
            [knoxx.backend.infra.temp-memory :as temp-memory]
            [knoxx.backend.shape.agent :refer [streaming?]]
            [knoxx.backend.shape.pipeline :as pipeline-shape]))

;; ── Rich Registry ──────────────────────────────────────────────────────

(defonce ^:private action-registry*
  (atom {}))

(defn register-action!
  "Register an action with metadata and handler. Overwrites if already registered.
   Metadata map keys:
     :action/tool        — {:name :description :parameters :risk-level}
     :action/events      — {:input :event/type :output :event/type}
     :action/scope       — {:actions [:actions/... :actions/...]}
     :action/description — human-readable description string"
  [action-key metadata handler]
  (swap! action-registry* assoc action-key
         {:action/key action-key
          :metadata metadata
          :handler handler}))

(defn get-action
  "Return the full action record {:action/key :metadata :handler} for an action key, or nil."
  [action-key]
  (get @action-registry* action-key))

(defn action-handler
  "Return the handler function for an action key, or nil."
  [action-key]
  (:handler (get-action action-key)))

(defn action-metadata
  "Return the metadata map for an action key, or nil."
  [action-key]
  (:metadata (get-action action-key)))

(defn get-tool
  "Return the tool metadata map for an action key, or nil.
   Returns the :action/tool value from the action's metadata."
  [action-key]
  (get-in (get-action action-key) [:metadata :action/tool]))

(defn get-scope-declaration
  "Return the scope data {:actions [...]} for an action key, or nil."
  [action-key]
  (get-in (get-action action-key) [:metadata :action/scope]))

(defn get-event-contract
  "Return the event contract {:input :output} for an action key, or nil."
  [action-key]
  (get-in (get-action action-key) [:metadata :action/events]))

(defn list-actions
  "Return all registered action keys."
  []
  (vec (sort (keys @action-registry*))))

(defn list-tools
  "Return action keys that have :action/tool metadata."
  []
  (->> @action-registry*
       (filter (fn [[_k v]] (get-in v [:metadata :action/tool])))
       (map key)
       sort
       vec))

(defn tool-count
  "Return the number of registered tools."
  []
  (count (list-tools)))

;; ── Multimethod Dispatch ───────────────────────────────────────────────

(defmulti run-action!
  "Dispatch an action map by :action/kind.

   This multimethod is the backward-compatible dispatch path. New actions
   should prefer `register-action!` with metadata instead of `defmethod`.
   The multimethod dispatches to registered handlers when available.

   Context shape: {:event :scope :actor} plus action-specific keys."
  (fn [_ctx action] (:action/kind action)))

;; Bridge: when a registered action exists, route multimethod calls through it.
;; Anonymous actions (:action/fn) take priority over the registry lookup.
(defmethod run-action! :default [ctx action]
  (let [kind (:action/kind action)]
    (if-let [anon-fn (:action/fn action)]
      (anon-fn ctx action)
      (if-let [handler (action-handler kind)]
        (handler ctx action)
        (do
          (if (string? kind)
            (js/console.warn "[knoxx/actions] string actions are not supported; use a keyword from the action registry. Got:" kind)
            (js/console.warn "[knoxx/actions] unknown action/kind" (pr-str kind)))
          (js/Promise.resolve {:ok false :error "unknown action/kind" :action/kind kind}))))))

;; ── Scope Resolution (Phase 1: flat) ───────────────────────────────────

(defn resolve-scope
  "Resolve an action's scope declaration into a map of action-key -> bound-fn.
   Phase 1: flat resolution only (direct references, no transitive walk)."
  [action-key]
  (let [scope-decl (get-scope-declaration action-key)
        action-keys (or (:actions scope-decl) [])]
    (into {}
          (map (fn [k] [k (fn [ctx action] (run-action! ctx action))]))
          action-keys)))

;; ── Helpers ────────────────────────────────────────────────────────────

(defn action-map
  "Build an action map from a normalized trigger contract.
   :trigger/with is the sole argument mechanism — it becomes :action/with.
   Composite resources may carry an inline :action/fn and an :action/scope;
   the action interpreter reads those keys from the raw resource entry."
  [trigger]
  (let [kind (:trigger/action trigger)
        raw (:trigger/raw trigger)]
    (cond-> {:action/id (when (keyword? kind) (name kind))
             :action/kind kind
             :action/with (or (:trigger/with trigger) {})}
      (some? (:action/fn raw)) (assoc :action/fn (:action/fn raw))
      (some? (:action/scope raw)) (assoc :action/scope (:action/scope raw)))))

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn- payload-value
  [event k]
  (let [payload (:event/payload event)]
    (or (get payload k)
        (get payload (keyword (name k)))
        (get payload (name k)))))

(defn- hello-world-message
  [ctx action]
  (let [event (:event ctx)
        name (or (nonblank (payload-value event :name))
                 (nonblank (payload-value event :sender))
                 "world")
        time-of-day (or (nonblank (payload-value event :time-of-day))
                        (nonblank (payload-value event :timeOfDay))
                        (nonblank (get-in action [:action/with :time-of-day]))
                        "day")
        actor-name (or (nonblank (get-in action [:action/with :actor-name]))
                       (nonblank (:actor/id ctx))
                       "Knoxx")]
    (str "Hello, " name "! "
         "I hope you are having a good " time-of-day ". "
         "My name is " actor-name ".")))

;; ── Legacy Multimethods (backward compat) ─────────────────────────────

(defmethod run-action! :invoke/noop [_ _]
  (js/Promise.resolve {:ok true :action/kind :invoke/noop}))

(defmethod run-action! :actions/noop [_ _]
  (js/Promise.resolve {:ok true :action/kind :actions/noop}))

(defmethod run-action! :actions/hello-world
  [ctx action]
  (let [event (:event ctx)
        recipient (or (nonblank (payload-value event :recipient))
                      (nonblank (payload-value event :sender))
                      (nonblank (:actor/id ctx)))
        sender (or (nonblank (:actor/id ctx)) "knoxx")]
    (js/Promise.resolve
     {:ok true
      :action/id (:action/id action)
      :action/kind :actions/hello-world
      :action/result :message/send.expectation
      :event/id (:event/id event)
      :event/type (:event/type event)
      :message/send {:sender sender
                     :recipient recipient
                     :text (hello-world-message ctx action)}})))

;; ── Run-Steps Handler (Phase 2) ────────────────────────────────────────

(defn ^:async run-steps-handler
  "Execute a sequence of actions from scope. Steps are pre-ordered.
   Each step is {:action :actions/... :with {...}}.
   Stops on first error, resolves with {:ok false :error ... :failed-step ...}."
  [ctx action]
  (let [steps (or (get-in action [:action/with :steps]) [])
        output-cfg (get-in action [:action/with :output])]
    (loop [remaining steps
           idx 0
           last-result nil]
      (if (empty? remaining)
        (do
          (when output-cfg
            (await (temp-memory/mem-set! (:key output-cfg) last-result {:ttl (:ttl output-cfg)})))
          {:ok true :action/kind :actions/run-steps :steps-run idx :result last-result})
        (let [step (first remaining)
              step-action-key (:action step)
              step-with (or (:with step) {})
              handler (get-in ctx [:scope step-action-key])]
          (if-not handler
            {:ok false
             :error (str "Action " step-action-key " not found in scope")
             :failed-step idx
             :failed-action step-action-key}
            (let [resolved-temps (await (temp-memory/resolve-temps step-with))
                  interpolated (pipeline-shape/interpolate-map step-with resolved-temps)
                  step-action {:action/kind step-action-key :action/with interpolated}
                  result (try
                           (await (handler ctx step-action))
                           (catch :default err
                             {:ok false :error (.-message err)}))]
              (if (and (map? result) (false? (:ok result)))
                (assoc result :failed-step idx :failed-action step-action-key)
                (recur (rest remaining) (inc idx) result)))))))))

;; ── Agent-Control Handler (Phase 4) ────────────────────────────────────

(defn ^:async agent-control-handler
  "Steer or follow-up an active agent session. Parameterized by :kind."
  [ctx action]
  (let [kind (or (get-in action [:action/with :kind]) "steer")
        conversation-id (or (:conversation-id ctx)
                            (get-in ctx [:event :event/payload :conversationId])
                            (get-in ctx [:event :event/payload :conversation_id]))
        message (or (get-in action [:action/with :message])
                    (get-in ctx [:event :event/payload :content])
                    (get-in ctx [:event :event/payload :text])
                    "")
        session (agent-session/active-agent-session conversation-id)]
    (when-not session
      (throw (js/Error. "No active session for conversation")))
    (when-not (streaming? session)
      (throw (js/Error. "No active running turn is available for live controls")))
    (let [session-id (some-> session .-sessionId)
          run-id (get-in ctx [:event :event/id])]
      (await (agent-runtime/queue-agent-control!
              nil nil
              {:conversation-id conversation-id
               :session-id session-id
               :run-id run-id
               :message message
               :kind kind})))))

;; ── Built-in Action Registration ──────────────────────────────────────

;; Register noop actions (no tool surface — internal use only)
(register-action!
 :actions/noop
 {:action/description "No-op action. Succeeds immediately."}
 (fn [_ctx _action]
   (js/Promise.resolve {:ok true :action/kind :actions/noop})))

(register-action!
 :invoke/noop
 {:action/description "Legacy no-op action. Succeeds immediately."}
 (fn [_ctx _action]
   (js/Promise.resolve {:ok true :action/kind :invoke/noop})))

;; Register hello-world action with tool metadata
(register-action!
 :actions/hello-world
 {:action/description "Produce a greeting message. Used for testing and demos."
  :action/tool
  {:name "hello.world"
   :description "Produce a greeting message"
   :parameters [:map
                [:name {:optional true} :string]
                [:time-of-day {:optional true} :string]]
   :risk-level "low"}
  :action/events
  {:input :message/greeting
   :output :message/send.expectation}
  :action/scope
  {:actions [:actions/noop]}}
 (fn [ctx action]
   (let [event (:event ctx)
         recipient (or (nonblank (payload-value event :recipient))
                       (nonblank (payload-value event :sender))
                       (nonblank (:actor/id ctx)))
         sender (or (nonblank (:actor/id ctx)) "knoxx")]
     (js/Promise.resolve
      {:ok true
       :action/id (:action/id action)
       :action/kind :actions/hello-world
       :action/result :message/send.expectation
       :event/id (:event/id event)
       :event/type (:event/type event)
       :message/send {:sender sender
                      :recipient recipient
                      :text (hello-world-message ctx action)}}))))

;; Register run-steps action
(register-action!
 :actions/run-steps
 {:action/description "Execute a sequence of actions from scope."
  :action/tool
  {:name "run.steps"
   :description "Execute a sequence of actions"
   :parameters [:map [:steps :vector]]
   :risk-level "medium"}
  :action/events
  {:input :actions.run-steps/request
   :output :actions.run-steps/complete}
  :action/scope
  {:actions []}}
 run-steps-handler)

;; Register agent-control action
(register-action!
 :actions/agent-control
 {:action/description "Steer or follow-up on an active agent session. Parameterized by :kind (\"steer\" or \"follow_up\")."
  :action/tool
  {:name "agent.control"
   :description "Steer or follow-up an active agent session"
   :parameters [:map
                [:kind [:enum "steer" "follow_up"]]
                [:message {:optional true} :string]]
   :risk-level "medium"}
  :action/events
  {:input :agent.control/request
   :output :agent.control/complete}
  :action/scope
  {:actions []}}
 agent-control-handler)
