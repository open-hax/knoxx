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

(def artifact
  "What the renderer hands the boundary. Produced ABOVE it — `execute-plan!`
   takes it as an argument, which is the whole reason it can be a fixture here."
  {:artifact/content "<!doctype html><p>Sonda</p>"
   :artifact/media-type "text/html"
   :artifact/encoding "utf-8"
   :artifact/locale :es
   :artifact/revision "probe-revision"})

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
                        store target {} (dissoc publish-plan :concrete-revision) artifact))]
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
                          store target {} (assoc publish-plan :previous previous) artifact))]
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
        first-receipt (await (effects/execute-plan! store target {} publish-plan artifact))
        second-receipt (await (effects/execute-plan! store target {} publish-plan artifact))]
    (is (= first-receipt second-receipt))
    (is (= 1 (count @routes)) "exactly one materialization")
    (is (= 1 (count (filter #(= :publish! (first %)) @calls)))
        "the adapter was asked to publish once, not twice")))

(deftest ^:async ambiguous-response-replay-is-safe
  (testing "the first attempt records the artifact but the caller never learns —
            replay must converge on the existing route, not duplicate it"
    (let [{:keys [store]} (fake-store)
          {:keys [target routes calls]} (fake-target {:swallow-response? true})
          failed (await (effects/execute-plan! store target {} publish-plan artifact))]
      (is (= :publication/failed (:receipt/type failed)))
      (is (= 1 (count @routes)) "the artifact WAS created despite the failure")
      (testing "the claim is retained, so the replay OBSERVES rather than
                republishing — convergence must not depend on the adapter
                deduplicating on our behalf"
        (reset! calls [])
        (let [replay (await (effects/execute-plan! store target {} publish-plan artifact))]
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
      (let [receipt (await (effects/execute-plan! store target {} publish-plan artifact))]
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
                        store target {} (assoc publish-plan :previous previous) artifact))]
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
        receipt (await (effects/execute-plan! store target {} publish-plan artifact))]
    (is (= :publication/failed (:receipt/type receipt)))
    (is (true? (:failure/drift? receipt)))
    (is (str/includes? (:failure/reason receipt) "adapter exploded"))
    (testing "desired state is untouched — identical map, not merely equal"
      (is (identical? before (:intent publish-plan)))
      (is (= :published (:publication/state (:intent publish-plan)))))
    (testing "and a retry still proceeds: the retained claim is reconciled by
              observation, which finds nothing and then republishes"
      (let [{:keys [target routes]} (fake-target {})
            retry (await (effects/execute-plan! store target {} publish-plan artifact))]
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
            receipt (await (effects/execute-plan! store target {} publish-plan artifact))]
        (testing label
          (is (= :publication/failed (:receipt/type receipt)))
          (is (not-any? :receipt (vals @state))
              "nothing may be recorded as done for a materialization we cannot confirm"))))))


;; ── the artifact is checked on the way IN, as the receipt is on the way OUT ──

