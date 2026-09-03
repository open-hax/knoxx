(ns knoxx.backend.infra.translation-split-projection
  "Materialize the current reviewed output for one durable candidate set.

  Split receipts are evaluation evidence; completed-translation receipts are
  what the publication gate indexes. This adapter is the single bridge between
  them. After any review mutation it folds complete history, writes either the
  fully corrected effective bytes or a non-ready raw projection, and records a
  new immutable translation receipt. The non-ready projection is deliberate:
  it supersedes an earlier ready revision and therefore revokes stale whole-file
  approval after a later rejection or edit request."
  (:require [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.translation-agent-content :as content]
            [knoxx.backend.infra.translation-content-integrity :as integrity]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-evidence :as evidence-law]
            [knoxx.backend.law.translation-split :as split-law]
            [knoxx.backend.law.translation-split-turn :as split-turn-law]))

(defn- split-lineage
  "Receipt coordinates inherited from one authenticated turn and candidate set."
  [turn candidate-set reviewed-output]
  {:translation/split-manifest-id
   (get-in turn [:translation-turn/manifest :split-manifest/id])
   :translation/candidate-claim-id
   (get-in turn [:translation-turn/candidate-claim :candidate-claim/id])
   :translation/candidate-set-id (:candidate-set/id candidate-set)
   :translation/candidate-set-digest (:candidate-set/digest candidate-set)
   :translation/split-count (count (:candidate-set/members candidate-set))
   :translation/split-turn-admitted-at (:translation-turn/admitted-at turn)
   :translation/split-review-order
   (:translation-reviewed-output/review-order reviewed-output)})

(defn- assert-record-binding!
  "Refuse projection through a dispatch record other than the admitted turn."
  [record turn]
  (when-not (and (= :dispatch/completed (:dispatch/outcome record))
                 (split-turn-law/review-binding-matches? record turn))
    (throw (ex-info "translation review projection does not match its dispatch"
                    {:expected (split-turn-law/turn-review-binding turn)
                     :actual (split-turn-law/dispatch-review-binding record)
                     :dispatch/outcome (:dispatch/outcome record)})))
  record)

(defn- ^:async histories!
  "Read complete authenticated review history for every manifest split."
  [store candidate-set-id manifest]
  (loop [remaining (:split-manifest/splits manifest)
         histories {}]
    (if-let [source-split (first remaining)]
      (let [split-id (:split/id source-split)
            history (await (split-store/review-history-for-split!
                            store candidate-set-id split-id))]
        (recur (next remaining) (assoc histories split-id history)))
      histories)))

(defn- ^:async authority!
  "Resolve and authenticate the exact turn/set pair named by a review mutation."
  [store candidate-set-id]
  (let [candidate-set (await (split-store/candidate-set-by-id!
                              store candidate-set-id))
        turn (await (split-store/turn-for-candidate-set!
                     store candidate-set-id))]
    (when-not (and candidate-set turn)
      (throw (ex-info "translation candidate set is not persisted"
                      {:candidate-set/id candidate-set-id})))
    {:turn turn :candidate-set candidate-set}))

(defn ^:async current-reviewed-output!
  "Read one candidate set's complete durable review snapshot.

  The returned value is side-effect free and is suitable for publication-gate
  admission as well as materialization. Keeping the fold here ensures the gate
  and the projector cannot disagree about which immutable review history is
  current."
  [{:keys [split-store digest-hex]} candidate-set-id]
  (when-not (and split-store (fn? digest-hex))
    (throw (ex-info "translation split review authority is unavailable"
                    {:status 503
                     :code "translation_split_evidence_unavailable"})))
  (let [{:keys [turn candidate-set] :as authority}
        (await (authority! split-store candidate-set-id))
        manifest (:translation-turn/manifest turn)
        review-histories (await (histories! split-store candidate-set-id manifest))]
    (assoc authority
           :review-histories review-histories
           :reviewed-output
           (split-law/reviewed-output
            digest-hex manifest candidate-set review-histories))))

(defn- receipt-reviewed-binding
  "Every fact a completed receipt claims about its reviewed split authority."
  [receipt]
  [(:translation/split-manifest-id receipt)
   (:translation/candidate-claim-id receipt)
   (:translation/candidate-set-id receipt)
   (:translation/candidate-set-digest receipt)
   (:translation/split-count receipt)
   (:translation/split-turn-admitted-at receipt)
   (:translation/org-id receipt)
   (:translation/project receipt)
   (:translation/garden receipt)
   (:translation/document receipt)
   (:translation/source-locale receipt)
   (:translation/locale receipt)
   (:translation/source-revision receipt)
   (:translation/dispatch-key receipt)
   (:translation/revision receipt)
   (:translation/content-digest receipt)
   (:translation/split-review-order receipt)])

