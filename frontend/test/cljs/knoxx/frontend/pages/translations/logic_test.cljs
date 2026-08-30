(ns knoxx.frontend.pages.translations.logic-test
  "Written FIRST (TDD) — pure-logic contract for the Helix port of
  src/pages/TranslationReviewPage.tsx."
  (:require [cljs.test :as t]
            [clojure.string :as str]
            [knoxx.frontend.pages.translations.logic :as logic]))

(defn- desired-work
  [index work-state]
  {:publication (str "publications/doc-" index "-es")
   :document (str "docs/doc-" index)
   :garden "gardens/promethean"
   :source_locale "en"
   :locale "es"
   :revision (str "source-sha-" index)
   :title (str "Document " index)
   :work_state work-state
   :allowed_actions (case work-state
                      "missing" ["dispatch"]
                      "failed" ["retry"]
                      [])})

(defn- observed-resource-work
  []
  (mapv (fn [index]
          (if (zero? index)
            (assoc (desired-work index "ready")
                   :candidate {:translation_revision "candidate-sha-0"
                               :translated_at "2026-08-30T12:00:00.000Z"
                               :content_source "agent"
                               :source_text "Source zero"
                               :translated_text "Destino cero"}
                   :approved false)
            (desired-work index
                          (case (mod index 3)
                            0 "missing"
                            1 "queued"
                            "failed"))))
        (range 18)))

(t/deftest lang-name-lookup
  (t/is (= "Español" (logic/lang-name "es")))
  (t/is (= "English" (logic/lang-name "en")))
  (t/is (= "xx" (logic/lang-name "xx")) "unknown codes pass through"))

(t/deftest default-label-shape
  (t/is (= {:adequacy "good" :fluency "good" :terminology "correct"
          :risk "safe" :overall "approve" :corrected_text "" :editor_notes ""}
         logic/default-label)))

(t/deftest status-badge-data
  (t/is (str/includes? (logic/status-class "approved") "emerald"))
  (t/is (str/includes? (logic/status-class "fully_rejected") "rose"))
  (t/is (str/includes? (logic/status-class "mixed") "purple"))
  (t/is (str/includes? (logic/status-class "failed") "rose"))
  (t/is (str/includes? (logic/status-class "in_flight") "blue"))
  (t/is (str/includes? (logic/status-class "published") "emerald"))
  (t/is (= (logic/status-class "pending") (logic/status-class "unknown-status"))
      "unknown statuses fall back to pending styling")
  (t/is (= "✅" (logic/status-icon "approved")))
  (t/is (= "🔄" (logic/status-icon "running")))
  (t/is (= "⏳" (logic/status-icon "whatever")))
  (t/is (= "pending review" (logic/status-label "pending_review"))))

(t/deftest progress-pct-contract
  (t/is (= 50 (logic/progress-pct 1 2)))
  (t/is (= 0 (logic/progress-pct 0 0)) "no segments → 0, not NaN"))

(t/deftest field-options-per-field
  (t/is (= ["safe" "sensitive" "policy_violation"] (logic/field-options :risk)))
  (t/is (= ["correct" "minor_errors" "major_errors"] (logic/field-options :terminology)))
  (t/is (= ["excellent" "good" "adequate" "poor" "unusable"] (logic/field-options :adequacy)))
  (t/is (= (logic/field-options :adequacy) (logic/field-options :fluency))))

(t/deftest prepare-label-payload-trims-and-omits
  (t/testing "blank corrected/notes are omitted entirely"
    (let [payload (logic/prepare-label-payload logic/default-label "approve")]
      (t/is (= "approve" (:overall payload)))
      (t/is (not (contains? payload :corrected_text)))
      (t/is (not (contains? payload :editor_notes)))))
  (t/testing "non-blank values are trimmed and kept; overall overridden"
    (let [payload (logic/prepare-label-payload
                   (assoc logic/default-label
                          :corrected_text "  fixed  "
                          :editor_notes "note"
                          :overall "approve")
                   "reject")]
      (t/is (= "reject" (:overall payload)))
      (t/is (= "fixed" (:corrected_text payload)))
      (t/is (= "note" (:editor_notes payload)))
      (t/is (= "good" (:adequacy payload)) "scores carried through"))))

