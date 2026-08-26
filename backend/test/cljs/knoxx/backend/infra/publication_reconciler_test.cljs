(ns knoxx.backend.infra.publication-reconciler-test
  "The reconciler runtime, end to end over the real seam.

  Every test here drives `reconcile!` with a real registry, a real idempotency
  store, the real pure planner and a real adapter — only desired state, the
  evidence provider and the artifact source are fixtures. That is the point:
  the planner, the gate, the effects and the adapters were merged and proven in
  isolation, and what was never proven is that a trigger walks all of them and
  leaves evidence behind.

  What is asserted, per the card's definition of done: that planning strictly
  precedes any adapter effect; that a materialization, a noop, a blocker, a
  removal and an adapter failure each produce one correlated receipt of the
  expected type; that repeating a trigger publishes once; and that no receipt
  can edit desired state or talk its way past a review blocker."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.publication-plan :as plan]
            [knoxx.backend.infra.publication-reconciler :as reconciler]
            [knoxx.backend.infra.publication-target-memory :as memory]
            [knoxx.backend.infra.publication-target-registry :as registry]))

;; ── Desired state ──────────────────────────────────────────────────────────

(def ^:private revision "sha256-aaa111bbb222")

(def ^:private target-id :knoxx.publication/memory)

(defn- intent
  [& {:keys [state review locale] :or {state :published
                                       review :required
                                       locale :es}}]
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/target target-id
   :publication/locale locale
   :publication/revision :source/current
   :publication/state state
   :publication/path "/probe-es"
   :translation/review review})

(defn- index
  [& {:keys [garden-status] :as opts}]
  {:documents {:knoxx.docs/probe {:document/id :knoxx.docs/probe
                                  :document/title "Probe"
                                  :document/source-locale :en
                                  :document/source {:path "docs/probe.md"}}}
   :gardens {:knoxx.docs/promethean {:garden/id :knoxx.docs/promethean
                                     :garden/title "Promethean"
                                     :garden/status (or garden-status :active)
                                     :garden/locales [:en :es]}}
   :publications [(apply intent (mapcat identity (dissoc opts :garden-status)))]})

(def ^:private declarations
  [{:publication-target/id target-id
    :publication-target/kind :publication-target/memory
    :publication-target/config {}
    :publication-target/enabled? true}])

;; ── The bundle under test ──────────────────────────────────────────────────

(defn- evidence-facts
  "The four gate lookups a provider owns. `translated?` and `approved?` are the
   two the review blocker turns on; both default to satisfied so a test that
   cares about something else does not have to restate them.

   `unresolvable-revision?` is a flag rather than `:source-revision nil`
   because destructuring `:or` cannot tell an absent key from a nil value, and
   a fixture that silently substituted a default for the very thing under test
   would prove the opposite of what it claims."
  [& {:keys [translated? approved? superseded? unresolvable-revision?]
      :or {translated? true approved? true superseded? false}}]
  (fn [_intent]
    {:current-source-revision (constantly (when-not unresolvable-revision? revision))
     :translated-revision? (fn [_document _garden _locale _revision] translated?)
     :approved? (fn [_document _garden _locale _revision] approved?)
     :source-revision-superseded? (fn [_intent _revision] superseded?)}))

(defn- artifact-for
  [_intent concrete-revision]
  (js/Promise.resolve {:artifact/content "<p>traducido</p>"
                       :artifact/media-type "text/html"
                       :artifact/encoding "utf-8"
                       :artifact/locale :es
                       :artifact/revision concrete-revision}))

