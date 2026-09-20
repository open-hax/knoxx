(ns knoxx.backend.domain.source-review
  "Pure admission, replay and acceptance decisions for source content."
  (:require [clojure.string :as str]
            [knoxx.backend.law.source-review :as law]))

(defn head
  "The last admitted operation identity, or nil for a new document."
  [events]
  (:review/id (peek events)))

(defn revision-events
  "Select facts for exactly these source bytes and declared locale."
  [events revision source-locale]
  (filterv #(and (= revision (:review/revision %))
                 (= source-locale (:review/source-locale %))) events))

(defn status
  "Comments retain workflow state; only explicit review actions change it."
  [events]
  (reduce (fn [current event]
            (case (:review/action event)
              :submit :in-review
              :request-changes :needs-revision
              :accept :accepted
              current))
          :draft events))

(defn accepted?
  "Acceptance never transfers across revisions or source languages."
  [events revision source-locale]
  (= :accepted (status (revision-events events revision source-locale))))

(defn- refuse!
  [code message]
  (throw (ex-info message {:status 409 :code code})))

(defn- assert-transition!
  [events event]
  (let [current (status (revision-events events (:review/revision event)
                                        (:review/source-locale event)))
        action (:review/action event)]
    (when-not (case action
                :comment true
                :submit (contains? #{:draft :needs-revision} current)
                :request-changes (contains? #{:in-review :accepted} current)
                :accept (= :in-review current)
                false)
      (refuse! "source_review_transition_refused"
               "source review action is not admissible in its current state"))
    (when (and (= :request-changes action)
               (str/blank? (:review/notes event))
               (empty? (:review/corrections event)))
      (refuse! "source_review_feedback_required"
               "requesting changes requires notes or a correction"))
    (when (and (= :accept action) (seq (:review/corrections event)))
      (refuse! "source_review_unapplied_correction"
               "apply proposed corrections as a new source revision before acceptance"))))

(defn append-event
  "Admit one causally consecutive event. Providers call this inside their CAS."
  [events scope event]
  (law/assert-valid! :source-review/scope law/Scope scope)
  (law/assert-valid! :source-review/event law/Event event)
  (when-not (= scope (:review/scope event))
    (refuse! "source_review_scope_mismatch" "source review scope does not match"))
  (when (some #(= (:review/id %) (:review/id event)) events)
    (refuse! "source_review_duplicate_operation" "source review operation already exists"))
  (when-not (= (head events) (:review/previous event))
    (refuse! "source_review_stale_head" "source review changed; refresh before submitting"))
  (assert-transition! events event)
  (conj events event))

(defn validated-history
  "Replay every scoped causal fact; malformed or reordered histories fail closed."
  [scope events]
  (law/assert-valid! :source-review/scope law/Scope scope)
  (when-not (vector? events)
    (refuse! "source_review_invalid_history" "source review history must be ordered"))
  (reduce #(append-event %1 scope %2) [] events))

(defn same-operation?
  "An exact retry preserves its causal predecessor; only server time may differ."
  [existing attempted]
  (= (dissoc existing :review/recorded-at)
     (dissoc attempted :review/recorded-at)))

(defn event-for-command
  "Normalize optional command data before identity comparison or durable admission."
  [scope actor command recorded-at]
  (law/assert-valid! :source-review/scope law/Scope scope)
  (law/assert-valid! :source-review/actor law/Actor actor)
  (law/assert-valid! :source-review/command law/Command command)
  (law/assert-valid!
   :source-review/event law/Event
   {:review/id (:operation-id command)
    :review/scope scope :review/actor actor
    :review/revision (:revision command)
    :review/source-locale (:source-locale command)
    :review/previous (:expected-head command)
    :review/action (:action command)
    :review/notes (or (:notes command) "")
    :review/corrections (or (:corrections command) [])
    :review/lessons (or (:lessons command) [])
    :review/recorded-at recorded-at}))

(defn decide
  "Return an exact installed retry or a validated fresh event for atomic admission."
  [events scope actor snapshot command recorded-at]
  (law/assert-valid! :source-review/snapshot law/Snapshot snapshot)
  (when-not (= (:document scope) (:document snapshot))
    (refuse! "source_review_scope_mismatch" "source document does not match review scope"))
  (let [history (validated-history scope events)
        event (event-for-command scope actor command recorded-at)]
    (if-let [existing (some #(when (= (:review/id %) (:review/id event)) %) history)]
      (if (same-operation? existing event)
        {:existing? true :event existing}
        (refuse! "source_review_operation_conflict"
                 "source review operation identity has different content"))
      (do
        (when-not (= [(:revision snapshot) (:source-locale snapshot)]
                     [(:revision command) (:source-locale command)])
          (refuse! "source_review_stale_revision"
                   "source content changed; review the current revision"))
        (append-event history scope event)
        {:existing? false :event event}))))

(defn- acceptance-cutoffs
  [events]
  (reduce-kv (fn [indexes index event]
               (let [coordinate [(:review/revision event) (:review/source-locale event)]]
                 (case (:review/action event)
                   :accept (assoc indexes coordinate index)
                   :request-changes (dissoc indexes coordinate)
                   :submit (dissoc indexes coordinate)
                   indexes)))
             {} events))

(defn learned-lessons
  "Acceptance endorses earlier scoped reviewer feedback in its language, with provenance."
  [events]
  (let [accepted-through (acceptance-cutoffs events)
        language-cutoffs (reduce-kv (fn [indexes [_revision locale] index]
                                      (update indexes locale #(max (or % -1) index)))
                                    {} accepted-through)]
    (into []
          (mapcat (fn [[index event]]
                    ;; Request-changes is a reviewer action. Its lessons can
                    ;; inform corrected source; comments on other revisions
                    ;; remain unreviewed. Ledger order is the causal boundary,
                    ;; not wall-clock time or an inferred revision ancestry.
                    (when-let [cutoff (if (= :request-changes (:review/action event))
                                       (get language-cutoffs (:review/source-locale event))
                                       (get accepted-through [(:review/revision event)
                                                              (:review/source-locale event)]))]
                      (when (<= index cutoff)
                        (let [acceptance (nth events cutoff)]
                          (mapv (fn [lesson]
                                  {:text lesson :review (:review/id event)
                                   :revision (:review/revision event)
                                   :actor (:review/actor event)
                                   :acceptance-review (:review/id acceptance)
                                   :acceptance-revision (:review/revision acceptance)})
                                (:review/lessons event)))))))
          (map-indexed vector events))))

(defn project
  "Project current source state while keeping all immutable review history visible."
  [events scope snapshot]
  (law/assert-valid! :source-review/snapshot law/Snapshot snapshot)
  (when-not (= (:document scope) (:document snapshot))
    (refuse! "source_review_scope_mismatch" "source document does not match review scope"))
  (let [history (validated-history scope events)
        current (revision-events history (:revision snapshot) (:source-locale snapshot))]
    (assoc snapshot
           :head (head history)
           :status (status current)
           :accepted (= :accepted (status current))
           :stale (boolean (and (seq history) (empty? current)))
           :history history
           :lessons (learned-lessons history))))