(deftest ^:async a-malformed-artifact-fails-before-any-effect
  (testing "`:artifact` used to be an unnamed payload the boundary forwarded and
            the target stored, so an artifact with no media type, no declared
            encoding, or no content at all became a SERVED route while every
            assertion about the materialization stayed green"
    (doseq [[label bad] [["absent entirely" nil]
                         ["not a map" "<!doctype html>"]
                         ["no content" (dissoc artifact :artifact/content)]
                         ["blank content" (assoc artifact :artifact/content "   ")]
                         ["empty bytes" (assoc artifact :artifact/content
                                               (js/Uint8Array. 0))]
                         ["no media type" (dissoc artifact :artifact/media-type)]
                         ["media type with a charset parameter"
                          (assoc artifact :artifact/media-type "text/html; charset=utf-8")]
                         ["no declared encoding" (dissoc artifact :artifact/encoding)]
                         ["no locale" (dissoc artifact :artifact/locale)]
                         ["a namespaced locale" (assoc artifact :artifact/locale :locale/es)]
                         ["no revision" (dissoc artifact :artifact/revision)]]]
      (let [{:keys [store state]} (fake-store)
            {:keys [target calls routes]} (fake-target {})
            receipt (await (effects/execute-plan! store target {} publish-plan bad))]
        (testing label
          (is (= :publication/failed (:receipt/type receipt)))
          (is (true? (:failure/drift? receipt)))
          (is (empty? @calls) "no effect may run for a malformed artifact")
          (is (empty? @routes) "and nothing may be served")
          (is (empty? @state)
              "not even a reservation: a claim for a publication that cannot
               happen is a claim nobody will ever complete"))))))

(deftest ^:async the-artifact-refuses-a-revision-selector-anywhere
  (testing "for the reason publish-idempotency-key refuses one — a selector gives
            a stable-looking identity to a moving target. `:artifact/revision` is
            typed, so the interesting cases are the ones a shape check alone
            misses: a selector riding along on some other key."
    (doseq [[label bad] [["as the revision itself"
                          (assoc artifact :artifact/revision :source/current)]
                         ["on an extra top-level key"
                          (assoc artifact :render/from :source/current)]
                         ["nested inside a map"
                          (assoc artifact :render/provenance {:revision :source/current})]
                         ["nested inside a vector"
                          (assoc artifact :render/inputs [:source/current])]
                         ["as a map KEY"
                          (assoc artifact :render/by {:source/current "probe-revision"})]
                         ["a sibling selector nobody has invented yet"
                          (assoc artifact :render/from :source/head)]]]
      (let [{:keys [store state]} (fake-store)
            {:keys [target calls]} (fake-target {})
            receipt (await (effects/execute-plan! store target {} publish-plan bad))]
        (testing label
          (is (= :publication/failed (:receipt/type receipt)))
          (is (empty? @calls))
          (is (empty? @state)))))))

(deftest ^:async an-artifact-revision-conflict-is-typed-and-carries-both-revisions
  (testing "the renderer and the planner disagreeing about what is being
            published is a CONFLICT, not a warning: the bytes came from one
            source state and the idempotency key names another, so publishing
            either way records a materialization that did not happen"
    (let [{:keys [store state]} (fake-store)
          {:keys [target calls routes]} (fake-target {})
          stale (assoc artifact :artifact/revision "an-older-revision")
          receipt (await (effects/execute-plan! store target {} publish-plan stale))]
      (is (= :publication/failed (:receipt/type receipt)))
      (testing "and BOTH revisions survive onto the receipt — a reader has to be
                able to tell which side was stale, which a message string cannot"
        (is (= {:conflict/type :publication/artifact-revision-conflict
                :conflict/artifact-revision "an-older-revision"
                :conflict/concrete-revision "probe-revision"}
               (:failure/conflict receipt)))
        (is (true? (law/artifact-revision-conflict? (:failure/conflict receipt)))))
      (is (empty? @calls) "nothing was published")
      (is (empty? @routes))
      (is (empty? @state) "and nothing was claimed"))))

(deftest ^:async an-agreeing-artifact-publishes-as-bytes-or-as-a-string
  (testing "bytes and a string are both content; the difference is only whether
            the renderer already applied the declared encoding"
    (doseq [[label content] [["a string" "<!doctype html><p>Sonda</p>"]
                             ["bytes" (.encode (js/TextEncoder.)
                                               "<!doctype html><p>Sonda</p>")]]]
      (let [{:keys [store]} (fake-store)
            {:keys [target routes]} (fake-target {})
            receipt (await (effects/execute-plan!
                            store target {}
                            publish-plan
                            (assoc artifact :artifact/content content)))]
        (testing label
          (is (= :publication/materialized (:receipt/type receipt)))
          (is (= 1 (count @routes))))))))
