(ns knoxx.backend.law.run-directory
  "Finite scoped run directory queries; policy grants are decided by the caller."
  (:require [knoxx.backend.law.run-store :as run]))

(def Scope [:or [:map {:closed true} [:org-id run/NonBlank]]
            [:map {:closed true} [:all? [:= true]]]])

(defn scope!
  "Require an explicit tenant or explicitly privileged all-tenant scope."
  [scope]
  (run/require! Scope scope))
