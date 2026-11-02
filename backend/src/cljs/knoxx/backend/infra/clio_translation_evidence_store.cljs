(ns knoxx.backend.infra.clio-translation-evidence-store
  "Canonical Clio accepted operations over the evidence protocol reference."
  (:require [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.translation-evidence-store :as reference]))

(defrecord ClioTranslationEvidenceStore [engine]
  reference/ITranslationEvidenceStore
  (reserve-dispatch! [_ record]
    (clio/write! engine :translation-evidence-store/reserve-dispatch [record]))
  (resolve-dispatch! [_ expected-record outcome detail]
    (clio/write! engine :translation-evidence-store/resolve-dispatch [expected-record outcome detail]))
  (bind-dispatch-batch! [_ expected-record batch-id]
    (clio/write! engine :translation-evidence-store/bind-dispatch-batch [expected-record batch-id]))
  (claim-dispatch-completion! [_ expected-record]
    (clio/write! engine :translation-evidence-store/claim-dispatch-completion [expected-record]))
  (finish-dispatch-completion! [_ expected-record detail]
    (clio/write! engine :translation-evidence-store/finish-dispatch-completion [expected-record detail]))
  (record-translation! [_ receipt]
    (clio/write! engine :translation-evidence-store/record-translation [receipt]))
  (record-approval! [_ approval]
    (clio/write! engine :translation-evidence-store/record-approval [approval]))
  (dispatch-for-key! [_ dispatch-key]
    (clio/read! engine :translation-evidence-store/dispatch-for-key [dispatch-key]))
  (dispatch-for-batch-document! [_ batch-id document-wire-id]
    (clio/read! engine :translation-evidence-store/dispatch-for-batch-document [batch-id document-wire-id]))
  (dispatch-for-batch! [_ batch-id]
    (clio/read! engine :translation-evidence-store/dispatch-for-batch [batch-id]))
  (completed-translations! [_ scope]
    (clio/read! engine :translation-evidence-store/completed-translations [scope]))
  (approvals! [_ scope]
    (clio/read! engine :translation-evidence-store/approvals [scope])))

(defn- projection []
  (let [state (atom {:dispatches {} :completion-owners {} :receipts {} :approvals {}})]
    {:store (reference/memory-store state) :snapshot #(dissoc @state :answer)}))

(defn open!
  "Open the isolated ledger; accepted timestamps and qualified namespaces replay unchanged."
  [{:keys [directory]}]
  (->ClioTranslationEvidenceStore
   (clio/open! {:directory directory :stream "knoxx/translation-evidence-store"
                :projection projection
                :reads {:translation-evidence-store/dispatch-for-key reference/dispatch-for-key!
                        :translation-evidence-store/dispatch-for-batch-document reference/dispatch-for-batch-document!
                        :translation-evidence-store/dispatch-for-batch reference/dispatch-for-batch!
                        :translation-evidence-store/completed-translations reference/completed-translations!
                        :translation-evidence-store/approvals reference/approvals!}
                :writes {:translation-evidence-store/reserve-dispatch reference/reserve-dispatch!
                        :translation-evidence-store/resolve-dispatch reference/resolve-dispatch!
                        :translation-evidence-store/bind-dispatch-batch reference/bind-dispatch-batch!
                        :translation-evidence-store/claim-dispatch-completion reference/claim-dispatch-completion!
                        :translation-evidence-store/finish-dispatch-completion reference/finish-dispatch-completion!
                        :translation-evidence-store/record-translation reference/record-translation!
                        :translation-evidence-store/record-approval reference/record-approval!}})))
