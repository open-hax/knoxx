(ns knoxx.backend.infra.translation-candidate-content
  "Authenticate the target bytes behind resource-backed translation receipts.

  Evidence and content have separate stores. A digest-bound receipt is still
  not a usable candidate when its reproducible `.translations` entry has been
  lost, and an authored receipt is current only while the exact declared locale
  file still exists. This adapter performs that join once so dispatch, review,
  and publication do not acquire three subtly different definitions of
  'translated'."
  (:require [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.translation-agent-content :as agent-content]
            [knoxx.backend.infra.translation-content-integrity :as integrity]
            [knoxx.backend.law.translation-evidence :as evidence-law]
            [promesa.core :as p]))

(defn receipt-identity
  "Exact immutable candidate identity shared by ledger and content snapshots."
  [receipt]
  ((juxt :translation/org-id
         :translation/project
         :translation/document
         :translation/garden
         :translation/source-locale
         :translation/locale
         :translation/source-revision
         :translation/revision
         :translation/content-digest)
   receipt))

(defn authored-identities
  "Exact identities produced by the current authored-content snapshot."
  [authored]
  (into #{} (map receipt-identity) authored))

(defn receipt-work-identity
  "Exact desired-work relation, excluding the candidate output that supersedes."
  [receipt]
  ((juxt :translation/org-id
         :translation/project
         :translation/document
         :translation/garden
         :translation/source-locale
         :translation/locale
         :translation/source-revision)
   receipt))

(defn current-receipts
  "Select the current candidate per work relation before content admission.

  Admission must never remove a newer unavailable candidate and thereby make an
  older output current again. A missing newest artifact means retranslation,
  not rollback to bytes whose approval was already superseded."
  [receipts]
  (vals
   (reduce (fn [index receipt]
             (let [checked (evidence-law/assert-receipt! receipt)
                   relation (receipt-work-identity checked)]
               (if (evidence-law/supersedes? checked (get index relation))
                 (assoc index relation checked)
                 index)))
           {}
           receipts)))

(defn- ^:async target-content-for-identities!
  [content-root roots documents authored-ids receipt]
  (when (and (some? receipt) (evidence-law/content-bound? receipt))
    (let [document (get documents (:translation/document receipt))]
      (when document
        (if (contains? authored-ids (receipt-identity receipt))
          (await (contract-content/localized-content!
                  (get roots (:document/id document))
                  document
                  (:translation/locale receipt)))
          (await (agent-content/content-for-receipt! content-root receipt)))))))

(defn ^:async target-content!
  "Load the target bytes for `receipt` from its current resource-owned source.

  A receipt in `authored` reads the declared locale file. Every other
  resource-backed receipt reads the immutable agent content store. There is no
  fallback to legacy OpenPlanner segments: borrowing a same-named row from the
  older CMS is the split-brain behavior this boundary removes."
  [content-root roots documents authored receipt]
  (await (target-content-for-identities!
          content-root roots documents (authored-identities authored) receipt)))

(defn ^:async authenticated-receipts!
  "Keep only receipts whose exact target bytes are present and digest-matched.

  The input remains immutable history in the evidence store. This is a
  request-snapshot projection: dropping a row here makes desired work visible
  and recoverable without deleting or rewriting the historical receipt."
  [content-root roots documents authored receipts]
  (let [authored-ids (authored-identities authored)
        pending
        (mapv (fn [receipt]
                (p/let [content (target-content-for-identities!
                                 content-root roots documents authored-ids receipt)]
                  (when (integrity/authenticated-content? receipt content)
                    receipt)))
              (current-receipts receipts))]
    (->> (await (p/all pending))
         (remove nil?)
         vec)))
