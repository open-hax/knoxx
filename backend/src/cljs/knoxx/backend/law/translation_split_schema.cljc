(ns knoxx.backend.law.translation-split-schema
  "Closed data contracts for durable translation splits and review memory."
  (:require [clojure.string :as str]
            [knoxx.backend.law.publication-locale :as locale]
            [knoxx.backend.law.translation-evidence :as evidence]
            [malli.core :as m]
            [malli.error :as me]))

(defn nonblank-string?
  "Whether `value` is a string containing non-whitespace text."
  [value]
  (and (string? value) (not (str/blank? value))))

(def NonBlankString
  "A string containing non-whitespace text."
  [:fn nonblank-string?])

(def SourceSplit
  "One immutable source member in a split manifest."
  [:map {:closed true}
   [:split/id NonBlankString]
   [:split/index [:int {:min 0}]]
   [:split/source-start [:int {:min 0}]]
   [:split/source-end [:int {:min 1}]]
   [:split/source-text NonBlankString]
   [:split/source-digest NonBlankString]])

(def SplitManifest
  "The exact ordered split set for one resource-backed translation input."
  [:map {:closed true}
   [:split-manifest/id NonBlankString]
   [:split-manifest/org-id NonBlankString]
   [:split-manifest/project {:optional true} [:maybe NonBlankString]]
   [:split-manifest/garden :qualified-keyword]
   [:split-manifest/document :qualified-keyword]
   [:split-manifest/source-locale locale/Locale]
   [:split-manifest/target-locale locale/Locale]
   [:split-manifest/source-revision evidence/ConcreteRevision]
   [:split-manifest/source-digest NonBlankString]
   [:split-manifest/splits [:vector {:min 1} SourceSplit]]])

(def CandidateClaimMember
  "One pre-provider attempt assignment for an admitted source split."
  [:map {:closed true}
   [:candidate-claim-member/attempt-id NonBlankString]
   [:candidate-claim-member/split-id NonBlankString]
   [:candidate-claim-member/split-index [:int {:min 0}]]
   [:candidate-claim-member/source-digest NonBlankString]])

(def CandidateClaim
  "The immutable split attempts admitted before one provider turn starts."
  [:map {:closed true}
   [:candidate-claim/id NonBlankString]
   [:candidate-claim/manifest-id NonBlankString]
   [:candidate-claim/revision evidence/ConcreteRevision]
   [:candidate-claim/members [:vector {:min 1} CandidateClaimMember]]])

(def CandidateSplit
  "One immutable candidate produced for a pre-admitted source split."
  [:map {:closed true}
   [:candidate/attempt-id NonBlankString]
   [:candidate/split-id NonBlankString]
   [:candidate/split-index [:int {:min 0}]]
   [:candidate/source-digest NonBlankString]
   [:candidate/text NonBlankString]
   [:candidate/digest NonBlankString]])

(def CandidateSet
  "A complete candidate revision composed from one split manifest."
  [:map {:closed true}
   [:candidate-set/id NonBlankString]
   [:candidate-set/manifest-id NonBlankString]
   [:candidate-set/claim-id NonBlankString]
   [:candidate-set/revision evidence/ConcreteRevision]
   [:candidate-set/digest NonBlankString]
   [:candidate-set/text NonBlankString]
   [:candidate-set/members [:vector {:min 1} CandidateSplit]]])

(def ReviewStatus
  "The canonical state derived from a segment review's overall judgment."
  [:enum :approved :in-review :rejected])

(def QualityRating
  "Adequacy and fluency ratings retained from the historical review card."
  [:enum "excellent" "good" "adequate" "poor" "unusable"])

(def TerminologyRating
  "Terminology judgment retained from the historical review card."
  [:enum "correct" "minor_errors" "major_errors"])

(def RiskRating
  "Safety judgment retained from the historical review card."
  [:enum "safe" "sensitive" "policy_violation"])

