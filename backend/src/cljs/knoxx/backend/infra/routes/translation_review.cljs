(ns knoxx.backend.infra.routes.translation-review
  "Translation approval facade.

  Lists resource translation work, appends candidate-bound split reviews, and
  records whole-output approval. Everything in and out is CLJS data — no
  Fastify handle enters or leaves this namespace. The owning extern adapter is
  `knoxx.backend.extern.fastify.translation-review`.

  Approval is evidence, not an effect. Nothing here materializes anything, and
  the card is explicit about why: an approval may make a publication plan
  admissible, but it must not itself publish. That separation is what lets a
  reviewer accept a translation without also deciding when the bytes go live."
  (:require [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.domain.translation-review-inventory :as inventory]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.infra.translation-split-projection :as split-projection]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-evidence :as law]
            [knoxx.backend.law.translation-split :as split-law]
            [knoxx.backend.law.translation-split-turn :as split-turn-law]
            [promesa.core :as p]))

(def ^:private split-lineage-keys
  "The all-or-none completed-receipt link to canonical split authority."
  [:translation/split-manifest-id
   :translation/candidate-claim-id
   :translation/candidate-set-id
   :translation/candidate-set-digest
   :translation/split-count
   :translation/split-turn-admitted-at])

(defn- complete-split-lineage?
  [receipt]
  (and receipt (every? #(contains? receipt %) split-lineage-keys)))

(defn- split-refusal!
  [message code details]
  (throw (ex-info message (merge {:status 409 :code code} details))))

(defn- manifest-receipt-binding
  "Every immutable source and tenant coordinate shared by a manifest/receipt."
  [manifest]
  [(:split-manifest/org-id manifest)
   (:split-manifest/project manifest)
   (:split-manifest/garden manifest)
   (:split-manifest/document manifest)
   (:split-manifest/source-locale manifest)
   (:split-manifest/target-locale manifest)
   (:split-manifest/source-revision manifest)])

(defn- receipt-manifest-binding
  [receipt]
  [(:translation/org-id receipt)
   (:translation/project receipt)
   (:translation/garden receipt)
   (:translation/document receipt)
   (:translation/source-locale receipt)
   (:translation/locale receipt)
   (:translation/source-revision receipt)])

(defn- assert-lineage-binding!
  "Require one receipt, authenticated turn, and authenticated set to be exact."
  [receipt turn candidate-set]
  (let [manifest (:translation-turn/manifest turn)
        claim (:translation-turn/candidate-claim turn)
        expected [(:translation/split-manifest-id receipt)
                  (:translation/candidate-claim-id receipt)
                  (:translation/candidate-set-id receipt)
                  (:translation/candidate-set-digest receipt)
                  (:translation/split-count receipt)
                  (:translation/split-turn-admitted-at receipt)
                  (:translation/dispatch-key receipt)
                  (receipt-manifest-binding receipt)]
        actual [(:split-manifest/id manifest)
                (:candidate-claim/id claim)
                (:candidate-set/id candidate-set)
                (:candidate-set/digest candidate-set)
                (count (:candidate-set/members candidate-set))
                (:translation-turn/admitted-at turn)
                (:translation-turn/dispatch-key turn)
                (manifest-receipt-binding manifest)]]
    (when-not (and (= expected actual)
                   ;; Initial completion carries the raw candidate revision.
                   ;; A review projection intentionally carries an effective or
                   ;; revoking revision instead, identified by review order.
                   (or (contains? receipt :translation/split-review-order)
                       (= (:translation/revision receipt)
                          (:candidate-set/revision candidate-set))))
      (split-refusal!
       "completed translation split lineage disagrees with canonical authority"
       "translation_split_lineage_mismatch"
       {:candidate-set/id (:translation/candidate-set-id receipt)}))
    {:manifest manifest :candidate-set candidate-set}))

(defn- effective-review-status
  [counts total]
  (let [approved (get counts :approved 0)
        rejected (get counts :rejected 0)
        pending (get counts nil 0)]
    (cond
      (= total approved) :fully-approved
      (= total rejected) :fully-rejected
      (= total pending) :pending-review
      (pos? pending) :partial-review
      :else :mixed)))

(def ^:private review-wire-fields
  "Current split-review facts retained by the resource UI projection."
  [[:review/id :review_id]
   [:review/recorded-at :reviewed_at]
   [:review/adequacy :adequacy]
   [:review/fluency :fluency]
   [:review/terminology :terminology]
   [:review/risk :risk]
   [:review/overall :overall]
   [:review/corrected-text :corrected_text]
   [:review/editor-notes :editor_notes]])

(defn- review-wire-values
  "Project only facts actually present on the latest immutable review."
  [review]
  (into {}
        (keep (fn [[review-key wire-key]]
                (when (contains? review review-key)
                  [wire-key (get review review-key)])))
        review-wire-fields))

(defn- review-label-wire-values
  "Project one authenticated receipt into the historical card's label shape.

  Resource review receipts retain stronger native names (`review_id` and
  `reviewed_at`) while the legacy card consumes `id` and `ts`; publishing both
  aliases keeps one durable history authoritative without inventing a second
  label store or translation workflow. Reviewer identity is always taken from
  the server-attributed principal on the receipt, never from client input."
  [review]
  (let [principal (:review/principal review)
        labeler-id (or (:principal/user-id principal)
                       (:principal/membership-id principal)
                       (:principal/user-email principal))
        labeler-email (or (:principal/user-email principal)
                          (:principal/user-id principal)
                          (:principal/membership-id principal))]
    (merge {:id (:review/id review)
            :review_id (:review/id review)
            :segment_id (:review/split-id review)
            :labeler_id labeler-id
            :labeler_email labeler-email
            :ts (:review/recorded-at review)
            :reviewed_at (:review/recorded-at review)
            :review_status (:review/status review)}
           (review-wire-values review))))

(defn- ^:async split-review-projection!
  "Project one authenticated receipt's current per-split review state."
  [translation-split-store digest-hex receipt]
  (let [{:keys [turn candidate-set review-histories]}
        (await
         (split-projection/current-reviewed-output!
          {:split-store translation-split-store :digest-hex digest-hex}
          (:translation/candidate-set-id receipt)))
        manifest (:translation-turn/manifest turn)
        _ (assert-lineage-binding! receipt turn candidate-set)
        candidate-by-split (into {}
                                 (map (juxt :candidate/split-id identity))
                                 (:candidate-set/members candidate-set))
        splits
        (mapv
         (fn [source-split]
           (let [split-id (:split/id source-split)
                 candidate (get candidate-by-split split-id)]
             (when-not (and candidate
                            (= (:split/index source-split)
                               (:candidate/split-index candidate)))
               (split-refusal!
                "candidate set does not cover its manifest in split order"
                "translation_split_candidate_mismatch"
                {:candidate-set/id (:candidate-set/id candidate-set)
                 :split/id split-id}))
             (let [history (get review-histories split-id)
                   effective (split-law/effective-review-receipt
                              digest-hex manifest candidate-set split-id history)
                   ;; The historical document API returned newest labels first.
                   ;; Preserve that card contract while retaining the store's
                   ;; canonical oldest-first ordering internally.
                   labels (mapv review-label-wire-values (reverse history))]
               (merge {:split_id split-id
                       :split_index (:split/index source-split)
                       :source_text (:split/source-text source-split)
                       :candidate_text (:candidate/text candidate)
                       :candidate_digest (:candidate/digest candidate)
                       :review_status (:review/status effective)
                       :label_count (count labels)
                       :labels labels}
                      (review-wire-values effective)))))
         (sort-by :split/index (:split-manifest/splits manifest)))
        counts (frequencies (map :review_status splits))]
    {:candidate_set_id (:candidate-set/id candidate-set)
     :manifest_id (:split-manifest/id manifest)
     :status (effective-review-status counts (count splits))
     :splits (vec splits)}))

(defn- ^:async attach-split-reviews!
  [translation-split-store digest-hex work receipts reviews]
  (let [current-by-work
        (evidence-domain/current-receipts-by-work receipts)]
    (await
     (p/all
      (mapv (fn [item review]
              (let [receipt (get current-by-work (inventory/work-key item))]
                (if (complete-split-lineage? receipt)
                  (p/let [projection
                          (split-review-projection!
                           translation-split-store digest-hex receipt)]
                    (assoc review :split_review projection))
                  (js/Promise.resolve review))))
            work reviews)))))

(defn- ^:async current-receipt!
  "The completed translation this request is about, or nil.

   Loads the receipts and indexes them through
   `domain.translation-evidence/evidence` rather than issuing a targeted query.
   That is more work than one lookup and it is deliberate: which receipt is
   *current* is decided by the evidence law's context-free total order over turn
   admission, candidate generation, manifest-order review coordinates, and
   immutable receipt tiebreaks. Pushing that rule into a store query would
   duplicate it — one copy per store implementation, each free to drift from the
   copy the gate uses. An approval validated against a different receipt than the
   gate will later read is the one failure this card exists to prevent, so the
   rule stays in one place.

   Scoped to the acting tenant and project before indexing. A reviewer must not
   be able to approve a translation belonging to another organization, and the
   dispatch card learned that lesson at the cost of two review rounds."
  [evidence-store scope request receipt-admissible? receipts-transform
   receipts-snapshot]
  (let [query-scope (select-keys scope [:org-id :project])
        ;; Scoped in the query, and checked again here. The query is the
        ;; optimization; the filter is the guarantee, since a replaceable store
        ;; that ignored the scope must not be able to widen what a reviewer can
        ;; approve.
        receipts (->> (if (some? receipts-snapshot)
                        receipts-snapshot
                        (await (store/completed-translations!
                                evidence-store query-scope)))
                      ((or receipts-transform identity))
                      (filterv #(and (= (:org-id scope) (:translation/org-id %))
                                     (= (:project scope) (:translation/project %))
                                     (or (nil? receipt-admissible?)
                                         (receipt-admissible? %)))))
        evidence (evidence-domain/evidence {:receipts receipts})]
    (evidence-domain/receipt-for evidence
                                 (:review/document request)
                                 (:review/garden request)
                                 (:review/locale request)
                                 (:review/revision request))))

(defn ^:async approve-translation!
  "Record approval of one translated revision, or refuse with typed evidence.

   Returns one of:

     {:approval/status :recorded :approval a}   first approval of this output
     {:approval/status :existing :approval a}   already approved, idempotent
     {:approval/refusal r}                      refused, with both sides named

   The request is validated, then the receipt is looked up, then the refusal is
   computed, and only then is anything written. Nothing is persisted on a
   refusal: an approval that was rejected must leave no trace a later read could
   mistake for evidence."
  [{:keys [evidence-store clock authorize-approval! receipt-admissible?
           receipts-transform receipts-snapshot]}
   scope request]
  (when-not (fn? authorize-approval!)
    (throw (ex-info "translation approval requires content admission"
                    {:status 503
                     :code "translation_approval_admission_unavailable"})))
  (let [checked (law/assert-approval-request! request)
        receipt (await (current-receipt! evidence-store scope checked
                                         receipt-admissible? receipts-transform
                                         receipts-snapshot))]
    (if-let [refusal (law/approval-refusal checked receipt)]
      {:approval/refusal refusal}
      (do
        ;; The HTTP adapter supplies the resource/content admission guard. It is
        ;; invoked only after the receipt law has accepted the exact immutable
        ;; coordinates, and before an approval can be persisted. Keeping it as
        ;; an injected boundary makes direct law/facade callers testable while
        ;; preventing a hand-written POST from bypassing the bytes the reviewer
        ;; was required to see.
        (await (authorize-approval! checked receipt))
        (await (store/record-approval!
                evidence-store
                (law/approve checked receipt (:principal scope) (clock))))))))

(defn- scoped-receipts
  [scope receipts]
  (filterv #(and (= (:org-id scope) (:translation/org-id %))
                 (= (:project scope) (:translation/project %)))
           receipts))

(defn- scoped-approvals
  [scope approvals]
  (filterv #(and (= (:org-id scope) (:review/org-id %))
                 (= (:project scope) (:review/project %)))
           approvals))

