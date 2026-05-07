;; contracts/loader.cljs
(ns knoxx.backend.contracts.loader
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.contracts.actor-scope :as actor-scope]
            [knoxx.backend.contracts.validator :as v]
            ["node:fs" :as node-fs]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

;; ── Constants ──────────────────────────────────────────────────────────────

(def contract-class-order
  ["agents" "actors" "roles" "capabilities" "policies"
   "model_families" "models" "ingest_sources" "actions" "pipelines" "triggers" "sub_agents"])

;; ── Predicates ─────────────────────────────────────────────────────────────

(defn- contract-edn-filename?
  [filename]
  (and (string? filename)
       (str/ends-with? filename ".edn")
       (not (str/starts-with? filename "."))))

;; ── Config helpers ─────────────────────────────────────────────────────────

(defn- configured-contracts-dir
  [config]
  (some-> (:contracts-dir config) str str/trim not-empty))

(defn- default-configured-contracts-dir?
  [value]
  (or (nil? value) (= value "contracts")))

(defn- contract-root-candidates
  [config]
  (let [configured (configured-contracts-dir config)]
    (if (default-configured-contracts-dir? configured)
      ["../contracts" "contracts"
       "packages/agents/knoxx/contracts"
       "orgs/open-hax/openplanner/packages/agents/knoxx/contracts"]
      [configured])))

