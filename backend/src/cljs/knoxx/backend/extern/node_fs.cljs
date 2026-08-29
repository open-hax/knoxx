(ns knoxx.backend.extern.node-fs
  "Node fs/path boundary helpers used by route adapters."
  (:require ["node:fs/promises" :as fs-promises]))

(defn ^:async ensure-private-sandbox-directory!
  "Create a new `directory` at `mode` and reassert that mode on reuse.

   Accepts and returns CLJS scalars; native filesystem options stay here."
  [directory mode]
  (await (.mkdir fs-promises directory
                 (clj->js {:recursive true :mode mode})))
  (await (.chmod fs-promises directory mode))
  directory)

(defn ^:async read-sandbox-metadata!
  "Read and keywordize sandbox metadata, or return nil when absent/malformed."
  [metadata-path]
  (try
    (let [text (await (.readFile fs-promises metadata-path "utf8"))]
      (js->clj (.parse js/JSON (str text)) :keywordize-keys true))
    (catch :default _ nil)))

(defn ^:async write-sandbox-metadata!
  "Privately create the metadata root and write one CLJS metadata value."
  [metadata-root metadata-path directory-mode metadata]
  (await (ensure-private-sandbox-directory! metadata-root directory-mode))
  (await (.writeFile fs-promises
                     metadata-path
                     (.stringify js/JSON (clj->js metadata) nil 2)
                     "utf8"))
  metadata)

(defn ^:async remove-sandbox-metadata!
  "Idempotently remove one host-only sandbox metadata file."
  [metadata-path]
  (try
    (await (.rm fs-promises metadata-path (clj->js {:force true})))
    (catch :default _ nil)))

(defn ^:async remove-sandbox-directory!
  "Idempotently remove one validated sandbox workspace tree."
  [directory]
  (try
    (await (.rm fs-promises directory
                (clj->js {:recursive true :force true})))
    (catch :default _ nil)))

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
