(ns knoxx.backend.infra.routes.translation-review-test
  "Recording one approval.

  What is worth testing here is not that a row gets written. It is that an
  approval cannot be recorded against a translation that does not exist, does not
  match, belongs to another tenant, or has since been replaced — and that
  recording one publishes nothing."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.routes.translation-review :as facade]
            [knoxx.backend.infra.translation-evidence-store :as store]))

(def ^:private at "2026-08-22T10:00:00.000Z")

(def ^:private query-scope
  "The scope every read in this suite narrows by — the same one the facade uses."
  {:org-id "org-1" :project "knoxx-session"})

(def ^:private scope
  {:org-id "org-1"
   :project "knoxx-session"
   :principal {:principal/user-email "reviewer@open-hax.local"}})

(defn- receipt
  [& {:keys [org project garden locale revision translation-revision]
      :or {org "org-1"
           project "knoxx-session"
           garden :knoxx.docs/promethean
           locale :es
           revision "sha256-aaa111bbb222"
           translation-revision "sha256-aaa111bbb222+es@batch-1"}}]
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/garden garden
   :translation/source-locale :en
   :translation/locale locale
   :translation/source-revision revision
   :translation/revision translation-revision
   :translation/dispatch-key "key-1"
   :translation/org-id org
   :translation/project project
   :translation/at at})

(defn- request
  [& {:keys [locale revision translation-revision document garden]
      :or {document :knoxx.docs/probe
           garden :knoxx.docs/promethean
           locale :es
           revision "sha256-aaa111bbb222"
           translation-revision "sha256-aaa111bbb222+es@batch-1"}}]
  {:review/document document
   :review/garden garden
   :review/locale locale
   :review/revision revision
   :review/translation-revision translation-revision})

(defn- ^:async store-with!
  [receipts]
  (let [evidence-store (store/memory-store)]
    (doseq [r receipts]
      (await (store/record-translation! evidence-store r)))
    {:evidence-store evidence-store
     :clock (constantly at)
     :publication-index
     {:publications [{:publication/id :knoxx.publications/probe-es
                      :publication/document :knoxx.docs/probe
                      :publication/garden :knoxx.docs/promethean
                      :publication/locale :es}]}}))

(deftest ^:async an-authorized-approval-is-recorded-with-attribution
  (let [deps (await (store-with! [(receipt)]))
        result (await (facade/approve-translation! deps scope (request)))
        approval (:approval result)]
    (testing "the approval is recorded"
      (is (= :recorded (:approval/status result))))

    (testing "identity comes from the receipt, not the request"
      (is (= :knoxx.docs/probe (:review/document approval)))
      (is (= :knoxx.docs/promethean (:review/garden approval)))
      (is (= :es (:review/locale approval)))
      (is (= "sha256-aaa111bbb222" (:review/revision approval)))
      (is (= "sha256-aaa111bbb222+es@batch-1" (:review/translation-revision approval))))

    (testing "the tenant and project are inherited from the evidence"
      ;; The request never carries them, so they cannot be forged.
      (is (= "org-1" (:review/org-id approval)))
      (is (= "knoxx-session" (:review/project approval))))

    (testing "the acting principal and the clock are attributed by the server"
      (is (= "reviewer@open-hax.local"
             (:principal/user-email (:review/principal approval))))
      (is (= at (:review/at approval))))))

(deftest ^:async approving-twice-is-the-same-fact-not-two
  (let [deps (await (store-with! [(receipt)]))
        first-result (await (facade/approve-translation! deps scope (request)))
        second-result (await (facade/approve-translation! deps scope (request)))]
    (testing "the second approval is recognized as the one already recorded"
      (is (= :recorded (:approval/status first-result)))
      (is (= :existing (:approval/status second-result)))
      (is (= (:approval first-result) (:approval second-result))))

    (testing "only one approval exists"
      ;; Appending would make `approved?` depend on how many times a button was
      ;; clicked.
      (is (= 1 (count (await (store/approvals! (:evidence-store deps) query-scope))))))))

(deftest ^:async an-approval-with-no-translation-behind-it-is-refused
  (let [deps (await (store-with! []))
        result (await (facade/approve-translation! deps scope (request)))]
    (testing "there is nothing to approve, and the refusal says so"
      (is (= :translation-receipt-missing (:refusal/type (:approval/refusal result)))))

    (testing "nothing was persisted"
      ;; A rejected approval must leave no trace a later read could mistake for
      ;; evidence.
      (is (empty? (await (store/approvals! (:evidence-store deps) query-scope)))))))