(defn- fixture
  "A reconciler over a real registry, store, planner and memory adapter.

   Returns the bundle plus the adapter and receipt handles a test reads. The
   adapter instance is created once and handed back by the factory every time,
   so `public-routes` and `materialization-count` observe the same target the
   runtime published through."
  [& {:keys [facts fail? load-index! locale-admissible? artifact-source
             declarations* emit-receipt!]}]
  (let [bundle (memory/memory-target {:id target-id :fail? fail?})
        receipts (atom [])
        resources (atom (index))]
    {:target bundle
     :receipts receipts
     :resources resources
     :reconciler
     (reconciler/make-reconciler
      {:registry (registry/make-registry
                  (or declarations* declarations)
                  {:publication-target/memory (constantly (:target bundle))})
       :store (:store (memory/memory-store))
       :load-index! (or load-index! (fn [] (js/Promise.resolve @resources)))
       :evidence-facts (or facts (evidence-facts))
       :artifact-source (or artifact-source artifact-for)
       :locale-admissible? (or locale-admissible? (constantly true))
       :emit-receipt! (fn [receipt]
                        (swap! receipts conj receipt)
                        (when emit-receipt! (emit-receipt! receipt))
                        nil)})}))

(defn- trigger
  [& {:keys [id] :or {id :test/trigger}}]
  {:trigger/id id
   :trigger/origin :manual
   :publication/id :knoxx.docs/probe-es})

;; ── Ordering ───────────────────────────────────────────────────────────────

(deftest ^:async planning-happens-before-any-adapter-effect
  ;; The card asks for this specifically, and it is not a style point: an
  ;; effect that ran before the plan decided would be an effect no evidence
  ;; admitted. Observed rather than assumed — the planner is wrapped and asked
  ;; what the adapter had done at the moment it was called.
  (let [{:keys [reconciler target]} (fixture)
        published-at-plan-time (atom nil)
        real plan/reconcile-plan]
    (with-redefs [plan/reconcile-plan
                  (fn [index* intent* facts*]
                    (reset! published-at-plan-time
                            (memory/materialization-count target))
                    (real index* intent* facts*))]
      (let [receipt (await (reconciler/reconcile! reconciler (trigger)))]
        (testing "nothing had been published when the planner ran"
          (is (= 0 @published-at-plan-time)))

        (testing "and the plan it produced is what got materialized"
          (is (= :publication/materialized (:receipt/type receipt)))
          (is (= 1 (memory/materialization-count target))))))))

(deftest ^:async a-blocked-plan-never-reaches-the-artifact-source
  ;; The other half of the ordering claim. Producing an artifact is work, and
  ;; for a plan that cannot publish it is work whose only possible effect is to
  ;; make a refused publication look attempted.
  (let [asked (atom 0)
        {:keys [target reconciler]}
        (fixture :facts (evidence-facts :approved? false)
                 :artifact-source (fn [i r] (swap! asked inc) (artifact-for i r)))
        receipt (await (reconciler/reconcile! reconciler (trigger)))]
    (testing "the trigger is refused with the review blocker"
      (is (= :publication/blocked (:receipt/type receipt)))
      (is (= [:translation-review-required] (vec (:blockers receipt)))))

    (testing "no artifact was produced and nothing was published"
      (is (= 0 @asked))
      (is (= 0 (memory/materialization-count target)))
      (is (empty? (memory/public-routes target))))))

;; ── One receipt per outcome ────────────────────────────────────────────────

(deftest ^:async a-materialization-produces-a-correlated-receipt
  (let [{:keys [reconciler receipts target]} (fixture)
        receipt (await (reconciler/reconcile! reconciler (trigger)))]
    (testing "the receipt names the materialization"
      (is (= :publication/materialized (:receipt/type receipt)))
      (is (= :knoxx.docs/probe-es (:publication/id receipt)))
      (is (= revision (:materialized/revision receipt)))
      (is (= "/probe-es" (:materialized/path receipt))))

    (testing "it carries the correlation the trigger supplied"
      (is (= :test/trigger (:correlation/trigger receipt)))
      (is (= :manual (:correlation/origin receipt)))
      (is (= :knoxx.docs/probe-es (:correlation/publication receipt)))
      (is (= revision (:correlation/revision receipt))))

    (testing "exactly one receipt was emitted, and the route serves the bytes"
      (is (= 1 (count @receipts)))
      (is (= "<p>traducido</p>"
             (:artifact/content (memory/served-artifact target "/probe-es")))))))

