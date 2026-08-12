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
   :garden/status :active})

(def legacy-garden
  {:garden/id :gardens/legacy
   :garden/title "Legacy"
   :garden/status :archived})

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

;; ── 4 Garden ──────────────────────────────────────────────────────────────

(deftest garden-shape-enumerates-status
  (is (true? (m/validate pub/Garden (assoc promethean-garden :garden/status :active))))
  (is (true? (m/validate pub/Garden (assoc promethean-garden :garden/status :archived))))
  (is (false? (m/validate pub/Garden (assoc promethean-garden :garden/status "active"))))
  (is (false? (m/validate pub/Garden (assoc promethean-garden :garden/status :deleted)))))

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
