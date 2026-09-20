(ns knoxx.backend.infra.auth.authz
  (:require [clojure.string :as str]
            [knoxx.backend.domain.wiki-capabilities :as capabilities]
            [knoxx.backend.extern.axxium :as transport]
            [knoxx.backend.infra.auth.session :as auth-session]
            [knoxx.backend.infra.identity :as identity]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.runtime.state :as runtime-state]))

(defn policy-db
  [_runtime]
  (runtime-state/current-policy-db))

(defn policy-db-enabled?
  [runtime]
  (some? (policy-db runtime)))

(defn ^:async policy-db-promise
  [runtime reply status promise]
  (if-not (policy-db-enabled? runtime)
    (transport/handler-result (http/json-response! reply 503 {:detail "Knoxx policy database is not configured"}))
    (try
      (let [result (await promise)]
        (await (transport/handler-result (http/json-response! reply status result))))
      (catch :default err
        (await (transport/handler-result (http/error-response! reply err)))))))

(defn ^:async resolve-request-context!
  [runtime request]
  (await (auth-session/resolve-auth-context request (policy-db runtime))))

(defn ^:async with-request-context!
  "Resolve auth context and call (f ctx). Returns a promise.
   When f is an ^:async fn, await works inside it."
  [runtime request reply f]
  (try
    (let [ctx (await (resolve-request-context! runtime request))]
      (await (transport/handler-result (f ctx))))
    (catch :default err
      (await (transport/handler-result (http/error-response! reply err))))))

(defn current-context!
  "Refresh authority for long-lived commands and change streams."
  [runtime ctx]
  (identity/current-context! (policy-db runtime) ctx))

(defn ctx-org-id [ctx] (or (:org-id ctx) (:orgId ctx) (get-in ctx [:org :id])))
(defn ctx-org-slug [ctx] (or (:org-slug ctx) (:orgSlug ctx) (get-in ctx [:org :slug])))
(defn ctx-user-id [ctx] (or (:user-id ctx) (:userId ctx) (get-in ctx [:user :id])))
(defn ctx-user-email [ctx] (or (:user-email ctx) (:userEmail ctx) (get-in ctx [:user :email])))
(defn ctx-membership-id [ctx] (or (:membership-id ctx) (:membershipId ctx) (get-in ctx [:membership :id])))
(defn ctx-actor-id [ctx] (or (:actor-id ctx) (:actorId ctx) (get-in ctx [:membership :actor-id]) (get-in ctx [:actor :id])))

(defn ctx-actor-binding
  "The membership's stored actor id, or nil when none is assigned.

   Distinct from ctx-actor-id, which falls back to a role-derived default and so
   is never nil — it answers \"which actor label applies\", not \"was an actor
   assigned\". Only the binding may decide authority: a token minted from the
   default would carry credential scope for system_admin or workspace_user that
   nobody granted, and clearing the stored id would not revoke it, because the
   default takes over and still matches."
  [ctx]
  (or (:actor-binding ctx) (:actorBinding ctx) (get-in ctx [:actor :binding])))

(defn ctx-role-slugs
  [ctx]
  (into #{}
        (or (:role-slugs ctx)
            (:roleSlugs ctx)
            (keep :slug (:roles ctx)))))

(defn primary-context-role
  [ctx]
  (or (first (:role-slugs ctx))
      (first (:roleSlugs ctx))
      (some->> (:roles ctx) (map :slug) first)
      "knowledge_worker"))

