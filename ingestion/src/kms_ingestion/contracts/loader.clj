(ns kms-ingestion.contracts.loader
  "Load, merge, and cache EDN contracts from the contracts/ directory.

  Precedence (low -> high):
    1. hardcoded runtime floor    (inline defaults in this ns)
    2. contracts/_defaults.edn    (global defaults)
    3. contracts/<tenant>/_defaults.edn  (tenant defaults)
    4. contracts/<tenant>/sources/<id>.edn  (source contract)
    5. per-job override map       (passed at call-site)

  Secret refs of the form :env/FOO or \"env:FOO\" are resolved
  against the process environment at load time.
  Secrets are never written to the merged contract map; they are
  replaced with the resolved string value or nil."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :as walk]))

;; ──────────────────────────────────────────────────────────────────────────────
;; Internal helpers
;; ──────────────────────────────────────────────────────────────────────────────

(defn- resolve-secret
  "Resolve a single secret ref to its environment value, or nil."
  [ref]
  (cond
    (keyword? ref)
    (let [n (name ref)]
      (when (str/starts-with? n "env/")
        (System/getenv (str/replace-first n "env/" ""))))

    (string? ref)
    (cond
      (str/starts-with? ref ":env/")
      (System/getenv (subs ref 5))
      (str/starts-with? ref "env:")
      (System/getenv (subs ref 4)))

    :else nil))

(defn- secret-ref?
  [v]
  (boolean
   (cond
     (keyword? v) (str/starts-with? (name v) "env/")
     (string?  v) (or (str/starts-with? v ":env/")
                      (str/starts-with? v "env:"))
     :else false)))

(defn resolve-secrets
  "Walk a contract map and replace all secret refs with resolved env values."
  [m]
  (walk/postwalk
   (fn [v]
     (if (secret-ref? v)
       (resolve-secret v)
       v))
   m))

(defn- deep-merge
  "Recursively merge maps. Non-map values in `b` overwrite `a`."
  [a b]
  (if (and (map? a) (map? b))
    (merge-with deep-merge a b)
    b))

(defn- read-edn-file
  "Read an EDN file from the filesystem, returning nil when absent."
  [^String path]
  (let [f (io/file path)]
    (when (.exists f)
      (edn/read-string (slurp f)))))

;; ──────────────────────────────────────────────────────────────────────────────
;; Contracts root — injectable for tests
;; ──────────────────────────────────────────────────────────────────────────────

(def ^:dynamic *contracts-dir* nil)

(defn with-contracts-dir*
  "Low-level helper; prefer the with-contracts-dir macro in tests."
  [path f]
  (binding [*contracts-dir* path] (f)))

(defmacro with-contracts-dir
  "Rebind the contracts directory for the duration of body. Test use only."
  [path & body]
  `(with-contracts-dir* ~path (fn [] ~@body)))

(defn- contracts-root
  "Locate the contracts root directory.
  Checked in order:
    - *contracts-dir* dynamic var  (test injection)
    - CONTRACTS_DIR env var
    - <cwd>/contracts
    - <cwd>/../contracts  (when running from ingestion/)"
  []
  (or *contracts-dir*
      (System/getenv "CONTRACTS_DIR")
      (let [cwd (System/getProperty "user.dir")]
        (or (let [local (str cwd "/contracts")]
              (when (.isDirectory (io/file local)) local))
            (let [parent (str cwd "/../contracts")]
              (when (.isDirectory (io/file parent)) parent))))))

;; ──────────────────────────────────────────────────────────────────────────────
;; Runtime floor
;; ──────────────────────────────────────────────────────────────────────────────

(def ^:private runtime-floor
  {:source/discovery
   {:hidden-policy      :skip
    :skip-dirs          #{"node_modules" ".git" "dist" ".next" "__pycache__" ".venv" "venv"
                          "target" ".pio" "coverage" ".pytest_cache" ".mypy_cache" "build"
                          ".gradle" ".idea" ".vscode" ".vs" "vendor" "fixtures" "__fixtures__"}
    :skip-files         #{"package-lock.json" "pnpm-lock.yaml" "yarn.lock" "Cargo.lock"
                          "poetry.lock" "composer.lock" "Gemfile.lock" "flake.lock"
                          ".DS_Store" ".env" ".env.local"}
    :skip-extensions    #{".min.js" ".min.css" ".d.ts" ".map" ".pyc" ".pyo"
                          ".so" ".dylib" ".dll" ".exe" ".bin"}
    :text-filenames     #{"dockerfile" "makefile" "justfile" "license" "copying"
                          "release" "publish" "hooks"}
    :follow-symlinks?   false}

   :contract/id      :defaults/ingestion
   :source/driver    :local
   :source/enabled?  true

   :tenant/id        "_defaults"

   :source/schedule
   {:mode                      :hybrid
    :sync-interval-minutes     30
    :scheduler-poll-ms         60000
    :passive-watch-enabled?    true
    :passive-watch-poll-ms     60000
    :passive-watch-debounce-ms 5000
    :bootstrap?                true}

   :source/semantic
   {:enabled?     true
    :build-index? true
    :chunk-size   800
    :chunk-overlap 100}

   :source/ingest
   {:batch-size        10
    :batch-parallelism 4
    :batch-delay-ms    100
    :throttle-enabled? true
    :max-load-per-core 0.85
    :throttle-sleep-ms 1000
    :retry-failed?     true}

   :source/sink
   {:type         :openplanner
    :visibility   "internal"
    :source-label "kms-ingestion"
    :created-by   "kms-ingestion"
    :language     "en"}})

;; ──────────────────────────────────────────────────────────────────────────────
;; Cache
;; ──────────────────────────────────────────────────────────────────────────────

(defonce ^:private cache (atom {}))

(defn invalidate-cache! [] (reset! cache {}))

;; ──────────────────────────────────────────────────────────────────────────────
;; Public API
;; ──────────────────────────────────────────────────────────────────────────────

(defn load-source-contract
  "Return the fully-merged, secret-resolved contract for a source.

  Args:
    tenant-id    - string, e.g. \"devel\"
    source-id    - string or keyword, e.g. \"workspace\"
    job-override - optional map of contract keys to merge last

  Returns a merged map conforming to IngestSourceContract schema,
  with secrets resolved.  Nil when no contracts directory is found."
  ([tenant-id source-id]
   (load-source-contract tenant-id source-id nil))
  ([tenant-id source-id job-override]
   (let [cache-key [tenant-id source-id]
         cached    (get @cache cache-key)]
     (if (and cached (nil? job-override))
       cached
       (let [root (contracts-root)]
         (when root
           (let [global-defaults (read-edn-file (str root "/_defaults.edn"))
                 tenant-defaults (read-edn-file (str root "/" tenant-id "/_defaults.edn"))
                 source-id-str   (if (keyword? source-id) (name source-id) source-id)
                 source-contract (read-edn-file
                                  (str root "/" tenant-id "/sources/" source-id-str ".edn"))
                 merged          (reduce deep-merge
                                         runtime-floor
                                         (filter some?
                                                 [global-defaults
                                                  tenant-defaults
                                                  source-contract
                                                  job-override]))
                 resolved        (resolve-secrets merged)]
             (when-not job-override
               (swap! cache assoc cache-key resolved))
             resolved)))))))

(defn load-all-source-contracts
  "Load all source contracts under contracts/<tenant>/sources/.
   Returns a map of source-id-string -> merged contract."
  [tenant-id]
  (let [root (contracts-root)]
    (when root
      (let [sources-dir (io/file (str root "/" tenant-id "/sources"))]
        (when (.isDirectory sources-dir)
          (into {}
                (for [^java.io.File f (.listFiles sources-dir)
                      :when (str/ends-with? (.getName f) ".edn")]
                  (let [source-id (str/replace (.getName f) #"\.edn$" "")]
                    [source-id (load-source-contract tenant-id source-id)]))))))))
