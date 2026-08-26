(ns knoxx.backend.infra.publication-reconciler
  "The publication reconciler runtime: turns a trigger into plan → effects and
   records what actually happened.

   One trigger reconciles ONE publication. The runtime loads desired state
   fresh, resolves the intent's declared target through the registry, observes
   that target, asks the pure planner for a decision, executes it through the
   registry boundary, and emits exactly one validated, correlated receipt per
   trigger — materialized, removed, noop, blocked, or failed. There is no path
   that attempts an effect and records nothing, and no path that records a
   materialization an adapter did not confirm.

   The runtime owns observation (`:materialized-publication`) because the
   planner must compare desired state against what the SAME target reports in
   THIS run; a provider-supplied observation could describe somewhere else.
   Translation and review evidence is provider-supplied: the runtime does not
   know where those receipts live and must not guess.

   Nothing here writes desired state. Resources are loaded and read; receipts
   are emitted to the configured sink. A failed effect leaves resource
   declarations byte-identical — drift is reported, never repaired by editing
   the goalposts."
  (:require [knoxx.backend.domain.publication-plan :as plan]
            [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.infra.publication-target-registry :as registry]
            [knoxx.backend.law.publication :as publication-law]
            [knoxx.backend.law.publication-receipts :as receipts-law]
            [knoxx.backend.law.publication-reconciler :as trigger-law]))

;; ── The reconciler bundle ──────────────────────────────────────────────────

(def bundle-functions
  "The function-valued members a reconciler bundle must supply:

     :load-index!         [] -> Promise<resource-index> — fresh desired state
     :evidence-facts      [intent] -> gate evidence map (the four lookups in
                          `gate-evidence-keys`); observation is NOT the
                          provider's to supply
     :artifact-source     [intent concrete-revision] -> Promise<artifact|nil>
     :locale-admissible?  [declaration intent artifact] -> boolean — the
                          locale-catalog guard the registry requires
     :emit-receipt!       [receipt] -> Promise<any> — the receipt sink"
  [:load-index! :evidence-facts :artifact-source :locale-admissible? :emit-receipt!])

(defn make-reconciler
  "Validate and return a reconciler bundle. Construction refuses an incomplete
   bundle so a trigger can never discover halfway through a run that there is
   no artifact source or no receipt sink — both would surface as fake drift or
   as evidence that vanished."
  [{:keys [registry store] :as bundle}]
  (when-not (and (map? registry) (map? (:declarations registry))
                 (map? (:factories registry)))
    (throw (ex-info "reconciler requires a publication target registry"
                    {:bundle/key :registry})))
  (when-not (satisfies? effects/IIdempotencyStore store)
    (throw (ex-info "reconciler requires an IIdempotencyStore"
                    {:bundle/key :store})))
  (doseq [k bundle-functions]
    (when-not (fn? (get bundle k))
      (throw (ex-info "reconciler bundle member must be a function"
                      {:bundle/key k}))))
  (select-keys bundle (conj bundle-functions :registry :store)))

;; ── Facts: provider evidence, runtime observation ──────────────────────────

(def gate-evidence-keys
  "The evidence lookups the publication gate requires of its facts map. Named
   so an incomplete provider fails here with the missing key, rather than
   inside the gate as an arity error on nil."
  [:current-source-revision :translated-revision? :approved?
   :source-revision-superseded?])

(defn- assert-evidence-facts!
  [facts]
  (doseq [k gate-evidence-keys]
    (when-not (fn? (get facts k))
      (throw (ex-info "reconciler evidence facts are incomplete"
                      {:missing-key k}))))
  facts)

(defn- runtime-facts
  "The planner's facts: provider evidence plus runtime-owned observation.

   `:materialized-publication` answers from the target observation this run
   already made, whatever the provider supplied for that key — the planner
   must compare desired state against what the same adapter reports in the
   same run, or convergence is decided against a rumor."
  [evidence-facts intent observed]
  (assoc (evidence-facts intent)
         :materialized-publication
         (fn [queried]
           (when (= (:publication/id queried) (:publication/id intent))
             observed))))

;; ── Receipts ───────────────────────────────────────────────────────────────

(defn- failure-receipt
  "The receipt for an effect that never completed. Never a materialization: a
   failed attempt records drift, and a typed artifact conflict travels with it
   so the reader can tell which side was stale. Internal stage markers are
   stripped from the evidence before it goes on the receipt."
  [target plan err]
  (let [evidence (dissoc (ex-data err) ::plan ::target)]
    (receipts-law/assert-receipt!
     (cond-> {:receipt/type :publication/failed
              :failure/reason (or (not-empty (str (ex-message err)))
                                  "unknown failure")
              :failure/drift? true}
       (and target (= :publish (:op plan)))
       (assoc :idempotency/key
              (try (effects/publish-idempotency-key (effects/target-id target)
                                                    (:intent plan)
                                                    (:concrete-revision plan))
                   (catch :default _ nil)))
       (or (receipts-law/artifact-revision-conflict? evidence)
           (receipts-law/artifact-locale-conflict? evidence))
       (assoc :failure/conflict evidence)))))

(defn- ^:async record!
  "Correlate, validate, and emit one receipt; return it.

   Validation happens BEFORE emission, so the sink never sees a receipt the
   law would reject. A sink failure propagates: the receipt is the only
   evidence an effect left, and dropping it quietly is worse than failing the
   trigger loudly."
  [reconciler trigger concrete-revision receipt]
  (let [correlated (->> (trigger-law/correlation trigger concrete-revision)
                        (trigger-law/correlate receipt)
                        (receipts-law/assert-receipt!))]
    (await ((:emit-receipt! reconciler) correlated))
    correlated))

;; ── Plan, then effects ─────────────────────────────────────────────────────

