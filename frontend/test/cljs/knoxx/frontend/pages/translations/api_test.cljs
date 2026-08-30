(ns knoxx.frontend.pages.translations.api-test
  "Wire-shape tests for project/garden scoped legacy translation calls."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.api :as http]
            [knoxx.frontend.pages.translations.api :as api]))

(def calls (atom []))
(def ^:private original-request http/request)

(t/use-fixtures
 :each
 {:before (fn []
            (reset! calls [])
            (set! http/request
                  (fn
                    ([path]
                     (swap! calls conj [path nil])
                     {:ok true})
                    ([path opts]
                     (swap! calls conj [path opts])
                     {:ok true}))))
  :after (fn [] (set! http/request original-request))})

(t/deftest document-detail-encodes-project-and-garden-query
  (api/get-document "docs/a b" "pt-BR"
                    {:project "knoxx-session" :garden-id "gardens/sonic"})
  (t/is (= [["/api/translations/documents/docs%2Fa%20b/pt-BR?project=knoxx-session&garden_id=gardens%2Fsonic"
             nil]]
           @calls)))

(t/deftest document-and-segment-review-carry-row-scope-in-body
  (api/review-document "docs/a" "es"
                       {:project "knoxx-session" :garden-id "gardens/sonic"}
                       {:overall "reject"})
  (api/submit-label "segment/1"
                    {:project "knoxx-session" :garden-id "gardens/sonic"}
                    {:overall "needs_edit" :adequacy "poor"})
  (t/is (= [["/api/translations/documents/docs%2Fa/es/review"
             {:method "POST"
              :body {:overall "reject"
                     :project "knoxx-session"
                     :garden_id "gardens/sonic"}}]
            ["/api/translations/segments/segment%2F1/labels"
             {:method "POST"
              :body {:overall "needs_edit"
                     :adequacy "poor"
                     :project "knoxx-session"
                     :garden_id "gardens/sonic"}}]]
           @calls)))

(t/deftest publication-split-review-uses-candidate-bound-endpoint
  (api/submit-publication-split-review
   {:candidate_set_id "candidate-set/1"
    :split_id "split/1"
    :status "approved"
    :adequacy "excellent"
    :fluency "good"
    :terminology "minor_errors"
    :risk "sensitive"
    :corrected_text "Texto corregido"
    :editor_notes "Checked against glossary"})
  (t/is (= [["/api/publications/translations/reviews"
             {:method "POST"
              :body {:candidate_set_id "candidate-set/1"
                     :split_id "split/1"
                     :status "approved"
                     :adequacy "excellent"
                     :fluency "good"
                     :terminology "minor_errors"
                     :risk "sensitive"
                     :corrected_text "Texto corregido"
                     :editor_notes "Checked against glossary"}}]]
           @calls)))

(t/deftest publication-bulk-review-delegates-split-enumeration-to-the-server
  (api/submit-publication-bulk-review
   {:candidate_set_id "candidate-set/1"
    :status "in-review"
    :adequacy "adequate"
    :fluency "poor"
    :terminology "minor_errors"
    :risk "sensitive"
    :editor_notes "Apply this evaluation to the set"})
  (t/is (= [["/api/publications/translations/reviews/bulk"
             {:method "POST"
              :body {:candidate_set_id "candidate-set/1"
                     :status "in-review"
                     :adequacy "adequate"
                     :fluency "poor"
                     :terminology "minor_errors"
                     :risk "sensitive"
                     :editor_notes "Apply this evaluation to the set"}}]]
           @calls)
      "the client sends no split ids or document-level correction"))

(t/deftest publication-dispatch-carries-the-exact-publication-selector
  (api/dispatch-publication-translation
   "open-hax.publications/promethean-start-here-es")
  (t/is (= [["/api/publications/translations/dispatch"
             {:method "POST"
              :body {:publication
                     "open-hax.publications/promethean-start-here-es"}}]]
           @calls)
      "the transport cannot regress from one selected publication to a corpus dispatch"))
