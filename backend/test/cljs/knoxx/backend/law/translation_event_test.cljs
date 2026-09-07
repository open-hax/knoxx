(ns knoxx.backend.law.translation-event-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.translation-event :as event]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-fixture :as fixture]))

(defn- completion-receipt
  [manifest claim candidate-set turn]
  {:receipt/type :translation/completed
   :translation/document (:split-manifest/document manifest)
   :translation/garden (:split-manifest/garden manifest)
   :translation/source-locale (:split-manifest/source-locale manifest)
   :translation/locale (:split-manifest/target-locale manifest)
   :translation/source-revision (:split-manifest/source-revision manifest)
   :translation/revision (:candidate-set/revision candidate-set)
   :translation/content-digest "sha256-complete-target"
   :translation/dispatch-key (:translation-turn/dispatch-key turn)
   :translation/org-id (:split-manifest/org-id manifest)
   :translation/project (:split-manifest/project manifest)
   :translation/split-manifest-id (:split-manifest/id manifest)
   :translation/candidate-claim-id (:candidate-claim/id claim)
   :translation/candidate-set-id (:candidate-set/id candidate-set)
   :translation/candidate-set-digest (:candidate-set/digest candidate-set)
   :translation/split-count (count (:candidate-set/members candidate-set))
   :translation/split-turn-admitted-at (:translation-turn/admitted-at turn)
   :translation/at "2026-08-30T12:01:00.000Z"})

(defn- completed-context
  []
  (let [manifest (fixture/manifest)
        claim (fixture/claim manifest)
        candidate-set (fixture/candidate-set manifest claim)
        turn (split/translation-turn-admission
              fixture/digest
              {:dispatch-key "dispatch-key-1"
               :run-id "translation-run-1"
               :admitted-at "2026-08-30T11:59:00.000Z"
               :manifest manifest
               :candidate-claim claim
               :execution (split/execution-snapshot
                           fixture/digest
                           {:agent-id "publication_translator"
                            :model "gemma4:e2b"
                            :thinking :medium
                            :system-prompt "Translate every admitted split."
                            :tool-ids ["save_translation"]})
               :memory (split/memory-snapshot {:status :empty :examples []})})
        receipt (completion-receipt manifest claim candidate-set turn)]
    {:manifest manifest :candidate-set candidate-set :turn turn :receipt receipt}))

(deftest completed-candidate-events-are-stable-and-lineage-complete
  (let [{:keys [manifest candidate-set turn receipt]} (completed-context)
        first-pass (event/candidate-events fixture/digest receipt turn candidate-set)
        replay (event/candidate-events fixture/digest receipt turn candidate-set)]
    (is (= first-pass replay) "an equal receipt replay rebuilds byte-equal envelopes")
    (is (= (count (:split-manifest/splits manifest)) (count first-pass)))
    (is (= (count first-pass) (count (set (map :id first-pass)))))
    (testing "source and target bytes travel with immutable candidate lineage"
      (doseq [[source candidate envelope]
              (map vector (:split-manifest/splits manifest)
                   (:candidate-set/members candidate-set)
                   first-pass)]
        (is (= "openplanner.event.v1" (:schema envelope)))
        (is (= "translation.segment" (:kind envelope)))
        (is (= "in_review" (get-in envelope [:meta :status])))
        (is (= (select-keys (:meta envelope)
                            [:source_lang :target_lang :source_text
                             :mt_model :status])
               (select-keys (:extra envelope)
                            [:source_lang :target_lang :source_text
                             :mt_model :status]))
            "translation metadata survives SDK persistence under extra")
        (is (= (:split/source-text source)
               (get-in envelope [:meta :source_text])))
        (is (= (:candidate/text candidate) (:text envelope)))
        (is (= (:candidate/attempt-id candidate)
               (get-in envelope [:extra :candidate_attempt_id])))
        (is (= (:candidate-set/id candidate-set)
               (get-in envelope [:extra :candidate_set_id])))
        (is (= "open-hax.documents/start-here"
               (get-in envelope [:source_ref :document_id])))))
    (is (= [0 1 2] (mapv #(get-in % [:source_ref :segment_index]) first-pass)))
    (is (every? #(= "gemma4:e2b" (get-in % [:meta :mt_model])) first-pass))))

(deftest an-event-cannot-be-projected-from-a-different-receipt
  (let [{:keys [candidate-set turn receipt]} (completed-context)]
    (is (thrown-with-msg?
         js/Error
         #"does not match its completed receipt"
         (event/candidate-events fixture/digest
                                 (assoc receipt :translation/dispatch-key "other")
                                 turn candidate-set)))))
