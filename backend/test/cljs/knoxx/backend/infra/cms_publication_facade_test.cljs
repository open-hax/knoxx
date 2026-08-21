(ns knoxx.backend.infra.cms-publication-facade-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [malli.core :as m]
            [knoxx.backend.domain.cms-publication :as cms]
            [knoxx.backend.infra.routes.cms-publication :as facade]
            [knoxx.backend.law.cms-publication :as law]
            [open-hax.publication-wire :as wire]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def document
  {:document/id :knoxx.docs/probe
   :document/title "Probe"
   :document/source-locale :en
   :document/source {:path "docs/probe.md"}})

(def intent
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :required
   :document/source-locale :en})

(def garden
  {:garden/id :knoxx.docs/promethean :garden/title "Promethean" :garden/status :active})

(def no-evidence {:receipts {} :blockers {}})

;; ── 1/2 the state-patch wire key convention ───────────────────────────────

(deftest state-patch-accepts-the-body-clj-js-produces
  (testing "round-tripped through clj->js / js->clj rather than hand-written, so
            this fails if api/request's serialization ever changes"
    (let [body (wire/state-patch-body :published)
          on-the-wire (js->clj (clj->js body) :keywordize-keys true)]
      (is (= {:state "published"} on-the-wire))
      (is (true? (m/validate law/PublicationStatePatchJson on-the-wire)))
      (is (= {:publication/state :published}
             (cms/decode-publication-state-patch on-the-wire))))))

(deftest state-patch-rejects-qualified-wire-key
  (testing "closed maps are what make this fail; an open map would accept the
            qualified key alongside the unqualified one and silently no-op"
    (is (false? (m/validate law/PublicationStatePatchJson {:publication/state "published"})))
    (is (thrown? js/Error (cms/decode-publication-state-patch
                           {:publication/state "published"})))
    (is (false? (m/validate law/PublicationStatePatchJson
                            {:state "published" :publication/state "withheld"})))))

(deftest frontend-request-shares-the-backend-contracts-vocabulary
  (testing "the backend contract's key and enum values ARE the ones the frontend
            builds its body from, so the two cannot drift apart"
    (is (= :state wire/state-patch-key))
    (doseq [state (keys wire/state-values)]
      (let [body (wire/state-patch-body state)]
        (is (true? (m/validate law/PublicationStatePatchJson body))
            (str (pr-str body) " must satisfy the backend contract"))
        (is (= {:publication/state state} (cms/decode-publication-state-patch body)))))))

;; ── 4 resource id round trip ──────────────────────────────────────────────

(deftest resource-id-round-trip
  (is (= "docs/probe" (wire/encode-id :docs/probe)))
  (is (= :docs/probe (wire/decode-id "docs/probe")))
  (testing "no EDN leading colon, so a PATCH URL contains no %3A"
    (is (not (str/starts-with? (wire/encode-id :docs/probe) ":")))
    (is (not (str/includes? (js/encodeURIComponent (wire/encode-id :docs/probe)) "%3A")))))

;; ── 5 explicit row encoding ───────────────────────────────────────────────

(deftest document-garden-publication-rows-encode-explicitly
  (let [document-wire (cms/document->wire document)
        garden-wire (cms/garden->wire garden)
        publication-wire (cms/publication->wire {:observed nil :blockers [:translation-missing]}
                                                intent)]
    (testing "every value is a JSON scalar"
      (is (true? (m/validate law/DocumentWireJson document-wire)))
      (is (true? (m/validate law/GardenWireJson garden-wire)))
      (is (true? (m/validate law/PublicationWireJson publication-wire))))
    (testing "ids keep their namespace"
      (is (= "knoxx.docs/probe" (:id document-wire)))
      (is (= "knoxx.docs/promethean" (:garden publication-wire))))
    (testing "enums cross as strings"
      (is (= "en" (:source-locale document-wire)))
      (is (= "active" (:status garden-wire)))
      (is (= "published" (:desired publication-wire)))
      (is (= ["translation-missing"] (:blockers publication-wire))))
    (testing "the revision selector keeps its namespace"
      (is (= "source/current" (:revision publication-wire))))
    (testing "no value carries an EDN leading colon"
      (doseq [value (filter string? (tree-seq coll? seq [document-wire garden-wire
                                                         publication-wire]))]
        (is (not (str/starts-with? value ":")))))))

(deftest document-source-is-mapped-field-by-field
  (testing "a future resource field on :document/source must NOT leak across the
            JSON boundary just because it exists"
    (let [dirty (assoc-in document [:document/source :internal-note] "secret")
          document-wire (cms/document->wire dirty)]
      (is (= {:path "docs/probe.md"} (:source document-wire)))
      (is (not (contains? (:source document-wire) :internal-note))))))

(deftest observed-and-desired-are-separate-fields
  (let [observed {:materialized/revision "abc123" :materialized/path "/probe"}
        with-evidence (cms/publication->wire {:observed observed :blockers []} intent)
        without (cms/publication->wire {:observed nil :blockers []} intent)]
    (is (= "published" (:desired with-evidence)))
    (is (= "abc123" (:observed with-evidence)))
    (testing "desired survives with no runtime evidence at all"
      (is (= "published" (:desired without)))
      (is (nil? (:observed without))))))

;; ── 6 the list view is not double-wrapped ─────────────────────────────────

