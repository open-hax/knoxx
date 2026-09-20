(ns knoxx.backend.law.thread-recovery
  "Exact prior-instance recovery under the documented single-writer contract."
  (:require [knoxx.backend.law.thread-store :as thread]))

(defn snapshot
  "Discard only the facade's disposable cache timestamp; retain every authority field."
  [value]
  (with-meta (dissoc value :cached-at) nil))

(defn assert-release!
  "Historical unstamped records are orphans; age alone never proves a live owner dead."
  [current observed same-view? stamp]
  (thread/assert-valid! :recovery/snapshot thread/Thread observed)
  (when-not (and same-view? (= (snapshot current) (snapshot observed))
                 (= "running" (:status current))
                 (not= (:system_instance_id current) (:instance-id stamp)))
    (throw (ex-info "Recovery snapshot is no longer an eligible prior-instance owner"
                    {:status 409 :code "thread_recovery_conflict"})))
  current)
