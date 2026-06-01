(ns knoxx.backend.domain.contracts.resolve
  (:require [clojure.string :as str]
            [knoxx.backend.domain.agent.agent-templates :as templates]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.domain.contracts.loader :as loader]
            [knoxx.backend.domain.contracts.roles :as roles]
            [knoxx.backend.domain.contracts.sources :as sources]
            [knoxx.backend.infra.registry.tools :as tool-registry]))

(def known-actor-keys #{:id :kind :default-agent :role-slugs :capability-ids :system-prompt :task-prompt :thinking-level :model :contract-id :model-profile :tool-policies :ui/actions :actor/sources :sources})
(def known-role-keys #{:id :role/capabilities :role/permissions :role/prompts :role/sources :sources})
(def known-capability-keys #{:id :capability/description :capability/tools})
(def known-agent-keys #{:id :enabled :agent/model :agent/thinking :prompts/task :prompts/system :trigger-kind :contract/actor :contract/actors :contract/uses :ui/actions :agent/sources :sources})

(defn contract-extras
  [contract-data known-set]
  (when contract-data
    (let [extras (apply dissoc contract-data (seq known-set))]
      (when (seq extras) extras))))

(defn actor-extras [actor-spec]
  (contract-extras actor-spec known-actor-keys))

(defn role-extras [role-data]
  (contract-extras role-data known-role-keys))

(defn capability-extras [cap-data]
  (contract-extras cap-data known-capability-keys))

(defn agent-extras [agent-contract]
  (contract-extras agent-contract known-agent-keys))

(defn- memory-hydration-from-contract
  [contract]
  (or (:memory-hydration contract)
      (:memoryHydration contract)
      (get-in contract [:memory :passive-hydration])
      (get-in contract [:memory :passiveHydration])))

(defn- context-policy-from-contract
  [contract]
  (or (:context contract)
      (:context-policy contract)
      (:contextPolicy contract)
      (get-in contract [:data :context])
      (get-in contract [:data :context-policy])))

(defn all-contract-extras
  [config actor-spec role-slugs capability-ids agent-contract]
  (let [actor-x (actor-extras (:actor actor-spec))
        role-x (map #(role-extras (roles/role-contract config %)) role-slugs)
        cap-x (map #(capability-extras (loader/contract-sync config "capabilities" %)) capability-ids)
        agent-x (agent-extras agent-contract)
        merged (reduce into {} (concat (filter seq role-x) (filter seq cap-x) (when agent-x [agent-x]) (when actor-x [actor-x])))]
    (when (seq merged) merged)))

(defn- keywordish->role-slug
  [value]
  (let [raw (cond
              (keyword? value) (name value)
              (string? value) value
              (nil? value) nil
              :else (str value))]
    (some-> raw str str/trim not-empty)))

(defn- keywordish->capability-ref
  [value]
  (let [raw (cond
              (keyword? value) (name value)
              (string? value) value
              (nil? value) nil
              :else (str value))]
    (some-> raw
            str
            str/trim
            (str/replace #"^cap/" "")
            (str/replace #"^cap_" "")
            not-empty)))

(defn- keywordish->wire
  [value]
  (cond
    (keyword? value) (if-let [ns (namespace value)]
                       (str ns "/" (name value))
                       (name value))
    (string? value) (some-> value str str/trim not-empty)
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn- ui-action-surfaces
  [action]
  (let [single (keywordish->wire (:surface action))
        many (->> (or (:surfaces action) [])
                  (map keywordish->wire)
                  (remove nil?))]
    (vec (distinct (concat (when single [single]) many)))))

(defn- normalize-ui-action
  [source action]
  (let [id (some-> (:id action) str str/trim not-empty)
        label (some-> (:label action) str str/trim not-empty)
        surfaces (ui-action-surfaces action)]
    (when (and id label (not (false? (:enabled? action))))
      (cond-> {:id id
               :label label
               :kind (or (keywordish->wire (:kind action)) "button")
               :surfaces surfaces
               :intent (or (keywordish->wire (:intent action)) "agent.run")
               :requires (->> (or (:requires action) [])
                              (map keywordish->wire)
                              (remove nil?)
                              vec)
               :confirm (boolean (:confirm? action))
               :enabled true
               :source source}
        (seq surfaces) (assoc :surface (first surfaces))
        (:icon action) (assoc :icon (str (:icon action)))
        (:agent/contract action) (assoc-in [:agent :contractId] (str (:agent/contract action)))
        (:agent/actor action) (assoc-in [:agent :actorId] (str (:agent/actor action)))
        (:tool/id action) (assoc-in [:tool :id] (str (:tool/id action)))
        (:media/from action) (assoc-in [:media :from] (keywordish->wire (:media/from action)))
        (:mode action) (assoc :mode (keywordish->wire (:mode action)))))))

(defn- action-matches-surface?
  [surface action]
  (let [wanted (some-> surface str str/trim not-empty)
        surfaces (:surfaces action)]
    (or (nil? wanted)
        (empty? surfaces)
        (some #(= wanted %) surfaces))))

(declare resolve-agent-contract)

(defn- enrich-ui-action
  [config action]
  (if-let [contract-id (get-in action [:agent :contractId])]
    (let [actor-id (get-in action [:agent :actorId])
          resolved (resolve-agent-contract config contract-id actor-id)]
      (cond-> action
        (:model resolved) (assoc-in [:agent :model] (:model resolved))))
    action))


(defn resolve-actor
  [config actor-id]
  (when-let [id (some-> actor-id str str/trim not-empty)]
    (let [record (loader/find-contract-record-sync config "actors" id)
          actor (:contract record)]
      (when actor
        {:id (:id record)
         :actor actor
         :kind (some-> (:actor/kind actor) name)
         :org (some-> (:actor/org actor) str str/trim not-empty)
         :default-agent (some-> (:actor/default-agent actor) str str/trim not-empty)
         :role-slugs (->> (or (:actor/roles actor) [])
                          (map keywordish->role-slug)
                          (remove nil?)
                          distinct
                          vec)
         :capability-ids (->> (or (:actor/capabilities actor) [])
                              (map keywordish->capability-ref)
                              (remove nil?)
                              distinct
                              vec)
         :system-prompt (templates/prompt-value (get-in actor [:prompts :system]))
         :task-prompt (templates/prompt-value (get-in actor [:prompts :task]))}))))

(defn actor-catalog
  [config]
  (->> (loader/load-all-contracts-sync config)
       (filter #(= "actors" (:contractClass %)))
       (map (fn [record]
              (resolve-actor config (:id record))))
       (remove nil?)
       (sort-by :id)
       vec))

(defn default-actor-id
  [config]
  (let [configured (some-> (:knoxx-default-actor-id config) str str/trim not-empty)
        configured-actor (when configured
                           (resolve-actor config configured))]
    (cond
      configured-actor configured
      :else (or (some-> (actor-catalog config) first :id)
                "chat_primary"))))

(defn- combine-prompts
  [& parts]
  (let [segments (->> parts
                      (keep templates/prompt-value)
                      vec)]
    (cond
      (empty? segments) nil
      (= 1 (count segments)) (first segments)
      :else (list (symbol "template") {:separator "\n\n"} segments))))

(defn- combine-system-prompts
  [& parts]
  (apply combine-prompts parts))

(defn- combine-task-prompts
  [& parts]
  (apply combine-prompts parts))

(defn- collect-role-tool-ids
  [config role-slugs]
  (->> role-slugs
       (map keywordish->role-slug)
       (remove nil?)
       distinct
       (mapcat (fn [role-slug]
                 (roles/role-tool-ids config role-slug)))
       distinct
       sort
       vec))

(defn- collect-capability-tool-ids
  [config capability-ids]
  (->> capability-ids
       (map keywordish->capability-ref)
       (remove nil?)
       distinct
       (mapcat (fn [cap-id]
                 (roles/capability-tool-ids config cap-id)))
       distinct
       sort
       vec))

(defn- legacy-explicit-tool-ids
  [contract]
  (->> (or (get-in contract [:data :tools]) [])
       (map tool-registry/normalize-tool-id)
       (remove str/blank?)
       distinct
       sort
       vec))

(defn- contract-actor-capability-claims
  [contract]
  (->> (concat (or (:actor/capabilities contract) [])
               (or (get-in contract [:actor :capabilities]) []))
       distinct
       vec))

(defn- role-context
  [config actor-spec contract]
  (let [contract-role-slugs (->> (actor-scope/agent-role-claims contract)
                                 (map keywordish->role-slug)
                                 (remove nil?)
                                 distinct
                                 vec)
        actor-role-slugs (vec (or (:role-slugs actor-spec) []))
        role-slugs (vec (distinct (concat actor-role-slugs contract-role-slugs)))
        role-system-prompts (->> role-slugs
                                 (keep #(roles/role-system-prompt config %))
                                 distinct
                                 vec)
        role-task-prompts (->> role-slugs
                               (keep #(roles/role-task-prompt config %))
                               distinct
                               vec)]
    {:contract-role-slugs contract-role-slugs
     :actor-role-slugs actor-role-slugs
     :role-slugs role-slugs
     :primary-role (or (first contract-role-slugs)
                       (first actor-role-slugs)
                       (:knoxx-default-role config))
     :role-system-prompts role-system-prompts
     :role-task-prompts role-task-prompts}))

(defn- role-capability-claims
  [config role-slugs]
  (->> (or role-slugs [])
       (mapcat #(roles/role-capability-ids config %))
       (map keywordish->capability-ref)
       (remove nil?)
       distinct
       vec))

(defn- capability-context
  [config actor-spec role-slugs contract]
  (let [actor-capability-ids (vec (or (:capability-ids actor-spec) []))
        role-capability-ids (role-capability-claims config role-slugs)
        contract-capability-ids (->> (contract-actor-capability-claims contract)
                                     (map keywordish->capability-ref)
                                     (remove nil?)
                                     distinct
                                     vec)]
    {:capability-ids (vec (distinct (concat actor-capability-ids role-capability-ids contract-capability-ids)))}))

(defn- tool-context
  [config role-slugs capability-ids contract]
  (let [role-tool-ids (collect-role-tool-ids config role-slugs)
        capability-tool-ids (collect-capability-tool-ids config capability-ids)
        explicit-tool-ids (legacy-explicit-tool-ids contract)
        tool-ids (->> (concat role-tool-ids capability-tool-ids explicit-tool-ids)
                      distinct
                      sort
                      vec)]
    {:tool-ids tool-ids
     :tool-policies (mapv (fn [tool-id]
                            {:toolId tool-id :effect "allow"})
                          tool-ids)}))

(defn- role-source-refs
  [config role-slugs]
  (->> role-slugs
       (map #(roles/role-contract config %))
       (mapcat (fn [role-contract]
                 (concat (or (:role/sources role-contract) [])
                         (or (:sources role-contract) []))))
       vec))

(defn- actor-source-refs
  [actor-spec]
  (concat (or (get-in actor-spec [:actor :actor/sources]) [])
          (or (get-in actor-spec [:actor :sources]) [])))

(defn- agent-source-refs
  [contract]
  (concat (or (:agent/sources contract) [])
          (or (:sources contract) [])))

(defn- runtime-sources-for-agent
  [config actor-spec role-slugs contract]
  (sources/compose-source-refs config
                               (actor-source-refs actor-spec)
                               (role-source-refs config role-slugs)
                               (agent-source-refs contract)))

(defn- prompt-context
  [actor-spec contract role-system-prompts role-task-prompts]
  (let [role-system-prompt (apply combine-system-prompts role-system-prompts)
        role-task-prompt (apply combine-task-prompts role-task-prompts)
        agent-system-prompt (templates/prompt-value (get-in contract [:prompts :system]))
        agent-task-prompt (templates/prompt-value (get-in contract [:prompts :task]))]
    {:role-system-prompt role-system-prompt
     :role-task-prompt role-task-prompt
     :agent-system-prompt agent-system-prompt
     :agent-task-prompt agent-task-prompt
     :system-prompt (combine-system-prompts
                     role-system-prompt
                     (:system-prompt actor-spec)
                     agent-system-prompt)
     :task-prompt (combine-task-prompts
                   role-task-prompt
                   (:task-prompt actor-spec)
                   agent-task-prompt)}))

(defn- resolved-agent-map
  [record contract contract-actors actor-spec role-ctx capability-ctx tool-ctx prompt-ctx runtime-sources all-extras enabled?]
  {:id (:id record)
   :enabled enabled?
   :contract contract
   :contract-actors contract-actors
   :contract-actor-ids (actor-scope/actor-claims->wire contract-actors)
   :actor-id (:id actor-spec)
   :actor-kind (:kind actor-spec)
   :actor-role-slugs (:actor-role-slugs role-ctx)
   :role-slugs (:role-slugs role-ctx)
   :capability-ids (:capability-ids capability-ctx)
   :role (:primary-role role-ctx)
   :model (some-> (get-in contract [:agent :model]) str str/trim not-empty)
   :thinking-level (some-> (get-in contract [:agent :thinking]) keywordish->role-slug)
   :role-system-prompt (:role-system-prompt prompt-ctx)
   :role-task-prompt (:role-task-prompt prompt-ctx)
   :actor-system-prompt (:system-prompt actor-spec)
   :actor-task-prompt (:task-prompt actor-spec)
   :agent-system-prompt (:agent-system-prompt prompt-ctx)
   :agent-task-prompt (:agent-task-prompt prompt-ctx)
   :system-prompt (:system-prompt prompt-ctx)
   :task-prompt (:task-prompt prompt-ctx)
   :trigger-kind (some-> (:trigger-kind contract) keywordish->role-slug)
   :memory-hydration (memory-hydration-from-contract contract)
   :context-policy (context-policy-from-contract contract)
   :sources runtime-sources
   :tool-ids (:tool-ids tool-ctx)
   :tool-policies (:tool-policies tool-ctx)
   :extras all-extras})

(defn resolve-agent-contract
  ([config contract-id]
   (resolve-agent-contract config contract-id nil))
  ([config contract-id actor-id]
   (when-let [id (some-> contract-id str str/trim not-empty)]
     (let [record (loader/find-contract-record-sync config "agents" id)
           contract0 (:contract record)
           contract (some-> contract0 actor-scope/normalize-agent-contract)]
       (when contract
         (let [enabled? (not (false? (:enabled contract)))
               contract-actors (actor-scope/normalized-contract-actors contract)
               requested-actor-id (some-> actor-id str str/trim not-empty)
               allowed-for-request? (or (nil? requested-actor-id)
                                        (actor-scope/actor-allowed? contract-actors requested-actor-id))
               effective-actor-id (actor-scope/effective-actor-id contract-actors requested-actor-id (default-actor-id config))]
           (when allowed-for-request?
             (let [actor-spec (or (when effective-actor-id (resolve-actor config effective-actor-id))
                                  (when-let [default-id (default-actor-id config)]
                                    (resolve-actor config default-id)))
                   role-ctx (role-context config actor-spec contract)
                   capability-ctx (capability-context config actor-spec (:role-slugs role-ctx) contract)
                   tool-ctx (tool-context config (:role-slugs role-ctx) (:capability-ids capability-ctx) contract)
                   prompt-ctx (prompt-context actor-spec contract
                                              (:role-system-prompts role-ctx)
                                              (:role-task-prompts role-ctx))
                   runtime-sources (runtime-sources-for-agent config actor-spec (:role-slugs role-ctx) contract)
                   all-extras (all-contract-extras config actor-spec (:role-slugs role-ctx) (:capability-ids capability-ctx) contract)]
                (resolved-agent-map record contract contract-actors actor-spec role-ctx capability-ctx tool-ctx prompt-ctx runtime-sources all-extras enabled?)))))))))

(defn- manual-agent-contract?
  [entry]
  (let [trigger-kind (some-> (:trigger-kind entry) str str/trim str/lower-case)]
    (or (str/blank? trigger-kind)
        (= "manual" trigger-kind))))

(defn agent-contract-catalog
  ([config]
   (agent-contract-catalog config nil))
  ([config actor-id]
   (let [wanted-actor-id (some-> actor-id str str/trim not-empty)]
     (->> (loader/load-all-contracts-sync config)
          (filter #(= "agents" (:contractClass %)))
          (map (fn [record]
                 (resolve-agent-contract config (:id record) wanted-actor-id)))
          (remove nil?)
          (filter :enabled)
          (filter manual-agent-contract?)
          (sort-by :id)
          vec))))

(defn default-agent-contract-id
  ([config]
   (default-agent-contract-id config nil))
  ([config actor-id]
   (let [actor-spec (or (when actor-id (resolve-actor config actor-id))
                        (when-let [default-id (default-actor-id config)]
                          (resolve-actor config default-id)))
         actor-default (some-> (:default-agent actor-spec) str str/trim not-empty)
         configured (some-> (:knoxx-default-agent-contract config) str str/trim not-empty)
         configured-manual (when configured (resolve-agent-contract config configured (:id actor-spec)))
         actor-default-manual (when actor-default (resolve-agent-contract config actor-default (:id actor-spec)))
         actor-catalog (agent-contract-catalog config (:id actor-spec))]
     (cond
       (and actor-default-manual (manual-agent-contract? actor-default-manual)) actor-default
       (and configured-manual
            (manual-agent-contract? configured-manual)
            (or (nil? actor-spec) (= (:id actor-spec) (:actor-id configured-manual)))) configured
       :else (some-> actor-catalog first :id)))))

(defn effective-agent-contract
  ([config requested-contract-id]
   (effective-agent-contract config requested-contract-id nil))
  ([config requested-contract-id actor-id]
   (or (when requested-contract-id (resolve-agent-contract config requested-contract-id actor-id))
       (when-let [actor-default-id (default-agent-contract-id config actor-id)]
         (resolve-agent-contract config actor-default-id actor-id))
       (when-let [global-default-id (default-agent-contract-id config nil)]
         (resolve-agent-contract config global-default-id actor-id)))))

(defn ui-actions-for-actor
  "Resolve contract-declared UI actions for an actor and optional surface.
   Actor actions are listed before default-agent actions; disabled actions are
   omitted. This is intentionally a render contract, not an execution contract."
  [config actor-id surface]
  (let [effective-actor-id (or (some-> actor-id str str/trim not-empty)
                               (default-actor-id config))
        actor-spec (resolve-actor config effective-actor-id)
        actor-actions (->> (get-in actor-spec [:actor :ui/actions])
                           (keep #(normalize-ui-action "actor" %)))
        default-agent-id (some-> (:default-agent actor-spec) str str/trim not-empty)
        agent-spec (when default-agent-id
                     (resolve-agent-contract config default-agent-id effective-actor-id))
        agent-actions (->> (get-in agent-spec [:contract :ui/actions])
                           (keep #(normalize-ui-action "agent" %)))
        actions (->> (concat actor-actions agent-actions)
                     (map #(enrich-ui-action config %))
                     (filter #(action-matches-surface? surface %))
                     vec)]
    {:actor-id effective-actor-id
     :surface (some-> surface str str/trim not-empty)
     :default-agent-id default-agent-id
     :actions actions}))
