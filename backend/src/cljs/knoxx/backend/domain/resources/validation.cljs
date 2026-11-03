(ns knoxx.backend.domain.resources.validation
  "Validate resource EDN and report actionable configuration diagnostics."
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.domain.resources.editing :as editing]
            [knoxx.backend.law.contracts :as validator]))

(defn- validation-warning
  [path message]
  {:path path
   :message message
   :severity "warn"})

(def ^:private mutable-agent-data-keys
  #{:world_state :world-state
    :plot_log :plot-log
    :composition_log :composition-log
    :last_tick_timestamp :last-tick-timestamp
    :composition_count :composition-count})

(defn- positive-int [value]
  (when-let [prefix (re-find #"^[+-]?\d+" (str/trim (str value)))]
    (let [number (parse-long prefix)]
      (when (pos? number) number))))

(defn- role-ref-warnings
  [path value]
  (let [warn-bare (validation-warning path "Role refs should use :role/<kebab-slug>; bare or snake_case role refs are tolerated but cause drift.")
        warn-snake (validation-warning path "Role refs should use kebab-case, e.g. :role/contract-librarian, not underscores.")]
    (cond
      (keyword? value)
      (cond-> []
        (not= "role" (namespace value)) (conj warn-bare)
        (str/includes? (name value) "_") (conj warn-snake))

      (string? value)
      (cond-> []
        (str/includes? value "_") (conj warn-snake))

      :else [])))

(defn- prompt-state-path-warnings
  [resource]
  (let [prompts [(get-in resource [:prompts :system])
                 (get-in resource [:prompts :task])]
        stale-ref? (some (fn [prompt]
                           (and (string? prompt)
                                (re-find #"(:data/|/world_state|/plot_log|:data/world_state|:data/plot_log)" prompt)))
                         prompts)]
    (if stale-ref?
      [(validation-warning ["prompts"] "Prompt references mutable :data paths (for example :data/world_state or :data/plot_log). Agent resource :data is static config; use a real state store or durable files instead.")]
      [])))

(defn- agent-configuration-warnings
  [resource data max-messages source-mode channels publish-channels]
  (cond-> []
        (contains? data :filter)
        (conj (validation-warning ["data" "filter"] "Runtime ignores :data/:filter. Use :data/:filters."))

        (contains? resource :source)
        (conj (validation-warning ["source"] "Event-agent runtime ignores top-level :source. Use :data {:source ...}."))

        (contains? resource :capabilities)
        (conj (validation-warning ["capabilities"] "Top-level :capabilities is legacy/inert in resource resolution. Put capability refs under :actor {:capabilities [...]}, or grant them through roles."))

        (and max-messages (> max-messages 100))
        (conj (validation-warning ["data" "source" "max-messages"] "Event-agent source max-messages is clamped to 100 at runtime."))

        (and (keyword? source-mode)
             (not= "source-mode" (namespace source-mode))
             (contains? #{:synthesize :template-synthesize} source-mode))
        (conj (validation-warning ["source-mode"] "Use :source-mode/discord-synthesis instead of opaque bare synthesis modes."))

        (and (= :source-mode/discord-synthesis source-mode)
             (empty? channels)
             (seq publish-channels))
        (conj (validation-warning ["data" "filters" "publishChannels"] ":publishChannels are output sinks only. Add explicit :channels or :guildIds for Discord source reads."))))

(defn- agent-resource-warnings
  [resource]
  (let [data (:data resource)
        source-config (get-in resource [:data :source])
        max-messages (positive-int (or (:max-messages source-config)
                                       (:maxMessages source-config)))
        role (get-in resource [:agent :role])
        roles (get-in resource [:agent :roles])
        source-mode (:source-mode resource)
        filters (get-in resource [:data :filters])
        channels (or (:channels filters) [])
        publish-channels (or (:publishChannels filters) (:publish_channels filters) [])]
    (vec
     (concat
      (agent-configuration-warnings resource data max-messages source-mode channels publish-channels)
      (mapcat (fn [k]
                (when (contains? data k)
                  [(validation-warning ["data" (name k)] "This looks like mutable runtime state inside a static resource. Prefer Mongo/OpenPlanner/durable files, not resource :data mutation.")]))
              mutable-agent-data-keys)
      (role-ref-warnings ["agent" "role"] role)
      (mapcat (fn [[idx value]]
                (role-ref-warnings ["agent" "roles" (str idx)] value))
              (map-indexed vector (or roles [])))
      (prompt-state-path-warnings resource)))))

(defn- resource-warnings
  [resource-class resource]
  (if (and (= (editing/normalize-resource-class resource-class) "agents")
           (map? resource))
    (agent-resource-warnings resource)
    []))

(defn validate-resource-edn
  "Parse and normalize EDN, apply the canonical resource contract, and report warnings."
  [resource-class edn-text]
  (let [trimmed (str/trim (str edn-text))]
    (if (str/blank? trimmed)
      {:ok false
       :contract nil
       :errors [{:path [] :message "EDN text is empty"}]
       :warnings []}
      (try
        (let [raw-resource (reader/read-string trimmed)
              resource (if (= (editing/normalize-resource-class resource-class) "agents")
                         (actor-scope/normalize-agent-contract raw-resource)
                         raw-resource)
              base (validator/validate resource-class resource)]
          {:ok (:ok base)
           :contract resource
           :errors (:errors base)
           :warnings (resource-warnings resource-class resource)})
        (catch :default err
          {:ok false
           :contract nil
           :errors [{:path [] :message (str "EDN parse error: " (ex-message err))}]
           :warnings []})))))

(defn validate-contract-edn
  "Compatibility alias for old contract route clients."
  [contract-class edn-text]
  (validate-resource-edn contract-class edn-text))