(t/deftest find-segment-by-index
  (let [detail {:segments [{:segment_index 0 :id "a"} {:segment_index 3 :id "b"}]}]
    (t/is (= "b" (:id (logic/find-segment detail 3))))
    (t/is (nil? (logic/find-segment detail 1)))
    (t/is (nil? (logic/find-segment nil 0)))
    (t/is (nil? (logic/find-segment detail nil)))))

(t/deftest available-langs-from-manifest
  (t/is (= ["es" "de" "ko" "fr" "ja" "zh" "it" "pt" "ru"]
         (logic/available-langs nil))
      "fallback list without a manifest")
  (t/is (= #{"es" "fr"}
         (set (logic/available-langs {:languages {:es {:total_segments 1} :fr {:total_segments 2}}})))
      "manifest keys win (keywordized json)"))

(t/deftest selection-preservation
  (let [docs [{:document_id "d1" :target_lang "es"} {:document_id "d1" :target_lang "fr"}]]
    (t/is (logic/still-listed? docs {:document_id "d1" :target_lang "fr"}))
    (t/is (not (logic/still-listed? docs {:document_id "d2" :target_lang "es"})))
    (t/is (not (logic/still-listed? docs {:document_id "d1" :target_lang "de"}))
        "same doc id but different lang is a different review")))

