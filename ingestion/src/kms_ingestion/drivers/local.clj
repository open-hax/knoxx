(ns kms-ingestion.drivers.local
  "Local filesystem driver for ingestion."
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [kms-ingestion.contracts.resolve :as cr]
   [kms-ingestion.drivers.protocol :as protocol])
  (:import
   [java.nio.file Files]
   [java.security MessageDigest]
   [java.time Instant]))

(defn- get-root-path
  "Extract root-path from config, handling both :root-path and :root_path keys."
  [config]
  (or (:root-path config)
      (:root_path config)
      (throw (ex-info "Missing root_path in driver config" {:config config}))))

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

(defn- contract-or-default
  [contract resolver default-value]
  (let [v (resolver contract)]
    (if (or (nil? v)
            (and (coll? v) (empty? v)))
      default-value
      v)))

(defn text-like-file?
  "Default heuristic for files we can safely treat as text documents."
  ([path]
   (text-like-file? path nil))
  ([path contract]
   (let [name      (str/lower-case (str (fs/file-name path)))
         ext-raw   (str/lower-case (or (fs/extension path) ""))
         ext       (if (str/starts-with? ext-raw ".") ext-raw (str "." ext-raw))
         text-extensions (contract-or-default contract cr/text-extensions default-text-extensions)
         text-filenames  (contract-or-default contract cr/text-filenames  default-text-filenames)]
     (or (text-extensions ext)
         (text-filenames name)
         (= name "dockerfile")
         (str/ends-with? name ".env.example")
         (str/ends-with? name ".service")
         (str/ends-with? name ".desktop")))))

(defn path-matches-glob?
  "Check if a filename matches a simple glob pattern like *.md."
  [filename pattern]
  (cond
    (.startsWith pattern "*.")
    (str/ends-with? filename (subs pattern 1))

    (.startsWith pattern "**/")
    (str/ends-with? filename (subs pattern 3))

    :else
    (= filename pattern)))

