(ns knoxx.backend.infra.agent.tool-catalog
  "Agent tool catalog and policy resolution ports."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.eta-mu :as eta-mu-extern]
            [knoxx.backend.infra.agent.hydration :refer [create-agent-custom-tools]]
            [knoxx.backend.infra.tooling :as tooling]))

(defprotocol IToolCatalog
  (available-tools [catalog context]))

(defprotocol IToolPolicyResolver
  (allowed-tools [resolver auth-context agent-spec requested-tools]))

(defn effective-tool-auth-context
  [auth-context allowed-tool-ids]
  (if-not auth-context
    nil
    (assoc auth-context
           :toolPolicies (mapv (fn [tool-id]
                                 {:toolId tool-id :effect "allow"})
                               (sort (vec allowed-tool-ids))))))

(defn allowed-tool-ids
  [config auth-context agent-spec]
  (tooling/allowed-tool-id-set config
                               (:role agent-spec)
                               auth-context
                               (:contract-id agent-spec)
                               (:actor-id agent-spec)))

(defn builtin-tools
  [runtime config tool-auth-context agent-spec]
  (tooling/create-runtime-tools runtime
                                config
                                tool-auth-context
                                (:role agent-spec)
                                (:contract-id agent-spec)
                                (:actor-id agent-spec)))

(defn custom-tools
  [runtime config tool-auth-context agent-spec allowed-tool-ids]
  (create-agent-custom-tools runtime config tool-auth-context agent-spec allowed-tool-ids))

(defn tool-runtime-names
  [builtin-tools custom-tools]
  (->> (concat (or builtin-tools [])
               (eta-mu-extern/tool-seq custom-tools))
       (keep eta-mu-extern/tool-runtime-name)
       distinct
       vec))

(defn visible-session-signature
  [runtime config auth-context agent-spec]
  (let [allowed-tool-ids (allowed-tool-ids config auth-context agent-spec)
        tool-auth-context (effective-tool-auth-context auth-context allowed-tool-ids)
        builtin (or (builtin-tools runtime config tool-auth-context agent-spec) [])
        custom (if-let [tools (custom-tools runtime config tool-auth-context agent-spec allowed-tool-ids)]
                 (eta-mu-extern/tool-seq tools)
                 [])]
    (pr-str {:tools (->> (concat builtin custom)
                         (keep eta-mu-extern/tool-runtime-name)
                         sort
                         distinct
                         vec)
             :contract-id (some-> (:contract-id agent-spec) str str/trim not-empty)
             :actor-id (some-> (:actor-id agent-spec) str str/trim not-empty)
             :role (some-> (:role agent-spec) str str/trim not-empty)
             :system-prompt (some-> (:system-prompt agent-spec) str str/trim not-empty)
             :task-prompt (some-> (:task-prompt agent-spec) str str/trim not-empty)})))

(defrecord DefaultToolPolicyResolver [config]
  IToolPolicyResolver
  (allowed-tools [_ auth-context agent-spec _requested-tools]
    (allowed-tool-ids config auth-context agent-spec)))

(defrecord DefaultToolCatalog [runtime config]
  IToolCatalog
  (available-tools [_ {:keys [auth-context agent-spec]}]
    (let [allowed (allowed-tool-ids config auth-context agent-spec)
          tool-auth-context (effective-tool-auth-context auth-context allowed)]
      {:allowed-tool-ids allowed
       :tool-auth-context tool-auth-context
       :builtin-tools (builtin-tools runtime config tool-auth-context agent-spec)
       :custom-tools (custom-tools runtime config tool-auth-context agent-spec allowed)})))

(defn tool-policy-resolver
  [config]
  (->DefaultToolPolicyResolver config))

(defn tool-catalog
  [runtime config]
  (->DefaultToolCatalog runtime config))
