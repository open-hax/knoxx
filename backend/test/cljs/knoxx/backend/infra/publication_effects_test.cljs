(ns knoxx.backend.infra.publication-effects-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.law.publication-receipts :as law]
            ["node:fs" :as node-fs]
            ["node:path" :as path]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def intent
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :required
   :document/source-locale :en})

(def publish-plan
  {:op :publish
   :intent intent
   :desired {:materialized/revision "probe-revision" :materialized/path "/probe"}
   :previous nil
   :concrete-revision "probe-revision"})

;; ── In-memory adapter and store ────────────────────────────────────────────

(defn- fake-store []
  (let [state (atom {})]
    {:state state
     :store (reify effects/IIdempotencyStore
              ;; Atomic: one swap!, no await between read and claim.
              (reserve! [_ k]
                (let [before @state]
                  (if-let [entry (get before k)]
                    (if (:receipt entry)
                      {:reservation/status :done :receipt (:receipt entry)}
                      {:reservation/status :in-flight})
                    (do (swap! state assoc k {:claimed? true})
                        {:reservation/status :reserved}))))
              (complete! [_ k receipt] (swap! state assoc k {:receipt receipt}))
              (release! [_ k] (swap! state dissoc k)))}))

(defn- materialize!
  "Record the materialization and answer as the adapter would. A path move
   replaces the logical materialization rather than leaving both routes public."
  [routes op swallow-response?]
  (let [desired-path (get-in op [:intent :publication/path])
        materialization {:materialized/revision (:concrete-revision op)
                         :materialized/path desired-path}]
    (swap! routes (fn [current]
                    (-> current
                        (dissoc (get-in op [:previous :materialized/path]))
                        (assoc desired-path materialization))))
    (if swallow-response?
      ;; Recorded, but the caller never learns the outcome.
      (js/Promise.reject (ex-info "ambiguous response" {}))
      (js/Promise.resolve
       (assoc materialization
              :receipt/type :publication/materialized
              :idempotency/key (:idempotency/key op))))))

(defn- fake-target
  "An in-memory publication target. `routes` maps path -> materialization, so
   two public routes for one publication is observable rather than assumed away."
  [{:keys [id fail? swallow-response?] :or {id :fake/target}}]
  (let [routes (atom {})
        calls (atom [])]
    {:routes routes
     :calls calls
     :target
     (reify effects/IPublicationTarget
       (target-id [_] id)
       (publish! [_ _ctx op]
         (swap! calls conj [:publish! (:idempotency/key op)])
         (when fail? (throw (ex-info "adapter exploded" {})))
         (materialize! routes op swallow-response?))
       (remove! [_ _ctx intent observed]
         (swap! calls conj [:remove! (:materialized/path observed)])
         (swap! routes dissoc (:materialized/path observed))
         (js/Promise.resolve {:receipt/type :publication/removed
                              :publication/id (:publication/id intent)
                              :removed/path (:materialized/path observed)}))
       (observe! [_ _ctx observed-intent]
         (swap! calls conj [:observe! (:publication/id observed-intent)])
         (js/Promise.resolve (get @routes (:publication/path observed-intent)))))}))

;; ── 1/2/3 idempotency key ─────────────────────────────────────────────────

(deftest key-is-stable-across-calls
  (is (= (effects/publish-idempotency-key :fake/target intent "probe-revision")
         (effects/publish-idempotency-key :fake/target intent "probe-revision")))
  (testing "and is a readable string, not an opaque hash"
    (is (string? (effects/publish-idempotency-key :fake/target intent "probe-revision")))))

(deftest key-includes-every-effect-dimension
  (let [base (effects/publish-idempotency-key :fake/target intent "probe-revision")]
    (testing "adapter id"
      (is (not= base (effects/publish-idempotency-key :other/target intent "probe-revision"))))
    (testing "concrete revision"
      (is (not= base (effects/publish-idempotency-key :fake/target intent "other-revision"))))
    (doseq [[label field value] [["publication id" :publication/id :knoxx.docs/other]
                                 ["garden" :publication/garden :knoxx.docs/other-garden]
                                 ["locale" :publication/locale :fr]
                                 ["path" :publication/path "/other"]]]
      (testing label
        (is (not= base (effects/publish-idempotency-key
                        :fake/target (assoc intent field value) "probe-revision")))))
    (testing "a non-effect field does NOT perturb the key"
      (is (= base (effects/publish-idempotency-key
                   :fake/target (assoc intent :translation/review :none) "probe-revision")))
      (is (= base (effects/publish-idempotency-key
                   :fake/target (assoc intent :publication/state :withheld) "probe-revision"))))))

