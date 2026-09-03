(ns knoxx.backend.infra.translation-agent-sink
  "The contract-backed half of `save_translation`.

  `save_translation` has always had one sink: an OpenPlanner translation
  segment. That is the right sink for a CMS document whose truth lives in
  OpenPlanner, and it is the wrong one for a publication document declared by a
  deployment contract — writing there would make the transport this cutover
  removes a hard dependency of the workflow replacing it.

  So the tool gains a second sink, chosen by the session's own resource
  policies rather than by configuration. A session started by the publication
  translation trigger carries `dispatch_key` and `run_id`; nothing else does.
  That makes the discriminator a property of *why this session exists*, which is
  the only thing that could correctly decide it — a config flag would have made
  one deployment able to send CMS translations into publication evidence.

  ## Nothing about translation evidence is decided here

  This namespace joins, validates, writes bytes, and then hands the run to
  `infra.translation-dispatch/resolve-batch-report!` — the same completion path
  the OpenPlanner worker's status callback goes through. The drift guard, the
  receipt minting, the claim settlement and the refusal law all stay there,
  unduplicated. That is deliberate and it is the whole shape of the composition:
  an agent actor is a different *producer* of translations, not a different
  *kind of evidence*.

  The one thing that happens before the handoff is the content write, and the
  order matters in the same way `record-completion!`'s does. Bytes are written
  first, so a crash between the two leaves content with no receipt — reconciled
  as 'not translated yet'. An equal retry reuses that immutable orphan; changed
  bytes conflict instead of replacing content behind the same output revision.
  The reverse order would leave a receipt the gate believes, pointing at bytes
  that were never stored, and the reconciler would then publish the authored
  fallback under an approval granted for agent output."
  (:require [knoxx.backend.infra.translation-agent-content :as content]
            [knoxx.backend.infra.translation-content-integrity :as content-integrity]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-agent-submission :as submission-law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-split :as split-law]))

(defn- claim-binding-refusal
  "Why the claim found for `run-id` is not the session's exact binding, or nil.

   The dispatch key is rechecked even though the run id already found the
   record. The run id is derived from the key, so agreement is expected — which
   is exactly why disagreement means something has gone wrong that no other
   check would catch: a store returning a record it was not asked for, or a
   trigger that built its overlay from one claim and its session id from
   another. Cheap, and it fails at the boundary instead of minting a receipt
   against the wrong revision."
  [record policies]
  (cond
    (nil? record)
    {:refusal/type :dispatch-record-missing
     :refusal/actual {:run_id (:run_id policies)}}

    (not= (:dispatch/key record) (:dispatch_key policies))
    {:refusal/type :worker-batch-mismatch
     :refusal/expected (:dispatch/key record)
     :refusal/actual (:dispatch_key policies)}

    :else nil))

(defn- terminal-claim-refusal
  "Why a non-idempotent submission cannot act on a settled dispatch claim."
  [record]
  {:refusal/type :dispatch-already-resolved
   :refusal/expected :dispatch/accepted
   :refusal/actual (:dispatch/outcome record)})

(defn- dispatch-turn-binding
  "All immutable dispatch coordinates one admitted turn must inherit."
  [record]
  [(:dispatch/key record)
   (:dispatch/batch-id record)
   (:dispatch/at record)
   (:dispatch/org-id record)
   (:dispatch/project record)
   (:dispatch/garden record)
   (:dispatch/document record)
   (:dispatch/source-locale record)
   (:dispatch/locale record)
   (:dispatch/revision record)
   (dispatch-law/output-revision record)])

(defn- persisted-turn-binding
  "The same coordinates recomputed from persisted turn authority."
  [turn]
  (let [manifest (:translation-turn/manifest turn)
        claim (:translation-turn/candidate-claim turn)]
    [(:translation-turn/dispatch-key turn)
     (:translation-turn/run-id turn)
     (:translation-turn/admitted-at turn)
     (:split-manifest/org-id manifest)
     (:split-manifest/project manifest)
     (:split-manifest/garden manifest)
     (:split-manifest/document manifest)
     (:split-manifest/source-locale manifest)
     (:split-manifest/target-locale manifest)
     (:split-manifest/source-revision manifest)
     (:candidate-claim/revision claim)]))

(defn- turn-binding-refusal
  "Refuse a valid turn that belongs to a different dispatch relation."
  [record turn]
  (when-not (= (dispatch-turn-binding record)
               (persisted-turn-binding turn))
    {:refusal/type :translation-turn-mismatch
     :refusal/expected (dispatch-turn-binding record)
     :refusal/actual (persisted-turn-binding turn)}))

