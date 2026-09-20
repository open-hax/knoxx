(ns knoxx.backend.infra.translation-dispatch-test
  "Translation dispatch dispatch contracts and finite fixtures."
  (:require [cljs.test :as t]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.translation-dispatch-fixture :as fixture]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as law]))

(t/deftest ^:async gated-work-reaches-the-worker-with-a-concrete-revision
  (let [{:keys [batches deps]} (fixture/fixture)
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the worker was asked once, in its own contract's shape"
      (t/is (= 1 (count @batches)))
      (t/is (= {:garden_id "knoxx.docs/promethean"
              :target_lang "es"
              :document_ids ["knoxx.docs/probe"]
              :source_lang "en"
              :org_id "org-1"
              :membership_id "member-1"
              :dispatch_key (:dispatch/key (:dispatch/record result))}
             (first @batches))))

    (t/testing "the revision the worker cannot carry is bound Knoxx-side"
      (t/is (= :dispatch/accepted (:dispatch/outcome result)))
      (t/is (= "sha256-aaa111bbb222" (:dispatch/revision (:dispatch/record result))))
      (t/is (= "batch-1" (:dispatch/batch-id (:dispatch/record result)))))

    (t/testing "no translation fact exists yet — the worker has not answered"
      (t/is (empty? (await (store/completed-translations! (:evidence-store deps) fixture/evidence-scope)))))))

(t/deftest ^:async duplicate-dispatch-does-not-enqueue-twice
  (let [{:keys [batches deps]} (fixture/fixture)
        first-result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        second-result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the second ask reuses the identity instead of translating again"
      (t/is (= :dispatch/accepted (:dispatch/outcome first-result)))
      (t/is (= :dispatch/duplicate (:dispatch/outcome second-result)))
      (t/is (= 1 (count @batches))
          "a second batch would translate the same revision twice"))

    (t/testing "the duplicate carries the running batch, so a caller can see it"
      (t/is (= "batch-1" (:dispatch/batch-id (:dispatch/record second-result)))))))

(t/deftest ^:async a-definite-worker-refusal-becomes-retriable
  ;; The create threw AND observation found no batch, so the send definitely did
  ;; not land. Only then is the claim safe to mark retriable.
  (let [{:keys [batches deps]} (fixture/fixture :answer (fn [_ _]
                                                  (throw (ex-info "worker refused" {}))))
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the outcome is failure, with the reason kept"
      (t/is (= :dispatch/failed (:dispatch/outcome result)))
      (t/is (= :dispatch/failed (:dispatch/outcome (:dispatch/record result))))
      (t/is (= "worker refused" (:dispatch/detail (:dispatch/record result)))))

    (t/testing "the claim reached a terminal outcome rather than sticking in flight"
      ;; Left in flight, the work would never be retried and never reported: it
      ;; would silently never happen.
      (let [stored (await (store/dispatch-for-key!
                           (:evidence-store deps)
                           (:dispatch/key (:dispatch/record result))))]
        (t/is (= :dispatch/failed (:dispatch/outcome stored)))))

    (t/testing "no translation fact was fabricated"
      (t/is (empty? (await (store/completed-translations! (:evidence-store deps) fixture/evidence-scope)))))
    (t/is (= 1 (count @batches)))))

(t/deftest ^:async a-nil-batch-id-cannot-produce-an-unattributable-dispatch
  (let [{:keys [deps]} (fixture/fixture :answer (fn [_ _] {:ok false}))
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "a client answering without a batch id is a failure, not success"
      (t/is (= :dispatch/failed (:dispatch/outcome result))))))

