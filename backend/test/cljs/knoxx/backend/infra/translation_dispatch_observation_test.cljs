(ns knoxx.backend.infra.translation-dispatch-observation-test
  "Translation dispatch observation contracts and finite fixtures."
  (:require [cljs.test :as t]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.translation-dispatch-fixture :as fixture]
            [knoxx.backend.law.translation-dispatch :as law]))

(t/deftest ^:async observation-will-not-bind-a-batch-older-than-the-claim
  ;; Matching on garden, locale and document alone is not a match on THIS
  ;; dispatch: a tenant that translated the same document into the same locale
  ;; before has an older batch matching all three. Binding to it would let
  ;; `recover-settled-batch!` mint a receipt for a revision that batch never saw.
  (let [{:keys [deps]}
        (fixture/fixture :answer (fn [_ _] (throw (ex-info "connection reset" {})))
                 :observed (fn [_] {:batches [{:batch_id "batch-from-last-year"
                                               :created_at "2025-01-01T00:00:00.000Z"
                                               :document_ids ["knoxx.docs/probe"]}]}))
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the historical batch is not adopted"
      (t/is (not= "batch-from-last-year" (:dispatch/batch-id (:dispatch/record result)))))

    (t/testing "a legacy response cannot prove the keyed batch absent"
      (t/is (= :dispatch/accepted (:dispatch/outcome result))))))

(t/deftest ^:async observation-will-not-bind-a-batch-of-unknown-age
  (let [{:keys [deps]}
        (fixture/fixture :answer (fn [_ _] (throw (ex-info "connection reset" {})))
                 :observed (fn [_] {:batches [{:batch_id "batch-undated"
                                               :document_ids ["knoxx.docs/probe"]}]}))
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "an uncorrelated legacy row is not evidence of provenance or absence"
      (t/is (not= "batch-undated" (:dispatch/batch-id (:dispatch/record result))))
      (t/is (= :dispatch/accepted (:dispatch/outcome result))))))

(t/deftest batch-provenance-is-decided-by-creation-time
  (let [claim-at "2026-08-22T09:00:00.000Z"]
    (t/testing "a batch created at or after the claim can be the claim's"
      (t/is (law/batch-created-after? {:created_at claim-at} claim-at))
      (t/is (law/batch-created-after? {:created_at "2026-08-22T09:00:00.001Z"} claim-at))
      (t/is (law/batch-created-after? {:createdAt claim-at} claim-at) "camelCase too"))

    (t/testing "a batch created before the claim cannot be"
      (t/is (not (law/batch-created-after? {:created_at "2026-08-22T08:59:59.999Z"}
                                         claim-at))))

    (t/testing "an absent or unparseable creation time is refused, not guessed"
      (t/is (not (law/batch-created-after? {} claim-at)))
      (t/is (not (law/batch-created-after? {:created_at ""} claim-at)))
      (t/is (not (law/batch-created-after? {:created_at "yesterday"} claim-at)))
      (t/is (not (law/batch-created-after? {:created_at "2026-08-22T09:00:00Z"} claim-at))
          "an instant in another format cannot be compared as a string"))))

