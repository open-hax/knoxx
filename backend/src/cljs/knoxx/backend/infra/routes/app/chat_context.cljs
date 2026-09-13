(ns knoxx.backend.infra.routes.app.chat-context
  "Resolve configured agent contracts and constrain requested chat authority."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.contracts.sources :as contract-sources]
            [knoxx.backend.infra.auth.authz :as auth-authz]
            [knoxx.backend.infra.tooling :as infra-tooling]
            [knoxx.backend.shape.agent-chat-input :as app-input]))

(defn merged-agent-spec
  "Resolve a configured agent contract and compose its requested source references."
  [config parsed]
  (let [requested (app-input/compact-agent-spec-overrides (or (:agent-spec parsed) {}))
        requested-actor-id (or (get requested :actor-id)
                               (infra-tooling/default-actor-id config))
        requested-contract-id (or (get requested :contract-id)
                                  (infra-tooling/default-agent-contract-id config requested-actor-id))
        resolved (infra-tooling/effective-agent-contract config requested-contract-id requested-actor-id)
        resolved-id (:id resolved)
        merged (merge (select-keys resolved [:role :model :system-prompt :task-prompt :thinking-level :tool-policies :contract-actor-ids :memory-hydration :context-policy :sources])
                      requested)
        runtime-sources (contract-sources/compose-source-refs config
                                                              (:sources resolved)
                                                              (:sources requested))]
    (cond-> (assoc merged :sources runtime-sources)
      requested-actor-id (assoc :actor-id requested-actor-id)
      (seq (:contract-actor-ids resolved)) (assoc :contract-actors (:contract-actor-ids resolved))
      resolved-id (assoc :contract-id resolved-id))))

(defn- effective-tool-policies
  [ctx parsed]
  (let [requested (app-input/requested-tool-policies parsed)
        context-policies (app-input/ctx-tool-policies ctx)]
    (cond
      (and (nil? ctx) (seq requested)) requested
      (and (nil? ctx) (:auth-context parsed)) (app-input/parsed-auth-tool-policies parsed)
      (empty? requested) context-policies
      (auth-authz/system-admin? ctx) requested
      :else (let [allowed (->> context-policies
                               (filter app-input/allow-policy?)
                               (keep app-input/tool-policy-id)
                               set)]
              (->> requested
                   (filter #(contains? allowed (app-input/tool-policy-id %)))
                   vec)))))

(defn effective-auth-context
  "Apply requested actor, role and policies within the current authority context."
  [ctx parsed]
  (let [base (or ctx (:auth-context parsed))
        requested-actor-id (some-> (get-in parsed [:agent-spec :actor-id]) str str/trim not-empty)
        requested-role-slug (app-input/requested-role parsed)
        role-slugs (cond
                     (and (nil? base) requested-role-slug) [requested-role-slug]
                     (and requested-role-slug (or (auth-authz/system-admin? ctx)
                                                  (contains? (into #{} (or (:roleSlugs base) [])) requested-role-slug)))
                     [requested-role-slug]
                     :else (vec (or (:roleSlugs base) [])))
        tool-policies (effective-tool-policies ctx parsed)
        resource-policies (or (get-in parsed [:agent-spec :resource-policies])
                              (get-in parsed [:auth-context :resourcePolicies])
                              (:resourcePolicies base))]
    (when (or base requested-actor-id requested-role-slug (seq tool-policies) resource-policies)
      (cond-> (or base {})
        requested-actor-id (assoc :actorId requested-actor-id)
        (seq role-slugs) (assoc :roleSlugs role-slugs)
        (or (seq tool-policies) (some? base)) (assoc :toolPolicies tool-policies)
        resource-policies (assoc :resourcePolicies resource-policies)))))
