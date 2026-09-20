(ns knoxx.backend.infra.agent.initial-admission
  "Release unadmitted startup resources without replacing the admission error."
  (:require [knoxx.backend.domain.action.run-state :as state]
            [knoxx.backend.domain.error-observatory :as errors]
            [knoxx.backend.infra.agent.session :as sessions]
            [knoxx.backend.infra.agent.run-admission :as admission]))

(defn- release! [{:keys [conversation-id startup-owner sink]}]
  (try
    (state/clear-event-stream-sink-if! sink)
    (finally
      (sessions/settle-startup-session! conversation-id startup-owner false))))

(defn- release-observed! [{:keys [conversation-id] :as context}]
  (try
    (release! context)
    {:ok true :startup/cleanup :released}
    (catch :default _cleanup-error
      (try
        (errors/log-error! :agent-turn/initial-admission-cleanup-failed
                           {:conversation-id conversation-id}
                           (ex-info "Initial admission cleanup failed" {:code "initial_cleanup_failed"}))
        {:ok false :code "initial_cleanup_failed" :diagnostic-emitted true}
        (catch :default _logging-error
          {:ok false :code "initial_cleanup_failed" :diagnostic-emitted false})))))

(defn- refuse! [context failure]
  (when-let [failed* (:startup-failed* context)] (reset! failed* true))
  (release-observed! context)
  (throw failure))

(defn ^:async construct-session!
  "Return the session while live, or a classified cleanup receipt after refusal.
   Promise.all already observes this continuation after its first rejection."
  [context operation!]
  (let [session (await (operation!))]
    (if (some-> context :startup-failed* deref)
      (release-observed! context)
      session)))

(defn ^:async hydrate!
  "Release currently owned startup resources promptly on the first hydration refusal."
  [context operation!]
  (try (await (operation!)) (catch :default failure (refuse! context failure))))

(defn ^:async admit!
  "Promote owned startup resources only after initial durable admissions succeed.
   Cleanup diagnostics are classified; a cleanup failure never masks the first refusal."
  [{:keys [conversation-id startup-owner] :as context} persist!]
  (try
    (let [result (await (persist!))]
      (sessions/settle-startup-session! conversation-id startup-owner true)
      result)
    (catch :default failure (refuse! context failure))))

(defn ^:async create-run!
  "Keep startup facts and the pre-prompt continuation inside one ownership boundary."
  ([context arguments] (await (create-run! context arguments (fn []))))
  ([context arguments before-prompt!]
   (await (admit! context
                  (fn [] (apply admission/create-initial-run! (conj (vec arguments) before-prompt!)))))))