(defn- authority-reviewed-binding
  "The exact receipt facts recomputed from current durable review history."
  [turn candidate-set reviewed-output]
  (let [manifest (:translation-turn/manifest turn)
        claim (:translation-turn/candidate-claim turn)]
    [(:split-manifest/id manifest)
     (:candidate-claim/id claim)
     (:candidate-set/id candidate-set)
     (:candidate-set/digest candidate-set)
     (count (:candidate-set/members candidate-set))
     (:translation-turn/admitted-at turn)
     (:split-manifest/org-id manifest)
     (:split-manifest/project manifest)
     (:split-manifest/garden manifest)
     (:split-manifest/document manifest)
     (:split-manifest/source-locale manifest)
     (:split-manifest/target-locale manifest)
     (:split-manifest/source-revision manifest)
     (:translation-turn/dispatch-key turn)
     (:translation-reviewed-output/revision reviewed-output)
     (integrity/content-digest (:translation-reviewed-output/text reviewed-output))
     (:translation-reviewed-output/review-order reviewed-output)]))

(defn current-ready-receipt?
  "Whether `receipt` exactly materializes a ready durable review snapshot.

  This is the publication gate's fail-closed join. A review receipt is durable
  before its content projection, so a process can die between those writes. In
  that window an earlier whole-output approval must not remain publishable.
  Recomputing and comparing every lineage/content coordinate makes readiness a
  property of current review history rather than projector liveness."
  [receipt {:keys [turn candidate-set reviewed-output]}]
  (let [checked (evidence-law/assert-receipt! receipt)]
    (and (= :ready (:translation-reviewed-output/status reviewed-output))
         (= (receipt-reviewed-binding checked)
            (authority-reviewed-binding turn candidate-set reviewed-output)))))

(defn- split-backed-receipt?
  [receipt]
  (some? (:translation/candidate-set-id receipt)))

(defn- ^:async ready-split-receipts!
  "Return the exact split-backed receipts that match current ready history."
  [deps receipts]
  (let [split-receipts (filterv split-backed-receipt? receipts)]
    (loop [remaining (distinct (map :translation/candidate-set-id split-receipts))
           snapshots {}]
      (if-let [candidate-set-id (first remaining)]
        (recur (next remaining)
               (assoc snapshots candidate-set-id
                      (await (current-reviewed-output! deps candidate-set-id))))
        (into #{}
              (filter (fn [receipt]
                        (current-ready-receipt?
                         receipt
                         (get snapshots (:translation/candidate-set-id receipt)))))
              split-receipts)))))

(defn ^:async current-review-approvals!
  "Fail closed approvals whose split receipt is stale against durable history.

  Legacy approvals pass through unchanged. If an approval binds any split-
  backed *current* receipt, that exact receipt must match a currently ready
  reviewed output. Selecting the current receipt first is load-bearing: two
  generations can claim the same output revision and digest, and readiness from
  an older generation must never authorize the newer one. This closes the
  rejection→projection crash window without changing whether a raw candidate
  counts as translated work."
  [deps receipts approvals]
  (let [current-receipts (vec (evidence-domain/current-receipts receipts))
        split-receipts (filterv split-backed-receipt? current-receipts)]
    (if (or (empty? split-receipts) (empty? approvals))
      approvals
      (let [ready (await (ready-split-receipts! deps split-receipts))]
        (filterv
         (fn [approval]
           (let [bound (filterv #(evidence-law/approval-current? approval %)
                                split-receipts)]
             (or (empty? bound)
                 (boolean (some ready bound)))))
         approvals)))))

(defn ^:async project-reviewed-output!
  "Write and record the deterministic current review projection.

  Equal retries return the first receipt. A delayed older projection cannot
  replace a newer review state because completed receipt selection compares the
  manifest-order review vector carried in `:translation/split-review-order`."
  [{:keys [split-store evidence-store content-root digest-hex clock]}
   candidate-set-id]
  (let [{:keys [turn candidate-set reviewed-output]}
        (await (current-reviewed-output!
                {:split-store split-store :digest-hex digest-hex}
                candidate-set-id))
        record (assert-record-binding!
                (await (evidence-store/dispatch-for-key!
                        evidence-store (:translation-turn/dispatch-key turn)))
                turn)
        revision (:translation-reviewed-output/revision reviewed-output)
        target-text (:translation-reviewed-output/text reviewed-output)
        _ (await (content/write! content-root record revision target-text))
        receipt (dispatch-law/translation-receipt
                 record revision (clock) (integrity/content-digest target-text)
                 (split-lineage turn candidate-set reviewed-output))
        stored (await (evidence-store/record-translation! evidence-store receipt))]
    {:translation/receipt stored
     :translation/review-status (:translation-reviewed-output/status reviewed-output)
     :translation/review-refusal
     (:translation-reviewed-output/refusal reviewed-output)}))
