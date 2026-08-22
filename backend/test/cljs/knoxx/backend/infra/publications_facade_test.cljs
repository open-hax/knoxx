(ns knoxx.backend.infra.publications-facade-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.infra.routes.publications :as facade]))

;; ── single-kind projection (Codex P1 on #230) ─────────────────────────────
;;
;; A composite manifest entry expands to one record per registered kind, and
;; every expanded definition retains ALL the composite keys. Without projecting
;; each record onto its own kind, one entry registering a document AND a
;; publication is indexed twice.

(def composite-definition
  {:namespace :knoxx.docs
   :document/id :knoxx.docs/probe
   :document/title "Probe"
   :document/source-locale :en
   :document/source {:path "docs/probe.md"}
   :publication/id :knoxx.docs/probe-en
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :en
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :none})

(def garden-definition
  {:namespace :knoxx.docs
   :garden/id :knoxx.docs/promethean
   :garden/title "Promethean"
   :garden/status :active
   :garden/locales [:en :es]})

(defn- expanded-records
  "What the loader actually hands the facade for a composite entry: one record
   per registered kind, each carrying the whole composite definition."
  []
  [{:ok? true :resource/kind :document :resource/definition composite-definition}
   {:ok? true :resource/kind :publication :resource/definition composite-definition}
   {:ok? true :resource/kind :garden :resource/definition garden-definition}])

(deftest single-kind-definition-keeps-only-its-own-facet
  (let [[document-record publication-record] (expanded-records)
        document (facade/single-kind-definition document-record)
        publication (facade/single-kind-definition publication-record)]
    (testing "the document record keeps only the document identity"
      (is (contains? document :document/id))
      (is (not (contains? document :publication/id))))
    (testing "the publication record keeps only the publication identity"
      (is (contains? publication :publication/id))
      (is (not (contains? publication :document/id))))
    (testing "a record of an unrelated kind carries no publication identity"
      (let [other (facade/single-kind-definition
                   {:resource/kind :agent :resource/definition composite-definition})]
        (is (not-any? #(contains? other %)
                      [:document/id :garden/id :publication/id]))))))

(deftest composite-entry-does-not-produce-a-false-relation-conflict
  (testing "indexing the raw expanded definitions appends the publication twice
            and reports a duplicate relation that does not exist"
    (is (thrown-with-msg?
         js/Error #"conflicting publication intents"
         (resolver/publication-index (mapv :resource/definition (expanded-records))))))
  (testing "projecting each record onto its own kind first indexes it once"
    (let [index (resolver/publication-index
                 (mapv facade/single-kind-definition (expanded-records)))]
      (is (= 1 (count (:publications index))))
      (is (= 1 (count (:documents index))))
      (is (= 1 (count (:gardens index)))))))

;; ── invalid resources are blockers, not omissions ─────────────────────────

(deftest invalid-resources-surface-as-blockers
  (testing "a record the loader rejected is reported rather than silently absent"
    (let [records [{:ok? true :resource/kind :garden :resource/definition garden-definition}
                   {:ok? false
                    :resource/kind :publication
                    :resource/file-path "/contracts/publications/broken.edn"}]
          blockers (facade/invalid-resource-blockers records)]
      (is (= [{:blocker :invalid-resource
               :resource/kind :publication
               :resource/file-path "/contracts/publications/broken.edn"}]
             blockers))))
  (testing "a fully valid record set yields no blockers"
    (is (empty? (facade/invalid-resource-blockers (expanded-records))))))

;; ── blockers stay publication-scoped (Codex P2 on #230) ───────────────────

(deftest unrelated-invalid-resources-are-not-publication-blockers
  (testing "the loader walks the entire contracts tree, so one rejected agent or
            role must not 409 every publication read"
    (is (empty? (facade/invalid-resource-blockers
                 [{:ok? false
                   :resource/kind :agent
                   :resource/file-path "/contracts/agents/broken.edn"}
                  {:ok? false
                   :resource/kind :role
                   :resource/file-path "/contracts/roles/broken.edn"}]))))
  (testing "while a rejected resource of a publication kind still blocks"
    (doseq [kind [:document :garden :publication]]
      (is (= 1 (count (facade/invalid-resource-blockers
                       [{:ok? false
                         :resource/kind kind
                         :resource/file-path "/contracts/broken.edn"}])))
          (str kind " must block")))))

(deftest a-file-level-failure-blocks-because-its-kind-is-unknowable
  (testing "an unreadable or unparseable file cannot be attributed to a kind, so
            it cannot be ruled out as a publication either"
    (is (= [{:blocker :invalid-resource
             :resource/kind nil
             :resource/file-path "/contracts/publications/malformed.edn"}]
           (facade/invalid-resource-blockers
            [{:ok? false
              :resource/file-path "/contracts/publications/malformed.edn"}])))))
