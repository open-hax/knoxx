(ns knoxx.backend.contracts.roles
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.contracts.loader :as contract-loader]
            [knoxx.backend.tools.registry :as tools]
            [promesa.core :as p]
            ["node:fs/promises" :as fsp]
            ["node:path" :as path]))

(def role-aliases
  {"executive" "knowledge_worker"
   "principal_architect" "developer"
   "junior_dev" "knowledge_worker"})

(defn- safe-segment [s]
  (when (and (string? s)
             (re-matches #"[A-Za-z0-9._-]+" s))
    s))

(defn- read-edn [file-path]
  (p/let [text (.readFile fsp file-path "utf8")]
         (some-> text str reader/read-string)))

(defn role-slug->file [config role-slug]
  (when-let [segment (safe-segment role-slug)]
    (contract-loader/role-file-path config segment)))

(defn cap-slug->file [config cap-slug]
  (when-let [segment (safe-segment cap-slug)]
    (contract-loader/capability-file-path config segment)))

(defn list-role-slugs [config]
  (-> (contract-loader/list-contract-ids! config "roles")
       (.catch (fn [_] (js/Promise.resolve ["knowledge_worker"])))))

(defn- known-roles->set [known]
  (set known))

(defn- matching-role [known role-str]
  (some (fn [candidate]
          (when (contains? (known-roles->set known) candidate)
            candidate))
        [role-str
         (get role-aliases role-str)
         (some-> role-str (str/replace #"-" "_"))
         (some-> role-str (str/replace #"_" "-"))
         (some-> (get role-aliases role-str) (str/replace #"-" "_"))]))

(defn normalize-role [config role]
  (-> (list-role-slugs config)
       (.then (fn [known]
                (or (matching-role known (str (or role "")))
                    "knowledge_worker")))))

(defn- normalize-cap-id [v]
  (cond
    (keyword? v) (str (namespace v) "/" (name v))
    (string? v) (some-> v str str/trim not-empty)
    :else nil))

(def cap-id->slug
  (comp #(str "cap_" (-> % (str/replace #"-" "_") str/trim))
        #(last (str/split (str %) #"/"))))

(defn role-capability-ids [config role]
  (-> (normalize-role config role)
       (.then (fn [role-slug]
                (when-let [path (role-slug->file config role-slug)]
                  (read-edn path))))
       (.then (fn [role-map]
                (->> (or (:role/capabilities role-map) [])
                     (keep normalize-cap-id)
                     distinct
                     vec)))))

(defn capability-tool-ids [config cap]
  (let [cap-slug (some-> cap normalize-cap-id cap-id->slug (when (str/starts-with? cap "cap_") cap))]
    (if-not cap-slug
      (js/Promise.resolve [])
      (-> (read-edn (cap-slug->file config cap-slug))
          (.then (fn [cap-map]
                   (->> (:cap/tools cap-map)
                        (map tools/normalize-tool-id)
                        distinct
                        sort
                        vec)))
          (.catch (fn [_] []))))))

(defn role-tool-ids [config role]
  (-> (role-capability-ids config role)
       (.then (fn [cap-ids]
                (-> (js/Promise.all (clj->js (mapv (partial capability-tool-ids config) cap-ids)))
                    (.then #(->> (js->clj %) (mapcat identity) distinct sort vec)))))))

(defn role-tools [config role]
  (-> (role-tool-ids config role)
       (.then (fn [tool-ids]
                (mapv (fn [tool-id]
                        (let [{:keys [label description]} (tools/get-tool tool-id)]
                          {:id tool-id :label label :description description}))
                      tool-ids)))))

(defn role-contract [config role]
  (-> (normalize-role config role)
       (.then (fn [slug]
                (when-let [path (role-slug->file config slug)]
                  (read-edn path))))))

(defn role-permissions [config role]
  (-> (role-contract config role)
       (.then (fn [role-map]
                (->> (or (:role/permissions role-map) [])
                     (map str)
                     distinct
                     sort
                     vec)))))

(defn role-system-prompt [config role]
  (-> (role-contract config role)
       (.then (fn [role-map]
                (some-> (or (get-in role-map [:prompts :system])
                            (get-in role-map [:role/prompts :system])
                            (:role/system-prompt role-map)
                            (:role/system_prompt role-map)
                            (:system-prompt role-map)
                            (:system_prompt role-map))
                            str str/trim not-empty)))))

(defn role-task-prompt [config role]
  (-> (role-contract config role)
       (.then (fn [role-map]
                (some-> (or (get-in role-map [:prompts :task])
                            (get-in role-map [:role/prompts :task])
                            (:role/task-prompt role-map)
                            (:role/task_prompt role-map)
                            (:task-prompt role-map)
                            (:task_prompt role-map))
                            str str/trim not-empty)))))