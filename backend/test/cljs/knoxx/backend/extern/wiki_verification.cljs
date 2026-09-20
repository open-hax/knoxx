(ns knoxx.backend.extern.wiki-verification
  "Verification-only native readers for canonical Clio memory and published routes."
  (:require ["node:fs" :as fs]
            ["node:path" :as path]
            [clojure.string :as str]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.infra.clio-translation-split-store :as clio-splits]
            [knoxx.backend.infra.translation-split-store :as splits]
            [knoxx.backend.law.publication-manifest :as manifest]
            [knoxx.backend.shape.resource-identity :as identity]))

(defn- ensure! [condition message data]
  (when-not condition (throw (ex-info message data))))

(defn- checked-input [native-input]
  (let [input (js->clj native-input :keywordize-keys true)]
    (doseq [key [:document :publication :garden :locale :firstCandidateSetId :secondCandidateSetId
                 :firstSourceRevision :secondSourceRevision :acceptedReviewId :acceptedSplitId
                 :sourceText :acceptedCorrection]]
      (ensure! (and (string? (get input key)) (not (str/blank? (get input key))))
               "Missing required canonical memory expectation" {:field key}))
    (ensure! (not= (:firstCandidateSetId input) (:secondCandidateSetId input))
             "Memory verification requires distinct candidates" {})
    (ensure! (not= (:firstSourceRevision input) (:secondSourceRevision input))
             "Memory verification requires distinct source revisions" {})
    input))

(defn- existing-store! [directory]
  (ensure! (and (string? directory) (.isAbsolute path directory))
           "An absolute existing split ledger directory is required" {})
  (let [file (.join path directory "events.edn") schemas (.join path directory "schemas")]
    (ensure! (.existsSync fs file) "Required translation split ledger is missing" {:file file})
    (ensure! (.isFile (.statSync fs file)) "Translation split ledger is not a file" {:file file})
    (ensure! (.existsSync fs schemas) "Required translation split schemas are missing" {:directory schemas})
    (ensure! (.isDirectory (.statSync fs schemas)) "Translation split schemas are not a directory" {:directory schemas})
    (clio-splits/open! {:directory directory :digest-hex crypto/sha256-hex})))

(defn- scope [turn]
  (select-keys (:translation-turn/manifest turn)
               [:split-manifest/org-id :split-manifest/project :split-manifest/document
                :split-manifest/garden :split-manifest/source-locale :split-manifest/target-locale]))

(defn- check-turns! [input first-turn second-turn]
  (ensure! (and first-turn second-turn) "Both candidate sets must resolve to canonical turns" {})
  (ensure! (not= (:translation-turn/id first-turn) (:translation-turn/id second-turn))
           "Canonical turn identities must differ" {})
  (ensure! (= (scope first-turn) (scope second-turn)) "Canonical turn scope changed" {})
  (let [first-manifest (:translation-turn/manifest first-turn)
        second-manifest (:translation-turn/manifest second-turn)]
    (doseq [[key expected] [[:split-manifest/document (keyword (:document input))]
                            [:split-manifest/garden (keyword (:garden input))]
                            [:split-manifest/target-locale (keyword (:locale input))]
                            [:split-manifest/source-revision (:firstSourceRevision input)]]]
      (ensure! (= expected (get first-manifest key)) "First canonical turn does not match the requested work" {:field key}))
    (ensure! (= (:secondSourceRevision input) (:split-manifest/source-revision second-manifest))
             "Second canonical turn does not match the new source revision" {})))

(defn- memory-expectation [input]
  {:translation-memory/document (keyword (:document input))
   :translation-memory/garden (keyword (:garden input))
   :translation-memory/target-locale (keyword (:locale input))
   :translation-memory/source-revision (:firstSourceRevision input)
   :translation-memory/candidate-set-id (:firstCandidateSetId input)
   :translation-memory/review-receipt-id (:acceptedReviewId input)
   :translation-memory/split-id (:acceptedSplitId input)
   :translation-memory/source-text (:sourceText input)
   :translation-memory/target-text (:acceptedCorrection input)})

(defn- accepted-memory! [input second-turn]
  (let [memory (:translation-turn/memory second-turn)
        expected (memory-expectation input)
        matching (filter #(= expected (select-keys % (keys expected)))
                         (:translation-memory-snapshot/examples memory))]
    (ensure! (= :found (:translation-memory-snapshot/status memory))
             "Second turn did not admit found translation memory" {})
    (ensure! (= 1 (count matching))
             "Second turn lacks one exact immutable accepted-correction reference" {:matches (count matching)})
    (first matching)))

(defn- ^:async accepted-review! [store input]
  (let [history (await (splits/review-history-for-split!
                       store (:firstCandidateSetId input) (:acceptedSplitId input)))
        receipt (some #(when (= (:acceptedReviewId input) (:review/id %)) %) history)]
    (ensure! (and receipt (= :approved (:review/status receipt))
                  (= (:acceptedCorrection input) (:review/corrected-text receipt)))
             "Accepted correction does not match its canonical approved review receipt" {})
    receipt))

(defn- memory-wire [example]
  {:memoryId (:translation-memory/id example)
   :candidateSetId (:translation-memory/candidate-set-id example)
   :candidateDigest (:translation-memory/candidate-digest example)
   :reviewId (:translation-memory/review-receipt-id example)
   :splitId (:translation-memory/split-id example)
   :manifestId (:translation-memory/manifest-id example)
   :sourceRevision (:translation-memory/source-revision example)
   :sourceText (:translation-memory/source-text example)
   :targetText (:translation-memory/target-text example)
   :targetSha256 (crypto/sha256-hex (:translation-memory/target-text example))})

(defn ^:async read-memory
  "Read existing Clio facts and prove the second turn admitted the exact approved correction."
  [directory native-input]
  (let [input (checked-input native-input)
        store (existing-store! directory)
        first-turn (await (splits/turn-for-candidate-set! store (:firstCandidateSetId input)))
        second-turn (await (splits/turn-for-candidate-set! store (:secondCandidateSetId input)))]
    (check-turns! input first-turn second-turn)
    (await (accepted-review! store input))
    (let [example (accepted-memory! input second-turn)]
      (clj->js {:verified true :firstTurnId (:translation-turn/id first-turn)
                :secondTurnId (:translation-turn/id second-turn)
                :firstSourceRevision (:firstSourceRevision input)
                :secondSourceRevision (:secondSourceRevision input)
                :document (:document input) :garden (:garden input) :locale (:locale input)
                :memoryStatus "found" :memory (memory-wire example)}))))

(defn- route-wire [route]
  {:path (:route/path route) :artifact (:route/artifact route)
   :mediaType (:route/media-type route) :revision (:route/revision route)
   :publication (identity/encode-keyword (:publication/id route))})

(defn read-manifest
  "Return only canonical admitted routes; absence is empty and corrupt EDN fails closed."
  [root]
  (ensure! (and (string? root) (.isAbsolute path root)) "An absolute content root is required" {})
  (let [file (.join path root "manifest.edn")
        text (try (.readFileSync fs file "utf8")
                  (catch :default error
                    (if (= "ENOENT" (.-code error)) nil (throw error))))]
    (clj->js (if (nil? text) [] (mapv route-wire (:manifest/routes (manifest/edn->manifest text)))))))
