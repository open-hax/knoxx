(ns knoxx.backend.infra.agent.recovery-coordinator
  "Recovery coordinator port over startup, periodic, explicit resume, and shutdown recovery."
  (:require [knoxx.backend.infra.agent.recovery :as recovery]
            [knoxx.backend.infra.agent.resume :as resume]))

(defprotocol IRecoveryCoordinator
  (recover-startup! [coordinator])
  (recover-session! [coordinator recovery-request])
  (resume-turn! [coordinator recovery-request])
  (shutdown-recovery! [coordinator]))

(defn startup-recover!
  [runtime app config]
  (resume/resume-on-process-startup! runtime app config))

(defn resume-recovered!
  [runtime config {:keys [session opts]}]
  (recovery/resume-recovered-session! runtime config session opts))

(defn stop-recovery!
  []
  (resume/stop-periodic-recovery!)
  (js/Promise.resolve {:stopped true}))

(defrecord AgentRecoveryCoordinator [runtime app config]
  IRecoveryCoordinator
  (recover-startup! [_]
    (startup-recover! runtime app config))

  (recover-session! [_ request]
    (resume-recovered! runtime config request))

  (resume-turn! [_ request]
    (resume-recovered! runtime config request))

  (shutdown-recovery! [_]
    (stop-recovery!)))

(defn recovery-coordinator
  [runtime app config]
  (->AgentRecoveryCoordinator runtime app config))
