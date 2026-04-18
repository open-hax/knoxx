(ns kms-ingestion.drivers.promptdb
  "Ingestion driver for promptdb — structured EDN epistemic records.

   Unlike the local/pi-sessions drivers which chunk text to embeddings,
   this driver performs schema-aware structured ingestion:
     1. Walk the filesystem path for .edn files
     2. Parse each file as EDN (one record or a vector of records)
     3. Validate every record against the epistemic kernel schemas
     4. Return records directly for storage in the epistemic/Datalog store

   source-kind: :promptdb

   Config keys:
     :path       — root directory to walk (required)
     :recursive? — descend into subdirs (default true)
     :strict?    — throw on validation failure vs warn+skip (default false)"
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [kms-ingestion.drivers.protocol :as protocol]
   [kms-ingestion.epistemic :as ep]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- edn-files
  "Return all .edn files under root, optionally recursive."
  [root recursive?]
  (let [f (io/file root)]
    (when-not (.exists f)
      (throw (ex-info "promptdb path does not exist" {:path root})))
    (if recursive?
      (->> (file-seq f)
           (filter #(and (.isFile %) (str/ends-with? (.getName %) ".edn"))))
      (->> (.listFiles f)
           (filter #(and (.isFile %) (str/ends-with? (.getName %) ".edn")))))))

(defn- file->id [root-path file]
  (let [root-str (.getCanonicalPath (io/file root-path))
        file-str (.getCanonicalPath file)]
    (str/replace-first file-str (str root-str "/") "")))

(defn- content-hash [s]
  (format "%x" (hash s)))

(defn- read-records
  "Parse EDN file to a sequence of records. Accepts single map or vector."
  [file]
  (let [raw (slurp file)
        parsed (edn/read-string {:readers {'inst #(java.util.Date.)}} raw)]
    (if (map? parsed) [parsed] (vec parsed))))

(defn- validate-record
  "Validate one record. Returns {:ok record} or {:error msg}."
  [record strict?]
  (if-let [err (ep/explain record)]
    (if strict?
      (throw (ex-info "Invalid epistemic record in promptdb" {:errors err :record record}))
      (do (log/warn "promptdb: skipping invalid record" {:errors err :record record})
          {:error err}))
    {:ok record}))

;; ---------------------------------------------------------------------------
;; Driver record
;; ---------------------------------------------------------------------------

(defrecord PromptdbDriver [config state-atom]

  protocol/Driver

  (discover [_ opts]
    (let [{:keys [path recursive?] :or {recursive? true}} config
          since      (:since opts)
          ex-hashes  (:existing-hashes opts {})
          files      (edn-files path recursive?)
          categorise (fn [f]
                       (let [id   (file->id path f)
                             hash (content-hash (slurp f))
                             mod  (java.util.Date. (.lastModified f))]
                         {:id           id
                          :path         (.getCanonicalPath f)
                          :content-hash hash
                          :size         (.length f)
                          :modified-at  mod
                          :status       (cond
                                          (and since (.before mod since)) :unchanged
                                          (= hash (get ex-hashes id))     :unchanged
                                          (contains? ex-hashes id)        :changed
                                          :else                           :new)}))
          categorised (map categorise files)
          by-status   (group-by :status categorised)]
      {:total-files     (count categorised)
       :new-files       (count (:new by-status))
       :changed-files   (count (:changed by-status))
       :deleted-files   0  ;; promptdb is append-only by convention
       :unchanged-files (count (:unchanged by-status))
       :files           (concat (:new by-status) (:changed by-status))}))

  (extract [_ file-id]
    (let [path    (get-in config [:path])
          f       (io/file path file-id)
          raw     (slurp f)
          records (read-records f)]
      {:id           file-id
       :path         (.getCanonicalPath f)
       :content      raw
       :content-hash (content-hash raw)
       ;; The key addition over text drivers:
       ;; parsed+validated epistemic records ready for store insertion
       :epistemic-records
       (->> records
            (map #(validate-record % (get config :strict? false)))
            (filter :ok)
            (map :ok))}))

  (extract-batch [this file-ids]
    (map #(protocol/extract this %) file-ids))

  (get-state [_]
    @state-atom)

  (set-state [_ state]
    (reset! state-atom state))

  (close [_]
    nil))

;; ---------------------------------------------------------------------------
;; Constructor
;; ---------------------------------------------------------------------------

(defn create-driver
  "Create a promptdb driver instance.

   Config:
     {:path       \"/path/to/promptdb-core\"
      :recursive? true        ;; default
      :strict?    false}      ;; default — warn+skip on bad records"
  [config]
  (when-not (:path config)
    (throw (ex-info "promptdb driver requires :path" {:config config})))
  (->PromptdbDriver config (atom {})))
