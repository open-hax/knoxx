(ns knoxx.backend.domain.mailbox-address
  "Pure actor/session/conversation address resolution from explicit lineage."
  (:require [clojure.string :as str] [knoxx.backend.law.mailbox-store :as law]))
(defn- value [map keys] (some #(get map %) keys))
(defn- parent [lineage]
  (let [spec (:agent-spec lineage)]
    {:conversation-id (value spec [:parent-conversation-id :parentConversationId :parent_conversation_id])
     :session-id (value spec [:parent-session-id :parentSessionId :parent_session_id])
     :run-id (value spec [:parent-run-id :parentRunId :parent_run_id])}))
(defn resolve-target
  "Wire target strings select an address, never a tenant or actor authority."
  [{:keys [target target-type conversation-id session-id run-id]} lineage]
  (let [parts (str/split target #":" 2) kind (or (when (= 2 (count parts)) (first parts)) target-type)
        id (if (= 2 (count parts)) (second parts) target) base {:target target}
        resolved (cond (= target "self") (merge base (select-keys lineage [:conversation-id :session-id :run-id]))
                       (= target "parent") (merge base (parent lineage))
                       (contains? #{"actor" "actor-id" "actorid"} kind) (assoc base :actor-id id)
                       (contains? #{"session" "session-id" "sessionid"} kind) (assoc base :session-id id)
                       (contains? #{"conversation" "conversation-id" "conversationid"} kind) (assoc base :conversation-id id)
                       :else (law/refuse! 400 "mailbox_target_invalid" "Use an explicit actor, session, conversation, self or parent address"))]
    (cond-> (into {} (remove (comp nil? val)) resolved)
      conversation-id (assoc :conversation-id conversation-id) session-id (assoc :session-id session-id) run-id (assoc :run-id run-id))))
