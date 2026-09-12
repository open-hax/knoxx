(ns knoxx.backend.law.translation-dispatch
  "Contracts for turning the publication gate's derived translation work into
  ingestion-worker input, and the worker's answer back into evidence.

  `dispatch_key` crosses into the worker for exact ambiguous-send recovery; the
  concrete source revision deliberately does not. Knoxx binds revision, locales,
  tenant, attempt and returned batch locally, then refuses stale or mismatched
  completion reports at that join. Dispatch attempts and completed translations
  remain separate facts: only the latter become publication evidence.

  Portable by mandate; the client, store, and clock stay at the runtime edge."
  (:require [clojure.string :as str]
            [knoxx.backend.law.publication-locale :as locale]
            [knoxx.backend.law.translation-dispatch-report :as report]
            [knoxx.backend.law.translation-evidence :as evidence]
            [malli.core :as m]
            [malli.error :as me]))

(def NonBlankString
  "Re-exported so a dispatch caller depends on one law namespace."
  evidence/NonBlankString)

(def ConcreteRevision
  "Re-exported for the same reason as `NonBlankString`. Anything keyed by a
   revision needs this rather than a bare string."
  evidence/ConcreteRevision)

(def Instant
  "Re-exported so a dispatch record and a translation receipt cannot disagree
   about what a timestamp is."
  evidence/Instant)

(defn assert-valid!
  "Return `value` when it satisfies `schema`; otherwise throw a named contract
   violation."
  [contract-id schema value]
  (if (m/validate schema value)
    value
    (throw
     (ex-info (str "Translation dispatch contract violation: " contract-id)
              {:contract contract-id
               :errors (me/humanize (m/explain schema value))}))))

;; ── Derived work ───────────────────────────────────────────────────────────

(def DerivedWork
  "The closed work item derived by the publication gate."
  [:map {:closed true}
   [:document :qualified-keyword]
   [:locale locale/Locale]
   [:revision ConcreteRevision]
   [:replace-stale? :boolean]])

(def DispatchContext
  "Closed resource and principal coordinates required for dispatch.
   Work cannot silently invent organization, membership or garden."
  [:map {:closed true}
   [:dispatch/garden NonBlankString]
   [:dispatch/document-wire-id NonBlankString]
   [:dispatch/source-locale locale/Locale]
   [:dispatch/org-id NonBlankString]
   [:dispatch/membership-id NonBlankString]
   [:dispatch/project {:optional true} [:maybe NonBlankString]]
   [:dispatch/source-digest {:optional true} [:maybe NonBlankString]]])

;; ── Dispatch identity ──────────────────────────────────────────────────────

(def key-dimensions
  "Everything that changes *which translation* is being asked for.

   Organization, project and garden belong because output is stored and consumed
   in all three scopes. Membership stays out because it identifies who asked,
   not the translated artifact."
  [:org-id :project :garden :document :source-locale :locale :revision])

(defn dispatch-key
  "Identify one concrete translation request within its tenant.

   Printable coordinates are deterministic and inspectable. Refuse selectors or
   absent organization/garden instead of minting stable-looking keys for moving
   or unscoped work."
  [{:keys [org-id project garden document source-locale locale revision]}]
  (when-not (m/validate ConcreteRevision revision)
    (throw (ex-info "translation dispatch key requires a concrete revision"
                    {:document document :revision revision})))
  (when-not (m/validate NonBlankString org-id)
    (throw (ex-info "translation dispatch key requires an organization"
                    {:document document :org-id org-id})))
  (when-not (m/validate NonBlankString garden)
    (throw (ex-info "translation dispatch key requires a garden"
                    {:document document :garden garden})))
  (->> [org-id project garden document source-locale locale revision]
       (mapv pr-str)
       (str/join "|")))