(defn- dispatch-matches-work?
  "Whether a point-read record's body names the work encoded by its lookup key."
  [scope work record]
  (= [(:org-id scope)
      (:project scope)
      (:translation/garden work)
      (:translation/document work)
      (:translation/source-locale work)
      (:translation/locale work)
      (:translation/source-revision work)]
     [(:dispatch/org-id record)
      (:dispatch/project record)
      (:dispatch/garden record)
      (:dispatch/document record)
      (:dispatch/source-locale record)
      (:dispatch/locale record)
      (:dispatch/revision record)]))

(defn- ^:async dispatches-for-work!
  "Point-read dispatch evidence for the already-derived desired work.

   At deployment scale this is bounded by the resource inventory (the observed
   case is eighteen rows) and the reads run concurrently.  It deliberately uses
   the existing atomic dispatch key instead of introducing an incidental Mongo
   migration: legacy dispatch rows do not have tenant/project query columns,
   while the key already includes both coordinates and exactly matches the
   claim boundary."
  [evidence-store scope work]
  (await
   (p/all
    (mapv (fn [item]
            (if-let [dispatch-key (inventory/dispatch-lookup-key scope item)]
              (p/let [record (store/dispatch-for-key! evidence-store dispatch-key)]
                ;; The point lookup is an optimization supplied by a replaceable
                ;; store; the key comparison is the tenant/project guarantee.
                ;; Without it, project-inventory's resource relation (which does
                ;; not repeat tenant coordinates) could attach another scope's
                ;; outcome and diagnostic detail to this row.
                (when (and record
                           (or (not= dispatch-key (:dispatch/key record))
                               (not (dispatch-matches-work? scope item record))))
                  (throw (ex-info "translation dispatch lookup returned the wrong claim"
                                  {:expected-dispatch-key dispatch-key
                                   :actual-dispatch-key (:dispatch/key record)
                                   :expected-work (inventory/work-key item)
                                   :actual-work (inventory/work-key record)})))
                record)
              (js/Promise.resolve nil)))
          work))))

