(ns knoxx.backend.infra.clio-source-review-store
  "Canonical Clio source-review operations with disposable reference replay."
  (:require [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.source-review-store :as reference]))
(defrecord ClioSourceReviewStore [ledger]
  reference/ISourceReviewStore
  (read-source-review-events! [_ scope] (clio/read! ledger :source-review/read [scope]))
  (admit-source-review! [_ scope expected-head event] (clio/write! ledger :source-review/admit [scope expected-head event])))
(defn open! "Open the explicit review ledger directory, refusing corrupt accepted history." [{:keys [directory]}]
  (->ClioSourceReviewStore
   (clio/open! {:directory directory :stream "knoxx/source-review"
                :projection #(let [store (reference/memory-store)] {:store store :snapshot (fn [] @(:state store))})
                :reads {:source-review/read reference/read-source-review-events!}
                :writes {:source-review/admit reference/admit-source-review!}})))
