(ns knoxx.frontend.law.migration
  "Malli contracts and monotonicity laws for the frontend migration ledger."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]))

(def legacy-source-pattern
  "The production tree governed by the TypeScript non-growth ratchet."
  #"^frontend/src/[\s\S]*\.(?:tsx?|mts|cts)$")

(defn assert-governed-extension!
  "Reject JavaScript source variants that the TypeScript ledger cannot count."
  [path]
  (when (re-find #"\.(?:jsx?|mjs|cjs)$" path)
    (throw (ex-info "Ungoverned JavaScript source in migration source tree"
                    {:path path})))
  path)

(defn contained-path?
  "Whether platform-relative path facts remain inside their source root."
  [{:keys [relative-path absolute? separator]}]
  (and (not absolute?)
       (not= relative-path "..")
       (not (str/starts-with? relative-path (str ".." separator)))))

(defn assert-source-root!
  "Admit only a real directory as the governed source root."
  [{:keys [path symbolic-link? directory?] :as facts}]
  (when (or symbolic-link? (not directory?))
    (throw (ex-info "Unsafe migration source root" {:path path})))
  facts)

(defn assert-source-symlink!
  "Admit only symbolic links to regular files inside the source root."
  [{:keys [path target file?] :as facts}]
  (when-not (and file? (contained-path? target))
    (throw (ex-info "Unsafe symbolic link in migration source tree" {:path path})))
  facts)

(defn assert-route-location!
  "Require parsed route declarations to remain in the supported application file."
  [{:keys [path supported-path route-count] :as facts}]
  (when (and (pos? route-count) (not= path supported-path))
    (throw (ex-info "Unsupported Shadow route location"
                    {:path path :supported-path supported-path})))
  facts)

(defn assert-project-macros!
  "Require explicit inventory support before project macros can generate frontend code."
  [{:keys [path definitions imports]}]
  (when (or (seq definitions) (seq imports))
    (throw (ex-info "Project macros require explicit migration inventory support"
                    {:path path :definitions definitions :imports imports}))))

(defn assert-import-target!
  "Admit a contained resolved import target and preserve its source evidence."
  [{:keys [target] :as facts}]
  (when-not (contained-path? facts)
    (throw (ex-info "Local import leaves governed frontend source tree"
                    (select-keys facts [:path :source :target]))))
  target)

(defn assert-vite-alias-target!
  "Admit an alias whose lexical and available canonical targets stay contained."
  [{:keys [target lexical canonical] :as facts}]
  (when (or (not (contained-path? lexical))
            (and canonical (not (contained-path? canonical))))
    (throw (ex-info "Vite alias leaves governed frontend source tree"
                    (select-keys facts [:path :target]))))
  target)

(defn assert-vite-entry!
  "Require a resolved bridge entry to match its governed inventory target."
  [{:keys [expected-target actual] :as facts}]
  (when-not (= expected-target actual)
    (throw (ex-info "Vite bridge entry leaves governed inventory"
                    (select-keys facts [:path :expected :actual]))))
  actual)

(defn assert-vite-script-configs!
  "Admit resolved Vite script configs against their governed names and presence."
  [{:keys [path script-name expected-config allowed-configs configs] :as facts}]
  (let [paths (mapv :config-path configs)
        detail (or (when (and expected-config (not= [expected-config] paths))
                     (str "Bridge build script must use its governed config: " script-name))
                   (some (fn [{:keys [config-path] config-present? :exists?}]
                           (cond
                             (not (contains? allowed-configs config-path))
                             (str "Active Vite build config leaves governed inventory: " config-path)
                             (not config-present?)
                             (str "Active Vite build config is missing: " config-path)))
                         configs))]
    (when detail
      (throw (ex-info "Unsupported Vite migration configuration" {:path path :detail detail}))))
  facts)

(defn- assert-vite-options! [path options]
  (let [contracts {:output {"globals" #(and (map? %) (every? string? (mapcat identity %)))
                            "inlineDynamicImports" boolean? "plugins" (constantly true)}
                   :esbuild {"jsxDev" boolean?}
                   :worker {"plugins" (constantly true)}}]
    (doseq [[placement settings] options
            [field value] settings]
      (when-not (when-let [accept? (get-in contracts [placement field])] (accept? value))
        (throw (ex-info "Unsupported Vite migration configuration"
                        {:path path :detail "Uninspected Vite code-producing options are not supported"
                         :placement placement :field field}))))))

(defn assert-vite-config-admission!
  "Reject root overrides and source hooks outside the inspected framework configuration."
  [{:keys [path root-override? plugin-lists options] :as facts}]
  (when root-override?
    (throw (ex-info "Unsupported Vite migration configuration"
                    {:path path :detail "Vite root overrides are not supported"})))
  (assert-vite-options! path options)
  (doseq [{:keys [placement static? plugins]} plugin-lists]
    (when-not (and static?
                   (every? #(and (= placement :vite)
                                  (= "@vitejs/plugin-react" (:factory-module %))
                                  (= 0 (:argument-count %))) plugins))
      (throw (ex-info "Unsupported Vite migration configuration"
                      {:path path :detail "Uninspected Vite plugins are not supported"
                       :placement placement}))))
  facts)

(defn assert-vitest-config-admission!
  "Reject parsed Vitest fields that can redirect uninspected source loading."
  [{:keys [path root-fields test-fields] :as facts}]
  (when-not (every? #{"test" "cacheDir"} root-fields)
    (throw (ex-info "Unsupported Vitest migration configuration"
                    {:path path :detail "Root overrides and uninspected Vite configuration fields are not supported"})))
  (when (some #{"root" "dir" "workspace" "projects" "typecheck" "alias"} test-fields)
    (throw (ex-info "Unsupported Vitest migration configuration"
                    {:path path :detail "Root, directory, project, workspace, typecheck and alias overrides are not supported"})))
  facts)

(defn assert-vitest-scope!
  "Require contained Vitest scopes and suite patterns counted by the census."
  [file field scope]
  (let [relative (str/replace scope #"^(?:\./)+" "")]
    (when-not (and (str/starts-with? relative "src/")
                   (not (str/includes? relative ".."))
                   (not (str/includes? relative "\\")))
      (throw (ex-info "Vitest source scope leaves governed frontend source tree"
                      {:path file :field field :scope scope})))
    (when (and (contains? #{"include" "includeSource"} field)
               (not (re-find #"\.(?:test|spec|\{(?:test|spec)(?:,(?:test|spec))*\})\.(?:tsx?|mts|cts|\{(?:tsx?|mts|cts)(?:,(?:tsx?|mts|cts))*\})$" relative)))
      (throw (ex-info "Vitest suite scope leaves governed filename inventory"
                      {:path file :field field :scope scope})))))

(defn assert-shadow-bridge-resolutions!
  "Require every active Shadow bridge resolution to use its governed Vite output."
  [{:keys [path resolutions] :as facts}]
  (let [expected {"@open-hax/knoxx-frontend-bridge"
                  {:target :file :file "dist/bridge/knoxx-frontend-bridge.es.js"}
                  "@open-hax/knoxx-app-bridge"
                  {:target :file :file "dist/bridge/knoxx-app-bridge.es.js"}}]
    (doseq [[module resolution] resolutions :when (contains? expected module)]
      (when-not (= (get expected module) resolution)
        (throw (ex-info "Shadow bridge resolution leaves governed inventory"
                        {:path path :module module :expected (get expected module)
                         :actual resolution})))))
  facts)

(defn required-bridge-builds
  "Derive governed build obligations from active Shadow bridge resolutions."
  [resolutions]
  (->> [["@open-hax/knoxx-frontend-bridge" "build:bridge"]
        ["@open-hax/knoxx-app-bridge" "build:app-bridge"]]
       (keep (fn [[module script-name]]
               (when (contains? resolutions module) script-name)))
       vec))

(defn assert-production-build!
  "Validate script presence and bridge ordering in parsed production build facts."
  [{:keys [phases required-bridges available-scripts] :as facts}]
  (when-not (every? available-scripts required-bridges)
    (throw (ex-info "Unsupported Vite migration configuration"
                    (assoc facts :detail "Active Shadow bridges require their governed build scripts"))))
  (let [bridges (take-while #{"build:bridge" "build:app-bridge"} phases)
        remaining (vec (drop (count bridges) phases))]
    (when-not (and (contains? #{[:shadow] [:shadow :css] [:html :shadow] [:html :shadow :css]} remaining)
                   (= (count bridges) (count (set bridges)))
                   (every? (set bridges) required-bridges))
      (throw (ex-info "Unsupported Vite migration configuration"
                      (assoc facts :detail
                             "Production build must compile every active bridge before Shadow release")))))
  facts)

(def NonBlankString
  "Schema for nonempty manifest string values."
  [:string {:min 1}])
(def Bridge
  "Schema for compatibility-bridge identities."
  [:enum :frontend :app])
(def Island
  "Schema for migration behavior-island identities."
  [:enum :agent-audit :auth :bridge :broadcast-studio :chat-workspace :cms
   :components :contracts :data :event-agents :layout :ops :routes :shared
   :test-infrastructure :translations :workspace])
;; Reserved for generated TypeScript declarations explicitly admitted by the
;; final-state law; no such file exists in the current inventory.
(def Disposition
  "Schema for the terminal action assigned to a legacy record."
  [:enum :port :wrap :delete :retain-generated])
(def Status
  "Schema for current route or source ownership."
  [:enum :legacy :native])

(def LegacyFileRecord
  "Closed schema for one governed TypeScript source file."
  [:map {:closed true}
   [:record/id NonBlankString]
   [:path NonBlankString]
   [:kind [:enum :ts :tsx]]
   [:island Island]
   [:role [:enum :test :bridge :library :route :component :support]]
   [:disposition Disposition]
   [:bridge {:optional true} Bridge]
   [:status [:= :legacy]]
   [:tests [:vector string?]]
   [:blocked-by [:vector keyword?]]])

(def BridgeExportRecord
  "Closed schema for one compatibility-bridge export."
  [:map {:closed true}
   [:record/id NonBlankString]
   [:path NonBlankString]
   [:kind [:= :bridge-export]]
   [:bridge Bridge]
   [:symbol NonBlankString]
   [:source NonBlankString]
   [:status [:= :legacy]]])

(def RouteRecord
  "Closed schema for one Shadow-owned application route."
  [:map {:closed true}
   [:record/id NonBlankString]
   [:path NonBlankString]
   [:kind [:= :route]]
   [:route NonBlankString]
   [:implementation NonBlankString]
   [:status Status]])

(def LegacyTestSuiteRecord
  "Closed schema for one Vitest retirement record."
  [:map {:closed true}
   [:record/id NonBlankString]
   [:path NonBlankString]
   [:kind [:= :legacy-test-suite]]
   [:island Island]
   [:disposition Disposition]
   [:status [:= :legacy]]])

(def ManifestRecord
  "Closed union schema for every migration-ledger record."
  [:multi {:dispatch :kind}
   [:ts LegacyFileRecord]
   [:tsx LegacyFileRecord]
   [:bridge-export BridgeExportRecord]
   [:route RouteRecord]
   [:legacy-test-suite LegacyTestSuiteRecord]])

(defn assert-record!
  "Return a valid manifest record or throw with humanized schema evidence."
  [record]
  (if (m/validate ManifestRecord record)
    record
    (throw (ex-info "Frontend migration record violates its contract"
                    {:record record
                     :errors (me/humanize (m/explain ManifestRecord record))}))))

(defn assert-manifest!
  "Validate every record and require stable unique record identities."
  [records]
  (run! assert-record! records)
  (let [ids (map :record/id records)]
    (when-not (= (count ids) (count (set ids)))
      (throw (ex-info "Frontend migration record identities must be unique"
                      {:duplicate-ids (->> ids frequencies
                                           (keep (fn [[id n]] (when (> n 1) id)))
                                           sort vec)}))))
  records)

(defn- records-of-kind [records kinds]
  (filter #(contains? kinds (:kind %)) records))

(defn- ids-of-kind [records kinds]
  (->> (records-of-kind records kinds) (map :record/id) set))

(defn- count-kind [records kind]
  (count (filter #(= kind (:kind %)) records)))

(defn legacy-surface-count
  "The migration surface whose monotonic decrease makes a behavior slice real."
  [records]
  (count (filter #(or (contains? #{:ts :tsx :bridge-export :legacy-test-suite}
                                 (:kind %))
                      (and (= :route (:kind %)) (= :legacy (:status %))))
                 records)))

(defn migration-surface-path?
  "Whether a changed path activates the migration progress contract."
  [path]
  (boolean
   (or (re-find legacy-source-pattern path)
       (re-find #"^frontend/src/[\s\S]*\.(?:cljs|cljc)$" path)
       (re-find #"^frontend/(?:migration/|public/|index\.html$|shadow-cljs\.edn$|package\.json$)" path)
       (contains? #{"frontend/.clj-kondo/config.edn"
                    "scripts/pre-push-checks.sh"
                    "scripts/lint-frontend-cljs-changed.sh"} path)
       (= path ".github/workflows/ci.yml"))))

(defn assert-vitest-runners!
  "Require governed test entrypoints to use direct runners, or retire their config together."
  [{:keys [path config-present? runners] :as facts}]
  (let [governed #{"test" "test:coverage" "test:watch"}
        present (filter governed (keys runners))]
    (doseq [[script-name {:keys [direct? mentions-vitest?]}] runners
            :when (or (contains? governed script-name) mentions-vitest?)]
      (when-not (and direct? config-present?)
        (throw (ex-info "Unsupported Vitest migration configuration"
                        {:path path :script script-name
                         :detail "Governed test scripts require the direct runner and its existing config"}))))
    (when (and config-present? (empty? present))
      (throw (ex-info "Unsupported Vitest migration configuration"
                      {:path path :detail "Retire the Vitest config and governed test scripts together"}))))
  facts)

(defn- ratchet-context
  [{:keys [baseline current changed-paths infrastructure?]}]
  (let [file-kinds #{:ts :tsx}
        baseline-files (ids-of-kind baseline file-kinds)
        current-files (ids-of-kind current file-kinds)
        baseline-exports (ids-of-kind baseline #{:bridge-export})
        current-exports (ids-of-kind current #{:bridge-export})
        baseline-native-routes (ids-of-kind
                                (filter #(= :native (:status %)) baseline)
                                #{:route})
        current-legacy-routes (ids-of-kind
                               (filter #(= :legacy (:status %)) current)
                               #{:route})]
    {:ts-before (count-kind baseline :ts)
     :ts-after (count-kind current :ts)
     :tsx-before (count-kind baseline :tsx)
     :tsx-after (count-kind current :tsx)
     :added-files (sort (set/difference current-files baseline-files))
     :added-exports (sort (set/difference current-exports baseline-exports))
     :added-legacy-routes (sort (set/difference current-legacy-routes
                                               (ids-of-kind baseline #{:route})))
     :regressed-routes (sort (set/intersection baseline-native-routes
                                               current-legacy-routes))
     :touched? (some migration-surface-path? changed-paths)
     :before (legacy-surface-count baseline)
     :after (legacy-surface-count current)
     :infrastructure? infrastructure?}))

(defn- route-identity-violations
  "Reject backslides without evaluating route expressions or trusting legacy renames."
  [{:keys [regressed-routes added-legacy-routes]}]
  (cond-> []
    (seq regressed-routes)
    (conj {:law :native-routes/no-regression :routes regressed-routes})
    ;; IDs retain source expressions, so equivalent URL values are unproven.
    ;; New legacy IDs, including ambiguous legacy renames, must fail closed.
    (seq added-legacy-routes)
    (conj {:law :legacy-routes/no-new-identities :routes added-legacy-routes})))

(defn ratchet-violations
  "Return regressions; touched migration surfaces must shrink or declare infrastructure."
  [inputs]
  (let [{:keys [ts-before ts-after tsx-before tsx-after added-files
                added-exports touched? before after infrastructure?]
         :as context} (ratchet-context inputs)
        route-violations (route-identity-violations context)]
    (cond-> []
      (> ts-after ts-before)
      (conj {:law :typescript-count/non-growth
             :before ts-before :after ts-after})

      (> tsx-after tsx-before)
      (conj {:law :tsx-count/non-growth
             :before tsx-before :after tsx-after})

      (seq added-files)
      (conj {:law :legacy-source/no-new-paths :added added-files})

      (seq added-exports)
      (conj {:law :bridge-exports/monotonic :added added-exports})

      (seq route-violations)
      (into route-violations)

      (and touched? (>= after before) (not infrastructure?))
      (conj {:law :migration-slice/must-progress
             :before before
             :after after
             :declaration "Add `Migration infrastructure: yes` to the PR body only for infrastructure work."}))))

(def NativeSourceCounts
  "Closed evidence shape for the revision-relative native CLJS floor."
  [:map {:closed true} [:before nat-int?] [:after nat-int?]])

(defn assert-native-source-counts!
  "Reject a CLJS count decrease; infrastructure declarations do not waive this law."
  [{:keys [before after] :as counts}]
  (when-not (m/validate NativeSourceCounts counts)
    (throw (ex-info "Invalid native CLJS count evidence" {:counts counts})))
  (when (< after before)
    (throw (ex-info "Native CLJS source count regressed"
                    (assoc counts :law :native-source/non-regression))))
  counts)
