(ns knoxx.backend.law.publication-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [malli.core :as m]
            [knoxx.backend.law.publication :as pub]
            ["node:fs" :as node-fs]
            ["node:path" :as path]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def example-manifest-intent
  "Mirrors the resource-contracts card's example manifest, with ids already
   namespace-qualified the way the resource loader qualifies them."
  {:publication/id :knoxx.docs/translation-pipeline-es
   :publication/document :knoxx.docs/translation-pipeline
   :publication/garden :gardens/promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/translation-pipeline"
   :translation/review :required})

(def translation-pipeline-document
  {:document/id :knoxx.docs/translation-pipeline
   :document/title "Translation Pipeline"
   :document/source-locale :en
   :document/source {:path "docs/translation-pipeline.md"}})

(def style-guide-document
  {:document/id :knoxx.docs/style-guide
   :document/title "Style Guide"
   :document/source-locale :en
   :document/source {:path "docs/style-guide.md"}})

(def promethean-garden
  {:garden/id :gardens/promethean
   :garden/title "Promethean"
   :garden/status :active
   :garden/locales [:en :es :fr]})

(def legacy-garden
  {:garden/id :gardens/legacy
   :garden/title "Legacy"
   :garden/status :archived
   :garden/locales [:en]})

(def style-guide-fr-intent
  {:publication/id :knoxx.docs/translation-pipeline-fr
   :publication/document :knoxx.docs/translation-pipeline
   :publication/garden :gardens/promethean
   :publication/locale :fr
   :publication/revision "abc123"
   :publication/state :withheld
   :publication/path "/translation-pipeline-fr"
   :translation/review :required})

(def style-guide-archived-intent
  {:publication/id :knoxx.docs/style-guide-en
   :publication/document :knoxx.docs/style-guide
   :publication/garden :gardens/legacy
   :publication/locale :en
   :publication/revision :source/current
   :publication/state :archived
   :publication/path "/style-guide"
   :translation/review :none})

(def mixed-topology-resources
  [translation-pipeline-document style-guide-document
   promethean-garden legacy-garden
   example-manifest-intent style-guide-fr-intent style-guide-archived-intent])

;; ── 1/2 valid-publication-path? ──────────────────────────────────────────

(deftest valid-publication-path?-accepts-rooted-route
  (is (true? (pub/valid-publication-path? "/translation-pipeline")))
  (is (true? (pub/valid-publication-path? "/docs/nested/route"))))

(deftest valid-publication-path?-rejects-malformed-routes
  (doseq [[label path-value]
          [["empty" ""]
           ["unrooted" "translation-pipeline"]
           ["query" "/docs?x=1"]
           ["fragment" "/docs#frag"]
           ["NUL" "/docs\u0000"]
           ["non-string" 42]]]
    (testing label
      (is (false? (pub/valid-publication-path? path-value)))
      (is (false? (m/validate pub/PublicationPath path-value))))))

;; ── 3 Document ────────────────────────────────────────────────────────────

(deftest document-shape-requires-source-locale
  (is (true? (m/validate pub/Document translation-pipeline-document)))
  (is (false? (m/validate pub/Document (dissoc translation-pipeline-document :document/source-locale))))
  (is (false? (m/validate pub/Document (assoc translation-pipeline-document :document/source-locale "en")))))

(deftest document-source-path-rejects-blank-strings
  (is (false? (m/validate pub/Document (assoc-in translation-pipeline-document [:document/source :path] ""))))
  (is (false? (m/validate pub/Document (assoc-in translation-pipeline-document [:document/source :path] "   ")))))

;; ── 4 Garden ──────────────────────────────────────────────────────────────

(deftest garden-shape-enumerates-status
  (is (true? (m/validate pub/Garden (assoc promethean-garden :garden/status :active))))
  (is (true? (m/validate pub/Garden (assoc promethean-garden :garden/status :archived))))
  (is (false? (m/validate pub/Garden (assoc promethean-garden :garden/status "active"))))
  (is (false? (m/validate pub/Garden (assoc promethean-garden :garden/status :deleted)))))

