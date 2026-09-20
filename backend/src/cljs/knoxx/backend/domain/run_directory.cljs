(ns knoxx.backend.domain.run-directory
  "Pure scope and deterministic ordering for visible persisted run snapshots."
  (:require [knoxx.backend.domain.run-store :as run]
            [knoxx.backend.law.run-directory :as law]))

(defn selected
  "Filter a provider result by its explicit scope and order newest snapshots first."
  [runs scope]
  (law/scope! scope)
  (->> runs
       (filter #(or (:all? scope) (= (:org-id scope) (:org_id %))))
       (sort-by (juxt :updated_at :run_id) #(compare %2 %1)) vec))

(defn visible
  "Read all unexpired snapshots through the same clock boundary as individual run reads."
  [state scope at-ms]
  (selected (keep #(run/visible-run state % at-ms) (keys (:runs state))) scope))
