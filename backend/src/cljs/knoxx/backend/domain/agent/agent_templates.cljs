(ns knoxx.backend.domain.agent.agent-templates
  "Trusted contract template runtime for Knoxx.

   Contracts are filesystem/editor-authored and may contain EDN list forms such as
   (template {:separator \" \"} [...]).  This namespace evaluates a small,
   deterministic ClojureScript-like data DSL against a runtime context and keeps
   the old event-agent job helper surface only as compatibility plumbing."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.config :as runtime-config]
            [knoxx.backend.infra.defaults :as defaults]))

;; =============================================================================
;; Contract template evaluation
;; =============================================================================

(def ^:private lookup-missing #js {})

(defn- symbol-name
  [value]
  (cond
    (symbol? value) (name value)
    (keyword? value) (name value)
    (string? value) value
    :else nil))

(defn template-form?
  "True when value is an EDN form whose head is the trusted template operator."
  [value]
  (and (sequential? value)
       (= "template" (symbol-name (first value)))))

(defn- key-candidates
  [k]
  (let [raw (cond
              (keyword? k) (name k)
              (symbol? k) (name k)
              (string? k) k
              :else (str k))]
    (distinct
     (remove nil?
             [k
              (when raw (keyword raw))
              (when raw raw)
              (when (and (keyword? k) (namespace k))
                (str (namespace k) "/" (name k)))]))))

(defn- lookup-one
  ([m k]
   (lookup-one m k nil))
  ([m k not-found]
   (cond
     (nil? m) not-found

     (map? m)
     (let [found (reduce (fn [_ candidate]
                           (if (contains? m candidate)
                             (reduced (get m candidate))
                             lookup-missing))
                         lookup-missing
                         (key-candidates k))]
       (if (identical? lookup-missing found) not-found found))

     (and (vector? m) (number? k))
     (get m k not-found)

     (and (array? m) (number? k))
     (let [v (aget m k)]
       (if (undefined? v) not-found v))

     (object? m)
     (let [raw (cond
                 (keyword? k) (name k)
                 (symbol? k) (name k)
                 :else (str k))
           v (aget m raw)]
       (if (undefined? v) not-found v))

     :else not-found)))

(defn- lookup-path
  [m ks]
  (reduce (fn [acc k]
            (lookup-one acc k nil))
          m
          ks))

(def ^:private known-template-op-names
  #{"template" "quote" "do" "let" "if" "when" "fn" "fn*"
    "str" "pr-str" "name" "count" "first" "second" "last" "rest"
    "vec" "distinct" "sort" "keys" "vals" "get" "get-in" "map"
    "filter" "keep" "join" "not" "and" "or" "="})

(def ^:private env-missing #js {})

(defn- env-candidates
  [value]
  (let [raw (symbol-name value)]
    (distinct
     (remove nil?
             [value
              (when raw raw)
              (when raw (symbol raw))]))))

(defn- lookup-env
  [env value]
  (reduce (fn [_ candidate]
            (if (contains? env candidate)
              (reduced (get env candidate))
              env-missing))
          env-missing
          (env-candidates value)))

(defn- env-ref?
  [env value]
  (not (identical? env-missing (lookup-env env value))))

(declare eval-template-form)

(defn- eval-env-ref-or-form
  [form env]
  (let [found (lookup-env env form)]
    (if (identical? env-missing found)
      (eval-template-form form env)
      found)))

(defn- keylike?
  [value]
  (or (keyword? value)
      (symbol? value)
      (string? value)))

(defn- keyword-call-vector?
  [value env]
  (and (vector? value)
       (> (count value) 1)
       (string? (first value))
       (not (contains? known-template-op-names (first value)))
       (env-ref? env (last value))))

(defn- executable-vector-form?
  [value env]
  (and (vector? value)
       (seq value)
       (let [op (first value)
             op-name (symbol-name op)]
         (or (keyword? op)
             (symbol? op)
             (contains? known-template-op-names op-name)
             (keyword-call-vector? value env)))))

(defn- eval-body
  [forms env]
  (reduce (fn [_ form] (eval-template-form form env)) nil forms))

(defn- bind-params
  [env params args]
  (reduce-kv (fn [acc idx param]
               (assoc acc param (nth args idx nil)))
             env
             (vec params)))

(defn- make-fn
  [params body env]
  (fn [& args]
    (eval-body body (bind-params env params args))))

(defn- truthy?
  [value]
  (not (or (nil? value) (false? value))))

(defn- render-segments
  [value]
  (cond
    (nil? value) []
    (string? value) [value]
    (sequential? value) (mapcat render-segments value)
    :else [(str value)]))

(defn- template-separator
  [opts]
  (or (:separator opts)
      (:seperator opts)
      (get opts "separator")
      (get opts "seperator")
      " "))

(defn- render-template-call
  [args env]
  (let [[opts-form parts-form] (if (and (= 2 (count args))
                                        (map? (first args)))
                                 [(first args) (second args)]
                                 [{} (first args)])
        opts (eval-template-form opts-form env)
        separator (template-separator opts)
        parts (eval-template-form parts-form env)]
    (str/join separator (render-segments parts))))

(defn- eval-keyword-call
  [k args env]
  (let [evaluated (mapv #(eval-env-ref-or-form % env) args)
        last-value (last evaluated)
        leading (butlast evaluated)]
    (cond
      (and (seq leading)
           (every? keylike? leading)
           (or (map? last-value) (object? last-value)))
      (lookup-path last-value (cons k leading))

      (= 1 (count evaluated))
      (lookup-one (first evaluated) k nil)

      (= 2 (count evaluated))
      (lookup-one (first evaluated) k (second evaluated))

      :else nil)))

(defn- eval-map-call
  [args env]
  (let [evaluated (mapv #(eval-template-form % env) args)
        [f coll] (if (fn? (first evaluated))
                   [(first evaluated) (second evaluated)]
                   [(second evaluated) (first evaluated)])]
    (when (and (fn? f) (sequential? coll))
      (map f coll))))

(defn- eval-filter-call
  [args env]
  (let [evaluated (mapv #(eval-template-form % env) args)
        [pred coll] (if (fn? (first evaluated))
                      [(first evaluated) (second evaluated)]
                      [(second evaluated) (first evaluated)])]
    (when (and (fn? pred) (sequential? coll))
      (filter pred coll))))

(defn- eval-keep-call
  [args env]
  (let [evaluated (mapv #(eval-template-form % env) args)
        [f coll] (if (fn? (first evaluated))
                   [(first evaluated) (second evaluated)]
                   [(second evaluated) (first evaluated)])]
    (when (and (fn? f) (sequential? coll))
      (keep f coll))))

(defn- eval-let-call
  [args env]
  (let [bindings (first args)
        body (rest args)
        env* (loop [pairs (partition 2 bindings)
                    acc env]
               (if-let [[sym form] (first pairs)]
                 (recur (rest pairs) (assoc acc sym (eval-template-form form acc)))
                 acc))]
    (eval-body body env*)))

(defn- eval-list-call
  [form env]
  (let [op (first form)
        args (rest form)
        op-name (symbol-name op)]
    (cond
      (or (keyword? op)
          (and (string? op)
               (not (contains? known-template-op-names op-name))
               (seq args)
               (env-ref? env (last args))))
      (eval-keyword-call op args env)

      (= "template" op-name) (render-template-call args env)
      (= "quote" op-name) (first args)
      (= "do" op-name) (eval-body args env)
      (= "let" op-name) (eval-let-call args env)
      (= "if" op-name) (if (truthy? (eval-template-form (first args) env))
                          (eval-template-form (second args) env)
                          (eval-template-form (nth args 2 nil) env))
      (= "when" op-name) (when (truthy? (eval-template-form (first args) env))
                            (eval-body (rest args) env))
      (or (= "fn" op-name) (= "fn*" op-name)) (make-fn (first args) (rest args) env)
      (= "str" op-name) (apply str (map #(eval-template-form % env) args))
      (= "pr-str" op-name) (pr-str (eval-template-form (first args) env))
      (= "name" op-name) (some-> (eval-template-form (first args) env) name)
      (= "count" op-name) (count (eval-template-form (first args) env))
      (= "first" op-name) (first (eval-template-form (first args) env))
      (= "second" op-name) (second (eval-template-form (first args) env))
      (= "last" op-name) (last (eval-template-form (first args) env))
      (= "rest" op-name) (rest (eval-template-form (first args) env))
      (= "vec" op-name) (vec (eval-template-form (first args) env))
      (= "distinct" op-name) (distinct (eval-template-form (first args) env))
      (= "sort" op-name) (sort (eval-template-form (first args) env))
      (= "keys" op-name) (keys (eval-template-form (first args) env))
      (= "vals" op-name) (vals (eval-template-form (first args) env))
      (= "get" op-name) (lookup-one (eval-template-form (first args) env)
                                     (eval-template-form (second args) env)
                                     (eval-template-form (nth args 2 nil) env))
      (= "get-in" op-name) (lookup-path (eval-template-form (first args) env)
                                         (eval-template-form (second args) env))
      (= "map" op-name) (eval-map-call args env)
      (= "filter" op-name) (eval-filter-call args env)
      (= "keep" op-name) (eval-keep-call args env)
      (= "join" op-name) (str/join (eval-template-form (first args) env)
                                    (eval-template-form (second args) env))
      (= "not" op-name) (not (truthy? (eval-template-form (first args) env)))
      (= "and" op-name) (loop [remaining args
                                result true]
                           (if (seq remaining)
                             (let [next-result (eval-template-form (first remaining) env)]
                               (if (truthy? next-result)
                                 (recur (rest remaining) next-result)
                                 next-result))
                             result))
      (= "or" op-name) (some (fn [arg]
                                (let [result (eval-template-form arg env)]
                                  (when (truthy? result) result)))
                              args)
      (= "=" op-name) (apply = (map #(eval-template-form % env) args))
      :else (throw (js/Error. (str "Unsupported contract template form: " op-name))))))

(defn eval-template-form
  "Evaluate a trusted contract template form against env."
  [form env]
  (cond
    (template-form? form) (eval-list-call form env)
    (executable-vector-form? form env) (eval-list-call form env)
    (symbol? form) (get env form)
    (keyword? form) form
    (vector? form) (mapv #(eval-template-form % env) form)
    (map? form) (into {} (map (fn [[k v]] [k (eval-template-form v env)]) form))
    (seq? form) (eval-list-call form env)
    :else form))

(defn- flatten-template-values
  ([value]
   (flatten-template-values nil value))
  ([prefix value]
   (cond
     (map? value)
     (into {}
           (mapcat (fn [[k v]]
                     (let [key-name (cond
                                      (keyword? k) (name k)
                                      (string? k) k
                                      :else (str k))
                           next-prefix (if prefix (str prefix "." key-name) key-name)]
                       (flatten-template-values next-prefix v))))
           value)

     (sequential? value)
     (when prefix
       {prefix (str/join ", " (map str value))})

     (some? value)
     (when prefix {prefix value})

     :else {})))

(defn- auth-context-template-values
  [auth-context]
  (let [user (:user auth-context)
        org (:org auth-context)
        membership (:membership auth-context)
        email (or (:email auth-context)
                  (:user-email auth-context)
                  (:userEmail auth-context)
                  (:email user)
                  (:user-email user)
                  (:userEmail user))
        display-name (or (:name auth-context)
                         (:display-name auth-context)
                         (:displayName auth-context)
                         (:name user)
                         (:display-name user)
                         (:displayName user)
                         email)
        org-slug (or (:org-slug auth-context)
                     (:orgSlug auth-context)
                     (:slug org)
                     (:org-slug org)
                     (:orgSlug org))]
    {"name" display-name
     "email" email
     "user.email" email
     "user.name" display-name
     "org.slug" org-slug
     "membership.id" (or (:membership-id auth-context)
                          (:membershipId auth-context)
                          (:id membership))}))

(defn contract-template-context
  "Build the ctx map visible to contract forms."
  [agent-spec auth-context template-context]
  (let [agent-spec (or agent-spec {})]
    (merge {:agent agent-spec
            :auth auth-context
            :actor-id (:actor-id agent-spec)
            :actorId (:actor-id agent-spec)
            :contract-id (:contract-id agent-spec)
            :contractId (:contract-id agent-spec)
            :role (:role agent-spec)
            :model (:model agent-spec)}
           (or template-context {}))))

(defn render-legacy-placeholders
  [template auth-context template-context]
  (let [values (merge (auth-context-template-values auth-context)
                      (flatten-template-values template-context))]
    (str/replace template #"\{ctx\.([A-Za-z0-9_.-]+)\}"
                 (fn [match]
                   (let [parts (if (vector? match) match [match])
                         full (first parts)
                         key (second parts)]
                     (or (some-> (get values key) str)
                         full))))))

(defn render-prompt
  "Render a prompt value. Strings use legacy {ctx.foo}; list forms use the
   trusted contract template evaluator."
  ([prompt agent-spec auth-context]
   (render-prompt prompt agent-spec auth-context nil))
  ([prompt agent-spec auth-context template-context]
   (let [ctx (contract-template-context agent-spec auth-context template-context)]
     (cond
       (string? prompt) (render-legacy-placeholders prompt auth-context ctx)
       (template-form? prompt) (str (eval-template-form prompt {'ctx ctx}))
       (seq? prompt) (str (eval-template-form prompt {'ctx ctx}))
       (some? prompt) (str prompt)
       :else nil))))

(defn prompt-value
  "Return a stored prompt value when it is non-empty. Forms are preserved."
  [value]
  (cond
    (string? value) (some-> value str/trim not-empty)
    (some? value) value
    :else nil))

(defn prompt-preview
  [value]
  (cond
    (string? value) (some-> value str/trim not-empty)
    (template-form? value) "(template …)"
    (some? value) (pr-str value)
    :else nil))

(defn render-agent-prompts
  "Render :system-prompt/:task-prompt (and camel/snake aliases) in agent-spec."
  ([agent-spec auth-context]
   (render-agent-prompts agent-spec auth-context nil))
  ([agent-spec auth-context template-context]
   (let [agent-spec (or agent-spec {})
         raw-system (or (:system-prompt agent-spec)
                        (:systemPrompt agent-spec)
                        (:system_prompt agent-spec))
         raw-task (or (:task-prompt agent-spec)
                      (:taskPrompt agent-spec)
                      (:task_prompt agent-spec))
         system (prompt-value (render-prompt raw-system agent-spec auth-context template-context))
         task (prompt-value (render-prompt raw-task agent-spec auth-context template-context))]
     (cond-> agent-spec
       system (assoc :system-prompt system :systemPrompt system)
       task (assoc :task-prompt task :taskPrompt task)))))

(defn discord-message-template-context
  [payload event]
  {:user-name (or (:authorUsername payload) (:username payload) (:authorName payload))
   :user-id (or (:authorId payload) (:userId payload))
   :guild (or (:guildName payload) (:guildId payload))
   :guild-id (:guildId payload)
   :channel (or (:channelName payload) (:channelId payload))
   :channel-id (:channelId payload)
   :timestamp (:timestamp event)
   :message-id (:messageId payload)
   :text (or (:content payload) (:text payload) (:summary payload) (:payloadPreview payload) "")})

(defn event-template-context
  "Context shape used by event/jobs/hooks.  :source/:messages matches the DSL
   example while :event and :payload preserve the raw event surface."
  ([job event]
   (event-template-context job event nil))
  ([job event summary]
   (let [payload (or (:payload event) {})
         messages (vec (or (:messages payload)
                           (:source/messages payload)
                           (when (or (:content payload) (:summary payload) (:payloadPreview payload))
                             [(discord-message-template-context payload event)])))]
     {:job job
      :event event
      :payload payload
      :summary summary
      :source (merge (or (:source job) {})
                     {:kind (:sourceKind event)
                      :event-kind (:eventKind event)
                      :event event
                      :payload payload
                      :messages messages})})))

;; =============================================================================
;; Compatibility job helpers
;; =============================================================================
;; Model profiles remain as small config aliases. Hard-coded persona templates do
;; not live here; agent behavior should come from filesystem contracts.

(def model-profiles
  {:local-fast     {:model "gemma4:e4b" :thinking-level "off"}
   :local-mid      {:model "gemma4:31b" :thinking-level "off"}
   :local-heavy    {:model "gemma4:31b" :thinking-level "minimal"}
   :cloud-heavy    {:model "glm-5" :thinking-level "high"}
   :cloud-fast     {:model "glm-5-fast" :thinking-level "off"}
   :cloud-balanced {:model "glm-5" :thinking-level "minimal"}})

(def templates
  "Deprecated compatibility registry. Contract prompts are the source of truth."
  {})

(defn resolve-model-profile [profile-id]
  (or (get model-profiles profile-id)
      {:model (defaults/default-model (runtime-config/cfg)) :thinking-level "off"}))

(defn all-model-profiles []
  (vec (keys model-profiles)))

(defn get-template [template-id]
  (get templates template-id))

(defn all-templates []
  (vec (keys templates)))

(defn resolve-template-spec
  "Resolve a legacy template id into an agent spec. Prefer contract ids instead."
  [template-id overrides]
  (let [template (get-template template-id)]
    (if-not template
      (throw (js/Error. (str "Unknown legacy agent template: " template-id
                             ". Agent behavior now belongs in contracts.")))
      (let [model-cfg (resolve-model-profile (:model-profile template))]
        (-> template
            (merge model-cfg)
            (merge overrides)
            (assoc :thinking-level (or (:thinking-level overrides)
                                       (:thinking-level template)
                                       (:thinking-level model-cfg)
                                       "off")))))))

(defn default-tool-policies []
  [{:toolId "discord.read" :effect "allow"}
   {:toolId "discord.channel.messages" :effect "allow"}
   {:toolId "discord.channel.scroll" :effect "allow"}
   {:toolId "discord.dm.messages" :effect "allow"}
   {:toolId "discord.search" :effect "allow"}
   {:toolId "discord.publish" :effect "allow"}
   {:toolId "discord.send" :effect "allow"}
   {:toolId "websearch" :effect "allow"}
   {:toolId "memory_search" :effect "allow"}
   {:toolId "graph_query" :effect "allow"}])

(defn instantiate-job
  "Create a concrete event-agent job from a legacy template. Prefer agent
   contracts with :trigger-kind/:source-kind instead."
  [template-id job-id trigger source filters & [overrides]]
  (let [agent-spec (resolve-template-spec template-id overrides)]
    {:id job-id
     :name (or (:name overrides) job-id)
     :enabled true
     :trigger trigger
     :source source
     :filters filters
     :agentSpec agent-spec
     :description (or (:description overrides)
                      (str "Instance of " (name template-id) " template"))
     :templateId (name template-id)}))

(defn normalize-job-for-persistence
  "Ensure a job spec has all required fields for durable storage."
  [job]
  (let [now (.toISOString (js/Date.))]
    (-> job
        (assoc :createdAt (or (:createdAt job) now))
        (assoc :updatedAt now)
        (assoc-in [:agentSpec :thinkingLevel]
                  (or (get-in job [:agentSpec :thinkingLevel])
                      (get-in job [:agentSpec :thinking-level])
                      "off"))
        (update :agentSpec #(-> %
                                (dissoc :thinking-level)
                                (assoc :thinkingLevel (or (:thinkingLevel %)
                                                          (:thinking-level %)
                                                          "off")))))))