(deftest key-requires-concrete-revision
  (is (thrown-with-msg? js/Error #"requires a concrete revision"
                        (effects/publish-idempotency-key :fake/target intent nil))))

;; ── Plan contract at the boundary ─────────────────────────────────────────

(deftest ^:async unrecognized-op-is-a-typed-failure
  (let [{:keys [store]} (fake-store)
        {:keys [target calls]} (fake-target {})
        receipt (await (effects/execute-plan! store target {} {:op :invented} nil))]
    (is (= :publication/failed (:receipt/type receipt))
        "falling through to nil would read as success at an effect boundary")
    (is (true? (:failure/drift? receipt)))
    (is (empty? @calls))))

(deftest ^:async publish-plan-without-concrete-revision-is-rejected
  (let [{:keys [store]} (fake-store)
        {:keys [target calls]} (fake-target {})
        receipt (await (effects/execute-plan!
                        store target {} (dissoc publish-plan :concrete-revision) nil))]
    (is (= :publication/failed (:receipt/type receipt)))
    (is (empty? @calls) "no effect may run for an invalid plan")))

;; ── 4 dispatch ────────────────────────────────────────────────────────────

(deftest ^:async execute-plan!-dispatches-by-op
  (testing ":publish passes the plan's :previous through"
    (let [{:keys [store]} (fake-store)
          {:keys [target routes]} (fake-target {})
          previous {:materialized/revision "old" :materialized/path "/old-route"}
          _ (swap! routes assoc "/old-route" previous)
          receipt (await (effects/execute-plan!
                          store target {} (assoc publish-plan :previous previous) nil))]
      (is (= :publication/materialized (:receipt/type receipt)))
      (is (= ["/probe"] (keys @routes)) "the stale route is gone")))
  (testing ":remove uses the plan's :observed"
    (let [{:keys [store]} (fake-store)
          {:keys [target routes calls]} (fake-target {})
          observed {:materialized/revision "probe-revision" :materialized/path "/probe"}
          _ (swap! routes assoc "/probe" observed)
          receipt (await (effects/execute-plan!
                          store target {} {:op :remove :intent intent :observed observed} nil))]
      (is (= :publication/removed (:receipt/type receipt)))
      (is (= [[:remove! "/probe"]] @calls))
      (is (empty? @routes))))
  (testing ":noop and :blocked produce receipts and perform NO effect"
    (doseq [[plan expected] [[{:op :noop :reason :garden-not-active} :publication/noop]
                             [{:op :blocked :blockers [:translation-missing]}
                              :publication/blocked]]]
      (let [{:keys [store]} (fake-store)
            {:keys [target calls]} (fake-target {})
            receipt (await (effects/execute-plan! store target {} plan nil))]
        (is (= expected (:receipt/type receipt)))
        (is (empty? @calls))))))

;; ── 5/6 replay ────────────────────────────────────────────────────────────

