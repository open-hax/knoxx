(ns knoxx.backend.domain.translation-review-inventory
  "Resource-first translation review inventory.

  Desired publication work is the outer relation. Dispatches, completed
  translations, and approvals are evidence attached to it; none of those
  collections is allowed to decide how many rows exist. Keeping that join pure
  makes the same projection usable by HTTP, MCP, and later UI adapters."
  (:require [knoxx.backend.domain.publication-gate :as publication-gate]
            [knoxx.backend.domain.translation-evidence :as translation-evidence]
            [knoxx.backend.law.publication :as publication-law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-evidence :as evidence-law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(defn work-key
  "Exact evidence relation for one translation work item or observed fact.

  Source locale is deliberately present even though the older publication gate
  key omits it. Changing a document's declared source locale creates different
  translation work and must not make an old receipt reviewable."
  [value]
  [(or (:translation/org-id value) (:dispatch/org-id value))
   (or (:translation/project value) (:dispatch/project value))
   (or (:translation/document value) (:dispatch/document value))
   (or (:translation/garden value) (:dispatch/garden value))
   (or (:translation/source-locale value) (:dispatch/source-locale value))
   (or (:translation/locale value) (:dispatch/locale value))
   (or (:translation/source-revision value) (:dispatch/revision value))])

(defn- work-sort-key
  [work]
  (mapv pr-str
        [(:translation/document work)
         (:translation/garden work)
         (:translation/locale work)
         (:translation/source-revision work)
         (:publication/id work)]))

(defn- concrete-revision
  [intent source-revisions]
  (let [resolved (publication-gate/resolve-concrete-revision
                  intent
                  {:current-source-revision #(get source-revisions %)})]
    ;; Publication resources historically admitted any nonblank string as an
    ;; explicit revision.  Evidence and dispatch correctly refuse the reserved
    ;; `source/*` namespace, because a selector can never be a durable fact.
    ;; Keep such a resource visible as unresolved instead of letting one bad pin
    ;; make the entire inventory fail while deriving its dispatch key.
    (when-not (evidence-law/revision-selector? resolved)
      resolved)))

(defn desired-work
  "Derive one work item per eligible publication intent.

  `source-revisions` is the one already-observed map of document id to concrete
  revision. A current selector that could not be resolved remains an explicit
  item with a nil `:translation/source-revision`; silent omission would make an
  unreadable source indistinguishable from a resource that never existed.

  Only structurally admissible, publishing, cross-locale intents derive work.
  Withheld and archived publications remain desired-state history, not work a
  translator should be asked to perform."
  [publication-index source-revisions]
  (->> (:publications publication-index)
       (keep (fn [intent]
               (let [hydrated (publication-law/hydrate-publication-intent
                               publication-index intent)]
                 (when (and (publication-law/admissible-publication?
                             publication-index hydrated)
                            (publication-gate/translation-work-eligible? hydrated))
                   {:publication/id (:publication/id hydrated)
                    :translation/document (:publication/document hydrated)
                    :translation/garden (:publication/garden hydrated)
                    :translation/source-locale (:document/source-locale hydrated)
                    :translation/locale (:publication/locale hydrated)
                    :translation/revision-selector (:publication/revision hydrated)
                    :translation/source-revision (concrete-revision
                                                  hydrated source-revisions)
                    :translation/title (:document/title hydrated)}))))
       (sort-by work-sort-key)
       vec))

(defn dispatch-key-input
  "The exact input used to look up this work's current dispatch claim.

  Returns nil while the source revision is unresolved, because a dispatch key
  is only lawful for a concrete revision. Garden is encoded exactly as the
  dispatch context encodes it before `law.translation-dispatch/dispatch-key` is
  called, preventing read and write keys from acquiring separate spellings."
  [scope work]
  (when (some? (:translation/source-revision work))
    {:org-id (:org-id scope)
     :project (:project scope)
     :garden (resource-identity/encode-keyword (:translation/garden work))
     :document (:translation/document work)
     :source-locale (:translation/source-locale work)
     :locale (:translation/locale work)
     :revision (:translation/source-revision work)}))

(defn dispatch-lookup-key
  "The durable dispatch key for `work`, or nil until its revision resolves."
  [scope work]
  (some-> (dispatch-key-input scope work) dispatch-law/dispatch-key))

(defn- dispatch-rank
  [record]
  [(:dispatch/at record)
   (pr-str (:dispatch/outcome record))
   (:dispatch/key record)])

(defn- index-dispatches
  "Latest deterministic dispatch observation per exact work relation.

  Today's store keeps one mutable claim per key. The total order also keeps the
  projection deterministic when a caller supplies snapshots from more than one
  store read or a later store retains attempt history."
  [dispatches]
  (reduce (fn [index record]
            (let [checked (dispatch-law/assert-record! record)
                  encoded-garden (resource-identity/encode-keyword
                                  (:dispatch/garden checked))
                  expected-key (dispatch-law/dispatch-key
                                {:org-id (:dispatch/org-id checked)
                                 :project (:dispatch/project checked)
                                 :garden encoded-garden
                                 :document (:dispatch/document checked)
                                 :source-locale (:dispatch/source-locale checked)
                                 :locale (:dispatch/locale checked)
                                 :revision (:dispatch/revision checked)})
                  _ (when-not (= expected-key (:dispatch/key checked))
                      (throw (ex-info "translation dispatch body does not match its key"
                                      {:expected-dispatch-key expected-key
                                       :actual-dispatch-key (:dispatch/key checked)})))
                  relation (work-key checked)
                  incumbent (get index relation)]
              (if (or (nil? incumbent)
                      (pos? (compare (dispatch-rank checked)
                                     (dispatch-rank incumbent))))
                (assoc index relation checked)
                index)))
          {}
          (remove nil? dispatches)))

(defn- approval-work-key
  "Exact work relation named by one approval fact."
  [approval]
  [(:review/org-id approval)
   (:review/project approval)
   (:review/document approval)
   (:review/garden approval)
   (:review/source-locale approval)
   (:review/locale approval)
   (:review/revision approval)])

(defn- work-state
  [work receipt approval dispatch]
  (cond
    (nil? (:translation/source-revision work)) :revision_unresolved
    (some? approval) :approved
    (and (some? receipt) (not (evidence-law/content-bound? receipt))) :evidence_unbound
    (some? receipt) :ready
    (= :dispatch/accepted (:dispatch/outcome dispatch)) :in_flight
    (= :dispatch/failed (:dispatch/outcome dispatch)) :failed
    (= :dispatch/rejected (:dispatch/outcome dispatch)) :rejected
    (= :dispatch/unreachable (:dispatch/outcome dispatch)) :stale
    (contains? #{:dispatch/completed :dispatch/duplicate}
               (:dispatch/outcome dispatch)) :evidence_missing
    :else :missing))

(defn- allowed-actions
  [state]
  (case state
    :missing [:dispatch]
    (:failed :rejected :evidence_missing :evidence_unbound) [:retry]
    []))

(defn- base-row
  [work state receipt approval]
  {:publication (:publication/id work)
   :document (:translation/document work)
   :garden (:translation/garden work)
   :project (:translation/project work)
   :source_locale (:translation/source-locale work)
   :locale (:translation/locale work)
   :revision (:translation/source-revision work)
   :revision_selector (:translation/revision-selector work)
   :title (:translation/title work)
   :work_state state
   ;; Candidate existence is not reviewability. Only a runtime edge that has
   ;; loaded and authenticated both source and target bytes may raise this to
   ;; true; keeping the reusable projection false prevents a future MCP/HTTP
   ;; adapter from accidentally treating a receipt as content admission.
   :candidate_present (boolean (and receipt
                                    (evidence-law/content-bound? receipt)))
   :reviewable false
   :approved (boolean approval)
   :allowed_actions (allowed-actions state)})

(defn- attach-observations
  [row receipt approval dispatch]
  (cond-> row
    receipt
    (assoc :translation_revision (:translation/revision receipt)
           :translated_at (:translation/at receipt))

    approval
    (assoc :approved_at (:review/at approval))

    dispatch
    (assoc :dispatch_outcome (:dispatch/outcome dispatch))

    (some? (:dispatch/detail dispatch))
    (assoc :dispatch_detail (:dispatch/detail dispatch))))

(defn- inventory-row
  [receipts-by-work evidence dispatches-by-work work]
  (let [relation (work-key work)
        revision (:translation/source-revision work)
        receipt (get receipts-by-work relation)
        approval (when (some? revision)
                   (translation-evidence/approval-for
                    evidence
                    (:translation/document work)
                    (:translation/garden work)
                    (:translation/locale work)
                    revision))
        dispatch (get dispatches-by-work relation)
        state (work-state work receipt approval dispatch)]
    (attach-observations (base-row work state receipt approval)
                         receipt approval dispatch)))

(defn project-inventory
  "Left-join observed evidence onto resource-derived translation work.

  The result has exactly one row per supplied work item. Orphan receipts,
  approvals, and dispatches cannot create rows. Receipt selection uses the
  evidence law's total supersession order, and approval selection is delegated
  to `domain.translation-evidence/approval-for`, so the publication gate and
  inventory agree about which output is current.

  Values are ready for the existing recursive wire encoder: semantic enum and
  action values remain keywords here and become strings only at the HTTP edge."
  [work receipts approvals dispatches]
  (let [receipts-by-work
        (translation-evidence/current-receipts-by-work receipts)
        desired-relations (set (map work-key work))
        desired-receipts (into []
                               (keep #(get receipts-by-work %))
                               desired-relations)
        desired-approvals (->> approvals
                               (map evidence-law/assert-approval!)
                               (filterv #(contains? desired-relations
                                                    (approval-work-key %))))
        ;; Build the established evidence projection from only the receipts that
        ;; won the source-locale-aware join above. Its older key intentionally
        ;; omits source locale, so feeding all receipt history into it here would
        ;; let a newer translation from a document's former source locale win.
        evidence (translation-evidence/evidence
                  {:receipts desired-receipts
                   :approvals desired-approvals})
        dispatches-by-work (index-dispatches dispatches)]
    (mapv #(inventory-row receipts-by-work evidence dispatches-by-work %) work)))
