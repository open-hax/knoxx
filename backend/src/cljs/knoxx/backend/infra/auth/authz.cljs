(ns knoxx.backend.infra.auth.authz
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.session :as auth-session]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.runtime.state :as runtime-state]))

(defn policy-db
  [_runtime]
  (runtime-state/current-policy-db))

(defn policy-db-enabled?
  [runtime]
  (some? (policy-db runtime)))

(defn- fastify-handler-result
  "Fastify treats a resolved promise value as a response payload.

   CLJS `nil` becomes JS `null`, which makes Fastify try to send a second
   response after we've already used reply.send/reply.code in async handlers.
   Normalize `nil` to `undefined` so promise-returning handlers can safely do
   side-effectful replies and resolve to 'no payload'."
  [value]
  (cond
    (instance? js/Promise value)
    (.then value (fn [resolved]
                   (if (nil? resolved)
                     js/undefined
                     resolved)))

    (nil? value)
    js/undefined

    :else
    value))

(defn policy-db-promise
  [runtime reply status promise]
  (if-not (policy-db-enabled? runtime)
    (fastify-handler-result
     (http/json-response! reply 503 {:detail "Knoxx policy database is not configured"}))
    (-> promise
        (.then (fn [result]
                 (http/json-response! reply status (js->clj result :keywordize-keys true))
                 js/undefined))
        (.catch (fn [err]
                  (http/error-response! reply err)
                  js/undefined)))))

(defn resolve-request-context!
  [runtime request]
  (if-not (policy-db-enabled? runtime)
    (js/Promise.resolve nil)
    (if-let [cached (aget request "__knoxxRequestContext")]
      (js/Promise.resolve cached)
      (let [headers (or (aget request "headers") #js {})
            header-email (str/trim (or (aget headers "x-knoxx-user-email") ""))
            header-mid (str/trim (or (aget headers "x-knoxx-membership-id") ""))
            ctx-promise (if (or (not (str/blank? header-email))
                                (not (str/blank? header-mid)))
                          (.resolveRequestContext (policy-db runtime) headers)
                          ;; Fall back to cookie-backed auth context resolution.
                          (auth-session/resolve-auth-context request (policy-db runtime)))]
        (-> ctx-promise
            (.then (fn [ctx]
                     (let [clj-ctx (js->clj ctx :keywordize-keys true)]
                       (aset request "__knoxxRequestContext" clj-ctx)
                       clj-ctx))))))))

(defn with-request-context!
  [runtime request reply f]
  (if-not (policy-db-enabled? runtime)
    (fastify-handler-result (f nil))
    (-> (resolve-request-context! runtime request)
        (.then (fn [ctx]
                 (fastify-handler-result (f ctx))))
        (.catch (fn [err]
                  (http/error-response! reply err)
                  js/undefined)))))

(defn ctx-org-id [ctx] (or (:orgId ctx) (get-in ctx [:org :id]) (when ctx (aget ctx "orgId"))))
(defn ctx-org-slug [ctx] (or (:orgSlug ctx) (get-in ctx [:org :slug]) (when ctx (aget ctx "orgSlug"))))
(defn ctx-user-id [ctx] (or (:userId ctx) (get-in ctx [:user :id]) (when ctx (aget ctx "userId"))))
(defn ctx-user-email [ctx] (or (:userEmail ctx) (get-in ctx [:user :email]) (when ctx (aget ctx "userEmail"))))
(defn ctx-membership-id [ctx] (or (:membershipId ctx) (get-in ctx [:membership :id]) (when ctx (aget ctx "membershipId"))))
(defn ctx-actor-id [ctx] (or (:actorId ctx) (:actor_id ctx) (:actor-id ctx) (get-in ctx [:actor :id]) (when ctx (aget ctx "actorId"))))

(defn ctx-role-slugs
  [ctx]
  (into #{}
        (or (:roleSlugs ctx)
            (keep :slug (:roles ctx)))))

(defn primary-context-role
  [ctx]
  (or (first (:roleSlugs ctx))
      (some->> (:roles ctx) (map :slug) first)
      "knowledge_worker"))

(defn system-admin?
  [ctx]
  (contains? (ctx-role-slugs ctx) "system_admin"))

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
          (when (= (str (:toolId policy)) (str tool-id))
            (:effect policy)))
        (:toolPolicies ctx)))

(defn ctx-tool-policy
  [ctx tool-id]
  (some (fn [policy]
          (when (= (str (:toolId policy)) (str tool-id))
            policy))
        (:toolPolicies ctx)))

(defn ctx-tool-constraints
  [ctx tool-id]
  (or (:constraints (ctx-tool-policy ctx tool-id))
      {}))

(defn ctx-tool-allowed?
  [ctx tool-id]
  (or (system-admin? ctx)
      (= "allow" (ctx-tool-effect ctx tool-id))))

(defn ensure-permission!
  [ctx permission]
  (when-not (or (system-admin? ctx)
                (ctx-permitted? ctx permission))
    (throw (http/http-error 403 "permission_denied" (str "Permission '" permission "' is required"))))
  ctx)

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

(defn record-org-id [record] (or (:org_id record) (:orgId record)))
(defn record-user-id [record] (or (:user_id record) (:userId record)))
(defn record-user-email [record] (or (:user_email record) (:userEmail record)))
(defn record-membership-id [record] (or (:membership_id record) (:membershipId record)))
(defn record-actor-id [record] (or (:actor_id record) (:actorId record) (:actor-id record)))

(defn principal-match?
  [ctx record]
  (let [ctx-membership (str (or (ctx-membership-id ctx) ""))
        record-membership (str (or (record-membership-id record) ""))
        ctx-user (str (or (ctx-user-id ctx) ""))
        record-user (str (or (record-user-id record) ""))
        ctx-email (str/lower-case (str (or (ctx-user-email ctx) "")))
        record-email (str/lower-case (str (or (record-user-email record) "")))
        ctx-actor (str (or (ctx-actor-id ctx) ""))
        record-actor (str (or (record-actor-id record) ""))
        actor-bound? (not (str/blank? record-actor))
        actor-match? (or (not actor-bound?) (= ctx-actor record-actor))
        user-bound? (or (not (str/blank? record-membership))
                        (not (str/blank? record-user))
                        (not (str/blank? record-email)))]
    (cond
      (not actor-match?) false
      (system-admin? ctx) true
      (and (not (str/blank? ctx-membership))
           (not (str/blank? record-membership)))
      (= ctx-membership record-membership)
      (and (not (str/blank? ctx-user))
           (not (str/blank? record-user)))
      (= ctx-user record-user)
      (and (not (str/blank? ctx-email))
           (not (str/blank? record-email)))
      (= ctx-email record-email)
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
   :role_slugs (vec (or (:roleSlugs ctx) []))
   :permissions (vec (or (:permissions ctx) []))
   :tool_policies (vec (or (:toolPolicies ctx) []))
   :membership_tool_policies (vec (or (:membershipToolPolicies ctx) []))
   :is_system_admin (boolean (:isSystemAdmin ctx))})

(defn auth-snapshot-has-principal?
  [snapshot]
  (boolean (or (:org_id snapshot)
               (:user_id snapshot)
               (:user_email snapshot)
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
