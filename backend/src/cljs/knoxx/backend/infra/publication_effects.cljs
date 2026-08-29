(ns knoxx.backend.infra.publication-effects
  "The publication effect boundary: executes an already-decided plan.

  This layer owns no publication semantics. It does not decide whether something
  should be public, and it never reinterprets desired state — given a `:remove`
  plan for a `:published` intent it removes, because the planner already
  resolved that. A hosted publishing backend, a filesystem, Git, and object
  storage are interchangeable implementations of one protocol; no
  adapter-specific identifier crosses upward, and no adapter is named here.

  Both directions are validated. The plan and the artifact are checked before any
  effect runs, and the adapter's return value is checked before a caller reads
  fields off it — an adapter is replaceable, so its output is untrusted input.

  The artifact is produced ABOVE this boundary: `execute-plan!` receives one, and
  no protocol method below returns or constructs one. `law.publication`'s
  `PublicationArtifact` docstring records why, and a test pins it."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [knoxx.backend.domain.publication-receipts :as receipts]
            [knoxx.backend.law.publication-receipts :as law]))

;; ── Protocols ──────────────────────────────────────────────────────────────

(defprotocol IPublicationTarget
  (target-id [target]
    "Stable adapter identity. Part of the idempotency key, so two adapters
     publishing the same intent never share a key.")
  (publish! [target ctx op]
    "Materialize `op`. Returns a Promise of a materialized receipt.")
  (remove! [target ctx intent observed]
    "Remove a prior materialization. Returns a Promise of a removed receipt.")
  (observe! [target ctx intent]
    "Observed materialization for an intent, or nil. Returns a Promise."))

(defprotocol IIdempotencyStore
  (reserve! [store idempotency-key]
    "ATOMICALLY claim `idempotency-key`, or report what is already known about
     it. Returns one of:

       {:reservation/status :done      :receipt r}  already completed
       {:reservation/status :in-flight}             claimed, never completed
       {:reservation/status :reserved}              freshly claimed by us

     Implementations must contain no `await` between reading the key and
     claiming it. A separate existence-check followed by a materialize is NOT
     equivalent: a concurrent call or a crash between the two steps can publish
     the same artifact twice, and the second publication is unrecoverable
     because nothing recorded that the first happened.")
  (complete! [store idempotency-key receipt]
    "Record the receipt for a claimed key.")
  (release! [store idempotency-key]
    "Abandon a claim so a later attempt may retry."))

;; ── Idempotency key ────────────────────────────────────────────────────────

(def key-dimensions
  "Effect-relevant materialization identity. Every dimension that changes what
   is public, and nothing that does not — `:translation/review` is a semantic
   policy the planner already applied, so it must not perturb the key."
  [:publication/id :publication/garden :publication/locale :publication/path])

(defn publish-idempotency-key
  "One stable key per logical materialization.

   A deterministic string rather than a hash: it is reproducible across
   processes and versions, and it is inspectable when a replay has to be
   explained to a human."
  [adapter-id intent concrete-revision]
  (when-not (m/validate law/ConcreteRevision concrete-revision)
    ;; Refusing `:source/current` here is the whole basis of replay safety, and
    ;; a nil check alone did not do it: the selector is a keyword, so it passed
    ;; and produced a stable-looking key for a moving target.
    (throw (ex-info "publish idempotency key requires a concrete revision"
                    {:publication/id (:publication/id intent)
                     :concrete-revision concrete-revision})))
  (->> (conj (mapv #(pr-str (get intent %)) key-dimensions)
             (pr-str (receipts/canonical-title (:document/title intent)))
             (pr-str adapter-id)
             (pr-str concrete-revision))
       (str/join "|")))

;; ── Replay-safe publish ────────────────────────────────────────────────────

(defn- requested-materialization
  "Canonical materialization the current operation is authorized to create."
  [op]
  (receipts/canonical-materialization
   (or (:desired op)
       {:materialized/revision (:concrete-revision op)
        :materialized/path (get-in op [:intent :publication/path])
        :materialized/title (get-in op [:intent :document/title])})))

(defn- ^:async reconcile-in-flight!
  "A claimed-but-never-completed key means the previous attempt's outcome is
   unknown — the classic ambiguous external response. Observe the target rather
   than blindly republishing: if the artifact is already there, record and
   return it; if not, the claim is abandoned so the retry can proceed."
  [store target ctx op]
  (let [idempotency-key (:idempotency/key op)
        observed (some-> (await (observe! target ctx (:intent op)))
                         receipts/canonical-materialization)]
    (if (= observed (requested-materialization op))
      (let [receipt (law/assert-receipt!
                     (assoc observed
                            :receipt/type :publication/materialized
                            :idempotency/key idempotency-key))]
        (complete! store idempotency-key receipt)
        receipt)
      (do (release! store idempotency-key)
          nil))))

(defn- assert-receipt-matches-op!
  "The adapter's receipt must describe the materialization that was *requested*.

   `assert-receipt!` checks only the shape, so a structurally valid receipt
   naming the wrong path, revision, or key passed and was recorded as `:done`
   under the requested key — after which every replay reported convergence for an
  artifact that may never have been materialized. An adapter is replaceable, so
  its agreement with the request is checked rather than assumed."
  [op receipt]
  (let [expected (assoc (requested-materialization op)
                        :idempotency/key (:idempotency/key op))
        actual (assoc (receipts/canonical-materialization receipt)
                      :idempotency/key (:idempotency/key receipt))]
    (when-not (= expected actual)
      (throw (ex-info "adapter receipt does not describe the requested materialization"
                      {:expected expected :actual actual})))
    receipt))

(defn ^:async publish-once!
  "Publish under an atomic key reservation. Replaying an identical key can never
   create a second public artifact."
  [store target ctx op]
  (let [idempotency-key (:idempotency/key op)
        reservation (reserve! store idempotency-key)]
    (case (:reservation/status reservation)
      :done (assert-receipt-matches-op! op (law/assert-receipt! (:receipt reservation)))

      :in-flight
      (or (await (reconcile-in-flight! store target ctx op))
          (await (publish-once! store target ctx op)))

      :reserved
      (try
        (let [receipt (assert-receipt-matches-op!
                       op
                       (law/assert-receipt! (await (publish! target ctx op))))]
          (complete! store idempotency-key receipt)
          receipt)
        (catch :default err
          ;; The claim is deliberately NOT released. A failed publish is an
          ;; *ambiguous* outcome: the artifact may already exist and only the
          ;; response was lost. Releasing here reported the retry as a fresh
          ;; reservation, which skipped observation and published again — safe
          ;; only for a target that independently deduplicates. Leaving the claim
          ;; in flight routes the retry through `reconcile-in-flight!`, which
          ;; observes first and republishes only if nothing is there.
          (throw err))))))

;; ── Plan execution ─────────────────────────────────────────────────────────

(defn- ^:async execute-publish!
  "Validate the artifact, then publish under a reservation.

   The artifact is checked BEFORE the key is derived and before the store is
   touched. Reserved first and validated after, a refusal leaves a claim nobody
   will ever complete: the next attempt reads `:in-flight` and has to go observe
   a target where nothing was written, to learn what was already decided here."
  [store target ctx plan artifact]
  (let [intent (:intent plan)
        concrete-revision (:concrete-revision plan)
        checked (law/assert-artifact! artifact intent concrete-revision)
        idempotency-key (publish-idempotency-key (target-id target)
                                                 intent
                                                 concrete-revision)]
    (await (publish-once! store target ctx
                          {:intent intent
                           :desired (:desired plan)
                           :artifact checked
                           :previous (:previous plan)
                           :concrete-revision concrete-revision
                           :idempotency/key idempotency-key}))))

(defn- ^:async dispatch-plan!
  [store target ctx plan artifact]
  (law/assert-plan! plan)
  (case (:op plan)
    :publish (await (execute-publish! store target ctx plan artifact))

    :remove (law/assert-receipt!
             (await (remove! target ctx (:intent plan) (:observed plan))))

    :noop (law/assert-receipt! {:receipt/type :publication/noop
                                :reason (:reason plan)})

    :blocked (law/assert-receipt! {:receipt/type :publication/blocked
                                   :blockers (vec (:blockers plan))})

    ;; `case` with no default throws on no match, and falling through to nil
    ;; would let an unrecognized op read as success. Neither is acceptable at an
    ;; effect boundary.
    (throw (ex-info "unrecognized publication plan op" {:op (:op plan)}))))

(defn- failure-receipt
  "Every refusal becomes a receipt, and a *typed* refusal keeps its evidence.

   An artifact-revision conflict is the one failure where the reader has to know
   which side was stale, so both revisions are copied onto the receipt rather
   than left inside the message string — `:failure/reason` alone cannot be read
   by anything but a human."
  [target plan err]
  (let [evidence (ex-data err)]
    (law/assert-receipt!
     (cond-> {:receipt/type :publication/failed
              :failure/reason (or (not-empty (str (ex-message err))) "unknown failure")
              :failure/drift? true}
       (= :publish (:op plan))
       (assoc :idempotency/key
              (try (publish-idempotency-key (target-id target)
                                            (:intent plan)
                                            (:concrete-revision plan))
                   (catch :default _ nil)))

        (or (law/artifact-revision-conflict? evidence)
            (law/artifact-locale-conflict? evidence))
        (assoc :failure/conflict evidence)))))

(defn ^:async execute-plan!
  "Execute a validated plan and return a validated receipt.

   Total: every path returns a receipt, including a malformed plan, so a
   reconciler loop never has to distinguish an effect failure from a garbage
   plan by catching. An adapter failure becomes failure/drift evidence rather
   than a thrown exception — desired resource state is never mutated by a failed
   effect, and reconciliation needs to see desired and observed disagree."
  [store target ctx plan artifact]
  (try
    (await (dispatch-plan! store target ctx plan artifact))
    (catch :default err
      (failure-receipt target plan err))))
