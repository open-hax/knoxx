(ns knoxx.backend.domain.publication-plan-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.publication-plan :as plan]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def active-garden
  {:garden/id :knoxx.docs/promethean :garden/title "Promethean" :garden/status :active})

(def archived-garden
  {:garden/id :knoxx.docs/legacy :garden/title "Legacy" :garden/status :archived})

(def resource-index
  {:gardens {:knoxx.docs/promethean active-garden
             :knoxx.docs/legacy archived-garden}})

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

(def converged-observation
  {:materialized/revision "probe-revision"
   :materialized/path "/probe"
   :materialized/job-id "job-1"})

(defn- facts
  "Fact stubs. `:revision` is what `:source/current` resolves to."
  [{:keys [revision translated? approved? superseded? observed]
    :or {revision "probe-revision" translated? true approved? true}}]
  {:current-source-revision (constantly revision)
   :translated-revision? (constantly (boolean translated?))
   :approved? (constantly (boolean approved?))
   :source-revision-superseded? (constantly (boolean superseded?))
   :materialized-publication (constantly observed)})

(defn- plan-for [intent-override fact-override]
  (plan/reconcile-plan resource-index
                       (merge intent intent-override)
                       (facts fact-override)))

(def blocker-matrix
  "Every combination of translation/review/staleness evidence."
  (for [translated? [true false]
        approved? [true false]
        superseded? [true false]]
    {:translated? translated? :approved? approved? :superseded? superseded?}))

;; ── 1/2/3 non-public state precedence ─────────────────────────────────────

(deftest withheld-with-observation-removes
  (let [result (plan-for {:publication/state :withheld}
                         {:observed converged-observation})]
    (is (= :remove (:op result)))
    (is (= :publication-not-public (:reason result)))
    (is (= converged-observation (:observed result)))))

(deftest withheld-without-observation-noops
  (let [result (plan-for {:publication/state :withheld} {})]
    (is (= :noop (:op result)))
    (is (= :publication-not-public (:reason result)))))

(deftest archived-with-observation-removes-despite-missing-evidence
  (testing "archive is decided BEFORE translation blockers — an archived intent
            whose translation was never finished still has to come down"
    (doseq [evidence blocker-matrix]
      (let [result (plan-for {:publication/state :archived}
                             (assoc evidence :observed converged-observation))]
        (is (= :remove (:op result)))
        (is (= :publication-not-public (:reason result)))))))

;; ── 4 garden archive dominates ────────────────────────────────────────────

(deftest archived-garden-removes
  (let [result (plan-for {:publication/garden :knoxx.docs/legacy}
                         {:observed converged-observation})]
    (is (= :remove (:op result)))
    (is (= :garden-not-active (:reason result))))
  (testing "and never publishes, even with clean evidence"
    (is (= :noop (:op (plan-for {:publication/garden :knoxx.docs/legacy} {})))))
  (testing "an unknown garden is treated as non-active rather than published"
    (is (= :garden-not-active
           (:reason (plan-for {:publication/garden :knoxx.docs/ghost}
                              {:observed converged-observation}))))))

;; ── 5 removal is never blocked ────────────────────────────────────────────