(defn- claim-member-at-index
  "Resolve the pre-admitted candidate attempt at one server split index."
  [turn index]
  (nth (get-in turn [:translation-turn/candidate-claim
                     :candidate-claim/members])
       index))

(defn- split-lineage
  "Project the complete raw candidate lineage onto a translation receipt."
  [candidate-set admitted-at]
  {:translation/split-manifest-id
   (:candidate-set/manifest-id candidate-set)
   :translation/candidate-claim-id
   (:candidate-set/claim-id candidate-set)
   :translation/candidate-set-id (:candidate-set/id candidate-set)
   :translation/candidate-set-digest (:candidate-set/digest candidate-set)
   :translation/split-count (count (:candidate-set/members candidate-set))
   :translation/split-turn-admitted-at admitted-at})

(defn- completion-receipt?
  "Whether a stored receipt is the exact completion of this raw candidate set."
  [record candidate-set receipt]
  (let [lineage (split-lineage candidate-set (:dispatch/at record))]
    (and (= (:dispatch/key record) (:translation/dispatch-key receipt))
         (= (dispatch-law/output-revision record) (:translation/revision receipt))
         (= (content-integrity/content-digest (:candidate-set/text candidate-set))
            (:translation/content-digest receipt))
         (every? (fn [[key value]] (= value (get receipt key))) lineage))))

(defn- ^:async existing-completion!
  "Read one exact idempotent completion receipt from its tenant scope."
  [evidence-store record candidate-set]
  (some #(when (completion-receipt? record candidate-set %) %)
        (await (store/completed-translations!
                evidence-store
                {:org-id (:dispatch/org-id record)
                 :project (:dispatch/project record)}))))

(defn- progress
  "Describe durable partial coverage without exposing unreviewed target bytes."
  [turn completed total]
  {:translation/progress
   {:turn-id (:translation-turn/id turn)
    :completed completed
    :total total}})

(defn- store-conflict-refusal
  "Translate an immutable candidate collision into an agent-correctable refusal."
  [err]
  (when (= :immutable-identity
           (:translation-split-store/conflict (ex-data err)))
    {:refusal/type :pair-candidate-conflict
     :refusal/expected
     (get-in (ex-data err) [:translation-split-store/existing :candidate/digest])
     :refusal/actual
     (get-in (ex-data err) [:translation-split-store/attempted :candidate/digest])}))

(defn- candidate-conflict-refusal
  "Describe changed bytes submitted behind one already-settled attempt."
  [existing attempted]
  {:refusal/type :pair-candidate-conflict
   :refusal/expected (:candidate/digest existing)
   :refusal/actual (:candidate/digest attempted)})

(defn- ^:async persist-candidate!
  "Append one authenticated split candidate, returning a typed conflict."
  [translation-store turn candidate]
  (try
    {:candidate (await (split-store/append-candidate-split!
                        translation-store (:translation-turn/id turn) candidate))}
    (catch :default err
      (if-let [refusal (store-conflict-refusal err)]
        {:refusal refusal}
        (throw err)))))

(defn- ^:async stored-candidate-for-attempt!
  "Read the first durable bytes owned by one admitted candidate attempt."
  [translation-store turn candidate]
  (some #(when (= (:candidate/attempt-id candidate)
                  (:candidate/attempt-id %))
           %)
        (await (split-store/candidate-splits-for-turn!
                translation-store (:translation-turn/id turn)))))

(defn- bind-authoritative-source-text
  "Fill an omitted source echo from the admitted server-owned split.

  Split-backed tool schemas deliberately do not ask a language model to copy
  source bytes: trailing newlines and other invisible Markdown bytes are easy to
  normalize accidentally. Explicit source_text remains checked, preserving the
  diagnostic and tamper refusal for non-tool callers and stale clients."
  [turn pair]
  (if (nil? (:source_text pair))
    (let [index (:segment_index pair)
          splits (get-in turn [:translation-turn/manifest
                               :split-manifest/splits])
          source-split (when (and (integer? index)
                                  (<= 0 index)
                                  (< index (count splits)))
                         (nth splits index))]
      (cond-> pair
        source-split (assoc :source_text (:split/source-text source-split))))
    pair))

