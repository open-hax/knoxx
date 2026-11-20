(ns knoxx.backend.infra.openplanner.knowledge-scope
  "Resolve the actor-scoped memory principal from trusted runtime/session context."
  (:require [knoxx.backend.domain.contracts.loader :as contracts]
            [knoxx.backend.infra.auth.authz :as authz]))

(defn principal
  "A role-derived default actor is not a binding; session actor config is server-owned."
  [config auth-context]
  (let [actor-id (or (:graph-actor-id config) (:memory-actor-id auth-context)
                     (authz/ctx-actor-binding auth-context))
        actor (when actor-id
                (some (fn [record]
                        (when (= actor-id (get-in record [:contract :actor/id])) (:contract record)))
                      (contracts/load-all-contracts-sync config)))
        org-slug (or (:actor/org actor) (authz/ctx-org-slug auth-context))
        binding (authz/ctx-actor-binding auth-context)]
    (when (and binding (not= binding actor-id) (not (authz/system-admin? auth-context)))
      (throw (ex-info "Configured actor is outside the authenticated binding"
                      {:status 403 :code "actor_scope_denied"})))
    (cond-> {:actor-id actor-id :org-slug org-slug}
      (= org-slug (authz/ctx-org-slug auth-context))
      (assoc :org-id (authz/ctx-org-id auth-context)))))

(defn context
  "Keep existing permissions while pinning the memory principal to this tool instance."
  [config auth-context]
  (assoc (or auth-context {}) :memory-principal (principal config auth-context)
         :memory-legacy-unscoped? (nil? auth-context)))
