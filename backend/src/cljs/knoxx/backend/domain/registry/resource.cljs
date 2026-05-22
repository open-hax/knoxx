(ns knoxx.backend.domain.registry.resource
  "Generic registry protocol for EDN-described resources.

   A registry advertises resources of one kind. Actions, triggers, schedules,
   roles, and the rest are separate resource kinds; all can share this registry
   protocol without pretending the resource definition itself is a contract."
  (:require [knoxx.backend.domain.resources.loader :as resources]))

(defprotocol Registry
  (registry-id [registry]
    "Return the stable registry id keyword.")
  (registry-resource-kind [registry]
    "Return the singular resource kind owned by this registry.")
  (registered-resource-ids [registry config]
    "Return sorted resource ids visible to this registry.")
  (registry-resource [registry config resource-id]
    "Return one resource record by id, or nil.")
  (registry-catalog [registry config]
    "Return a catalog map for this registry."))

(defrecord EdnResourceRegistry [id resource-kind resource-class]
  Registry
  (registry-id [_] id)
  (registry-resource-kind [_] resource-kind)
  (registered-resource-ids [_ config]
    (resources/list-resource-ids-sync config resource-kind))
  (registry-resource [_ config resource-id]
    (resources/resource-record-sync config resource-kind resource-id))
  (registry-catalog [this config]
    {:catalog/resources {(registry-resource-kind this)
                         (registered-resource-ids this config)}}))

(def registry-specs
  [{:id :registry/actions :kind :action}
   {:id :registry/rules :kind :rule}
   {:id :registry/triggers :kind :trigger}
   {:id :registry/actors :kind :actor}
   {:id :registry/users :kind :user}
   {:id :registry/agents :kind :agent}
   {:id :registry/capabilities :kind :capability}
   {:id :registry/roles :kind :role}
   {:id :registry/workflows :kind :workflow}
   {:id :registry/pipelines :kind :pipeline}
   {:id :registry/schedules :kind :schedule}])

(defn make-registry
  [{:keys [id kind]}]
  (->EdnResourceRegistry id kind (resources/resource-class kind)))

(def registries-by-kind
  (->> registry-specs
       (map make-registry)
       (map (fn [registry]
              [(registry-resource-kind registry) registry]))
       (into {})))

(def actions-registry (get registries-by-kind :action))
(def rules-registry (get registries-by-kind :rule))
(def triggers-registry (get registries-by-kind :trigger))
(def actors-registry (get registries-by-kind :actor))
(def users-registry (get registries-by-kind :user))
(def agents-registry (get registries-by-kind :agent))
(def capabilities-registry (get registries-by-kind :capability))
(def roles-registry (get registries-by-kind :role))
(def workflows-registry (get registries-by-kind :workflow))
(def pipelines-registry (get registries-by-kind :pipeline))
(def schedules-registry (get registries-by-kind :schedule))

(defn registry
  [resource-kind]
  (get registries-by-kind (resources/normalize-resource-kind resource-kind)))

(defn catalog
  ([config]
   (catalog config (keys registries-by-kind)))
  ([config resource-kinds]
   {:catalog/resources
    (->> resource-kinds
         (keep (fn [resource-kind]
                 (when-let [owned-registry (registry resource-kind)]
                   [(registry-resource-kind owned-registry)
                    (registered-resource-ids owned-registry config)])))
         (into {}))}))
