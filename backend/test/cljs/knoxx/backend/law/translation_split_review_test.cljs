(ns knoxx.backend.law.translation-split-review-test
  (:require [cljs.test :as t]
            [clojure.string :as str]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-fixture :as fixture]))

(defn- review-context
  "Build authenticated source, candidate set, and selected split fixtures."
  []
  (let [manifest (fixture/manifest)
        candidate-claim (fixture/claim manifest)
        candidate-set (fixture/candidate-set manifest candidate-claim)]
    {:manifest manifest
     :candidate-claim candidate-claim
     :candidate-set candidate-set
     :split-id (:split/id (second (:split-manifest/splits manifest)))}))

(defn- receipt
  "Build one server-attributed receipt with request overrides."
  [manifest candidate-set split-id recorded-at overrides]
  (fixture/review-receipt
   manifest candidate-set split-id fixture/principal recorded-at
   (fixture/review-request overrides)))

(defn- nonapproval-request
  "Build a review request that cannot carry positive corrected memory."
  [overall operation-id]
  (-> (fixture/review-request {:review/operation-id operation-id
                               :review/overall overall})
      (dissoc :review/corrected-text)))

(t/deftest review-request-is-closed-and-receipt-is-server-attributed
  (let [{:keys [manifest candidate-set split-id]} (review-context)
        request (fixture/review-request)
        value (fixture/review-receipt manifest candidate-set split-id request)
        candidate (some #(when (= split-id (:candidate/split-id %)) %)
                        (:candidate-set/members candidate-set))]
    (t/testing "candidate coordinates, actor, time, and derived state come from the server"
      (t/is (= (:split-manifest/id manifest) (:review/manifest-id value)))
      (t/is (= (:candidate-set/id candidate-set) (:review/candidate-set-id value)))
      (t/is (= (:candidate/digest candidate) (:review/candidate-digest value)))
      (t/is (= fixture/principal (:review/principal value)))
      (t/is (= fixture/recorded-at (:review/recorded-at value)))
      (t/is (= :approved (:review/status value)))
      (t/is (string? (:review/correction-id value))))

    (t/testing "a client cannot smuggle any receipt-owned authority into its request"
      (doseq [[field forged]
              [[:review/principal {:principal/user-id "claimed-admin"}]
               [:review/recorded-at "2000-01-01T00:00:00.000Z"]
               [:review/source-revision "forged-source"]
               [:review/manifest-id "forged-manifest"]
               [:review/candidate-set-id "forged-set"]
               [:review/candidate-set-digest "forged-set-digest"]
               [:review/candidate-revision "forged-revision"]
               [:review/split-id "forged-split"]
               [:review/candidate-attempt-id "forged-attempt"]
               [:review/candidate-digest "forged-candidate"]
               [:review/status :approved]
               [:review/correction-id "forged-correction"]
               [:review/id "forged-receipt"]
               [:review/digest "forged-receipt-digest"]]]
        (t/is (thrown-with-msg?
               js/Error
               #"invalid review-request"
               (fixture/review-receipt
                manifest candidate-set split-id (assoc request field forged))))))

    (t/testing "principal and time fail closed at their server-owned boundaries"
      (t/is (thrown-with-msg?
             js/Error
             #"invalid review-principal"
             (fixture/review-receipt
              manifest candidate-set split-id {} fixture/recorded-at request)))
      (doseq [invalid-at ["not-a-time"
                          "2026-08-30T12:00:00Z"
                          "2026-08-30T14:00:00.000+02:00"
                          "2026-02-30T12:00:00.000Z"]]
        (t/is (thrown-with-msg?
               js/Error
               #"invalid review-recorded-at"
               (fixture/review-receipt
                manifest candidate-set split-id fixture/principal invalid-at request)))))

    (t/testing "any authenticated durable principal coordinate is sufficient"
      (doseq [principal [{:principal/user-id "reviewer-1"}
                         {:principal/user-email "reviewer@example.test"}
                         {:principal/membership-id "membership-1"}]]
        (t/is (= principal
                 (:review/principal
                  (fixture/review-receipt
                   manifest candidate-set split-id principal
                   fixture/recorded-at request))))))

    (t/testing "map insertion order and explicit nil optionals cannot change identity"
      (let [reversed-request (into (array-map) (reverse (seq request)))
            reversed-principal (into (array-map) (reverse (seq fixture/principal)))
            reordered (fixture/review-receipt
                       manifest candidate-set split-id reversed-principal
                       fixture/recorded-at reversed-request)
            without-optionals (dissoc request :review/corrected-text :review/editor-notes)
            omitted (fixture/review-receipt
                     manifest candidate-set split-id without-optionals)
            explicit-nil (fixture/review-receipt
                          manifest candidate-set split-id
                          (assoc without-optionals
                                 :review/corrected-text nil
                                 :review/editor-notes nil))]
        (t/is (= value reordered))
        (t/is (= omitted explicit-nil))))))

