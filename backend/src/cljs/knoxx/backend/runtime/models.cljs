(ns knoxx.backend.runtime.models
  (:require [clojure.string :as str]))

(def ^:private default-model-prefix-allowlist
  ["glm-5" "gpt-5" "qwen3" "gemma4:" "gemma3:" "deepseek" "kimi-k2" "nemotron" "cogito" "devstral" "minimax" "ministral" "mistral-large"])

(defn- parse-prefix-allowlist
  [raw]
  (-> (str (or raw ""))
      (str/split #",")
      (->> (map (fn [v] (some-> v str str/trim not-empty)))
           (remove nil?)
           vec)))

(defn allowlisted-model-id?
  "Returns true if model-id should be visible/selectable in Knoxx.

   Allowlist is configured via env var KNOXX_MODEL_PREFIX_ALLOWLIST.
   This is a simple prefix match (not a glob/regex engine)."
  [config model-id]
  (let [prefixes (let [configured (seq (:model-prefix-allowlist config))]
                   (or configured default-model-prefix-allowlist))
        id (str (or model-id ""))]
    (boolean
     (some (fn [prefix]
             (str/starts-with? id (str prefix)))
           prefixes))))

(def ^:private thinking-levels
  #{"off" "minimal" "low" "medium" "high" "xhigh"})

(defn normalize-thinking-level
  [value]
  (let [normalized (some-> value str str/trim str/lower-case not-empty)]
    (when (contains? thinking-levels normalized)
      normalized)))

(defn model-supports-reasoning?
  [config model-id]
  (let [normalized-model (some-> model-id str str/trim str/lower-case)
        prefixes (->> (str/split (or (:reasoning-model-prefixes config) "") #",")
                      (map str/trim)
                      (remove str/blank?))]
    (boolean
     (and normalized-model
          (some (fn [prefix]
                  (let [normalized-prefix (-> prefix
                                              str/lower-case
                                              (str/replace #"\\*$" ""))]
                    (str/starts-with? normalized-model normalized-prefix)))
                prefixes)))))

(defn model-thinking-format
  [model-id]
  (let [normalized-model (some-> model-id str str/trim str/lower-case)]
    (cond
      (and normalized-model (str/starts-with? normalized-model "glm-")) "zai"
      :else nil)))

(defn tool-cost
  []
  {:input 0 :output 0 :cacheRead 0 :cacheWrite 0})

(defn provider-model-config
  [config model-id]
  {:id model-id
   :name model-id
   :reasoning (model-supports-reasoning? config model-id)
   :input ["text"]
   :contextWindow 128000
   :maxTokens 8192
   :cost (tool-cost)})

(defn proxx-openai-base-url
  [config]
  (let [base (or (:proxx-base-url config) "")]
    (cond
      (str/blank? base) "http://localhost:8789/v1"
      (str/ends-with? base "/v1") base
      (str/ends-with? base "/") (str base "v1")
      :else (str base "/v1"))))

(defn per-model-compat
  "Compute per-model compat so reasoning/thinking settings aren't
   incorrectly shared across models that don't support them."
  [config model-id]
  (cond-> {:supportsDeveloperRole false}
    (model-supports-reasoning? config model-id)
    (assoc :supportsReasoningEffort true)
    (some? (model-thinking-format model-id))
    (assoc :thinkingFormat (model-thinking-format model-id))))

(defn models-config
  ([config]
   (models-config config nil))
  ([config model-ids]
   (let [default-model (:proxx-default-model config)
         normalized-models (->> (or model-ids [])
                                (map (fn [m] (some-> m str str/trim not-empty)))
                                (remove nil?)
                                distinct
                                vec)
         models (if (seq normalized-models)
                  normalized-models
                  [default-model])
         base-compat {:supportsDeveloperRole false}]
     {:providers
      {:proxx
       {:baseUrl (proxx-openai-base-url config)
        :apiKey "PROXX_AUTH_TOKEN"
        :authHeader true
        :api "openai-completions"
        :compat base-compat
        :models (mapv (fn [model-id]
                        (merge (provider-model-config config model-id)
                               {:compat (per-model-compat config model-id)}))
                      models)}}})))

(defn enrich-config
  "Augment an env-only config map with derived model config fields.

   Keeps knoxx.backend.runtime.config strictly env-only, while ensuring legacy
   call sites continue to find these keys." 
  [config]
  (merge
   {:model-prefix-allowlist
    (parse-prefix-allowlist
     (or (:model-prefix-allowlist config)
         (aget js/process.env "KNOXX_MODEL_PREFIX_ALLOWLIST")
         "glm-5,gpt-5,qwen3,gemma4:,gemma3:,deepseek,kimi-k2,nemotron,cogito,devstral,minimax,ministral,mistral-large"))

    :agent-thinking-level
    (or (normalize-thinking-level
         (or (:agent-thinking-level config)
             (aget js/process.env "KNOXX_THINKING_LEVEL")
             "off"))
        "off")

    :reasoning-model-prefixes
    (or (:reasoning-model-prefixes config)
        (aget js/process.env "KNOXX_REASONING_MODEL_PREFIXES")
        "glm-")}
   config))
