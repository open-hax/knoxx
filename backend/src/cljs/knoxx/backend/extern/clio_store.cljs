(ns knoxx.backend.extern.clio-store
  "Node path boundary for application-owned canonical Clio directories."
  (:require ["node:path" :as path]
            [knoxx.backend.law.clio-application-store :as law]))

(defn resolve-directory
  "Resolve an explicitly validated directory without changing the process cwd."
  [directory]
  (path/resolve (law/assert-directory! directory)))
