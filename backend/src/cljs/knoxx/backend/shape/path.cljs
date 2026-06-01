(ns knoxx.backend.shape.path
  (:require [clojure.string :as str]
            [knoxx.backend.infra.config :refer [cfg]]
            [knoxx.backend.infra.core-memory :refer [trim-mention-token]]
            [knoxx.backend.infra.document-state :refer [normalize-relative-path]]))

(defn- strip-root-prefix
  [root value]
  (let [normalized-root (some-> root str (str/replace #"/+$" ""))]
    (cond
      (str/blank? (or normalized-root ""))
      nil

      (= value normalized-root)
      ""

      (str/starts-with? value (str normalized-root "/"))
      (subs value (inc (count normalized-root)))

      :else
      nil)))

(defn normalize-workspace-path
  [value]
  (let [trimmed (trim-mention-token value)
        config (cfg)
        roots (cons (:workspace-root config) (:extra-workspace-roots config))
        no-prefix (or (some #(strip-root-prefix % trimmed) roots) trimmed)
        normalized (normalize-relative-path no-prefix)]
    (when (and (not (str/blank? normalized))
               (re-find #"^(orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.ημ)/" normalized))
      normalized)))

(defn normalize-devel-path
  "Backward-compatible alias for callers still using the old devel-lake name."
  [value]
  (normalize-workspace-path value))
(defn basename
  [path]
  (let [s (-> (str path)
              (str/replace #"\\\\" "/")
              (str/replace #"/+" "/"))
        parts (->> (str/split s #"/")
                   (remove str/blank?))]
    (or (last parts) s)))
(defn path-resolve
  [^js node-path & parts]
  (case (count parts)
    0 (.resolve node-path)
    1 (.resolve node-path (nth parts 0))
    2 (.resolve node-path (nth parts 0) (nth parts 1))
    3 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2))
    4 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3))
    5 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4))
    6 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5))
    7 (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5) (nth parts 6))
    (.resolve node-path (nth parts 0) (nth parts 1) (nth parts 2) (nth parts 3) (nth parts 4) (nth parts 5) (nth parts 6) (nth parts 7))))

(defn path-relative
  [^js node-path from to]
  (.relative node-path from to))

(defn path-is-absolute?
  [^js node-path value]
  (.isAbsolute node-path value))
