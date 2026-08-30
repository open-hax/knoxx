(ns knoxx.backend.law.translation-split-effective-test
  (:require [cljs.test :as t]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-fixture :as fixture]))

(defn- split-context
  "Build one complete raw candidate set and its manifest-order source members."
  []
  (let [manifest (fixture/manifest)
        claim (fixture/claim manifest)
        candidate-set (fixture/candidate-set manifest claim)]
    {:manifest manifest
     :claim claim
     :candidate-set candidate-set
     :source-splits (:split-manifest/splits manifest)}))

(defn- review-request
  "Build one approval/nonapproval request with correction presence made explicit."
  [operation-id overall corrected-text]
  (cond-> (fixture/review-request
           {:review/operation-id operation-id
            :review/overall overall})
    (some? corrected-text)
    (assoc :review/corrected-text corrected-text)

    (nil? corrected-text)
    (dissoc :review/corrected-text :review/editor-notes)))

(defn- receipt
  "Build one immutable fixture receipt for a manifest split."
  [manifest candidate-set source-split operation-id recorded-at overall corrected-text]
  (fixture/review-receipt
   manifest candidate-set (:split/id source-split) fixture/principal recorded-at
   (review-request operation-id overall corrected-text)))

(defn- approved-histories
  "Approve every split, correcting only the middle target member."
  [manifest candidate-set source-splits]
  (let [[first-split second-split third-split] source-splits
        first-review (receipt manifest candidate-set first-split "approve-0"
                              "2026-08-30T12:00:00.000Z" "approve" nil)
        second-review (receipt manifest candidate-set second-split "approve-1"
                               "2026-08-30T12:01:00.000Z" "approve"
                               "Corrected\n\n")
        third-review (receipt manifest candidate-set third-split "approve-2"
                              "2026-08-30T12:02:00.000Z" "approve" nil)]
    {(:split/id first-split) [first-review]
     (:split/id second-split) [second-review]
     (:split/id third-split) [third-review]}))

(defn- ready-value
  "Extract a ready value while asserting the discriminated result branch."
  [result]
  (t/is (= :ready (:effective-candidate-set/status result)))
  (:effective-candidate-set/value result))

(t/deftest complete-approved-history-composes-corrections-in-manifest-order
  (let [{:keys [manifest candidate-set source-splits]} (split-context)
        histories (approved-histories manifest candidate-set source-splits)
        result (split/effective-candidate-set fixture/digest manifest candidate-set
                                              histories)
        value (ready-value result)
        members (:effective-candidate-set/members value)]
    (t/is (= [0 1 2] (mapv :effective-candidate/split-index members)))
    (t/is (= (mapv :split/id source-splits)
             (mapv :effective-candidate/split-id members)))
    (t/is (= ["# Empieza aquí\n\n"
              "Corrected\n\n"
              "  Segundo párrafo.\n"]
             (mapv :effective-candidate/target-text members)))
    (t/is (= "# Empieza aquí\n\nCorrected\n\n  Segundo párrafo.\n"
             (:effective-candidate-set/text value)))
    (t/is (nil? (:effective-candidate/correction-id (first members))))
    (t/is (string? (:effective-candidate/correction-id (second members))))
    (t/is (= (:split-manifest/id manifest)
             (:effective-candidate-set/manifest-id value)))
    (t/is (= (:candidate-set/id candidate-set)
             (:effective-candidate-set/candidate-set-id value)))
    (t/is (= value
             (split/assert-effective-candidate-set-integrity!
              fixture/digest manifest candidate-set histories value)))))

