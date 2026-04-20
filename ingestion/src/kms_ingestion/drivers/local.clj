(ns kms-ingestion.drivers.local
  "Local filesystem driver for ingestion."
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [kms-ingestion.contracts.resolve :as cr]
   [kms-ingestion.drivers.protocol :as protocol])
  (:import
   [java.nio.file Files]
   [java.security MessageDigest]
   [java.time Instant]))

;;; ---- config helpers -------------------------------------------------------

(defn- get-root-path [config]
  (or (:root-path config)
      (:root_path config)
      (throw (ex-info "Missing root_path in driver config" {:config config}))))

(defn- contract-or-default [contract resolver default-value]
  (let [v (resolver contract)]
    (if (or (nil? v) (and (coll? v) (empty? v)))
      default-value
      v)))

;;; ---- file classification --------------------------------------------------

(def default-text-extensions
  #{".md" ".markdown" ".txt" ".rst" ".org" ".adoc"
    ".mdx" ".astro"
    ".json" ".jsonl" ".yaml" ".yml" ".toml" ".ini" ".cfg" ".conf" ".env"
    ".http" ".graphql" ".gql" ".proto" ".ipynb"
    ".xml" ".csv" ".tsv"
    ".html" ".htm" ".css" ".js" ".jsx" ".ts" ".tsx" ".mjs" ".cjs" ".cts" ".mts"
    ".py" ".rb" ".php" ".java" ".kt" ".go" ".rs" ".c" ".cc" ".cpp" ".h" ".hpp"
    ".clj" ".cljs" ".cljc" ".edn" ".sql" ".sh" ".bash" ".zsh" ".fish" ".el" ".lisp" ".scm"
    ".tex" ".bib" ".nix" ".dockerfile" ".gradle" ".properties" ".patch" ".diff" ".traffic"})

(def default-text-filenames
  #{"dockerfile" "makefile" "justfile" "license" "copying" "release" "publish" "hooks"})

(def ^:private default-skip-extensions
  #{".min.js" ".min.css" ".d.ts" ".map" ".pyc" ".pyo"
    ".so" ".dylib" ".dll" ".exe" ".bin"})

