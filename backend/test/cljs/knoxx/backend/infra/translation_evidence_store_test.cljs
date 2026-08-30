(ns knoxx.backend.infra.translation-evidence-store-test
  "Focused transition tests shared by every in-memory evidence-store caller."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]))

(def ^:private work
  {:document :knoxx.docs/probe
   :locale :es
   :revision "sha256-aaa111bbb222"
   :replace-stale? false})

(def ^:private context
  {:dispatch/garden "knoxx.docs/promethean"
   :dispatch/document-wire-id "knoxx.docs/probe"
   :dispatch/source-locale :en
   :dispatch/org-id "org-1"
   :dispatch/membership-id "member-1"})

(defn- accepted-record
  [at & {:keys [recovery-reason]}]
  (dispatch-law/dispatch-record
   work context :dispatch/accepted at :recovery-reason recovery-reason))

(deftest ^:async completed-claims-require-explicit-recovery-in-memory
  (let [evidence-store (store/memory-store)
        original (accepted-record "2026-08-22T09:00:00.000Z")
        _ (await (store/reserve-dispatch! evidence-store original))
        _ (await (store/bind-dispatch-batch!
                  evidence-store (:dispatch/key original) "batch-old"))
        _ (await (store/resolve-dispatch!
                  evidence-store (:dispatch/key original)
                  :dispatch/completed "candidate was originally present"))
        ordinary (await (store/reserve-dispatch!
                         evidence-store
                         (accepted-record "2026-08-22T10:00:00.000Z")))
        recovery (accepted-record
                  "2026-08-22T11:00:00.000Z"
                  :recovery-reason :candidate-unavailable)
        reopened (await (store/reserve-dispatch! evidence-store recovery))
        repeated (await (store/reserve-dispatch! evidence-store recovery))]
    (testing "an ordinary duplicate cannot reopen completed"
      (is (= :done (:reservation/status ordinary)))
      (is (= :dispatch/completed
             (:dispatch/outcome (:record ordinary)))))

    (testing "the validated recovery marker replaces completed wholesale"
      (is (= :reserved (:reservation/status reopened)))
      (is (= recovery (:record reopened)))
      (is (= :candidate-unavailable
             (:dispatch/recovery-reason (:record reopened))))
      (is (not (contains? (:record reopened) :dispatch/batch-id)))
      (is (not (contains? (:record reopened) :dispatch/detail))))

    (testing "the atom admits the recovery once and keeps its new claim in flight"
      (is (= :in-flight (:reservation/status repeated)))
      (is (= recovery (:record repeated)))
      (is (= recovery
             (await (store/dispatch-for-key!
                     evidence-store (:dispatch/key original))))))))
