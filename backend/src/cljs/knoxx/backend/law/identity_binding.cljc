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

(defn assert-principal-binding!
  "Refuse a changed entity/kind behind an already bound Axxium actor ID."
  [principal binding]
  (assert-binding! binding)
  (when-not (= [(:principal/id principal) (:principal/entity-id principal) (:principal/kind principal)]
               [(:principal-id binding) (:entity-id binding) (:kind binding)])
    (throw (ex-info "Axxium principal conflicts with its durable Knoxx binding"
                    {:status 403 :code "identity_binding_conflict"})))
  binding)
