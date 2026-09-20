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

