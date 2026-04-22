(ns knoxx.backend.runtime.contract-loader
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.runtime.contract-validator :as v]
            ["node:fs" :as node-fs]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

(def contract-class-order ["agents" "actors" "roles" "capabilities" "policies" "model_families" "models"])

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
  (let [configured (some-> (:contracts-dir config) str str/trim not-empty)
        cwd (.cwd js/process)
        candidates (->> [configured
                         "contracts"
                         "backend/contracts"
                         "packages/agents/knoxx/backend/contracts"
                         "orgs/open-hax/openplanner/packages/agents/knoxx/backend/contracts"]
                        (keep identity)
                        (map #(.resolve path cwd %))
                        distinct
                        vec)]
    (or (some (fn [candidate]
                (when (.existsSync node-fs candidate)
                  candidate))
              candidates)
        (.resolve path cwd (or configured "contracts")))))

(defn contracts-dir-path
  [config]
  (resolve-contracts-dir config))

(defn contract-file-path
  "Resolve a contract file path.

   2-arity defaults to contracts/agents/<id>.edn for backwards compatibility.
   3-arity accepts an explicit contract class directory."
  ([config contract-id]
   (contract-file-path config "agents" contract-id))
  ([config contract-class contract-id]
   (let [dir (resolve-contracts-dir config)
         klass (normalize-contract-class contract-class)
         id (safe-path-segment! contract-id "contract-id")]
     (.join path dir klass (str id ".edn")))))

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
   (let [dir (.join path (resolve-contracts-dir config) (normalize-contract-class contract-class))]
     (-> (.readdir fs dir #js {:withFileTypes true})
         (.then (fn [entries]
                  (->> (array-seq entries)
                       (keep (fn [ent]
                               (when (and (.isFile ent)
                                          (str/ends-with? (.-name ent) ".edn"))
                                 (subs (.-name ent) 0 (- (count (.-name ent)) 4)))))
                       sort
                       vec)))
         (.catch (fn [_]
                   (js/Promise.resolve [])))))))

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
                  (let [contract (reader/read-string trimmed)
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
