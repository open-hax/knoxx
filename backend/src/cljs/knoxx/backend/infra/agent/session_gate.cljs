(ns knoxx.backend.infra.agent.session-gate
  "Reconcile direct-spawn requests with existing live or orphaned sessions."
  (:require [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.extern.session-recovery :as host]
            [knoxx.backend.infra.agent.session :as session]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]
            [knoxx.backend.infra.system-instance :as system-instance]
            [knoxx.backend.shape.agent :as agent-shape]))

(def DISPATCH_RECLAIM_COOLDOWN_MS "Minimum spacing between orphaned-session reclaim attempts." 60000)

(defn- runtime-owns-live-run?
  "True when this process is actively executing work for the conversation."
  [conversation-id]
  (let [agent-session (session/active-agent-session conversation-id)]
    (or (and agent-session (agent-shape/streaming? agent-session))
        (some? (turn-control/active-turn conversation-id)))))

(defn- session-cold?
  "True when the document is old enough that a live-but-unregistered run
   (e.g. one orphaned in-memory by a hot reload) cannot plausibly own it."
  [session]
  (>= (- (host/now-ms) (host/session-updated-ms session))
      DISPATCH_RECLAIM_COOLDOWN_MS))

(defn- ^:async reclaim-orphaned-session!
  "Mark a running session document that no live run owns as failed so the
   pending dispatch can proceed. Returns true on success; failures are
   logged and the caller falls through to the normal busy error."
  [body session reason]
  (try
    (host/log-reclaim! (:session-id body) reason)
    (await (session-store/complete-session!
            (str (:session-id body))
            (str (or (:conversation-id body) ""))
            {:status "failed"
             :error (str "Session reclaimed by dispatch: " reason)
             :messages (:messages session)}))
    true
    (catch :default err
      (host/log-failure! err)
      false)))

(defn- ^:async reclaim-and-dispatch!
  "Reclaim an orphaned running session, then dispatch the pending turn."
  [runtime config body session reason dispatch!]
  (if (await (reclaim-orphaned-session! body session reason))
    (await (dispatch! runtime config body))
    (await (host/reject! "agent_already_processing: orphaned session reclaim failed"))))

(defn ^:async dispatch-with-session-gate!
  "Resolve the session busy-gate for a direct spawn.

   running + stamped by a previous system instance  → reclaim, dispatch
   running + no live run here + document gone cold  → reclaim, dispatch
   running + live run in this instance              → busy error
   otherwise                                        → dispatch"
  [runtime config body dispatch!]
  (let [session (await (session-store/get-session (:session-id body)))
        can-send-result (session-store/session-can-send? session)
        conversation-id (:conversation-id body)
        agent-session (session/active-agent-session conversation-id)]
    (cond
      (:can-send can-send-result)
      (if (and agent-session (agent-shape/streaming? agent-session))
        (await (host/reject! "agent_already_processing: active stream"))
        (await (dispatch! runtime config body)))

      (not (system-instance/owned-by-current-instance? session))
      (await (reclaim-and-dispatch! runtime config body session
                                    "owned by previous system instance (restart)" dispatch!))

      (and (not (runtime-owns-live-run? conversation-id))
           (session-cold? session))
      (await (reclaim-and-dispatch! runtime config body session
                                    "no live run in current system instance" dispatch!))

      :else
      (await (host/reject! (str "agent_already_processing: "
                              (:reason can-send-result)))))))