(defn ^:async reviewable-translations!
  "Resource-derived translation work, left-joined with observed evidence.

   Resource intent is cardinality authority. A missing receipt is a visible
   `:missing` row rather than no row, and an orphan receipt cannot invent work.
   This is the inverse of the old receipt-first projection that reduced eighteen
   desired relations to the only completed candidate."
  [{:keys [evidence-store split-store digest-hex publication-index
           source-revisions receipts-transform receipts-snapshot]} scope]
  (let [query-scope (select-keys scope [:org-id :project])
        work (mapv #(assoc %
                           :translation/org-id (:org-id scope)
                           :translation/project (:project scope))
                   (inventory/desired-work publication-index source-revisions))
        receipts (scoped-receipts
                  scope
                  ((or receipts-transform identity)
                   (if (some? receipts-snapshot)
                     receipts-snapshot
                     (await (store/completed-translations!
                             evidence-store query-scope)))))
        stored-approvals (scoped-approvals
                          scope
                          (await (store/approvals! evidence-store query-scope)))
        approvals (await
                   (split-projection/current-review-approvals!
                    {:split-store split-store :digest-hex digest-hex}
                    receipts stored-approvals))
        dispatches (await (dispatches-for-work! evidence-store scope work))
        reviews (inventory/project-inventory work receipts approvals dispatches)
        reviews (await (attach-split-reviews! split-store digest-hex
                                              work receipts reviews))]
    {:project (:project scope)
     :reviews reviews}))