(t/deftest resource-work-inventory-retains-rows-without-candidates
  (let [orphan {:document_id "legacy/orphan" :garden_id "gardens/promethean"
                :target_lang "es" :title "Orphan Legacy Row"
                :source_lang "en" :total_segments 4}
        rows (logic/normalize-work-inventory [orphan] (observed-resource-work))
        ready (first (filter #(= "ready" (:work_state %)) rows))
        without-candidates (remove logic/candidate-present? rows)]
    (t/is (= 18 (count rows))
        "desired resource work, not completed evidence or an orphan legacy row,
         owns exact cardinality")
    (t/is (not-any? #(= "legacy/orphan" (:document_id %)) rows))
    (t/is (= 18 (count (set (map logic/work-row-id rows))))
        "each publication intent remains a distinct row")
    (t/is (= {"ready" 1 "queued" 6 "failed" 6 "missing" 5}
           (frequencies (map :work_state rows))))
    (t/is (every? logic/work-row? rows))
    (t/is (logic/candidate-present? ready))
    (t/is (= "candidate-sha-0" (:translation_revision ready))
        "nested candidate evidence is flattened for the current view")
    (t/is (= 17 (count without-candidates)))
    (t/is (every? #(nil? (:publication_review %)) without-candidates))
    (t/is (every? #(not (:contract_content %)) without-candidates)
        "candidate-less work must not enter the synthetic read-only detail path")))

(t/deftest resource-candidate-wins-a-legacy-coordinate-collision
  (let [legacy {:document_id "docs/doc-0" :garden_id "gardens/promethean"
                :target_lang "es" :source_lang "en" :title "Legacy title"
                :translation_revision "legacy-revision"
                :content_source "legacy-segments"
                :source_text "stale source" :translated_text "stale target"
                :candidate {:translation_revision "legacy-nested-revision"}
                :total_segments 9}
        work (assoc (desired-work 0 "ready")
                    :project "knoxx-session"
                    :candidate {:translation_revision "resource-revision"
                                :content_source "agent"
                                :source_text "canonical source"
                                :translated_text "canonical target"})
        [row] (logic/normalize-work-inventory [legacy] [work])]
    (t/is (= "resource-revision" (:translation_revision row)))
    (t/is (= "agent" (:content_source row)))
    (t/is (= "canonical source" (:source_text row)))
    (t/is (= "canonical target" (:translated_text row)))
    (t/is (= (:publication work) (logic/work-row-id row)))
    (t/is (= "resource-revision"
             (get-in row [:publication_review :translation_revision]))
        "the review payload is the resource candidate, never the colliding
         Mongo summary")))

(t/deftest receipt-shaped-resource-candidate-never-borrows-legacy-mutations
  (let [legacy {:document_id "docs/doc-0" :garden_id "gardens/promethean"
                :target_lang "es" :source_lang "en" :project "knoxx-session"
                :title "Legacy" :total_segments 4}
        work (assoc (desired-work 0 "ready")
                    :project "knoxx-session"
                    :translation_revision "receipt-without-content"
                    :reviewable false)
        [row] (logic/normalize-work-inventory [legacy] [work])]
    (t/is (logic/candidate-present? row))
    (t/is (not (logic/legacy-candidate? row)))
    (t/is (not (:contract_content row)))
    (t/is (= 1 (:total_segments row))
        "legacy split totals cannot make receipt-shaped evidence look hydrated")
    (t/is (not (logic/legacy-mutation-admitted? row "knoxx-session")))))

(t/deftest matching-legacy-splits-bridge-a-candidate-less-resource-row
  (let [legacy {:document_id "docs/doc-4" :garden_id "gardens/promethean"
                :target_lang "es" :source_lang "en" :title "Legacy title"
                :project "knoxx-session"
                :total_segments 3 :approved 1 :pending 1 :rejected 0
                :in_review 1 :overall_status "partial_review"}
        work (assoc (desired-work 4 "missing")
                    :project "knoxx-session"
                    :reviewable false)
        [row] (logic/normalize-work-inventory [legacy] [work])]
    (t/is (= 1 (count (logic/normalize-work-inventory [legacy] [work])))
        "resource intent still owns cardinality")
    (t/is (logic/work-row? row))
    (t/is (logic/candidate-present? row)
        "an exact persisted split set keeps the historical review card usable")
    (t/is (:legacy_candidate row))
    (t/is (= {:basis :exact-coordinate-match
              :document "docs/doc-4"
              :garden "gardens/promethean"
              :source_locale "en"
              :legacy_source_locale "en"
              :locale "es"
              :project "knoxx-session"
              :legacy_project "knoxx-session"
              :source_revision :unavailable}
             (:legacy_compatibility row))
        "the bridge records coordinate-only evidence, never revision equality")
    (t/is (= 3 (:total_segments row)))
    (t/is (= 1 (:approved row)))
    (t/is (= "partial_review" (:overall_status row)))
    (t/is (= "missing" (:work_state row))
        "legacy evidence cannot rewrite resource dispatch state")
    (t/is (= ["dispatch"] (:allowed_actions row))
        "legacy evidence cannot mint resource actions")
    (t/is (nil? (:publication_review row))
        "legacy splits are not whole-file publication evidence")
    (t/is (not (:contract_content row)))))

(t/deftest legacy-splits-do-not-bridge-the-wrong-resource-coordinate
  (let [work (assoc (desired-work 4 "missing") :project "knoxx-session")
        wrong-garden {:document_id "docs/doc-4" :garden_id "gardens/sonic"
                      :target_lang "es" :project "knoxx-session"
                      :total_segments 3}
        wrong-locale {:document_id "docs/doc-4" :garden_id "gardens/promethean"
                      :target_lang "fr" :project "knoxx-session"
                      :source_lang "en" :total_segments 3}
        wrong-source {:document_id "docs/doc-4" :garden_id "gardens/promethean"
                      :target_lang "es" :project "knoxx-session"
                      :source_lang "fr" :total_segments 3}
        wrong-project {:document_id "docs/doc-4" :garden_id "gardens/promethean"
                       :target_lang "es" :project "devel"
                       :source_lang "en" :total_segments 3}
        missing-project {:document_id "docs/doc-4" :garden_id "gardens/promethean"
                         :target_lang "es" :source_lang "en" :total_segments 3}
        [row] (logic/normalize-work-inventory
               [wrong-garden wrong-locale wrong-source wrong-project missing-project]
               [work])]
    (t/is (not (logic/candidate-present? row)))
    (t/is (not (:legacy_candidate row)))
    (t/is (= 0 (:total_segments row)))
    (t/is (nil? (:publication_review row)))))

(t/deftest empty-resource-inventory-preserves-legacy-candidates
  (let [legacy [{:document_id "docs/legacy" :garden_id "gardens/sonic"
                 :target_lang "fr" :source_lang "en" :title "Legacy"
                 :total_segments 2 :approved 0 :overall_status "pending_review"}]
        [row :as rows] (logic/normalize-work-inventory legacy [])]
    (t/is (= 1 (count rows)))
    (t/is (= "docs/legacy" (:document_id row)))
    (t/is (logic/candidate-present? row)
        "persisted segments remain selectable and reviewable in compatibility mode")
    (t/is (not (logic/work-row? row)))
    (t/is (= 2 (:total_segments row)))))

(t/deftest nil-garden-is-an-exact-legacy-coordinate
  (let [legacy {:document_id "docs/legacy" :target_lang "es"
                :source_lang "en" :project "knoxx-session"
                :title "Gardenless legacy" :total_segments 2}
        work {:publication "publications/legacy-es"
              :document "docs/legacy" :garden nil
              :source_locale "en" :locale "es" :project "knoxx-session"
              :title "Gardenless legacy" :revision "source-r"
              :work_state "missing" :allowed_actions ["dispatch"]}
        [row] (logic/normalize-work-inventory [legacy] [work])]
    (t/is (logic/legacy-candidate? row))
    (t/is (= {:project "knoxx-session"}
             (logic/legacy-review-scope row "devel")))
    (t/is (logic/legacy-mutation-admitted? row "devel")
        "omitted garden selects only the backend's exact nil-garden relation")))

(t/deftest work-row-identity-is-publication-scoped
  (let [shared {:document "docs/shared"
                :source_locale "en"
                :locale "es"
                :revision "source-sha"
                :title "Shared"
                :work_state "missing"
                :allowed_actions ["dispatch"]}
        first-work (assoc shared
                          :publication "publications/shared-promethean-es"
                          :garden "gardens/promethean")
        second-work (assoc shared
                           :publication "publications/shared-sonic-es"
                           :garden "gardens/sonic")
        rows (logic/normalize-work-inventory [] [first-work second-work])]
    (t/is (= 2 (count rows)))
    (t/is (= #{"publications/shared-promethean-es"
             "publications/shared-sonic-es"}
           (set (map logic/work-row-id rows))))
    (t/is (not (logic/same-work? (first rows) (second rows))))
    (t/is (logic/still-listed? [(second rows)] (second rows)))
    (t/is (not (logic/still-listed? [(first rows)] (second rows))))))

(t/deftest work-actions-remain-server-authored
  (let [[missing failed queued]
        (logic/normalize-work-inventory
         []
         [(desired-work 1 "missing")
          (desired-work 2 "failed")
          (desired-work 3 "queued")])]
    (t/is (= ["dispatch"] (:allowed_actions missing)))
    (t/is (logic/allowed-action? missing "dispatch"))
    (t/is (not (logic/allowed-action? missing "retry")))
    (t/is (= ["retry"] (:allowed_actions failed)))
    (t/is (logic/allowed-action? failed :retry)
        "keyword callers and string wire values compare without changing truth")
    (t/is (= [] (:allowed_actions queued)))
    (t/is (not (logic/allowed-action? queued "dispatch")))
    (t/is (not (logic/allowed-action? queued "retry")))))

(t/deftest legacy-api-scope-prefers-server-project-and-keeps-garden
  (t/is (= {:project "knoxx-session" :garden-id "gardens/sonic"}
           (logic/legacy-review-scope
            {:project "knoxx-session" :garden "gardens/sonic"}
            "devel")))
  (t/is (= {:project "devel" :garden-id "gardens/promethean"}
           (logic/legacy-review-scope
            {:garden_id "gardens/promethean"}
            "devel")))
  (t/is (= "knoxx-session"
           (logic/effective-project [{:project "knoxx-session"}] "devel")))
  (t/is (= "configured-project"
           (logic/effective-project {:project "configured-project" :reviews []}
                                    "devel"))
      "the envelope preserves server scope when inventory is empty")
  (t/is (= "devel" (logic/effective-project [{}] "devel"))))

(t/deftest legacy-mutations-require-explicit-candidate-and-project-scope
  (t/is (logic/legacy-mutation-admitted?
         {:legacy_candidate true :project "knoxx-session"
          :garden_id "gardens/promethean"}
         "devel"))
  (t/is (logic/legacy-mutation-admitted?
         {:legacy_candidate true :garden_id "gardens/promethean"}
         "devel")
      "a legacy-only list is scoped by the project used for its request")
  (t/is (logic/legacy-mutation-admitted?
         {:legacy_candidate true :project "knoxx-session"}
         "devel")
      "nil garden remains one exact backend coordinate, not a wildcard")
  (t/is (not (logic/legacy-mutation-admitted?
              {:legacy_candidate true}
              nil))
      "project scope is still mandatory")
  (t/is (not (logic/legacy-mutation-admitted?
              {:translation_revision "resource-candidate"
               :project "knoxx-session" :garden "gardens/promethean"}
              "devel"))
      "resource evidence never implies legacy mutation authority"))

(t/deftest contract-candidate-hydration-fails-closed
  (t/is (not (logic/blocked-resource-candidate?
              {:contract_candidate true :reviewable true
               :hydration_state "displayable"})))
  (doseq [row [{:contract_candidate true :reviewable false
                :hydration_state "content_missing"}
               {:contract_candidate true :reviewable false
                :hydration_state "source_moved"}
               {:contract_candidate true :reviewable false
                :hydration_state "content_moved"}
               {:contract_candidate true :reviewable true}
               {:contract_candidate true :reviewable true
                :hydration_state "future_state"}]]
    (t/is (logic/blocked-resource-candidate? row))))

(t/deftest segment-review-form-follows-selected-segment
  (t/is (= logic/default-label (logic/segment-review-form nil)))
  (t/is (= (assoc logic/default-label
                  :adequacy "poor"
                  :overall "needs_edit"
                  :corrected_text "Persisted correction")
           (logic/segment-review-form
            {:id "segment/1"
             :labels [{:id "label/newest"
                       :adequacy "poor"
                       :overall "needs_edit"
                       :corrected_text "Persisted correction"}
                      {:id "label/older" :adequacy "excellent"}]}))
      "detail labels are newest first and hydrate only admitted form fields"))

(t/deftest publication-review-joins-on-document-garden-and-locale
  (let [review {:publication "publications/doc-1-es"
                :document "docs/doc-1" :garden "gardens/promethean"
                :locale "es" :revision "source-sha"
                :translation_revision "translation-sha" :approved false}
        [joined] (logic/attach-publication-reviews
                  [{:document_id "docs/doc-1" :garden_id "gardens/promethean"
                    :target_lang "es"}]
                  [review])]
    (t/is (= review (:publication_review joined)))
    (t/is (= {:document "docs/doc-1" :garden "gardens/promethean"
            :locale "es" :revision "source-sha"
            :translation_revision "translation-sha"}
           (logic/approval-request review)))))

(t/deftest authored-contract-reviews-remain-visible-without-worker-documents
  (let [review {:publication "publications/doc-1-es"
                :document "docs/doc-1" :garden "gardens/promethean"
                :locale "es" :source_locale "en"
                :title "Promethean" :content_source "authored-contract"
                :source_text "Hello\n\nGarden"
                :translated_text "Hola\n\nJardín"
                :revision "source-sha"
                :translation_revision "translation-sha"
                :approved false}
        [document] (logic/attach-publication-reviews [] [review])
        detail (logic/authored-detail document)]
    (t/is (:contract_content document))
    (t/is (= "authored-contract" (:content_source document))
        "which kind of bytes these are survives onto the row, so the reviewer
         can be told what they are approving")
    (t/is (= "Promethean" (:title document)))
    (t/is (= "pending_review" (:overall_status document)))
    (t/is (= 2 (count (:segments detail))))
    (t/is (= "Hello" (get-in detail [:segments 0 :source_text])))
    (t/is (= "Jardín" (get-in detail [:segments 1 :translated_text])))))

(t/deftest agent-produced-reviews-remain-visible-without-worker-documents
  (t/testing "an agent translation of a contract-backed document has no worker
            document either — `translation-agent-sink` writes content and a
            receipt and creates no Mongo segments, while
            /api/translations/documents aggregates over the segments collection.
            Keeping only authored-contract here dropped every agent translation:
            invisible, so unapprovable, so unpublishable under
            :translation/review :required."
    (let [review {:publication "publications/doc-1-fr"
                  :document "docs/doc-1" :garden "gardens/promethean"
                  :locale "fr" :source_locale "en"
                  :title "Promethean" :content_source "agent"
                  :source_text "Hello\n\nGarden"
                  :translated_text "Bonjour\n\nJardin"
                  :revision "source-sha"
                  :translation_revision "agent-output-sha"
                  :approved false}
          [document] (logic/attach-publication-reviews [] [review])
          detail (logic/authored-detail document)]
      (t/is (some? document) "an agent-produced review is not dropped")
      (t/is (:contract_content document)
          "read-only for the same reason authored content is: no persisted
           segment for a label to attach to")
      (t/is (= "agent" (:content_source document))
          "and distinguishable from authored bytes, because approving generated
           text is a different act")
      (t/is (= 2 (count (:segments detail))))
      (t/is (= "Bonjour" (get-in detail [:segments 0 :translated_text]))))))

(t/deftest resource-work-without-content-is-visible-but-not-a-candidate
  (let [[row] (logic/attach-publication-reviews
               []
               [{:publication "publications/doc-9-de"
                 :document "docs/doc-9" :garden "gardens/promethean"
                 :source_locale "en" :locale "de" :revision "r"
                 :title "Document Nine" :work_state "missing"
                 :allowed_actions ["dispatch"]}])]
    (t/is (= "publications/doc-9-de" (logic/work-row-id row)))
    (t/is (= "missing" (:work_state row)))
    (t/is (logic/allowed-action? row "dispatch"))
    (t/is (not (logic/candidate-present? row)))
    (t/is (nil? (:publication_review row)))
    (t/is (not (:contract_content row)))))

(t/deftest missing-resource-candidate-bytes-fail-closed-over-legacy-content
  (let [legacy {:document_id "docs/doc-9" :garden_id "gardens/promethean"
                :target_lang "de" :source_lang "en" :title "Legacy Nine"
                :total_segments 4 :approved 3 :overall_status "partial_review"}
        work {:publication "publications/doc-9-de"
              :document "docs/doc-9" :garden "gardens/promethean"
              :source_locale "en" :locale "de" :revision "source-r"
              :translation_revision "target-r" :title "Document Nine"
              :work_state "ready" :reviewable false :approved false
              :contract_candidate true :hydration_state "content_missing"
              :allowed_actions []}
        [row] (logic/normalize-work-inventory [legacy] [work])]
    (t/is (logic/candidate-present? row))
    (t/is (:contract_content row)
        "resource ownership prevents the detail loader from substituting legacy bytes")
    (t/is (not (:legacy_candidate row)))
    (t/is (= 1 (:total_segments row))
        "legacy aggregate evidence does not decorate an authoritative candidate")
    (t/is (= false (get-in row [:publication_review :reviewable])))
    (t/is (= "content_missing" (:hydration_state row)))))

(t/deftest sft-filename-contract
  (t/is (= "devel-es-translations.jsonl" (logic/sft-filename "devel" "es")))
  (t/is (= "devel-all-translations.jsonl" (logic/sft-filename "devel" ""))))
