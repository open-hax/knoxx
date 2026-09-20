(ns knoxx.backend.infra.identity
  "Compose current Axxium authentication with explicitly bound Knoxx policy."
  (:require [axxium.infra.identity :as axxium]
            [clojure.string :as str]
            [knoxx.backend.domain.wiki-capabilities :as wiki]
            [knoxx.backend.extern.axxium :as transport]
            [knoxx.backend.extern.mcp-token :as token-boundary]
            [knoxx.backend.infra.clio-policy-store :as local-policy]
            [knoxx.backend.infra.db.policy :as policy]
            [knoxx.backend.infra.identity-bindings :as bindings]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as delegated]
            [knoxx.backend.law.identity-binding :as law]))

(defonce ^:private configured (atom nil))

(defn configure!
  "Install an explicitly initialized context for compatibility entry points."
  [context]
  (reset! configured context))

(defn context
  "Resolve initialized identity composition; absent identity never enables anonymous authority."
  [candidate]
  (let [value (if (:axxium-service candidate) candidate @configured)]
    (when-not (:axxium-service value)
      (throw (ex-info "Axxium identity is not initialized" {:status 503 :code "identity_unavailable"})))
    value))

(defn- unauthenticated! []
  (throw (ex-info "Authentication required" {:status 401 :code "unauthenticated"})))

(defn principal-binding
  "Derive private-workspace coordinates from a verified immutable identity."
  [principal]
  (let [id (:principal/id principal)]
    (law/assert-binding!
     {:principal-id id :entity-id (:principal/entity-id principal) :kind (:principal/kind principal)
      :user-id (str "user_" id) :membership-id (str "membership_" id)
      :org-id (str "org_" id) :actor-id id})))

(defn ^:async ensure-binding!
  "Bind a new verified local principal to its own workspace, never an email match."
  [{:keys [identity-bindings clio-policy-store] :as policy-context} principal]
  (or (when-let [existing (await (bindings/read! identity-bindings (:principal/id principal)))]
        (law/assert-principal-binding! principal existing))
      (when clio-policy-store
        (let [binding (principal-binding principal)]
          (await (local-policy/initialize!
                  clio-policy-store
                  {:org {:id (:org-id binding) :slug (:org-id binding)
                         :name (:principal/display-name principal) :kind "personal" :status "active"}
                   :roles (:local-policy-role-catalog policy-context) :tools (:local-policy-tools policy-context [])}))
          (await (bindings/bind! identity-bindings binding))))
      (throw (ex-info "This verified identity needs an explicit policy binding"
                      {:status 403 :code "identity_binding_required"}))))

(defn ^:async principal-context!
  "Resolve current directory membership and roles only after verifying the stable binding."
  [policy-context principal]
  (let [{:keys [clio-policy-store] :as policy-context} (context policy-context)
        binding (await (ensure-binding! policy-context principal))
        result (if clio-policy-store
                 (do (await (local-policy/observe! clio-policy-store binding principal))
                     (await (local-policy/read! clio-policy-store :policy/context [binding principal])))
                 (await (policy/resolve-bound-context! policy-context binding)))]
    (when-not result (unauthenticated!))
    (assoc result :axxium-principal principal :identity-binding binding)))

(declare resolve-request!)

(defn ^:async current-context!
  "Recheck the original credential and current actor/directory state for a long-lived view."
  [_policy-context previous]
  (if-let [refresh (:identity/refresh! previous)]
    (assoc (await (refresh)) :identity/refresh! refresh)
    (unauthenticated!)))

(defn- restrict-delegated-tools [ctx grant]
  (let [allowed (set (:tools grant))
        denied (into [] (comp (map :id) (remove allowed)
                             (map (fn [id] {:tool-id id :effect "deny"}))) wiki/command-catalog)]
    (update ctx :tool-policies #(into (vec %) denied))))

(defn- ^:async resolve-token-context! [policy-context token bearer?]
  (let [service (:axxium-service policy-context)]
    (if-let [principal (axxium/resolve-principal service token)]
      (let [result (await (principal-context! policy-context principal))]
        (when-not (= principal (axxium/resolve-principal service token)) (unauthenticated!))
        result)
      (if-not bearer?
        (unauthenticated!)
        (if-let [grant (some-> (await (delegated/get-token! token)) token-boundary/stored-record)]
          (let [_ (law/assert-delegated-grant! grant)
                principal (axxium/resolve-active-principal service (:axxiumPrincipalId grant))]
            (when-not (and principal (= (:principal/entity-id principal) (:axxiumEntityId grant)))
              (unauthenticated!))
            (let [ctx (await (principal-context! policy-context principal))]
              (when-not (and (= (get-in ctx [:membership :id]) (:membershipId grant))
                             (or (nil? (:actorId grant)) (= (get-in ctx [:actor :id]) (:actorId grant)))
                             (or (nil? (:orgSlug grant)) (= (get-in ctx [:org :slug]) (:orgSlug grant))))
                (unauthenticated!))
              (when-not (and (= grant (some-> (await (delegated/get-token! token)) token-boundary/stored-record))
                             (= principal (axxium/resolve-active-principal service (:axxiumPrincipalId grant))))
                (unauthenticated!))
              (restrict-delegated-tools ctx grant)))
          (unauthenticated!))))))

(defn ^:async resolve-request!
  "Authenticate the request; asserted email/member headers never establish authority."
  [policy-context request]
  (let [policy-context (context policy-context)
        {:keys [token bearer?] :as credentials} (transport/request-data request)
        origin (get-in policy-context [:axxium-service :options :public-base-url])]
    (transport/require-origin! credentials origin)
    (when (or (not (string? token)) (str/blank? token)) (unauthenticated!))
    (let [authentication-id (transport/authentication-id token)
          refresh (fn ^:async refresh-context []
                    (assoc (await (resolve-token-context! policy-context token bearer?))
                           :identity/authentication-id authentication-id))
          result (await (refresh))]
      ;; The secret is captured privately, not stored in a serializable context field.
      (assoc result :identity/refresh! refresh))))

(defn ^:async resolve-bearer-context!
  "Authenticate one direct or delegated bearer for MCP with the same current policy checks."
  [policy-context token]
  (when (or (not (string? token)) (str/blank? token)) (unauthenticated!))
  (let [policy-context (context policy-context)
        authentication-id (transport/authentication-id token)
        refresh (fn ^:async refresh-bearer []
                  (assoc (await (resolve-token-context! policy-context token true))
                         :identity/authentication-id authentication-id))]
    (assoc (await (refresh)) :identity/refresh! refresh)))
