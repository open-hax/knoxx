(ns knoxx.backend.infra.translation-dictionary-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.translation-dictionary :as dictionary]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-fixture :as fixture]))

(defn- context
  []
  (let [manifest (fixture/manifest)
        claim (fixture/claim manifest)
        candidates (fixture/candidates claim)
        candidate-set (split/complete-candidate-set
                       fixture/digest manifest claim candidates)
        turn (split/translation-turn-admission
              fixture/digest
              {:dispatch-key "dictionary-dispatch"
               :run-id "dictionary-run"
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
               :memory (split/memory-snapshot {:status :empty :examples []})})]
    {:manifest manifest
     :candidates candidates
     :candidate-set candidate-set
     :turn turn}))

(defn- persist-candidate-set!
  [store {:keys [turn candidates candidate-set]}]
  (split-store/admit-turn! store turn)
  (doseq [candidate candidates]
    (split-store/append-candidate-split!
     store (:translation-turn/id turn) candidate))
  (split-store/complete-candidate-set!
   store (:translation-turn/id turn) candidate-set))

(defn- scope
  [manifest candidate-set]
  {:org-id (:split-manifest/org-id manifest)
   :project (:split-manifest/project manifest)
   :garden (:split-manifest/garden manifest)
   :source-locale (:split-manifest/source-locale manifest)
   :target-locale (:split-manifest/target-locale manifest)
   :current-candidate-set-ids #{(:candidate-set/id candidate-set)}})

(deftest ^:async dictionary-is-only-current-effective-approved-memory
  (let [{:keys [manifest candidate-set] :as workflow} (context)
        store (split-store/memory-store fixture/digest)
        split-id (get-in manifest [:split-manifest/splits 1 :split/id])
        selector (scope manifest candidate-set)
        approval (fixture/review-receipt
                  manifest candidate-set split-id fixture/principal
                  "2026-08-30T12:00:00.000Z"
                  (fixture/review-request))
        rejection (fixture/review-receipt
                   manifest candidate-set split-id fixture/principal
                   "2026-08-30T12:01:00.000Z"
                   (fixture/review-request
                    {:review/operation-id "dictionary-rejection"
                     :review/overall "reject"
                     :review/corrected-text nil}))]
    (persist-candidate-set! store workflow)

    (testing "unreviewed model output never enters the dictionary"
      (is (empty? (:translation-dictionary/entries
                   (await (dictionary/current! store selector))))))

    (split-store/append-review-receipt! store approval)
    (let [entries (:translation-dictionary/entries
                   (await (dictionary/current! store selector)))
          entry (first entries)]
      (testing "approved reviewer correction wins and retains exact lineage"
        (is (= 1 (count entries)))
        (is (= "First paragraph.\n\n"
               (:translation-dictionary/source-text entry)))
        (is (= "Párrafo inicial corregido.\n\n"
               (:translation-dictionary/target-text entry)))
        (is (= (:review/id approval)
               (get-in entry [:translation-dictionary/evidence
                              :translation-memory/review-receipt-id])))))

    (testing "tenant, garden, and server current-set authority are exact"
      (is (empty? (:translation-dictionary/entries
                   (await (dictionary/current!
                           store (assoc selector :org-id "another-org"))))))
      (is (empty? (:translation-dictionary/entries
                   (await (dictionary/current!
                           store (assoc selector :garden :other.gardens/site))))))
      (is (empty? (:translation-dictionary/entries
                   (await (dictionary/current!
                           store (assoc selector :current-candidate-set-ids #{})))))))

    (testing "a later effective rejection revokes, rather than overwrites, memory"
      (split-store/append-review-receipt! store rejection)
      (is (empty? (:translation-dictionary/entries
                   (await (dictionary/current! store selector)))))
      (is (= [approval rejection]
             (split-store/review-history-for-split!
              store (:candidate-set/id candidate-set) split-id))))))
