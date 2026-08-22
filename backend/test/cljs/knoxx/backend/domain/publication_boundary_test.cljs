(ns knoxx.backend.domain.publication-boundary-test
  "Whole-seam proof: desired resources -> pure plan -> effects -> observed
  receipts, with no hosted publishing backend present anywhere."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.publication-plan :as plan]
            [knoxx.backend.domain.publication-receipts :as receipts]
            [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.infra.publication-target-memory :as memory]
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
   :publication/path "/docs/demo"
   :translation/review :required
   :document/source-locale :en})

(def artifact
  "Rendered ABOVE the boundary and handed to it, at the same revision `facts`
   reports as current — an artifact naming a different one is a conflict, not
   something the seam is allowed to publish."
  {:artifact/content "<!doctype html><p>demo</p>"
   :artifact/media-type "text/html"
   :artifact/encoding "utf-8"
   :artifact/locale :es
   :artifact/revision "abc123"})

(def resource-index
  {:gardens {:knoxx.docs/promethean {:garden/id :knoxx.docs/promethean
                                     :garden/status :active}
             :knoxx.docs/legacy {:garden/id :knoxx.docs/legacy
                                 :garden/status :archived}}})

(defn- facts
  "Clean evidence, with observation read live from the target so the planner sees
   what the adapter actually did."
  [target-bundle]
  {:current-source-revision (constantly "abc123")
   :translated-revision? (constantly true)
   :approved? (constantly true)
   :source-revision-superseded? (constantly false)
   ;; Observation is keyed by publication identity, not by the desired path. A
   ;; path-keyed lookup cannot see the route a move is replacing.
   :materialized-publication (fn [publication-intent]
                               (->> (vals (memory/public-routes target-bundle))
                                    (filter #(= (:publication/id %)
                                                (:publication/id publication-intent)))
                                    first))})

(defn- ^:async converge!
  "Plan for `current-intent` against live observation, then execute."
  [store target-bundle current-intent]
  (let [current-plan (plan/reconcile-plan resource-index
                                          current-intent
                                          (facts target-bundle))]
    {:plan current-plan
     :receipt (await (effects/execute-plan! store (:target target-bundle)
                                            {} current-plan artifact))}))

;; ── 5 the card's own sketch ───────────────────────────────────────────────

(deftest ^:async publication-boundary-without-a-hosted-backend
  (let [{:keys [store]} (memory/memory-store)
        target-bundle (memory/memory-target)
        first-run (await (converge! store target-bundle intent))
        replay (await (converge! store target-bundle intent))]
    (is (= :publish (:op (:plan first-run))))
    (is (= :publication/materialized (:receipt/type (:receipt first-run))))
    (testing "replay converges: the second plan is a noop, nothing republished"
      (is (= :noop (:op (:plan replay))))
      (is (= 1 (memory/materialization-count target-bundle))))
    (testing "and the observed materialization is the expected revision and path"
      (is (= {:materialized/revision "abc123" :materialized/path "/docs/demo"}
             (receipts/observed-materialization (:receipt first-run)))))
    (testing "the receipt satisfies the full observation contract"
      (is (true? (receipts/materialized? (:receipt first-run)))))))

(deftest ^:async replaying-the-same-plan-yields-the-same-receipt
  (testing "executing the identical plan twice — bypassing re-planning — returns
            an equal receipt and leaves exactly one materialization"
    (let [{:keys [store]} (memory/memory-store)
          target-bundle (memory/memory-target)
          publish-plan (plan/reconcile-plan resource-index intent (facts target-bundle))
          first-receipt (await (effects/execute-plan!
                                store (:target target-bundle) {} publish-plan artifact))
          replay-receipt (await (effects/execute-plan!
                                 store (:target target-bundle) {} publish-plan artifact))]
      (is (= first-receipt replay-receipt))
      (is (= 1 (memory/materialization-count target-bundle)))
      (is (= 1 (count (memory/public-routes target-bundle)))))))

;; ── 6 path move ───────────────────────────────────────────────────────────

(deftest ^:async boundary-covers-path-move
  (let [{:keys [store]} (memory/memory-store)
        target-bundle (memory/memory-target)
        _ (await (converge! store target-bundle intent))
        moved (assoc intent :publication/path "/docs/moved")
        after (await (converge! store target-bundle moved))]
    (is (= :publish (:op (:plan after))))
    (testing "exactly one route is public, and it is the new one"
      (is (= ["/docs/moved"] (keys (memory/public-routes target-bundle)))))
    (testing "the prior route is gone rather than orphaned alongside it"
      (is (nil? (get (memory/public-routes target-bundle) "/docs/demo"))))))

;; ── 7 withheld and archived removal ───────────────────────────────────────

