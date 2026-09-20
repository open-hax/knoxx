(ns knoxx.backend.law.startup-admission
  "Portable ownership and failure decisions for partial run/thread startup."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def NonBlank [:and :string [:fn #(not (str/blank? %))]])
(def Record [:map [:startup_token NonBlank] [:run_id NonBlank]
             [:session_id NonBlank] [:conversation_id NonBlank]
             [:org_id {:optional true} [:maybe NonBlank]]
             [:user_id {:optional true} [:maybe NonBlank]]])
(def identity-fields [:startup_token :run_id :session_id :conversation_id :org_id :user_id])
(def active-statuses #{"running" "queued" "waiting_input"})

(defn validate! "Require a server-created attempt and its complete coordinates." [record]
  (when-not (m/validate Record record)
    (throw (ex-info "Invalid startup admission record" {:status 400 :code "startup_admission_invalid"})))
  record)

(defn same-owner? "Match the token and every immutable owning coordinate." [current proposed]
  (= (mapv current identity-fields) (mapv proposed identity-fields)))

(defn claimable?
  "Runs may resume their private reservation; threads may start after an idle predecessor."
  [kind current bound? proposed]
  (and (not (and (same-owner? (or current {}) proposed) (:startup_failure current)))
       (if (= kind :run)
         (or (not bound?) (same-owner? (or current {}) proposed))
         (or (not= "running" (:status current)) (same-owner? current proposed)))))

(defn failed-record "Preserve accepted data while recording this failed attempt." [current proposed]
  (assoc (if (same-owner? (or current {}) proposed) (merge proposed current) (merge current proposed))
         :status "failed" :has_active_stream false
         :error "Initial turn admission failed" :startup_failure "initial_admission_failed"))

(defn decide
  "Return the admitted record or nil when settlement has no remaining ownership.
   Refusal settlement also fences an unchanged preimage after an ambiguous claim."
  [kind phase current bound? same-view? proposed]
  (validate! proposed)
  (case phase
    :claim
    (if (and same-view? (claimable? kind current bound? proposed))
      (-> (merge current proposed) (dissoc :startup_failure)
          (assoc :error (:error proposed) :answer (:answer proposed)))
      (throw (ex-info "Startup admission conflicts with the current owner"
                      {:status 409 :code "startup_admission_conflict"})))
    :settle
    (cond
      (and current (same-owner? current proposed) (:startup_failure current)) current
      (and current (same-owner? current proposed) (active-statuses (:status current)))
      (failed-record current proposed)
      (and same-view? (claimable? kind current bound? proposed)) (failed-record current proposed)
      :else nil)
    (throw (ex-info "Invalid startup admission phase" {:status 400 :code "startup_admission_invalid"}))))
