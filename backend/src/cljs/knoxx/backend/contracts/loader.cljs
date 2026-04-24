(ns knoxx.backend.contracts.loader
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.contracts.actor-scope :as actor-scope]
            [knoxx.backend.contracts.validator :as v]
            ["node:fs" :as node-fs]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

(def contract-class-order ["agents" "actors" "roles" "capabilities" "policies" "model_families" "models"])

(defn- contract-edn-filename?
  [name]
  (and (string? name)
       (str/ends-with? name ".edn")
       (not (str/starts-with? name "."))))

(defn- configured-contracts-dir
  [config]
  (some-> (:contracts-dir config) str str/trim not-empty))

(defn- default-configured-contracts-dir?
  [value]
  (or (nil? value)
      (= value "contracts")))

(defn- contract-root-candidates
  [config]
  (let [configured (configured-contracts-dir config)
        preferred (if (default-configured-contracts-dir? configured)
                    ["../contracts"
                     "contracts"
                     "packages/agents/knoxx/contracts"
                     "orgs/open-hax/openplanner/packages/agents/knoxx/contracts"]
                    [configured
                     "../contracts"
                     "contracts"
                     "packages/agents/knoxx/contracts"
                     "orgs/open-hax/openplanner/packages/agents/knoxx/contracts"])]
    (->> preferred
         (keep identity)
         distinct
         vec)))