(defn- ^:async authenticate-completed-candidate!
  [translation-store turn candidate]
  (let [stored (await (split-store/candidate-splits-for-turn!
                       translation-store (:translation-turn/id turn)))
        existing (some #(when (= (:candidate/attempt-id candidate)
                                 (:candidate/attempt-id %)) %)
                       stored)]
    (cond
      (= existing candidate) {:candidate existing}
      existing {:refusal (candidate-conflict-refusal existing candidate)}
      :else {:refusal {:refusal/type :dispatch-candidate-missing
                       :refusal/actual
                       {:attempt-id (:candidate/attempt-id candidate)}}})))

(defn- ^:async admit-submitted-candidate!
  "Persist an in-flight candidate or authenticate a durable replay.

   An accepted run may be replaying after a process crash. If this attempt
   already has candidate bytes, those first bytes remain authoritative even
   when a nondeterministic provider submits a different translation on replay.
   Returning the durable member as progress lets the same turn continue with
   its missing splits without deleting, overwriting, or conflicting on the
   prefix that already succeeded. Completed runs remain strict: a terminal
   replay must be byte-equal to repair only downstream projection."
  [translation-store record turn candidate]
  (case (:dispatch/outcome record)
    :dispatch/accepted
    (if-let [existing (await (stored-candidate-for-attempt!
                              translation-store turn candidate))]
      {:candidate existing :candidate/reused? true}
      (await (persist-candidate! translation-store turn candidate)))

    :dispatch/completed
    (await (authenticate-completed-candidate!
            translation-store turn candidate))

    {:refusal (terminal-claim-refusal record)}))

(defn- complete-candidate-set
  "Construct the exact raw set from one turn and its durable candidates."
  [digest-hex turn candidates]
  (split-law/complete-candidate-set
   digest-hex
   (:translation-turn/manifest turn)
   (:translation-turn/candidate-claim turn)
   candidates))

(defn- ^:async project-completion-events!
  "Project a receipt only after it is durable, preserving the completion result.

  Event failure is intentionally visible. The dispatch is already completed and
  its receipt is immutable at this point, so the caller can repeat the exact
  final split: the terminal replay path authenticates the stored evidence and
  retries these same stable event ids without translating again."
  [{:keys [emit-candidate-events!]} result turn candidate-set]
  (if-let [receipt (:translation/receipt result)]
    (do
      (when-not (fn? emit-candidate-events!)
        (throw (ex-info "translation candidate event emitter is not configured"
                        {:translation-event/error :emitter-missing
                         :translation/revision (:translation/revision receipt)})))
      (await (emit-candidate-events! {:receipt receipt
                                     :turn turn
                                     :candidate-set candidate-set}))
      result)
    result))

(defn- ^:async settle-accepted-set!
  [deps record turn candidate-set]
  (let [{:keys [content-root]} deps]
    (await (content/write! content-root record
                           (dispatch-law/output-revision record)
                           (:candidate-set/text candidate-set)))
    (let [result
          (await (dispatch/resolve-batch-report!
                  (assoc deps
                         :translation-content-digest
                         (content-integrity/content-digest
                          (:candidate-set/text candidate-set))
                         :translation-split-evidence
                         (split-lineage candidate-set
                                        (:translation-turn/admitted-at turn)))
                  (submission-law/completion-report
                   (:translation-turn/run-id turn)
                   (:dispatch/document-wire-id record))))]
      (await (project-completion-events! deps result turn candidate-set)))))

(defn- ^:async repair-completed-set!
  [deps record turn candidate-set]
  (let [{:keys [content-root evidence-store]} deps]
    (if-let [receipt (await (existing-completion! evidence-store record candidate-set))]
      (do
        ;; Recoverable equal orphan: content is immutable, so recreating a
        ;; missing file under the same output revision cannot change evidence.
        (await (content/write! content-root record
                               (dispatch-law/output-revision record)
                               (:candidate-set/text candidate-set)))
        (await (project-completion-events!
                deps {:translation/receipt receipt} turn candidate-set)))
      {:translation/refusal
       {:refusal/type :dispatch-receipt-missing
        :refusal/actual {:dispatch-key (:dispatch/key record)
                         :candidate-set-id (:candidate-set/id candidate-set)}}})))

(defn- ^:async finish-candidate-set!
  "Persist, write, and settle one exact complete candidate set once."
  [{:keys [split-store digest-hex] :as deps} record turn candidates]
  (let [candidate-set (complete-candidate-set digest-hex turn candidates)
        stored-set (await (split-store/complete-candidate-set!
                           split-store (:translation-turn/id turn) candidate-set))]
    (case (:dispatch/outcome record)
      :dispatch/accepted
      (await (settle-accepted-set! deps record turn stored-set))

      :dispatch/completed
      (await (repair-completed-set! deps record turn stored-set))

      {:translation/refusal (terminal-claim-refusal record)})))

