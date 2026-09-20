(ns knoxx.backend.law.identity-binding
  "Explicit identity-to-policy bindings; email never supplies authority."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(defn assert-delegated-grant!
  "A delegated grant must name stable actor/entity/member identities, never an email alias."
  [grant]
  (when-not (every? #(and (string? %) (not (str/blank? %)))
                   (map #(get grant %) [:axxiumPrincipalId :axxiumEntityId :membershipId]))
    (throw (ex-info "This delegated token requires Axxium reauthorization"
                    {:status 401 :code "identity_reauthorization_required"})))
  grant)

(def Binding
  "Stable Axxium identity coordinates bound to one Knoxx policy membership."
  [:map {:closed true}
   [:principal-id [:string {:min 1}]]
   [:entity-id [:string {:min 1}]]
   [:kind [:enum :human :agent :service :automation]]
   [:user-id [:string {:min 1}]]
   [:membership-id [:string {:min 1}]]
   [:org-id [:string {:min 1}]]
   [:actor-id [:string {:min 1}]]])

(def RoleCatalog
  "Server-owned role policy; unknown roles receive no permissions."
  [:map-of :string
   [:map
    [:permissions [:vector :string]]
    [:tool-policies {:optional true} [:vector :map]]
    [:name {:optional true} :string]]])

(defn assert-binding!
  "Validate a complete durable binding without consulting aliases or email."
  [binding]
  (when-not (m/validate Binding binding)
    (throw (ex-info "invalid Axxium policy binding"
                    {:status 400 :code "identity_binding_invalid"})))
  binding)

(defn assert-role-catalog!
  "Require explicit permissions for every configured role."
  [roles]
  (when-not (m/validate RoleCatalog roles)
    (throw (ex-info "invalid local identity role catalog"
                    {:status 500 :code "identity_role_catalog_invalid"})))
  roles)

(defn assert-provider-config!
  "Refuse unknown provider selections before any persistence provider is opened."
  [config]
  (when (and (contains? config :identity-authority)
             (not (contains? #{:embedded :remote} (:identity-authority config))))
    (throw (ex-info "Unsupported identity authority" {:status 500 :code "identity_authority_invalid"})))
  (when-not (contains? #{:edn :mongo} (:policy-provider config :edn))
    (throw (ex-info "Unsupported policy provider" {:status 500 :code "policy_provider_invalid"})))
  config)

(defn assert-principal-binding!
  "Refuse a changed entity/kind behind an already bound Axxium actor ID."
  [principal binding]
  (assert-binding! binding)
  (when-not (= [(:principal/id principal) (:principal/entity-id principal) (:principal/kind principal)]
               [(:principal-id binding) (:entity-id binding) (:kind binding)])
    (throw (ex-info "Axxium principal conflicts with its durable Knoxx binding"
                    {:status 403 :code "identity_binding_conflict"})))
  binding)

(defn membership-selector
  "Decode untrusted scope hints only; no header can establish an identity owner."
  [headers]
  (reduce-kv
   (fn [selector header field]
     (let [value (get headers header)]
       (cond
         (or (nil? value) (= "" value)) selector
         (and (string? value) (not (str/blank? value))) (assoc selector field value)
         :else (throw (ex-info "Invalid membership selector"
                               {:status 400 :code "identity_membership_selector_invalid"})))))
   {} {:x-knoxx-membership-id :membership-id :x-knoxx-org-id :org-id :x-knoxx-org-slug :org-slug}))

(defn assert-selected-context!
  "A requested scope can only narrow the exact context already authorized by a credential."
  [context selector]
  (let [actual {:membership-id (get-in context [:membership :id])
                :org-id (get-in context [:org :id]) :org-slug (get-in context [:org :slug])}]
    (when-not (= selector (select-keys actual (keys selector)))
      (throw (ex-info "Requested scope differs from the credential's authorized membership"
                      {:status 403 :code "identity_membership_scope_denied"}))))
  context)