(def ^:private system-admin-slugs #{"system_admin" "system-admin"})

(defn system-admin?
  [ctx]
  (boolean (some system-admin-slugs (ctx-role-slugs ctx))))

(defn ctx-permissions
  [ctx]
  (into #{} (or (:permissions ctx) [])))

(defn ctx-permitted?
  [ctx permission]
  (contains? (ctx-permissions ctx) permission))

(defn ctx-any-permission?
  [ctx permissions]
  (boolean (some #(ctx-permitted? ctx %) permissions)))

(defn ctx-tool-effect
  [ctx tool-id]
  (some (fn [policy]
          (when (= (str (or (:tool-id policy) (:toolId policy))) (str tool-id))
            (:effect policy)))
        (or (:tool-policies ctx) (:toolPolicies ctx))))

(defn ctx-tool-policy
  [ctx tool-id]
  (some (fn [policy]
          (when (= (str (or (:tool-id policy) (:toolId policy))) (str tool-id))
            policy))
        (or (:tool-policies ctx) (:toolPolicies ctx))))

(defn ctx-tool-constraints
  [ctx tool-id]
  (or (:constraints (ctx-tool-policy ctx tool-id))
      {}))

(defn ctx-tool-allowed?
  [ctx tool-id]
  (or (system-admin? ctx)
      (= "allow" (ctx-tool-effect ctx tool-id))))

(defn permission-allowed?
  "The permission decision enforced by commands, including the administrator role."
  [ctx permission]
  (or (system-admin? ctx) (ctx-permitted? ctx permission)))

(defn ensure-permission!
  [ctx permission]
  (when-not (permission-allowed? ctx permission)
    (throw (http/http-error 403 "permission_denied" (str "Permission '" permission "' is required"))))
  ctx)

(defn ensure-capability!
  "Authorize a declared Wiki capability through its application permission."
  [ctx capability]
  (if-let [permission (capabilities/permission capability)]
    (ensure-permission! ctx permission)
    (throw (http/http-error 403 "capability_denied" "Unknown capability"))))

(defn ensure-tool!
  "Enforce tool access for request-scoped endpoints.

   Prefer this over ensure-permission! for endpoints that gate on tool ids
   like `multimodal.upload`.

   NOTE: system_admin bypasses tool policy checks." 
  [ctx tool-id]
  (when-not (or (system-admin? ctx)
                (ctx-tool-allowed? ctx tool-id))
    (throw (http/http-error 403 "tool_denied" (str "Tool '" tool-id "' is required"))))
  ctx)

(defn ensure-any-permission!
  [ctx permissions code message]
  (when-not (or (system-admin? ctx)
                (ctx-any-permission? ctx permissions))
    (throw (http/http-error 403 code message)))
  ctx)

(defn ensure-org-scope!
  [ctx org-id permission]
  (ensure-permission! ctx permission)
  (when-not (or (system-admin? ctx)
                (= (str (ctx-org-id ctx)) (str org-id)))
    (throw (http/http-error 403 "org_scope_denied" "Requested org is outside the current Knoxx scope")))
  ctx)

(defn record-org-id [record] (or (:org-id record) (:orgId record) (:org_id record)))
(defn record-user-id [record] (or (:user-id record) (:userId record) (:user_id record)))
(defn record-user-email [record] (or (:user-email record) (:userEmail record) (:user_email record)))
(defn record-membership-id [record] (or (:membership-id record) (:membershipId record) (:membership_id record)))
(defn record-actor-id [record] (or (:actor-id record) (:actorId record) (:actor_id record)))

(defn principal-match?
  [ctx record]
  (let [ctx-membership (str (or (ctx-membership-id ctx) ""))
        record-membership (str (or (record-membership-id record) ""))
        ctx-user (str (or (ctx-user-id ctx) ""))
        record-user (str (or (record-user-id record) ""))
        ctx-actor (str (or (ctx-actor-id ctx) ""))
        record-actor (str (or (record-actor-id record) ""))
        actor-bound? (not (str/blank? record-actor))
        actor-match? (or (not actor-bound?) (= ctx-actor record-actor))
        user-bound? (or (not (str/blank? record-membership))
                        (not (str/blank? record-user)))]
    (cond
      (not actor-match?) false
      (system-admin? ctx) true
      (and (not (str/blank? ctx-membership))
           (not (str/blank? record-membership)))
      (= ctx-membership record-membership)
      (and (not (str/blank? ctx-user))
           (not (str/blank? record-user)))
      (= ctx-user record-user)
      :else
      (and actor-bound?
           (not user-bound?)
           (= ctx-actor record-actor)))))

(defn auth-snapshot
  [ctx]
  {:org_id (ctx-org-id ctx)
   :org_slug (ctx-org-slug ctx)
   :user_id (ctx-user-id ctx)
   :user_email (ctx-user-email ctx)
   :membership_id (ctx-membership-id ctx)
   :actor_id (ctx-actor-id ctx)
   :role_slugs (vec (ctx-role-slugs ctx))
   :permissions (vec (or (:permissions ctx) []))
   :tool_policies (vec (or (:tool-policies ctx) (:toolPolicies ctx) []))
   :membership_tool_policies (vec (or (:membership-tool-policies ctx) (:membershipToolPolicies ctx) []))
   :is_system_admin (boolean (or (:is-system-admin ctx) (:isSystemAdmin ctx)))})

(defn auth-snapshot-has-principal?
  [snapshot]
  (boolean (or (:org_id snapshot)
               (:user_id snapshot)
               (:membership_id snapshot)
               (:actor_id snapshot)
               (:is_system_admin snapshot))))

(defn ensure-conversation-access!
  [conversation-access* ctx conversation-id]
  (when (and ctx (not (str/blank? (str conversation-id))))
    (when-let [existing (get @conversation-access* conversation-id)]
      (when-not (principal-match? ctx existing)
        (throw (http/http-error 403 "conversation_scope_denied" "Conversation belongs to another Knoxx user")))))
  ctx)

(defn remember-conversation-access!
  [conversation-access* ctx conversation-id]
  (when (and ctx (not (str/blank? (str conversation-id))))
    (let [snapshot (auth-snapshot ctx)]
      (when (auth-snapshot-has-principal? snapshot)
        (ensure-conversation-access! conversation-access* ctx conversation-id)
        (swap! conversation-access* assoc conversation-id snapshot)))))

(defn run-visible?
  [ctx run]
  (cond
    (nil? ctx) true
    (system-admin? ctx) true
    (ctx-permitted? ctx "agent.runs.read_all") true
    (and (= (str (ctx-org-id ctx)) (str (record-org-id run)))
         (ctx-permitted? ctx "agent.runs.read_org")) true
    (and (ctx-permitted? ctx "agent.runs.read_own")
         (principal-match? ctx run)) true
    :else false))
