(ns knoxx.backend.shape.startup-admission
  "Provider-owned compare-and-set capability for one partial startup attempt.")

(defprotocol IStartupAdmission
  (startup-view [store id]
    "Capture an opaque provider preimage; only this provider may interpret it.")
  (claim-startup! [store record view]
    "Admit this attempt only from the exact preimage and permitted prior owner.")
  (settle-startup! [store record view]
    "Fail this token or fence its unchanged preimage; never mutate a successor."))