(defn skip-directory-name?
  ([name]
   (skip-directory-name? name nil))
  ([name contract]
   (let [skip-dirs     (contract-or-default contract cr/skip-dirs #{})
         hidden-policy (cr/hidden-policy contract)]
     (or (and (= hidden-policy :skip)
              (str/starts-with? name ".")
              (not= name ".github"))
         (skip-dirs name)))))

(defn sha256
  "Compute SHA-256 hash of a file."
  [path]
  (try
    (let [digest     (MessageDigest/getInstance "SHA-256")
          bytes      (Files/readAllBytes (fs/path path))
          hash-bytes (.digest digest bytes)]
      (format "%064x" (BigInteger. 1 hash-bytes)))
    (catch Exception _ nil)))

(defn modified-time
  "Get file modification time as Instant."
  [path]
  (try
    (-> (Files/getLastModifiedTime (fs/path path) (make-array java.nio.file.LinkOption 0))
        (.toInstant))
    (catch Exception _ nil)))

(defn- file-seq-recursive
  "Lazy depth-first file walk that skips hidden/generated dirs before descent."
  ([^java.io.File root]
   (file-seq-recursive root nil))
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

(defn- candidate-file?
  [rel-path f-name ext file-types include-patterns exclude-patterns abs-path contract]
  (let [skip-dirs       (contract-or-default contract cr/skip-dirs       #{})
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

(defn stream-files
  "Lazy stream of new/changed file metadata based on size+mtime first.
   Does not read file contents or hash unchanged files during discovery."
  [root-path opts]
  (if (or (nil? root-path) (str/blank? (str root-path)))
    ()
    (let [root-file        (java.io.File. (str root-path))
          root-abs         (.getAbsoluteFile root-file)
          existing-state   (:existing-state opts)
          file-types       (:file-types opts)
          include-patterns (:include-patterns opts)
          exclude-patterns (:exclude-patterns opts)
          contract         (:contract opts)]
      (when (.exists root-abs)
        (->> (file-seq-recursive root-abs contract)
             (keep
              (fn [^java.io.File file]
                (let [abs-path (.getAbsolutePath file)
                      rel-path (str (.relativize (.toPath root-abs) (.toPath file)))
                      f-name   (.getName file)
                      ext      (let [dot (.lastIndexOf f-name ".")]
                                 (if (>= dot 0) (subs f-name dot) ""))]
                  (when (candidate-file? rel-path f-name ext
                                         file-types include-patterns exclude-patterns
                                         abs-path contract)
                    (let [file-id        abs-path
                          mod-time       (modified-time abs-path)
                          size           (.length file)
                          existing       (get existing-state file-id)
                          existing-meta  (:metadata existing)
                          same-version?  (and existing
                                              (= (:size existing-meta) size)
                                              (= (str (:modified_at existing-meta))
                                                 (some-> mod-time str)))]
                      (when-not same-version?
                        {:id         file-id
                         :path       rel-path
                         :size       size
                         :modified-at mod-time
                         :change     (if existing :changed :new)}))))))))))))

(defn snapshot-files
  "Lightweight file snapshot for passive watch diffing.
   Uses path/size/mtime only, no content hashing."
  [root-path opts]
  (if (or (nil? root-path) (str/blank? (str root-path)))
    {}
    (let [root-file        (java.io.File. (str root-path))
          root-abs         (.getAbsoluteFile root-file)
          file-types       (:file-types opts)
          include-patterns (:include-patterns opts)
          exclude-patterns (:exclude-patterns opts)
          contract         (:contract opts)
          entries          (atom {})]
      (when (.exists root-abs)
        (doseq [^java.io.File file (file-seq-recursive root-abs contract)]
          (let [abs-path (.getAbsolutePath file)
                rel-path (str (.relativize (.toPath root-abs) (.toPath file)))
                f-name   (.getName file)
                ext      (let [dot (.lastIndexOf f-name ".")]
                           (if (>= dot 0) (subs f-name dot) ""))]
            (when (candidate-file? rel-path f-name ext
                                   file-types include-patterns exclude-patterns
                                   abs-path contract)
              (swap! entries assoc rel-path
                     {:id          abs-path
                      :path        rel-path
                      :size        (.length file)
                      :modified-at (modified-time abs-path)})))))
      @entries)))

(defn find-files
  "Find all files under root-path, applying filters."
  [root-path opts]
  (if (or (nil? root-path) (str/blank? (str root-path)))
    {:total-files 0 :new-files 0 :changed-files 0 :unchanged-files 0 :skipped-files 0 :files []}
    (let [root-path-str    (str root-path)
          root-file        (java.io.File. root-path-str)
          root-abs         (.getAbsoluteFile root-file)
          existing-state   (:existing-state opts)
          file-types       (:file-types opts)
          include-patterns (:include-patterns opts)
          exclude-patterns (:exclude-patterns opts)
          contract         (:contract opts)
          files            (atom [])
          stats            (atom {:total 0 :new 0 :changed 0 :unchanged 0 :skipped 0})]
      (when (.exists root-abs)
        (doseq [^java.io.File file (file-seq-recursive root-abs contract)]
          (let [abs-path (.getAbsolutePath file)
                rel-path (str (.relativize (.toPath root-abs) (.toPath file)))
                f-name   (.getName file)
                ext      (let [dot (.lastIndexOf f-name ".")]
                           (if (>= dot 0) (subs f-name dot) ""))]
            (if (candidate-file? rel-path f-name ext
                                 file-types include-patterns exclude-patterns
                                 abs-path contract)
              (let [file-id       abs-path
                    mod-time      (try
                                    (-> (java.nio.file.Files/getLastModifiedTime
                                         (.toPath file)
                                         (make-array java.nio.file.LinkOption 0))
                                        (.toInstant))
                                    (catch Exception _ nil))
                    size          (.length file)
                    existing      (get existing-state file-id)
                    existing-meta (:metadata existing)
                    same-version? (and existing
                                       (= (:size existing-meta) size)
                                       (= (str (:modified_at existing-meta))
                                          (some-> mod-time str)))]
                (swap! stats update :total inc)
                (cond
                  same-version?
                  (swap! stats update :unchanged inc)

                  existing
                  (do (swap! stats update :changed inc)
                      (swap! files conj {:id          file-id
                                         :path        rel-path
                                         :size        size
                                         :modified-at mod-time}))

                  :else
                  (do (swap! stats update :new inc)
                      (swap! files conj {:id          file-id
                                         :path        rel-path
                                         :size        size
                                         :modified-at mod-time}))))
              ;; candidate-file? returned false
              (swap! stats update :skipped inc)))))
      {:total-files     (:total    @stats)
       :new-files       (:new      @stats)
       :changed-files   (:changed  @stats)
       :unchanged-files (:unchanged @stats)
       :skipped-files   (:skipped  @stats)
       :files           @files})))

(defn read-file-content
  "Read file content as UTF-8 string."
  [path]
  (try
    (slurp path :encoding "UTF-8")
    (catch Exception _ nil)))

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

  (close [_this]
    nil))

(defn create-driver
  "Create a new LocalDriver instance."
  [config]
  (->LocalDriver config (atom {})))
