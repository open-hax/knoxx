(ns knoxx.backend.openplanner-translation-review-scope-test
  "Regression laws for the legacy OpenPlanner review bridge.

  A CMS review card identifies one organization/project/garden relation.  The
  legacy compatibility path must preserve all three coordinates, including the
  old rows whose garden is nil, or a review can silently reach another card."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.openplanner-translation-mongo.documents :as documents]
            [knoxx.backend.extern.openplanner-translation-mongo.labels :as labels]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.routes.translation :as translation]
            [knoxx.backend.law.openplanner-translation :as contract]
            [malli.core :as m]))

(def legacy-segment-id "64b000000000000000000001")

(defn- request
  [body route-params]
  (js-obj "body" (clj->js body)
          "params" (clj->js route-params)))

(def route-context
  {:user-id "reviewer-1"
   :user-email "reviewer@example.test"
   :org-id "org-1"})

(def route-handlers
  {:ctx-user-id :user-id
   :ctx-user-email :user-email
   :ctx-org-id :org-id})

(deftest document-review-selector-keeps-the-complete-card-scope
  (doseq [garden [nil "garden-1"]]
    (testing (str "garden " (pr-str garden) " is an exact coordinate")
      (let [selector (#'documents/exact-document-selector
                      "doc-1" "es"
                      {:org_id "org-1"
                       :project "review-stage"
                       :garden_id garden})]
        (is (= {:document_id "doc-1"
                :target_lang "es"
                :org_id "org-1"
                :project "review-stage"
                :garden_id garden}
               selector))
        (is (contains? selector :garden_id))))))

(deftest segment-label-selector-keeps-the-complete-card-scope
  (doseq [garden [nil "garden-1"]]
    (testing (str "garden " (pr-str garden) " remains present in Mongo selector")
      (let [selector (#'labels/review-selector
                      legacy-segment-id "org-1"
                      {:project "review-stage"
                       :garden_id garden})]
        (is (= "org-1" (aget selector "org_id")))
        (is (= "review-stage" (aget selector "project")))
        (is (= garden (aget selector "garden_id")))
        (is (.hasOwnProperty selector "garden_id"))))))

(deftest review-contracts-require-a-project-coordinate
  (let [label {:adequacy "good"
               :fluency "good"
               :terminology "correct"
               :risk "safe"
               :overall "approve"
               :org_id "org-1"}
        document {:overall "approve"
                  :org_id "org-1"}]
    (is (false? (m/validate contract/LabelTranslationSegmentRequest label)))
    (is (true? (m/validate contract/LabelTranslationSegmentRequest
                           (assoc label :project "review-stage"))))
    (is (false? (m/validate contract/ReviewTranslationDocumentRequest document)))
    (is (true? (m/validate contract/ReviewTranslationDocumentRequest
                           (assoc document :project "review-stage"))))))

(deftest route-operations-inject-the-server-project-into-review-writes
  (let [config {:session-project-name "review-stage"}
        label-call (atom nil)
        document-call (atom nil)
        label-op (#'translation/label-segment-op config)
        document-op (#'translation/review-document-op config)]
    (with-redefs [openplanner-client/client (constantly ::client)
                  openplanner-client/label-translation-segment!
                  (fn [client segment-id payload]
                    (reset! label-call [client segment-id payload])
                    ::labelled)
                  openplanner-client/review-translation-document!
                  (fn [client document-id target-lang payload]
                    (reset! document-call
                            [client document-id target-lang payload])
                    ::reviewed)]
      (is (= ::labelled
             (label-op
              (request {:adequacy "good"
                        :fluency "good"
                        :terminology "correct"
                        :risk "safe"
                        :overall "approve"
                        :garden_id nil}
                       {:id legacy-segment-id})
              route-context route-handlers)))
      (is (= [::client legacy-segment-id
              {:adequacy "good"
               :fluency "good"
               :terminology "correct"
               :risk "safe"
               :overall "approve"
               :garden_id nil
               :labeler_id "reviewer-1"
               :labeler_email "reviewer@example.test"
               :org_id "org-1"
               :project "review-stage"}]
             @label-call))

      (is (= ::reviewed
             (document-op
              (request {:overall "reject" :garden_id nil}
                       {:documentId "doc-1" :targetLang "es"})
              route-context route-handlers)))
      (is (= [::client "doc-1" "es"
              {:overall "reject"
               :garden_id nil
               :labeler_id "reviewer-1"
               :labeler_email "reviewer@example.test"
               :org_id "org-1"
               :project "review-stage"}]
             @document-call)))))
