(ns knoxx.backend.infra.publication-migration-fold-test
  "Fold-level review findings on #232.

  A separate namespace from `domain.publication-migration-test` deliberately:
  these assert the *effectful* fold's contract with its writer and its receipt
  store, and that test file is already at the file-size budget."
  (:require [cljs.test :refer [deftest is testing]]
            [malli.core :as m]
            [knoxx.backend.domain.publication-migration :as migration]
            [knoxx.backend.infra.publication-migration :as fold]
            [knoxx.backend.law.publication :as law]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def policy
  {:migration/namespace :knoxx.docs
   :migration/membership-review :required})

(def legacy-document
  "The raw legacy shape, with a namespace-LOCAL :document/id — which is the
   whole point of these tests: the document phase qualifies it, so anything
   referencing it must qualify it the same way."
  {:legacy/doc-id "doc-1"
   :document/id :translation-pipeline
   :document/source-locale "en"
   :title "Translation Pipeline"
   :source_path "docs/translation-pipeline.md"
   :metadata {:garden_publications [{:garden_id "garden-a"}]}})

(defn- membership-row
  [document]
  (let [[result] (migration/normalize-publication-rows document)]
    (is (= :normalized (:migration/status result)))
    (:row result)))

(defn- recording-ctx
  "Like the domain test's fake, but the writer is swappable so the fold's
   contract with it can be asserted directly."
  ([source] (recording-ctx source (fn [resource] (js/Promise.resolve resource))))
  ([source write-fn]
   (let [receipts (atom {})
         writes (atom [])]
     {:ctx {:read-records! (fn [] (js/Promise.resolve source))
            :write! (fn [resource]
                      (swap! writes conj resource)
                      (write-fn resource))
            :append-receipt-once! (fn [receipt-key decision]
                                    (swap! receipts
                                           (fn [current]
                                             (if (contains? current receipt-key)
                                               current
                                               (assoc current receipt-key decision))))
                                    (js/Promise.resolve receipt-key))}
      :receipts receipts
      :writes writes})))

;; ── the publication's document reference is canonicalized (Codex P1) ───────

(deftest publication-reference-is-canonicalized-from-the-raw-legacy-document
  (testing "built from the SAME raw legacy document the document phase consumes,
            the reference must be qualified — copying :document/id verbatim left
            it bare, and PublicationIntentResource then aborted the batch"
    (let [row (membership-row legacy-document)
          decision (migration/publication->decision policy legacy-document row)]
      (is (= :candidate (:migration/status decision)))
      (is (= :knoxx.docs/translation-pipeline
             (get-in decision [:resource :publication/document])))
      (is (true? (m/validate law/PublicationIntentResource (:resource decision))))))
  (testing "and it agrees with what the document phase actually writes"
    (is (= (get-in (migration/document->decision policy legacy-document)
                   [:resource :document/id])
           (get-in (migration/publication->decision
                    policy legacy-document (membership-row legacy-document))
                   [:resource :publication/document])))))

;; ── one id, two payloads is a conflict, not last-write-wins (Codex P1) ─────

(def ^:private colliding-documents
  "Two legacy rows whose names canonicalize to one id while disagreeing on
   source path. A keyword and a string name resolve to the same component."
  [{:document/id :translation-pipeline
    :document/source-locale "en"
    :title "First"
    :source_path "docs/first.md"}
   {:document/id "translation-pipeline"
    :document/source-locale "en"
    :title "Second"
    :source_path "docs/second.md"}])

(deftest ^:async one-canonical-id-with-two-payloads-conflicts
  (let [{:keys [ctx receipts]} (recording-ctx {:documents colliding-documents})
        result (await (fold/migrate-publication-records!
                       ctx policy migration/empty-index))]
    (testing "the first writes and the second is refused"
      (is (= 1 (count (:written result))))
      (is (= 1 (count (:conflicts result))))
      (is (= :resource-identity-conflict (:reason (first (:conflicts result))))))
    (testing "the conflict names the identity and carries both payloads"
      (let [conflict (first (:conflicts result))]
        (is (= :documents (:resource/kind conflict)))
        (is (= :knoxx.docs/translation-pipeline (:resource/id conflict)))
        (is (some? (:candidate conflict)))
        (is (some? (:existing conflict)))
        (is (some? (:source conflict)))))
    (testing "and the first payload still stands — a blind write would have
              replaced it, making the result depend on input order"
      (is (= "docs/first.md"
             (get-in result [:index :documents :knoxx.docs/translation-pipeline
                             :document/source :path]))))
    (testing "the refusal is recorded for resolution"
      (is (= 1 (count @receipts))))))

;; ── every malformed row keeps its own receipt (Codex P1) ───────────────────

(deftest ^:async malformed-rows-do-not-share-one-receipt
  (testing "rows whose identity is itself the malformed thing have no id to key
            on; keying them all as nil let the append-once store keep only one,
            so not every broken row stayed available for resolution"
    (let [{:keys [ctx receipts]} (recording-ctx
                                  {:gardens [{:title "Garden A" :status "active"}
                                             {:title "Garden B" :status "active"}]})
          result (await (fold/migrate-publication-records!
                         ctx policy migration/empty-index))]
      (is (= 2 (count (:conflicts result))))
      (is (empty? (:written result)))
      (testing "two distinct rows, two distinct receipts"
        (is (= 2 (count @receipts))))))
  (testing "while two byte-identical rows are one fact and share a key"
    (let [identical-row {:title "Garden A" :status "active"}
          {:keys [ctx receipts]} (recording-ctx {:gardens [identical-row identical-row]})
          result (await (fold/migrate-publication-records!
                         ctx policy migration/empty-index))]
      (is (= 2 (count (:conflicts result))))
      (is (= 1 (count @receipts))))))

;; ── the writer's return value is checked, not trusted (Codex P1) ───────────

(deftest ^:async a-writer-that-does-not-return-the-saved-resource-fails
  (testing "an acknowledgement instead of the resource would otherwise be
            indexed as saved state, and every later row in the run would
            reconcile against it"
    (let [{:keys [ctx]} (recording-ctx {:documents [legacy-document]}
                                       (fn [_resource] (js/Promise.resolve {:acknowledged true})))]
      (is (thrown? js/Error
                   (await (fold/migrate-publication-records!
                           ctx policy migration/empty-index))))))
  (testing "and so would a writer that resolves to nothing"
    (let [{:keys [ctx]} (recording-ctx {:documents [legacy-document]}
                                       (fn [_resource] (js/Promise.resolve nil)))]
      (is (thrown? js/Error
                   (await (fold/migrate-publication-records!
                           ctx policy migration/empty-index))))))
  (testing "a writer returning the resource it was given is accepted"
    (let [{:keys [ctx]} (recording-ctx {:documents [legacy-document]})
          result (await (fold/migrate-publication-records!
                         ctx policy migration/empty-index))]
      (is (= 1 (count (:written result))))
      (is (true? (m/validate law/Document (first (:written result))))))))