(defn text-like-file?
  ([path] (text-like-file? path nil))
  ([path contract]
   (let [name            (str/lower-case (str (fs/file-name path)))
         ext-raw         (str/lower-case (or (fs/extension path) ""))
         ext             (if (str/starts-with? ext-raw ".") ext-raw (str "." ext-raw))
         skip-extensions (contract-or-default contract cr/skip-extensions default-skip-extensions)
         text-extensions (contract-or-default contract cr/text-extensions default-text-extensions)
         text-filenames  (contract-or-default contract cr/text-filenames  default-text-filenames)]
     (boolean
      (and (not (some #(str/ends-with? name %) skip-extensions))
           (or (text-extensions ext)
               (text-filenames name)
               (= name "dockerfile")
               (str/ends-with? name ".env.example")
               (str/ends-with? name ".service")
               (str/ends-with? name ".desktop")))))))

(defn path-matches-glob? [filename pattern]
  (cond
    (.startsWith pattern "*.")  (str/ends-with? filename (subs pattern 1))
    (.startsWith pattern "**/") (str/ends-with? filename (subs pattern 3))
    (.endsWith   pattern "/*")  (str/starts-with? filename (subs pattern 0 (- (count pattern) 1)))
    :else                       (= filename pattern)))

(def ^:private hardcoded-skip-dirs
  #{"node_modules" ".git" "dist" ".next" "__pycache__" ".venv" "venv"
    "target" ".pio" "coverage" ".pytest_cache" ".mypy_cache" "build"
    ".gradle" ".idea" ".vscode" ".vs" "vendor" "fixtures" "__fixtures__"})

(defn skip-directory-name?
  ([name] (skip-directory-name? name nil))
  ([name contract]
   (let [skip-dirs     (if (nil? contract)
                         hardcoded-skip-dirs
                         (contract-or-default contract cr/skip-dirs hardcoded-skip-dirs))
         hidden-policy (cr/hidden-policy contract)]
     (or (and (= hidden-policy :skip)
              (str/starts-with? name ".")
              (not= name ".github"))
         (boolean (skip-dirs name))))))

(defn- candidate-file? [rel-path f-name ext file-types include-patterns exclude-patterns abs-path contract]
  (let [skip-dirs       (contract-or-default contract cr/skip-dirs       hardcoded-skip-dirs)
        skip-files      (contract-or-default contract cr/skip-files      #{})
        skip-extensions (contract-or-default contract cr/skip-extensions #{})]
    (not
     (or (and (= (cr/hidden-policy contract) :skip)
              (some #(str/starts-with? % ".") (str/split rel-path #"/")))
         (some skip-dirs       (str/split rel-path #"/"))
         (skip-files      f-name)
         (skip-extensions ext)
         (and (seq include-patterns)
              (not (some #(path-matches-glob? rel-path %) include-patterns)))
         (and (seq exclude-patterns)
              (some #(path-matches-glob? rel-path %) exclude-patterns))
         (and (seq file-types)
              (not (some #(path-matches-glob? f-name (str "*" %)) file-types)))
         (and (empty? file-types)
              (not (text-like-file? abs-path contract)))))))

;;; ---- crypto / time --------------------------------------------------------

(defn sha256 [path]
  (try
    (let [digest     (MessageDigest/getInstance "SHA-256")
          bytes      (Files/readAllBytes (fs/path path))
          hash-bytes (.digest digest bytes)]
      (format "%064x" (BigInteger. 1 hash-bytes)))
    (catch Exception _ nil)))

(defn modified-time [path]
  (try
    (-> (Files/getLastModifiedTime (fs/path path) (make-array java.nio.file.LinkOption 0))
        (.toInstant))
    (catch Exception _ nil)))

;;; ---- walker ---------------------------------------------------------------

(defn- file-seq-recursive
  ([^java.io.File root] (file-seq-recursive root nil))
  ([^java.io.File root contract]
   (if-not (.exists root)
     ()
     (letfn [(walk [stack]
               (lazy-seq
                (when-let [^java.io.File entry (first stack)]
                  (let [rest-stack (rest stack)
                        name       (.getName entry)]
                    (cond
                      (.isDirectory entry)
                      (if (skip-directory-name? name contract)
                        (walk rest-stack)
                        (let [children (->> (or (.listFiles entry) (into-array java.io.File []))
                                            (sort-by #(.getName ^java.io.File %))
                                            reverse)]
                          (walk (concat children rest-stack))))

                      (.isFile entry)
                      (cons entry (walk rest-stack))

                      :else
                      (walk rest-stack))))))]
       (walk (list root))))))

;;; ---- file descriptor helpers ----------------------------------------------

(defn- file->descriptor
  "Build a path/name/ext map for a File relative to root-abs."
  [^java.io.File root-abs ^java.io.File file]
  (let [abs-path (.getAbsolutePath file)
        rel-path (str (.relativize (.toPath root-abs) (.toPath file)))
        f-name   (.getName file)
        dot      (.lastIndexOf f-name ".")
        ext      (if (>= dot 0) (subs f-name dot) "")]
    {:abs-path abs-path
     :rel-path rel-path
     :f-name   f-name
     :ext      ext}))

(defn- changed-entry?
  "Return nil when file matches existing state (size+mtime), else a change map."
  [{:keys [abs-path rel-path]} existing-state]
  (let [mod-time      (modified-time abs-path)
        size          (.length (java.io.File. abs-path))
        existing      (get existing-state abs-path)
        existing-meta (:metadata existing)
        same?         (and existing
                           (= (:size existing-meta) size)
                           (= (str (:modified_at existing-meta)) (some-> mod-time str)))]
    (when-not same?
      {:id          abs-path
       :path        rel-path
       :size        size
       :modified-at mod-time
       :change      (if existing :changed :new)})))

;;; ---- public API -----------------------------------------------------------

(defn stream-files
  "Lazy stream of new/changed file entries. No content reading."
  [root-path opts]
  (when-not (or (nil? root-path) (str/blank? (str root-path)))
    (let [root-abs         (.getAbsoluteFile (java.io.File. (str root-path)))
          existing-state   (:existing-state opts {})
          file-types       (:file-types opts)
          include-patterns (:include-patterns opts)
          exclude-patterns (:exclude-patterns opts)
          contract         (:contract opts)]
      (when (.exists root-abs)
        (->> (file-seq-recursive root-abs contract)
             (map    #(file->descriptor root-abs %))
             (filter #(candidate-file? (:rel-path %) (:f-name %) (:ext %)
                                       file-types include-patterns exclude-patterns
                                       (:abs-path %) contract))
             (keep   #(changed-entry? % existing-state)))))))

(defn snapshot-files
  "Map of rel-path -> entry for passive watch diffing."
  [root-path opts]
  (if (or (nil? root-path) (str/blank? (str root-path)))
    {}
    (let [root-abs         (.getAbsoluteFile (java.io.File. (str root-path)))
          file-types       (:file-types opts)
          include-patterns (:include-patterns opts)
          exclude-patterns (:exclude-patterns opts)
          contract         (:contract opts)]
      (if-not (.exists root-abs)
        {}
        (->> (file-seq-recursive root-abs contract)
             (map    #(file->descriptor root-abs %))
             (filter #(candidate-file? (:rel-path %) (:f-name %) (:ext %)
                                       file-types include-patterns exclude-patterns
                                       (:abs-path %) contract))
             (reduce (fn [acc {:keys [rel-path abs-path]}]
                       (assoc acc rel-path
                              {:id          abs-path
                               :path        rel-path
                               :size        (.length (java.io.File. abs-path))
                               :modified-at (modified-time abs-path)}))
                     {}))))))

(defn- classify-file
  "Return [:new|:changed|:unchanged entry-or-nil] for a descriptor."
  [{:keys [abs-path rel-path]} existing-state]
  (let [mod-time      (modified-time abs-path)
        size          (.length (java.io.File. abs-path))
        existing      (get existing-state abs-path)
        existing-meta (:metadata existing)
        same?         (and existing
                           (= (:size existing-meta) size)
                           (= (str (:modified_at existing-meta)) (some-> mod-time str)))
        entry         {:id abs-path :path rel-path :size size :modified-at mod-time}]
    (cond
      same?    [:unchanged nil]
      existing [:changed   entry]
      :else    [:new       entry])))

(defn find-files
  "Eagerly walk root-path and return stats + file list."
  [root-path opts]
  (if (or (nil? root-path) (str/blank? (str root-path)))
    {:total-files 0 :new-files 0 :changed-files 0 :unchanged-files 0 :skipped-files 0 :files []}
    (let [root-abs         (.getAbsoluteFile (java.io.File. (str root-path)))
          existing-state   (:existing-state opts {})
          file-types       (:file-types opts)
          include-patterns (:include-patterns opts)
          exclude-patterns (:exclude-patterns opts)
          contract         (:contract opts)]
      (if-not (.exists root-abs)
        {:total-files 0 :new-files 0 :changed-files 0 :unchanged-files 0 :skipped-files 0 :files []}
        (let [descriptors (map #(file->descriptor root-abs %)
                               (file-seq-recursive root-abs contract))
              {candidates true skipped false}
              (group-by #(candidate-file? (:rel-path %) (:f-name %) (:ext %)
                                          file-types include-patterns exclude-patterns
                                          (:abs-path %) contract)
                        descriptors)
              classified  (map #(classify-file % existing-state) (or candidates []))
              counts      (frequencies (map first classified))]
          {:total-files     (count candidates)
           :new-files       (get counts :new       0)
           :changed-files   (get counts :changed   0)
           :unchanged-files (get counts :unchanged 0)
           :skipped-files   (count skipped)
           :files           (vec (keep second classified))})))))

;;; ---- content --------------------------------------------------------------

(defn read-file-content [path]
  (try (slurp path :encoding "UTF-8") (catch Exception _ nil)))

;;; ---- driver record --------------------------------------------------------

(defrecord LocalDriver [config state]
  protocol/Driver
  (discover [_this opts]
    (find-files (get-root-path config) opts))

  (extract [_this file-id]
    (let [content      (read-file-content file-id)
          content-hash (when content (sha256 file-id))
          path         (str (.relativize (fs/path (get-root-path config)) (fs/path file-id)))]
      {:id           file-id
       :path         path
       :content      content
       :content-hash content-hash}))

  (extract-batch [this file-ids]
    (mapv (partial protocol/extract this) file-ids))

  (get-state [_this]
    {:root-path (get-root-path config)
     :last-scan (str (Instant/now))})

  (set-state [_this new-state]
    (reset! state new-state))

  (close [_this] nil))

(defn create-driver [config]
  (->LocalDriver config (atom {})))
