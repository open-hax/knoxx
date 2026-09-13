(ns knoxx.backend.extern.resource-files
  "Named filesystem boundary for resource text and native failure metadata."
  (:require ["node:fs/promises" :as fs]))

(defn ^:async read-text!
  "Read resource EDN as UTF-8 text."
  [path]
  (await (.readFile fs path "utf8")))

(defn ^:async delete-file!
  "Remove the resource file during a reversible write rollback."
  [path]
  (await (.unlink fs path)))

(defn error-code
  "Read the native filesystem error code without exposing its host object."
  [error]
  (.-code error))
