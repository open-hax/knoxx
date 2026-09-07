(ns knoxx.frontend.infra.migration-git
  "Git transport for migration baseline text and changed repository paths."
  (:require ["node:child_process" :as child-process]
            [clojure.string :as str]))

(defn- commit-exists?
  "Whether Git can resolve a revision to a commit in the local checkout."
  [root sha]
  (try
    (child-process/execFileSync
     "git" #js ["cat-file" "-e" (str sha "^{commit}")]
     #js {:cwd root :stdio #js ["ignore" "ignore" "ignore"]})
    true
    (catch :default _ false)))

(defn baseline-text
  "Read raw ledger text; nil means an absent path or omitted revision."
  [root path sha]
  (when (seq sha)
    (when-not (commit-exists? root sha)
      (throw (ex-info "Git cannot resolve the migration baseline revision"
                      {:base-sha sha})))
    (let [entries (child-process/execFileSync
                   "git" #js ["ls-tree" "-z" "--name-only" sha "--" path]
                   #js {:cwd root :encoding "utf8"
                        :stdio #js ["ignore" "pipe" "pipe"]})]
      (when (seq entries)
        (child-process/execFileSync
         "git" #js ["show" (str sha ":" path)]
         #js {:cwd root :encoding "utf8"
              :stdio #js ["ignore" "pipe" "pipe"]})))))

(defn- parse-changed-paths
  "Validate Git's NUL-terminated path records without trimming filename characters."
  [output]
  (if (empty? output)
    []
    (do
      (when-not (str/ends-with? output "\u0000")
        (throw (ex-info "Git changed paths must be NUL-terminated" {})))
      (let [paths (str/split (subs output 0 (dec (count output))) #"\u0000" -1)]
        (when (some empty? paths)
          (throw (ex-info "Git changed paths cannot contain empty records" {})))
        paths))))

(defn changed-paths
  "Return repository paths changed between a Git revision and HEAD."
  [root sha]
  (if (seq sha)
    (-> (child-process/execFileSync
         "git" #js ["diff" "--name-only" "-z" (str sha "...HEAD")]
         #js {:cwd root :encoding "utf8"})
        parse-changed-paths)
    []))

(defn native-source-count
  "Count native CLJS file paths in the exact base tree, independent of its ledger."
  [root sha]
  (->> (child-process/execFileSync
         "git" #js ["ls-tree" "-r" "--name-only" "-z" sha "--" "frontend/src/"]
         #js {:cwd root :encoding "utf8"})
       parse-changed-paths
       (filter #(str/ends-with? % ".cljs"))
       count))
