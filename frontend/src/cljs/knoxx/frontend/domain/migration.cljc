(ns knoxx.frontend.domain.migration
  "Pure assembly of source facts into the canonical frontend migration ledger."
  (:require [clojure.string :as str]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.shape.migration :as shape]))

(def test-source-pattern
  "Pattern identifying governed Vitest source paths."
  #"\.(?:test|spec)\.(?:tsx?|mts|cts)$")

(defn test-source?
  "Whether a governed TypeScript path is a Vitest suite."
  [path]
  (boolean (re-find test-source-pattern path)))

(defn file-role
  "Classify a governed file by its migration responsibility."
  [path]
  (cond
    (test-source? path) :test
    (str/includes? path "/bridge/") :bridge
    (str/includes? path "/src/lib/") :library
    (str/includes? path "/src/pages/") :route
    (str/includes? path "/src/components/") :component
    :else :support))

(defn source-stem
  "Remove TypeScript and test suffixes for sibling test association."
  [path]
  (-> path
      (str/replace test-source-pattern "")
      (str/replace #"\.(?:tsx?|mts|cts)$" "")))

(defn tests-by-source
  "Index legacy test suites by their probable sibling source stem."
  [sources]
  (->> sources
       (filter (comp test-source? :path))
       (group-by (comp source-stem :path))
       (map (fn [[stem tests]] [stem (mapv :path tests)]))
       (into {})))

(def island-rules
  "Ordered ownership rules for the migration's behavior islands."
  [[#"(?:chat-page|ChatPage|ChatComposer|ToolReceiptBlock|MemorySignalChip|LoungePanel|ConsolePanel)" :chat-workspace]
   [#"(?:agent-audit|AgentsPage)" :agent-audit]
   [#"(?:event-agent|EventAgent|EventsPage)" :event-agents]
   [#"(?:ContractsPage|api/contracts|EdnEditor)" :contracts]
   [#"(?:BroadcastStudio|components/studio)" :broadcast-studio]
   [#"(?:CmsPage|VisualCms|components/cms|components/editor|components/review|ReviewQueue|ContentEditor|publication|viewContract|contractComposition)" :cms]
   [#"(?:DataPage|DocumentsPage|IngestionPage|VectorsPage|RawGraphExport|GraphExplorer|ingestion-page|raw-graph-export|DataLakes)" :data]
   [#"(?:OpsRoot|components/ops|SidebarOpsStatus)" :ops]
   [#"(?:auth-context|useAuth)" :auth]
   [#"(?:TranslationPage|translation-page)" :translations]
   [#"(?:components/layout|context-bar)" :layout]
   [#"(?:workspace-context|WorkspaceBrowser)" :workspace]
   [#"(?:bridge/)" :bridge]
   [#"(?:src/lib/)" :shared]
   [#"(?:src/pages/)" :routes]
   [#"(?:src/components/)" :components]
   [#"(?:src/test/)" :test-infrastructure]])

(def island-blockers
  "Known migration dependencies between behavior islands."
  {:agent-audit [:chat-workspace]
   :broadcast-studio [:chat-workspace :audio-visualization-adapter]
   :cms [:chat-workspace :codemirror-adapter :puck-adapter]
   :contracts [:chat-workspace :codemirror-adapter]
   :data [:chart-adapter :webgl-graph-adapter]})

(defn classify-island
  "Assign a governed source path to its migration behavior island."
  [path]
  (or (some (fn [[pattern island]]
              (when (re-find pattern path) island))
            island-rules)
      (throw (ex-info "No migration island rule matches governed path"
                      {:path path}))))

(def heavy-widget-pattern
  "Legacy widgets whose libraries should stay behind thin Helix adapters."
  #"(?:EdnEditor|VisualEditor|GraphExplorer|VectorsPage|DataPage|MusicPlayerView)")

(defn file-disposition
  "Choose the migration's terminal action for a legacy source file."
  [path source]
  (cond
    (str/includes? path "/bridge/") :delete
    (str/includes? source "window.knoxx.frontend") :delete
    (re-find heavy-widget-pattern path) :wrap
    :else :port))

(defn- reachable-references [live-references local-definitions]
  (loop [pending (seq live-references)
         visited #{}
         references []]
    (if-let [reference (first pending)]
      (let [local-name (:name reference)
            dependencies (when-not (contains? visited local-name)
                           (get local-definitions local-name))]
        (recur (concat (rest pending) dependencies)
               (cond-> visited local-name (conj local-name))
               (conj references reference)))
      references)))

(defn legacy-route-exports
  "Identify app route exports and unresolved exports that cannot prove native ownership."
  [exports]
  (->> exports
       (filter #(and (= :app (:bridge %))
                     (or (empty? (:origin-paths %))
                         (some (fn [path] (= :route (file-role path))) (:origin-paths %)))))
       (map :symbol)
       set))

(defn- unresolved-project-ownership?
  [source-namespace project-namespaces route-exports references]
  (let [{:keys [aliases referred-names]} (get project-namespaces source-namespace)
        starts (keep #(or (get aliases (:namespace %)) (get aliases (:name %))
                          (get referred-names (:name %)) (:namespace %)) references)]
    (loop [pending (seq starts) visited #{source-namespace}]
      (if-let [module (first pending)]
        (if (contains? visited module)
          (recur (rest pending) visited)
          (let [{:keys [dependencies bridge-exports unresolved-bridge?]} (get project-namespaces module)]
            (if (or unresolved-bridge? (some (or route-exports #{}) bridge-exports))
              true
              (recur (concat (rest pending) dependencies) (conj visited module)))))
        false))))

(defn route-implementation
  "Select known ownership; unresolved bridge module values cannot prove native ownership."
  [{:keys [bridge-alias live-references rendered-components local-definitions
           source-namespace project-namespaces lexically-bound-components]
    route-exports :legacy-route-exports}]
  (let [references (reachable-references live-references local-definitions)
        bridge-referred-names (get-in project-namespaces [source-namespace :bridge-referred-names])]
    (or (when bridge-alias
          (some #(or (when (= bridge-alias (:namespace %)) (:name %))
                     (when-let [export (and (nil? (:namespace %))
                                             (get bridge-referred-names (:name %)))]
                       (str bridge-alias "/" export))) references))
        (when-not (or (seq lexically-bound-components)
                      (and bridge-alias
                            (some #(and (nil? (:namespace %)) (= bridge-alias (:name %))) references))
                      (unresolved-project-ownership? source-namespace project-namespaces
                                                     route-exports references))
          (or (some #(when (:namespace %) (:name %)) rendered-components)
              (some #(when (contains? #{"LegacyOpsRedirect" "Navigate" "PlaceholderPage"} (:name %))
                       (:name %))
                    rendered-components))))))

(defn- route-record
  "Classify parsed route ownership before constructing its structural record."
  [{:keys [bridge-alias implementation] :as route}]
  (shape/route-record
    (assoc route :legacy?
                 (str/starts-with? implementation (str bridge-alias "/")))))

(defn assemble-records
  "Build, validate, and deterministically order all manifest records."
  [{:keys [sources bridge-exports routes]}]
  (let [test-index (tests-by-source sources)
        file-records (map (fn [{:keys [path] :as source}]
                            (let [island (classify-island path)]
                              (shape/legacy-file-record
                               (assoc source
                                      :island island
                                      :role (file-role path)
                                      :blocked-by (get island-blockers island [])
                                      :disposition (file-disposition path (:source source))
                                      :tests (if (test-source? path)
                                               []
                                               (get test-index (source-stem path) []))))))
                          sources)
        suite-records (->> file-records
                           (filter #(= :test (:role %)))
                           (map (fn [record]
                                  (shape/legacy-test-suite-record
                                   {:path (:path record)
                                    :island (:island record)
                                    :disposition (:disposition record)}))))
        route-records (map route-record routes)
        records (concat file-records bridge-exports route-records suite-records)]
    (->> records
         (sort-by :record/id)
         vec
         law/assert-manifest!)))
