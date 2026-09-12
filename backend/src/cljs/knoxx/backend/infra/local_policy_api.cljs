(ns knoxx.backend.infra.local-policy-api
  "Authenticated finite operations for the selected Clio policy directory."
  (:require [axxium.infra.identity :as axxium]
            [knoxx.backend.domain.local-policy-reads :as reads]
            [knoxx.backend.extern.local-policy :as host]
            [knoxx.backend.infra.clio-policy-store :as store]
            [knoxx.backend.infra.identity-bindings :as bindings]
            [knoxx.backend.law.identity-binding :as binding-law]
            [knoxx.backend.law.local-policy :as law]))

(defn selected?
  "Recognize only an explicitly initialized Clio policy provider."
  [context]
  (some? (:clio-policy-store context)))

(defn acting-context
  "Attach the authenticated request, preserving its private credential refresh closure."
  [context verified-context]
  (assoc context :acting-context verified-context))

(defn- ^:async current-actor! [context]
  (let [previous (:acting-context context)
        refresh (:identity/refresh! previous)]
    (when-not refresh
      (law/refuse! 401 "unauthenticated" "A current Axxium session is required"))
    (let [current (await (refresh))
          principal (:axxium-principal current)
          actor {:principal principal :user-id (get-in current [:user :id])
                 :membership-id (get-in current [:membership :id])}]
      (when-not (and (= (get-in previous [:user :id]) (:user-id actor))
                     (= (get-in previous [:membership :id]) (:membership-id actor))
                     (= principal (axxium/resolve-active-principal (:axxium-service context) (:principal/id principal))))
        (law/refuse! 401 "unauthenticated" "The acting Axxium identity is no longer current"))
      (law/assert-schema! law/Actor actor))))

(defn ^:async read!
  "Refresh the original session and recheck directory grants on every public read."
  [context operation args]
  (let [actor (await (current-actor! context))]
    (await (store/authorized-read! (:clio-policy-store context) actor operation args))))

(defn trusted-read!
  "Use a named provider read only from trusted internal composition."
  [context operation args]
  (store/read! (:clio-policy-store context) operation args))

(defn- normalize-payload [payload]
  (if (map? payload)
    (reduce-kv (fn [result wire internal]
                 (if (contains? result wire) (-> result (assoc internal (get result wire)) (dissoc wire)) result))
               payload {:permissionCodes :permission-codes :toolPolicies :tool-policies})
    payload))

(defn ^:async command!
  "Reauthenticate immediately before atomic Clio authorization and command admission."
  [context operation args]
  (let [actor (await (current-actor! context))
        args (mapv normalize-payload args)]
    (doseq [arg args]
      (when-let [principal (:verified-principal arg)]
        (when-not (= principal (axxium/resolve-active-principal (:axxium-service context) (:principal/id principal)))
          (law/refuse! 409 "identity_principal_changed" "The selected Axxium principal is no longer current"))))
    (await (store/command! (:clio-policy-store context)
                          {:id (host/operation-id) :at (host/now-iso)
                           :actor actor :operation operation :args args}))))

(defn- ^:async verified-user-payload! [context payload]
  (let [payload (dissoc payload :verified-principal :verified-binding)]
    (if-let [principal-id (:axxium-principal-id payload)]
      (let [_ (law/nonblank! principal-id)
            principal (axxium/resolve-active-principal (:axxium-service context) principal-id)
            _ (when-not principal
                (law/refuse! 404 "identity_principal_not_found" "An active Axxium principal is required"))
            canonical (await (bindings/read! (:identity-bindings context) principal-id))
            org-id (law/nonblank! (:org-id payload))
            verified-binding {:principal-id principal-id :entity-id (:principal/entity-id principal)
                     :kind (:principal/kind principal) :user-id (or (:user-id canonical) (str "user_" principal-id))
                     :membership-id (if (= org-id (:org-id canonical)) (:membership-id canonical)
                                      (str "membership_" (count org-id) "_" org-id "_" principal-id))
                     :org-id org-id :actor-id principal-id}]
        (binding-law/assert-principal-binding! principal verified-binding)
        (when-not (= principal (axxium/resolve-active-principal (:axxium-service context) principal-id))
          (law/refuse! 409 "identity_principal_changed" "The selected Axxium principal changed during binding"))
        (assoc payload :verified-principal principal :verified-binding verified-binding))
      payload)))

(defn ^:async create-user!
  "Resolve explicit principal IDs on the server; supplemental membership never rebinds login context."
  [context payload]
  (let [payload (await (verified-user-payload! context payload))]
    (await (command! context :policy/create-user [payload]))))

(defn ^:async bootstrap!
  "Expose directory configuration only after a fresh administrative read check."
  [context]
  (await (read! context :policy/orgs []))
  {:primaryOrg (:primary-org context) :bootstrapUser nil})

(defn- owner-active? [context state row]
  (when-let [principal-id (get-in state [:users (:user-id row) :principal-id])]
    (let [principal (axxium/resolve-active-principal (:axxium-service context) principal-id)
          user (get-in state [:users (:user-id row)])]
      (and principal (= (:entity-id user) (:principal/entity-id principal))
           (= (:kind user) (:principal/kind principal))))))

(defn ^:async credential!
  "Resolve an exact actor credential only while its Axxium owner and directory scope remain active."
  [context actor-id provider scope]
  (law/nonblank! actor-id)
  (law/nonblank! provider)
  (let [state (await (trusted-read! context :policy/state []))
        row (reads/scoped-credential state actor-id provider scope)]
    (if (and row (owner-active? context state row))
      (reads/read-operation state :policy/credential [actor-id provider scope])
      {:credential nil})))

(defn ^:async credentials!
  "List service credentials for trusted provider composition with current owner checks."
  [context provider]
  (law/nonblank! provider)
  (let [state (await (trusted-read! context :policy/state []))
        active (into {} (filter (fn [[_ row]] (owner-active? context state row))) (:credentials state))]
    (reads/read-operation (assoc state :credentials active) :policy/credentials [provider])))

(defn unavailable!
  "Refuse unsupported legacy identity operations without falling through to Mongo."
  [operation]
  (law/refuse! 501 "identity_owned_by_axxium"
               (str "The selected EDN provider delegates identity operations to Axxium: " (name operation))))
