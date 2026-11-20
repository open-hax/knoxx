(ns knoxx.backend.infra.agent.provider.native-subscription
  "Run host-local subscription workers for one explicitly enrolled membership and actor."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.models :as models]
            [knoxx.backend.extern.eta-mu :as eta-mu-extern]
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

(defn- require-owner!
  [config ctx agent-spec]
  (let [binding (authz/ctx-actor-binding ctx)
        principal {:org-id (authz/ctx-org-id ctx)
                   :membership-id (authz/ctx-membership-id ctx)
                   :actor-id binding}]
    (when-not (and (::mcp-authorized? (meta ctx))
                   (every? #(and (string? %) (not (str/blank? %))) (vals principal))
                   (= (:native-subscription-owner config) principal)
                   (= binding (authz/ctx-actor-id ctx))
                   (= binding (or (:actor-id agent-spec) binding)))
      (throw (http/http-error 403 "native_subscription_unavailable"
                              "Subscription model is unavailable to this actor")))))

(defn authorize-proxx-relay!
  "Require the verified MCP owner before selecting the dedicated Proxx relay."
  [config ctx provider-id agent-spec]
  (when (= models/subscription-relay-provider-id provider-id)
    (require-owner! config ctx agent-spec)
    (when (or (str/blank? (get (:provider-base-urls config) provider-id))
              (not (eta-mu-extern/provider-token-configured?
                    (get (:provider-auth-tokens config) provider-id))))
      (throw (http/http-error 503 "subscription_relay_not_configured"
                              "The dedicated Proxx relay is not configured")))))

(defn resolve-worker!
  "Authorize subscription models and return the native worker selection when applicable."
  [config ctx model-id agent-spec]
  (if (model-id? model-id)
    (let [home (:native-claude-home config)]
      (require-owner! config ctx agent-spec)
      (when-not (= model-id claude-model-id)
        (throw (http/http-error 403 "native_subscription_unavailable"
                                "Native subscription model is unavailable to this actor")))
      (when (str/blank? home)
        (throw (http/http-error 503 "native_claude_home_not_configured"
                                "A dedicated Claude Code profile is required on this host")))
      {:provider :claude :model "haiku" :home home})
    (authorize-proxx-relay! config ctx
                            (:provider (models/resolve-model-contract config model-id))
                            agent-spec)))

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
