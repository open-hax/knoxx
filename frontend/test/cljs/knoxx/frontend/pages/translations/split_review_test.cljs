(ns knoxx.frontend.pages.translations.split-review-test
  "Pure contract tests for resource-backed per-split review state."
  (:require [cljs.test :as t]
            [knoxx.frontend.pages.translations.logic :as logic]
            [knoxx.frontend.pages.translations.split-review :as split-review]))

(def ^:private aggregate
  {:candidate_set_id "candidate-set/7"
   :manifest_id "manifest/7"
   :status "partial-review"
   :splits [{:split_id "split/0"
             :split_index 0
             :source_text "First source"
             :candidate_text "Primer candidato"
             :review_status "approved"
             :review_id "review/2"
             :reviewed_at "2026-08-29T15:04:05.000Z"
             :overall "approve"
             :adequacy "adequate"
             :fluency "good"
             :terminology "minor_errors"
             :risk "sensitive"
             :corrected_text "Primer texto corregido"
             :editor_notes "First persisted note"
             :labels [{:id "review/2"
                       :review_id "review/2"
                       :segment_id "split/0"
                       :labeler_id "reviewers/alex"
                       :labeler_email "alex@example.test"
                       :ts "2026-08-29T15:04:05.000Z"
                       :reviewed_at "2026-08-29T15:04:05.000Z"
                       :review_status "approved"
                       :overall "approve"
                       :adequacy "adequate"
                       :fluency "good"
                       :terminology "minor_errors"
                       :risk "sensitive"
                       :corrected_text "Primer texto corregido"
                       :editor_notes "First persisted note"}
                      {:id "review/1"
                       :review_id "review/1"
                       :segment_id "split/0"
                       :labeler_id "reviewers/sam"
                       :labeler_email "sam@example.test"
                       :ts "2026-08-28T13:02:03.000Z"
                       :reviewed_at "2026-08-28T13:02:03.000Z"
                       :review_status "in-review"
                       :overall "needs_edit"
                       :adequacy "poor"
                       :fluency "adequate"
                       :terminology "major_errors"
                       :risk "safe"}]}
            {:split_id "split/1"
             :split_index 1
             :source_text "Second source"
             :candidate_text "Segundo candidato"
             :review_status "in-review"}
            {:split_id "split/2"
             :split_index 2
             :source_text "Third source"
             :candidate_text "Tercer candidato"
             :review_status nil}]})

(def ^:private row
  {:publication "publications/doc-es"
   :document "docs/doc"
   :garden "gardens/promethean"
   :source_locale "en"
   :locale "es"
   :title "Document"
   :revision "source-r"
   :reviewable true
   :approved false
   :split_review aggregate})

(t/deftest aggregate-projects-ordered-real-splits-and-progress
  (let [progress (split-review/progress row)
        detail (split-review/detail
                {:title "Document" :source_lang "en" :target_lang "es"
                 :split_review aggregate})]
    (t/is (= {:total 3 :approved 1 :in-review 1 :rejected 0
              :pending 1 :reviewed 2 :all-approved? false}
             progress))
    (t/is (= ["split/0" "split/1" "split/2"]
             (mapv :split_id (:segments detail))))
    (t/is (= [0 1 2] (mapv :segment_index (:segments detail))))
    (t/is (= "Primer candidato"
             (get-in detail [:segments 0 :candidate_text])))
    (t/is (= "Primer texto corregido"
             (get-in detail [:segments 0 :corrected_text])))
    (t/is (= "review/2" (get-in detail [:segments 0 :review_id]))
          "the latest immutable review identity survives projection")
    (t/is (= (get-in aggregate [:splits 0 :labels])
             (get-in detail [:segments 0 :labels]))
          "immutable labels remain ordered and intact through projection")
    (t/is (= 2 (get-in detail [:segments 0 :label_count])))
    (t/is (= {:adequacy "adequate"
              :fluency "good"
              :terminology "minor_errors"
              :risk "sensitive"
              :editor_notes "First persisted note"}
             (select-keys (first (:segments detail))
                          [:adequacy :fluency :terminology :risk
                           :editor_notes])))
    (t/is (= "in_review" (get-in detail [:segments 1 :status])))
    (t/is (= {:total_segments 3 :approved 1
              :overall_status "partial_review"}
             (:summary detail)))))

