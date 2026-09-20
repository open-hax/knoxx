(ns knoxx.backend.shape.thread-recovery
  "Opaque read receipts for conditional recovery of one persisted conversation.")

(def view-key ::view)
(def owner-key ::owner)

(defprotocol IThreadRecovery
  (release-recovery! [store observed]
    "Release only this exact eligible persisted snapshot; refuse stale or unreceipted input."))