(defn wire-resource-id
  "Decode a qualified wire id such as `docs/site` without a JS dependency."
  [value]
  (let [[namespace-part name-part] (str/split (str value) #"/" 2)]
    (when (and (seq namespace-part) (seq name-part))
      (keyword namespace-part name-part))))

;; ── Worker input ───────────────────────────────────────────────────────────

(def WorkerRequest
  "Portable mirror of CreateTranslationBatchRequest, pinned by tests.
   Exactly one document prevents a batch from joining different source revisions."
  [:map {:closed true}
   [:garden_id NonBlankString]
   [:target_lang NonBlankString]
   [:document_ids [:and [:vector NonBlankString]
                   [:fn {:error/message "a revision-bound batch carries exactly one document"}
                    #(= 1 (count %))]]]
   [:source_lang NonBlankString]
   [:org_id NonBlankString]
   [:membership_id NonBlankString]
   [:dispatch_key NonBlankString]
   [:project {:optional true} [:maybe NonBlankString]]])

(defn worker-request
  "Map validated work and scope into one revision-bound worker request.
   Language keywords become wire tags with name, never printed EDN."
  [work context]
  (assert-valid!
   :translation-dispatch/worker-request
   WorkerRequest
   (cond-> {:garden_id (:dispatch/garden context)
            :target_lang (name (:locale work))
            :document_ids [(:dispatch/document-wire-id context)]
            :source_lang (name (:dispatch/source-locale context))
            :org_id (:dispatch/org-id context)
            :membership_id (:dispatch/membership-id context)
            :dispatch_key (dispatch-key
                           {:org-id (:dispatch/org-id context)
                            :project (:dispatch/project context)
                            :garden (:dispatch/garden context)
                            :document (:document work)
                            :source-locale (:dispatch/source-locale context)
                            :locale (:locale work)
                            :revision (:revision work)})}
     (some? (:dispatch/project context))
     (assoc :project (:dispatch/project context)))))

;; ── Dispatch records ───────────────────────────────────────────────────────

(def outcomes
  "Distinct attempt states: accepted remains in flight, never completed evidence."
  #{:dispatch/accepted
    :dispatch/duplicate
    :dispatch/rejected
    :dispatch/failed
    :dispatch/completed
    :dispatch/unreachable})

(def Outcome
  "One dispatch outcome."
  (into [:enum] (sort outcomes)))

(def recovery-reasons
  "Closed reasons for reopening an otherwise terminal claim.
   Persisted with the replacement so the exceptional transition remains inspectable."
  #{:candidate-unavailable})

(def RecoveryReason
  "One validated reason for exceptionally reopening a terminal claim."
  (into [:enum] (sort recovery-reasons)))

(def unreachable-outcome
  "Terminal because this claim's source revision can no longer be produced.
   A source/current intent resolves a fresh key when the source changes."
  :dispatch/unreachable)

(def retriable-outcomes
  "Outcomes a later pass may replace with a fresh attempt.

   Failed or rejected attempts produced no translation and remain actionable.
   Completed, duplicate and unreachable outcomes are terminal except for the
   explicit candidate-unavailable recovery in `replaceable-claim?`."
  #{:dispatch/failed :dispatch/rejected})

(defn retriable?
  "Whether an existing claim with this outcome may be replaced by a new attempt."
  [outcome]
  (contains? retriable-outcomes outcome))

(defn terminal?
  "Whether this outcome is settled for ordinary dispatch.

   A completed claim remains terminal here even though `replaceable-claim?` may
   admit the explicit candidate-unavailable recovery transition."
  [outcome]
  (and (contains? outcomes outcome)
       (not (retriable? outcome))
       (not= :dispatch/accepted outcome)))

(def DispatchRecord
  "An immutable attempted translation with mutable settlement fields.
   Historical missing attempt/membership identities remain readable; new claims
   validate both before persistence."
  [:map
   [:dispatch/key NonBlankString]
   ;; Historical records predate attempt identity. Reads continue to admit those
   ;; rows so the rollout does not strand an in-flight claim, but every newly
   ;; constructed record below requires one. The token distinguishes successive
   ;; attempts that intentionally reuse the same logical dispatch key.
   [:dispatch/attempt-id {:optional true} NonBlankString]
   ;; Historical claims remain readable; only scoped claims can emit new agent work.
   [:dispatch/membership-id {:optional true} NonBlankString]
   [:dispatch/outcome Outcome]
   [:dispatch/org-id NonBlankString]
   [:dispatch/project {:optional true} [:maybe NonBlankString]]
   [:dispatch/garden :qualified-keyword]
   [:dispatch/document :qualified-keyword]
   [:dispatch/document-wire-id NonBlankString]
   [:dispatch/source-locale locale/Locale]
   [:dispatch/locale locale/Locale]
   [:dispatch/revision ConcreteRevision]
   [:dispatch/at Instant]
   [:dispatch/source-digest {:optional true} [:maybe NonBlankString]]
   [:dispatch/recovery-reason {:optional true} RecoveryReason]
   [:dispatch/batch-id {:optional true} [:maybe NonBlankString]]
   [:dispatch/detail {:optional true} [:maybe :string]]])

(defn assert-record!
  "Validate a dispatch record before it is persisted, and again when read back."
  [record]
  (assert-valid! :translation-dispatch/record DispatchRecord record))

(def mutable-record-keys
  "Fields that may change while one immutable dispatch attempt is in flight.

   Kept in the law so in-memory and durable stores compare the exact same
   attempt. Everything else, including an optional historical absence of
   `:dispatch/attempt-id`, is part of the attempt binding."
  [:dispatch/outcome :dispatch/batch-id :dispatch/detail])

(defn attempt-binding
  "Return immutable attempt coordinates, including a historical token absence.
   A replacement has a fresh attempt identity, so delayed settlement loses."
  [record]
  (apply dissoc (assert-record! record) mutable-record-keys))

(defn same-attempt?
  "Whether two records describe the exact same immutable dispatch attempt."
  [left right]
  (= (attempt-binding left) (attempt-binding right)))

(defn- record-coordinates [work context outcome at attempt-id]
  {:dispatch/key (dispatch-key
                           {:org-id (:dispatch/org-id context)
                            :project (:dispatch/project context)
                            :garden (:dispatch/garden context)
                            :document (:document work)
                            :source-locale (:dispatch/source-locale context)
                            :locale (:locale work)
                            :revision (:revision work)})
            :dispatch/attempt-id attempt-id
            :dispatch/outcome outcome
            :dispatch/org-id (:dispatch/org-id context)
            :dispatch/membership-id (:dispatch/membership-id context)
            :dispatch/garden (wire-resource-id (:dispatch/garden context))
            :dispatch/document (:document work)
            :dispatch/document-wire-id (:dispatch/document-wire-id context)
            :dispatch/source-locale (:dispatch/source-locale context)
            :dispatch/locale (:locale work)
            :dispatch/revision (:revision work)
            :dispatch/at at})

(defn dispatch-record
  "Build a dispatch record for one new attempt. Pure: identity and time are
   supplied, not sampled here.

   `attempt-id` is mandatory for newly constructed records. The schema keeps it
   optional solely so claims persisted before attempt-bound settlement remain
   readable and recoverable during rollout. New records always carry membership,
   so record validation refuses missing membership before persistence."
  [work context outcome at & {:keys [attempt-id batch-id detail recovery-reason]}]
  (when-not (m/validate NonBlankString attempt-id)
    (throw (ex-info "new translation dispatch records require an attempt id"
                    {:dispatch/attempt-id attempt-id})))
  (assert-record!
   (cond-> (record-coordinates work context outcome at attempt-id)
     (some? (:dispatch/project context))
     (assoc :dispatch/project (:dispatch/project context))

     (some? (:dispatch/source-digest context))
     (assoc :dispatch/source-digest (:dispatch/source-digest context))

     (some? recovery-reason)
     (assoc :dispatch/recovery-reason recovery-reason)

     (some? batch-id) (assoc :dispatch/batch-id batch-id)
     (some? detail) (assoc :dispatch/detail detail))))

(defn replaceable-claim?
  "Allow a fresh attempt only after failure or explicit candidate recovery.
   Key equality prevents replacing a different logical translation claim."
  [proposed existing]
  (and (= :dispatch/accepted (:dispatch/outcome proposed))
       (= (:dispatch/key proposed) (:dispatch/key existing))
       (or (retriable? (:dispatch/outcome existing))
           (and (contains? #{:dispatch/completed :dispatch/duplicate}
                           (:dispatch/outcome existing))
                (= :candidate-unavailable
                   (:dispatch/recovery-reason proposed))))))

;; ── The worker's answers ───────────────────────────────────────────────────

(def BatchCreated
  "Worker report contract, retained at the dispatch facade."
  report/BatchCreated)

(def batch-statuses
  "Worker report contract, retained at the dispatch facade."
  report/batch-statuses)

(def BatchStatusReport
  "Worker report contract, retained at the dispatch facade."
  report/BatchStatusReport)

(def refusal-types
  "Worker report contract, retained at the dispatch facade."
  report/refusal-types)

(def Refusal
  "Worker report contract, retained at the dispatch facade."
  report/Refusal)

(def BatchView
  "Worker report contract, retained at the dispatch facade."
  report/BatchView)

(defn failed-document-id
  "Apply the worker report law through the established dispatch API."
  [value]
  (report/failed-document-id value))

(defn report-batch-id
  "Apply the worker report law through the established dispatch API."
  [report-data]
  (report/report-batch-id report-data))

(defn completion-refusal
  "Apply the worker report law through the established dispatch API."
  [record report-data]
  (report/completion-refusal record report-data))

(defn output-revision
  "Apply the worker report law through the established dispatch API."
  [record]
  (report/output-revision assert-valid! record))

(defn batch-created-after?
  "Apply the worker report law through the established dispatch API."
  [batch at]
  (report/batch-created-after? batch at))

(defn pin-refusal
  "Apply the worker report law through the established dispatch API."
  [work context]
  (report/pin-refusal work context))

(defn batch-matches-dispatch?
  "Apply the worker report law through the established dispatch API."
  [batch context work at]
  (report/batch-matches-dispatch? batch context work at))

(defn batch-document-outcome
  "Apply the worker report law through the established dispatch API."
  [batch document-wire-id]
  (report/batch-document-outcome batch document-wire-id))

(defn source-drift-refusal
  "Apply the worker report law through the established dispatch API."
  [record observed-digest]
  (report/source-drift-refusal record observed-digest))

(defn translation-receipt
  "Mint a receipt through the existing explicit public arities."
  ([record produced-revision at]
   (report/translation-receipt record produced-revision at))
  ([record produced-revision at content-digest]
   (report/translation-receipt record produced-revision at content-digest))
  ([record produced-revision at content-digest split-evidence]
   (report/translation-receipt record produced-revision at content-digest split-evidence)))
