(ns knoxx.backend.runtime.contract-loader
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.runtime.contract-validator :as v]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

(defn safe-path-segment!
  "Reject path traversal and odd unicode by constraining to a conservative charset." 
  [segment kind]
  (let [s (str segment)]
    (when (or (str/blank? s)
              (not (re-matches #"[A-Za-z0-9._-]+" s)))
      (throw (js/Error. (str "Invalid " kind " segment: " segment))))
    s))

(defn- resolve-contracts-dir
  [config]
  (.resolve path (or (:contracts-dir config) "contracts")))

(defn contract-file-path
  "Resolve a contract file path within contracts/agents.

   Contract IDs map 1:1 to filenames <id>.edn."
  [config contract-id]
  (let [dir (resolve-contracts-dir config)
        id (safe-path-segment! contract-id "contract-id")]
    (.join path dir "agents" (str id ".edn"))))

(defn role-file-path
  [config role-slug]
  (let [dir (resolve-contracts-dir config)
        slug (safe-path-segment! role-slug "role")]
    (.join path dir "roles" (str slug ".edn"))))

(defn capability-file-path
  [config cap-slug]
  (let [dir (resolve-contracts-dir config)
        slug (safe-path-segment! cap-slug "capability")]
    (.join path dir "capabilities" (str slug ".edn"))))

(defn actor-file-path
  [config actor-id]
  (let [dir (resolve-contracts-dir config)
        id (safe-path-segment! actor-id "actor-id")]
    (.join path dir "actors" (str id ".edn"))))

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

(defn list-agent-contract-ids!
  "List contract IDs (filenames without .edn) from contracts/agents.
   Returns Promise<vector<string>>."
  [config]
  (let [dir (.join path (resolve-contracts-dir config) "agents")]
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
                  ;; Missing dir is fine.
                  (js/Promise.resolve []))))))

(defn load-contract!
  "Load + parse + validate a contract by id.

   Returns Promise of {:ok? boolean :edn-text string :contract map|nil :validation {...}}"
  [config contract-id]
  (let [file-path (contract-file-path config contract-id)]
    (-> (.readFile fs file-path "utf8")
        (.then (fn [edn-text]
                 (let [trimmed (str/trim (str edn-text))]
                   (if (str/blank? trimmed)
                     {:ok? false
                      :edn-text (str edn-text)
                      :contract nil
                      :validation {:ok false :errors [{:path [] :message "EDN text is empty"}]}}
                     (try
                       (let [contract (reader/read-string trimmed)
                             validation (v/validate contract)]
                         {:ok? (:ok validation)
                          :edn-text (str edn-text)
                          :contract contract
                          :validation (dissoc validation :value)})
                       (catch :default err
                         {:ok? false
                          :edn-text (str edn-text)
                          :contract nil
                          :validation {:ok false
                                       :errors [{:path [] :message (str "EDN parse error: " (.-message err))}]}})))))))
        (.catch (fn [err]
                 (if (= "ENOENT" (.-code err))
                   {:ok? false
                    :edn-text ""
                    :contract nil
                    :validation {:ok false :errors [{:path [] :message "Contract not found"}]}}
                   (throw err))))))