(t/deftest request-payload-is-closed-byte-preserving-and-candidate-bound
  (let [split (first (:segments (split-review/detail
                                 {:source_lang "en" :target_lang "es"
                                  :split_review aggregate})))]
    (t/is (= {:candidate_set_id "candidate-set/7"
              :split_id "split/0"
              :status "approved"
              :adequacy "poor"
              :fluency "excellent"
              :terminology "major_errors"
              :risk "policy_violation"
              :corrected_text "  Texto final  \n\n"
              :editor_notes "Needs domain review"}
             (split-review/review-payload row split :approved
                                          {:adequacy "poor"
                                           :fluency "excellent"
                                           :terminology "major_errors"
                                           :risk "policy_violation"
                                           :corrected_text "  Texto final  \n\n"
                                           :editor_notes
                                           "  Needs domain review  "})))
    (t/is (= {:candidate_set_id "candidate-set/7"
              :split_id "split/0"
              :status "rejected"
              :adequacy "good"
              :fluency "good"
              :terminology "correct"
              :risk "safe"}
             (split-review/review-payload
              row split "rejected" (split-review/review-form {}))))
    (t/is (thrown-with-msg?
           js/Error #"request is incomplete"
           (split-review/review-payload row split "approve"
                                        (split-review/review-form {}))))
    (t/is (thrown-with-msg?
           js/Error #"evaluations are incomplete or invalid"
           (split-review/review-payload
            row split "approved"
            (assoc (split-review/review-form {}) :risk "unknown"))))))

(t/deftest review-form-is-split-local-and-all-approved-is-exact
  (let [detail (split-review/detail
                {:source_lang "en" :target_lang "es" :split_review aggregate})
        splits (:segments detail)
        approved-row (assoc-in row [:split_review :splits]
                               (mapv #(assoc % :review_status "approved")
                                     (:splits aggregate)))]
    (t/is (= {:adequacy "adequate"
              :fluency "good"
              :terminology "minor_errors"
              :risk "sensitive"
              :corrected_text "Primer texto corregido"
              :editor_notes "First persisted note"}
             (split-review/review-form (first splits))))
    (t/is (= {:adequacy "good"
              :fluency "good"
              :terminology "correct"
              :risk "safe"
              :corrected_text ""
              :editor_notes ""}
             (split-review/review-form (second splits))))
    (t/is (not (split-review/all-approved? row)))
    (t/is (split-review/all-approved? approved-row))))

(t/deftest skip-advances-in-manifest-order-without-wrapping
  (let [segments (:segments
                  (split-review/detail
                   {:source_lang "en" :target_lang "es"
                    :split_review aggregate}))]
    (t/is (= 1 (split-review/next-split-index segments 0)))
    (t/is (= 2 (split-review/next-split-index segments 1)))
    (t/is (nil? (split-review/next-split-index segments 2)))
    (t/is (nil? (split-review/next-split-index segments nil)))))

(t/deftest bulk-review-carries-evaluation-but-never-correction-or-split-ids
  (t/is (= {:candidate_set_id "candidate-set/7"
            :status "in-review"
            :adequacy "poor"
            :fluency "adequate"
            :terminology "major_errors"
            :risk "sensitive"
            :editor_notes "Review the whole set"}
           (split-review/bulk-review-payload
            row "in-review"
            {:adequacy "poor"
             :fluency "adequate"
             :terminology "major_errors"
             :risk "sensitive"
             :corrected_text "Must stay split-local"
             :editor_notes "  Review the whole set  "}))))

(t/deftest split-review-evidence-decorates-without-changing-resource-cardinality
  (let [work-items
        (mapv (fn [index]
                (cond-> {:publication (str "publications/doc-" index "-es")
                         :document (str "docs/doc-" index)
                         :garden "gardens/promethean"
                         :source_locale "en"
                         :locale "es"
                         :title (str "Document " index)
                         :revision (str "source-r-" index)
                         :work_state (if (zero? index) "ready" "missing")
                         :reviewable (zero? index)
                         :approved false
                         :allowed_actions (if (zero? index) [] ["dispatch"])}
                  (zero? index) (assoc :split_review aggregate)))
              (range 18))
        rows (logic/normalize-work-inventory [] work-items)
        reviewed (first rows)]
    (t/is (= 18 (count rows)))
    (t/is (= 18 (count (set (map logic/work-row-id rows)))))
    (t/is (= 3 (:total_segments reviewed)))
    (t/is (= 1 (:approved reviewed)))
    (t/is (= "partial_review" (:overall_status reviewed)))
    (t/is (:contract_content reviewed))
    (t/is (not (logic/legacy-candidate? reviewed)))))
