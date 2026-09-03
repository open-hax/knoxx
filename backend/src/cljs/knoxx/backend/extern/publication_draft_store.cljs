(ns knoxx.backend.extern.publication-draft-store
  "Node filesystem boundary for generated publication drafts.

  Callers provide and receive only CLJS strings, booleans, and maps. Native
  path objects, filesystem options, and Node error fields stay inside this
  adapter."
  (:require [knoxx.backend.domain.node.fs :as node-fs]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

(defn- absent-file-error?
  [error]
  (= "ENOENT" (.-code error)))

(defn draft-paths
  "Resolve the three generated-draft paths from CLJS scalar inputs."
  [configured-contracts-dir local-id source-path]
  (let [contracts-root (.resolve path (.cwd js/process)
                                 (str configured-contracts-dir))
        generated-root (.dirname path contracts-root)]
    {:content-path (.join path generated-root (str source-path))
     :manifest-path (.join path contracts-root "namespaces"
                           (str local-id ".edn"))
     :completion-path (.join path generated-root ".knoxx"
                            "draft-admission-completions"
                            (str local-id ".edn"))}))

(defn ^:async file-exists?
  "True when `file-path` exists; false only for Node's absent-file result."
  [file-path]
  (try
    (await (.access fs (str file-path)))
    true
    (catch :default err
      (if (absent-file-error? err)
        false
        (throw err)))))

(defn ^:async read-text!
  "Read one UTF-8 file and return a CLJS string."
  [file-path]
  (str (await (.readFile fs (str file-path) "utf8"))))

(defn ^:async read-text-or-nil!
  "Read one UTF-8 file, returning nil only when it is absent."
  [file-path]
  (try
    (await (read-text! file-path))
    (catch :default err
      (if (absent-file-error? err)
        nil
        (throw err)))))

(defn ^:async install-text-exclusive!
  "Create parent directories, then crash-safely install immutable UTF-8 bytes.

  Returns true when this caller installed the final path and false when an
  incumbent already owns it."
  [file-path content]
  (await (.mkdir fs (.dirname path (str file-path)) #js {:recursive true}))
  (node-fs/install-file-exclusive-sync! file-path content))
