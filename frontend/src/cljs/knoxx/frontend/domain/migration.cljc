(ns knoxx.frontend.domain.migration
  "Pure assembly of source facts into the canonical frontend migration ledger."
  (:require [clojure.string :as str]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.shape.migration :as shape]))

(defn source-stem
  "Remove TypeScript and test suffixes for sibling test association."
  [path]
  (-> path
      (str/replace #"\.(?:test|spec)\.tsx?$" "")
      (str/replace #"\.tsx?$" "")))

(defn tests-by-source
  "Index legacy test suites by their probable sibling source stem."
  [sources]
  (->> sources
       (filter (comp shape/test-source? :path))
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

(defn assemble-records
  "Build, validate, and deterministically order all manifest records."
  [{:keys [sources bridge-exports routes]}]
  (let [test-index (tests-by-source sources)
        file-records (map (fn [{:keys [path] :as source}]
                            (let [island (classify-island path)]
                              (shape/legacy-file-record
                               (assoc source
                                      :island island
                                      :blocked-by (get island-blockers island [])
                                      :disposition (file-disposition path (:source source))
                                      :tests (if (shape/test-source? path)
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
        records (concat file-records bridge-exports routes suite-records)]
    (->> records
         (sort-by :record/id)
         vec
         law/assert-manifest!)))