(deftest ^:async replay-of-identical-publish-converges
  (let [{:keys [store]} (fake-store)
        {:keys [target routes calls]} (fake-target {})
        first-receipt (await (effects/execute-plan! store target {} publish-plan nil))
        second-receipt (await (effects/execute-plan! store target {} publish-plan nil))]
    (is (= first-receipt second-receipt))
    (is (= 1 (count @routes)) "exactly one materialization")
    (is (= 1 (count (filter #(= :publish! (first %)) @calls)))
        "the adapter was asked to publish once, not twice")))

(deftest ^:async ambiguous-response-replay-is-safe
  (testing "the first attempt records the artifact but the caller never learns —
            replay must converge on the existing route, not duplicate it"
    (let [{:keys [store]} (fake-store)
          {:keys [target routes calls]} (fake-target {:swallow-response? true})
          failed (await (effects/execute-plan! store target {} publish-plan nil))]
      (is (= :publication/failed (:receipt/type failed)))
      (is (= 1 (count @routes)) "the artifact WAS created despite the failure")
      (testing "the claim is retained, so the replay OBSERVES rather than
                republishing — convergence must not depend on the adapter
                deduplicating on our behalf"
        (reset! calls [])
        (let [replay (await (effects/execute-plan! store target {} publish-plan nil))]
          (is (= :publication/materialized (:receipt/type replay)))
          (is (= 1 (count @routes)) "no second artifact")
          (is (not-any? #(= :publish! (first %)) @calls)
              "a target that does not deduplicate would have gained a second route")
          (is (some #(= :observe! (first %)) @calls)))))))

(deftest ^:async in-flight-claim-reconciles-by-observation
  (testing "a claimed-but-never-completed key means the prior outcome is unknown;
            the boundary observes rather than blindly republishing"
    (let [{:keys [store state]} (fake-store)
          {:keys [target routes calls]} (fake-target {})
          idempotency-key (effects/publish-idempotency-key
                           :fake/target intent "probe-revision")]
      ;; Simulate a crash after claiming and after the artifact landed.
      (swap! state assoc idempotency-key {:claimed? true})
      (swap! routes assoc "/probe" {:materialized/revision "probe-revision"
                                    :materialized/path "/probe"})
      (let [receipt (await (effects/execute-plan! store target {} publish-plan nil))]
        (is (= :publication/materialized (:receipt/type receipt)))
        (is (= 1 (count @routes)))
        (is (not-any? #(= :publish! (first %)) @calls)
            "it must NOT republish an artifact that is already there")
        (is (some #(= :observe! (first %)) @calls))))))

;; ── 7 path move ───────────────────────────────────────────────────────────

(deftest ^:async path-move-replaces-previous-route
  (let [{:keys [store]} (fake-store)
        {:keys [target routes]} (fake-target {})
        previous {:materialized/revision "probe-revision" :materialized/path "/old-route"}
        _ (swap! routes assoc "/old-route" previous)
        receipt (await (effects/execute-plan!
                        store target {} (assoc publish-plan :previous previous) nil))]
    (is (= :publication/materialized (:receipt/type receipt)))
    (is (= ["/probe"] (keys @routes)))
    (is (nil? (get @routes "/old-route")) "the old route is unavailable")
    (testing "a path move is a distinct operation, so it has a distinct key"
      (is (not= (effects/publish-idempotency-key :fake/target intent "probe-revision")
                (effects/publish-idempotency-key
                 :fake/target (assoc intent :publication/path "/old-route")
                 "probe-revision"))))))

;; ── 8 removal ─────────────────────────────────────────────────────────────

(deftest ^:async remove-works-for-prior-materialization-and-is-idempotent
  (let [{:keys [store]} (fake-store)
        {:keys [target routes]} (fake-target {})
        observed {:materialized/revision "probe-revision" :materialized/path "/probe"}
        _ (swap! routes assoc "/probe" observed)
        remove-plan {:op :remove :intent intent :observed observed}
        first-receipt (await (effects/execute-plan! store target {} remove-plan nil))
        second-receipt (await (effects/execute-plan! store target {} remove-plan nil))]
    (is (= :publication/removed (:receipt/type first-receipt)))
    (is (= first-receipt second-receipt))
    (is (empty? @routes))))

;; ── 9 the adapter does not reinterpret desired state ──────────────────────

(deftest ^:async adapter-does-not-reinterpret-desired-state
  (testing "a :remove plan for a :published intent still removes — semantics
            come from the plan, never from the adapter"
    (let [{:keys [store]} (fake-store)
          {:keys [target routes]} (fake-target {})
          observed {:materialized/revision "probe-revision" :materialized/path "/probe"}
          _ (swap! routes assoc "/probe" observed)
          receipt (await (effects/execute-plan!
                          store target {}
                          {:op :remove :intent intent :observed observed} nil))]
      (is (= :published (:publication/state intent)))
      (is (= :publication/removed (:receipt/type receipt)))
      (is (empty? @routes)))))

;; ── 10 failure is drift, not mutation ────────────────────────────────────

(deftest ^:async adapter-failure-produces-drift-not-mutation
  (let [{:keys [store]} (fake-store)
        {:keys [target]} (fake-target {:fail? true})
        before intent
        receipt (await (effects/execute-plan! store target {} publish-plan nil))]
    (is (= :publication/failed (:receipt/type receipt)))
    (is (true? (:failure/drift? receipt)))
    (is (str/includes? (:failure/reason receipt) "adapter exploded"))
    (testing "desired state is untouched — identical map, not merely equal"
      (is (identical? before (:intent publish-plan)))
      (is (= :published (:publication/state (:intent publish-plan)))))
    (testing "and a retry still proceeds: the retained claim is reconciled by
              observation, which finds nothing and then republishes"
      (let [{:keys [target routes]} (fake-target {})
            retry (await (effects/execute-plan! store target {} publish-plan nil))]
        (is (= :publication/materialized (:receipt/type retry)))
        (is (= 1 (count @routes)))))))

;; ── 11 no adapter identifier leaks upward ────────────────────────────────

(deftest no-legacy-identifier-in-plan-or-contract
  (let [legacy-marker (str "open" "planner")
        read-source (fn [relative-path]
                      (str/lower-case
                       (.readFileSync node-fs
                                      (.join path (.cwd js/process) relative-path)
                                      "utf8")))]
    (doseq [relative-path ["src/cljs/knoxx/backend/domain/publication_plan.cljs"
                           "src/cljs/knoxx/backend/law/publication.cljs"
                           "src/cljs/knoxx/backend/law/publication_receipts.cljs"
                           "src/cljs/knoxx/backend/infra/publication_effects.cljs"]]
      (testing relative-path
        (is (not (str/includes? (read-source relative-path) legacy-marker)))))))

;; ── the boundary refuses what it cannot key or verify (Codex on #236) ──────

(deftest the-idempotency-key-refuses-a-revision-selector
  (testing "the nil check alone let :source/current through, so a key meaning
            \"whatever is current\" got a stable-looking name for a moving target —
            and replaying it would report done while publishing other content"
    (doseq [bad [nil :source/current "" "   "]]
      (is (thrown? js/Error (effects/publish-idempotency-key :fake/target intent bad))
          (str (pr-str bad) " must not produce a key"))))
  (testing "the plan contract refuses it too, so it cannot reach the boundary"
    (is (thrown? js/Error
                 (law/assert-plan! (assoc publish-plan :concrete-revision :source/current)))))
  (testing "while a concrete revision keys normally"
    (is (string? (effects/publish-idempotency-key :fake/target intent "probe-revision")))))

(deftest a-remove-plan-must-carry-what-remove-needs
  (testing "remove! takes both intent and observed; leaving them optional let a
            plan validate and then call the adapter with two nils"
    (doseq [plan [{:op :remove}
                  {:op :remove :intent intent}
                  {:op :remove :observed {:materialized/path "/probe"}}]]
      (is (thrown? js/Error (law/assert-plan! plan))
          (str (pr-str plan) " must not validate"))))
  (testing "a complete remove plan still validates"
    (is (map? (law/assert-plan! {:op :remove
                                 :intent intent
                                 :observed {:materialized/path "/probe"}})))))

(deftest ^:async a-receipt-that-does-not-describe-the-request-is-refused
  (testing "a structurally valid receipt naming the wrong artifact was recorded
            as :done, after which every replay reported convergence for something
            that may never have been materialized"
    (doseq [[label wrong] [["wrong path" {:materialized/path "/somewhere-else"}]
                           ["wrong revision" {:materialized/revision "other-revision"}]
                           ["wrong key" {:idempotency/key "not-the-requested-key"}]]]
      (let [{:keys [store state]} (fake-store)
            target (reify effects/IPublicationTarget
                     (target-id [_] :fake/target)
                     (publish! [_ _ctx op]
                       (js/Promise.resolve
                        (merge {:receipt/type :publication/materialized
                                :materialized/revision (:concrete-revision op)
                                :materialized/path (get-in op [:intent :publication/path])
                                :idempotency/key (:idempotency/key op)}
                               wrong)))
                     (remove! [_ _ctx _intent _observed] (js/Promise.resolve nil))
                     (observe! [_ _ctx _intent] (js/Promise.resolve nil)))
            receipt (await (effects/execute-plan! store target {} publish-plan nil))]
        (testing label
          (is (= :publication/failed (:receipt/type receipt)))
          (is (not-any? :receipt (vals @state))
              "nothing may be recorded as done for a materialization we cannot confirm"))))))