(t/deftest every-translation-revision-is-concrete
  (let [manifest (fixture/manifest)]
    (doseq [selector ["source/current" ":source/current" " source/current "]]
      (t/is (thrown-with-msg?
             js/Error
             #"invalid manifest"
             (split/split-manifest
              fixture/digest
              (assoc fixture/coordinates
                     :source-revision selector
                     :source-text fixture/source
                     :source-parts fixture/source-parts)))))
    (t/is (thrown-with-msg?
           js/Error
           #"invalid candidate-claim"
           (split/candidate-claim fixture/digest manifest "source/current")))))

(t/deftest effective-review-deterministically-supersedes-older-evidence
  (let [{:keys [manifest candidate-set split-id]} (review-context)
        approval (receipt manifest candidate-set split-id
                          "2026-08-30T12:00:00.000Z" {})
        rejection (fixture/review-receipt
                   manifest candidate-set split-id fixture/principal
                   "2026-08-30T12:01:00.000Z"
                   (nonapproval-request "reject" "review-operation-2"))
        needs-edit (fixture/review-receipt
                    manifest candidate-set split-id fixture/principal
                    "2026-08-30T12:02:00.000Z"
                    (nonapproval-request "needs_edit" "review-operation-3"))
        later-approval (receipt manifest candidate-set split-id
                                "2026-08-30T12:03:00.000Z"
                                {:review/operation-id "review-operation-4"})]
    (t/testing "new rejection or needs-edit suppresses prior positive memory in any input order"
      (doseq [history [[approval rejection]
                       [rejection approval]
                       [approval needs-edit]
                       [needs-edit approval]]]
        (t/is (nil? (split/approved-memory-example
                     fixture/digest manifest candidate-set split-id history)))))

    (t/testing "a genuinely later approval restores corrected positive memory"
      (let [example (split/approved-memory-example
                     fixture/digest manifest candidate-set split-id
                     [needs-edit approval later-approval])]
        (t/is (= "Párrafo inicial corregido.\n\n"
                 (:translation-memory/target-text example)))
        (t/is (= (:review/id later-approval)
                 (:translation-memory/review-receipt-id example)))))

    (t/testing "empty history has no effective approval"
      (t/is (nil? (split/approved-memory-example
                   fixture/digest manifest candidate-set split-id []))))

    (t/testing "identical retries dedupe while changed reuse conflicts"
      (t/is (= approval
               (split/effective-review-receipt
                fixture/digest manifest candidate-set split-id [approval approval])))
      (let [conflicting (fixture/review-receipt
                         manifest candidate-set split-id fixture/principal
                         fixture/recorded-at
                         (nonapproval-request "reject" "review-operation-1"))]
        (t/is (= (:review/id approval) (:review/id conflicting)))
        (t/is (thrown-with-msg?
               js/Error
               #"conflicts behind one receipt id"
               (split/effective-review-receipt
                fixture/digest manifest candidate-set split-id
                [approval conflicting])))))))

(t/deftest exact-time-ties-have-an-order-independent-winner
  (let [{:keys [manifest candidate-set split-id]} (review-context)
        first-receipt (receipt manifest candidate-set split-id fixture/recorded-at
                               {:review/operation-id "tie-operation-a"})
        second-receipt (fixture/review-receipt
                        manifest candidate-set split-id fixture/principal
                        fixture/recorded-at
                        (nonapproval-request "reject" "tie-operation-b"))
        expected (if (pos? (compare (:review/operation-id first-receipt)
                                    (:review/operation-id second-receipt)))
                   first-receipt
                   second-receipt)]
    (t/is (= expected
             (split/effective-review-receipt
              fixture/digest manifest candidate-set split-id
              [first-receipt second-receipt])))
    (t/is (= expected
             (split/effective-review-receipt
              fixture/digest manifest candidate-set split-id
              [second-receipt first-receipt])))))

(defn- bulk-group-order-digest
  "Make receipt-id order disagree across splits while preserving integrity."
  [value]
  (let [text (str value)]
    (cond
      (str/includes? text "bulk-group-a/split-000") "zz-receipt-a-000"
      (str/includes? text "bulk-group-b/split-000") "aa-receipt-b-000"
      (str/includes? text "bulk-group-a/split-001") "aa-receipt-a-001"
      (str/includes? text "bulk-group-b/split-001") "zz-receipt-b-001"
      :else (fixture/digest value))))

