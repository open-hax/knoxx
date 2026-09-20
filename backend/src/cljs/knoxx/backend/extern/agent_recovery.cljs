(ns knoxx.backend.extern.agent-recovery
  "Timer, promise settlement and local diagnostic boundary for session recovery.")

(defn log-failure!
  "Keep provider/transport errors local, outside persisted conversation state."
  [session-id conversation-id error]
  (js/console.error "[knoxx] recovered session failed"
                    #js {:sessionId session-id :conversationId conversation-id :error (str error)}))

(defn report-failure!
  "Return a sanitized diagnostic receipt even when the local logger itself is unavailable."
  [session-id conversation-id error]
  (try
    (log-failure! session-id conversation-id error)
    {:code "recovery_launch_failed" :diagnostic-emitted true}
    (catch :default _logging-error
      {:code "recovery_logging_failed" :diagnostic-emitted false})))

(defn ^:async observe-launch!
  "Observe post-kickoff rejection without an unhandled promise or stale thread write."
  [launch session-id conversation-id]
  (try (await launch)
       (catch :default error (report-failure! session-id conversation-id error))))

(defn ^:async wait-for-kickoff!
  "Await a registered turn or completed launch, with bounded and always-cleared timers."
  [active? launch]
  (let [timer* (atom nil) timeout* (atom nil)]
    (try
      (await (js/Promise.
               (fn [resolve reject]
                 (reset! timer* (js/setInterval #(when (active?) (resolve true)) 25))
                 (reset! timeout* (js/setTimeout #(reject (ex-info "Recovered session kickoff timed out"
                                                                 {:status 504 :code "recovery_kickoff_timeout"})) 5000))
                 ((^:async fn [] (try (await launch) (resolve true)
                                     (catch :default error (reject error)))))
                 (when (active?) (resolve true)))))
      (finally (js/clearInterval @timer*) (js/clearTimeout @timeout*)))))