(t/deftest composition-is-order-independent-but-review-lineage-sensitive
  (let [{:keys [manifest candidate-set source-splits]} (split-context)
        histories (approved-histories manifest candidate-set source-splits)
        second-split (second source-splits)
        older-rejection
        (receipt manifest candidate-set second-split "older-rejection"
                 "2026-08-30T11:00:00.000Z" "reject" nil)
        shuffled-histories
        (into (array-map)
              (reverse
               (seq (update histories (:split/id second-split)
                            #(vector (first %) older-rejection)))))
        original (ready-value
                  (split/effective-candidate-set fixture/digest manifest candidate-set
                                                 histories))
        shuffled (ready-value
                  (split/effective-candidate-set fixture/digest manifest candidate-set
                                                 shuffled-histories))
        later-same-target
        (receipt manifest candidate-set second-split "later-same-target"
                 "2026-08-30T13:00:00.000Z" "approve"
                 "Corrected\n\n")
        rereviewed-histories
        (update histories (:split/id second-split) conj later-same-target)
        rereviewed (ready-value
                    (split/effective-candidate-set fixture/digest manifest candidate-set
                                                   rereviewed-histories))]
    (t/testing "map and receipt arrival order do not affect latest-review selection"
      (t/is (= original shuffled)))

    (t/testing "byte-identical re-review changes lineage, identity, and revision"
      (t/is (= (:effective-candidate-set/text original)
               (:effective-candidate-set/text rereviewed)))
      (t/is (= (:effective-candidate-set/content-digest original)
               (:effective-candidate-set/content-digest rereviewed)))
      (doseq [field [:effective-candidate-set/id
                     :effective-candidate-set/digest
                     :effective-candidate-set/revision]]
        (t/is (not= (get original field) (get rereviewed field)))))))

(t/deftest raw-candidate-lineage-remains-bound-when-effective-bytes-match
  (let [{:keys [manifest claim candidate-set source-splits]} (split-context)
        changed-middle
        (split/candidate-split fixture/digest
                               (second (:candidate-claim/members claim))
                               "Machine draft that review replaces.\n\n")
        changed-set
        (split/complete-candidate-set
         fixture/digest manifest claim
         (assoc (:candidate-set/members candidate-set) 1 changed-middle))
        original (ready-value
                  (split/effective-candidate-set
                   fixture/digest manifest candidate-set
                   (approved-histories manifest candidate-set source-splits)))
        changed (ready-value
                 (split/effective-candidate-set
                  fixture/digest manifest changed-set
                  (approved-histories manifest changed-set source-splits)))]
    (t/is (= (:effective-candidate-set/text original)
             (:effective-candidate-set/text changed)))
    (t/is (= (:effective-candidate-set/content-digest original)
             (:effective-candidate-set/content-digest changed)))
    (doseq [field [:effective-candidate-set/id
                   :effective-candidate-set/digest
                   :effective-candidate-set/revision]]
      (t/is (not= (get original field) (get changed field))))))

(t/deftest every-blocking-latest-review-is-returned-without-partial-bytes
  (let [{:keys [manifest candidate-set source-splits]} (split-context)
        [_missing-split rejected-split editing-split] source-splits
        older-approval
        (receipt manifest candidate-set rejected-split "old-approval"
                 "2026-08-30T12:00:00.000Z" "approve" nil)
        latest-rejection
        (receipt manifest candidate-set rejected-split "new-rejection"
                 "2026-08-30T12:01:00.000Z" "reject" nil)
        latest-edit
        (receipt manifest candidate-set editing-split "needs-edit"
                 "2026-08-30T12:02:00.000Z" "needs_edit" nil)
        result
        (split/effective-candidate-set
         fixture/digest manifest candidate-set
         {(:split/id rejected-split) [latest-rejection older-approval]
          (:split/id editing-split) [latest-edit]})
        refusal (:effective-candidate-set/refusal result)
        blockers (:refusal/splits refusal)]
    (t/is (= :not-ready (:effective-candidate-set/status result)))
    (t/is (= :effective-candidate-set/not-ready (:refusal/type refusal)))
    (t/is (= (mapv :split/id source-splits)
             (mapv :effective-candidate-not-ready/split-id blockers)))
    (t/is (= [:review-missing :review-rejected :review-in-review]
             (mapv :effective-candidate-not-ready/reason blockers)))
    (t/is (= (:review/id latest-rejection)
             (:effective-candidate-not-ready/review-receipt-id
              (second blockers))))
    (t/is (not (contains? result :effective-candidate-set/value)))
    (t/is (every?
           #(every? #{:effective-candidate-not-ready/split-id
                      :effective-candidate-not-ready/split-index
                      :effective-candidate-not-ready/reason
                      :effective-candidate-not-ready/review-status
                      :effective-candidate-not-ready/review-receipt-id}
                    (keys %))
           blockers))))

(t/deftest invalid-source-candidate-or-review-evidence-fails-before-composition
  (let [{:keys [manifest claim candidate-set source-splits]} (split-context)
        histories (approved-histories manifest candidate-set source-splits)
        first-split (first source-splits)
        second-split (second source-splits)
        second-review
        (receipt manifest candidate-set second-split "wrong-split-review"
                 "2026-08-30T13:00:00.000Z" "approve" nil)]
    (t/is (thrown-with-msg?
           js/Error #"manifest identity"
           (split/effective-candidate-set
            fixture/digest
            (assoc manifest :split-manifest/source-digest "forged")
            candidate-set histories)))
    (t/is (thrown-with-msg?
           js/Error #"candidate digest"
           (split/effective-candidate-set
            fixture/digest manifest
            (assoc-in candidate-set [:candidate-set/members 0 :candidate/text]
                      "forged")
            histories)))
    (t/is (thrown-with-msg?
           js/Error #"another candidate split"
           (split/effective-candidate-set
            fixture/digest manifest candidate-set
            (assoc histories (:split/id first-split) [second-review]))))

    (let [value (ready-value
                 (split/effective-candidate-set fixture/digest manifest candidate-set
                                                histories))]
      (t/is (thrown-with-msg?
             js/Error #"effective candidate set identity"
             (split/assert-effective-candidate-set-integrity!
              fixture/digest manifest candidate-set histories
              (assoc value :effective-candidate-set/text "forged")))))

    ;; Claim is used above to prove the fixture retained a complete raw set; keep
    ;; the assertion local so a future fixture change cannot weaken this test.
    (t/is (= (count (:candidate-claim/members claim)) (count source-splits)))))

(t/deftest every-review-state-has-a-revision-that-revokes-stale-approval
  (let [{:keys [manifest candidate-set source-splits]} (split-context)
        missing (split/reviewed-output fixture/digest manifest candidate-set {})
        histories (approved-histories manifest candidate-set source-splits)
        ready (split/reviewed-output fixture/digest manifest candidate-set histories)
        first-split (first source-splits)
        rejected (receipt manifest candidate-set first-split "later-rejection"
                          "2026-08-30T13:00:00.000Z" "reject" nil)
        revoked (split/reviewed-output
                 fixture/digest manifest candidate-set
                 (update histories (:split/id first-split) conj rejected))]
    (t/testing "not-ready state preserves raw candidate bytes under its own revision"
      (t/is (= :not-ready (:translation-reviewed-output/status missing)))
      (t/is (= (:candidate-set/text candidate-set)
               (:translation-reviewed-output/text missing)))
      (t/is (re-find #"^translation-review-state-"
                     (:translation-reviewed-output/revision missing))))

    (t/testing "complete approval selects the corrected effective revision"
      (t/is (= :ready (:translation-reviewed-output/status ready)))
      (t/is (= (get-in ready [:translation-reviewed-output/effective-candidate-set
                              :effective-candidate-set/revision])
               (:translation-reviewed-output/revision ready))))

    (t/testing "a later rejection creates a different current revision"
      (t/is (= :not-ready (:translation-reviewed-output/status revoked)))
      (t/is (not= (:translation-reviewed-output/revision ready)
                  (:translation-reviewed-output/revision revoked)))
      (t/is (not= (:translation-reviewed-output/revision missing)
                  (:translation-reviewed-output/revision revoked))))))
