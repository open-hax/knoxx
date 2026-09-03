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
  [:and
   [:map {:closed true}
    [:principal/user-id {:optional true} [:maybe NonBlankString]]
    [:principal/user-email {:optional true} [:maybe NonBlankString]]
    [:principal/membership-id {:optional true} [:maybe NonBlankString]]]
   [:fn {:error/message "a reviewer needs at least one durable identity"}
    #(some nonblank-string?
           [(:principal/user-id %)
            (:principal/user-email %)
            (:principal/membership-id %)])]])

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

(def TranslationExecutionSnapshot
  "The exact agent execution policy admitted before a provider can run."
  [:map {:closed true}
   [:translation-execution/agent-id NonBlankString]
   [:translation-execution/model NonBlankString]
   [:translation-execution/thinking NonBlankString]
   [:translation-execution/system-prompt NonBlankString]
   [:translation-execution/tool-ids [:vector NonBlankString]]
   [:translation-execution/tools-choice {:optional true} [:enum :required-first]]
   [:translation-execution/digest NonBlankString]])

(def TranslationMemoryStatus
  "Whether memory retrieval found examples, found none, or failed explicitly."
  [:enum :found :empty :failed])

(def TranslationMemorySnapshot
  "The exact reviewed examples pinned into one translation turn."
  [:and
   [:map {:closed true}
    [:translation-memory-snapshot/status TranslationMemoryStatus]
    [:translation-memory-snapshot/examples [:vector TranslationMemoryExample]]
    [:translation-memory-snapshot/error {:optional true} [:maybe NonBlankString]]]
   [:fn {:error/message "memory status must agree with its examples and error"}
    (fn [snapshot]
      (let [status (:translation-memory-snapshot/status snapshot)
            examples (:translation-memory-snapshot/examples snapshot)
            error (:translation-memory-snapshot/error snapshot)]
        (case status
          :found (and (seq examples) (nil? error))
          :empty (and (empty? examples) (nil? error))
          :failed (and (empty? examples) (nonblank-string? error))
          false)))]] )

(def TranslationTurnAdmission
  "One atomic, immutable pre-provider translation turn."
  [:map {:closed true}
   [:translation-turn/id NonBlankString]
   [:translation-turn/dispatch-key NonBlankString]
   [:translation-turn/run-id NonBlankString]
   [:translation-turn/admitted-at evidence/Instant]
   [:translation-turn/manifest SplitManifest]
   [:translation-turn/candidate-claim CandidateClaim]
   [:translation-turn/execution TranslationExecutionSnapshot]
   [:translation-turn/memory TranslationMemorySnapshot]])

(def EffectiveReviewHistories
  "Complete review histories keyed by the split identity they describe."
  [:map-of NonBlankString SplitReviewHistory])

(def EffectiveCandidateMember
  "One approved target member selected from immutable candidate and review facts."
  [:map {:closed true}
   [:effective-candidate/split-id NonBlankString]
   [:effective-candidate/split-index [:int {:min 0}]]
   [:effective-candidate/candidate-attempt-id NonBlankString]
   [:effective-candidate/candidate-digest NonBlankString]
   [:effective-candidate/review-receipt-id NonBlankString]
   [:effective-candidate/review-receipt-digest NonBlankString]
   [:effective-candidate/target-text NonBlankString]
   [:effective-candidate/target-digest NonBlankString]
   [:effective-candidate/correction-id {:optional true} [:maybe NonBlankString]]])

(def EffectiveCandidateSet
  "The complete reviewed target selected in manifest order.

  `:effective-candidate-set/content-digest` authenticates the composed target
  bytes. The separate lineage digest also binds the raw candidate set and every
  effective review receipt, so byte-identical re-review remains a new reviewed
  revision rather than silently inheriting prior publication authority."
  [:map {:closed true}
   [:effective-candidate-set/id NonBlankString]
   [:effective-candidate-set/manifest-id NonBlankString]
   [:effective-candidate-set/candidate-set-id NonBlankString]
   [:effective-candidate-set/candidate-set-digest NonBlankString]
   [:effective-candidate-set/revision evidence/ConcreteRevision]
   [:effective-candidate-set/digest NonBlankString]
   [:effective-candidate-set/content-digest NonBlankString]
   [:effective-candidate-set/text NonBlankString]
   [:effective-candidate-set/members
    [:vector {:min 1} EffectiveCandidateMember]]])

(def EffectiveCandidateNotReadyReason
  "Why one manifest split cannot yet contribute effective target bytes."
  [:enum :review-missing :review-in-review :review-rejected])

(def EffectiveCandidateNotReadySplit
  "A non-content-bearing explanation for one split blocking composition."
  [:map {:closed true}
   [:effective-candidate-not-ready/split-id NonBlankString]
   [:effective-candidate-not-ready/split-index [:int {:min 0}]]
   [:effective-candidate-not-ready/reason EffectiveCandidateNotReadyReason]
   [:effective-candidate-not-ready/review-status
    {:optional true} ReviewStatus]
   [:effective-candidate-not-ready/review-receipt-id
    {:optional true} NonBlankString]])

(def EffectiveCandidateSetRefusal
  "Typed evidence that composition is blocked without exposing partial bytes."
  [:map {:closed true}
   [:refusal/type [:= :effective-candidate-set/not-ready]]
   [:refusal/splits
    [:vector {:min 1} EffectiveCandidateNotReadySplit]]])

(def EffectiveCandidateSetResult
  "Ready effective content or a typed, content-free not-ready refusal."
  [:or
   [:map {:closed true}
    [:effective-candidate-set/status [:= :ready]]
    [:effective-candidate-set/value EffectiveCandidateSet]]
   [:map {:closed true}
    [:effective-candidate-set/status [:= :not-ready]]
    [:effective-candidate-set/refusal EffectiveCandidateSetRefusal]]])

(defn assert-valid!
  "Return `value` when it satisfies `schema`, otherwise throw named evidence."
  [what schema value]
  (if (m/validate schema value)
    value
    (throw (ex-info (str "invalid " (name what))
                    {:what what
                     :errors (-> (m/explain schema value) me/humanize)}))))