(t/deftest ^:async a-truncated-batch-listing-does-not-license-a-retry
  ;; The batch listing sorts newest-first and stops at `batch-listing-cap`, so a
  ;; busy garden can push our batch off the end. Reading that absence as "the
  ;; send did not land" is how a duplicate translation happens.
  (let [full-page (vec (repeat dispatch/batch-listing-cap
                               {:batch_id "someone-elses"
                                :created_at "2026-08-22T09:00:00.000Z"
                                :document_ids ["knoxx.docs/other"]}))
        {:keys [deps]} (fixture/fixture :answer (fn [_ _] (throw (ex-info "timeout" {})))
                                :observed (fn [_] {:batches full-page}))
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "the claim stays in flight rather than becoming retriable"
      (t/is (= :dispatch/accepted (:dispatch/outcome result)))
      (t/is (re-find #"truncated" (:dispatch/detail result))))))

(t/deftest ^:async a-short-batch-listing-does-license-a-retry
  (let [{:keys [deps]} (fixture/fixture :answer (fn [_ _] (throw (ex-info "timeout" {})))
                                :observed (fn [_] {:batches []}))
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "an absence that is conclusive is treated as one"
      (t/is (= :dispatch/failed (:dispatch/outcome result))))))

(t/deftest the-mirrored-listing-cap-matches-the-store
  (t/testing "the cap this namespace reasons about is the one the store applies"
    ;; Mirrored rather than imported because the store boundary does not expose
    ;; it. If that changes, this fails rather than silently drifting.
    (t/is (= 50 dispatch/batch-listing-cap))))

(t/deftest ^:async an-ambiguous-legacy-set-is-not-adopted
  ;; Two old-shape matching batches remain uncorrelated. A server returning them
  ;; may have ignored the dispatch_key filter, so Knoxx neither adopts one nor
  ;; treats their response as evidence that its exact request is absent.
  (let [candidate (fn [id] {:batch_id id
                            :created_at "2026-08-22T09:00:00.000Z"
                            :document_ids ["knoxx.docs/probe"]})
        {:keys [deps]}
        (fixture/fixture :answer (fn [_ _] (throw (ex-info "connection reset" {})))
                 :observed (fn [_] {:batches [(candidate "batch-a") (candidate "batch-b")]}))
        result (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context)))]
    (t/testing "neither candidate is bound"
      (t/is (nil? (:dispatch/batch-id (:dispatch/record result)))))

    (t/testing "the claim stays in flight, and says why"
      (t/is (= :dispatch/accepted (:dispatch/outcome result)))
      (t/is (re-find #"not proof the send failed" (:dispatch/detail result))))))

(t/deftest batch-matching-compares-every-field-the-batch-carries
  (let [context* {:dispatch/garden "knoxx.docs/promethean"
                  :dispatch/document-wire-id "knoxx.docs/probe"
                  :dispatch/source-locale :en
                  :dispatch/org-id "org-1"
                  :dispatch/membership-id "member-1"
                  :dispatch/project "knoxx-session"}
        work* {:document :knoxx.docs/probe :locale :es
               :revision fixture/dispatched-revision :replace-stale? false}
        at "2026-08-22T09:00:00.000Z"
        batch {:batch_id "b1"
               :created_at at
               :document_ids ["knoxx.docs/probe"]
               :project "knoxx-session"
               :source_lang "en"
               :target_lang "es"}]
    (t/testing "a fully agreeing batch matches"
      (t/is (law/batch-matches-dispatch? batch context* work* at)))

    (t/testing "a disagreement on any carried field is not a match"
      (doseq [[field value] [[:project "other"] [:source_lang "de"] [:target_lang "fr"]
                             [:document_ids ["knoxx.docs/other"]]]]
        (t/is (not (law/batch-matches-dispatch? (assoc batch field value)
                                              context* work* at))
            (str field " was ignored"))))

    (t/testing "a field the batch does not carry is not compared"
      ;; The batch record is another repository's shape. Requiring a field it may
      ;; not carry would reject every candidate and turn every ambiguous send
      ;; into a duplicate translation.
      (doseq [field [:project :source_lang :target_lang]]
        (t/is (law/batch-matches-dispatch? (dissoc batch field) context* work* at)
            (str "absent " field " was treated as a mismatch"))))))

(defn- ^:async observed-outcome!
  "The dispatch outcome when the create fails and observation returns `observed`."
  [observed]
  (let [{:keys [deps]} (fixture/fixture :answer (fn [_ _]
                                          (throw (ex-info "connection reset" {})))
                                :observed observed)]
    (:dispatch/outcome (await (dispatch/dispatch-work! deps (fixture/work) (fixture/context))))))

(def ^:private observed-candidate
  {:batch_id "b"
   :created_at "2026-08-22T09:00:00.000Z"
   :document_ids ["knoxx.docs/probe"]})

(t/deftest ^:async only-a-conclusive-absence-makes-a-claim-retriable
  ;; The invariant is about the *set* of outcomes: exactly one observation result
  ;; licenses a retry, and it is the one where absence is actually evidence.
  ;; Every other shape leaves the claim in flight.
  (t/testing "a conclusive absence is the only retriable case"
    (t/is (= :dispatch/failed (await (observed-outcome! (fn [_] {:batches []}))))))

  (t/testing "one matching batch is not — a match does not identify the request"
    (t/is (= :dispatch/accepted
           (await (observed-outcome! (fn [_] {:batches [observed-candidate]}))))))

  (t/testing "several matching batches are not"
    (t/is (= :dispatch/accepted
           (await (observed-outcome!
                   (fn [_] {:batches [observed-candidate
                                      (assoc observed-candidate :batch_id "b2")]}))))))

  (t/testing "a truncated listing is not, even with nothing matching"
    (let [full-page (vec (repeat dispatch/batch-listing-cap
                                 (assoc observed-candidate
                                        :document_ids ["knoxx.docs/other"])))]
      (t/is (= :dispatch/accepted
             (await (observed-outcome! (fn [_] {:batches full-page}))))))))

