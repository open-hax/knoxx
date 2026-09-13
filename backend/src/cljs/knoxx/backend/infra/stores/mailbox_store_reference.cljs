(ns knoxx.backend.infra.stores.mailbox-store-reference
  "Disposable reference state for canonical mailbox operations."
  (:require [knoxx.backend.domain.mailbox-store :as domain]))

(defn apply-operation!
  "Apply one pure admission and return its exact receipt."
  [state operation]
  (let [[next-state result] (domain/transition @state operation)]
    (reset! state next-state) result))
(defn resolve-route "Read a scoped live route from a reference projection." [state scope actor at]
  (domain/resolve-route @state scope actor at))
(defn list-entries "Read scoped mailbox metadata from a reference projection." [state scope filters at]
  (domain/list-entries @state scope filters at))
(defn read-message "Read canonical content under sender or recipient authority." [state scope id at]
  (domain/read-message @state scope id at))
(defn projection "Create an empty replay target." []
  (let [state (atom {})] {:store state :snapshot #(deref state)}))
