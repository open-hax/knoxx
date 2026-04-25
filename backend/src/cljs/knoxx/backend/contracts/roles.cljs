(ns knoxx.backend.contracts.roles
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.contracts.loader :as contract-loader]
            [knoxx.backend.tools.registry :as tools]
            ["node:fs" :as fs]
            ["node:path" :as path]))

(def role-aliases
  {"executive" "knowledge_worker"
   "principal_architect" "developer"
   "junior_dev" "knowledge_worker"})

(defn- safe-segment
  [s]
  (when (and (string? s)
             (re-matches #"[A-Za-z0-9._-]+" s))
    s))

(defn- read-edn-sync
  [file-path]
  (try
    (let [text (.readFileSync fs file-path "utf8")]
      (reader/read-string (str text)))
    (catch :default _
      nil)))

(defn role-slug->file
  [config role-slug]
  (when-let [slug (safe-segment role-slug)]
    (contract-loader/role-file-path config slug)))

(defn cap-slug->file
  [config cap-slug]
  (when-let [slug (safe-segment cap-slug)]
    (contract-loader/capability-file-path config slug)))

(defn list-role-slugs
  "List role slugs (filenames without .edn) from contracts/roles.

  Uses synchronous IO; role catalogs are small and used in request paths."
  [config]
  (try
    (contract-loader/list-contract-ids-sync config "roles")
    (catch :default _
      ["knowledge_worker"])))

(defn normalize-role
  "Normalize an incoming role slug to a role that exists in contracts/roles.

   Falls back to knowledge_worker."
  [config role]
  (let [role (str (or role ""))
        candidates (->> [role
                         (get role-aliases role)
                         (some-> role (str/replace #"-" "_"))
                         (some-> role (str/replace #"_" "-"))
                         (some-> (get role-aliases role) (str/replace #"-" "_"))]
                        (remove nil?))
        known (set (list-role-slugs config))]
    (or (some (fn [candidate]
                (when (contains? known candidate)
                  candidate))
              candidates)
        "knowledge_worker")))

(defn- normalize-cap-id
  [v]
  (cond
    (keyword? v) (str (namespace v) "/" (name v))
    (string? v) (let [s (some-> v str str/trim not-empty)]
                  (when s s))
    :else nil))

(defn- cap-id->slug
  "Map a :cap/* id to a filename slug like cap_read."
  [cap-id]
  (let [base (last (str/split (str cap-id) #"/"))]
    (str "cap_" (-> base
                     (str/replace #"-" "_")
                     (str/trim)))))

(defn role-capability-ids
  "Return normalized capability ids (e.g. cap/read) for a role slug."
  [config role]
  (let [role (normalize-role config role)
        role-path (role-slug->file config role)
        role-map (when role-path (read-edn-sync role-path))]
    (->> (or (:role/capabilities role-map)
             [])
         (keep normalize-cap-id)
         distinct
         vec)))

(defn capability-tool-ids
  "Return a vector of tool ids (strings) for one capability id or slug."
  [config cap]
  (let [cap-slug (cond
                   (and (string? cap) (str/starts-with? cap "cap_")) cap
                   :else (some-> cap normalize-cap-id cap-id->slug))]
    (if-not cap-slug
      []
      (->> (read-edn-sync (cap-slug->file config cap-slug))
           :cap/tools
           (map tools/normalize-tool-id)
           distinct
           sort
           vec))))

(defn role-tool-ids
  "Return a vector of tool ids (strings) for a role.

   Reads roles/<role>.edn and its referenced capabilities/*.edn."
  [config role]
  (let [role (normalize-role config role)
        cap-ids (role-capability-ids config role)
        tool-ids (->> cap-ids
                      (mapcat (fn [cap-id]
                                (capability-tool-ids config cap-id)))
                      distinct
                      sort
                      vec)]
    tool-ids))

(defn role-tools
  "Return vector of {:id :label :description :enabled} tool entries for a role."
  [config role]
  (mapv (fn [tool-id]
          (let [{:keys [label description]} (tools/get-tool tool-id)]
            {:id tool-id
             :label label
             :description description}))
        (role-tool-ids config role)))

(defn role-contract
  "Load a role contract map from contracts/roles/<slug>.edn.

   `role` may be a slug (knowledge_worker), a normalized slug (knowledge-worker),
   or a keyword (:role/knowledge-worker)."
  [config role]
  (let [slug (normalize-role config role)
        role-path (role-slug->file config slug)]
    (when role-path
      (read-edn-sync role-path))))

(defn role-permissions
  "Return a vector of permission code strings for a role.

   Reads :role/permissions from contracts/roles/<role>.edn."
  [config role]
  (let [role-map (role-contract config role)]
    (->> (or (:role/permissions role-map)
             [])
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