(deftest ^:async boundary-covers-withheld-and-archive-removal
  (doseq [state [:withheld :archived]]
    (testing (str "flipping desired state to " state " converges to removal")
      (let [{:keys [store]} (memory/memory-store)
            target-bundle (memory/memory-target)
            _ (await (converge! store target-bundle intent))
            _ (is (= 1 (count (memory/public-routes target-bundle))))
            after (await (converge! store target-bundle
                                    (assoc intent :publication/state state)))]
        (is (= :remove (:op (:plan after))))
        (is (= :publication/removed (:receipt/type (:receipt after))))
        (is (empty? (memory/public-routes target-bundle)))))))

;; ── 8 archived garden removal ─────────────────────────────────────────────

(deftest ^:async boundary-covers-archived-garden-removal
  (let [{:keys [store]} (memory/memory-store)
        target-bundle (memory/memory-target)
        _ (await (converge! store target-bundle intent))
        after (await (converge! store target-bundle
                                (assoc intent :publication/garden :knoxx.docs/legacy)))]
    (is (= :remove (:op (:plan after))))
    (is (= :garden-not-active (:reason (:plan after))))
    (is (empty? (memory/public-routes target-bundle)))))

;; ── 9 observation after convergence ───────────────────────────────────────

(deftest ^:async observation-after-convergence-matches-plan
  (let [{:keys [store]} (memory/memory-store)
        target-bundle (memory/memory-target)
        _ (await (converge! store target-bundle intent))
        observed (await (effects/observe! (:target target-bundle) {} intent))]
    (is (some? observed))
    (testing "the observation makes the planner emit :noop"
      (let [next-plan (plan/reconcile-plan resource-index intent (facts target-bundle))]
        (is (= :noop (:op next-plan)))))
    (testing "and it is comparable to the planner's desired materialization"
      (is (= (plan/desired-materialization intent "abc123")
             (select-keys observed receipts/drift-keys))))))

;; ── 10 failure is drift, intent untouched ─────────────────────────────────

(deftest ^:async adapter-failure-leaves-intent-untouched
  (let [{:keys [store]} (memory/memory-store)
        target-bundle (memory/memory-target {:fail? true})
        before intent
        {:keys [receipt]} (await (converge! store target-bundle intent))]
    (is (= :publication/failed (:receipt/type receipt)))
    (is (true? (:failure/drift? receipt)))
    (is (empty? (memory/public-routes target-bundle)))
    (testing "desired intent is the same map, not merely an equal one"
      (is (identical? before intent))
      (is (= :published (:publication/state intent))))
    (testing "and the failure produces no observable materialization"
      (is (nil? (receipts/observed-materialization receipt))))))

;; ── 11 absence of a hosted backend is proven, not assumed ─────────────────

(deftest no-hosted-backend-in-the-seam
  (let [legacy-marker (str "open" "planner")
        read-source (fn [relative-path]
                      (str/lower-case
                       (.readFileSync node-fs
                                      (.join path (.cwd js/process) relative-path)
                                      "utf8")))]
    (doseq [relative-path ["src/cljs/knoxx/backend/domain/publication_plan.cljs"
                           "src/cljs/knoxx/backend/domain/publication_gate.cljs"
                           "src/cljs/knoxx/backend/domain/publication_receipts.cljs"
                           "src/cljs/knoxx/backend/law/publication.cljs"
                           "src/cljs/knoxx/backend/law/publication_receipts.cljs"
                           "src/cljs/knoxx/backend/infra/publication_effects.cljs"
                           "src/cljs/knoxx/backend/infra/publication_target_memory.cljs"
                           "test/cljs/knoxx/backend/domain/publication_boundary_test.cljs"]]
      (testing relative-path
        (is (not (str/includes? (read-source relative-path) legacy-marker))
            "the whole seam must be expressible with no hosted backend named")))))

;; ── a removal must be attributable to its publication (Codex P1 on #237) ───

(deftest ^:async removal-receipts-are-attributable-to-their-publication
  (testing "observed-for filters receipts by :publication/id, so a removal
            without one is invisible to the projection: the retracted
            materialization stays observed, and a later republish of the same
            revision reads as :noop while nothing is public"
    (let [{:keys [store]} (memory/memory-store)
          target-bundle (memory/memory-target)
          published (await (converge! store target-bundle intent))
          removed (await (converge! store target-bundle
                                    (assoc intent :publication/state :withheld)))
          publish-receipt (:receipt published)
          removal-receipt (:receipt removed)]
      (is (= :publication/materialized (:receipt/type publish-receipt)))
      (is (= :publication/removed (:receipt/type removal-receipt)))
      (testing "the adapter's own removal carries the id — not just a hand-built one"
        (is (= (:publication/id intent) (:publication/id removal-receipt))))
      (testing "so the projection sees the retraction"
        (is (nil? (receipts/observed-for [publish-receipt removal-receipt]
                                         (:publication/id intent)))))
      (testing "and without the id the projection would still report the old route"
        (is (some? (receipts/observed-for
                    [publish-receipt (dissoc removal-receipt :publication/id)]
                    (:publication/id intent)))
            "which is precisely the bug: the removal is filtered out"))
      (testing "the contract refuses an unattributable removal outright"
        (is (thrown? js/Error
                     (law/assert-receipt! (dissoc removal-receipt :publication/id))))))))

