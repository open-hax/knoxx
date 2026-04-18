(ns kms-ingestion.epistemic
  "Epistemic kernel — shared Malli schemas for fact/obs/inference/attestation/judgment.
   .cljc so JVM (ingestion/Knoxx backend) and Node (openplanner-core) share one schema.

   Every other system emits one of these records.
   Contracts, actors, events, receipts are controllers over this store."
  (:require [malli.core :as m]
            [malli.error :as me]))

;; ---------------------------------------------------------------------------
;; Confidence scalar
;; ---------------------------------------------------------------------------

(def Confidence
  [:double {:min 0.0 :max 1.0}])

;; ---------------------------------------------------------------------------
;; Primitive schemas
;; ---------------------------------------------------------------------------

(def Fact
  [:map
   [:kind       [:= :fact]]
   [:ctx        :keyword]          ;; asserting principal (己 = self)
   [:claim      :any]              ;; the proposition
   [:src        :any]              ;; provenance: event-id, actor-id, URL …
   [:p          Confidence]
   [:time       inst?]])

(def Obs
  [:map
   [:kind       [:= :obs]]
   [:ctx        :keyword]          ;; who perceived it
   [:about      :any]              ;; what was sensed
   [:signal     :any]              ;; raw signal shape
   [:p          Confidence]])

(def EvidenceRef
  [:map
   [:kind  [:enum :fact :obs :inference]]
   [:ref   :string]])              ;; e.g. "fact/actor-discord-patrol-001"

(def Inference
  [:map
   [:kind   [:= :inference]]
   [:from   [:vector EvidenceRef]]  ;; evidence chain
   [:rule   :keyword]              ;; contract-id that fired
   [:actor  :keyword]              ;; who executed the contract
   [:claim  :any]                  ;; derived proposition
   [:p      Confidence]])

(def Attestation
  [:map
   [:kind     [:= :attestation]]
   [:actor    :keyword]            ;; who is attesting
   [:did      :any]                ;; what they claim they did
   [:run-id   uuid?]
   [:causedby {:optional true} uuid?]  ;; what triggered the run
   [:p        Confidence]])

(def Judgment
  [:map
   [:kind    [:= :judgment]]
   [:of      uuid?]                ;; which inference or attestation
   [:verdict [:enum :held :failed :partial]]
   [:auditor :keyword]             ;; who or what judged
   [:p       Confidence]])

;; ---------------------------------------------------------------------------
;; Actor binding
;; ---------------------------------------------------------------------------

(def ActorBinding
  [:map
   [:actor/id          :string]
   [:actor/user-id     :string]
   [:actor/org-id      :string]
   [:actor/role        :keyword]
   [:actor/status      [:enum :active :suspended :archived]]
   [:actor/contract-id :string]])

;; ---------------------------------------------------------------------------
;; Union — any epistemic record
;; ---------------------------------------------------------------------------

(def EpistemicRecord
  [:multi {:dispatch :kind}
   [:fact        Fact]
   [:obs         Obs]
   [:inference   Inference]
   [:attestation Attestation]
   [:judgment    Judgment]])

;; ---------------------------------------------------------------------------
;; Validation helpers
;; ---------------------------------------------------------------------------

(defn valid?
  "Returns true if record conforms to EpistemicRecord."
  [record]
  (m/validate EpistemicRecord record))

(defn explain
  "Returns human-readable error for an invalid record, nil if valid."
  [record]
  (when-not (valid? record)
    (me/humanize (m/explain EpistemicRecord record))))

(defn validate!
  "Throws ex-info if record is invalid."
  [record]
  (when-let [err (explain record)]
    (throw (ex-info "Invalid epistemic record" {:errors err :record record})))
  record)