(t/deftest ^:async a-legacy-matching-batch-is-never-adopted-even-when-it-is-the-only-one
  ;; The create threw and one old-shape matching batch exists. Without the
  ;; dispatch key this does not identify the request that created it: one
  ;; unrelated actor creating the same document/locale batch produces exactly
  ;; this situation. A legacy server may also ignore the new dispatch_key query
  ;; field, so its response is not evidence that the keyed batch is absent.
  (let [{:keys [batches deps]}
        (fixture/fixture :answer (fn [_ _] (throw (ex-info "connection reset" {})))
                 :observed (fn [_] {:batches [{:batch_id "batch-existing"
                                               :created_at "2026-08-22T09:00:00.000Z"
                                               :document_ids ["knoxx.docs/probe"]}]}))
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "nothing is bound"
      (t/is (nil? (:dispatch/batch-id (:dispatch/record result)))))

    (t/testing "the claim is left in flight, not marked retriable"
      ;; Retriable would risk translating the same revision twice; bound would
      ;; risk fabricating evidence. In flight is the only honest answer.
      (t/is (= :dispatch/accepted (:dispatch/outcome result)))
      (t/is (re-find #"not proof the send failed" (:dispatch/detail result))))

    (t/testing "no second batch is ever sent"
      (let [retry (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
        (t/is (= 1 (count @batches)))
        (t/is (not= :dispatch/failed (:dispatch/outcome retry))
            "a claim that may be running must not become retriable")))))

(t/deftest ^:async an-unobservable-send-stays-in-flight-rather-than-duplicating
  ;; Both the create and the observation failed, so nothing is known. A stuck
  ;; claim is visible and fixable; a duplicate translation is neither.
  (let [{:keys [deps]}
        (fixture/fixture :answer (fn [_ _] (throw (ex-info "connection reset" {})))
                 :observed (fn [_] (throw (ex-info "worker unreachable" {}))))
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the conservative end is chosen, and it says why"
      (t/is (= :dispatch/accepted (:dispatch/outcome result)))
      (t/is (re-find #"observation also failed" (:dispatch/detail result))))))

(t/deftest ^:async a-batch-level-failure-resolves-by-batch-id-alone
  ;; The worker's terminal failure report names no document at all — it sends
  ;; status "failed" plus an error. Without resolving by batch id the claim would
  ;; sit in flight forever and never be retried.
  (let [{:keys [batches deps]} (fixture/fixture)
        _ (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))
        failed (await (dispatch/fail-batch! deps "batch-1" "All documents failed"))]
    (t/testing "the binding is found and marked failed"
      (t/is (= :dispatch/failed (:dispatch/outcome failed)))
      (t/is (= "All documents failed" (:dispatch/detail (:dispatch/record failed)))))

    (t/testing "the claim is retriable, so the work can happen"
      (let [retry (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
        (t/is (= :dispatch/accepted (:dispatch/outcome retry)))
        (t/is (= 2 (count @batches)))))

    (t/testing "an unknown batch is refused rather than guessed at"
      (t/is (= :dispatch-record-missing
             (:refusal/type (:translation/refusal
                             (await (dispatch/fail-batch! deps "batch-99" "boom")))))))))

(t/deftest ^:async intents-with-no-derived-work-are-not-dispatched
  (let [{:keys [batches deps]} (fixture/fixture)
        translated (assoc fixture/facts :translated-revision? (constantly true))
        results (await (dispatch/dispatch-intents! deps [fixture/intent] translated fixture/scope))]
    (t/testing "an already-translated intent derives nothing and is absent"
      (t/is (empty? results))
      (t/is (empty? @batches)))))

(t/deftest ^:async dispatch-intents-reports-one-entry-per-dispatched-intent
  (let [{:keys [batches deps]} (fixture/fixture)
        results (await (dispatch/dispatch-intents! deps [fixture/intent] fixture/facts fixture/scope))]
    (t/testing "the report names the publication it belongs to"
      (t/is (= 1 (count results)))
      (t/is (= :knoxx.docs/probe-es (:publication/id (first results))))
      (t/is (= :dispatch/accepted (:dispatch/outcome (first results))))
      (t/is (= 1 (count @batches))))))

(t/deftest a-selector-revision-can-never-be-dispatched
  (t/testing "work whose revision is a selector is refused before the worker is called"
    ;; Defence in depth: the gate resolves the selector, so this shape should be
    ;; impossible — which is exactly why it is asserted rather than assumed.
    (t/is (thrown? js/Error
                 (law/assert-valid! :translation-dispatch/work
                                    law/DerivedWork
                                    {:document :knoxx.docs/probe
                                     :locale :es
                                     :revision :source/current
                                     :replace-stale? false})))))

(t/deftest retriable-and-terminal-outcomes-are-disjoint-and-complete
  (t/testing "every outcome is exactly one of accepted, retriable, or terminal"
    (doseq [outcome law/outcomes]
      (let [accepted? (= :dispatch/accepted outcome)
            retriable? (law/retriable? outcome)
            terminal? (law/terminal? outcome)]
        (t/is (= 1 (count (filter true? [accepted? retriable? terminal?])))
            (str outcome " is not in exactly one class")))))

  (t/testing "a completed translation is never retriable"
    (t/is (not (law/retriable? :dispatch/completed)))
    (t/is (law/terminal? :dispatch/completed)))

  (t/testing "a failed or rejected attempt is retriable"
    (t/is (law/retriable? :dispatch/failed))
    (t/is (law/retriable? :dispatch/rejected))))

