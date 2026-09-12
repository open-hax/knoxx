(ns knoxx.backend.infra.source-authoring-store
  "Immutable full-source facts and digest/CAS reference semantics."
  (:require [knoxx.backend.infra.publication-source-revision :as revisions]
            [knoxx.backend.law.source-authoring :as law]
            [knoxx.backend.law.source-review :as review]))

(defprotocol ISourceAuthoringStore
  (source-events! [store scope] "Read the complete scoped immutable source history.")
  (admit-source! [store scope expected-revision event] "Admit one exact operation against the prior source digest."))

(defn- refuse! [code message] (throw (ex-info message {:status 409 :code code})))
(defn- validate-event! [scope event]
  (review/assert-valid! :source-authoring/scope review/Scope scope)
  (review/assert-valid! :source-authoring/event law/Event event)
  (when-not (= scope (:source/scope event)) (refuse! "source_authoring_scope_mismatch" "Source scope differs"))
  (when-not (= (:document scope) (get-in event [:source/document :document/id]))
    (refuse! "source_authoring_scope_mismatch" "Source document differs"))
  (when-not (= (:org-id scope) (get-in event [:source/document :document/org-id]))
    (refuse! "source_authoring_owner_required" "Source facts require explicit owning organization"))
  (when-not (= (:source/revision event) (revisions/content-revision (:source/content event)))
    (refuse! "source_authoring_digest_mismatch" "Source digest does not match canonical content")) event)
(defn- append-event [events scope event]
  (validate-event! scope event)
  (when-not (= (:source/previous-revision event) (:source/revision (peek events)))
    (refuse! "source_authoring_stale_revision" "Source changed; refresh before saving"))
  (when (and (seq events) (not= :save (:source/action event)))
    (refuse! "source_authoring_transition_refused" "Only a save may follow an admitted source"))
  (when (and (empty? events) (= :save (:source/action event)))
    (refuse! "source_authoring_observation_required" "Observe the current source before saving"))
  (when (and (seq events) (not= (:source/document (peek events)) (:source/document event)))
    (refuse! "source_authoring_document_conflict" "Saving content cannot replace document metadata"))
  (conj events event))
(defn validated-history
  "Replay digest, identity, scope and predecessor obligations before exposing source facts."
  [scope events]
  (review/assert-valid! :source-authoring/scope review/Scope scope)
  (when-not (vector? events) (refuse! "source_authoring_history_invalid" "Source history must be ordered"))
  (reduce (fn [history event]
            (when (some #(= (:source/id event) (:source/id %)) history)
              (refuse! "source_authoring_duplicate_operation" "Source operation already exists"))
            (append-event history scope event)) [] events))
(defn admit-in-state [state scope expected-revision event]
  (validate-event! scope event)
  (when-not (= expected-revision (:source/previous-revision event))
    (refuse! "source_authoring_stale_revision" "Source predecessor differs from requested CAS"))
  (let [events (validated-history scope (get state scope []))
        existing (some #(when (= (:source/id event) (:source/id %)) %) events)]
    (if existing
      (if (= (dissoc existing :source/recorded-at) (dissoc event :source/recorded-at))
        [state {:existing? true :event existing}]
        (refuse! "source_authoring_operation_conflict" "Source operation identity has different content"))
      [(assoc state scope (append-event events scope event)) {:existing? false :event event}])))
(defrecord MemorySourceAuthoringStore [state]
  ISourceAuthoringStore
  (source-events! [_ scope] (validated-history scope (get @state scope [])))
  (admit-source! [_ scope expected-revision event]
    (let [[next-state result] (admit-in-state @state scope expected-revision event)]
      (reset! state next-state) result)))
(defn memory-store "Create a disposable full-source reference projection." []
  (->MemorySourceAuthoringStore (atom {})))
