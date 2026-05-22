(ns knoxx.backend.domain.control.catalog
  "Resource runtime catalog for the event/trigger/action/generator model.

   This namespace does not normalize legacy agent jobs. It projects loaded
   resource records into flat tables and reports boundary-contract violations so
   callers can stop treating agents, triggers, schedules, actions, and
   generators as one blended runtime shape."
  (:require [clojure.string :as str]))

(def class->kind
  {"actions" :action
   "actors" :actor
   "agents" :agent
   "capabilities" :capability
   "generators" :generator
   "pipelines" :pipeline
   "roles" :role
   "schedules" :schedule
   "triggers" :trigger
   "users" :user
   "workflows" :workflow})

(def catalog-resource-kinds
  [:agent :actor :action :trigger :schedule :generator])

(def agent-runtime-keys
  #{:trigger-kind
    :source-kind
    :source-mode
    :trigger/kind
    :trigger/events
    :trigger/action
    :trigger/agent
    :trigger/source
    :trigger/schedule
    :events
    :sources})

(def trigger-schedule-keys
  #{:trigger/schedule
    :schedule
    :schedule/rule})

(def trigger-source-keys
  #{:trigger/source
    :source-kind
    :source-mode})

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn- resource-class
  [record]
  (or (:resource/class record)
      (:contractClass record)))

(defn- resource-kind
  [record]
  (or (:resource/kind record)
      (get class->kind (resource-class record))))

(defn- resource-id
  [record]
  (or (:resource/id record)
      (:id record)))

(defn- resource-definition
  [record]
  (or (:resource/definition record)
      (:contract record)))

(defn- resource-row
  [record]
  (let [id (resource-id record)
        kind (resource-kind record)]
    {:id id
     :resource/id id
     :resource/kind kind
     :class (resource-class record)
     :resource (resource-definition record)}))

(defn- resources-of-kind
  [records kind]
  (->> records
       (filter #(= kind (resource-kind %)))
       (mapv resource-row)))

(defn- resource-ids-of-kind
  [records kind]
  (->> (resources-of-kind records kind)
       (mapv :id)))

(defn- forbidden-present
  [resource forbidden-keys]
  (->> forbidden-keys
       (filter #(contains? resource %))
       sort
       vec))

(defn- violation
  [record kind severity message data]
  {:resource/id (resource-id record)
   :resource/kind (resource-kind record)
   :violation/kind kind
   :severity severity
   :message message
   :data data})

(defn- agent-violations
  [record]
  (let [resource (resource-definition record)
        forbidden (forbidden-present resource agent-runtime-keys)]
    (when (seq forbidden)
      [(violation record
                  :agent/contains-runtime-agreement
                  :block
                  "Agent resources must not define triggers, schedules, generators, or runtime sources. Agents define prompting, roles, capabilities, ownership, and policy only."
                  {:keys forbidden})])))

(defn- missing-trigger-field-violations
  [record]
  (let [resource (resource-definition record)]
    (cond-> []
      (not (:trigger/action resource))
      (conj (violation record
                       :trigger/missing-action
                       :block
                       "Trigger resources must reference an action resource or registered action key."
                       {}))

      (empty? (:trigger/events resource))
      (conj (violation record
                       :trigger/missing-events
                       :block
                       "Trigger resources must declare observed event types; schedules generate events separately."
                       {})))))

(defn- misplaced-trigger-field-violations
  [record]
  (let [resource (resource-definition record)
        schedule-keys (forbidden-present resource trigger-schedule-keys)
        source-keys (forbidden-present resource trigger-source-keys)]
    (cond-> []
      (seq schedule-keys)
      (conj (violation record
                       :trigger/contains-schedule
                       :block
                       "Schedule rules belong in schedule resources. Triggers only agree to act after observing an event that meets a condition."
                       {:keys schedule-keys}))

      (seq source-keys)
      (conj (violation record
                       :trigger/contains-source
                       :block
                       "The source concept is retired at this boundary. Use generator resources for event producers and event provenance for emitted events."
                       {:keys source-keys})))))

(defn- trigger-violations
  [record]
  (vec (concat (missing-trigger-field-violations record)
               (misplaced-trigger-field-violations record))))

(defn- action-violations
  [record]
  (let [resource (resource-definition record)]
    (when-not (or (:action/kind resource)
                  (:action/handler resource))
      [(violation record
                  :action/missing-handler
                  :block
                  "Action resources must identify the registered behavior they expose."
                  {})])))

(defn- schedule-violations
  [record]
  (let [resource (resource-definition record)]
    (cond-> []
      (not (or (:schedule/rule resource)
               (:schedule/cron resource)
               (:schedule/at resource)))
      (conj (violation record
                       :schedule/missing-temporal-rule
                       :block
                       "Schedule resources must define a temporal rule for emitting a synthetic event."
                       {}))

      (not (:schedule/event resource))
      (conj (violation record
                       :schedule/missing-event
                       :block
                       "Schedule resources must define the synthetic event they will emit."
                       {})))))

(defn- generator-violations
  [record]
  (let [resource (resource-definition record)]
    (when-not (or (:generator/kind resource)
                  (:generator/driver resource)
                  (:generator/emits resource))
      [(violation record
                  :generator/missing-event-production-resource
                  :block
                  "Generator resources must declare how they produce events."
                  {})])))

(defn- legacy-source-violations
  [records class-name]
  (mapv (fn [record]
          (violation record
                     :legacy/source-resource
                     :block
                     "Runtime source/source-mode resources are legacy at the event runtime boundary; use generator resources instead."
                     {:class class-name}))
        (filter #(= class-name (resource-class %)) records)))

(defn violations
  [records]
  (vec
   (concat
    (mapcat agent-violations (filter #(= :agent (resource-kind %)) records))
    (mapcat trigger-violations (filter #(= :trigger (resource-kind %)) records))
    (mapcat action-violations (filter #(= :action (resource-kind %)) records))
    (mapcat schedule-violations (filter #(= :schedule (resource-kind %)) records))
    (mapcat generator-violations (filter #(= :generator (resource-kind %)) records))
    (legacy-source-violations records "sources")
    (legacy-source-violations records "source_modes"))))

(defn catalog
  [records]
  (let [rows (vec (or records []))
        current-violations (violations rows)]
    {:resources (->> catalog-resource-kinds
                     (map (fn [kind]
                            [kind (resources-of-kind rows kind)]))
                     (into {}))
     :catalog {:catalog/resources
               (->> catalog-resource-kinds
                    (map (fn [kind]
                           [kind (resource-ids-of-kind rows kind)]))
                    (into {}))}
     :runtime {:agents-are-executables true
               :triggers-are-event-action-agreements true
               :schedules-emit-synthetic-events true}
     :admissibility {:ok? (empty? current-violations)
                     :violations current-violations}}))

(defn generator-kind-options
  [records]
  (->> (resources-of-kind (vec (or records [])) :generator)
       (keep (fn [{:keys [resource]}]
               (nonblank (or (:generator/kind resource)
                             (:generator/driver resource)))))
       distinct
       sort
       vec))

(def trigger-kind-options
  ["event"])
