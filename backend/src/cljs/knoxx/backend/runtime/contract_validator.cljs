(ns knoxx.backend.runtime.contract-validator
  (:require [malli.core :as m]
            [malli.error :as me]))

(def AgentContract
  "Minimal agent-contract schema for EDN contracts stored on disk."
  [:map
   [:contract/id string?]
   [:contract/kind keyword?]
   [:contract/version {:optional true} int?]
   [:enabled {:optional true} boolean?]
   [:actor/id {:optional true} string?]])

(def ActorContract
  [:map
   [:actor/id string?]
   [:actor/kind keyword?]
   [:actor/label {:optional true} string?]
   [:actor/contract {:optional true} string?]
   [:actor/roles {:optional true} [:sequential keyword?]]])

(def RoleContract
  [:map
   [:role/id keyword?]
   [:role/capabilities {:optional true} [:sequential keyword?]]])

(def CapabilityContract
  [:map
   [:cap/id keyword?]
   [:cap/tools {:optional true} [:sequential any?]]])

(defn- infer-contract-class
  [value]
  (cond
    (contains? value :contract/id) "agents"
    (contains? value :actor/id) "actors"
    (contains? value :role/id) "roles"
    (contains? value :cap/id) "capabilities"
    :else "agents"))

(defn- schema-for
  [contract-class value]
  (case (or contract-class (infer-contract-class value))
    "agents" AgentContract
    "actors" ActorContract
    "roles" RoleContract
    "capabilities" CapabilityContract
    AgentContract))

(defn- collect-humanized-errors
  [prefix value]
  (cond
    (nil? value) []
    (string? value) [{:path prefix :message value}]
    (vector? value) (mapcat #(collect-humanized-errors prefix %) value)
    (sequential? value) (mapcat #(collect-humanized-errors prefix %) value)
    (map? value) (mapcat (fn [[k v]]
                           (collect-humanized-errors (conj prefix (name k)) v))
                         value)
    :else [{:path prefix :message (pr-str value)}]))

(defn validate
  "Validate a parsed contract-like map.

   Returns:
   - {:ok true :value value :errors []}
   - {:ok false :value value :errors [{:path [...] :message <text>} ...]}"
  ([value]
   (validate nil value))
  ([contract-class value]
   (let [schema (schema-for contract-class value)
         ok? (m/validate schema value)
         explained (when-not ok? (m/explain schema value))
         errors (if explained
                  (->> (collect-humanized-errors [] (me/humanize explained))
                       (map (fn [err]
                              (update err :path (fn [p] (mapv str p)))))
                       vec)
                  [])]
     {:ok ok?
      :value value
      :errors errors})))
