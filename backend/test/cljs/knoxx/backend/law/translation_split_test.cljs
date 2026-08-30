(ns knoxx.backend.law.translation-split-test
  (:require [cljs.test :as t]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-fixture :as fixture]))

(t/deftest manifest-admits-logical-source-parts-with-exact-spans
  (let [value (fixture/manifest)]
    (t/testing "logical source parts are admitted explicitly, not invented by the UI"
      (t/is (= fixture/source (split/source-text value)))
      (t/is (= fixture/source-parts
             (mapv :split/source-text (:split-manifest/splits value))))
      (t/is (= [[0 14] [14 32] [32 52]]
             (mapv (juxt :split/source-start :split/source-end)
                   (:split-manifest/splits value)))))

    (t/testing "equal revision and parts reproduce the same manifest"
      (t/is (= value (fixture/manifest))))

    (t/testing "another source revision cannot inherit old split identities"
      (t/is (not= (:split-manifest/id value)
                (:split-manifest/id
                 (split/split-manifest
                  fixture/digest
                  (assoc fixture/coordinates
                         :source-revision "sha256-source-v2"
                         :source-text fixture/source
                         :source-parts fixture/source-parts))))))))

(t/deftest manifest-refuses-lossy-or-phantom-source-parts
  (t/testing "the admitted vector must reconstruct the exact source"
    (t/is (thrown-with-msg?
         js/Error
         #"reconstruct"
         (split/split-manifest
          fixture/digest
          (assoc fixture/coordinates
                 :source-text "one\n\ntwo"
                 :source-parts ["one" "two"])))))

  (t/testing "a trailing separator stays in a real member instead of becoming an empty split"
    (let [value (split/split-manifest
                 fixture/digest
                 (assoc fixture/coordinates
                        :source-text "one\n\n"
                        :source-parts ["one\n\n"]))]
      (t/is (= ["one\n\n"]
             (mapv :split/source-text (:split-manifest/splits value))))
      (t/is (thrown-with-msg?
           js/Error
           #"nonblank"
           (split/split-manifest
            fixture/digest
            (assoc fixture/coordinates
                   :source-text "one\n\n"
                   :source-parts ["one\n\n" ""]))))))

  (t/testing "a moving source selector is not a concrete manifest revision"
    (t/is (thrown-with-msg?
           js/Error
           #"invalid manifest"
           (split/split-manifest
            fixture/digest
            (assoc fixture/coordinates
                   :source-revision "source/current"
                   :source-text fixture/source
                   :source-parts fixture/source-parts))))))

(t/deftest qualified-manifest-coordinates-are-canonical-input
  (let [unqualified (fixture/manifest)
        qualified-input
        (-> fixture/coordinates
            (dissoc :org-id :project :garden :document :source-locale
                    :target-locale :source-revision)
            (assoc :split-manifest/org-id (:org-id fixture/coordinates)
                   :split-manifest/project (:project fixture/coordinates)
                   :split-manifest/garden (:garden fixture/coordinates)
                   :split-manifest/document (:document fixture/coordinates)
                   :split-manifest/source-locale (:source-locale fixture/coordinates)
                   :split-manifest/target-locale (:target-locale fixture/coordinates)
                   :split-manifest/source-revision
                   (:source-revision fixture/coordinates)
                   :source-text fixture/source
                   :source-parts fixture/source-parts))]
    (t/is (= unqualified (split/split-manifest fixture/digest qualified-input)))))

(t/deftest candidate-attempts-come-from-the-pre-provider-claim
  (let [value (fixture/manifest)
        candidate-claim (fixture/claim value)
        member (first (:candidate-claim/members candidate-claim))
        candidate (split/candidate-split fixture/digest member "translation")]
    (t/testing "the candidate copies the admitted attempt identity"
      (t/is (= (:candidate-claim-member/attempt-id member)
             (:candidate/attempt-id candidate))))

    (t/testing "substituting an attempt id is refused at completion"
      (let [members (fixture/candidates candidate-claim)]
        (t/is (thrown-with-msg?
             js/Error
             #"attempt"
             (split/complete-candidate-set
              fixture/digest value candidate-claim
              (assoc members 0
                     (assoc (first members)
                            :candidate/attempt-id "minted-at-save-time")))))))

    (t/testing "empty or whitespace-only targets are inadmissible candidates"
      (t/is (thrown-with-msg?
             js/Error
             #"invalid candidate"
             (split/candidate-split fixture/digest member "  "))))))

(t/deftest complete-candidate-set-follows-claim-order
  (let [value (fixture/manifest)
        candidate-claim (fixture/claim value)
        complete-set (fixture/candidate-set value candidate-claim)]
    (t/testing "out-of-order provider answers compose in pre-admitted order"
      (t/is (= "# Empieza aquí\n\nPrimer párrafo.\n\n  Segundo párrafo.\n"
             (:candidate-set/text complete-set)))
      (t/is (= [0 1 2]
             (mapv :candidate/split-index (:candidate-set/members complete-set)))))

    (t/testing "the completed set binds the exact manifest and turn claim"
      (t/is (= (:split-manifest/id value) (:candidate-set/manifest-id complete-set)))
      (t/is (= (:candidate-claim/id candidate-claim)
             (:candidate-set/claim-id complete-set))))))

(t/deftest incomplete-drifted-or-forged-candidates-are-refused
  (let [value (fixture/manifest)
        candidate-claim (fixture/claim value)
        members (fixture/candidates candidate-claim)]
    (t/testing "one saved split is not document completion"
      (t/is (thrown-with-msg?
           js/Error
           #"does not cover"
           (split/complete-candidate-set
            fixture/digest value candidate-claim [(first members)]))))

    (t/testing "changed bytes cannot retain an old candidate digest"
      (t/is (thrown-with-msg?
           js/Error
           #"candidate digest"
           (split/complete-candidate-set
            fixture/digest value candidate-claim
            (assoc members 0 (assoc (first members) :candidate/text "poison"))))))

    (t/testing "legitimate changed bytes produce another candidate-set identity"
      (let [original (fixture/candidate-set value candidate-claim)
            changed (split/complete-candidate-set
                     fixture/digest value candidate-claim
                     (assoc members 0
                            (split/candidate-split
                             fixture/digest
                             (last (:candidate-claim/members candidate-claim))
                             "changed")))]
        (t/is (not= (:candidate-set/id original) (:candidate-set/id changed)))))))
