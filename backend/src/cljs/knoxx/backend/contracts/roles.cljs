(ns knoxx.backend.contracts.roles
  (:require [clojure.string :as str]
            [knoxx.backend.contracts.loader :as contract-loader]
            [knoxx.backend.tools.registry :as tools]))

(def role-aliases
  "Legacy export retained for callers that inspect it. Runtime role resolution must
   not use hard-coded aliases; canonical role identity comes from role contracts."
  {})

(defn- keywordish-id
  [value]
  (cond
    (keyword? value) (some-> value name str/trim not-empty)
    (string? value) (some-> value str str/trim not-empty)
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn- id-candidates
  [value]
  (let [raw (keywordish-id value)]
    (->> [raw
          (some-> raw (str/replace #"_" "-"))
          (some-> raw (str/replace #"-" "_"))
          (when (some-> raw (str/starts-with? "cap_"))
            (subs raw 4))]
         (remove str/blank?)
         distinct
         vec)))

(defn- contract-record
  [config contract-class id]
  (let [klass (contract-loader/normalize-contract-class contract-class)
        candidates (set (id-candidates id))]
    (some (fn [record]
            (when (and (= klass (:contractClass record))
                       (contains? candidates (:id record)))
              record))
          (contract-loader/load-all-contracts-sync config))))

(defn- contract-by-id
  [config contract-class id]
  (some-> (contract-record config contract-class id) :contract))

(defn list-role-slugs
  "List canonical role contract IDs from parsed contract records.

   IDs come from :role/id through contracts.loader identity extraction, not from
   filenames or contracts/roles directory placement."
  [config]
  (->> (contract-loader/load-all-contracts-sync config)
       (filter #(= "roles" (:contractClass %)))
       (map :id)
       distinct
       sort
       vec))

(defn normalize-role
  "Return the canonical role contract id when it exists.

   This intentionally does not fall back to a default role: missing role claims
   are contract drift and should be visible to callers."
  [config role]
  (or (some-> (contract-record config "roles" role) :id)
      (keywordish-id role)))

(defn- normalize-cap-id
  [v]
  (keywordish-id v))

(defn cap-id->slug
  "Legacy helper retained for callers. It no longer names a file path; it returns
   the canonical-ish capability id candidate used for contract identity lookup."
  [cap-id]
  (or (some-> (contract-record {} "capabilities" cap-id) :id)
      (some-> cap-id keywordish-id (str/replace #"^cap_" ""))))

(defn role-capability-ids
  "Return normalized capability ids for a role contract."
  [config role]
  (let [role-map (contract-by-id config "roles" role)]
    (->> (or (:role/capabilities role-map) [])
         (keep normalize-cap-id)
         distinct
         vec)))

(defn capability-tool-ids
  "Return a vector of tool ids for one capability id."
  [config cap]
  (if-let [cap-map (contract-by-id config "capabilities" cap)]
    (->> (:cap/tools cap-map)
         (map tools/normalize-tool-id)
         (remove str/blank?)
         distinct
         sort
         vec)
    []))

(defn role-tool-ids
  "Return a vector of tool ids for a role contract."
  [config role]
  (let [cap-ids (role-capability-ids config role)]
    (->> cap-ids
         (mapcat (fn [cap-id]
                   (capability-tool-ids config cap-id)))
         distinct
         sort
         vec)))

(defn role-tools
  "Return vector of {:id :label :description} tool entries for a role."
  [config role]
  (mapv (fn [tool-id]
          (let [{:keys [label description]} (tools/get-tool tool-id)]
            {:id tool-id
             :label label
             :description description}))
        (role-tool-ids config role)))

(defn role-contract
  "Load a role contract map by canonical role identity."
  [config role]
  (contract-by-id config "roles" role))

(defn role-permissions
  "Return a vector of permission code strings for a role contract."
  [config role]
  (let [role-map (role-contract config role)]
    (->> (or (:role/permissions role-map) [])
         (map str)
         distinct
         sort
         vec)))

(defn role-system-prompt
  "Return the role-level system prompt text (string) if present."
  [config role]
  (let [role-map (role-contract config role)
        prompt (or (get-in role-map [:prompts :system])
                   (get-in role-map [:role/prompts :system])
                   (:role/system-prompt role-map)
                   (:role/system_prompt role-map)
                   (:system-prompt role-map)
                   (:system_prompt role-map))]
    (some-> prompt str str/trim not-empty)))

(defn role-task-prompt
  "Return the role-level task prompt text (string) if present."
  [config role]
  (let [role-map (role-contract config role)
        prompt (or (get-in role-map [:prompts :task])
                   (get-in role-map [:role/prompts :task])
                   (:role/task-prompt role-map)
                   (:role/task_prompt role-map)
                   (:task-prompt role-map)
                   (:task_prompt role-map))]
    (some-> prompt str str/trim not-empty)))
