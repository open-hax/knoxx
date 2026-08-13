(ns knoxx.backend.infra.publication-effects
  "The publication effect boundary: executes an already-decided plan.

  This layer owns no publication semantics. It does not decide whether something
  should be public, and it never reinterprets desired state — given a `:remove`
  plan for a `:published` intent it removes, because the planner already
  resolved that. A hosted publishing backend, a filesystem, Git, and object
  storage are interchangeable implementations of one protocol; no
  adapter-specific identifier crosses upward, and no adapter is named here.

  Both directions are validated. The plan is checked before any effect runs, and
  the adapter's return value is checked before a caller reads fields off it — an
  adapter is replaceable, so its output is untrusted input."
  (:require [clojure.string :as str]
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
  (when (nil? concrete-revision)
    (throw (ex-info "publish idempotency key requires a concrete revision"
                    {:publication/id (:publication/id intent)})))
  (->> (conj (mapv #(pr-str (get intent %)) key-dimensions)
             (pr-str adapter-id)
             (pr-str concrete-revision))
       (str/join "|")))

;; ── Replay-safe publish ────────────────────────────────────────────────────

(defn- ^:async reconcile-in-flight!
  "A claimed-but-never-completed key means the previous attempt's outcome is
   unknown — the classic ambiguous external response. Observe the target rather
   than blindly republishing: if the artifact is already there, record and
   return it; if not, the claim is abandoned so the retry can proceed."
  [store target ctx op]
  (let [idempotency-key (:idempotency/key op)
        observed (await (observe! target ctx (:intent op)))]
    (if (and observed
             (= (:materialized/revision observed) (:concrete-revision op))
             (= (:materialized/path observed) (get-in op [:intent :publication/path])))
      (let [receipt (law/assert-receipt!
                     {:receipt/type :publication/materialized
                      :materialized/revision (:materialized/revision observed)
                      :materialized/path (:materialized/path observed)
                      :idempotency/key idempotency-key})]
        (complete! store idempotency-key receipt)
        receipt)
      (do (release! store idempotency-key)
          nil))))

(defn ^:async publish-once!
  "Publish under an atomic key reservation. Replaying an identical key can never
   create a second public artifact."
  [store target ctx op]
  (let [idempotency-key (:idempotency/key op)
        reservation (reserve! store idempotency-key)]
    (case (:reservation/status reservation)
      :done (law/assert-receipt! (:receipt reservation))

      :in-flight
      (or (await (reconcile-in-flight! store target ctx op))
          (await (publish-once! store target ctx op)))

      :reserved
      (try
        (let [receipt (law/assert-receipt! (await (publish! target ctx op)))]
          (complete! store idempotency-key receipt)
          receipt)
        (catch :default err
          (release! store idempotency-key)
          (throw err))))))

;; ── Plan execution ─────────────────────────────────────────────────────────

(defn- ^:async execute-publish!
  [store target ctx plan artifact]
  (let [intent (:intent plan)
        idempotency-key (publish-idempotency-key (target-id target)
                                                 intent
                                                 (:concrete-revision plan))]
    (await (publish-once! store target ctx
                          {:intent intent
                           :artifact artifact
                           :previous (:previous plan)
                           :concrete-revision (:concrete-revision plan)
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
  [target plan err]
  (law/assert-receipt!
   (cond-> {:receipt/type :publication/failed
            :failure/reason (or (not-empty (str (ex-message err))) "unknown failure")
            :failure/drift? true}
     (= :publish (:op plan))
     (assoc :idempotency/key
            (try (publish-idempotency-key (target-id target)
                                          (:intent plan)
                                          (:concrete-revision plan))
                 (catch :default _ nil))))))

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
