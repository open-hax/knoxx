(ns knoxx.backend.domain.publication-gate-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.publication-gate :as gate]
            [knoxx.backend.law.publication :as law]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def spanish-intent
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :required
   :document/source-locale :en})

(defn- recording-facts
  "Fact stubs that record every lookup argument, so a test can prove a selector
   token never reaches a receipt lookup."
  [{:keys [revision translated? approved? superseded?]}]
  (let [calls (atom [])
        record! (fn [op args] (swap! calls conj (into [op] args)))]
    {:calls calls
     :facts {:current-source-revision (fn [document]
                                        (record! :current-source-revision [document])
                                        revision)
             :translated-revision? (fn [& args]
                                     (record! :translated-revision? args)
                                     (boolean translated?))
             :approved? (fn [& args]
                          (record! :approved? args)
                          (boolean approved?))
             :source-revision-superseded? (fn [& args]
                                            (record! :source-revision-superseded? args)
                                            (boolean superseded?))}}))

(defn- clean-facts []
  (recording-facts {:revision "probe-revision" :translated? true :approved? true}))

;; ── 1/2/3 revision resolution ──────────────────────────────────────────────

(deftest source-current-resolves-once
  (let [{:keys [calls facts]} (clean-facts)
        evidence (gate/publication-evidence spanish-intent facts)]
    (is (= "probe-revision" (:concrete-revision evidence)))
    (testing "the selector resolves exactly once"
      (is (= 1 (count (filter #(= :current-source-revision (first %)) @calls)))))
    (testing "and the selector token never reaches any receipt lookup"
      (is (not-any? #(some #{:source/current} %) @calls))
      (doseq [call @calls
              :when (contains? #{:translated-revision? :approved?} (first call))]
        (is (some #{"probe-revision"} call))))))

(deftest unresolvable-source-current-blocks
  (let [{:keys [calls facts]} (recording-facts {:revision nil})
        evidence (gate/publication-evidence spanish-intent facts)]
    (is (= [:publication-revision-unresolved] (:blockers evidence)))
    (is (nil? (:concrete-revision evidence)))
    (testing "no evidence lookup happens against a revision that does not exist"
      (is (= [:current-source-revision] (mapv first @calls))))
    (testing "and no translation work is derived"
      (is (nil? (gate/translation-work spanish-intent evidence))))
    (testing "nor is it admissible"
      (is (false? (gate/admissible? spanish-intent evidence))))))

(deftest evidence-returns-concrete-revision-and-blockers
  (let [{:keys [facts]} (clean-facts)
        evidence (gate/publication-evidence spanish-intent facts)]
    (is (= #{:concrete-revision :blockers} (set (keys evidence))))
    (is (= (:blockers evidence) (gate/blockers evidence)))))

;; ── The compute-once law (the review thread's regression) ─────────────────

(deftest one-evidence-result-supplies-every-consumer
  (testing "a facts stub whose current revision CHANGES between calls proves the
            gate resolves once and threads that single revision everywhere"
    (let [answers (atom ["first-revision" "second-revision" "third-revision"])
          facts {:current-source-revision (fn [_document]
                                            (let [[head & tail] @answers]
                                              (reset! answers (or tail [head]))
                                              head))
                 :translated-revision? (constantly false)
                 :approved? (constantly true)
                 :source-revision-superseded? (constantly false)}
          result (gate/gate spanish-intent facts)]
      (is (= "first-revision" (:concrete-revision result)))
      (testing "the queued work carries the SAME revision the decision used"
        (is (= "first-revision" (get-in result [:translation-work :action/with :revision])))
        (is (not= "second-revision"
                  (get-in result [:translation-work :action/with :revision])))))))

(deftest gate-is-deterministic
  (let [{:keys [facts]} (clean-facts)]
    (is (= (gate/publication-evidence spanish-intent facts)
           (gate/publication-evidence spanish-intent facts)))))

;; ── 4 blocker semantics ────────────────────────────────────────────────────

(deftest translation-missing-blocks
  (let [{:keys [facts]} (recording-facts {:revision "probe-revision"
                                          :translated? false :approved? true})]
    (is (= [:translation-missing]
           (:blockers (gate/publication-evidence spanish-intent facts))))))

(deftest review-required-blocks
  (let [{:keys [facts]} (recording-facts {:revision "probe-revision"
                                          :translated? true :approved? false})]
    (is (= [:translation-review-required]
           (:blockers (gate/publication-evidence spanish-intent facts))))))

(deftest stale-translation-blocks
  (let [{:keys [facts]} (recording-facts {:revision "probe-revision"
                                          :translated? true :approved? true
                                          :superseded? true})]
    (is (= [:translation-stale]
           (:blockers (gate/publication-evidence spanish-intent facts))))))

(deftest a-clean-publication-is-admissible
  (let [{:keys [facts]} (clean-facts)
        evidence (gate/publication-evidence spanish-intent facts)]
    (is (empty? (:blockers evidence)))
    (is (true? (gate/admissible? spanish-intent evidence)))))

;; ── 5 source locale comes from the intent ─────────────────────────────────

(deftest source-locale-comes-from-intent
  (testing "a publication in the document's own language needs no translation"
    (let [same-locale (assoc spanish-intent
                             :publication/locale :en
                             :translation/review :none)
          {:keys [calls facts]} (recording-facts {:revision "probe-revision"
                                                  :translated? false})
          evidence (gate/publication-evidence same-locale facts)]
      (is (empty? (:blockers evidence)))
      (is (false? (gate/translation-required? same-locale)))
      (testing "and no translation lookup is performed at all"
        (is (not-any? #(= :translated-revision? (first %)) @calls)))))
  (testing "the gate never defaults a source language"
    (let [no-source (dissoc spanish-intent :document/source-locale)]
      (is (true? (gate/translation-required? no-source))
          "an absent source locale differs from :es, so translation is required
           rather than silently assumed to match"))))

;; ── 7/8/9 desired state gates queueing ────────────────────────────────────

(deftest only-published-intent-derives-translation-work
  (let [{:keys [facts]} (recording-facts {:revision "probe-revision"
                                          :translated? false :approved? true})]
    (doseq [[state expected] [[:archived nil] [:withheld nil]]]
      (testing (str state " derives no work even though its translation is missing")
        (let [intent (assoc spanish-intent :publication/state state)
              evidence (gate/publication-evidence intent facts)]
          (is (contains? (set (:blockers evidence)) :translation-missing))
          (is (= expected (gate/translation-work intent evidence)))
          (is (false? (gate/admissible? intent evidence))))))
    (testing "the otherwise identical published intent does derive work"
      (let [intent (assoc spanish-intent :publication/state :published)
            evidence (gate/publication-evidence intent facts)
            work (gate/translation-work intent evidence)]
        (is (= :actions/request-translation (:action/id work)))))))

;; ── 10 work is keyed to the concrete revision ─────────────────────────────

(deftest work-is-keyed-to-concrete-revision
  (let [{:keys [facts]} (recording-facts {:revision "probe-revision"
                                          :translated? false :approved? true})
        evidence (gate/publication-evidence spanish-intent facts)
        work (gate/translation-work spanish-intent evidence)]
    (is (= "probe-revision" (get-in work [:action/with :revision])))
    (is (not= :source/current (get-in work [:action/with :revision])))
    (is (= :knoxx.docs/probe (get-in work [:action/with :document])))
    (is (= :es (get-in work [:action/with :locale])))
    (testing "replace-stale? is set only for stale evidence"
      (is (false? (get-in work [:action/with :replace-stale?])))
      (let [stale-facts (:facts (recording-facts {:revision "probe-revision"
                                                  :translated? true :approved? true
                                                  :superseded? true}))
            stale-evidence (gate/publication-evidence spanish-intent stale-facts)]
        (is (true? (get-in (gate/translation-work spanish-intent stale-evidence)
                           [:action/with :replace-stale?])))))))

;; ── 11 approval is revision-specific ─────────────────────────────────────

(deftest approval-is-revision-specific
  (let [old-approval {:review/document :knoxx.docs/probe
                      :review/locale :es
                      :review/revision "old-revision"
                      :review/state :approved}]
    (testing "the old approval satisfied the old revision"
      (is (true? (gate/review-satisfies-intent? old-approval spanish-intent "old-revision"))))
    (testing "but cannot satisfy the replacement revision"
      (is (false? (gate/review-satisfies-intent? old-approval spanish-intent "new-revision"))))
    (testing "and the old receipt is untouched — nothing was deleted"
      (is (= :approved (:review/state old-approval)))
      (is (= "old-revision" (:review/revision old-approval))))
    (testing "a non-approved review never satisfies, whatever the revision"
      (is (false? (gate/review-satisfies-intent?
                   (assoc old-approval :review/state :rejected)
                   spanish-intent "old-revision"))))
    (testing "nor does an approval for another locale"
      (is (false? (gate/review-satisfies-intent?
                   (assoc old-approval :review/locale :fr)
                   spanish-intent "old-revision"))))))

;; ── Purity ─────────────────────────────────────────────────────────────────

(deftest gate-emits-no-operational-state
  (let [{:keys [facts]} (recording-facts {:revision "probe-revision"
                                          :translated? false :approved? false})
        result (gate/gate spanish-intent facts)
        rendered (pr-str result)]
    (doseq [operational [":translating" ":reviewing" ":worker-failed"
                         "published-at" "job-id"]]
      (is (not (str/includes? rendered operational))
          (str operational " must never appear in gate output")))))

;; ── the two admissibility layers share one vocabulary (Codex P1 on #234) ───

(deftest the-gate-and-the-contract-layer-cannot-drift-on-what-publish-means
  (testing "which state means publish is owned by the law, not restated here"
    (is (true? (law/publishes? {:publication/state :published})))
    (doseq [state [:withheld :archived nil :publish "published"]]
      (is (false? (law/publishes? {:publication/state state}))
          (str (pr-str state) " must not read as a request to publish"))))
  (testing "publishing states are a strict subset of the reconcilable ones —
            :withheld reconciles toward removal, which is lawful but is not a
            request to publish"
    (is (every? law/reconcilable-publication-states law/publishing-publication-states))
    (is (not (every? law/publishing-publication-states
                     law/reconcilable-publication-states))))
  (testing "and the gate's evidential admissibility agrees with it: no amount of
            clean evidence admits a state the law says does not publish"
    (doseq [state [:withheld :archived nil]]
      (is (false? (gate/admissible? {:publication/state state}
                                    {:concrete-revision "rev-1" :blockers []}))
          (str (pr-str state) " must never be admissible")))
    (is (true? (gate/admissible? {:publication/state :published}
                                 {:concrete-revision "rev-1" :blockers []})))))