(deftest garden-locale-catalog-is-explicit-and-distinct
  (is (true? (m/validate pub/Garden promethean-garden)))
  (doseq [locales [nil [] [:en :en] [:locale/en]]]
    (is (false? (m/validate pub/Garden (assoc promethean-garden :garden/locales locales)))
        (pr-str locales))))

;; ── 5 PublicationIntentResource ──────────────────────────────────────────

(deftest publication-intent-resource-validates-relation
  (is (true? (m/validate pub/PublicationIntentResource example-manifest-intent)))
  (testing "missing garden ref"
    (is (false? (m/validate pub/PublicationIntentResource
                             (dissoc example-manifest-intent :publication/garden)))))
  (testing "non-keyword document ref"
    (is (false? (m/validate pub/PublicationIntentResource
                             (assoc example-manifest-intent :publication/document "knoxx.docs/translation-pipeline")))))
  (testing "publication/state as string"
    (is (false? (m/validate pub/PublicationIntentResource
                             (assoc example-manifest-intent :publication/state "published")))))
  (testing "malformed publication/path"
    (is (false? (m/validate pub/PublicationIntentResource
                             (assoc example-manifest-intent :publication/path "translation-pipeline"))))))

;; ── 6 revision ────────────────────────────────────────────────────────────

(deftest publication-intent-accepts-both-revision-forms
  (is (true? (m/validate pub/PublicationRevision "abc123")))
  (is (true? (m/validate pub/PublicationRevision :source/current)))
  (is (false? (m/validate pub/PublicationRevision nil)))
  (is (false? (m/validate pub/PublicationRevision 42))))

(deftest publication-revision-rejects-blank-strings
  (is (false? (m/validate pub/PublicationRevision "")))
  (is (false? (m/validate pub/PublicationRevision "   "))))

;; ── 7/8 hydrate-publication-intent ────────────────────────────────────────

(deftest hydrate-publication-intent-copies-document-source-locale
  (let [resource-index (pub/index-resources [translation-pipeline-document])
        hydrated (pub/hydrate-publication-intent resource-index example-manifest-intent)]
    (is (= :en (:document/source-locale hydrated)))
    (is (true? (m/validate pub/PublicationIntent hydrated)))))

(deftest hydrate-publication-intent-rejects-dangling-document
  (let [resource-index (pub/index-resources [])]
    (is (thrown? js/Error (pub/hydrate-publication-intent resource-index example-manifest-intent)))))

;; ── 9 admissible-publication? ─────────────────────────────────────────────

(deftest admissible-publication?-requires-active-garden
  (let [resource-index (pub/index-resources [translation-pipeline-document style-guide-document
                                              promethean-garden legacy-garden])]
    (is (true? (pub/admissible-publication? resource-index example-manifest-intent)))
    (is (false? (pub/admissible-publication? resource-index style-guide-archived-intent)))
    (is (false? (pub/admissible-publication?
                 resource-index
                 (assoc example-manifest-intent :publication/document :knoxx.docs/unknown))))
    (is (false? (pub/admissible-publication?
                  resource-index
                  (assoc example-manifest-intent :publication/garden :gardens/unknown))))))

(deftest admissible-publication?-requires-a-target-accepted-locale
  (let [resource-index (pub/index-resources [translation-pipeline-document promethean-garden])]
    (is (true? (pub/admissible-publication? resource-index example-manifest-intent)))
    (is (false? (pub/admissible-publication?
                 resource-index
                 (assoc example-manifest-intent :publication/locale :de))))
    (is (= :publication-locale-unsupported
           (pub/publication-locale-blocker
            resource-index
            (assoc example-manifest-intent :publication/locale :de))))))

(deftest admissible-publication?-rejects-archived-intent-state-with-an-active-garden
  (let [resource-index (pub/index-resources [translation-pipeline-document promethean-garden])]
    (is (false? (pub/admissible-publication?
                 resource-index
                 (assoc example-manifest-intent :publication/state :archived))))))

