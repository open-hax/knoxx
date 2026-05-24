(ns knoxx.backend.infra.routes.resources
  "Resource route implementation. Registers resource-native admin routes and
   legacy /contracts compatibility routes while the UI migrates."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.domain.contracts.resolve :as contracts-resolve]
            [knoxx.backend.infra.event-runtime :as event-runtime]
            [knoxx.backend.infra.redis-client :as redis]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.law.contracts :as validator]
            ["node:fs" :as node-fs]
            ["node:fs/promises" :as fs]))

(def ^:private resources-index-key "resources:index")
(def ^:private resource-watch-debounce-ms 350)

(defonce ^:private resource-watchers* (atom []))
(defonce ^:private resource-watch-timer* (atom nil))
(defonce ^:private resource-watch-running?* (atom false))

(defn- normalize-resource-class
  [raw]
  (resources/resource-class raw))

(defn- normalize-contract-class
  "Compatibility alias for old contract route clients."
  [raw]
  (normalize-resource-class raw))

(defn- resource-id->index-key
  [resource-class resource-id]
  (str (normalize-resource-class resource-class) "/" resource-id))


(defn- model-id->slug
  [model-id]
  (some-> model-id
          str
          (str/replace #"[^A-Za-z0-9._-]+" "_")
          (str/replace #"_+" "_")))

(defn- parsed-resource-id
  [resource-class value]
  (case (normalize-resource-class resource-class)
    "agents" (some-> (:contract/id value) str)
    "policies" (some-> (:contract/id value) str)
    "sources" (some-> (:contract/id value) str)
    "actions" (some-> (:contract/id value) str)
    "pipelines" (some-> (:contract/id value) str)
    "triggers" (some-> (:contract/id value) str)
    "sub_agents" (some-> (:contract/id value) str)
    "actors" (some-> (:actor/id value) str)
    "roles" (some-> (:role/id value) name)
    "capabilities" (some-> (:cap/id value) name)
    "model_families" (some-> (:model-family/id value) str)
    "models" (some-> (:model/id value) model-id->slug)
    nil))

(defn- wire-key
  [key]
  (if (keyword? key)
    (if-let [key-ns (namespace key)]
      (str key-ns "/" (name key))
      (name key))
    key))

(defn- wire-value
  [value]
  (cond
    (keyword? value) (wire-key value)
    (symbol? value) (wire-key value)
    (map? value) (into {} (map (fn [[k v]] [(wire-key k) (wire-value v)])) value)
    (set? value) (mapv wire-value value)
    (sequential? value) (mapv wire-value value)
    :else value))

(defn- keywordish-name
  [value]
  (cond
    (keyword? value) (wire-key value)
    (symbol? value) (wire-key value)
    (some? value) (str value)
    :else nil))

(defn- record-definition
  [record]
  (or (:resource/definition record)
      (:contract record)))

(defn- record-class
  [record]
  (or (:resource/class record)
      (:contractClass record)))

(defn- record-kind
  [record]
  (or (:resource/kind record)
      (some-> (record-class record) resources/normalize-resource-kind)))

(defn- trigger-summary
  [record]
  (let [resource (record-definition record)]
    {:kind (keywordish-name (:trigger/kind resource))
     :target (:trigger/target resource)
     :events (wire-value (:trigger/events resource))
     :filters (wire-value (get-in resource [:data :filters]))
     :context (wire-value (get-in resource [:data :context]))}))

(defn- pipeline-summary
  [record]
  {:steps (mapv (fn [step]
                  {:id (:step/id step)
                   :resource (:step/contract step)})
                (get-in (record-definition record) [:pipeline/steps]))})

(defn- action-summary
  [record]
  {:handler (get-in (record-definition record) [:action/handler])})

(defn- resource-list-summary
  [record]
  (let [resource-class (record-class record)
        resource-kind (record-kind record)
        summary {:id (:resource/id record)
                 :resource/id (:resource/id record)
                 :resource/kind resource-kind
                 :resourceClass resource-class
                 :kind (some-> resource-kind name)
                 :path (str resource-class "/" (:resource/id record) ".edn")}]
    (cond-> summary
      (= :trigger resource-kind)
      (assoc :trigger (trigger-summary record))

      (= :pipeline resource-kind)
      (assoc :pipeline (pipeline-summary record))

      (= :action resource-kind)
      (assoc :action (action-summary record)))))

(defn- contract-list-summary
  "Compatibility summary for old /contracts clients."
  [record]
  (assoc (resource-list-summary record)
         :contractClass (record-class record)))

(defn- wire-validation
  [validation]
  (cond-> validation
    (:contract validation) (update :contract wire-value)))

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

(defn- positive-int
  [value]
  (let [n (js/parseInt value 10)]
    (when (and (number? n) (not (js/isNaN n)) (pos? n))
      n)))

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
        (conj (validation-warning ["data" "filters" "publishChannels"] ":publishChannels are output sinks only. Add explicit :channels or :guildIds for Discord source reads.")))
      (mapcat (fn [k]
                (when (contains? data k)
                  [(validation-warning ["data" (name k)] "This looks like mutable runtime state inside a static resource. Prefer Redis/OpenPlanner/durable files, not resource :data mutation.")]))
              mutable-agent-data-keys)
      (role-ref-warnings ["agent" "role"] role)
      (mapcat (fn [[idx value]]
                (role-ref-warnings ["agent" "roles" (str idx)] value))
              (map-indexed vector (or roles [])))
      (prompt-state-path-warnings resource)))))