(def OverallJudgment
  "The review action from which effective candidate status is derived."
  [:enum "approve" "needs_edit" "reject"])

(def ReviewPrincipal
  "Server-authenticated reviewer identity; never accepted inside a review request."
  [:map {:closed true}
   [:principal/user-id NonBlankString]
   [:principal/user-email {:optional true} [:maybe NonBlankString]]
   [:principal/membership-id {:optional true} [:maybe NonBlankString]]])

(def SplitReviewRequest
  "The closed judgment and correction shape a review client may submit."
  [:map {:closed true}
   [:review/operation-id NonBlankString]
   [:review/adequacy QualityRating]
   [:review/fluency QualityRating]
   [:review/terminology TerminologyRating]
   [:review/risk RiskRating]
   [:review/overall OverallJudgment]
   [:review/corrected-text {:optional true} [:maybe NonBlankString]]
   [:review/editor-notes {:optional true} [:maybe NonBlankString]]])

(def SplitReviewReceipt
  "One immutable, candidate-bound segment review receipt."
  [:map {:closed true}
   [:review/id NonBlankString]
   [:review/digest NonBlankString]
   [:review/operation-id NonBlankString]
   [:review/status ReviewStatus]
   [:review/source-revision evidence/ConcreteRevision]
   [:review/manifest-id NonBlankString]
   [:review/candidate-set-id NonBlankString]
   [:review/candidate-set-digest NonBlankString]
   [:review/candidate-revision evidence/ConcreteRevision]
   [:review/split-id NonBlankString]
   [:review/candidate-attempt-id NonBlankString]
   [:review/candidate-digest NonBlankString]
   [:review/principal ReviewPrincipal]
   [:review/recorded-at evidence/Instant]
   [:review/adequacy QualityRating]
   [:review/fluency QualityRating]
   [:review/terminology TerminologyRating]
   [:review/risk RiskRating]
   [:review/overall OverallJudgment]
   [:review/corrected-text {:optional true} [:maybe NonBlankString]]
   [:review/correction-id {:optional true} [:maybe NonBlankString]]
   [:review/editor-notes {:optional true} [:maybe NonBlankString]]])

(def SplitReviewHistory
  "The complete candidate-split receipt history supplied to effective selection."
  [:vector SplitReviewReceipt])

(def TranslationMemoryExample
  "One approved effective source/target pair available to later translation."
  [:map {:closed true}
   [:translation-memory/id NonBlankString]
   [:translation-memory/org-id NonBlankString]
   [:translation-memory/project {:optional true} [:maybe NonBlankString]]
   [:translation-memory/garden :qualified-keyword]
   [:translation-memory/document :qualified-keyword]
   [:translation-memory/source-locale locale/Locale]
   [:translation-memory/target-locale locale/Locale]
   [:translation-memory/manifest-id NonBlankString]
   [:translation-memory/source-revision evidence/ConcreteRevision]
   [:translation-memory/source-digest NonBlankString]
   [:translation-memory/candidate-set-id NonBlankString]
   [:translation-memory/candidate-set-digest NonBlankString]
   [:translation-memory/candidate-revision evidence/ConcreteRevision]
   [:translation-memory/split-id NonBlankString]
   [:translation-memory/split-source-digest NonBlankString]
   [:translation-memory/candidate-attempt-id NonBlankString]
   [:translation-memory/candidate-digest NonBlankString]
   [:translation-memory/source-text NonBlankString]
   [:translation-memory/target-text NonBlankString]
   [:translation-memory/correction-id {:optional true} [:maybe NonBlankString]]
   [:translation-memory/review-receipt-id NonBlankString]])

(defn assert-valid!
  "Return `value` when it satisfies `schema`, otherwise throw named evidence."
  [what schema value]
  (if (m/validate schema value)
    value
    (throw (ex-info (str "invalid " (name what))
                    {:what what
                     :errors (-> (m/explain schema value) me/humanize)}))))