(deftest ^:async a-converged-publication-reconciles-to-a-noop
  (let [{:keys [reconciler receipts target]} (fixture)
        _ (await (reconciler/reconcile! reconciler (trigger)))
        second-run (await (reconciler/reconcile! reconciler (trigger)))]
    (testing "the second trigger finds desired and observed already equal"
      (is (= :publication/noop (:receipt/type second-run))))

    (testing "it is still a receipt, and still correlated"
      (is (= 2 (count @receipts)))
      (is (= :knoxx.docs/probe-es (:correlation/publication second-run)))
      (is (= revision (:correlation/revision second-run))))

    (testing "and a noop published nothing new and took nothing down"
      ;; The failure this rules out is a noop that reads as convergence while
      ;; having quietly removed the route it converged on.
      (is (= 1 (memory/materialization-count target)))
      (is (= 1 (count (memory/public-routes target))))
      (is (some? (memory/served-artifact target "/probe-es"))))))

(deftest ^:async a-withheld-publication-reconciles-to-a-removal
  ;; Removal is decided before translation and review evidence — a publication
  ;; being taken down must not be held up by evidence about publishing it.
  (let [{:keys [reconciler receipts resources target]} (fixture)
        _ (await (reconciler/reconcile! reconciler (trigger)))
        _ (reset! resources (index :state :withheld))
        removal (await (reconciler/reconcile! reconciler (trigger)))]
    (testing "the route comes down"
      (is (= :publication/removed (:receipt/type removal)))
      (is (= "/probe-es" (:removed/path removal)))
      (is (empty? (memory/public-routes target))))

    (testing "the removal is correlated like every other outcome"
      ;; Two so far: the materialization, then this removal.
      (is (= 2 (count @receipts)))
      (is (= :knoxx.docs/probe-es (:correlation/publication removal))))

    (testing "a withheld publication with nothing materialized is a noop"
      (is (= :publication/noop
             (:receipt/type (await (reconciler/reconcile! reconciler (trigger)))))))))

(deftest ^:async an-adapter-failure-is-drift-not-a-materialization
  (let [{:keys [reconciler receipts target]} (fixture :fail? true)
        receipt (await (reconciler/reconcile! reconciler (trigger)))]
    (testing "the failure is a receipt rather than an exception"
      (is (= :publication/failed (:receipt/type receipt)))
      (is (true? (:failure/drift? receipt)))
      (is (string? (:failure/reason receipt))))

    (testing "it is correlated, so a broken target is traceable to its trigger"
      (is (= :test/trigger (:correlation/trigger receipt)))
      (is (= revision (:correlation/revision receipt))))

    (testing "and nothing became public"
      (is (= 0 (memory/materialization-count target)))
      (is (empty? (memory/public-routes target)))
      (is (= 1 (count @receipts))))))

(deftest ^:async an-unresolvable-target-is-a-failed-receipt-not-a-crash
  ;; A misdeclared target is an operator defect, and it has to be observable in
  ;; the same channel as every other outcome rather than as an exception nobody
  ;; correlated.
  (let [{:keys [reconciler receipts]}
        (fixture :declarations* [(assoc (first declarations)
                                        :publication-target/enabled? false)])
        receipt (await (reconciler/reconcile! reconciler (trigger)))]
    (is (= :publication/failed (:receipt/type receipt)))
    (is (true? (:failure/drift? receipt)))
    (is (= :knoxx.docs/probe-es (:correlation/publication receipt)))
    (is (= 1 (count @receipts)))))

;; ── Idempotency ────────────────────────────────────────────────────────────

(deftest ^:async repeating-a-trigger-publishes-once
  ;; Not the same claim as the noop test. That one proves the *planner* sees
  ;; convergence; this one proves the idempotency store refuses a second
  ;; publish of the same operation even when the planner is asked for one,
  ;; which is what a retry after a lost response actually looks like.
  (let [{:keys [reconciler target]} (fixture)
        first-run (await (reconciler/reconcile! reconciler (trigger)))
        ;; A different trigger id, so nothing is deduplicated by correlation —
        ;; the operation identity has to be what stops the second publish.
        second-run (await (reconciler/reconcile! reconciler (trigger :id :test/retry)))]
    (testing "both triggers answer with a receipt"
      (is (= :publication/materialized (:receipt/type first-run)))
      (is (some? (:receipt/type second-run))))

    (testing "the adapter published exactly once"
      (is (= 1 (memory/materialization-count target)))
      (is (= 1 (count (memory/public-routes target)))))

    (testing "and the second answer is correlated to its own trigger"
      (is (= :test/retry (:correlation/trigger second-run))))))

