(ns knoxx.backend.extern.node-fs
  "Node fs/path boundary helpers used by route adapters.")

(defn mkdir!
  [node-fs p opts]
  (.mkdir node-fs p (clj->js opts)))

(defn readdir-vector!
  [node-fs p]
  (-> (.readdir node-fs p)
      (.then (fn [files]
               (vec (array-seq files))))))

(defn rm!
  [node-fs p]
  (.rm node-fs p))

(defn read-file!
  [node-fs p]
  (.readFile node-fs p))

(defn write-buffer!
  [node-fs p content]
  (.writeFile node-fs p content))

(defn promise-all-vector
  [promises]
  (-> (js/Promise.all (clj->js (vec promises)))
      (.then (fn [results]
               (vec (array-seq results))))))
