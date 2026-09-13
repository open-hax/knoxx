(ns knoxx.backend.infra.identity-bootstrap
  "Initialize Axxium credentials and independent, explicit Knoxx policy coordinates."
  (:require [axxium.infra.identity :as axxium]
            [clojure.string :as str]
            [knoxx.backend.domain.wiki-capabilities :as wiki]
            [knoxx.backend.extern.axxium :as transport]
            [knoxx.backend.infra.clio-policy-store :as local-policy]
            [knoxx.backend.infra.db.policy :as policy]
            [knoxx.backend.infra.identity :as identity]
            [knoxx.backend.infra.identity-bindings :as bindings]
            [knoxx.backend.law.identity-binding :as law]))

(def default-role-catalog
  "Explicit server-owned default roles; external identity attributes cannot create grants."
  {"system-admin" {:name "System administrator" :permissions [] :tool-policies []}
   "basic-user" {:name "Workspace member"
                 :permissions ["org.publications.read" "org.publications.manage" "org.publications.assist"]
                 :tool-policies []}})

(defn assert-config!
  "Validate identity composition before the caller opens any application ledger."
  [config]
  (law/assert-provider-config! config))

(defn- configured-bootstrap? [bootstrap]
  (and (string? (:email bootstrap)) (not (str/blank? (:email bootstrap)))
       (string? (:password bootstrap)) (not (str/blank? (:password bootstrap)))))

(defn- bootstrap-binding [principal org mongo-bootstrap]
  (let [derived (identity/principal-binding principal)]
    (law/assert-binding!
     (cond-> (assoc derived :org-id (:id org))
       mongo-bootstrap (assoc :user-id (get-in mongo-bootstrap [:user :id])
                              :membership-id (get-in mongo-bootstrap [:membership :id])
                              :actor-id (or (get-in mongo-bootstrap [:membership :actor-id])
                                            (:principal/id principal)))))))

(defn- local-primary-org [options]
  {:id (str "org_" (or (:primaryOrgSlug options) "open-hax"))
   :slug (or (:primaryOrgSlug options) "open-hax")
   :name (or (:primaryOrgName options) "Open Hax")
   :kind (or (:primaryOrgKind options) "platform_owner") :status "active"})

(defn ^:async create-context!
  "Create the selected policy provider with Axxium-owned credentials and durable bindings."
  [config policy-options]
  (assert-config! config)
  (let [options (transport/options config)
        service (axxium/open! options)
        binding-store (bindings/open! {:directory (str (:directory options) "-bindings")})
        bootstrap (transport/bootstrap-options policy-options)
        role-catalog (law/assert-role-catalog! (or (:identity-role-catalog config) default-role-catalog))
        tools (or (:identity-tools config) (mapv #(select-keys % [:id]) wiki/command-catalog))
        principal (when (configured-bootstrap? bootstrap) (await (axxium/bootstrap! service bootstrap)))
        edn? (= :edn (:policy-provider config :edn))
        base (if edn?
               {:clio-policy-store (local-policy/open! {:directory (str (:directory options) "-policy")})}
               (await (policy/create-policy-db (dissoc policy-options :bootstrapSystemAdminPassword))))
        primary-org (if edn?
                      (local-primary-org policy-options)
                      (:primary-org base))
        context (assoc base :axxium-service service :identity-bindings binding-store :primary-org primary-org
                       :local-policy-role-catalog role-catalog :local-policy-tools tools)]
    (when edn?
      (await (local-policy/initialize! (:clio-policy-store context)
                                      {:org primary-org :roles role-catalog :tools tools})))
    (when principal
      (let [binding (bootstrap-binding principal primary-org (:bootstrap base))]
        (await (bindings/bind! binding-store binding))
        (await (identity/principal-context! context principal))))
    (identity/configure! context)
    context))
