(ns knoxx.backend.infra.routes.app.session-abort
  "Resolve and abort an explicitly selected operator session."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.time :as domain-time]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.extern.app-session-requests :as session-requests]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.stores.mongo-session-store :as session-store]))

(defn- ^:async abort-target!
  [{:keys [requested-conversation-id requested-session-id requested-run-id]}]
  (let [run (when-not (str/blank? requested-run-id) (get @run-state/runs* requested-run-id))
        session-id (or (some-> requested-session-id not-empty) (:session_id run))
        session (await (if (not (str/blank? (str session-id)))
                         (session-store/get-session session-id)
                         (js/Promise.resolve nil)))]
    {:conversation-id (str (or (some-> requested-conversation-id not-empty)
                              (:conversation_id run) (:conversation_id session) ""))
     :session-id (str (or session-id (:session_id session) ""))
     :run-id (str (or (not-empty requested-run-id) (:run_id run) (:run_id session) ""))}))

(defn- ^:async mark-aborted!
  [{:keys [session-id run-id]} reason]
  (when-not (str/blank? run-id)
    (run-state/update-run! run-id
                          (fn [run] (assoc run :status "aborted" :error reason
                                           :updated_at (domain-time/now-iso)))))
  (await (if (not (str/blank? session-id))
           (session-store/update-session! session-id
                                          {:status "aborted" :error reason :has_active_stream false})
           (js/Promise.resolve nil))))

(defn ^:async handle-admin-abort
  "Resolve the selected session, abort its turn and persist the operator result." [reply _ctx request]
  (let [command (session-requests/abort-command request)
        reason (:reason command)]
    (try
      (let [{:keys [conversation-id session-id run-id] :as target} (await (abort-target! command))]
        (if (str/blank? conversation-id)
          (infra-http/json-response! reply 400 {:ok false :error "conversation_id, session_id, or run_id is required"})
          (let [abort-result (await (turn-control/abort-active-turn! conversation-id reason))]
            (await (mark-aborted! target reason))
            (infra-http/json-response! reply 200
                            (assoc abort-result :ok true :conversation_id conversation-id
                                   :session_id session-id :run_id run-id :marked_aborted true)))))
      (catch :default err
        (infra-http/error-response! reply err 409)))))
