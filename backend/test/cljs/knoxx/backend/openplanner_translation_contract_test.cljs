(ns knoxx.backend.openplanner-translation-contract-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [knoxx.backend.infra.openplanner.translation-scope :as translation-scope]
            [knoxx.backend.law.openplanner-translation :as contract]
            [malli.core :as m]))

(deftest tenant-scope-is-required-at-the-contract-boundary
  (testing "direct storage request contracts reject missing org_id"
    (doseq [schema [contract/TenantScopeRequest
                    contract/TranslationSegmentsRequest
                    contract/LabelTranslationSegmentRequest
                    contract/TranslationManifestRequest
                    contract/TranslationSftRequest
                    contract/CreateTranslationSegmentsBatchRequest
                    contract/TranslationDocumentsRequest
                    contract/ReviewTranslationDocumentRequest
                    contract/CreateTranslationBatchRequest
                    contract/TranslationBatchesRequest
                    contract/UpdateTranslationBatchRequest]]
      (is (false? (m/validate schema {}))))))

(deftest manifest-requires-a-scoped-map
  (testing "the legacy bare-project string cannot bypass organization scope"
    (is (false? (m/validate contract/TranslationManifestRequest "project-only")))
    (is (true? (m/validate contract/TranslationManifestRequest
                           {:project "knoxx" :org_id "org-1"})))))

(deftest label-response-advertises-top-level-id
  (testing "frontend-compatible label responses include label_id"
    (is (false? (m/validate contract/LabelTranslationSegmentResponse
                            {:ok true
                             :label {:id "label-1"}
                             :new_status "approved"})))
    (is (true? (m/validate contract/LabelTranslationSegmentResponse
                           {:ok true
                            :label_id "label-1"
                            :label {:id "label-1"}
                            :new_status "approved"})))))

(deftest batch-creation-binds-membership
  (let [base {:garden_id "garden-1"
              :target_lang "es"
              :document_ids ["doc-1"]
              :org_id "org-1"}]
    (is (false? (m/validate contract/CreateTranslationBatchRequest base)))
    (is (true? (m/validate contract/CreateTranslationBatchRequest
                           (assoc base :membership_id "membership-1"))))))

(deftest pagination-falls-back-and-clamps
  (testing "malformed query values never become NaN"
    (is (= 50 (common/normalized-query-number "not-a-number" 50 1 100)))
    (is (= 1 (common/normalized-query-number -20 50 1 100)))
    (is (= 100 (common/normalized-query-number 200 50 1 100)))
    (is (= 4 (common/normalized-query-number 4.9 50 1 100)))))

(deftest segment-change-detection-is-content-based
  (let [row #js {"source_text" "Hello"
                 "translated_text" "Hola"
                 "status" "pending"}
        unchanged {:source_text "Hello"
                   :translated_text "Hola"
                   :status "pending"}
        changed (assoc unchanged :translated_text "Buenas")]
    (is (true? (common/segment-doc-matches? row unchanged)))
    (is (false? (common/segment-doc-matches? row changed)))))

(deftest managed-translation-scope-is-fail-closed
  (testing "ordinary memberships remain in their current organization"
    (is (= "org-1"
           (translation-scope/translation-org-id!
            {:orgId "org-1" :roleSlugs ["org_admin"]}
            {:org_id "org-1"})))
    (is (thrown-with-msg?
         js/Error
         #"cannot target another organization"
         (translation-scope/translation-org-id!
          {:orgId "org-1" :roleSlugs ["org_admin"]}
          {:org_id "org-2"}))))
  (testing "system admins may carry an explicit legacy batch organization"
    (is (= "org-2"
           (translation-scope/translation-org-id!
            {:orgId "primary" :roleSlugs ["system_admin"]}
            {:org_id "org-2"}))))
  (testing "a target organization is always required"
    (is (thrown-with-msg?
         js/Error
         #"organization is required"
         (translation-scope/translation-org-id! {} {}))))
  (testing "non-string authorization values never become tenant identifiers"
    (is (thrown-with-msg?
         js/Error
         #"contract violation"
         (translation-scope/translation-org-id! {:orgId false} {})))
    (is (thrown-with-msg?
         js/Error
         #"contract violation"
         (translation-scope/translation-org-id! {:orgId "org-1"} {:org_id 42})))))

(deftest batch-views-keep-the-owning-membership-private
  (let [row #js {"_id" "batch-row-1"
                 "batch_id" "batch-1"
                 "org_id" "org-1"
                 "membership_id" "membership-1"
                 "status" "queued"}]
    (testing "tenant-facing batch responses omit the creator's membership"
      (let [view (common/batch-view row)]
        (is (= "org-1" (:org_id view)))
        (is (not (contains? view :membership_id)))))
    (testing "the worker claim path still carries the owning membership"
      (is (= "membership-1" (:membership_id (common/worker-batch-view row)))))
    (testing "a missing row yields no view on either path"
      (is (nil? (common/batch-view nil)))
      (is (nil? (common/worker-batch-view nil))))))

(deftest batch-claim-scope-gates-the-membership-projection
  (testing "the claim scope carries an explicit membership projection flag"
    (is (true? (m/validate contract/NextTranslationBatchScope
                           {:org_id "org-1" :include_membership true})))
    (is (true? (m/validate contract/NextTranslationBatchScope {:org_id "org-1"})))
    (is (false? (m/validate contract/NextTranslationBatchScope {})))
    (is (false? (m/validate contract/NextTranslationBatchScope
                            {:org_id "org-1" :include_membership "yes"})))))