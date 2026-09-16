(ns knoxx.frontend.law.migration-vitest-modules
  "Admission of Vitest's built-in module selectors during the migration census.")

(def ^:private reporters
  #{"default" "basic" "verbose" "dot" "json" "tap" "tap-flat" "junit"
    "hanging-process" "github-actions" "html"})

(def ^:private coverage-reporters
  #{"clover" "cobertura" "html" "html-spa" "json" "json-summary" "lcov"
    "lcovonly" "none" "teamcity" "text" "text-lcov" "text-summary"})

(def ^:private environments #{"node" "jsdom" "happy-dom" "edge-runtime"})
(def ^:private pools #{"forks" "threads" "browser" "vmThreads" "vmForks" "typescript"})

(defn- unsupported! [path field value]
  (throw (ex-info "Custom Vitest modules require explicit migration inventory support"
                  {:path path :field field :value value})))

(defn- assert-selector! [path field value builtins]
  (when (and (some? value) (not (contains? builtins value)))
    (unsupported! path field value)))

(defn- section [path settings field]
  (let [value (get settings field)]
    (cond
      (or (nil? value) (false? value)) {}
      (map? value) value
      :else (unsupported! path field value))))

(defn- reporter-names [path field value]
  (cond
    (nil? value) []
    (string? value) [value]
    (vector? value)
    (mapv (fn [entry]
            (cond
              (string? entry) entry
              (and (vector? entry) (<= 1 (count entry) 2)
                   (string? (first entry))
                   (or (= 1 (count entry)) (map? (second entry)))) (first entry)
              :else (unsupported! path field entry))) value)
    :else (unsupported! path field value)))

(defn- assert-reporters! [path field value builtins]
  (doseq [reporter (reporter-names path field value)]
    (assert-selector! path field reporter builtins)))

(defn- assert-match-selectors! [path field value builtins]
  (when (some? value)
    (when-not (and (vector? value)
                   (every? #(and (vector? %) (= 2 (count %)) (string? (first %))) value))
      (unsupported! path field value))
    (doseq [[_ selector] value]
      (assert-selector! path field selector builtins))))

(defn- assert-empty-modules! [path field value]
  (when-not (or (nil? value) (= [] value))
    (unsupported! path field value)))

(defn- assert-coverage! [path settings]
  (let [coverage (section path settings "coverage")]
    (assert-selector! path "coverage.provider" (get coverage "provider") #{"v8" "istanbul"})
    (assert-selector! path "coverage.customProviderModule" (get coverage "customProviderModule") #{})
    (assert-reporters! path "coverage.reporter" (get coverage "reporter") coverage-reporters)))

(defn- assert-pool-arguments! [path settings]
  (let [options (section path settings "poolOptions")]
    (doseq [pool ["forks" "threads" "vmForks" "vmThreads"]
            :let [pool-settings (section path options pool)]]
      (assert-empty-modules! path (str "poolOptions." pool ".execArgv")
                             (get pool-settings "execArgv")))))

(defn assert-config!
  "Admit known Vitest loaders while requiring explicit support for custom modules."
  [{:keys [path settings] :as facts}]
  (assert-reporters! path "reporters" (get settings "reporters") reporters)
  (assert-selector! path "environment" (get settings "environment") environments)
  (assert-selector! path "pool" (get settings "pool") pools)
  (assert-match-selectors! path "environmentMatchGlobs" (get settings "environmentMatchGlobs") environments)
  (assert-match-selectors! path "poolMatchGlobs" (get settings "poolMatchGlobs") pools)
  (doseq [field ["runner" "snapshotEnvironment"]]
    (assert-selector! path field (get settings field) #{}))
  (assert-empty-modules! path "snapshotSerializers" (get settings "snapshotSerializers"))
  (when-let [diff (get settings "diff")]
    (when-not (map? diff) (unsupported! path "diff" diff)))
  (assert-selector! path "sequence.sequencer" (get (section path settings "sequence") "sequencer") #{})
  (assert-selector! path "browser.provider" (get (section path settings "browser") "provider")
                    #{"webdriverio" "playwright" "none"})
  (assert-reporters! path "benchmark.reporters" (get (section path settings "benchmark") "reporters")
                     #{"default" "verbose"})
  (assert-coverage! path settings)
  (assert-pool-arguments! path settings)
  facts)