(defn ^:async settle-durable-turn!
  "Settle an accepted turn whose complete candidate evidence survived a crash.

   Returns nil for an incomplete durable prefix, which tells the dispatcher it
   still needs the provider to supply missing splits. A persisted candidate set
   is used directly. Exact full member coverage without the aggregate set is
   completed first, closing the adjacent crash window. No provider bytes enter
   this path."
  [{:keys [split-store] :as deps} record turn]
  (when (= :dispatch/accepted (:dispatch/outcome record))
    (if-let [candidate-set
             (await (split-store/candidate-set-for-turn!
                     split-store (:translation-turn/id turn)))]
      (await (settle-accepted-set! deps record turn candidate-set))
      (let [candidates (await (split-store/candidate-splits-for-turn!
                               split-store (:translation-turn/id turn)))
            total (count (get-in turn [:translation-turn/candidate-claim
                                       :candidate-claim/members]))]
        (when (and (pos? total) (= total (count candidates)))
          (await (finish-candidate-set! deps record turn candidates)))))))

(defn- ^:async submit-to-turn!
  "Validate and record one pair against its exact persisted turn authority."
  [{:keys [split-store digest-hex] :as deps} record turn policies pair]
  (let [pair (bind-authoritative-source-text turn pair)]
    (if-let [refusal (or (submission-law/pair-refusal policies pair)
                         (submission-law/split-pair-refusal turn policies pair))]
      {:translation/refusal refusal}
      (let [member (claim-member-at-index turn (:segment_index pair))
            candidate (split-law/candidate-split
                       digest-hex member (:translated_text pair))
            admitted (await (admit-submitted-candidate!
                             split-store record turn candidate))]
        (if-let [candidate-refusal (:refusal admitted)]
          {:translation/refusal candidate-refusal}
          (let [candidates (await (split-store/candidate-splits-for-turn!
                                   split-store (:translation-turn/id turn)))
                total (count (get-in turn [:translation-turn/candidate-claim
                                           :candidate-claim/members]))]
            (if (< (count candidates) total)
              (progress turn (count candidates) total)
              (await (finish-candidate-set! deps record turn candidates)))))))))

(defn ^:async submit-pair!
  "Record one agent-submitted translation pair as publication evidence.

   Returns `{:translation/receipt r}` on success, or `{:translation/refusal f}`
   with a typed, agent-readable refusal for ordinary bad input.

   `deps` needs `:content-root`, `:evidence-store`, `:split-store`, `:digest-hex`,
   `:clock`, `:emit-candidate-events!` and `:observe-source-revision`. The event
   writer and drift observer are required: omitting either silently loses a
   durable fact the publication workflow depends on."
  [{:keys [evidence-store split-store] :as deps} policies pair]
  (if-let [pair-refusal (submission-law/pair-refusal policies pair)]
    {:translation/refusal pair-refusal}
    (let [record (await (store/dispatch-for-batch! evidence-store
                                                   (:run_id policies)))]
      (if-let [refusal (claim-binding-refusal record policies)]
        {:translation/refusal refusal}
        (if-let [turn (await (split-store/turn-for-run! split-store
                                                       (:run_id policies)))]
          (if-let [refusal (turn-binding-refusal record turn)]
            {:translation/refusal refusal}
            (await (submit-to-turn! deps record turn policies pair)))
          {:translation/refusal
           {:refusal/type :translation-turn-missing
            :refusal/actual {:run-id (:run_id policies)}}})))))

(defn refusal-error
  "The error a refused submission becomes at the tool boundary.

   An `ex-info` rather than a bare `js/Error`, so the refusal type survives for
   a route or a test that wants to classify it, while the message is the
   sentence the agent reads. `law.translation-agent-submission/refusal-message` owns the
   wording for its own refusals; a dispatch-layer refusal reaching here is a
   misconfiguration rather than something an agent can fix, and says so."
  [refusal]
  (let [type (:refusal/type refusal)]
    (ex-info (if (contains? submission-law/pair-refusal-types type)
               (submission-law/refusal-message refusal)
               (str "this translation session is not bound to a live publication"
                    " dispatch claim (" (name type) "); it cannot record"
                    " translation evidence"))
             refusal)))