(deftest ^:async a-mismatched-approval-is-refused-with-both-sides-named
  (let [deps (await (store-with! [(receipt)]))]
    (testing "a different target locale has no receipt of its own"
      (is (= :translation-receipt-missing
             (:refusal/type (:approval/refusal
                             (await (facade/approve-translation!
                                     deps scope (request :locale :fr))))))))

    (testing "naming a translation output that is not the current one is refused"
      (let [refusal (:approval/refusal
                     (await (facade/approve-translation!
                             deps scope
                             (request :translation-revision "sha256-aaa111bbb222+es@batch-9"))))]
        (is (= :translation-revision-mismatch (:refusal/type refusal)))
        (is (= "sha256-aaa111bbb222+es@batch-9" (:refusal/requested refusal)))
        (is (= "sha256-aaa111bbb222+es@batch-1" (:refusal/recorded refusal))
            "both sides travel, so a reviewer can see which was stale")))

    (testing "nothing was persisted by any refusal"
      (is (empty? (await (store/approvals! (:evidence-store deps) query-scope)))))))

(deftest ^:async a-reviewer-cannot-approve-another-tenants-translation
  (let [deps (await (store-with! [(receipt :org "org-other")]))
        result (await (facade/approve-translation! deps scope (request)))]
    (testing "the other tenant's receipt is invisible, so there is nothing to approve"
      (is (= :translation-receipt-missing (:refusal/type (:approval/refusal result)))))

    (testing "nothing was persisted"
      (is (empty? (await (store/approvals! (:evidence-store deps) query-scope)))))))

(deftest ^:async a-reviewer-cannot-approve-another-projects-translation
  (let [deps (await (store-with! [(receipt :project "other-project")]))
        result (await (facade/approve-translation! deps scope (request)))]
    (testing "project scope is enforced like tenant scope"
      (is (= :translation-receipt-missing (:refusal/type (:approval/refusal result)))))))

(deftest ^:async a-selector-revision-cannot-be-approved
  (let [deps (await (store-with! [(receipt)]))]
    (testing "the wire's dangerous string shape is refused by contract"
      ;; A revision arrives as decoded wire input here, so the string form of the
      ;; selector is the one that has to be caught.
      (is (thrown? js/Error
                   (await (facade/approve-translation!
                           deps scope (request :revision "source/current")))))
      (is (thrown? js/Error
                   (await (facade/approve-translation!
                           deps scope
                           (request :translation-revision "source/current"))))))))

(deftest ^:async approval-materializes-nothing
  (let [deps (await (store-with! [(receipt)]))
        _ (await (facade/approve-translation! deps scope (request)))]
    (testing "an approval may make a plan admissible but must not publish"
      ;; The card is explicit: recording review evidence and materializing
      ;; content are separate acts, so a reviewer can accept a translation
      ;; without also deciding when the bytes go live.
      (is (= 1 (count (await (store/approvals! (:evidence-store deps) query-scope)))))
      (is (= 1 (count (await (store/completed-translations!
                              (:evidence-store deps) query-scope)))))
      (is (empty? (filter #(= :publication/materialized (:receipt/type %))
                          (await (store/completed-translations!
                                  (:evidence-store deps) query-scope))))))))

(deftest ^:async review-list-exposes-the-exact-revisions-the-approval-needs
  (let [deps (await (store-with! [(receipt)]))
        before (first (:reviews (await (facade/reviewable-translations! deps scope))))]
    (is (= :knoxx.docs/probe (:document before)))
    (is (= :knoxx.publications/probe-es (:publication before)))
    (is (= :knoxx.docs/promethean (:garden before)))
    (is (= :es (:locale before)))
    (is (= "sha256-aaa111bbb222" (:revision before)))
    (is (= "sha256-aaa111bbb222+es@batch-1" (:translation_revision before)))
    (is (false? (:approved before)))
    (await (facade/approve-translation! deps scope (request)))
    (let [after (first (:reviews (await (facade/reviewable-translations! deps scope))))]
      (is (true? (:approved after)))
      (is (= at (:approved_at after))))))
