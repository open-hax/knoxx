(ns knoxx.backend.extern.session-recovery
  "Native timing, diagnostics and rejection at the existing session-recovery boundary.")

(defn session-updated-ms
  "Decode the last readable session update or creation timestamp."
  [session]
  (let [at (or (:updated_at session) (:created_at session))]
    (cond (number? at) at
          (string? at) (let [ms (.getTime (js/Date. at))] (if (js/isNaN ms) 0 ms))
          :else 0)))

(defn now-ms "Read the current recovery clock." [] (js/Date.now))
(defn log-reclaim! "Report the existing orphan-reclaim diagnostic." [session-id reason]
  (js/console.warn "[agent-runner] reclaiming orphaned session" (str session-id) "-" reason))
(defn log-failure! "Report an existing orphan-reclaim failure." [error]
  (js/console.warn "[agent-runner] orphan reclaim failed:" error))
(defn reject! "Return the existing busy-gate native rejection." [message]
  (js/Promise.reject (js/Error. message)))
