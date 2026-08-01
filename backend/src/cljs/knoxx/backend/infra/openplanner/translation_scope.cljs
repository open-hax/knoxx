(ns knoxx.backend.infra.openplanner.translation-scope
  "Authorization helpers for translation-agent target organization scope."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [system-admin?]]))

(defn translation-org-id!
  "Resolve the organization used by save_translation.

  Ordinary memberships may only target their current organization. System
  admins may use an explicit resource-policy organization for legacy managed
  batches while retaining an auditable system principal."
  [auth-context resource-policies]
  (let [context-org (or (:orgId auth-context)
                        (:org-id auth-context)
                        (:org_id auth-context))
        policy-org (or (:org_id resource-policies)
                       (:org-id resource-policies)
                       (:orgId resource-policies))
        context-value (some-> context-org str str/trim not-empty)
        policy-value (some-> policy-org str str/trim not-empty)]
    (cond
      (and policy-value
           context-value
           (not= policy-value context-value)
           (not (system-admin? auth-context)))
      (throw (js/Error. "save_translation cannot target another organization"))

      (and policy-value (or (= policy-value context-value)
                            (system-admin? auth-context)))
      policy-value

      context-value
      context-value

      :else
      (throw (js/Error. "organization is required for save_translation")))))