(defn- resource-warnings
  [resource-class resource]
  (if (and (= (normalize-resource-class resource-class) "agents")
           (map? resource))
    (agent-resource-warnings resource)
    []))

(defn validate-resource-edn
  [resource-class edn-text]
  (let [trimmed (str/trim (str edn-text))]
    (if (str/blank? trimmed)
      {:ok false
       :contract nil
       :errors [{:path [] :message "EDN text is empty"}]
       :warnings []}
      (try
        (let [raw-resource (reader/read-string trimmed)
              resource (if (= (normalize-resource-class resource-class) "agents")
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
           :errors [{:path [] :message (str "EDN parse error: " (.-message err))}]
           :warnings []})))))

(defn validate-contract-edn
  "Compatibility alias for old contract route clients."
  [contract-class edn-text]
  (validate-resource-edn contract-class edn-text))

(defn- safe-resource-id
  [raw-id]
  (try
    {:ok true
     :id (resources/safe-resource-id! raw-id)}
    (catch :default err
      {:ok false
       :error (or (.-message err) (str err))})))

(defn- safe-contract-id
  "Compatibility alias for old contract route clients."
  [raw-id]
  (safe-resource-id raw-id))

(defn- safe-resource-class
  [raw-class]
  (try
    {:ok true
     :class (normalize-resource-class raw-class)}
    (catch :default err
      {:ok false
       :error (or (.-message err) (str err))})))

(defn- safe-contract-class
  "Compatibility alias for old contract route clients."
  [raw-class]
  (safe-resource-class raw-class))

(defn- update-resource-id-in-edn-text
  [resource-class edn-text new-id]
  (case (normalize-resource-class resource-class)
    "agents"
    (if (str/includes? edn-text ":contract/id")
      (str/replace edn-text #":contract/id\s+\"[^\"]+\"" (str ":contract/id \"" new-id "\""))
      (str ":contract/id \"" new-id "\"\n" edn-text))

    "policies"
    (if (str/includes? edn-text ":contract/id")
      (str/replace edn-text #":contract/id\s+\"[^\"]+\"" (str ":contract/id \"" new-id "\""))
      (str ":contract/id \"" new-id "\"\n" edn-text))

    "sources"
    (let [source-id (str ":source/" (str/replace (str new-id) #"_" "-"))]
      (-> (if (str/includes? edn-text ":contract/id")
            (str/replace edn-text #":contract/id\s+\"[^\"]+\"" (str ":contract/id \"" new-id "\""))
            (str ":contract/id \"" new-id "\"\n" edn-text))
          (cond-> (str/includes? edn-text ":source/id")
            (str/replace #":source/id\s+:[^\s\]}]+" (str ":source/id " source-id)))))

    "actors"
    (if (str/includes? edn-text ":actor/id")
      (str/replace edn-text #":actor/id\s+\"[^\"]+\"" (str ":actor/id \"" new-id "\""))
      (str ":actor/id \"" new-id "\"\n" edn-text))

    "roles"
    (let [keyword-id (str ":role/" (str/replace new-id #"_" "-"))]
      (if (str/includes? edn-text ":role/id")
        (str/replace edn-text #":role/id\s+:[^\s\]}]+" (str ":role/id " keyword-id))
        (str ":role/id " keyword-id "\n" edn-text)))

    "capabilities"
    (let [slug (str/replace (str new-id) #"^cap_" "")
          keyword-id (str ":cap/" (str/replace slug #"_" "-"))]
      (if (str/includes? edn-text ":cap/id")
        (str/replace edn-text #":cap/id\s+:[^\s\]}]+" (str ":cap/id " keyword-id))
        (str ":cap/id " keyword-id "\n" edn-text)))

    "model_families"
    (if (str/includes? edn-text ":model-family/id")
      (str/replace edn-text #":model-family/id\s+\"[^\"]+\"" (str ":model-family/id \"" new-id "\""))
      (str ":model-family/id \"" new-id "\"\n" edn-text))

    "models"
    (let [model-id (str/replace (str new-id) #"_" ":")]
      (if (str/includes? edn-text ":model/id")
        (str/replace edn-text #":model/id\s+\"[^\"]+\"" (str ":model/id \"" model-id "\""))
        (str ":model/id \"" model-id "\"\n" edn-text)))

    edn-text))

(defn sync-resource-index!
  "Sync resource EDN files → Redis resources:index set.

   Redis is a cache + fast index; disk is canonical. Invalid resource files are
   omitted by the loader and must not block backend startup or the repair UI."
  [config]
  (if-let [client (redis/get-client)]
    (-> (resources/load-all-resources! config)
        (.then (fn [records]
                 (let [ids (->> records
                                (map (fn [record]
                                       (resource-id->index-key (:resource/class record)
                                                               (:resource/id record))))
                                distinct
                                sort
                                vec)]
                   (-> (redis/smembers client resources-index-key)
                       (.then (fn [existing]
                                (let [existing-set (set (map str (js/Array.from (or existing (js/Array.)))))
                                      desired-set (set ids)
                                      to-add (vec (sort (set/difference desired-set existing-set)))
                                      to-remove (vec (sort (set/difference existing-set desired-set)))
                                      ops (concat
                                           (for [id to-add]
                                             (redis/sadd client resources-index-key id))
                                           (for [id to-remove]
                                             (redis/srem client resources-index-key id)))]
                                  (-> (js/Promise.all (clj->js ops))
                                      (.then (fn [_]
                                               (println "[resources] synced Redis index; add=" (count to-add) "remove=" (count to-remove))
                                               {:ok true
                                                :added to-add
                                                :removed to-remove
                                                :count (count ids)}))))))))))
        (.catch (fn [err]
                  (println "[resources] sync-resource-index! failed; startup continuing:" (.-message err))
                  {:ok false :error (.-message err)})))
    (js/Promise.resolve {:ok false :error "Redis not connected"})))

(defn sync-contract-index!
  "Compatibility alias for old contract route callers."
  [config]
  (sync-resource-index! config))

(defn- clear-resource-watch-timer!
  []
  (when-let [timer @resource-watch-timer*]
    (js/clearTimeout timer)
    (reset! resource-watch-timer* nil)))

(defn stop-resource-watcher!
  []
  (clear-resource-watch-timer!)
  (doseq [watcher @resource-watchers*]
    (when watcher
      (try
        (.close watcher)
        (catch :default _ nil))))
  (reset! resource-watchers* [])
  (reset! resource-watch-running?* false)
  nil)

(defn stop-contract-watcher!
  "Compatibility alias for old contract route callers."
  []
  (stop-resource-watcher!))

(defn- watchable-resource-change?
  [filename]
  (or (nil? filename)
      (str/ends-with? (str/lower-case (str filename)) ".edn")))

(defn- schedule-resource-refresh!
  [config reason]
  (clear-resource-watch-timer!)
  (reset! resource-watch-timer*
          (js/setTimeout
           (fn []
             (reset! resource-watch-timer* nil)
             (println "[resources] watcher refresh triggered by" reason)
             (-> (sync-resource-index! config)
                 (.catch (fn [err]
                           (println "[resources] watcher sync failed:" (.-message err))
                           nil))
                  (.then (fn [_]
                            (event-runtime/debounced-reload!)
                            (println "[resources] event runtime reload queued after resource change")
                           nil))
                 (.catch (fn [err]
                           (println "[resources] watcher reload failed:" (.-message err))
                           nil))))
           resource-watch-debounce-ms)))

(defn start-resource-watcher!
  [config]
  (when-not @resource-watch-running?*
    (let [roots (->> (resources/resource-root-paths config)
                     (filter #(.existsSync node-fs %))
                     distinct
                     vec)
          watch-root (fn [root]
                       (try
                         (.watch node-fs
                                 root
                                 (clj->js {:recursive true})
                                 (fn [event-type filename]
                                   (let [filename-str (some-> filename str)]
                                     (when (watchable-resource-change? filename-str)
                                       (schedule-resource-refresh! config
                                                                  (str root " :: " event-type " :: " (or filename-str "<unknown>")))))))
                         (catch :default err
                           (println "[resources] failed to watch" root ":" (.-message err))
                           nil)))
          watchers (->> roots
                        (map watch-root)
                        (remove nil?)
                        vec)]
      (when (seq watchers)
        (reset! resource-watchers* watchers)
        (reset! resource-watch-running?* true)
        (println "[resources] watching" (count watchers) "resource roots for live reload")))))

(defn start-contract-watcher!
  "Compatibility alias for old contract route callers."
  [config]
  (start-resource-watcher! config))

(defn handle-list-resources
  "List all resources, optionally filtered by resource kind/class.
   Public so tests can call it directly."
  [do-json config resource-kind]
  (-> (resources/load-all-resources! config)
      (.then (fn [all]
               (let [resource-class (when resource-kind (normalize-resource-class resource-kind))
                     selected (cond->> all
                                resource-class (filter #(= (:resource/class %)
                                                           resource-class))
                                :always        (sort-by (juxt :resource/class :resource/id))
                                :always        vec)]
                 (do-json 200 {:resources (mapv resource-list-summary selected)}))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Failed to list resources: " (.-message err))})))))

(defn handle-list-contracts
  "Compatibility alias for old /contracts clients."
  [do-json config contract-class]
  (-> (resources/load-all-resources! config)
      (.then (fn [all]
               (let [resource-class (when contract-class (normalize-resource-class contract-class))
                     selected (cond->> all
                                resource-class (filter #(= (:resource/class %)
                                                           resource-class))
                                :always        (sort-by (juxt :resource/class :resource/id))
                                :always        vec)]
                 (do-json 200 {:contracts (mapv contract-list-summary selected)}))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Failed to list contracts: " (.-message err))})))))

(defn- handle-get-resource
  [do-json config resource-kind resource-id]
  (-> (.readFile fs (resources/resource-file-path config resource-kind resource-id) "utf8")
      (.then (fn [edn-text]
               (let [resource-class (normalize-resource-class resource-kind)
                     validation (validate-resource-edn resource-class (or edn-text ""))]
                 (do-json 200 {:resourceClass resource-class
                               :resource/id resource-id
                               :ednText (or edn-text "")
                               :resource (wire-value (:contract validation))
                               :validation (dissoc validation :contract)}))))
      (.catch (fn [err]
                (if (= "ENOENT" (.-code err))
                  (do-json 404 {:detail (str "Resource not found: " resource-id)})
                  (do-json 500 {:detail (str "Failed to read resource: " (.-message err))}))))))

(defn- handle-get-contract
  [do-json config contract-class contract-id]
  (-> (.readFile fs (resources/resource-file-path config contract-class contract-id) "utf8")
      (.then (fn [edn-text]
               (let [validation (validate-contract-edn contract-class (or edn-text ""))]
                 (do-json 200 {:contractClass (normalize-contract-class contract-class)
                               :ednText (or edn-text "")
                               :contract (wire-value (:contract validation))
                               :validation (dissoc validation :contract)}))))
      (.catch (fn [err]
                (if (= "ENOENT" (.-code err))
                  (do-json 404 {:detail (str "Contract not found: " contract-id)})
                  (do-json 500 {:detail (str "Failed to read contract: " (.-message err))}))))))

(defn- handle-save-resource
  [do-json config resource-kind resource-id edn-text]
  (let [resource-class (normalize-resource-class resource-kind)
        validation (validate-resource-edn resource-class edn-text)
        validation-out (dissoc validation :contract)
        parsed (:contract validation)
        parsed-id (parsed-resource-id resource-class parsed)
        route-id (str resource-id)]
    (cond
      (not (:ok validation))
      (do-json 400 {:ok false
                    :detail "Resource EDN failed validation"
                    :validation validation-out})

      (and parsed-id (not= route-id parsed-id))
      (do-json 400 {:ok false
                    :detail "Refusing to save resource: record id does not match route resourceId"
                    :routeResourceId route-id
                    :ednResourceId parsed-id
                    :validation validation-out})

      :else
      (let [file-path (resources/resource-file-path config resource-class route-id)]
        (-> (resources/write-edn-file! file-path edn-text)
            (.then (fn [_]
                     (sync-resource-index! config)))
            (.then (fn [_]
                     (do-json 200 {:ok true
                                   :resourceClass resource-class
                                   :resource/id route-id
                                   :ednText edn-text
                                   :resource (wire-value parsed)
                                   :validation validation-out})))
            (.catch (fn [err]
                      (do-json 500 {:detail (str "Failed to save resource: " (.-message err))}))))))))

(defn- handle-save-contract
  [do-json config contract-class contract-id edn-text]
  (let [klass (normalize-contract-class contract-class)
        validation (validate-contract-edn klass edn-text)
        validation-out (dissoc validation :contract)
        parsed (:contract validation)
        parsed-id (parsed-resource-id klass parsed)
        route-id (str contract-id)]
    (cond
      (not (:ok validation))
      (do-json 400 {:ok false
                    :detail "Contract EDN failed validation"
                    :validation validation-out})

      (and parsed-id (not= route-id parsed-id))
      (do-json 400 {:ok false
                    :detail "Refusing to save contract: record id does not match route contractId"
                    :routeContractId route-id
                    :ednContractId parsed-id
                    :validation validation-out})

      :else
      (let [file-path (resources/resource-file-path config klass route-id)]
        (-> (resources/write-edn-file! file-path edn-text)
            (.then (fn [_]
                     (sync-resource-index! config)))
            (.then (fn [_]
                     (do-json 200 {:ok true
                                   :contractClass klass
                                   :ednText edn-text
                                   :contract (wire-value parsed)
                                   :validation validation-out})))
            (.catch (fn [err]
                      (do-json 500 {:detail (str "Failed to save contract: " (.-message err))}))))))))

(defn- handle-copy-resource
  [do-json config resource-kind source-id new-id]
  (-> (.readFile fs (resources/resource-file-path config resource-kind source-id) "utf8")
      (.then (fn [source-edn]
               (let [text (or source-edn "")
                     cloned (update-resource-id-in-edn-text resource-kind text new-id)]
                 (handle-save-resource do-json config resource-kind new-id cloned))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Failed to copy resource: " (.-message err))})))) )

(defn- handle-copy-contract
  [do-json config contract-class source-id new-id]
  (-> (.readFile fs (resources/resource-file-path config contract-class source-id) "utf8")
      (.then (fn [source-edn]
               (let [text (or source-edn "")
                     cloned (update-resource-id-in-edn-text contract-class text new-id)]
                 (handle-save-contract do-json config contract-class new-id cloned))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Failed to copy contract: " (.-message err))})))) )

(defn- handle-validate-resource
  [do-json resource-kind edn-text]
  (let [resource-class (normalize-resource-class resource-kind)]
    (do-json 200 (assoc (wire-validation (validate-resource-edn resource-class edn-text))
                        :resourceClass resource-class))))

(defn- handle-validate-contract
  [do-json contract-class edn-text]
  (do-json 200 (assoc (wire-validation (validate-contract-edn contract-class edn-text))
                      :contractClass (normalize-contract-class contract-class))))

(defn- handle-agent-list-contracts
  [do-text config contract-class]
  (-> (resources/list-resource-ids! config contract-class)
      (.then (fn [ids]
               (do-text 200 (pr-str ids))))
      (.catch (fn [err]
                (do-text 500 (str ";; Failed to list contracts: " (.-message err)))))))

(defn- handle-agent-get-contract-edn
  [do-text config contract-class contract-id]
  (-> (.readFile fs (resources/resource-file-path config contract-class contract-id) "utf8")
      (.then (fn [edn-text]
               (do-text 200 (str edn-text))))
      (.catch (fn [err]
                (if (= "ENOENT" (.-code err))
                  (do-text 404 (str ";; Contract not found: " contract-id))
                  (do-text 500 (str ";; Failed to read contract: " (.-message err))))))))

(defn- handle-agent-validate-contract-edn
  [do-json contract-class edn-text]
  (do-json 200 (assoc (wire-validation (validate-contract-edn contract-class edn-text))
                      :contractClass (normalize-contract-class contract-class))))

(defn- handle-agent-put-contract-edn
  [do-text config contract-class contract-id edn-text]
  (let [klass (normalize-contract-class contract-class)
        validation (validate-contract-edn klass edn-text)]
    (if-not (:ok validation)
      (do-text 422 (pr-str {:ok false
                            :errors (:errors validation)
                            :warnings (:warnings validation)}))
      (let [parsed (:contract validation)
            parsed-id (parsed-resource-id klass parsed)
            route-id (str contract-id)]
        (if (not= route-id parsed-id)
          (do-text 400 (pr-str {:ok false
                                :error "contract_id_mismatch"
                                :routeContractId route-id
                                :ednContractId parsed-id}))
          (-> (resources/write-edn-file! (resources/resource-file-path config klass route-id) edn-text)
              (.then (fn [_]
                       (sync-resource-index! config)))
              (.then (fn [_]
                       (do-text 200 (pr-str {:ok true
                                             :contractClass klass
                                             :contract/id route-id
                                             :contract parsed
                                             :warnings (:warnings validation)}))))
              (.catch (fn [err]
                        (do-text 500 (str ";; Failed to save contract: " (.-message err)))))))))))

(defn- handle-ui-actions
  [do-json config actor-id surface]
  (let [resolved (contracts-resolve/ui-actions-for-actor config actor-id surface)]
    (do-json 200 {:actor_id (:actor-id resolved)
                  :surface (:surface resolved)
                  :default_agent_id (:default-agent-id resolved)
                  :actions (:actions resolved)})))

(defn- text-response!
  [reply status text]
  (.end reply (.status reply status) text (clj->js {"Content-Type" "text/plain; charset=utf-8"})))

(defn- body-map
  [request]
  (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true))

(defn- request-resource-kind
  [request default]
  (or (aget request "query" "kind")
      (aget request "query" "class")
      default))

(defn- request-contract-class
  "Compatibility alias for old contract route clients."
  [request default]
  (request-resource-kind request default))

(defn- body-resource-kind
  ([body default]
   (body-resource-kind body nil default))
  ([body request default]
   (or (:kind body)
       (:class body)
       (:resource_kind body)
       (:resource-kind body)
       (:resourceClass body)
       (:resource-class body)
       (:contract_class body)
       (:contract-class body)
       (some-> request (aget "query" "kind"))
       (some-> request (aget "query" "class"))
       default)))

(defn- body-contract-class
  "Compatibility alias for old contract route clients."
  ([body default]
   (body-resource-kind body default))
  ([body request default]
   (body-resource-kind body request default)))

(defn- body-edn-text
  [body]
  (str (or (:ednText body)
           (:edn_text body)
           (:edn-text body)
           "")))

(defn- with-route-context
  [runtime do-ctx do-err f]
  (fn [request reply]
    (do-ctx runtime request reply
      (fn [ctx]
        (try
          (f ctx request reply)
          (catch :default err
            (do-err reply err)))))))

(defn- agent-ui-actions-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [actor-id (or (aget request "query" "actor")
                         (aget request "query" "actor_id")
                         (aget request "query" "actorId"))
            surface (or (aget request "query" "surface")
                        (aget request "query" "surface_id")
                        (aget request "query" "surfaceId"))]
        (handle-ui-actions (partial do-json reply) config actor-id surface)))))

(defn- agent-list-contracts-route
  [runtime config do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [safe-kind (safe-contract-class (request-contract-class request "agents"))]
        (if-not (:ok safe-kind)
          (text-response! reply 400 (str ";; Invalid contract class: " (:error safe-kind)))
          (handle-agent-list-contracts (partial text-response! reply) config (:class safe-kind)))))))

(defn- agent-validate-contract-route
  [runtime do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [body (body-map request)
            safe-kind (safe-contract-class (body-contract-class body request "agents"))]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (handle-agent-validate-contract-edn (partial do-json reply) (:class safe-kind) (body-edn-text body)))))))

(defn- agent-get-contract-route
  [runtime config do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [contract-id (str (or (aget request "params" "contractId") ""))
            safe (safe-contract-id contract-id)
            safe-kind (safe-contract-class (request-contract-class request "agents"))]
        (cond
          (str/blank? contract-id) (text-response! reply 400 ";; contractId is required")
          (not (:ok safe-kind)) (text-response! reply 400 (str ";; Invalid contract class: " (:error safe-kind)))
          (not (:ok safe)) (text-response! reply 400 (str ";; Invalid contractId: " (:error safe)))
          :else (handle-agent-get-contract-edn (partial text-response! reply) config (:class safe-kind) (:id safe)))))))

(defn- agent-put-contract-route
  [runtime config do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [contract-id (str (or (aget request "params" "contractId") ""))
            safe (safe-contract-id contract-id)
            safe-kind (safe-contract-class (request-contract-class request "agents"))
            edn-text (str (or (aget request "body") ""))]
        (cond
          (str/blank? contract-id) (text-response! reply 400 ";; contractId is required")
          (not (:ok safe-kind)) (text-response! reply 400 (str ";; Invalid contract class: " (:error safe-kind)))
          (not (:ok safe)) (text-response! reply 400 (str ";; Invalid contractId: " (:error safe)))
          :else (handle-agent-put-contract-edn (partial text-response! reply) config (:class safe-kind) (:id safe) edn-text))))))

(defn- register-agent-contract-routes!
  [app runtime config helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)]
    (do-route app "GET" "/api/contracts/ui-actions"
              (agent-ui-actions-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/agent/contracts"
              (agent-list-contracts-route runtime config do-err do-ctx do-perm))
    (do-route app "POST" "/api/agent/contracts/validate"
              (agent-validate-contract-route runtime do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/agent/contracts/:contractId"
              (agent-get-contract-route runtime config do-err do-ctx do-perm))
    (do-route app "PUT" "/api/agent/contracts/:contractId"
              (agent-put-contract-route runtime config do-err do-ctx do-perm))))

(defn- admin-list-resources-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [kind (request-resource-kind request nil)
            safe-kind (if kind (safe-resource-class kind) {:ok true :class nil})]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid resource kind" :error (:error safe-kind)})
          (handle-list-resources (partial do-json reply) config (:class safe-kind)))))))

(defn- admin-get-resource-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [resource-id (str (or (aget request "params" "resourceId") ""))
            safe (safe-resource-id resource-id)
            safe-kind (safe-resource-class (request-resource-kind request "agents"))]
        (cond
          (str/blank? resource-id) (do-json reply 400 {:detail "resourceId is required"})
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid resource kind" :error (:error safe-kind)})
          (not (:ok safe)) (do-json reply 400 {:detail "Invalid resourceId" :error (:error safe)})
          :else (handle-get-resource (partial do-json reply) config (:class safe-kind) (:id safe)))))))

(defn- admin-save-resource-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [resource-id (str (or (aget request "params" "resourceId") ""))
            body (body-map request)
            safe (safe-resource-id resource-id)
            safe-kind (safe-resource-class (body-resource-kind body request "agents"))]
        (cond
          (str/blank? resource-id) (do-json reply 400 {:detail "resourceId is required"})
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid resource kind" :error (:error safe-kind)})
          (not (:ok safe)) (do-json reply 400 {:detail "Invalid resourceId" :error (:error safe)})
          :else (handle-save-resource (partial do-json reply) config (:class safe-kind) (:id safe) (body-edn-text body)))))))

(defn- admin-validate-resource-route
  [runtime do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [body (body-map request)
            safe-kind (safe-resource-class (body-resource-kind body "agents"))]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid resource kind" :error (:error safe-kind)})
          (handle-validate-resource (partial do-json reply) (:class safe-kind) (body-edn-text body)))))))

(defn- admin-copy-resource-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [source-id (str (or (aget request "params" "resourceId") ""))
            body (body-map request)
            new-id (str (or (:newId body) ""))
            safe-kind (safe-resource-class (body-resource-kind body "agents"))
            safe-source (safe-resource-id source-id)
            safe-new (safe-resource-id new-id)]
        (cond
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid resource kind" :error (:error safe-kind)})
          (or (str/blank? source-id) (str/blank? new-id)) (do-json reply 400 {:detail "source resourceId and newId are required"})
          (not (:ok safe-source)) (do-json reply 400 {:detail "Invalid source resourceId" :error (:error safe-source)})
          (not (:ok safe-new)) (do-json reply 400 {:detail "Invalid newId" :error (:error safe-new)})
          :else (handle-copy-resource (partial do-json reply) config (:class safe-kind) (:id safe-source) (:id safe-new)))))))

(defn- admin-list-contracts-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [kind (request-contract-class request nil)
            safe-kind (if kind (safe-contract-class kind) {:ok true :class nil})]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (handle-list-contracts (partial do-json reply) config (:class safe-kind)))))))

(defn- admin-get-contract-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [contract-id (str (or (aget request "params" "contractId") ""))
            safe (safe-contract-id contract-id)
            safe-kind (safe-contract-class (request-contract-class request "agents"))]
        (cond
          (str/blank? contract-id) (do-json reply 400 {:detail "contractId is required"})
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (not (:ok safe)) (do-json reply 400 {:detail "Invalid contractId" :error (:error safe)})
          :else (handle-get-contract (partial do-json reply) config (:class safe-kind) (:id safe)))))))

(defn- admin-save-contract-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [contract-id (str (or (aget request "params" "contractId") ""))
            body (body-map request)
            safe (safe-contract-id contract-id)
            safe-kind (safe-contract-class (body-contract-class body request "agents"))]
        (cond
          (str/blank? contract-id) (do-json reply 400 {:detail "contractId is required"})
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (not (:ok safe)) (do-json reply 400 {:detail "Invalid contractId" :error (:error safe)})
          :else (handle-save-contract (partial do-json reply) config (:class safe-kind) (:id safe) (body-edn-text body)))))))

(defn- admin-validate-contract-route
  [runtime do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [body (body-map request)
            safe-kind (safe-contract-class (body-contract-class body "agents"))]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (handle-validate-contract (partial do-json reply) (:class safe-kind) (body-edn-text body)))))))

