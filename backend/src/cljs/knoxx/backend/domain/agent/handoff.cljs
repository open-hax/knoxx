(ns knoxx.backend.domain.agent.handoff
  "Actor-owned delegation plans. Child authority comes only from named resources."
  (:require [clojure.string :as str]
            [knoxx.backend.law.handoff-evidence :as evidence]
            [malli.core :as m]))

(def Parameters
  "A handoff cannot supply a model, actor, prompt override, or tool policy."
  [:map {:closed true}
   [:agent {:description "Exact declared worker contract ID; not a model or actor."} [:string {:min 1 :max 160}]]
   [:message {:description "Bounded task, source evidence, allowed paths, and success check."} [:string {:min 1 :max 16000}]]])

(def default-status-wait-ms
  "One bounded observation gives a local model time to finish its prefill."
  180000)

(def StatusParameters
  "The opaque run identity returned by an accepted handoff."
  [:map {:closed true}
   [:run_id [:string {:min 1 :max 200}]]
   [:wait_ms {:optional true
              :description "Observation wait in milliseconds; default 180000, maximum 300000. A pending result is not completed work."}
    [:int {:min 0 :max 300000}]]])

(defn request-plan
  "Validate ownership and delegated target before producing a pinned child request."
  [actor-id actor target parameters]
  (when-not (m/validate Parameters parameters)
    (throw (ex-info "Invalid agent_dispatch arguments: supply only agent (declared worker contract ID) and message (bounded task). Do not use worker/task or override actor, model, thinking, or tools. No child was started."
                    {:status 400 :code "agent_handoff_parameters_invalid"})))
  (let [target-id (:agent parameters)
        allowed (set (get-in actor [:actor/metadata :delegates]))]
    (when-not (and (not (str/blank? actor-id))
                   (= actor-id (:actor/id actor))
                   (contains? allowed target-id)
                   (= target-id (:contract/id target))
                   (not= false (:enabled target))
                   (= :agent (:contract/kind target))
                   (some #{actor-id} (:contract/actors target)))
      (throw (ex-info "Agent handoff is outside this actor's delegation contract"
                      {:status 403 :code "agent_handoff_denied"})))
    {:message (:message parameters)
     :agent_spec {:contract_id target-id :actor_id actor-id}}))

(def AcceptedChild
  "The control API binds a queued child to its three runtime identities."
  [:map
   [:ok [:= true]]
   [:queued [:= true]]
   [:run_id [:string {:min 1}]]
   [:session_id [:string {:min 1}]]
   [:conversation_id [:string {:min 1}]]
   [:model {:optional true} [:maybe :string]]])

(defn issued-run
  "Record precisely the child that the captured session started."
  [owner accepted]
  (when-not (m/validate AcceptedChild accepted)
    (throw (ex-info "Child admission did not confirm its run, session and conversation"
                    {:status 502 :code "agent_handoff_acceptance_invalid"})))
  {:owner owner
   :run (assoc (select-keys accepted [:run_id :session_id :conversation_id :model])
               :actor_id (:actor-id owner) :status "queued" :record_pending true)})

(defn require-issued-run!
  "A scoped session can observe only a run it received from successful admission."
  [owner run-id issued]
  (when-not (and issued (= owner (:owner issued)) (= run-id (get-in issued [:run :run_id])))
    (throw (ex-info "Agent run was not issued to this acting session"
                    {:status 403 :code "agent_handoff_scope_denied"})))
  (:run issued))

(defn visible-result
  "Verify the persisted run still names the issued actor, run, session and conversation."
  [issued-run run]
  (let [identity-keys [:actor_id :run_id :session_id :conversation_id]]
    (when-not (= (select-keys issued-run identity-keys) (select-keys run identity-keys))
      (throw (ex-info "Agent run is outside the acting identity"
                      {:status 403 :code "agent_handoff_scope_denied"}))))
  (let [observations (evidence/project (:tool_receipts run))]
    (when-not (m/validate evidence/Evidence observations)
      (throw (ex-info "Worker tool evidence does not satisfy its bounded output contract"
                      {:status 500 :code "agent_handoff_evidence_invalid"})))
    (assoc (select-keys run [:run_id :status :model :updated_at :answer :error :record_pending])
           :tool_evidence observations)))

(defn terminal?
  "Whether a recorded child can no longer produce a new result in this run."
  [result]
  (contains? #{"completed" "failed" "cancelled"} (:status result)))
