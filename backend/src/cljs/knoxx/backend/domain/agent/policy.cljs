(ns knoxx.backend.domain.agent.policy
  "Chat policy enforcement: model allow-lists and rate-limiting."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.auth.authz :as authz]
            [knoxx.backend.infra.redis-client :as redis]))

(defn- chat-policy-constraints
  [auth-context]
  (let [constraints (authz/ctx-tool-constraints auth-context "agent.chat")]
    (cond
      (map? constraints) constraints
      (and constraints (= "object" (goog/typeOf constraints))) (js->clj constraints :keywordize-keys true)
      :else {})))

(defn- positive-int
  [value]
  (cond
    (number? value) (let [n (int value)]
                      (when (pos? n) n))
    (string? value) (let [parsed (js/parseInt value 10)]
                      (when (and (not (js/isNaN parsed)) (> parsed 0))
                        parsed))
    :else nil))

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

(defn enforce-chat-policy!
  [auth-context model-id]
  (let [constraints (chat-policy-constraints auth-context)
        permitted-models (allowed-models constraints)
        max-requests (positive-int (or (:maxRequests constraints)
                                       (:max-requests constraints)))
        window-seconds (positive-int (or (:windowSeconds constraints)
                                         (:window-seconds constraints)))
        principal (some-> (chat-rate-limit-principal auth-context) str not-empty)
        redis-client (redis/get-client)]
    (cond
      (and (seq permitted-models)
           (not (contains? permitted-models model-id)))
      (js/Promise.reject (model-policy-error model-id permitted-models))

      (and principal redis-client max-requests window-seconds)
      (let [key (str "knoxx:chat-rate:" principal ":" window-seconds)]
        (-> (.incr redis-client key)
            (.then (fn [count]
                     (if (= count 1)
                       (-> (.expire redis-client key window-seconds)
                           (.then (fn [_] count)))
                       (js/Promise.resolve count))))
            (.then (fn [count]
                     (if (> count max-requests)
                       (js/Promise.reject (rate-limit-error max-requests window-seconds))
                       (js/Promise.resolve nil))))
            (.catch (fn [err]
                      (if (= (aget err "code") "chat_rate_limited")
                        (js/Promise.reject err)
                        (js/Promise.resolve nil))))))

      :else
      (js/Promise.resolve nil))))

(defn validate-chat-policy!
  [auth-context model-id]
  (enforce-chat-policy! auth-context model-id))