(defn- ^:async publish-artifact!
  "Produce the artifact for a publish plan, above the effect boundary. A source
   answering nil is a failure, not a new blocker: the planner already decided
   the evidence admits publication, and proceeding without bytes would record
   a materialization that did not happen."
  [artifact-source intent plan]
  (or (await (artifact-source intent (:concrete-revision plan)))
      (throw (ex-info "artifact source produced no artifact for a publish plan"
                      {:publication/id (:publication/id intent)
                       :concrete-revision (:concrete-revision plan)}))))

(defn- ^:async plan-then-execute!
  "Ask the pure planner for a decision, then execute it through the registry.

   Planning happens strictly before any adapter effect: the artifact is
   produced only for a `:publish` plan, above the effect boundary, and the
   registry re-validates target and locale admission before delegating. A
   failure after planning is rethrown carrying the plan and target, so the
   failure receipt can name the exact operation and revision that failed."
  [reconciler target ctx index intent observed]
  (let [facts (assert-evidence-facts!
               (runtime-facts (:evidence-facts reconciler) intent observed))
        planned (plan/reconcile-plan index intent facts)]
    (try
      {:plan planned
       :receipt
       (await (registry/execute-plan!
               (:registry reconciler) (:store reconciler) ctx planned
               (when (= :publish (:op planned))
                 (await (publish-artifact! (:artifact-source reconciler) intent planned)))
               (:locale-admissible? reconciler)))}
      (catch :default err
        (throw (ex-info (ex-message err)
                        (assoc (ex-data err) ::plan planned ::target target)
                        err))))))

(defn- find-intent
  "The intent `publication-id` names in the freshly loaded index, or nil."
  [index publication-id]
  (->> (:publications index)
       (filter #(= publication-id (:publication/id %)))
       first))

(defn- ^:async outcome-of!
  "The receipt one trigger produced, and the revision to correlate it with.

   Every failure past intent lookup — a dangling document reference,
   an unresolvable target, a failed observation, a missing artifact, a refused
   effect — becomes a failed receipt here, so a broken publication is
   observable in the same channel as every other outcome rather than as an
   exception nobody correlated.

   Hydration is deliberately INSIDE this try. It was outside, which meant a
   dangling `:publication/document` escaped the runtime with no receipt at all
   — the one failure the comment above it promised could not happen.

   Emission is deliberately OUTSIDE it: see `reconcile-intent!`."
  [reconciler ctx index raw-intent]
  (try
    (let [intent (publication-law/hydrate-publication-intent index raw-intent)
          target (registry/resolve-target! (:registry reconciler)
                                           (:publication/target intent))
          observed (await (effects/observe! target ctx intent))
          {:keys [plan receipt]} (await (plan-then-execute! reconciler target ctx
                                                            index intent observed))]
      {:revision (:concrete-revision plan) :receipt receipt})
    (catch :default err
      (let [evidence (ex-data err)]
        {:revision (some-> evidence ::plan :concrete-revision)
         :receipt (failure-receipt (::target evidence) (::plan evidence) err)}))))

(defn- ^:async reconcile-intent!
  "Resolve, observe, plan, execute, record — for an intent known to exist.

   Exactly one `record!`, and it is outside the failure handling on purpose.
   With emission inside the try, a sink that threw while writing a *success*
   receipt was caught as though the reconciliation had failed, and the handler
   emitted a second receipt describing the sink failure instead of the outcome.
   One trigger, two emission attempts, and the error that propagated was the
   second one — so the actual result of the run was lost.

   Now the outcome is decided first and emitted once. A sink failure propagates
   untouched, which is what the receipt being the only evidence of an effect
   demands: dropping it quietly, or replacing it with a receipt about the
   dropping, are both worse than failing the trigger loudly."
  [reconciler trigger index raw-intent]
  (let [ctx {:reconciliation/trigger (:trigger/id trigger)
             :reconciliation/origin (:trigger/origin trigger)}
        {:keys [revision receipt]} (await (outcome-of! reconciler ctx index raw-intent))]
    (await (record! reconciler trigger revision receipt))))

(defn ^:async reconcile!
  "Run one reconciliation trigger to a receipt.

   Loads desired state fresh, plans purely, executes through the registry, and
   emits one correlated receipt. An UNKNOWN publication is not a receipt
   outcome — the trigger names desired state that does not exist, so it throws
   with `:publication/id` in ex-data for the channel to classify."
  [reconciler trigger]
  (let [trigger (trigger-law/assert-trigger! trigger)
        index (await ((:load-index! reconciler)))
        intent (find-intent index (:publication/id trigger))]
    (when-not intent
      (throw (ex-info "unknown publication"
                      {:publication/id (:publication/id trigger)})))
    (await (reconcile-intent! reconciler trigger index intent))))

;; ── The default emission channel ───────────────────────────────────────────

(def receipt-journal-limit
  "Bound on the in-memory receipt journal. Process-local and deliberately lossy
   past the bound — durable receipt persistence is a store decision this card
   does not take."
  200)

(defn make-receipt-journal
  "A receipt sink keeping the newest `receipt-journal-limit` receipts in
   memory: `{:emit! (fn [receipt] ...) :receipts (fn [] [...])}`.

   The default emission channel when no durable sink is configured. Emission
   is real — every receipt the runtime validated is queryable until the bound
   — it is just not persistent, and the docstring on
   `receipt-journal-limit` says so rather than letting a restart look like a
   clean slate."
  []
  (let [receipts* (atom [])]
    {:emit! (fn [receipt]
              (swap! receipts*
                     (fn [receipts]
                       (vec (take-last receipt-journal-limit (conj receipts receipt)))))
              nil)
     :receipts (fn [] @receipts*)}))
