(ns knoxx.backend.shape.run-directory
  "Provider-neutral scoped listing, independent from session-specific active run reads.")

(defprotocol IRunDirectoryStore
  (list-runs [store scope]
    "Return visible durable snapshots for explicit {:org-id id} or {:all? true}."))