(deftest list-view-is-not-double-wrapped
  (let [list-view {:documents [{:document document :publications [intent]}]
                   :gardens [garden]}
        encoded (cms/list-view->wire no-evidence list-view)]
    (is (true? (m/validate law/CmsListWireJson encoded)))
    (is (= #{:documents :gardens} (set (keys encoded))))
    (testing "a document view is {document, publications}, not nested again"
      (is (= #{:document :publications} (set (keys (first (:documents encoded))))))
      (is (= "knoxx.docs/probe" (get-in encoded [:documents 0 :document :id])))
      (is (nil? (get-in encoded [:documents 0 :document :document]))))))

;; ── 7 identity cannot move through a state edit ───────────────────────────

(deftest state-patch-cannot-move-identity
  (let [patched (cms/apply-state-patch intent {:publication/state :withheld})]
    (is (= :withheld (:publication/state patched)))
    (testing "every identity dimension is unchanged"
      (doseq [field cms/identity-keys]
        (is (= (get intent field) (get patched field))
            (str field " must be immutable through a state edit")))))
  (testing "a patch carrying identity fields is rejected by the closed contract"
    (doseq [field [:publication/document :publication/garden
                   :publication/locale :publication/revision]]
      (is (thrown? js/Error
                   (cms/apply-state-patch intent {:publication/state :published
                                                  field :knoxx.docs/elsewhere})))))
  (testing "and the patched resource still satisfies the resource contract"
    (doseq [state (keys wire/state-values)]
      (is (some? (cms/apply-state-patch intent {:publication/state state}))))))

;; ── Revision selector versus concrete revision ────────────────────────────

(deftest revision-selector-and-concrete-revision-are-distinguishable
  (testing "a selector round-trips to a keyword"
    (is (= "source/current" (cms/encode-revision :source/current)))
    (is (= :source/current (cms/decode-revision "source/current"))))
  (testing "a concrete revision stays a string — keywordizing would corrupt a sha"
    (is (= "abc123" (cms/encode-revision "abc123")))
    (is (= "abc123" (cms/decode-revision "abc123")))
    (is (string? (cms/decode-revision "abc123")))))

;; ── a state edit must not destroy the file it edits (Codex P1 on #239) ─────

(def ^:private authored-manifest
  "What a human actually writes: one namespace, several resources, and an entry
   that declares a document and a publication together."
  {:namespace :knoxx.docs
   :resources [{:document/id :probe
                :document/title "Probe"
                :document/source-locale :en
                :document/source {:path "docs/probe.md"}}
               {:garden/id :promethean
                :garden/title "Promethean"
                :garden/status :active}
               {:publication/id :probe-es
                :publication/document :probe
                :publication/garden :promethean
                :publication/locale :es
                :publication/revision :source/current
                :publication/state :published
                :publication/path "/probe"
                :translation/review :required}
               {:publication/id :probe-fr
                :publication/document :probe
                :publication/garden :promethean
                :publication/locale :fr
                :publication/revision :source/current
                :publication/state :published
                :publication/path "/probe-fr"
                :translation/review :required}]})

(deftest a-state-edit-preserves-the-whole-authored-manifest
  (let [updated (facade/authored-with-state authored-manifest
                                            :knoxx.docs/probe-es
                                            :withheld)]
    (testing "the wrapper survives — serializing the projected intent over the
              file used to delete :namespace, :resources and every sibling"
      (is (= :knoxx.docs (:namespace updated)))
      (is (= 4 (count (:resources updated)))))
    (testing "only the target publication moved"
      (let [by-id (fn [m k] (some #(when (= k (:publication/id %)) %) (:resources m)))]
        (is (= :withheld (:publication/state (by-id updated :probe-es))))
        (is (= :published (:publication/state (by-id updated :probe-fr)))
            "a sibling publication in the same manifest is untouched")))
    (testing "the document and garden facets are byte-identical"
      (is (= (first (:resources authored-manifest)) (first (:resources updated))))
      (is (= (second (:resources authored-manifest)) (second (:resources updated)))))
    (testing "and nothing but the one state key differs anywhere in the file"
      (is (= (assoc-in authored-manifest [:resources 2 :publication/state] :withheld)
             updated)))))

(deftest a-standalone-publication-file-keeps-its-authored-keys
  (testing "the projection runs select-keys, so writing it back dropped
            :namespace and anything else the author wrote"
    (let [authored {:namespace :knoxx.docs
                    :publication/id :probe-es
                    :publication/document :knoxx.docs/probe
                    :publication/garden :knoxx.docs/promethean
                    :publication/locale :es
                    :publication/revision :source/current
                    :publication/state :published
                    :publication/path "/probe"
                    :translation/review :required}
          updated (facade/authored-with-state authored :knoxx.docs/probe-es :archived)]
      (is (= :archived (:publication/state updated)))
      (is (= :knoxx.docs (:namespace updated)))
      (is (= (assoc authored :publication/state :archived) updated)))))

(deftest an-edit-to-a-file-that-does-not-declare-it-is-refused
  (testing "a manifest without the publication"
    (is (thrown? js/Error
                 (facade/authored-with-state
                  (update authored-manifest :resources #(vec (remove :publication/id %)))
                  :knoxx.docs/probe-es
                  :withheld))))
  (testing "and a standalone file for a different publication"
    (is (thrown? js/Error
                 (facade/authored-with-state
                  {:namespace :knoxx.docs :publication/id :other :publication/state :published}
                  :knoxx.docs/probe-es
                  :withheld)))))

