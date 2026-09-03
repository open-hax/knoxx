(ns knoxx.backend.law.translation-agent-submission-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.translation-agent-submission :as law]
            [malli.core :as m]))

(def ^:private policies
  {:document_id "open-hax.documents/promethean"
   :garden_id "open-hax.gardens/promethean"
   :source_lang "en"
   :target_lang "de"
   :org_id "open-hax"
   :dispatch_key "\"open-hax\"|nil|\"g\"|:d|:en|:de|\"r\""
   :run_id "translation-run-x"})

(def ^:private source
  "Open Hax is a garden for tools, research, art, and systems.")

(def ^:private pair
  {:source_text source
   :translated_text "Open Hax ist ein Garten für Werkzeuge, Forschung, Kunst und Systeme."
   :segment_index 0})

(deftest an-accepted-pair-is-refused-for-nothing
  (testing "a well-formed whole-document submission passes"
    (is (nil? (law/pair-refusal policies pair))))

  (testing "the coordinates may be omitted, because the session is already pinned"
    ;; The tool defaults every one of them from the resource policies, so absence
    ;; means the agent accepted the pin. Refusing absence would refuse the
    ;; ordinary case.
    (is (nil? (law/pair-refusal policies (dissoc pair :document_id))))
    (is (nil? (law/pair-refusal policies
                                (assoc pair :document_id nil :garden_id nil)))))

  (testing "the coordinates may also be restated, as long as they agree"
    (is (nil? (law/pair-refusal policies (merge pair (select-keys policies
                                                                 [:document_id
                                                                  :garden_id
                                                                  :source_lang
                                                                  :target_lang
                                                                  :org_id]))))))

  (testing "an absent segment index is a whole-document submission"
    (is (nil? (law/pair-refusal policies (dissoc pair :segment_index))))
    (is (nil? (law/pair-refusal policies (assoc pair :segment_index "0"))))))

(deftest a-pair-about-something-else-is-refused-before-its-content-is-judged
  (testing "each pinned coordinate is enforced"
    (doseq [[field value expected]
            [[:document_id "other/doc" :pair-document-mismatch]
             [:garden_id "other/garden" :pair-garden-mismatch]
             [:target_lang "fr" :pair-locale-mismatch]
             [:source_lang "ja" :pair-source-locale-mismatch]
             [:org_id "someone-else" :pair-org-mismatch]]]
      (is (= expected (:refusal/type (law/pair-refusal policies
                                                       (assoc pair field value))))
          (str field " => " value " was not refused as " expected))))

  (testing "identity is checked before content, so a pair about the wrong document is never reported as the right document having bad content"
    (let [refusal (law/pair-refusal policies (assoc pair
                                                    :document_id "other/doc"
                                                    :translated_text ""))]
      (is (= :pair-document-mismatch (:refusal/type refusal)))))

  (testing "a refusal carries both sides"
    (let [refusal (law/pair-refusal policies (assoc pair :target_lang "fr"))]
      (is (= "de" (:refusal/expected refusal)))
      (is (= "fr" (:refusal/actual refusal))))))

(deftest content-that-is-not-a-translation-is-refused
  (testing "a blank translation is refused"
    (is (= :pair-translation-missing
           (:refusal/type (law/pair-refusal policies (assoc pair :translated_text "")))))
    (is (= :pair-translation-missing
           (:refusal/type (law/pair-refusal policies (assoc pair :translated_text "  \n ")))))
    (is (= :pair-translation-missing
           (:refusal/type (law/pair-refusal policies (dissoc pair :translated_text))))))

  (testing "prose echoed back untranslated is refused"
    (is (= :pair-translation-untranslated
           (:refusal/type (law/pair-refusal policies
                                            (assoc pair :translated_text source))))))

  (testing "a short shared token is not treated as an untranslated echo"
    ;; "Knoxx" translated into German is "Knoxx". Refusing that would make a
    ;; correct submission impossible for a one-word document.
    (is (nil? (law/pair-refusal policies (assoc pair
                                                :source_text "Knoxx"
                                                :translated_text "Knoxx")))))

  (testing "the generic content law leaves numbered split authority to the turn law"
    ;; `split-pair-refusal` validates this ordinal against the persisted atomic
    ;; turn. Treating every non-zero index as invalid here recreated the old
    ;; whole-document path and made real server splits impossible to submit.
    (is (nil? (law/pair-refusal policies (assoc pair :segment_index 3))))))

(deftest every-refusal-type-can-tell-the-agent-what-to-do
  (testing "no refusal type is missing a message"
    (doseq [type law/pair-refusal-types]
      (is (contains? law/refusal-messages type)
          (str type " has no message, so an agent receiving it learns nothing"))))

  (testing "an unknown refusal still produces a sentence rather than nil"
    (is (string? (law/refusal-message {:refusal/type :something-new})))))

(deftest the-completion-report-is-shaped-as-the-worker-path-expects
  (testing "a report validates and names the run and the document wire id"
    (let [report (law/completion-report "translation-run-x" "open-hax.documents/promethean")]
      (is (m/validate law/CompletionReport report))
      (is (= "complete" (:status report)))
      (is (= "translation-run-x" (:batch_id report)))
      (is (= "open-hax.documents/promethean" (:completed_document report)))))

  (testing "a blank run id or document cannot produce one"
    (is (thrown? js/Error (law/completion-report "" "open-hax.documents/promethean")))
    (is (thrown? js/Error (law/completion-report "translation-run-x" "")))
    (is (thrown? js/Error (law/completion-report nil nil)))))