(defn- admin-copy-contract-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [source-id (str (or (aget request "params" "contractId") ""))
            body (body-map request)
            new-id (str (or (:newId body) ""))
            safe-kind (safe-contract-class (body-contract-class body "agents"))
            safe-source (safe-contract-id source-id)
            safe-new (safe-contract-id new-id)]
        (cond
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (or (str/blank? source-id) (str/blank? new-id)) (do-json reply 400 {:detail "source contractId and newId are required"})
          (not (:ok safe-source)) (do-json reply 400 {:detail "Invalid source contractId" :error (:error safe-source)})
          (not (:ok safe-new)) (do-json reply 400 {:detail "Invalid newId" :error (:error safe-new)})
          :else (handle-copy-contract (partial do-json reply) config (:class safe-kind) (:id safe-source) (:id safe-new)))))))

(defn- register-admin-resource-routes!
  [app runtime config helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)]
    (do-route app "GET" "/api/admin/resources"
              (admin-list-resources-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/admin/resources/:resourceId"
              (admin-get-resource-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "PUT" "/api/admin/resources/:resourceId"
              (admin-save-resource-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "POST" "/api/admin/resources/validate"
              (admin-validate-resource-route runtime do-json do-err do-ctx do-perm))
    (do-route app "POST" "/api/admin/resources/:resourceId/copy"
              (admin-copy-resource-route runtime config do-json do-err do-ctx do-perm))))

(defn- register-admin-contract-routes!
  [app runtime config helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)]
    (do-route app "GET" "/api/admin/contracts"
              (admin-list-contracts-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/admin/contracts/:contractId"
              (admin-get-contract-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "PUT" "/api/admin/contracts/:contractId"
              (admin-save-contract-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "POST" "/api/admin/contracts/validate"
              (admin-validate-contract-route runtime do-json do-err do-ctx do-perm))
    (do-route app "POST" "/api/admin/contracts/:contractId/copy"
              (admin-copy-contract-route runtime config do-json do-err do-ctx do-perm))))

(defn register-resource-routes!
  [app runtime config helpers]
  (register-agent-contract-routes! app runtime config helpers)
  (register-admin-resource-routes! app runtime config helpers)
  (register-admin-contract-routes! app runtime config helpers)
  nil)

(defn register-contracts-routes!
  "Compatibility alias for old route registration."
  [app runtime config helpers]
  (register-resource-routes! app runtime config helpers))
