(ns knoxx.backend.infra.routes.resources
  "Resource route implementation. Registers resource-native admin routes and
   legacy /contracts compatibility routes while the UI migrates."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.domain.contracts.resolve :as contracts-resolve]
            [knoxx.backend.infra.event-runtime :as event-runtime]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.law.contracts :as validator]
            [knoxx.backend.infra.publication-admission-hook :as publication-admission-hook]
            ["node:fs" :as node-fs]
            ["node:fs/promises" :as fs]))

(def ^:private resource-watch-debounce-ms 350)

(defonce ^:private resource-watchers* (atom []))
(defonce ^:private resource-watch-timer* (atom nil))
(defonce ^:private resource-watch-running?* (atom false))

(def ^:private translation-admission-permission
  "org.translations.manage")

(def ^:private publication-admission-permission
  "org.publications.manage")

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

(defn- qualified-id-with-local-slug
  [value new-id]
  (let [existing-ns (when (keyword? value) (namespace value))
        local-name (str/replace (str new-id) #"_" "-")]
    (keyword existing-ns local-name)))

(defn- replace-qualified-resource-id
  [edn-text id-key pattern new-id]
  (let [parsed (reader/read-string edn-text)
        value (qualified-id-with-local-slug (get parsed id-key) new-id)]
    (str/replace edn-text pattern (str id-key " " value))))

(defn- parsed-resource-id
  [resource-class value]
  (case (normalize-resource-class resource-class)
    "agents" (some-> (:contract/id value) str)
    "policies" (some-> (:contract/id value) str)
    "sources" (some-> (:contract/id value) str)
    "actions" (some-> (:contract/id value) str)
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
  (let [resource (record-definition record)
        events (wire-value (:trigger/events resource))]
    {:kind   (keywordish-name (:trigger/kind resource))
     :target (or (:trigger/target resource)
                 (get-in resource [:trigger/with :agent-id])
                 (:trigger/agent resource))
     :events events
     :source {:events events}
     :filters (wire-value (get-in resource [:data :filters]))
     :context (wire-value (get-in resource [:data :context]))}))

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
                  [(validation-warning ["data" (name k)] "This looks like mutable runtime state inside a static resource. Prefer Mongo/OpenPlanner/durable files, not resource :data mutation.")]))
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

(defn update-resource-id-in-edn-text
  "Rewrite the copied resource's own identity while preserving its namespace."
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

    "documents" (replace-qualified-resource-id
                 edn-text :document/id #":document/id\s+:[^\s\]}]+" new-id)
    "gardens" (replace-qualified-resource-id
               edn-text :garden/id #":garden/id\s+:[^\s\]}]+" new-id)
    "publications" (replace-qualified-resource-id
                    edn-text :publication/id #":publication/id\s+:[^\s\]}]+" new-id)

    edn-text))

(defonce ^:private resource-index* (atom #{}))

(defn ^:async sync-resource-index!
  "Sync resource EDN files → in-memory resource index set.

   The index is a fast in-process cache; disk is canonical. Invalid resource
   files are omitted by the loader and must not block backend startup or the
   repair UI."
  [config]
  (try
    (let [records (await (resources/load-all-resources! config))
          ids (->> records
                   (map (fn [record]
                          (resource-id->index-key (:resource/class record)
                                                  (:resource/id record))))
                   distinct
                   sort
                   vec)
          existing-set @resource-index*
          desired-set (set ids)
          to-add (vec (sort (set/difference desired-set existing-set)))
          to-remove (vec (sort (set/difference existing-set desired-set)))]
      (reset! resource-index* desired-set)
      (println "[resources] synced resource index; add=" (count to-add) "remove=" (count to-remove))
      {:ok true
       :added to-add
       :removed to-remove
       :count (count ids)})
    (catch :default err
      (println "[resources] sync-resource-index! failed; startup continuing:" (.-message err))
      {:ok false :error (.-message err)})))

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

(defn- ^:async resource-refresh!
  [config]
  (try
    (await (sync-resource-index! config))
    (event-runtime/debounced-reload!)
    (println "[resources] event runtime reload queued after resource change")
    (catch :default err
      (println "[resources] watcher refresh failed:" (.-message err)))))

(defn- schedule-resource-refresh!
  [config reason]
  (clear-resource-watch-timer!)
  (reset! resource-watch-timer*
          (js/setTimeout
           (fn []
             (reset! resource-watch-timer* nil)
             (println "[resources] watcher refresh triggered by" reason)
             (resource-refresh! config))
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

(defn ^:async handle-list-resources
  "List all resources, optionally filtered by resource kind/class.
   Public so tests can call it directly."
  [do-json config resource-kind]
  (try
    (let [all (await (resources/load-all-resources! config))
          resource-class (when resource-kind (normalize-resource-class resource-kind))
          selected (cond->> all
                     resource-class (filter #(= (:resource/class %)
                                                resource-class))
                     :always        (sort-by (juxt :resource/class :resource/id))
                     :always        vec)]
      (do-json 200 {:resources (mapv resource-list-summary selected)}))
    (catch :default err
      (do-json 500 {:detail (str "Failed to list resources: " (.-message err))}))))

(defn ^:async handle-list-contracts
  "Compatibility alias for old /contracts clients."
  [do-json config contract-class]
  (try
    (let [all (await (resources/load-all-resources! config))
          resource-class (when contract-class (normalize-resource-class contract-class))
          selected (cond->> all
                     resource-class (filter #(= (:resource/class %)
                                                resource-class))
                     :always        (sort-by (juxt :resource/class :resource/id))
                     :always        vec)]
      (do-json 200 {:contracts (mapv contract-list-summary selected)}))
    (catch :default err
      (do-json 500 {:detail (str "Failed to list contracts: " (.-message err))}))))

(defn- ^:async handle-get-resource
  [do-json config resource-kind resource-id]
  (try
    (let [edn-text (await (.readFile fs (resources/resource-file-path config resource-kind resource-id) "utf8"))
          resource-class (normalize-resource-class resource-kind)
          validation (validate-resource-edn resource-class (or edn-text ""))]
      (do-json 200 {:resourceClass resource-class
                    :resource/id resource-id
                    :ednText (or edn-text "")
                    :resource (wire-value (:contract validation))
                    :validation (dissoc validation :contract)}))
    (catch :default err
      (if (= "ENOENT" (.-code err))
        (do-json 404 {:detail (str "Resource not found: " resource-id)})
        (do-json 500 {:detail (str "Failed to read resource: " (.-message err))})))))

(defn- ^:async handle-get-contract
  [do-json config contract-class contract-id]
  (try
    (let [edn-text (await (.readFile fs (resources/resource-file-path config contract-class contract-id) "utf8"))
          validation (validate-contract-edn contract-class (or edn-text ""))]
      (do-json 200 {:contractClass (normalize-contract-class contract-class)
                    :ednText (or edn-text "")
                    :contract (wire-value (:contract validation))
                    :validation (dissoc validation :contract)}))
    (catch :default err
      (if (= "ENOENT" (.-code err))
        (do-json 404 {:detail (str "Contract not found: " contract-id)})
        (do-json 500 {:detail (str "Failed to read contract: " (.-message err))})))))

(defn admission-document-id
  "Return the document whose translation inventory changed after a resource write."
  [resource-class resource]
  (case (normalize-resource-class resource-class)
    "documents" (:document/id resource)
    "publications" (:publication/document resource)
    nil))

(defn admission-scope
  "Project an authenticated write context into the trusted admission scope."
  [config ctx]
  (let [org-id (some-> (authz/ctx-org-id ctx) str not-empty)
        membership-id (some-> (authz/ctx-membership-id ctx) str not-empty)]
    (when-not (and org-id membership-id)
      (throw (ex-info "automatic document admission requires an organization and membership context"
                      {:status 403
                       :code "document_admission_context_required"})))
    (cond-> {:org-id org-id :membership-id membership-id}
      (some-> (:session-project-name config) str not-empty)
      (assoc :project (str (:session-project-name config))))))

(defn ^:async admit-saved-publication-resource!
  "Admit a saved document/publication through the app-owned internal hook."
  [config ctx resource-class resource admit!]
  (when-let [document-id (admission-document-id resource-class resource)]
    (when ctx
      (authz/ensure-permission! ctx translation-admission-permission)
      (authz/ensure-permission! ctx publication-admission-permission))
    (let [result (await (admit! (admission-scope config ctx)
                                {:document document-id}))]
      (when-not (and (true? (:ok result))
                     (zero? (or (:failed result) 0))
                     (pos? (or (:admitted result) 0)))
        (throw (ex-info "automatic document admission did not complete"
                        {:status 503
                         :code "document_admission_failed"
                         :document/id document-id
                         :admission result})))
      result)))

(defn- missing-file-error?
  [err]
  (= "ENOENT" (or (.-code err) (:code (ex-data err)))))

(defn- ^:async require-index-sync!
  [sync-index! config file-path phase]
  (let [result (await (sync-index! config))]
    (when-not (true? (:ok result))
      (throw (ex-info "resource index synchronization failed"
                      {:status 503
                       :code "resource_index_sync_failed"
                       :resource/path file-path
                       :resource/index-sync-phase phase
                       :resource/index-sync-result result})))
    result))

(defonce ^:private resource-write-tails* (atom {}))

(defn- ^:async run-after-resource-write!
  [previous task-fn]
  (when previous
    (try
      (await previous)
      ;; knoxx-lint/allow-silent-catch — an earlier write must not poison this resource queue.
      (catch :default _
        nil)))
  (await (task-fn)))

(defn- ^:async recover-resource-write-tail!
  [task]
  (try
    (await task)
    ;; knoxx-lint/allow-silent-catch — only the stored recovery tail consumes this rejection.
    (catch :default _
      nil)))

(defn- ^:async retire-resource-write-tail!
  [file-path tail]
  (await tail)
  (swap! resource-write-tails*
         (fn [tails]
           (if (identical? tail (get tails file-path))
             (dissoc tails file-path)
             tails))))

(defn- enqueue-resource-write!
  "Run one complete write transaction after earlier work for the exact path."
  [file-path task-fn]
  ;; ClojureScript runs this deref/create/assoc sequence without yielding. The
  ;; task itself first yields only after the tail has been installed, so another
  ;; request cannot observe a stale predecessor on the single JS event loop.
  (let [task (run-after-resource-write!
              (get @resource-write-tails* file-path) task-fn)
        tail (recover-resource-write-tail! task)]
    (swap! resource-write-tails* assoc file-path tail)
    (retire-resource-write-tail! file-path tail)
    task))

(defn- ^:async rollback-resource-write!
  [config file-path previous write-file! delete-file! sync-index!]
  (if (:exists? previous)
    (await (write-file! file-path (:text previous)))
    (try
      (await (delete-file! file-path))
      (catch :default delete-err
        (when-not (missing-file-error? delete-err)
          (throw delete-err)))))
  (await (require-index-sync! sync-index! config file-path :rollback)))

(defn- resource-write-rollback-error
  [file-path write-error rollback-error]
  (ex-info "resource write/index synchronization failed and its file rollback also failed"
           {:status 500
            :code "resource_write_rollback_failed"
            :resource/path file-path
            :write/error (or (ex-message write-error)
                             (str write-error))
            :rollback/error (or (ex-message rollback-error)
                                (str rollback-error))}
           rollback-error))

(defn- ^:async write-resource-and-admit-once!
  "Perform one resource write/admission transaction while its path is owned."
  [config file-path edn-text admission!
   {:keys [read-file! write-file! delete-file! sync-index!] :as _deps}]
  (let [read-file! (or read-file! (fn [path] (.readFile fs path "utf8")))
        write-file! (or write-file! resources/write-edn-file!)
        delete-file! (or delete-file! (fn [path] (.unlink fs path)))
        sync-index! (or sync-index! sync-resource-index!)
        previous (try
                   {:exists? true :text (await (read-file! file-path))}
                   (catch :default err
                     (if (missing-file-error? err)
                       {:exists? false}
                       (throw err))))]
    (try
      (await (write-file! file-path edn-text))
      (await (require-index-sync! sync-index! config file-path :forward))
      (catch :default err
        (try
          (await (rollback-resource-write!
                  config file-path previous write-file! delete-file! sync-index!))
          (catch :default rollback-err
            (throw (resource-write-rollback-error file-path err rollback-err))))
        (throw err)))
    ;; Invoking admission crosses into durable event and queue effects that the
    ;; filesystem cannot roll back. Preserve the exact entered bytes if that
    ;; work fails so a retry or reconciliation pass still has its source.
    (when admission! (await (admission!)))))

(defn ^:async write-resource-and-admit!
  "Persist one resource and run its admission action as one serialized write.

  Filesystem and translation/event persistence cannot share a transaction. This
  boundary therefore treats writing plus forward index synchronization as its
  reversible phase: either failure restores the previous file (or removes a
  newly created one), refreshes the index, and rethrows the original failure.

  Invoking admission begins an irreversible phase because it may append events
  or enqueue agents before failing. An admission failure is rethrown without
  restoring or deleting the entered file. Its exact bytes remain available to
  retry or reconciliation instead of leaving durable effects whose source has
  vanished.

  The snapshot, write, sync, admission, and any pre-admission rollback are
  serialized by exact file path. A newer request therefore observes the bytes
  actually left by the preceding request before publishing its own.

  The optional dependency map exists for focused failure-path tests; production
  uses the real resource writer, filesystem, and index sync functions."
  ([config file-path edn-text admission!]
   (write-resource-and-admit! config file-path edn-text admission! {}))
  ([config file-path edn-text admission!
    deps]
   (await
    (enqueue-resource-write!
     file-path
     (fn []
       (write-resource-and-admit-once!
        config file-path edn-text admission! deps))))))

(defn ^:async handle-save-resource
  ([do-json config resource-kind resource-id edn-text]
   (handle-save-resource do-json config resource-kind resource-id edn-text nil nil))
  ([do-json config resource-kind resource-id edn-text ctx admit!]
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
       (try
         (let [file-path (resources/resource-file-path config resource-class route-id)
               admission (await
                          (write-resource-and-admit!
                           config file-path edn-text
                           (when admit!
                             (fn []
                               (admit-saved-publication-resource!
                                config ctx resource-class parsed admit!)))))]
             (do-json 200 (cond-> {:ok true
                                   :resourceClass resource-class
                                   :resource/id route-id
                                   :ednText edn-text
                                   :resource (wire-value parsed)
                                   :validation validation-out}
                            admission (assoc :admission (wire-value admission)))))
         (catch :default err
           (let [status (or (:status (ex-data err)) 500)]
             (do-json status {:ok false
                              :detail (str "Failed to save and admit resource: " (.-message err))
                              :code (:code (ex-data err))}))))))))

(defn- ^:async handle-save-contract
  ([do-json config contract-class contract-id edn-text]
   (handle-save-contract do-json config contract-class contract-id edn-text nil nil))
  ([do-json config contract-class contract-id edn-text ctx admit!]
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
       (try
         (let [file-path (resources/resource-file-path config klass route-id)
               admission (await
                          (write-resource-and-admit!
                           config file-path edn-text
                           (when admit!
                             (fn []
                               (admit-saved-publication-resource!
                                config ctx klass parsed admit!)))))]
           (do-json 200 (cond-> {:ok true
                                 :contractClass klass
                                 :ednText edn-text
                                 :contract (wire-value parsed)
                                 :validation validation-out}
                          admission (assoc :admission (wire-value admission)))))
         (catch :default err
           (let [status (or (:status (ex-data err)) 500)]
             (do-json status {:ok false
                              :detail (str "Failed to save and admit contract: "
                                           (.-message err))
                              :code (:code (ex-data err))}))))))))

(defn- ^:async handle-copy-resource
  ([do-json config resource-kind source-id new-id]
   (handle-copy-resource do-json config resource-kind source-id new-id nil nil))
  ([do-json config resource-kind source-id new-id ctx admit!]
   (try
     (let [source-edn (await (.readFile fs (resources/resource-file-path config resource-kind source-id) "utf8"))
           text (or source-edn "")
           cloned (update-resource-id-in-edn-text resource-kind text new-id)]
       (await (handle-save-resource do-json config resource-kind new-id cloned ctx admit!)))
     (catch :default err
       (do-json 500 {:detail (str "Failed to copy resource: " (.-message err))})))))

(defn- ^:async handle-copy-contract
  ([do-json config contract-class source-id new-id]
   (handle-copy-contract do-json config contract-class source-id new-id nil nil))
  ([do-json config contract-class source-id new-id ctx admit!]
   (try
     (let [source-edn (await (.readFile fs (resources/resource-file-path config contract-class source-id) "utf8"))
           text (or source-edn "")
           cloned (update-resource-id-in-edn-text contract-class text new-id)]
       (await (handle-save-contract do-json config contract-class new-id cloned ctx admit!)))
     (catch :default err
       (do-json 500 {:detail (str "Failed to copy contract: " (.-message err))})))))

(defn- handle-validate-resource
  [do-json resource-kind edn-text]
  (let [resource-class (normalize-resource-class resource-kind)]
    (do-json 200 (assoc (wire-validation (validate-resource-edn resource-class edn-text))
                        :resourceClass resource-class))))

(defn- handle-validate-contract
  [do-json contract-class edn-text]
  (do-json 200 (assoc (wire-validation (validate-contract-edn contract-class edn-text))
                      :contractClass (normalize-contract-class contract-class))))

(defn- ^:async handle-agent-list-contracts
  [do-text config contract-class]
  (try
    (let [ids (await (resources/list-resource-ids! config contract-class))]
      (do-text 200 (pr-str ids)))
    (catch :default err
      (do-text 500 (str ";; Failed to list contracts: " (.-message err))))))

(defn- ^:async handle-agent-get-contract-edn
  [do-text config contract-class contract-id]
  (try
    (let [edn-text (await (.readFile fs (resources/resource-file-path config contract-class contract-id) "utf8"))]
      (do-text 200 (str edn-text)))
    (catch :default err
      (if (= "ENOENT" (.-code err))
        (do-text 404 (str ";; Contract not found: " contract-id))
        (do-text 500 (str ";; Failed to read contract: " (.-message err)))))))

(defn- handle-agent-validate-contract-edn
  [do-json contract-class edn-text]
  (do-json 200 (assoc (wire-validation (validate-contract-edn contract-class edn-text))
                      :contractClass (normalize-contract-class contract-class))))

(defn- ^:async persist-agent-contract-edn!
  [do-text config route-id edn-text ctx admit! parsed warnings]
  (try
    (let [admission
          (await
           (write-resource-and-admit!
            config (resources/resource-file-path config "agents" route-id)
            edn-text
            (when admit!
              (fn []
                (admit-saved-publication-resource!
                 config ctx "agents" parsed admit!)))))]
      (do-text 200 (pr-str (cond-> {:ok true
                                    :contractClass "agents"
                                    :contract/id route-id
                                    :contract parsed
                                    :warnings warnings}
                             admission (assoc :admission admission)))))
    (catch :default err
      (do-text (or (:status (ex-data err)) 500)
               (str ";; Failed to save and admit contract: "
                    (or (.-message err) (ex-message err)))))))

(defn ^:async handle-agent-put-contract-edn
  "Compatibility PUT handler retained for agent clients during resource migration."
  ([do-text config contract-class contract-id edn-text]
   (handle-agent-put-contract-edn do-text config contract-class contract-id edn-text nil nil))
  ([do-text config contract-class contract-id edn-text ctx admit!]
   (let [klass (normalize-contract-class contract-class)
         route-id (str contract-id)]
     (if-not (= "agents" klass)
       (do-text 400 (pr-str {:ok false
                             :error "compatibility_contract_class_not_writable"
                             :contractClass klass}))
       (let [validation (validate-contract-edn klass edn-text)
             parsed (:contract validation)
             parsed-id (parsed-resource-id klass parsed)]
         (cond
           (not (:ok validation))
           (do-text 422 (pr-str {:ok false
                                 :errors (:errors validation)
                                 :warnings (:warnings validation)}))

           (and parsed-id (not= route-id parsed-id))
           (do-text 400 (pr-str {:ok false
                                 :error "contract_id_mismatch"
                                 :routeContractId route-id
                                 :ednContractId parsed-id}))

           :else
           (await (persist-agent-contract-edn!
                   do-text config route-id edn-text ctx admit! parsed
                   (:warnings validation)))))))))

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
          :else (handle-agent-put-contract-edn (partial text-response! reply) config
                                               (:class safe-kind) (:id safe) edn-text
                                               ctx publication-admission-hook/admit!))))))

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
          :else (handle-save-resource (partial do-json reply) config (:class safe-kind) (:id safe)
                                      (body-edn-text body) ctx publication-admission-hook/admit!))))))

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
          :else (handle-copy-resource (partial do-json reply) config (:class safe-kind)
                                      (:id safe-source) (:id safe-new)
                                      ctx publication-admission-hook/admit!))))))

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
          :else (handle-save-contract (partial do-json reply) config (:class safe-kind) (:id safe)
                                      (body-edn-text body) ctx publication-admission-hook/admit!))))))

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
          :else (handle-copy-contract (partial do-json reply) config (:class safe-kind)
                                      (:id safe-source) (:id safe-new)
                                      ctx publication-admission-hook/admit!))))))

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
