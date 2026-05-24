(ns knoxx.backend.domain.action.registry
  "Action dispatch table.

   The generic resource registry protocol lives in
   knoxx.backend.domain.registry.resource. This namespace is only the action
   interpreter table: it maps an action key to executable behavior.

   Built-in actions:
     :actions/start-agent-session — spawn an agent session
     :actions/run-pipeline        — execute a pipeline resource
     :actions/hello-world         — produce a message/send expectation
     :actions/noop                — no-op, succeeds immediately"
  (:require [clojure.string :as str]))

(defmulti run-action!
  "Dispatch an action map by :action/kind."
  (fn [_ctx action] (:action/kind action)))

(defn action-map
  "Build an action map from a normalized trigger contract."
  [trigger]
  (let [kind (:trigger/action trigger)]
    {:action/id (when (keyword? kind) (name kind))
     :action/kind kind
     :action/with (merge (:trigger/with trigger)
                         (when-let [agent-id (:trigger/agent trigger)]
                           {:agent-id agent-id}))}))

(defmethod run-action! :invoke/noop [_ _]
  (js/Promise.resolve {:ok true :action/kind :invoke/noop}))

(defmethod run-action! :actions/noop [_ _]
  (js/Promise.resolve {:ok true :action/kind :actions/noop}))

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

(defmethod run-action! :default [_ctx action]
  (let [kind (:action/kind action)]
    (if (string? kind)
      (js/console.warn "[knoxx/actions] string actions are not supported; use a keyword from the action registry. Got:" kind)
      (js/console.warn "[knoxx/actions] unknown action/kind" (pr-str kind)))
    (js/Promise.resolve {:ok false :error "unknown action/kind" :action/kind kind})))
