(ns knoxx.backend.contracts.resolve
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.contracts.actor-scope :as actor-scope]
            [knoxx.backend.contracts.loader :as loader]
            [knoxx.backend.contracts.roles :as roles]
            [knoxx.backend.tools.registry :as tool-registry]
            ["node:fs" :as fs]
            ["node:path" :as path]))

(def known-actor-keys #{:id :kind :default-agent :role-slugs :capability-ids :system-prompt :task-prompt :thinking-level :model :contract-id :model-profile :tool-policies})
(def known-role-keys #{:id :role/capabilities :role/permissions :role/prompts})
(def known-capability-keys #{:id :capability/description :capability/tools})
(def known-agent-keys #{:id :enabled :agent/model :agent/thinking :prompts/task :prompts/system :trigger-kind :contract/actor :contract/actors :contract/uses})

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

(defn all-contract-extras
  [config actor-spec role-slugs capability-ids agent-contract]
  (let [actor-x (actor-extras (:actor actor-spec))
        role-x (map #(role-extras (roles/load-role config %)) role-slugs)
        cap-x (map #(capability-extras (loader/load-capability config %)) capability-ids)
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
  (cond
    (keyword? value) (str (namespace value) "/" (name value))
    (string? value) (some-> value str str/trim not-empty)
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn- read-edn-sync
  [file-path]
  (try
    (some-> (.readFileSync fs file-path "utf8") str reader/read-string)
    (catch :default _
      nil)))

(defn resolve-actor
  [config actor-id]
  (when-let [id (some-> actor-id str str/trim not-empty)]
    (let [file-path (loader/actor-file-path config id)
          actor (read-edn-sync file-path)]
      (when actor
        {:id id
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
         :system-prompt (some-> (get-in actor [:prompts :system]) str str/trim not-empty)}))))

(defn actor-catalog
  [config]
  (->> (loader/list-contract-ids-sync config "actors")
       (map (fn [actor-id]
              (resolve-actor config actor-id)))
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

(defn- combine-system-prompts
  [& parts]
  (let [segments (->> parts
                      (map (fn [part]
                             (some-> part str str/trim not-empty)))
                      (remove nil?))]
    (when (seq segments)
      (str/join "\n\n" segments))))

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

(defn resolve-agent-contract
  ([config contract-id]
   (resolve-agent-contract config contract-id nil))
  ([config contract-id actor-id]
   (when-let [id (some-> contract-id str str/trim not-empty)]
     (let [file-path (loader/contract-file-path config "agents" id)
           contract0 (read-edn-sync file-path)
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
                   contract-role-slugs (->> (actor-scope/agent-role-claims contract)
                                            (map keywordish->role-slug)
                                            (remove nil?)
                                            distinct
                                            vec)
                   actor-role-slugs (vec (or (:role-slugs actor-spec) []))
                   role-slugs (vec (distinct (concat actor-role-slugs contract-role-slugs)))
                   actor-capability-ids (vec (or (:capability-ids actor-spec) []))
                   contract-capability-ids (->> (contract-actor-capability-claims contract)
                                                (map keywordish->capability-ref)
                                                (remove nil?)
                                                distinct
                                                vec)
                   capability-ids (vec (distinct (concat actor-capability-ids contract-capability-ids)))
                   role-tool-ids (collect-role-tool-ids config role-slugs)
                   capability-tool-ids (collect-capability-tool-ids config capability-ids)
                   explicit-tool-ids (legacy-explicit-tool-ids contract)
                   tool-ids (->> (concat role-tool-ids capability-tool-ids explicit-tool-ids)
                                 distinct
                                 sort
                                 vec)
                   tool-policies (mapv (fn [tool-id]
                                         {:toolId tool-id :effect "allow"})
                                       tool-ids)
                   primary-role (or (first contract-role-slugs)
                                    (first actor-role-slugs)
                                    (:knoxx-default-role config))
                   role-system-prompt-text (some-> (roles/role-system-prompt config primary-role)
                                                   str str/trim not-empty)
agent-system-prompt (some-> (get-in contract [:prompts :system]) str str/trim not-empty)
                    system-prompt (combine-system-prompts
                                   role-system-prompt-text
                                   (:system-prompt actor-spec)
                                   agent-system-prompt)
                    all-extras (all-contract-extras config actor-spec role-slugs capability-ids contract)]
                {:id id
                :enabled enabled?
                :contract contract
                :contract-actors contract-actors
                :contract-actor-ids (actor-scope/actor-claims->wire contract-actors)
                :actor-id (:id actor-spec)
                :actor-kind (:kind actor-spec)
                :actor-role-slugs actor-role-slugs
                :role-slugs role-slugs
                :capability-ids capability-ids
                :role primary-role
                :model (some-> (get-in contract [:agent :model]) str str/trim not-empty)
                :thinking-level (some-> (get-in contract [:agent :thinking]) keywordish->role-slug)
                :role-system-prompt role-system-prompt-text
                :actor-system-prompt (:system-prompt actor-spec)
                :agent-system-prompt agent-system-prompt
                :system-prompt system-prompt
                :task-prompt (some-> (get-in contract [:prompts :task]) str str/trim not-empty)
                :trigger-kind (some-> (:trigger-kind contract) keywordish->role-slug)
                :tool-ids tool-ids
                :tool-policies tool-policies
                 :extras all-extras}))))))))

(defn- manual-agent-contract?
  [entry]
  (= "manual" (some-> (:trigger-kind entry) str str/trim str/lower-case)))

(defn agent-contract-catalog
  ([config]
   (agent-contract-catalog config nil))
  ([config actor-id]
   (let [ids (loader/list-contract-ids-sync config "agents")
         wanted-actor-id (some-> actor-id str str/trim not-empty)]
     (->> ids
          (map (fn [id]
                 (resolve-agent-contract config id wanted-actor-id)))
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
