(ns knoxx.backend.infra.openplanner.scope
  "Explicit organization scope for internal OpenPlanner service calls."
  (:require [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.law.local-openplanner :as law]))

(defn org-id!
  "Return the explicitly bound organization or fail before provider effects."
  [config]
  (:org_id (law/scope! {:org_id (:openplanner-org-id config)})))

(defn scoped-config
  "Bind verified request authority, replacing any ambient configuration scope."
  [config ctx]
  (when-not ctx (throw (ex-info "Authentication is required" {:status 401 :code "authentication_required"})))
  (let [org-id (:org_id (law/scope! {:org_id (authz/ctx-org-id ctx)}))]
    (assoc config :openplanner-org-id org-id)))

(defn session-options
  "Build protocol options without trusting query-string tenant aliases."
  [config]
  {:org_id (org-id! config) :project (:session-project-name config)})
