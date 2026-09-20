(ns knoxx.backend.domain.thread-recovery
  "Release an exact orphaned conversation without adopting its immutable run identity."
  (:require [knoxx.backend.law.thread-recovery :as law]))

(defn release
  "Only the new turn may claim a new run; recovery itself leaves an idle conversation."
  [current observed same-view? stamp]
  (law/assert-release! current observed same-view? stamp)
  (assoc (law/snapshot current) :status "waiting_input" :has_active_stream false
         :recovered_at (:at stamp) :recovered_from_run_id (:run_id current)))
