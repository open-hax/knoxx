(ns knoxx.backend.law.spawn-diagnostic
  "Closed public failure contract; raw operator diagnostics are never replay data."
  (:require [knoxx.backend.law.run-store :as law]
            [malli.core :as m]))

(def Status
  "Finite HTTP failure statuses accepted from private diagnostic data."
  [:int {:min 400 :max 599}])

(def PublicDiagnostic
  "Only fixed public text, a bounded status and a stable failure code may persist."
  [:map {:closed true}
   [:message [:= "Agent turn could not be started."]]
   [:status Status]
   [:code [:= "async_spawn_failed"]]])

(defn public-diagnostic
  "Validate the public projection without copying arbitrary messages, stacks or data."
  [private-diagnostic]
  (let [status (get-in private-diagnostic [:data :status])]
    (law/require! PublicDiagnostic
                  {:message "Agent turn could not be started."
                   :status (if (m/validate Status status) status 500)
                   :code "async_spawn_failed"})))