(t/deftest exact-time-bulk-operation-groups-win-consistently-across-splits
  (let [manifest (split/split-manifest
                  bulk-group-order-digest
                  (assoc fixture/coordinates
                         :source-text fixture/source
                         :source-parts fixture/source-parts))
        claim (split/candidate-claim bulk-group-order-digest manifest
                                     "candidate-revision-bulk-order")
        candidates (mapv split/candidate-split
                         (repeat bulk-group-order-digest)
                         (:candidate-claim/members claim)
                         ["# Empieza aqui\n\n"
                          "Primer parrafo.\n\n"
                          "  Segundo parrafo.\n"])
        candidate-set (split/complete-candidate-set
                       bulk-group-order-digest manifest claim candidates)
        [split-0 split-1] (mapv :split/id
                                (take 2 (:split-manifest/splits manifest)))
        at "2026-08-30T12:00:00.000Z"
        approve-request (fn [operation-id]
                          (fixture/review-request
                           {:review/operation-id operation-id
                            :review/overall "approve"}))
        reject-request (fn [operation-id]
                         (nonapproval-request "reject" operation-id))
        receipt-for (fn [split-id operation-id request]
                      (split/review-receipt
                       bulk-group-order-digest manifest candidate-set split-id
                       fixture/principal at (request operation-id)))
        group-a-0 (receipt-for split-0 "bulk-group-a/split-000"
                               approve-request)
        group-b-0 (receipt-for split-0 "bulk-group-b/split-000"
                               reject-request)
        group-a-1 (receipt-for split-1 "bulk-group-a/split-001"
                               approve-request)
        group-b-1 (receipt-for split-1 "bulk-group-b/split-001"
                               reject-request)]
    (t/testing "receipt ids alone would select different bulk groups"
      (t/is (pos? (compare (:review/id group-a-0) (:review/id group-b-0))))
      (t/is (neg? (compare (:review/id group-a-1) (:review/id group-b-1)))))

    (t/testing "the lexically later operation group wins every split"
      (doseq [[split-id group-a group-b histories]
              [[split-0 group-a-0 group-b-0
                [[group-a-0 group-b-0] [group-b-0 group-a-0]]]
               [split-1 group-a-1 group-b-1
                [[group-b-1 group-a-1] [group-a-1 group-b-1]]]]
              history histories]
        (t/is (= group-b
                 (split/effective-review-receipt
                  bulk-group-order-digest manifest candidate-set split-id
                  history)))
        (t/is (pos? (compare (:review/operation-id group-b)
                             (:review/operation-id group-a))))))))

(t/deftest tampered-or-stale-evidence-cannot-donate-memory
  (let [{:keys [manifest candidate-claim candidate-set split-id]} (review-context)
        approval (fixture/review-receipt
                  manifest candidate-set split-id (fixture/review-request))
        changed-members (assoc (fixture/candidates candidate-claim) 0
                               (split/candidate-split
                                fixture/digest
                                (last (:candidate-claim/members candidate-claim))
                                "changed"))
        stale-set (split/complete-candidate-set
                   fixture/digest manifest candidate-claim changed-members)
        stale-review (fixture/review-receipt
                      manifest stale-set split-id (fixture/review-request))]
    (t/testing "another valid candidate set is still stale for this projection"
      (t/is (thrown-with-msg?
             js/Error
             #"review receipt identity"
             (split/approved-memory-example
              fixture/digest manifest candidate-set split-id [stale-review]))))

    (t/testing "scope, candidate bytes, and corrected bytes are authenticated"
      (t/is (thrown-with-msg?
             js/Error
             #"manifest identity"
             (split/approved-memory-example
              fixture/digest
              (assoc manifest :split-manifest/org-id "another-tenant")
              candidate-set split-id [approval])))
      (t/is (thrown-with-msg?
             js/Error
             #"candidate digest"
             (split/approved-memory-example
              fixture/digest manifest
              (update candidate-set :candidate-set/members
                      #(assoc % 1 (assoc (second %) :candidate/text "TAMPERED")))
              split-id [approval])))
      (t/is (thrown-with-msg?
             js/Error
             #"review receipt identity"
             (split/approved-memory-example
              fixture/digest manifest candidate-set split-id
              [(assoc approval :review/corrected-text "Fabricated correction.")]))))))
