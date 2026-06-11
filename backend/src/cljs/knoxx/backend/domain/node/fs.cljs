;; knoxx.backend.domain.node.fs
;;
;; THE JS BOUNDARY FOR FILESYSTEM OPS.
;; node:fs, node:fs/promises, and Dirent objects live HERE AND NOWHERE ELSE.
;; Every fn accepts CLJS strings/maps, returns CLJS values or Promise<CLJS>.
;; js/Array.from conversions happen at the edge — nothing leaks out.

(ns knoxx.backend.domain.node.fs
  (:refer-clojure :exclude [exists?])
  (:require [clojure.string :as str]
            ["node:fs"           :as node-fs]
            ["node:fs/promises"  :as fs]
            ["node:path"         :as path]))

;; ── Sync ─────────────────────────────────────────────────────────────────

(defn exists?
  "Returns true if path exists on disk. Never throws."
  [p]
  (.existsSync node-fs (str p)))

(defn read-file-sync
  "Read file synchronously. Returns string or nil on error."
  [p]
  (try (.readFileSync node-fs (str p) "utf8")
       (catch :default _ nil)))

(defn readdir-sync
  "List immediate child names in dir. Returns vec<string>, [] on error."
  [p]
  (try (vec (.readdirSync node-fs (str p)))
       (catch :default _ [])))

;; ── Async ────────────────────────────────────────────────────────────────

(defn read-file!
  "Promise<string>. Rejects on ENOENT or other errors."
  [p]
  (.readFile fs (str p) "utf8"))

(defn write-file!
  "Promise<nil>. Creates file (and overwrites). Dirs must already exist."
  [p text]
  (.writeFile fs (str p) (str text) "utf8"))

(defn mkdir!
  "Promise<nil>. Creates directory and all parents. Safe to call if exists."
  [p]
  (.mkdir fs (str p) #js {:recursive true}))

(defn ^:async write-file-ensure-dir!
  "mkdir -p the parent dir then write. Returns Promise<nil>."
  [p text]
  (await (mkdir! (.dirname path (str p))))
  (write-file! p text))

(defn ^:async stat!
  "Promise<{:size :mtime-ms :is-file? :is-dir?}>. Rejects if not found."
  [p]
  (let [s (await (.stat fs (str p)))]
    {:size     (.-size s)
     :mtime-ms (.getTime (.-mtime s))
     :mtime    (.toISOString (.-mtime s))
     :is-file? (.isFile s)
     :is-dir?  (.isDirectory s)}))

(defn ^:async stat-or-nil!
  "Like stat! but resolves to nil if file does not exist."
  [p]
  (try
    (await (stat! p))
    (catch :default _ nil)))

(defn ^:async readdir!
  "Promise<vec<string>> — immediate child names only. [] on ENOENT."
  [p]
  (try
    (let [entries (await (.readdir fs (str p)))]
      (vec (js/Array.from entries)))
    (catch :default _ [])))

(defn ^:async readdir-deep!
  "Recursively find all files under root whose names pass pred.
   Returns Promise<vec<string>> of absolute paths. [] on ENOENT."
  ([root] (readdir-deep! root (constantly true)))
  ([root pred]
   (try
     (let [entries (await (.readdir fs (str root) #js {:withFileTypes true :recursive true}))]
       (->> (js/Array.from entries)
            (keep (fn [e]
                    (when (and (.isFile e) (pred (.-name e)))
                      (.join path (.-parentPath e) (.-name e)))))
            vec))
     (catch :default _ []))))

(defn watch!
  "Watch path recursively. cb called with [event filename-str].
   Returns the watcher object (call .close to stop)."
  [p cb]
  (.watch node-fs (str p)
          #js {:recursive true}
          (fn [event filename]
            (cb event (some-> filename str)))))

(defn ^:async unlink!
  "Promise<nil>. Delete a file. Resolves (not rejects) if already gone."
  [p]
  (try
    (await (.unlink fs (str p)))
    (catch :default err
      (when-not (= "ENOENT" (.-code err))
        (throw err)))))
