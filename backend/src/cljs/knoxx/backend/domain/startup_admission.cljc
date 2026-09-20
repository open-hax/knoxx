(ns knoxx.backend.domain.startup-admission
  "Pure state decisions for one owned startup attempt and its failure fence."
  (:require [knoxx.backend.law.startup-admission :as law]))

(defn failed-record "Preserve accepted data while recording this failed attempt." [current proposed]
  (assoc (if (law/same-owner? (or current {}) proposed) (merge proposed current) (merge current proposed))
         :status "failed" :has_active_stream false
         :error "Initial turn admission failed" :startup_failure "initial_admission_failed"))

(defn decide
  "Return the admitted record or nil when settlement has no remaining ownership.
   Refusal settlement also fences an unchanged preimage after an ambiguous claim."
  [kind phase current bound? same-view? proposed]
  (law/validate! proposed)
  (case phase
    :claim
    (if (and same-view? (law/claimable? kind current bound? proposed))
      (-> (merge current proposed) (dissoc :startup_failure)
          (assoc :error (:error proposed) :answer (:answer proposed)))
      (throw (ex-info "Startup admission conflicts with the current owner"
                      {:status 409 :code "startup_admission_conflict"})))
    :settle
    (cond
      (and current (law/same-owner? current proposed) (:startup_failure current)) current
      (and current (law/same-owner? current proposed) (law/active-statuses (:status current)))
      (failed-record current proposed)
      (and same-view? (law/claimable? kind current bound? proposed)) (failed-record current proposed)
      :else nil)
    (throw (ex-info "Invalid startup admission phase" {:status 400 :code "startup_admission_invalid"}))))
