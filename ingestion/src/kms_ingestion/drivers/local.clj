(ns kms-ingestion.drivers.local
  "Local filesystem driver for ingestion."
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [kms-ingestion.drivers.protocol :as protocol]
   [clojure.string :as str])
  (:import
   [java.security MessageDigest]
   [java.nio.file Files OpenOption]
   [java.time Instant]))

(defn- get-root-path
  "Extract root-path from config, handling both :root-path and :root_path keys."
  [config]
  (or (:root-path config)
      (:root_path config)
      (throw (ex-info "Missing root_path in driver config" {:config config}))))

;; Skip patterns
(def skip-dirs
  #{"node_modules" ".git" "dist" ".next" "__pycache__" ".venv" "venv"
    "target" ".pio" "coverage" ".pytest_cache" ".mypy_cache" "build"
    ".gradle" ".idea" ".vscode" ".vs" "vendor" "fixtures" "__fixtures__"})

(def skip-files
  #{"package-lock.json" "pnpm-lock.yaml" "yarn.lock" "Cargo.lock"
    "poetry.lock" "composer.lock" "Gemfile.lock" "flake.lock"
    ".DS_Store" ".env" ".env.local"})

(def skip-extensions
  #{".min.js" ".min.css" ".d.ts" ".map" ".pyc" ".pyo"
     ".so" ".dylib" ".dll" ".exe" ".bin"})

(def default-text-extensions
  #{".md" ".markdown" ".txt" ".rst" ".org" ".adoc"
    ".json" ".jsonl" ".yaml" ".yml" ".toml" ".ini" ".cfg" ".conf" ".env"
    ".xml" ".csv" ".tsv"
    ".html" ".htm" ".css" ".js" ".jsx" ".ts" ".tsx"
    ".py" ".rb" ".php" ".java" ".kt" ".go" ".rs" ".c" ".cc" ".cpp" ".h" ".hpp"
    ".clj" ".cljs" ".cljc" ".edn" ".sql" ".sh" ".bash" ".zsh" ".fish"
    ".tex" ".bib" ".nix" ".dockerfile" ".gradle" ".properties"})

(defn text-like-file?
  "Default heuristic for files we can safely treat as text documents."
  [path]
  (let [name (str/lower-case (str (fs/file-name path)))
        ext-raw (str/lower-case (or (fs/extension path) ""))
        ext (if (str/starts-with? ext-raw ".") ext-raw (str "." ext-raw))
        result (or (default-text-extensions ext)
                   (= name "dockerfile")
                   (str/ends-with? name ".env.example")
                   (str/ends-with? name ".service")
                   (str/ends-with? name ".desktop"))]
    (when-not result
      (println (str "[TEXT-LIKE-FALSE] name=" name " ext=" ext " in-set?=" (contains? default-text-extensions ext))))
    result))

(defn path-matches-glob?
  "Check if a filename matches a simple glob pattern like *.md."
  [filename pattern]
  (cond
    ;; Handle *.ext patterns
    (.startsWith pattern "*.")
    (str/ends-with? filename (subs pattern 1))
    
    ;; Handle **/ prefix patterns
    (.startsWith pattern "**/")
    (str/ends-with? filename (subs pattern 3))
    
    ;; Handle exact matches
    :else
    (= filename pattern)))

(defn should-skip?
  "Check if a path should be skipped."
  [path]
  (let [parts (fs/components path)
        name (fs/file-name path)
        ext (fs/extension path)]
    (or
     ;; Skip hidden directories (except .github)
     (some (fn [part]
             (and (str/starts-with? part ".")
                  (not= part ".github")
                  (or (skip-dirs part)
                      (not (fs/extension part))))) ; hidden dir
           parts)
     ;; Skip specific files
     (skip-files name)
     ;; Skip by extension
     (skip-extensions ext)
     ;; Skip test files (unless explicitly included)
     (and (or (str/includes? (str/lower-case name) "test")
              (str/includes? (str/lower-case name) "spec"))
          ;; Don't skip if there are explicit include patterns
          false))))

(defn sha256
  "Compute SHA-256 hash of a file."
  [path]
  (try
    (let [digest (MessageDigest/getInstance "SHA-256")
          bytes (Files/readAllBytes (fs/path path))]
      (let [hash-bytes (.digest digest bytes)]
        (format "%064x" (BigInteger. 1 hash-bytes))))
    (catch Exception _ nil)))

(defn file-size
  "Get file size in bytes."
  [path]
  (try
    (fs/size path)
    (catch Exception _ 0)))

(defn modified-time
  "Get file modification time as Instant."
  [path]
  (try
    (-> (Files/getLastModifiedTime (fs/path path) (make-array java.nio.file.LinkOption 0))
        (.toInstant))
    (catch Exception _ nil)))

(defn- file-seq-recursive
  "Recursively list all files under a directory using java.nio.file.Files/walkFileTree."
  [^java.io.File root]
  (if-not (.exists root)
    (do (println "[FILE-SEQ-RECURSIVE] root does not exist:" (.getAbsolutePath root))
        [])
    (let [results (atom [])
          visitor (proxy [java.nio.file.SimpleFileVisitor] []
                    (visitFile [file attrs]
                      (swap! results conj (.toFile file))
                      java.nio.file.FileVisitResult/CONTINUE)
                    (visitFileFailed [file exc]
                      (println "[FILE-SEQ-RECURSIVE] visitFileFailed:" file exc)
                      java.nio.file.FileVisitResult/CONTINUE))]
      (Files/walkFileTree (.toPath root) visitor)
      (println (str "[FILE-SEQ-RECURSIVE] Found " (count @results) " files under " (.getAbsolutePath root)))
      @results)))