(def ^:private default-evaluations
  "Compatibility defaults from the historical OpenPlanner review card."
  {:approved {:review/adequacy "good"
              :review/fluency "good"
              :review/terminology "correct"
              :review/risk "safe"}
   :in-review {:review/adequacy "adequate"
               :review/fluency "adequate"
               :review/terminology "minor_errors"
               :review/risk "safe"}
   :rejected {:review/adequacy "adequate"
              :review/fluency "adequate"
              :review/terminology "minor_errors"
              :review/risk "safe"}})

(def ^:private overall-by-status
  {:approved "approve"
   :in-review "needs_edit"
   :rejected "reject"})

(def ^:private semantic-review-keys
  [:review/adequacy :review/fluency :review/terminology :review/risk
   :review/overall :review/corrected-text :review/editor-notes])

(defn- canonical-review-request
  [request]
  (let [status (:split-review/status request)]
    (when-not (contains? overall-by-status status)
      (throw (ex-info "invalid translation split review status"
                      {:status 400
                       :code "translation_split_review_invalid"})))
    (cond-> (merge (get default-evaluations status)
                   (select-keys request
                                [:review/adequacy :review/fluency
                                 :review/terminology :review/risk
                                 :review/editor-notes])
                   {:review/overall (get overall-by-status status)})
      (some? (:split-review/corrected-text request))
      (assoc :review/corrected-text
             (:split-review/corrected-text request)))))