(defn safe-path-segment!
  "Reject path traversal and odd unicode by constraining to a conservative charset."
  [segment kind]
  (let [s (str segment)]
    (when (or (str/blank? s)
              (not (re-matches #"[A-Za-z0-9._-]+" s)))
      (throw (js/Error. (str "Invalid " kind " segment: " segment))))
    s))

(defn normalize-contract-class
  [value]
  (let [raw (some-> value str str/trim str/lower-case)]
    (case raw
      ("agent" "agents" "contract" "contracts" nil "") "agents"
      ("actor" "actors" "user" "users" "human" "humans") "actors"
      ("role" "roles") "roles"
      ("cap" "caps" "capability" "capabilities") "capabilities"
      ("policy" "policies") "policies"
      ("model-family" "model-families" "model_family" "model_families") "model_families"
      ("model" "models") "models"
      (throw (js/Error. (str "Unknown contract class: " value))))))

(defn- resolve-contracts-dir
  [config]
  (let [configured (configured-contracts-dir config)
        cwd (.cwd js/process)
        candidates (->> (contract-root-candidates config)
                        (map #(.resolve path cwd %))
                        distinct
                        vec)]
    (or (some (fn [candidate]
                (when (.existsSync node-fs candidate)
                  candidate))
              candidates)
        (.resolve path cwd (or configured "../contracts")))))

(defn contract-root-paths
  [config]
  (let [cwd (.cwd js/process)
        configured (configured-contracts-dir config)
        resolved (->> (contract-root-candidates config)
                      (map #(.resolve path cwd %))
                      distinct
                      vec)
        existing (->> resolved
                      (filter #(.existsSync node-fs %))
                      vec)]
    (if (seq existing)
      existing
      [(.resolve path cwd (or configured "../contracts"))])))

(defn contracts-dir-path
  [config]
  (resolve-contracts-dir config))

(defn contract-class-dir-paths
  [config contract-class]
  (let [klass (normalize-contract-class contract-class)]
    (mapv (fn [root]
            (.join path root klass))
          (contract-root-paths config))))

(defn contract-file-path
  "Resolve a contract file path.

   2-arity defaults to contracts/agents/<id>.edn for backwards compatibility.
   3-arity accepts an explicit contract class directory."
  ([config contract-id]
   (contract-file-path config "agents" contract-id))
  ([config contract-class contract-id]
   (let [klass (normalize-contract-class contract-class)
         id (safe-path-segment! contract-id "contract-id")
         filename (str id ".edn")
         existing-path (some (fn [root]
                               (let [candidate (.join path root klass filename)]
                                 (when (.existsSync node-fs candidate)
                                   candidate)))
                             (contract-root-paths config))]
     (or existing-path
         (.join path (contracts-dir-path config) klass filename)))))

(defn role-file-path
  [config role-slug]
  (contract-file-path config "roles" role-slug))

(defn capability-file-path
  [config cap-slug]
  (contract-file-path config "capabilities" cap-slug))

(defn actor-file-path
  [config actor-id]
  (contract-file-path config "actors" actor-id))

(defn read-edn-file!
  [file-path]
  (-> (.readFile fs file-path "utf8")
      (.then (fn [text]
               (reader/read-string (str text))))))

(defn ensure-dir!
  [dir]
  (.mkdir fs dir #js {:recursive true}))

(defn write-edn-file!
  [file-path edn-text]
  (let [dir (.dirname path file-path)]
    (-> (ensure-dir! dir)
        (.then (fn []
                 (.writeFile fs file-path edn-text "utf8"))))))

(defn list-contract-ids!
  "List contract IDs (filenames without .edn) from a contract class directory.
   Returns Promise<vector<string>>. Missing dirs resolve to []."
  ([config]
   (list-contract-ids! config "agents"))
  ([config contract-class]
   (let [dirs (contract-class-dir-paths config contract-class)]
     (-> (js/Promise.all
          (clj->js
           (mapv (fn [dir]
                   (-> (.readdir fs dir #js {:withFileTypes true})
                       (.then (fn [entries]
                                (->> (array-seq entries)
                                     (keep (fn [ent]
                                             (when (and (.isFile ent)
                                                        (contract-edn-filename? (.-name ent)))
                                               (subs (.-name ent) 0 (- (count (.-name ent)) 4)))))
                                     vec)))
                       (.catch (fn [_]
                                 (js/Promise.resolve []))))
                 dirs))))
         (.then (fn [results]
                  (->> (js->clj results)
                       (mapcat identity)
                       distinct
                       sort
                       vec)))))))

(defn list-contract-ids-sync
  [config contract-class]
  (->> (contract-class-dir-paths config contract-class)
       (mapcat (fn [dir]
                 (try
                   (->> (.readdirSync node-fs dir)
                        (keep (fn [name]
                                (when (contract-edn-filename? name)
                                  (subs name 0 (- (count name) 4))))))
                   (catch :default _
                     []))))
       distinct
       sort
       vec))

(defn list-agent-contract-ids!
  [config]
  (list-contract-ids! config "agents"))

(defn load-contract!
  "Load + parse + validate a contract-like EDN file.

   2-arity defaults to contracts/agents/<id>.edn.
   Returns Promise of {:ok? boolean :edn-text string :contract map|nil :validation {...}}"
  ([config contract-id]
   (load-contract! config "agents" contract-id))
  ([config contract-class contract-id]
   (let [klass (normalize-contract-class contract-class)
         file-path (contract-file-path config klass contract-id)]
     (-> (.readFile fs file-path "utf8")
         (.then
          (fn [edn-text]
            (let [trimmed (str/trim (str edn-text))]
              (if (str/blank? trimmed)
                {:ok? false
                 :edn-text (str edn-text)
                 :contract nil
                 :validation {:ok false :errors [{:path [] :message "EDN text is empty"}]}}
                (try
                  (let [raw-contract (reader/read-string trimmed)
                        contract (if (= klass "agents")
                                   (actor-scope/normalize-agent-contract raw-contract)
                                   raw-contract)
                        validation (v/validate klass contract)]
                    {:ok? (:ok validation)
                     :edn-text (str edn-text)
                     :contract contract
                     :validation (dissoc validation :value)})
                  (catch :default err
                    {:ok? false
                     :edn-text (str edn-text)
                     :contract nil
                     :validation {:ok false
                                  :errors [{:path [] :message (str "EDN parse error: " (.-message err))}]}}))))))
         (.catch
          (fn [err]
            (if (= "ENOENT" (.-code err))
              {:ok? false
               :edn-text ""
               :contract nil
               :validation {:ok false :errors [{:path [] :message "Contract not found"}]}}
              (throw err))))))))
