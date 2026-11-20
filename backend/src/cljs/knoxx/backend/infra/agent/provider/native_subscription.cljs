(ns knoxx.backend.infra.agent.provider.native-subscription
  "Run host-local subscription workers for one explicitly enrolled membership and actor."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.native-subscription :as worker]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.shape.agent :as agent]))

(def ^:private claude-model-id "claude-code/haiku")

(defn model-id?
  "Reserve native Claude route names even when the worker is disabled."
  [model-id]
  (and (string? model-id)
       (str/starts-with? model-id "claude-code/")))

(defn with-mcp-authorization
  "Mark a caller whose stored actor binding was verified by the MCP grant resolver."
  [ctx]
  (vary-meta ctx assoc ::mcp-authorized? true))

(defn resolve-worker!
  "Resolve a reserved model only for the configured, stored actor binding."
  [config ctx model-id agent-spec]
  (when (model-id? model-id)
    (let [owner (:native-subscription-owner config)
          binding (authz/ctx-actor-binding ctx)
          principal {:org-id (authz/ctx-org-id ctx)
                     :membership-id (authz/ctx-membership-id ctx)
                     :actor-id binding}
          home (:native-claude-home config)]
      (when-not (and (= model-id claude-model-id)
                     (::mcp-authorized? (meta ctx))
                     (every? #(and (string? %) (not (str/blank? %))) (vals principal))
                     (= owner principal)
                     (= binding (authz/ctx-actor-id ctx))
                     (= binding (or (:actor-id agent-spec) binding)))
        (throw (http/http-error 403 "native_subscription_unavailable"
                                "Native subscription model is unavailable to this actor")))
      (when (str/blank? home)
        (throw (http/http-error 503 "native_claude_home_not_configured"
                                "A dedicated Claude Code profile is required on this host")))
      {:provider :claude :model "haiku" :home home})))

(defn- provider-prompt [system-prompt history text]
  (str (when (seq system-prompt) (str "Instructions:\n" system-prompt "\n\n"))
       (when (seq history)
         (str "Conversation so far (role and text):\n"
              (pr-str (mapv #(select-keys % [:role :text]) history)) "\n\n"))
       text))

(defn- accept-follow-up! [active* message]
  (when-not (string? message)
    (throw (js/Error. "Native subscription sessions accept plain text only")))
  (let [active @active*]
    (when-not (and (:steer active) (< (count (:pending active)) 8))
      (throw (js/Error. "Native subscription session has no available follow-up slot")))
    (swap! active* update :pending conj message)
    (js/Promise.resolve nil)))

(defn- ^:async send-turns! [selection system-prompt active* history* listeners* controller initial-text]
  (try
    (loop [text initial-text usage {}]
      (let [result (await (worker/run-turn!
                           selection controller
                           (provider-prompt system-prompt @history* text)
                           (fn [steer] (swap! active* assoc :steer steer))
                           (fn [delta]
                             (let [event (worker/text-delta delta)]
                               (doseq [listener @listeners*] (listener event))))))
            next-usage (merge-with + usage (:usage result))
            pending (:pending @active*)]
        (swap! history* into [{:role "user" :text text}
                              {:role "assistant" :text (:text result) :usage next-usage}])
        (if (seq pending)
          (do (swap! active* assoc :steer nil :pending (subvec pending 1))
              (recur (first pending) next-usage))
          result)))
    (finally (reset! active* nil))))

(defn- start-turn! [send! active* text]
  (when-not (and (string? text) (not (str/blank? text)))
    (throw (js/Error. "Native subscription sessions accept plain text only")))
  (let [controller (worker/abort-controller)]
    (when-not (compare-and-set! active* nil {:controller controller :pending []})
      (throw (js/Error. "Native subscription session is already running")))
    (let [completion (send! controller text)]
      (swap! active* assoc :completion completion)
      completion)))

(defn- ^:async abort-turn! [active]
  (when-let [controller (:controller active)]
    (worker/abort! controller)
    (try (await (:completion active))
         (catch :default _ nil))))

(defn create-session
  "Build the text-only provider session used by normal Knoxx turn admission."
  [selection system-prompt]
  (let [active* (atom nil)
        listeners* (atom #{})
        history* (atom [])
        send! (partial send-turns! selection system-prompt active* history* listeners*)]
    (reify agent/IAgentSession
      (streaming? [_] (boolean (:steer @active*)))
      (current-turn [_] (:controller @active*))
      (messages [_] (mapv worker/transcript-message @history*))
      (subscribe! [_ listener]
        (swap! listeners* conj listener)
        #(swap! listeners* disj listener))
      (send-user-message! [_ text]
        (start-turn! send! active* text))
      (follow-up! [_ message]
        (accept-follow-up! active* message))
      (steer! [_ message]
        (let [steer (:steer @active*)]
          (when-not steer
            (throw (js/Error. "Native subscription provider has no active generation")))
          (steer message)))
      (set-thinking-level! [_ _level] nil)
      (abort! [_]
        (abort-turn! @active*)))))