(deftest admissible-publication?-fails-closed-on-unrecognized-state
  (testing "only :published and :withheld reconcile"
    (is (= #{:published :withheld} pub/reconcilable-publication-states)))
  (let [resource-index (pub/index-resources [translation-pipeline-document promethean-garden])]
    (doseq [state [:published :withheld]]
      (testing (str "reconcilable state " state)
        (is (true? (pub/admissible-publication?
                    resource-index
                    (assoc example-manifest-intent :publication/state state))))))
    (doseq [[label state] [["archived" :archived]
                           ["unrecognized keyword" :deleted]
                           ["nil" nil]
                           ["string" "published"]]]
      (testing (str "non-reconcilable state: " label)
        (is (false? (pub/admissible-publication?
                     resource-index
                     (assoc example-manifest-intent :publication/state state))))))
    (testing "state key absent entirely"
      (is (false? (pub/admissible-publication?
                   resource-index
                   (dissoc example-manifest-intent :publication/state)))))))

;; ── 10 resource-only fixture ──────────────────────────────────────────────

(defn- source-text
  [relative-path]
  (.readFileSync node-fs (.join path (.cwd js/process) relative-path) "utf8"))

(deftest resource-fixture-describes-mixed-publication-topology
  (let [resource-index (pub/index-resources mixed-topology-resources)]
    (is (= 2 (count (:documents resource-index))))
    (is (= 2 (count (:gardens resource-index))))
    (is (= 3 (count (:publications resource-index))))
    (doseq [document [translation-pipeline-document style-guide-document]]
      (is (true? (m/validate pub/Document document))))
    (doseq [garden [promethean-garden legacy-garden]]
      (is (true? (m/validate pub/Garden garden))))
    (doseq [intent [example-manifest-intent style-guide-fr-intent style-guide-archived-intent]]
      (is (true? (m/validate pub/PublicationIntentResource intent))))
    (is (= #{:published :withheld :archived}
           (set (map :publication/state [example-manifest-intent style-guide-fr-intent style-guide-archived-intent]))))
    (is (= #{:es :fr :en}
           (set (map :publication/locale [example-manifest-intent style-guide-fr-intent style-guide-archived-intent]))))
    (testing "transitive requires carry no legacy publishing-backend segment"
      (let [law-require (-> (source-text "src/cljs/knoxx/backend/law/publication.cljs")
                             (str/split #"\n\n" 2)
                             first)
            legacy-backend-marker (str "open" "planner")]
        (is (not (str/includes? (str/lower-case law-require) legacy-backend-marker)))))))

;; ── 11 the materialized artifact ──────────────────────────────────────────

(def probe-artifact
  {:artifact/content "<!doctype html><p>Sonda</p>"
   :artifact/media-type "text/html"
   :artifact/encoding "utf-8"
   :artifact/locale :es
   :artifact/revision "rev-7f3a91c"})

(deftest publication-artifact-declares-content-media-type-encoding-locale-revision
  (is (true? (m/validate pub/PublicationArtifact probe-artifact)))
  (testing "bytes are content too — the difference from a string is only whether
            the renderer already applied the declared encoding"
    (is (true? (m/validate pub/PublicationArtifact
                           (assoc probe-artifact :artifact/content
                                  (.encode (js/TextEncoder.) "<p>Sonda</p>"))))))
  (testing "every field is load-bearing: an adapter that has to guess one of them
            is an adapter making a publication decision"
    (doseq [field [:artifact/content :artifact/media-type :artifact/encoding
                   :artifact/locale :artifact/revision]]
      (is (false? (m/validate pub/PublicationArtifact (dissoc probe-artifact field)))
          (str field " must be required"))))
  (testing "extra keys are allowed — an artifact may carry renderer provenance"
    (is (true? (m/validate pub/PublicationArtifact
                           (assoc probe-artifact :render/engine :markdown))))))

(deftest media-type-carries-no-charset-parameter
  (testing "accepted"
    (doseq [media-type ["text/html" "application/edn" "image/svg+xml" "text/plain"]]
      (is (true? (pub/valid-media-type? media-type)) media-type)))
  (testing "refused — a charset parameter duplicates :artifact/encoding, and two
            places to declare one encoding is two places that can disagree"
    (doseq [media-type ["text/html; charset=utf-8" "text/html charset=utf-8"
                        "texthtml" "text/" "/html" "" "  "]]
      (is (false? (pub/valid-media-type? media-type)) (pr-str media-type)))))

(deftest character-encoding-is-declared-not-guessed
  (doseq [encoding ["utf-8" "UTF-8" "iso-8859-1" "windows-1252"]]
    (is (true? (pub/valid-character-encoding? encoding)) encoding))
  (doseq [bad ["" "  " "utf 8" "utf-8; q=1"]]
    (is (false? (pub/valid-character-encoding? bad)) (pr-str bad))))

(deftest artifact-rejects-a-revision-selector-anywhere
  (testing "the whole value is walked, keys included, so a selector cannot ride
            along on a key the shape does not name"
    (doseq [[label value] [["the revision itself" {:artifact/revision :source/current}]
                           ["an extra key" {:render/from :source/current}]
                           ["nested in a map" {:render/provenance {:from :source/current}}]
                           ["nested in a vector" {:render/inputs [:source/current]}]
                           ["a map key" {:render/by {:source/current true}}]
                           ["a sibling nobody has invented yet"
                            {:render/from :source/head}]]]
      (is (false? (pub/free-of-revision-selectors? (merge probe-artifact value)))
          label)
      (is (false? (m/validate pub/PublicationArtifact (merge probe-artifact value)))
          label)))
  (testing "while a document's own :source key is not a selector — the namespace
            is what marks one, not the word"
    (is (true? (pub/free-of-revision-selectors?
                (assoc probe-artifact :document/source {:path "docs/probe.md"}))))))

(deftest artifact-revision-conflict-carries-both-revisions
  (is (nil? (pub/artifact-revision-conflict probe-artifact "rev-7f3a91c"))
      "agreement is not a conflict")
  (let [conflict (pub/artifact-revision-conflict probe-artifact "rev-other")]
    (is (= {:conflict/type :publication/artifact-revision-conflict
            :conflict/artifact-revision "rev-7f3a91c"
            :conflict/concrete-revision "rev-other"}
           conflict))
    (is (true? (pub/artifact-revision-conflict? conflict)))
    (is (true? (m/validate pub/ArtifactRevisionConflict conflict))))
  (testing "assert-artifact! returns the artifact on agreement and throws the
            conflict otherwise, with both revisions in ex-data"
    (is (= probe-artifact (pub/assert-artifact! probe-artifact "rev-7f3a91c")))
    (is (thrown-with-msg? js/Error #"revision conflict"
                          (pub/assert-artifact! probe-artifact "rev-other")))
    (is (= {:conflict/type :publication/artifact-revision-conflict
            :conflict/artifact-revision "rev-7f3a91c"
            :conflict/concrete-revision "rev-other"}
            (try (pub/assert-artifact! probe-artifact "rev-other")
                 nil
                 (catch :default err (ex-data err)))))))

(deftest artifact-locale-conflict-carries-both-locales
  (let [intent (assoc example-manifest-intent :publication/locale :es)
        conflict (pub/artifact-locale-conflict (assoc probe-artifact :artifact/locale :fr) intent)]
    (is (= :cross-check pub/artifact-locale-identity-decision))
    (is (= {:conflict/type :publication/artifact-locale-conflict
            :conflict/artifact-locale :fr
            :conflict/publication-locale :es}
           conflict))
    (is (true? (pub/artifact-locale-conflict? conflict)))
    (is (thrown-with-msg? js/Error #"locale conflict"
                          (pub/assert-artifact! (assoc probe-artifact :artifact/locale :fr)
                                                intent
                                                "rev-7f3a91c")))))
