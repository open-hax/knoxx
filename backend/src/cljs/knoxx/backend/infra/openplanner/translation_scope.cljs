(ns knoxx.backend.infra.openplanner.translation-scope
  "Authorization helpers for translation-agent target organization scope."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [ctx-org-id system-admin?]]
            [knoxx.backend.law.openplanner-translation :as contract]))

(defn- scope-org-id
  "Return the trimmed organization identifier carried by a validated policy map."
  [scope]
  (some-> (or (:orgId scope) (:org-id scope) (:org_id scope))
          str/trim
          not-empty))

(defn- context-org-id
  "Return the trimmed organization identifier carried by a validated auth context.

  Delegates to `ctx-org-id` rather than repeating its alias list, so this
  boundary cannot drift from the shape request authentication actually builds:
  `request-context-map` nests the organization at `[:org :id]` and sets no
  top-level alias, which a local alias list silently read as nil."
  [auth]
  (some-> (ctx-org-id auth) str/trim not-empty))

(defn- resolve-org
  [auth context-value policy-value]
  (cond
    (and policy-value
         context-value
         (not= policy-value context-value)
         (not (system-admin? auth)))
    (throw (ex-info "save_translation cannot target another organization"
                    {:type ::cross-organization-translation
                     :context-org context-value
                     :policy-org policy-value}))

    (and policy-value (or (= policy-value context-value)
                          (system-admin? auth)))
    policy-value

    context-value
    context-value

    :else
    (throw (ex-info "organization is required for save_translation"
                    {:type ::missing-translation-organization}))))

(defn translation-org-id!
  "Resolve the organization used by save_translation.

  Ordinary memberships may only target their current organization. System
  admins may use an explicit resource-policy organization for legacy managed
  batches while retaining an auditable system principal. Both inputs are
  validated against `TranslationScopeAuthContext` and
  `TranslationScopeResourcePolicies` so no non-string value can become a
  tenant identifier."
  [auth-context resource-policies]
  (let [auth (contract/assert-valid! :translation-scope/auth-context
                                     contract/TranslationScopeAuthContext
                                     auth-context)
        policies (contract/assert-valid! :translation-scope/resource-policies
                                         contract/TranslationScopeResourcePolicies
                                         resource-policies)]
    (resolve-org auth (context-org-id auth) (scope-org-id policies))))
