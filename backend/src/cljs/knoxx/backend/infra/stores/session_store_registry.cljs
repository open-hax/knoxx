(ns knoxx.backend.infra.stores.session-store-registry
  "Holds the session store atom to avoid circular dependencies.
   Both bootstrap and openplanner-memory import this.")

(defonce session-store* (atom nil))