(defn- same-semantic-review?
  [receipt principal canonical-request]
  (and (= principal (:review/principal receipt))
       (= canonical-request (select-keys receipt semantic-review-keys))))

(defn- operation-id
  "Derive a retry-stable single-split operation while allowing A→B→A history."
  [digest-hex candidate-set-id split-id principal canonical-request effective]
  (if (and effective
           (same-semantic-review? effective principal canonical-request))
    (:review/operation-id effective)
    (str "publication-split-review-"
         (digest-hex
          (pr-str
           [candidate-set-id
            split-id
            [(:principal/user-id principal)
             (:principal/user-email principal)
             (:principal/membership-id principal)]
            [(:review/adequacy canonical-request)
             (:review/fluency canonical-request)
             (:review/terminology canonical-request)
             (:review/risk canonical-request)
             (:review/overall canonical-request)
             (:review/corrected-text canonical-request)
             (:review/editor-notes canonical-request)]
            (:review/id effective)])))))

(defn- bulk-operation-group-id
  "Derive one deterministic rank shared by every receipt in a bulk mutation.

  The group includes the complete canonical per-split intent and the effective
  receipt ids observed before any append. Two concurrent calls from the same
  snapshot therefore either share one idempotent group (equal intent) or receive
  different lexical group ranks (conflicting intent). Every split suffix comes
  after this group, so the review law chooses the same winning group for every
  split even when both calls carry the same server timestamp and their appends
  interleave. A retry after a partial write sees changed effective ids and
  deliberately derives a fresh group that restamps every member coherently."
  [digest-hex candidate-set-id principal plans]
  (str "publication-split-review-bulk-"
       (digest-hex
        (pr-str
         [candidate-set-id
          [(:principal/user-id principal)
           (:principal/user-email principal)
           (:principal/membership-id principal)]
          (mapv (fn [{:keys [split-id canonical effective]}]
                  [split-id
                   [(:review/adequacy canonical)
                    (:review/fluency canonical)
                    (:review/terminology canonical)
                    (:review/risk canonical)
                    (:review/overall canonical)
                    (:review/corrected-text canonical)
                    (:review/editor-notes canonical)]
                   (:review/id effective)])
                plans)]))))

(defn- bulk-operation-id
  "Append the split selector after a group's complete shared lexical rank."
  [group-id split-id]
  (str group-id ":split:" split-id))

(defn- assert-review-scope!
  [scope manifest]
  (when-not (= [(:org-id scope) (:project scope)]
               [(:split-manifest/org-id manifest)
                (:split-manifest/project manifest)])
    (throw (ex-info "translation candidate set belongs to another review scope"
                    {:status 403
                     :code "translation_split_review_scope_mismatch"}))))

(defn- manifest-review-request
  "Project one authenticated manifest onto the current-receipt selector."
  [manifest]
  {:review/document (:split-manifest/document manifest)
   :review/garden (:split-manifest/garden manifest)
   :review/locale (:split-manifest/target-locale manifest)
   :review/revision (:split-manifest/source-revision manifest)})

