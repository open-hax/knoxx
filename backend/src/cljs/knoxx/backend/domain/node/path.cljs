;; knoxx.backend.domain.node.path
;;
;; THE JS BOUNDARY FOR PATH OPS.
;; node:path lives HERE AND NOWHERE ELSE.
;; All fns are pure and synchronous — they return plain CLJS strings.

(ns knoxx.backend.domain.node.path
  (:require ["node:path"    :as node-path]
            ["node:process" :as node-process]))

(defn join
  "Join path segments. Any falsy segment is skipped."
  [& parts]
  (.apply (.-join node-path) node-path (clj->js (keep #(when % (str %)) parts))))

(defn resolve
  "Resolve segments to absolute path."
  [& parts]
  (.apply (.-resolve node-path) node-path (clj->js (keep #(when % (str %)) parts))))

(defn dirname
  "Parent directory of path."
  [p]
  (.dirname node-path (str p)))

(defn basename
  "Final component of path, optionally stripping ext."
  ([p]     (.basename node-path (str p)))
  ([p ext] (.basename node-path (str p) (str ext))))

(defn extname
  "File extension including dot, e.g. \".edn\"."
  [p]
  (.extname node-path (str p)))

(defn relative
  "Relative path from `from` to `to`."
  [from to]
  (.relative node-path (str from) (str to)))

(defn normalize
  "Normalize a path (resolve ./ and ../)."
  [p]
  (.normalize node-path (str p)))

(defn cwd
  "Current working directory of the Node process."
  []
  (.cwd node-process))

(defn absolute?
  "True if path is absolute."
  [p]
  (.isAbsolute node-path (str p)))
