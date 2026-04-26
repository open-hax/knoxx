(ns knoxx.backend.runtime.models
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.runtime.contract-loader :as contract-loader]
            [promesa.core :as p]
            ["node:fs/promises" :as fsp]
            ["node:path" :as path]))

(def ^:private default-model-prefix-allowlist
  ["glm-5" "gpt-5" "qwen3" "gemma4:" "gemma3:" "deepseek" "kimi-k2" "nemotron" "cogito" "devstral" "minimax" "ministral" "mistral-large"])

(def ^:private thinking-levels
  #{"off" "minimal" "low" "medium" "high" "xhigh"})

(def ^:private input-kinds
  #{"text" "image" "audio" "video" "document"})

(def ^:private pi-registry-input-kinds
  #{"text" "image"})

(defn- parse-prefix-allowlist
  [raw]
  (-> (str (or raw ""))
      (str/split #",")
      (->> (map (fn [v] (some-> v str str/trim not-empty)))
           (remove nil?)
           vec)))

(defn normalize-thinking-level
  [value]
  (let [normalized (some-> value str str/trim str/lower-case not-empty)]
    (when (contains? thinking-levels normalized)
      normalized)))

(defn normalize-input-kind
  [value]
  (let [normalized (cond
                     (keyword? value) (some-> value name str/trim str/lower-case not-empty)
                     :else (some-> value str str/trim str/lower-case not-empty))]
    (when (contains? input-kinds normalized)
      normalized)))

(defn- read-edn
  [file-path]
  (p/let [text (.readFile fsp file-path "utf8")]
         (some-> text str reader/read-string)))

(defn- read-contract-dir
  [dir]
  (p/let [entries (.readdir fsp dir)]
         (->> entries
              (filter (fn [name]
                        (and (string? name) (str/ends-with? name ".edn"))))
              (map (fn [name]
                     (p/let [contract (read-edn (.join path dir name))]
                            contract)))
              (remove nil?)
              vec)))

(defn- read-contract-dirs
  [dirs]
  (p/let [results (p/all (mapv read-contract-dir dirs))]
         (mapcat identity results)))

(defn- normalize-boolean
  [value]
  (cond
    (true? value) true
    (false? value) false
    :else nil))

(defn- normalize-string-seq
  [values]
  (->> (or values [])
       (map (fn [value]
              (cond
                (keyword? value) (some-> value name str/trim not-empty)
                :else (some-> value str str/trim not-empty))))
       (remove nil?)
       distinct
       vec))

(defn- normalize-thinking-level-seq
  [values]
  (->> (or values [])
       (map normalize-thinking-level)
       (remove nil?)
       distinct
       vec))

(defn- normalize-input-kind-seq
  [values]
  (->> (or values [])
       (map normalize-input-kind)
       (remove nil?)
       distinct
       vec))

(defn- normalize-model-family-contract
  [contract]
  (when-let [family-id (some-> (:model-family/id contract) str str/trim not-empty)]
    {:id family-id
     :provider (some-> (:model-family/provider contract) name)
     :prefixes (normalize-string-seq (:model-family/prefixes contract))
     :allowlisted (normalize-boolean (:model-family/allowlisted contract))
     :reasoning (normalize-boolean (:model-family/reasoning contract))
     :default-thinking (normalize-thinking-level (:model-family/default-thinking contract))
     :thinking-levels (normalize-thinking-level-seq (:model-family/thinking-levels contract))
     :context-window (when (number? (:model-family/context-window contract)) (:model-family/context-window contract))
     :max-tokens (when (number? (:model-family/max-tokens contract)) (:model-family/max-tokens contract))
     :input (normalize-input-kind-seq (:model-family/input contract))}))

(defn- normalize-model-contract
  [contract]
  (when-let [model-id (some-> (:model/id contract) str str/trim not-empty)]
    {:id model-id
     :family-id (some-> (:model-family/id contract) str str/trim not-empty)
     :provider (some-> (:model/provider contract) name)
     :default (normalize-boolean (:model/default contract))
     :allowlisted (normalize-boolean (:model/allowlisted contract))
     :reasoning (normalize-boolean (:model/reasoning contract))
     :default-thinking (normalize-thinking-level (:model/default-thinking contract))
     :thinking-levels (normalize-thinking-level-seq (:model/thinking-levels contract))
     :context-window (when (number? (:model/context-window contract)) (:model/context-window contract))
     :max-tokens (when (number? (:model/max-tokens contract)) (:model/max-tokens contract))
     :input (normalize-input-kind-seq (:model/input contract))
     :label (some-> (:model/label contract) str str/trim not-empty)}))

(defn model-family-contracts
  [config]
  (p/let [dirs (contract-loader/contract-class-dir-paths config "model_families")
          contracts (read-contract-dirs dirs)]
         (->> contracts
              (map normalize-model-family-contract)
              (remove nil?)
              (reverse)
              (reduce (fn [acc contract]
                        (assoc acc (:id contract) contract))
                      {})
              vals
              vec)))

(defn model-contracts
  [config]
  (p/let [dirs (contract-loader/contract-class-dir-paths config "models")
          contracts (read-contract-dirs dirs)]
         (->> contracts
              (map normalize-model-contract)
              (remove nil?)
              (reverse)
              (reduce (fn [acc contract]
                        (assoc acc (:id contract) contract))
                      {})
              vals
              vec)))

(defn resolve-model-family
  [config model-id]
  (p/let [id (some-> model-id str str/trim not-empty)]
    (when id
      (p/let [families (model-family-contracts config)]
        (->> families
             (filter (fn [family]
                       (some (fn [prefix]
                               (str/starts-with? id prefix))
                             (:prefixes family))))
             (sort-by (fn [family]
                        (- (apply max 0 (map count (:prefixes family))))))
             first)))))

(defn resolve-model-contract
  [config model-id]
  (p/let [id (some-> model-id str str/trim not-empty)]
    (when id
      (p/let [contracts (model-contracts config)
              exact (some (fn [contract]
                            (when (= id (:id contract))
                              contract))
                          contracts)
              families (model-family-contracts config)
              family (or (when-let [family-id (:family-id exact)]
                           (some (fn [contract]
                                   (when (= family-id (:id contract))
                                     contract))
                                 families))
                         (resolve-model-family config id))]
        (merge family exact)))))

(defn- fallback-prefix-allowlisted?
  [config model-id]
  (let [prefixes (let [configured (seq (:model-prefix-allowlist config))]
                   (or configured default-model-prefix-allowlist))
        id (str (or model-id ""))]
    (boolean
     (some (fn [prefix]
             (str/starts-with? id (str prefix)))
           prefixes))))

(defn allowlisted-model-id?
  "Returns true if model-id should be visible/selectable in Knoxx.

   Contract model overrides win. Fallback is env-configured prefix allowlist."
  [config model-id]
  (p/let [model-spec (resolve-model-contract config model-id)]
    (if (some? (:allowlisted model-spec))
      (boolean (:allowlisted model-spec))
      (fallback-prefix-allowlisted? config model-id))))

(defn model-supports-reasoning?
  [config model-id]
  (p/let [model-spec (resolve-model-contract config model-id)]
    (if (some? (:reasoning model-spec))
      (boolean (:reasoning model-spec))
      (let [normalized-model (some-> model-id str str/trim str/lower-case)
            prefixes (->> (str/split (or (:reasoning-model-prefixes config) "") #",")
                          (map str/trim)
                          (remove str/blank?))]
        (boolean
         (and normalized-model
              (some (fn [prefix]
                      (let [normalized-prefix (-> prefix
                                                  str/lower-case
                                                  (str/replace #"\*$" ""))]
                        (str/starts-with? normalized-model normalized-prefix)))
                    prefixes)))))))

(defn model-prefers-responses?
  [config model-id]
  (p/let [model-spec (resolve-model-contract config model-id)]
    (or (false? (:prefers-completions model-spec))
        (let [normalized-model (some-> model-id str str/trim str/lower-case)
              prefixes (->> (str/split (or (:responses-model-prefixes config) "") #",")
                            (map str/trim)
                            (remove str/blank?))]
          (boolean
           (and normalized-model
                (some (fn [prefix]
                        (str/starts-with? normalized-model prefix))
                      prefixes)))))))

(defn effective-thinking-level
  [config model-id requested-thinking-level]
  (p/let [requested (normalize-thinking-level requested-thinking-level)
          model-spec (resolve-model-contract config model-id)
          allowed-levels (let [contract-levels (seq (:thinking-levels model-spec))]
                           (cond
                             contract-levels (set contract-levels)
                             (false? (:reasoning model-spec)) #{"off"}
                             :else thinking-levels))
          default-level (or (:default-thinking model-spec)
                            (:agent-thinking-level config)
                            "off")]
    (if (and requested (contains? allowed-levels requested))
      requested
      default-level)))

(defn model-thinking-format
  [model-id]
  (let [normalized-model (some-> model-id str str/trim str/lower-case)]
    (cond
      (and normalized-model (str/starts-with? normalized-model "glm-")) "zai"
      :else nil)))

(defn model-input-modes
  [config model-id]
  (p/let [model-spec (resolve-model-contract config model-id)
          inputs (->> (or (:input model-spec) [])
                      (map normalize-input-kind)
                      (remove nil?)
                      distinct
                      vec)]
    (if (seq inputs)
      inputs
      ["text"])))

(defn model-supports-input?
  [config model-id input-kind]
  (p/let [modes (model-input-modes config model-id)
          wanted (or (normalize-input-kind input-kind) "text")]
    (boolean
     (some #(= wanted %) modes))))

(defn provider-model-config
  [config model-id]
  (p/let [model-spec (resolve-model-contract config model-id)
          reasoning? (model-supports-reasoning? config model-id)
          prefers-responses (model-prefers-responses? config model-id)
          api (if prefers-responses
                "openai-responses"
                "openai-completions")
        ;; pi-coding-agent 0.63.x model registry only accepts text/image input kinds.
        ;; Keep Knoxx's richer contract input metadata for request validation, but
        ;; down-project provider config so models.json stays loadable.
        registry-inputs (->> (model-input-modes config model-id)
                             (filter pi-registry-input-kinds)
                             distinct
                             vec)]
    {:id model-id
     :name (or (:label model-spec) model-id)
     :api api
     :reasoning reasoning?
     :input (if (seq registry-inputs) registry-inputs ["text"])
     :contextWindow (or (:context-window model-spec) 128000)
     :maxTokens (or (:max-tokens model-spec) 8192)
     :cost (tool-cost)}))

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
  (p/let [supports-reasoning (model-supports-reasoning? config model-id)
          thinking-fmt (model-thinking-format model-id)]
    (cond-> {:supportsDeveloperRole false}
      supports-reasoning
      (assoc :supportsReasoningEffort true)
      thinking-fmt
      (assoc :thinkingFormat thinking-fmt))))

(defn models-config
  ([config]
   (models-config config nil))
  ([config model-ids]
   (p/let [default-model (:proxx-default-model config)
           normalized-models (->> (or model-ids [])
                                  (map (fn [m] (some-> m str str/trim not-empty)))
                                  (remove nil?)
                                  distinct
                                  vec)
           models (if (seq normalized-models)
                    normalized-models
                    [default-model])
           base-compat {:supportsDeveloperRole false}
           model-configs (p/all (mapv (fn [model-id]
                                        (p/let [cfg (provider-model-config config model-id)
                                                compat (per-model-compat config model-id)]
                                               (merge cfg {:compat compat})))
                                      models))]
     {:providers
      {:proxx
       {:baseUrl (proxx-openai-base-url config)
        :apiKey "PROXX_AUTH_TOKEN"
        :authHeader true
        :api "openai-completions"
        :compat base-compat
        :models model-configs}}})))

(defn- default-model-from-contracts
  [config]
  (p/let [contracts (model-contracts config)]
    (or (some (fn [contract]
                (when (:default contract)
                  (:id contract)))
              contracts)
        (some :id contracts))))

(defn enrich-config
  "Augment an env-only config map with derived model config fields.

   Keeps knoxx.backend.runtime.config strictly env-only, while ensuring legacy
   call sites continue to find these keys."
  [config]
  (p/let [default-model (default-model-from-contracts config)]
    (merge
     {:model-prefix-allowlist
      (parse-prefix-allowlist
       (or (:model-prefix-allowlist config)
           (aget js/process.env "KNOXX_MODEL_PREFIX_ALLOWLIST")
           "glm-5,gpt-5,qwen3,gemma4:,gemma3:,deepseek,kimi-k2,nemotron,cogito,devstral,minimax,ministral,mistral-large"))

      :proxx-default-model
      (or (:proxx-default-model config)
          default-model
          "glm-5")

      :agent-thinking-level
      (or (normalize-thinking-level
           (or (:agent-thinking-level config)
               (aget js/process.env "KNOXX_THINKING_LEVEL")
               "off"))
          "off")

      :reasoning-model-prefixes
      (or (:reasoning-model-prefixes config)
          (aget js/process.env "KNOXX_REASONING_MODEL_PREFIXES")
          "glm-")

      :responses-model-prefixes
      (or (:responses-model-prefixes config)
          (aget js/process.env "KNOXX_RESPONSES_MODEL_PREFIXES")
          "gpt-")}
     config)))
