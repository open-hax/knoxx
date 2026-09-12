(ns knoxx.backend.infra.clio-source-authoring-store
  "Canonical Clio full-source operations with disposable reference replay."
  (:require [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.source-authoring-store :as reference]))
(defrecord ClioSourceAuthoringStore [ledger]
  reference/ISourceAuthoringStore
  (source-events! [_ scope] (clio/read! ledger :source-authoring/read [scope]))
  (admit-source! [_ scope expected-revision event] (clio/write! ledger :source-authoring/admit [scope expected-revision event])))
(defn open! "Open the explicit full-source ledger directory, refusing corrupt accepted history." [{:keys [directory]}]
  (->ClioSourceAuthoringStore
   (clio/open! {:directory directory :stream "knoxx/source-authoring"
                :projection #(let [store (reference/memory-store)] {:store store :snapshot (fn [] @(:state store))})
                :reads {:source-authoring/read reference/source-events!}
                :writes {:source-authoring/admit reference/admit-source!}})))
