(ns knoxx.backend.infra.agent.policy
  "Chat policy enforcement: model allow-lists and rate-limiting."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.redis-client :as redis]))
;; This kind of file isn't suposed to exist at all
;; these should all be things we can define in the contract DSL

(defn- chat-policy-constraints
  [auth-context]
  (let [constraints (authz/ctx-tool-constraints auth-context "agent.chat")]
    (cond
      (map? constraints) constraints
      (and constraints (= "object" (goog/typeOf constraints))) (js->clj constraints :keywordize-keys true)
      :else {})))



(defn- allowed-models
  [constraints]
  (let [raw (or (:allowedModels constraints)
                (:allowed-models constraints)
                (:models constraints))]
    (->> (cond
           (sequential? raw) raw
           (array? raw) (array-seq raw)
           :else [])
         (keep #(when (string? %)
                  (let [t (str/trim %)]
                    (when-not (str/blank? t) t))))
         set)))

(defn- chat-rate-limit-principal
  [auth-context]
  (or (get-in auth-context [:membership :id])
      (:membershipId auth-context)
      (get-in auth-context [:user :id])
      (:userId auth-context)
      (get-in auth-context [:user :email])
      (:userEmail auth-context)))

(defn- positive-int
  [value]
  (let [parsed (js/parseInt value 10)]
    (when (and (number? parsed) (not (js/isNaN parsed)) (pos? parsed))
      parsed)))

(defn- rate-limit-error
  [max-requests window-seconds]
  (doto (js/Error. (str "Chat rate limit exceeded: more than " max-requests
                        " requests in " window-seconds " seconds"))
    (aset "statusCode" 429)
    (aset "code" "chat_rate_limited")))

(defn- model-policy-error
  [model-id allowed]
  (doto (js/Error. (str "Model '" model-id "' is not allowed for this account. Allowed models: "
                        (str/join ", " (sort allowed))))
    (aset "statusCode" 403)
    (aset "code" "model_not_allowed")))

(defn- check-model-policy!
  "Throw if model-id is not in the allow-list."
  [model-id permitted-models]
  (when (and (seq permitted-models)
             (not (contains? permitted-models model-id)))
    (throw (model-policy-error model-id permitted-models))))

(defn ^:async check-rate-limit!
  "Check and enforce the chat rate limit via Redis INCR + EXPIRE."
  [principal redis-client max-requests window-seconds]
  (let [key (str "knoxx:chat-rate:" principal ":" window-seconds)]
    (try
      (let [count (await (.incr redis-client key))]
        (when (= count 1)
          (await (.expire redis-client key window-seconds)))
        (when (> count max-requests)
          (throw (rate-limit-error max-requests window-seconds))))
      (catch :default err
        (when (= (aget err "code") "chat_rate_limited")
          (throw err))))))

(defn enforce-chat-policy!
  "Enforce model allow-list and rate-limit constraints for a chat turn.
   Returns a Promise that resolves to nil on success or rejects on policy violation."
  [auth-context model-id]
  (let [constraints (chat-policy-constraints auth-context)
        permitted-models (allowed-models constraints)
        max-requests (positive-int (or (:maxRequests constraints)
                                       (:max-requests constraints)))
        window-seconds (positive-int (or (:windowSeconds constraints)
                                         (:window-seconds constraints)))
        principal (some-> (chat-rate-limit-principal auth-context) str not-empty)
        redis-client (redis/get-client)]
    (check-model-policy! model-id permitted-models)
    (if (and principal redis-client max-requests window-seconds)
      (check-rate-limit! principal redis-client max-requests window-seconds)
      (js/Promise.resolve nil))))

(defprotocol IPolicyEngine
  (authorize-turn [engine turn-request])
  (resolve-model-policy [engine auth-context requested-model])
  (resolve-tool-policy [engine auth-context agent-spec])
  (resolve-resource-policy [engine auth-context agent-spec]))

(defrecord ChatPolicyEngine []
  IPolicyEngine
  (authorize-turn [_ turn-request]
    (enforce-chat-policy! (:auth-context turn-request)
                          (or (:model turn-request)
                              (get-in turn-request [:agent-spec :model]))))

  (resolve-model-policy [_ auth-context requested-model]
    (let [p (enforce-chat-policy! auth-context requested-model)]
      (-> p (.then (fn [_] {:model requested-model :allowed true})))))

  (resolve-tool-policy [_ auth-context agent-spec]
    (js/Promise.resolve {:auth-context auth-context
                         :agent-spec agent-spec}))

  (resolve-resource-policy [_ auth-context agent-spec]
    (js/Promise.resolve {:auth-context auth-context
                         :agent-spec agent-spec})))

(def default-policy-engine
  (->ChatPolicyEngine))

(defn validate-chat-policy!
  [auth-context model-id]
  (resolve-model-policy default-policy-engine auth-context model-id))
