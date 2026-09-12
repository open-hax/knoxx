(ns knoxx.backend.infra.clio-translation-split-store
  "Canonical Clio accepted operations over the split protocol reference."
  (:require [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.translation-split-store :as reference]))

(defrecord ClioTranslationSplitStore [engine]
  reference/ITranslationSplitStore
  (admit-turn! [_ turn]
    (clio/write! engine :translation-split-store/admit-turn [turn]))
  (append-candidate-split! [_ turn-id candidate]
    (clio/write! engine :translation-split-store/append-candidate-split [turn-id candidate]))
  (complete-candidate-set! [_ turn-id candidate-set]
    (clio/write! engine :translation-split-store/complete-candidate-set [turn-id candidate-set]))
  (append-review-receipt! [_ receipt]
    (clio/write! engine :translation-split-store/append-review-receipt [receipt]))
  (turn-for-run! [_ run-id]
    (clio/read! engine :translation-split-store/turn-for-run [run-id]))
  (turn-by-id! [_ turn-id]
    (clio/read! engine :translation-split-store/turn-by-id [turn-id]))
  (candidate-splits-for-turn! [_ turn-id]
    (clio/read! engine :translation-split-store/candidate-splits-for-turn [turn-id]))
  (candidate-set-for-turn! [_ turn-id]
    (clio/read! engine :translation-split-store/candidate-set-for-turn [turn-id]))
  (candidate-set-by-id! [_ candidate-set-id]
    (clio/read! engine :translation-split-store/candidate-set-by-id [candidate-set-id]))
  (turn-for-candidate-set! [_ candidate-set-id]
    (clio/read! engine :translation-split-store/turn-for-candidate-set [candidate-set-id]))
  (review-history-for-split! [_ candidate-set-id split-id]
    (clio/read! engine :translation-split-store/review-history-for-split [candidate-set-id split-id]))
  (applicable-memory! [_ scope]
    (clio/read! engine :translation-split-store/applicable-memory [scope])))

(defn- projection [digest-hex]
  (let [store (reference/memory-store digest-hex)]
    {:store store :snapshot #(dissoc @(:state store) :answer)}))

(defn open!
  "Open the isolated ledger; accepted timestamps and qualified namespaces replay unchanged."
  [{:keys [directory digest-hex]}]
  (when-not (fn? digest-hex)
    (throw (ex-info "Translation split provider requires a digest function" {:status 400})))
  (->ClioTranslationSplitStore
   (clio/open! {:directory directory :stream "knoxx/translation-split-store"
                :projection #(projection digest-hex)
                :reads {:translation-split-store/turn-for-run reference/turn-for-run!
                        :translation-split-store/turn-by-id reference/turn-by-id!
                        :translation-split-store/candidate-splits-for-turn reference/candidate-splits-for-turn!
                        :translation-split-store/candidate-set-for-turn reference/candidate-set-for-turn!
                        :translation-split-store/candidate-set-by-id reference/candidate-set-by-id!
                        :translation-split-store/turn-for-candidate-set reference/turn-for-candidate-set!
                        :translation-split-store/review-history-for-split reference/review-history-for-split!
                        :translation-split-store/applicable-memory reference/applicable-memory!}
                :writes {:translation-split-store/admit-turn reference/admit-turn!
                        :translation-split-store/append-candidate-split reference/append-candidate-split!
                        :translation-split-store/complete-candidate-set reference/complete-candidate-set!
                        :translation-split-store/append-review-receipt reference/append-review-receipt!}})))