(defn contract-root-paths
  [config]
  (let [cwd      (.cwd js/process)
        resolved (->> (contract-root-candidates config)
                      (map #(.resolve path cwd %))
                      distinct
                      vec)
        existing (filterv #(.existsSync node-fs %) resolved)]
    (if (seq existing)
      existing
      [(.resolve path cwd (or (configured-contracts-dir config) "../contracts"))])))

(defn contracts-dir-path
  "First existing contract root (legacy single-root compat)."
  [config]
  (first (contract-root-paths config)))

;; ── Path safety ────────────────────────────────────────────────────────────

(defn safe-path-segment!
  [segment kind]
  (let [s (str segment)]
    (when (or (str/blank? s)
              (not (re-matches #"[A-Za-z0-9._-]+" s)))
      (throw (js/Error. (str "Invalid " kind " segment: " segment))))
    s))

(defn normalize-contract-class
  [value]
  (let [raw (some-> value
                    (cond-> (keyword? value) name
                            (not (keyword? value)) str)
                    str/trim
                    str/lower-case)]
    (case raw
      ("agent" "agents" "contract" "contracts" nil "") "agents"
      ("actor" "actors" "user" "users" "human" "humans") "actors"
      ("role" "roles") "roles"
      ("cap" "caps" "capability" "capabilities") "capabilities"
      ("policy" "policies") "policies"
      ("model-family" "model-families" "model_family" "model_families") "model_families"
      ("model" "models") "models"
      ("ingest-source" "ingest-sources" "ingest_source" "ingest_sources") "ingest_sources"
      ("action" "actions") "actions"
      ("pipeline" "pipelines") "pipelines"
      ("trigger" "triggers") "triggers"
      ("sub-agent" "sub-agents" "sub_agent" "sub_agents") "sub_agents"
      (throw (js/Error. (str "Unknown contract class: " value))))))

;; ── Stderr logging ─────────────────────────────────────────────────────────

(defn- stderr!
  [& parts]
  (.write js/process.stderr (str (str/join "" parts) "\n")))

;; ── Discovery ──────────────────────────────────────────────────────────────

(defn entry->file-path
  "Returns absolute path if entry is a non-hidden .edn file, else nil."
  [ent]
  (when (and (.isFile ent) (contract-edn-filename? (.-name ent)))
    (.join path (.-parentPath ent) (.-name ent))))

(defn discover-contract-files!
  "Find all .edn files under root via recursive readdir. Returns Promise<vector<string>>."
  [root]
  (-> (.readdir fs root #js {:withFileTypes true :recursive true})
      (.then (fn [entries]
               (->> (js/Array.from entries)
                    (keep entry->file-path)
                    vec)))
      (.catch (fn [err]
                (stderr! "[contracts] readdir failed: " root " — " (.-message err))
                []))))

;; ── Per-file parsing ───────────────────────────────────────────────────────

(defn- keyword->str
  [v]
  (if (keyword? v) (name v) (str v)))

(defn- extract-contract-identity
  [raw]
  ;; IMPORTANT: prefer :model/id over :model-family/id.
  ;; Many model contracts include both keys, but they must be classified as "models"
  ;; (otherwise they'll be validated as a model-family and fail on :model-family/prefixes).
  (let [kind (some-> (or (:contract/kind raw) (:kind raw)
                          (when (:actor/id raw) "actors")
                          (when (:role/id raw) "roles")
                          (when (:cap/id raw) "capabilities")
                          (when (:model/id raw) "models")
                          (when (:model-family/id raw) "model_families"))
                      keyword->str str/trim not-empty)
        id   (some-> (or (:contract/id raw) (:id raw)
                          (:actor/id raw) (:role/id raw) (:cap/id raw)
                          (:model/id raw) (:model-family/id raw))
                      keyword->str str/trim not-empty)]
    (when (and kind id) [kind id])))

(defn- validate-and-build
  [file-path edn-text raw]
  (let [[raw-kind id] (extract-contract-identity raw)]
    (when-not (and raw-kind id)
      (stderr! "[contracts] missing :contract/id or :contract/kind: " file-path)
      (throw (js/Error. "missing identity")))
    (let [kind  (normalize-contract-class raw-kind)
          valid (v/validate kind raw)]
      (if (:ok valid)
        {:ok?          true
         :id           id
         :contractClass kind
         :contract     raw
         :file-path    file-path
         :edn-text     (str edn-text)}
        (do (stderr! "[contracts] validation failed: " file-path
                     " — " (pr-str (:errors valid)))
            nil)))))

(defn parse-contract-file!
  "Parse + validate a single .edn file. Returns contract record or nil."
  [file-path edn-text]
  (try
    (let [raw (reader/read-string (str edn-text))]
      (validate-and-build file-path edn-text raw))
    (catch :default err
      (stderr! "[contracts] parse error: " file-path " — " (.-message err))
      nil)))

(defn- read-contract-file!
  [file-path]
  (-> (.readFile fs file-path "utf8")
      (.then (partial parse-contract-file! file-path))
      (.catch (fn [err]
                (stderr! "[contracts] read error: " file-path " — " (.-message err))
                nil))))

;; ── Deduplication ──────────────────────────────────────────────────────────

(defn dedup-contracts
  "First-wins dedup on [contractClass id]. Logs collisions to stderr."
  [records]
  (let [seen (atom #{})]
    (reduce (fn [acc r]
              (let [k [(:contractClass r) (:id r)]]
                (if (@seen k)
                  (do (stderr! "[contracts] collision on " (pr-str k)
                               " — keeping first, ignoring " (:file-path r))
                      acc)
                  (do (swap! seen conj k)
                      (conj acc r)))))
            []
            (remove nil? records))))

;; ── Public API ─────────────────────────────────────────────────────────────

(defn load-all-contracts!
  "Discover all .edn files under all contract roots, parse+validate each,
   deduplicate on [kind id]. Returns Promise<vector<contract-record>>."
  [config]
  (let [roots (contract-root-paths config)]
    (-> (js/Promise.all (clj->js (map discover-contract-files! roots)))
        (.then (fn [file-lists]
                 (->> (js/Array.from file-lists)
                      (mapcat #(js/Array.from %))
                      distinct
                      vec)))
        (.then (fn [files]
                 (js/Promise.all (clj->js (map read-contract-file! files)))))
        (.then (fn [results]
                 (dedup-contracts (js/Array.from results)))))))

(defn list-contract-ids!
  ([config] (list-contract-ids! config "agents"))
  ([config contract-class]
   (let [klass (normalize-contract-class contract-class)]
     (-> (load-all-contracts! config)
         (.then (fn [all]
                  (->> all
                       (filter #(= (:contractClass %) klass))
                       (mapv :id)
                       sort
                       vec)))))))

(defn list-agent-contract-ids!
  [config]
  (list-contract-ids! config "agents"))

;; ── Individual file ops ────────────────────────────────────────────────────

(defn- resolve-contracts-dir
  [config]
  (or (first (filter #(.existsSync node-fs %)
                     (->> (contract-root-candidates config)
                          (map #(.resolve path (.cwd js/process) %)))))
      (.resolve path (.cwd js/process) (or (configured-contracts-dir config) "../contracts"))))

(defn contract-class-dir-paths
  [config contract-class]
  (let [klass (normalize-contract-class contract-class)]
    (mapv #(.join path % klass) (contract-root-paths config))))

(defn- find-contract-file-recursive
  "Search for {id}.edn under {root}/{class} recursively."
  [root klass filename]
  (try
    (let [entries (.readdirSync node-fs (.join path root klass) #js {:withFileTypes true :recursive true})]
      (some (fn [ent]
              (when (and (.isFile ent) (= (.-name ent) filename))
                (.join path (.-parentPath ent) (.-name ent))))
            entries))
    (catch :default _ nil)))

(defn contract-file-path
  ([config contract-id]
   (contract-file-path config "agents" contract-id))
  ([config contract-class contract-id]
   (let [klass    (normalize-contract-class contract-class)
         id       (safe-path-segment! contract-id "contract-id")
         filename (str id ".edn")
         existing (some (fn [root]
                          (find-contract-file-recursive root klass filename))
                        (contract-root-paths config))]
     (or existing
         (.join path (resolve-contracts-dir config) klass filename)))))

(defn role-file-path       [config slug]     (contract-file-path config "roles"        slug))
(defn capability-file-path [config slug]     (contract-file-path config "capabilities" slug))
(defn actor-file-path      [config actor-id] (contract-file-path config "actors"       actor-id))

(defn read-edn-file!
  [file-path]
  (-> (.readFile fs file-path "utf8")
      (.then (fn [text] (reader/read-string (str text))))))

(defn ensure-dir!
  [dir]
  (.mkdir fs dir #js {:recursive true}))

(defn write-edn-file!
  [file-path edn-text]
  (let [dir (.dirname path file-path)]
    (-> (ensure-dir! dir)
        (.then (fn [] (.writeFile fs file-path edn-text "utf8"))))))

(defn list-contract-ids-sync
  [config contract-class]
  (->> (contract-class-dir-paths config contract-class)
       (mapcat (fn [dir]
                 (try (->> (.readdirSync node-fs dir #js {:withFileTypes true :recursive true})
                           (keep (fn [ent]
                                   (when-let [fp (entry->file-path ent)]
                                     (let [id (subs (.-name ent) 0 (- (count (.-name ent)) 4))]
                                       id)))))
                      (catch :default _ []))))
       distinct sort vec))

(defn load-contract!
  ([config contract-id] (load-contract! config "agents" contract-id))
  ([config contract-class contract-id]
   (let [klass     (normalize-contract-class contract-class)
         file-path (contract-file-path config klass contract-id)]
     (-> (.readFile fs file-path "utf8")
         (.then (fn [edn-text]
                  (let [trimmed (str/trim (str edn-text))]
                    (if (str/blank? trimmed)
                      {:ok? false :edn-text (str edn-text) :contract nil
                       :validation {:ok false :errors [{:path [] :message "EDN text is empty"}]}}
                      (try
                        (let [raw-contract (reader/read-string trimmed)
                              contract     (if (= klass "agents")
                                             (actor-scope/normalize-agent-contract raw-contract)
                                             raw-contract)
                              validation   (v/validate klass contract)]
                          {:ok?        (:ok validation)
                           :edn-text   (str edn-text)
                           :contract   contract
                           :validation (dissoc validation :value)})
                        (catch :default err
                          {:ok? false :edn-text (str edn-text) :contract nil
                           :validation {:ok false
                                        :errors [{:path [] :message (str "EDN parse error: " (.-message err))}]}}))))) )
         (.catch (fn [err]
                   (if (= "ENOENT" (.-code err))
                     {:ok? false :edn-text "" :contract nil
                      :validation {:ok false :errors [{:path [] :message "Contract not found"}]}}
                     (throw err))))))))
