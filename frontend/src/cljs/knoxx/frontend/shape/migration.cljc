(ns knoxx.frontend.shape.migration
  "Pure classification and record construction for the frontend strangler
   manifest. This namespace knows source shapes; it performs no filesystem I/O."
  (:require [clojure.string :as str]))

(def legacy-source-pattern
  "The production tree governed by the TypeScript non-growth ratchet."
  #"^frontend/src/.*\.tsx?$")

(def test-source-pattern
  "Pattern identifying governed Vitest source paths."
  #"\.(?:test|spec)\.tsx?$")

(def island-rules
  "Ordered source-path rules. First match wins, making every classification
   deterministic and reviewable rather than a second hand-maintained list."
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

(def heavy-widget-pattern
  "Legacy widgets whose libraries should stay behind thin Helix adapters."
  #"(?:EdnEditor|VisualEditor|GraphExplorer|VectorsPage|DataPage|MusicPlayerView)")

(defn normalize-path
  "Return a repository path with POSIX separators."
  [path]
  (str/replace path "\\" "/"))

(defn test-source?
  "Whether a governed TypeScript path is a Vitest suite."
  [path]
  (boolean (re-find test-source-pattern path)))

(defn classify-island
  "Assign one behavior island from the ordered path rules."
  [path]
  (or (some (fn [[pattern island]]
              (when (re-find pattern path) island))
            island-rules)
      (throw (ex-info "No migration island rule matches governed path"
                      {:path path}))))

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

(defn file-disposition
  "Choose the intended terminal action for a legacy file. Loader shims and
   bridge modules disappear; heavy widgets keep only a CLJS-owned adapter."
  [path source]
  (cond
    (str/includes? path "/bridge/") :delete
    (and (test-source? path) (str/includes? source "window.knoxx.frontend")) :delete
    (str/includes? source "window.knoxx.frontend") :delete
    (re-find heavy-widget-pattern path) :wrap
    :else :port))

(defn legacy-file-record
  "Construct one file record from repository-relative path and source text."
  [{:keys [path source bridge tests]}]
  (let [path (normalize-path path)
        kind (if (str/ends-with? path ".tsx") :tsx :ts)
        island (classify-island path)]
    (cond-> {:record/id (str "file:" path)
             :path path
             :kind kind
             :island island
             :role (file-role path)
             :disposition (file-disposition path source)
             :status :legacy
             :tests (vec (sort tests))
             :blocked-by (get island-blockers island [])}
      bridge (assoc :bridge bridge))))

(defn bridge-export-record
  "Construct one record for a named compatibility-bridge export."
  [{:keys [bridge path source] :as attributes}]
  (let [export-name (:symbol attributes)]
    {:record/id (str "bridge:" (name bridge) ":" export-name)
     :path (normalize-path path)
     :kind :bridge-export
     :bridge bridge
     :symbol export-name
     :source source
     :status :legacy}))

(defn route-record
  "Construct one record for a Shadow-owned route and its current implementation."
  [{:keys [path route implementation legacy?]}]
  {:record/id (str "route:" route)
   :path (normalize-path path)
   :kind :route
   :route route
   :implementation implementation
   :status (if legacy? :legacy :native)})

(defn legacy-test-suite-record
  "Construct the explicit retirement record for one Vitest suite."
  [{:keys [path island disposition]}]
  {:record/id (str "legacy-test-suite:" (normalize-path path))
   :path (normalize-path path)
   :kind :legacy-test-suite
   :island island
   :disposition disposition
   :status :legacy})

(defn migration-surface-path?
  "Whether a changed path makes a pull request part of the frontend migration."
  [path]
  (boolean
   (or (re-find #"^frontend/src/.*\.(?:ts|tsx|cljs|cljc)$" path)
       (re-find #"^frontend/(?:migration/|shadow-cljs\.edn$|package\.json$)" path)
       (= path ".github/workflows/ci.yml"))))