(defn- ^:async assert-current-review-authority!
  "Require this exact candidate set to own the current completed generation.

   Candidate sets and their review history are append-only, so existence is not
   currentness. The completed-receipt join prevents a caller holding an old set
   id from appending positive evidence after recovery produced a replacement.
   The point-read dispatch check independently binds that receipt to the exact
   completed attempt/run which admitted the turn."
  [evidence-store scope turn candidate-set]
  (let [manifest (:translation-turn/manifest turn)
        candidate-set-id (:candidate-set/id candidate-set)
        record (await (store/dispatch-for-key!
                       evidence-store (:translation-turn/dispatch-key turn)))
        record-current? (split-turn-law/review-binding-matches?
                         record turn)
        receipt (await (current-receipt!
                        evidence-store scope (manifest-review-request manifest)
                        (fn [candidate]
                          (and record-current?
                               (= (:translation/dispatch-key candidate)
                                  (:dispatch/key record))
                               (store/receipt-visible-for-dispatch?
                                candidate record)))
                        nil nil))
        current? (and record-current?
                      (complete-split-lineage? receipt)
                      (= candidate-set-id
                         (:translation/candidate-set-id receipt))
                      (some? (:translation/dispatch-attempt-id receipt))
                      (store/receipt-visible-for-dispatch? receipt record))]
    (when-not current?
      (split-refusal!
       "translation candidate set is no longer the current completed generation"
       "translation_split_candidate_set_stale"
       {:candidate-set/id candidate-set-id
        :current-candidate-set/id (:translation/candidate-set-id receipt)
        :dispatch/outcome (:dispatch/outcome record)}))
    (assert-lineage-binding! receipt turn candidate-set)
    receipt))

(defn- assert-review-dependencies!
  [{:keys [split-store evidence-store digest-hex clock]}]
  (when-not (and split-store evidence-store (fn? digest-hex) (fn? clock))
    (throw (ex-info "translation split review dependencies are unavailable"
                    {:status 503
                     :code "translation_split_review_unavailable"}))))

(defn- ^:async review-authority!
  "Resolve one candidate set and require its exact current completion authority."
  [{:keys [split-store evidence-store]} scope candidate-set-id]
  (let [candidate-set (await (split-store/candidate-set-by-id!
                              split-store candidate-set-id))
        turn (await (split-store/turn-for-candidate-set!
                     split-store candidate-set-id))]
    (when-not (and candidate-set turn)
      (split-refusal! "translation candidate set is not persisted"
                      "translation_split_candidate_set_missing"
                      {:candidate-set/id candidate-set-id}))
    (assert-review-scope! scope (:translation-turn/manifest turn))
    (await (assert-current-review-authority!
            evidence-store scope turn candidate-set))
    {:candidate-set candidate-set :turn turn}))

(defn- ^:async current-review-plan!
  "Read and canonicalize one split before any review append occurs."
  [{:keys [split-store digest-hex]}
   {:keys [candidate-set turn]} split-id request preserve-current-correction?]
  (let [candidate-set-id (:candidate-set/id candidate-set)
        manifest (:translation-turn/manifest turn)
        history (await (split-store/review-history-for-split!
                        split-store candidate-set-id split-id))
        effective (split-law/effective-review-receipt
                   digest-hex manifest candidate-set split-id history)
        request (cond-> request
                  (and preserve-current-correction?
                       (some? (:review/corrected-text effective)))
                  (assoc :split-review/corrected-text
                         (:review/corrected-text effective)))]
    {:split-id split-id
     :history history
     :effective effective
     :canonical (canonical-review-request request)}))

(defn- ^:async append-review-plan!
  "Append one precomputed canonical review at a caller-owned operation/time."
  [{:keys [split-store digest-hex]} scope
   {:keys [candidate-set turn]} {:keys [split-id history canonical]}
   recorded-at review-operation-id]
  (let [manifest (:translation-turn/manifest turn)
        review-request (assoc canonical :review/operation-id review-operation-id)
        attempted (split-law/review-receipt
                   digest-hex manifest candidate-set split-id
                   (:principal scope) recorded-at review-request)
        existing? (boolean (some #(= (:review/id attempted) (:review/id %))
                                 history))
        stored (await (split-store/append-review-receipt!
                       split-store attempted))]
    {:status (if existing? :existing :recorded)
     :receipt stored}))

(defn- ^:async append-current-review!
  "Append one canonical single-split receipt at its current review head.

  Separating the append from projection lets a document-level fast path persist
  every split and then compose once. A crash between appends remains safe: the
  publication gate derives readiness from durable review history, while a retry
  resumes through the same deterministic operation identities."
  [{:keys [digest-hex clock] :as deps} scope
   {:keys [candidate-set] :as authority} split-id request]
  (let [{:keys [canonical effective] :as plan}
        (await (current-review-plan! deps authority split-id request false))
        review-operation-id
        (operation-id digest-hex (:candidate-set/id candidate-set) split-id
                      (:principal scope) canonical effective)]
    (await (append-review-plan! deps scope authority plan (clock)
                                review-operation-id))))