;; ── Receipts are evidence, never authority ─────────────────────────────────

(deftest ^:async a-failed-effect-leaves-desired-state-byte-identical
  ;; Drift is reported, never repaired by moving the goalposts. If a failure
  ;; could edit resources, the next run would converge on the damage.
  (let [{:keys [reconciler resources]} (fixture :fail? true)
        before @resources
        _ (await (reconciler/reconcile! reconciler (trigger)))]
    (is (= before @resources))))

(deftest ^:async no-number-of-triggers-talks-past-a-review-blocker
  ;; The blocker is evidential, so it cannot be worn down by repetition — and a
  ;; receipt recording that it blocked must not become the evidence that
  ;; unblocks it.
  (let [{:keys [reconciler receipts target]}
        (fixture :facts (evidence-facts :approved? false))]
    (doseq [_ (range 3)]
      (await (reconciler/reconcile! reconciler (trigger))))
    (testing "every attempt is blocked for the same reason"
      (is (= 3 (count @receipts)))
      (is (every? #(= :publication/blocked (:receipt/type %)) @receipts))
      (is (every? #(= [:translation-review-required] (vec (:blockers %))) @receipts)))

    (testing "and nothing was ever published"
      (is (= 0 (memory/materialization-count target)))
      (is (empty? (memory/public-routes target))))))

(deftest ^:async a-missing-translation-blocks-publication
  (let [{:keys [reconciler target]}
        (fixture :facts (evidence-facts :translated? false))
        receipt (await (reconciler/reconcile! reconciler (trigger)))]
    (is (= :publication/blocked (:receipt/type receipt)))
    (is (= [:translation-missing] (vec (:blockers receipt))))
    (is (= 0 (memory/materialization-count target)))))

(deftest ^:async an-unresolvable-source-revision-blocks-before-any-lookup
  (let [{:keys [reconciler target]}
        (fixture :facts (evidence-facts :unresolvable-revision? true))
        receipt (await (reconciler/reconcile! reconciler (trigger)))]
    (testing "no revision means no evidence can be keyed, so it blocks"
      (is (= :publication/blocked (:receipt/type receipt)))
      (is (= [:publication-revision-unresolved] (vec (:blockers receipt)))))
    (is (= 0 (memory/materialization-count target)))))

;; ── Exactly one receipt, whatever happens ──────────────────────────────────

(deftest ^:async a-failing-sink-is-not-answered-with-a-second-receipt
  ;; The regression: `record!` used to sit inside the failure handling, so a
  ;; sink that threw while writing a SUCCESS receipt was caught as though the
  ;; reconciliation had failed — and the handler emitted a second receipt
  ;; describing the sink failure instead of the outcome. One trigger, two
  ;; emission attempts, and the error that propagated was the second one, so
  ;; the actual result of the run was lost.
  (let [{:keys [reconciler receipts target]}
        (fixture :emit-receipt! (fn [_] (throw (ex-info "sink is down" {}))))]
    (testing "the sink failure propagates rather than being swallowed"
      (is (thrown? js/Error (await (reconciler/reconcile! reconciler (trigger))))))

    (testing "the sink was offered exactly one receipt"
      ;; Two would mean the failure handler re-entered emission.
      (is (= 1 (count @receipts)))
      (is (= :publication/materialized (:receipt/type (first @receipts)))))

    (testing "and the effect itself still happened"
      ;; A sink that cannot record is not a reason to claim nothing was
      ;; published — that is exactly the divergence the receipt exists to make
      ;; visible, and pretending otherwise would hide it.
      (is (= 1 (memory/materialization-count target))))))

(deftest ^:async a-dangling-document-reference-is-a-receipt-not-an-escape
  ;; `hydrate-publication-intent` throws on a dangling `:publication/document`
  ;; rather than defaulting a source locale. It used to run outside the failure
  ;; handling, so that throw left the runtime with no receipt at all — the one
  ;; failure the handler's own comment promised could not happen.
  (let [{:keys [reconciler receipts resources target]} (fixture)
        _ (swap! resources update :documents dissoc :knoxx.docs/probe)
        receipt (await (reconciler/reconcile! reconciler (trigger)))]
    (testing "the broken reference is reported as drift"
      (is (= :publication/failed (:receipt/type receipt)))
      (is (true? (:failure/drift? receipt))))

    (testing "it is correlated, and it is the only receipt"
      (is (= :knoxx.docs/probe-es (:correlation/publication receipt)))
      (is (= :test/trigger (:correlation/trigger receipt)))
      ;; No plan was ever produced, so there is no revision to correlate with.
      (is (nil? (:correlation/revision receipt)))
      (is (= 1 (count @receipts))))

    (testing "and nothing was published on the strength of a missing document"
      (is (= 0 (memory/materialization-count target))))))

;; ── The trigger itself ─────────────────────────────────────────────────────

(deftest ^:async an-unknown-publication-is-not-a-receipt-outcome
  ;; The trigger names desired state that does not exist. There is nothing to
  ;; record a receipt about, and inventing one would put a publication id into
  ;; the evidence stream that no resource declares.
  (let [{:keys [reconciler receipts]} (fixture)]
    (is (thrown? js/Error
                 (await (reconciler/reconcile!
                         reconciler
                         (assoc (trigger) :publication/id :knoxx.docs/nope)))))
    (is (empty? @receipts))))

(deftest ^:async a-malformed-trigger-is-refused-before-desired-state-is-loaded
  (let [loads (atom 0)
        {:keys [reconciler]} (fixture :load-index! (fn []
                                                     (swap! loads inc)
                                                     (js/Promise.resolve (index))))]
    (doseq [[label bad] [["no origin" (dissoc (trigger) :trigger/origin)]
                         ["no publication" (dissoc (trigger) :publication/id)]
                         ["unqualified id" (assoc (trigger) :publication/id :probe)]
                         ["extra field" (assoc (trigger) :sneaky "x")]]]
      (testing label
        (is (thrown? js/Error (await (reconciler/reconcile! reconciler bad))))))
    (testing "not one of them reached desired state"
      (is (= 0 @loads)))))

(deftest an-incomplete-bundle-is-refused-at-construction
  ;; Refused when the reconciler is built, not halfway through a run: a missing
  ;; artifact source surfaces as fake drift and a missing sink as evidence that
  ;; vanished, and both are far from the line that caused them.
  (let [complete {:registry (registry/make-registry
                             declarations
                             {:publication-target/memory
                              (constantly (:target (memory/memory-target {:id target-id})))})
                  :store (:store (memory/memory-store))
                  :load-index! (fn [])
                  :evidence-facts (fn [_])
                  :artifact-source (fn [_ _])
                  :locale-admissible? (fn [_ _ _])
                  :emit-receipt! (fn [_])}]
    (testing "the complete bundle builds"
      (is (some? (reconciler/make-reconciler complete))))

    (doseq [k [:load-index! :evidence-facts :artifact-source
               :locale-admissible? :emit-receipt!]]
      (testing (str "missing " k)
        (is (thrown? js/Error (reconciler/make-reconciler (dissoc complete k))))))

    (testing "missing registry"
      (is (thrown? js/Error (reconciler/make-reconciler (dissoc complete :registry)))))

    (testing "missing idempotency store"
      (is (thrown? js/Error (reconciler/make-reconciler (dissoc complete :store)))))))

(deftest ^:async an-incomplete-evidence-provider-fails-with-the-missing-key
  ;; Rather than as an arity error on nil somewhere inside the gate.
  (let [{:keys [reconciler receipts]}
        (fixture :facts (fn [_] {:current-source-revision (constantly revision)}))
        receipt (await (reconciler/reconcile! reconciler (trigger)))]
    (testing "it surfaces as a failed receipt naming the defect"
      (is (= :publication/failed (:receipt/type receipt)))
      (is (re-find #"evidence facts are incomplete" (:failure/reason receipt))))
    (is (= 1 (count @receipts)))))