(deftest removal-is-never-blocked
  (doseq [state [:withheld :archived]
          evidence blocker-matrix
          observed [nil converged-observation]]
    (let [result (plan-for {:publication/state state}
                           (assoc evidence :observed observed))]
      (is (contains? #{:remove :noop} (:op result))
          (str state " with " (pr-str evidence) " must never be :blocked"))
      (is (not= :blocked (:op result)))))
  (testing "the same holds for an archived garden"
    (doseq [evidence blocker-matrix]
      (is (contains? #{:remove :noop}
                     (:op (plan-for {:publication/garden :knoxx.docs/legacy}
                                    (assoc evidence :observed converged-observation))))))))

;; ── 6/7 convergence and drift ─────────────────────────────────────────────

(deftest converged-state-noops
  (let [result (plan-for {} {:observed converged-observation})]
    (is (= :noop (:op result)))
    (is (= {:materialized/revision "probe-revision" :materialized/path "/probe"}
           (:desired result)))
    (is (= "probe-revision" (:concrete-revision result)))))

(deftest revision-drift-publishes
  (let [result (plan-for {} {:observed (assoc converged-observation
                                              :materialized/revision "older")})]
    (is (= :publish (:op result)))
    (is (= "probe-revision" (get-in result [:desired :materialized/revision])))
    (testing "the prior observation is carried so the effect layer can replace it"
      (is (= "older" (get-in result [:previous :materialized/revision]))))))

(deftest path-only-drift-publishes
  (testing "a path change with an identical revision is still drift"
    (let [result (plan-for {} {:observed (assoc converged-observation
                                                :materialized/path "/old-route")})]
      (is (= :publish (:op result)))
      (is (= "/probe" (get-in result [:desired :materialized/path])))
      (is (= "/old-route" (get-in result [:previous :materialized/path])))
      (is (= "probe-revision" (get-in result [:desired :materialized/revision]))))))

(deftest first-publication-publishes
  (let [result (plan-for {} {})]
    (is (= :publish (:op result)))
    (is (nil? (:previous result)))))

;; ── 8/9 blocking ──────────────────────────────────────────────────────────

(deftest blocked-plan-carries-blockers
  (doseq [[label evidence expected]
          [["translation missing" {:translated? false} :translation-missing]
           ["review required" {:approved? false} :translation-review-required]
           ["stale" {:superseded? true} :translation-stale]]]
    (testing label
      (let [result (plan-for {} evidence)]
        (is (= :blocked (:op result)))
        (is (contains? (set (:blockers result)) expected))
        (is (= "probe-revision" (:concrete-revision result)))
        (testing "and no materialization is described"
          (is (nil? (:desired result))))))))

(deftest unresolved-revision-is-hard-blocker
  (let [result (plan-for {} {:revision nil})]
    (is (= :blocked (:op result)))
    (is (= [:publication-revision-unresolved] (:blockers result)))
    (is (nil? (:concrete-revision result)))
    (testing "no publish plan is emitted"
      (is (not= :publish (:op result))))))

;; ── 10 no publish plan carries a nil revision ─────────────────────────────

(deftest no-publish-plan-has-nil-revision
  (let [results (for [state [:published :withheld :archived]
                      garden [:knoxx.docs/promethean :knoxx.docs/legacy]
                      revision ["probe-revision" nil]
                      evidence blocker-matrix
                      observed [nil converged-observation]]
                  (plan-for {:publication/state state :publication/garden garden}
                            (assoc evidence :revision revision :observed observed)))]
    (is (pos? (count (filter #(= :publish (:op %)) results)))
        "the sweep must actually reach publish plans, or it proves nothing")
    (doseq [result results
            :when (= :publish (:op result))]
      (is (some? (:concrete-revision result)))
      (is (some? (get-in result [:desired :materialized/revision]))))))

;; ── 11 the plan shares the gate's revision ────────────────────────────────

(deftest plan-shares-gate-concrete-revision
  (testing "a facts stub whose current revision changes between calls proves the
            planner does not resolve a revision of its own"
    (let [answers (atom ["probe-revision" "second-revision"])
          next! (fn [& _]
                  (let [[head & tail] @answers]
                    (reset! answers (or tail [head]))
                    head))
          result (plan/reconcile-plan
                  resource-index intent
                  {:current-source-revision next!
                   :translated-revision? (constantly true)
                   :approved? (constantly true)
                   :source-revision-superseded? (constantly false)
                   :materialized-publication (constantly nil)})]
      (is (= "probe-revision" (:concrete-revision result)))
      (is (= "probe-revision" (get-in result [:desired :materialized/revision])))
      (is (not= "second-revision" (get-in result [:desired :materialized/revision]))))))

;; ── 12 determinism and purity ─────────────────────────────────────────────

(deftest plan-is-deterministic
  (doseq [evidence blocker-matrix]
    (is (= (plan-for {} (assoc evidence :observed converged-observation))
           (plan-for {} (assoc evidence :observed converged-observation))))))

(deftest plan-carries-no-adapter-detail
  (testing "observation may carry a job id, but the desired materialization
            describes only revision and path"
    (let [result (plan-for {} {:observed (assoc converged-observation
                                                :materialized/revision "older")})]
      (is (= #{:materialized/revision :materialized/path} (set (keys (:desired result)))))))
  (testing "no legacy backend appears in any plan"
    (let [legacy-marker (str "open" "planner")]
      (doseq [evidence blocker-matrix]
        (is (not (str/includes?
                  (str/lower-case
                   (pr-str (plan-for {} (assoc evidence :observed converged-observation))))
                  legacy-marker)))))))