(defn find-files
  "Find all files under root-path, applying filters."
  [root-path opts]
  (if (or (nil? root-path) (str/blank? (str root-path)))
    (do (println "[FIND-FILES] ERROR: root-path is nil or blank")
        {:total-files 0 :new-files 0 :changed-files 0 :unchanged-files 0 :skipped-files 0 :files []})
    (let [root-path-str (str root-path)
        root-file (java.io.File. root-path-str)
        root-abs (.getAbsoluteFile root-file)
        existing-hashes (:existing-hashes opts)
        file-types (:file-types opts)
        include-patterns (:include-patterns opts)
        exclude-patterns (:exclude-patterns opts)
        files (atom [])
        stats (atom {:total 0 :new 0 :changed 0 :unchanged 0 :skipped 0})]
    (when (.exists root-abs)
      (println (str "[FIND-FILES] Walking: " root-abs " isDir=" (.isDirectory root-abs)))
          (doseq [^java.io.File file (file-seq-recursive root-abs)]
        (let [abs-path (.getAbsolutePath file)
              rel-path (str (.relativize (.toPath root-abs) (.toPath file)))
              f-name (.getName file)
              ext (let [dot (.lastIndexOf f-name ".")]
                    (if (>= dot 0) (subs f-name dot) ""))]
          (cond
            ;; Skip dot-files and dot-dirs
            (some #(str/starts-with? % ".")
                  (str/split rel-path #"/"))
            (do (println "[SKIP] dot:" rel-path) (swap! stats update :skipped inc))

            ;; Skip by name
            (skip-files f-name)
            (do (println "[SKIP] name:" f-name) (swap! stats update :skipped inc))

            ;; Skip by extension
            (skip-extensions ext)
            (do (println "[SKIP] ext:" ext) (swap! stats update :skipped inc))

            ;; Apply include patterns
            (and (seq include-patterns)
                 (not (some #(path-matches-glob? rel-path %) include-patterns)))
            (do (println "[SKIP] include:" rel-path) (swap! stats update :skipped inc))

            ;; Apply exclude patterns
            (and (seq exclude-patterns)
                 (some #(path-matches-glob? rel-path %) exclude-patterns))
            (do (println "[SKIP] exclude:" rel-path) (swap! stats update :skipped inc))

            ;; Apply file type filter
            (and (seq file-types)
                 (not (some #(path-matches-glob? f-name (str "*" %)) file-types)))
            (do (println "[SKIP] file-type:" f-name "ext=" ext "types=" file-types) (swap! stats update :skipped inc))

            ;; Default: text-like only if no file-types specified
            (and (empty? file-types)
                 (not (text-like-file? abs-path)))
            (do (println "[SKIP] text-like:" f-name) (swap! stats update :skipped inc))

            ;; Include file
            :else
            (do
              (println (str "[INCLUDE] " rel-path))
              (let [file-id abs-path
                  content-hash (try
                                 (let [digest (java.security.MessageDigest/getInstance "SHA-256")
                                       bytes (java.nio.file.Files/readAllBytes (.toPath file))]
                                   (format "%064x" (BigInteger. 1 (.digest digest bytes))))
                                 (catch Exception _ nil))
                  mod-time (try
                             (-> (java.nio.file.Files/getLastModifiedTime (.toPath file)
                                   (make-array java.nio.file.LinkOption 0))
                                 (.toInstant))
                             (catch Exception _ nil))
                  size (.length file)
                  existing-hash (get existing-hashes file-id)]
              (swap! stats update :total inc)
              (cond
                (and existing-hash (= existing-hash content-hash))
                (swap! stats update :unchanged inc)

                existing-hash
                (do
                  (swap! stats update :changed inc)
                  (swap! files conj {:id file-id
                                     :path rel-path
                                     :content-hash content-hash
                                     :size size
                                     :modified-at mod-time}))

                :else
                (do
                  (swap! stats update :new inc)
                  (swap! files conj {:id file-id
                                     :path rel-path
                                     :content-hash content-hash
                                     :size size
                                      :modified-at mod-time}))))))))
    {:total-files (:total @stats)
     :new-files (:new @stats)
     :changed-files (:changed @stats)
     :unchanged-files (:unchanged @stats)
     :skipped-files (:skipped @stats)
     :files @files}))))

(defn read-file-content
  "Read file content as UTF-8 string."
  [path]
  (try
    (slurp path :encoding "UTF-8")
    (catch Exception _ nil)))

;; ============================================================
;; Driver Record
;; ============================================================

(defrecord LocalDriver [config state]
  protocol/Driver
  (discover [this opts]
    (let [root-path (get-root-path config)
          _ (println (str "[DRIVER-DISCOVER] root-path=" root-path " exists=" (.exists (java.io.File. (str root-path)))))
          result (find-files root-path opts)]
      (println (str "[DRIVER-DISCOVER] result: total=" (:total-files result) " new=" (:new-files result)))
      result))
  
  (extract [this file-id]
    (let [content (read-file-content file-id)
          content-hash (when content (sha256 file-id))
          path (str (.relativize (fs/path (get-root-path config)) (fs/path file-id)))]
      {:id file-id
       :path path
       :content content
       :content-hash content-hash}))
  
  (extract-batch [this file-ids]
    (mapv (partial protocol/extract this) file-ids))
  
  (get-state [this]
    {:root-path (get-root-path config)
     :last-scan (str (Instant/now))})
  
  (set-state [this new-state]
    (reset! state new-state))
  
  (close [this]
    nil))

(defn create-driver
  "Create a new LocalDriver instance."
  [config]
  (->LocalDriver config (atom {})))
