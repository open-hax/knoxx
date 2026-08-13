(ns knoxx.backend.shape.resource-manifest-test
  "The invariant here is the one that was violated in production: publishing a
   document must not delete the document."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.shape.resource-manifest :as manifest]))

(def ^:private multi-resource-manifest
  "A document, its garden, and a publication relating them — the ordinary shape
   of a hand-authored manifest, and the shape that used to be destroyed."
  {:namespace :knoxx.verify
   :resources
   [{:document/id :probe
     :document/title "Probe"
     :document/source-locale :en
     :document/source {:path "docs/probe.md"}}
    {:garden/id :probe-garden
     :garden/title "Verification Garden"
     :garden/status :active}
    {:publication/id :probe-es
     :publication/document :probe
     :publication/garden :probe-garden
     :publication/locale :es
     :publication/revision "rev-1"
     :publication/state :withheld
     :publication/path "/verify/probe-es"
     :translation/review :required}]})

(deftest assoc-entry-field-changes-only-the-named-field
  (let [next (manifest/assoc-entry-field multi-resource-manifest
                                         :publication/id :knoxx.verify/probe-es
                                         :publication/state :published)
        publication (nth (:resources next) 2)]
    (testing "the targeted field changed"
      (is (= :published (:publication/state publication))))
    (testing "and nothing else on that entry did"
      (is (= (dissoc (nth (:resources multi-resource-manifest) 2) :publication/state)
             (dissoc publication :publication/state))))))

(deftest assoc-entry-field-preserves-every-sibling
  (testing "publishing must not delete the document or the garden it publishes"
    (let [next (manifest/assoc-entry-field multi-resource-manifest
                                           :publication/id :knoxx.verify/probe-es
                                           :publication/state :published)]
      (is (= 3 (count (:resources next))))
      (is (= (nth (:resources multi-resource-manifest) 0) (nth (:resources next) 0)))
      (is (= (nth (:resources multi-resource-manifest) 1) (nth (:resources next) 1)))
      (testing "and the manifest keeps its namespace, without which every"
        (testing "namespace-local id below it stops canonicalizing"
          (is (= :knoxx.verify (:namespace next))))))))

(deftest assoc-entry-field-matches-a-namespace-local-id
  (testing "entries write local ids; the caller holds a canonical one"
    (is (manifest/contains-entry? multi-resource-manifest
                                  :publication/id :knoxx.verify/probe-es))
    (is (not (manifest/contains-entry? multi-resource-manifest
                                       :publication/id :other.ns/probe-es)))))

(deftest assoc-entry-field-handles-a-standalone-resource-file
  (testing "a file holding one already-qualified resource, not a manifest"
    (let [standalone {:publication/id :knoxx.verify/probe-es
                      :publication/state :withheld
                      :publication/path "/verify/probe-es"}
          next (manifest/assoc-entry-field standalone
                                           :publication/id :knoxx.verify/probe-es
                                           :publication/state :published)]
      (is (= :published (:publication/state next)))
      (is (= "/verify/probe-es" (:publication/path next)))
      (is (not (manifest/namespace-manifest? standalone))))))

;; ── Unique target ──────────────────────────────────────────────────────────
;;
;; Duplicate canonical publication ids are not rejected upstream today
;; (knoxx-publication-duplicate-identity), so a manifest really can declare the
;; same id twice. A request naming one resource must never rewrite two.

(def ^:private duplicate-id-manifest
  (update multi-resource-manifest :resources conj
          {:publication/id :probe-es
           :publication/document :probe
           :publication/garden :probe-garden
           :publication/locale :es
           :publication/revision "rev-CONFLICTING"
           :publication/state :withheld
           :publication/path "/verify/probe-es-conflicting"
           :translation/review :none}))

(deftest assoc-entry-field-refuses-when-two-entries-claim-the-id
  (let [thrown (try
                 (manifest/assoc-entry-field duplicate-id-manifest
                                             :publication/id :knoxx.verify/probe-es
                                             :publication/state :published)
                 nil
                 (catch :default err err))]
    (is (some? thrown) "must refuse rather than edit both declarations")
    (is (= 2 (:matches (ex-data thrown))))
    (testing "and the manifest is untouched"
      (is (= 4 (count (:resources duplicate-id-manifest))))
      (is (every? #(not= :published (:publication/state %))
                  (filter :publication/id (:resources duplicate-id-manifest)))))))

(deftest assoc-entry-field-refuses-when-nothing-matches
  (is (some? (try (manifest/assoc-entry-field multi-resource-manifest
                                              :publication/id :other.ns/nope
                                              :publication/state :published)
                  nil
                  (catch :default err err)))))

;; ── Widening guard ─────────────────────────────────────────────────────────

(deftest unchanged-except-accepts-a-single-field-edit
  (let [next (manifest/assoc-entry-field multi-resource-manifest
                                         :publication/id :knoxx.verify/probe-es
                                         :publication/state :published)]
    (is (manifest/unchanged-except? multi-resource-manifest next
                                    :publication/id :knoxx.verify/probe-es
                                    :publication/state))))

(deftest unchanged-except-catches-a-widened-write
  (testing "the regression this guards: the whole file replaced by one resource"
    (is (not (manifest/unchanged-except?
              multi-resource-manifest
              {:publication/id :knoxx.verify/probe-es :publication/state :published}
              :publication/id :knoxx.verify/probe-es :publication/state))))
  (testing "a sibling quietly dropped"
    (is (not (manifest/unchanged-except?
              multi-resource-manifest
              (update multi-resource-manifest :resources #(vec (rest %)))
              :publication/id :knoxx.verify/probe-es :publication/state))))
  (testing "a second field changed on the targeted entry"
    (is (not (manifest/unchanged-except?
              multi-resource-manifest
              (update-in multi-resource-manifest [:resources 2]
                         assoc :publication/state :published
                               :publication/path "/moved")
              :publication/id :knoxx.verify/probe-es :publication/state))))
  (testing "the namespace stripped, which would de-canonicalize every local id"
    (is (not (manifest/unchanged-except?
              multi-resource-manifest
              (dissoc multi-resource-manifest :namespace)
              :publication/id :knoxx.verify/probe-es :publication/state)))))