(defn- ^:async project-current-review!
  [{:keys [split-store evidence-store content-root digest-hex clock
           project-reviewed-output!]} candidate-set-id]
  (let [projector (or project-reviewed-output!
                      split-projection/project-reviewed-output!)]
    (await (projector {:split-store split-store
                       :evidence-store evidence-store
                       :content-root content-root
                       :digest-hex digest-hex
                       :clock clock}
                      candidate-set-id))))

(defn- mutation-result
  [status receipts projected]
  (cond-> {:split-review/status status
           :split-review/current-status (:translation/review-status projected)
           :split-review/current-translation-receipt
           (:translation/receipt projected)}
    (= 1 (count receipts)) (assoc :split-review/receipt (first receipts))
    (not= 1 (count receipts)) (assoc :split-review/receipts receipts)))

(defn- ^:async append-bulk-review-plans!
  "Append one operation group in manifest order.

   Sequential writes make interruption/retry behavior easy to observe, but
   correctness does not depend on their order: concurrent groups share one
   timestamp internally and the review law selects one common lexical rank."
  [deps scope authority plans recorded-at group-id]
  (loop [remaining plans
         results []]
    (if-let [plan (first remaining)]
      (recur (next remaining)
             (conj results
                   (await
                    (append-review-plan!
                     deps scope authority plan recorded-at
                     (bulk-operation-id group-id (:split-id plan))))))
      results)))

(defn ^:async record-split-review!
  "Append one server-attributed split review and refresh its current output.

  The request selects only a candidate set, split, verdict, optional correction,
  and optional evaluation fields. Tenant/project, principal, clock, immutable
  candidate coordinates, and retry identity are all resolved on this side of
  the trust boundary."
  [deps scope request]
  (assert-review-dependencies! deps)
  (let [candidate-set-id (:split-review/candidate-set-id request)
        split-id (:split-review/split-id request)
        authority (await (review-authority! deps scope candidate-set-id))
        appended (await (append-current-review! deps scope authority split-id
                                                request))
        projected (await (project-current-review! deps candidate-set-id))]
    (mutation-result (:status appended) [(:receipt appended)] projected)))

(defn ^:async record-candidate-set-review!
  "Apply one document-level verdict to every split in an exact candidate set.

  The server enumerates the persisted manifest, so a stale or malicious client
  cannot omit members or smuggle split ids from another candidate. Review facts
  are appended in manifest order and the effective document is projected once.
  A partial crash is retry-safe and publication-safe for the reasons documented
  by `append-current-review!`."
  [deps scope request]
  (assert-review-dependencies! deps)
  (when (contains? request :split-review/corrected-text)
    (throw (ex-info
            "document-level review cannot apply one correction to every split"
            {:status 400
             :code "translation_split_bulk_correction_unsupported"})))
  (let [candidate-set-id (:split-review/candidate-set-id request)
        authority (await (review-authority! deps scope candidate-set-id))
        split-ids (->> (get-in authority [:turn :translation-turn/manifest
                                          :split-manifest/splits])
                       (sort-by :split/index)
                       (mapv :split/id))
        ;; One clock sample is the causal coordinate of the whole document action.
        ;; Never sample per split: same-millisecond conflicting groups must remain
        ;; internally tied so their operation rank, not append interleaving, wins.
        recorded-at ((:clock deps))
        plans (await
               (p/all
                (mapv #(current-review-plan! deps authority % request true)
                      split-ids)))
        complete-replay?
        (every? #(same-semantic-review? (:effective %)
                                       (:principal scope)
                                       (:canonical %))
                plans)
        appended
        (if complete-replay?
          (mapv (fn [{:keys [effective]}]
                  {:status :existing :receipt effective})
                plans)
          (let [group-id (bulk-operation-group-id
                          (:digest-hex deps) candidate-set-id
                          (:principal scope) plans)]
            (await (append-bulk-review-plans!
                    deps scope authority plans recorded-at group-id))))
        projected (await (project-current-review! deps candidate-set-id))
        status (if complete-replay? :existing :recorded)
        receipts (mapv :receipt appended)]
    (-> (mutation-result status receipts projected)
        (dissoc :split-review/receipt)
        (assoc :split-review/receipts receipts))))
