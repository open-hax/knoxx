(ns knoxx.backend.extern.node-fs
  "Node fs/path boundary helpers used by route adapters.")

(defn mkdir!
  [node-fs p opts]
  (.mkdir node-fs p (clj->js opts)))

(defn chmod!
  "Apply numeric POSIX mode `mode` to path `p` through node:fs/promises."
  [node-fs p mode]
  (.chmod node-fs p mode))

(defn ^:async readdir-vector!
  [node-fs p]
  (let [files (await (.readdir node-fs p))]
    (vec (array-seq files))))

(defn rm!
  [node-fs p]
  (.rm node-fs p))

(defn read-file!
  [node-fs p]
  (.readFile node-fs p))

(defn write-buffer!
  [node-fs p content]
  (.writeFile node-fs p content))

(defn ^:async promise-all-vector
  [promises]
  (let [results (await (js/Promise.all (clj->js (vec promises))))]
    (vec (array-seq results))))
