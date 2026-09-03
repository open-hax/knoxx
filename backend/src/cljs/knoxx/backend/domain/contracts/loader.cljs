;; contracts/loader.cljs
(ns knoxx.backend.domain.contracts.loader
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.domain.resources.namespace-file :as ns-file]
            [knoxx.backend.law.contracts :as v]
            [knoxx.backend.shape.resource-identity :as resource-identity]
            ["node:fs" :as node-fs]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

;; ── Constants ──────────────────────────────────────────────────────────────

(def contract-class-order
  ["agents" "actors" "roles" "capabilities" "policies" "authentication" "mcp_servers"
   "generators" "schedules" "source_modes" "sources" "model_families" "models" "runtime_features" "ingest_sources" "actions" "triggers" "stores" "sub_agents" "cms" "documents" "gardens" "publications"])

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
  (let [configured (configured-contracts-dir config)
        authored (if (default-configured-contracts-dir? configured)
                   ["../contracts" "contracts"
                    "packages/agents/knoxx/contracts"
                    "orgs/open-hax/openplanner/packages/agents/knoxx/contracts"]
                   [configured])
        generated (some-> (:generated-contracts-dir config) str str/trim not-empty)]
    (cond-> authored generated (conj generated))))

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

(def ^:private class-aliases
  "Every spelling that names a contract class, mapped to the canonical one.

   Data rather than a `case` so adding a class is a line in a table instead of
   a branch, and so the table stays readable as it grows past twenty classes."
  (into {}
        (mapcat (fn [[canonical & aliases]]
                  (map (fn [alias] [alias canonical]) (cons canonical aliases))))
        [["agents" "agent" "contract" "contracts" nil ""]
         ["actors" "actor" "user" "users" "human" "humans"]
         ["roles" "role"]
         ["capabilities" "cap" "caps" "capability"]
         ["policies" "policy"]
         ["authentication" "auth" "auth-method" "auth-methods" "auth_method" "auth_methods"]
         ["mcp_servers" "mcp-server" "mcp-servers" "mcp_server"]
         ["generators" "generator"]
         ["schedules" "schedule"]
         ["source_modes" "source-mode" "source-modes" "source_mode"]
         ["sources" "source" "runtime-source" "runtime-sources" "runtime_source" "runtime_sources"]
         ["runtime_features" "runtime-feature" "runtime-features" "runtime_feature" "runtime"]
         ["model_families" "model-family" "model-families" "model_family"]
         ["models" "model"]
         ["ingest_sources" "ingest-source" "ingest-sources" "ingest_source"]
         ["cms" "cms-config" "cms-configs" "cms_config" "cms_configs"
          "cms-block-registry" "cms-block-registries" "cms-template-registry"
          "cms-template-registries" "cms-templates" "cms-template" "cms-templates-registry"]
         ["actions" "action"]
         ["pipelines" "pipeline"]
         ["triggers" "trigger"]
         ["stores" "store"]
         ["sub_agents" "sub-agent" "sub-agents" "sub_agent"]
         ["documents" "document"]
         ["gardens" "garden"]
         ["publications" "publication"]]))

(defn normalize-contract-class
  [value]
  (let [raw (some-> value
                    (cond-> (keyword? value) name
                            (not (keyword? value)) str)
                    str/trim
                    str/lower-case)]
    (or (get class-aliases raw)
        (throw (js/Error. (str "Unknown contract class: " value))))))

;; ── Stderr logging ─────────────────────────────────────────────────────────

(defn- stderr!
  [& parts]
  (.write js/process.stderr (str (str/join "" parts) "\n")))

(defonce ^:private sync-contract-record-cache* (atom nil))
(def ^:private sync-contract-record-cache-ttl-ms 2000)

(defn- now-ms
  []
  (.now js/Date))

(defn invalidate-sync-contract-cache!
  []
  (reset! sync-contract-record-cache* nil))

;; ── Discovery ──────────────────────────────────────────────────────────────

(defn entry->file-path
  "Returns absolute path if entry is a non-hidden .edn file, else nil."
  [ent]
  (when (and (.isFile ent) (contract-edn-filename? (.-name ent)))
    (.join path (.-parentPath ent) (.-name ent))))

(defn ^:async discover-contract-files!
  "Find all .edn files under root via recursive readdir. Returns Promise<vector<string>>."
  [root]
  (try
    (let [entries (await (.readdir fs root #js {:withFileTypes true :recursive true}))]
      (->> (js/Array.from entries)
           (keep entry->file-path)
           vec))
    (catch :default err
      (stderr! "[contracts] readdir failed: " root " — " (.-message err))
      [])))

;; ── Per-file parsing ───────────────────────────────────────────────────────

(defn- keyword->str
  [v]
  (if (keyword? v) (name v) (str v)))

(defn qualified-keyword->str
  "Like keyword->str, but keeps the namespace segment. Document/garden/
   publication ids are namespace-qualified on purpose; stringifying them with
   plain `name` would collide distinct namespaces (:tenant-a/foo and
   :tenant-b/foo both becoming \"foo\"). Public so lookup call sites (e.g.
   knoxx.backend.domain.resources.loader) can normalize a keyword id the
   same way it was normalized when the record was indexed."
  [v]
  (if (keyword? v) (subs (str v) 1) (str v)))

(defn- extract-contract-identity
  [raw]
  ;; IMPORTANT: prefer structural/canonical class inference before the raw
  ;; :contract/kind value. Otherwise namespaced/dashed kinds like :source-mode
  ;; can be handed to normalize-contract-class before older aliases know them.
  ;; Also prefer :model/id over :model-family/id: many model contracts include
  ;; both keys, but they must be classified as "models".
  (let [kind (some-> (or (when (:actor/id raw) "actors")
                          (when (:role/id raw) "roles")
                          (when (:cap/id raw) "capabilities")
                          (when (:model/id raw) "models")
                          (when (:generator/id raw) "generators")
                          (when (:schedule/id raw) "schedules")
                          (when (= :generator (:contract/kind raw)) "generators")
                          (when (= :schedule (:contract/kind raw)) "schedules")
                          (when (:source-mode/id raw) "source_modes")
                          (when (= :source-mode (:contract/kind raw)) "source_modes")
                          (when (= :source (:contract/kind raw)) "sources")
                          (when (:runtime-feature/id raw) "runtime_features")
                          (when (= :runtime-feature (:contract/kind raw)) "runtime_features")
                          (when (:model-family/id raw) "model_families")
                          (when (:document/id raw) "documents")
                          (when (:garden/id raw) "gardens")
                          (when (:publication/id raw) "publications")
                          (:contract/kind raw)
                          (:kind raw))
                      keyword->str str/trim not-empty)
        qualified-id (some-> (or (:document/id raw) (:garden/id raw) (:publication/id raw))
                              qualified-keyword->str str/trim not-empty)
        id   (or qualified-id
                 (some-> (or (:contract/id raw) (:id raw)
                             (:actor/id raw) (:role/id raw) (:cap/id raw)
                             (:model/id raw) (:model-family/id raw)
                             (:generator/id raw) (:schedule/id raw)
                             (:source-mode/id raw)
                             (:source/id raw)
                             (:runtime-feature/id raw))
                         keyword->str str/trim not-empty))]
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
            ;; A rejected record is reported rather than dropped, so callers that
            ;; must not silently omit a resource can surface it as a blocker.
            ;; `load-all-contracts!` filters these out, so every existing lookup
            ;; caller sees exactly what it saw before.
            {:ok? false
             :id id
             :contractClass kind
             :file-path file-path
             :errors (:errors valid)})))))

(defn- namespace-resource-record
  "Validate one expanded namespace resource definition into a contract record.

   Identity is canonicalized BEFORE validation. Document, garden, and
   publication ids are qualified keywords in their Malli shapes, but a manifest
   entry declares its namespace once and writes the local id — so validating
   first would fail those definitions and drop them here, and the resource
   would never reach any projection."
  [file-path edn-text {:resource/keys [kind definition]}]
  (let [klass (normalize-contract-class (name kind))
        definition (resource-identity/canonicalize-identity kind definition)
        valid (v/validate klass definition)]
    (if (:ok valid)
      {:ok? true
       :id (:contract/id definition)
       :contractClass klass
       :contract definition
       :file-path file-path
       :edn-text (str edn-text)}
      (do (stderr! "[contracts] namespace resource validation failed: " file-path
                   " " (pr-str (:resource/qualified-id definition))
                   " — " (pr-str (:errors valid)))
          {:ok? false
           :id (:contract/id definition)
           :contractClass klass
           :file-path file-path
           :errors (:errors valid)}))))

(defn parse-contract-file-records!
  "Parse + validate a single .edn file into a vector of contract records.
   Namespace files ({:namespace ... :resources [...]}) expand to one record per
   interpreter kind per resource entry; plain contract files yield one record."
  [file-path edn-text]
  (try
    (let [raw (reader/read-string (str edn-text))]
      (if (ns-file/namespace-file? raw)
        (->> (ns-file/namespace-file-definitions raw)
             (keep (partial namespace-resource-record file-path edn-text))
             vec)
        (if-let [record (validate-and-build file-path edn-text raw)]
          [record]
          [])))
    (catch :default err
      (stderr! "[contracts] parse error: " file-path " — " (.-message err))
      [])))

(defn- file-level-rejection
  "A rejected record standing for a file that produced no record at all.

   `parse-contract-file-records!` answers `[]` for a file it cannot read as EDN,
   and every lookup caller depends on that contract — so the evidence is
   reconstructed here, in the only path that exposes rejected records. Without
   it a malformed publication file leaves no trace and the projection answers
   200 with that intent silently absent. It carries no kind: an unparsed file
   has no readable identity to take one from."
  [file-path reason message]
  {:ok? false
   :file-path file-path
   :errors [(cond-> {:file reason}
              message (assoc :message message))]})

(defn- ^:async read-contract-file!
  [file-path]
  (try
    (let [records (parse-contract-file-records!
                   file-path
                   (await (.readFile fs file-path "utf8")))]
      (if (seq records)
        records
        [(file-level-rejection file-path :unparseable nil)]))
    (catch :default err
      (stderr! "[contracts] read error: " file-path " — " (.-message err))
      [(file-level-rejection file-path :unreadable (.-message err))])))

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

(defn discover-contract-files-sync
  "Synchronously find all .edn files under root. Runtime sync consumers must
   still parse the files through parse-contract-file! so identity comes from the
   contract body, not from the directory or filename."
  [root]
  (->> (.readdirSync node-fs root #js {:withFileTypes true :recursive true})
       array-seq
       (keep entry->file-path)
       vec))

(defn- load-all-contracts-sync-uncached
  [config]
  (->> (contract-root-paths config)
       (mapcat discover-contract-files-sync)
       distinct
        (mapcat (fn [file-path]
                  (parse-contract-file-records!
                   file-path
                   (.readFileSync node-fs file-path "utf8"))))
        ;; Same as the async path: rejected records never reach a lookup caller.
        (filterv :ok?)
        dedup-contracts))

(defn load-all-contracts-sync
  "Synchronously load all contract records through the same parser/validator and
   [contractClass id] dedup path as load-all-contracts!. This is the escape hatch
   for startup/runtime code that cannot await but still must not use filepath or
   folder placement as contract truth.

   A short process-local cache prevents startup/model resolution from reparsing
   the entire contract tree dozens of times in the same tick. Invalid contracts
   are still omitted; they must not pin the event loop or block HTTP startup."
  [config]
  (let [now (now-ms)
        roots (contract-root-paths config)
        cached @sync-contract-record-cache*]
    (if (and cached
             (= roots (:roots cached))
             (< (- now (:ts cached)) sync-contract-record-cache-ttl-ms))
      (:records cached)
      (let [records (load-all-contracts-sync-uncached config)]
        (reset! sync-contract-record-cache* {:ts now :roots roots :records records})
        records))))

(defn find-contract-record-sync
  [config contract-class contract-id]
  (let [klass (normalize-contract-class contract-class)
        wanted-id (some-> contract-id str str/trim not-empty)]
    (some (fn [record]
            (when (and (= klass (:contractClass record))
                       (= wanted-id (:id record)))
              record))
          (load-all-contracts-sync config))))

(defn contract-sync
  [config contract-class contract-id]
  (some-> (find-contract-record-sync config contract-class contract-id)
          :contract))

;; ── Public API ─────────────────────────────────────────────────────────────

(defn ^:async load-all-contract-records!
  "Every parsed+validated record, WITHOUT `[kind id]` dedup.

   `dedup-contracts` is first-wins, so two files declaring the same canonical
   id with different payloads silently collapse to whichever the filesystem
   enumerated first. Callers that must detect that collision rather than
   inherit it — the publication projection, whose whole contract is that a
   conflicting identity fails deterministically — need the undeduped records.
   Ordinary lookup callers should keep using `load-all-contracts!`."
  [config]
  (let [roots (contract-root-paths config)
        file-lists (await (js/Promise.all (clj->js (map discover-contract-files! roots))))
        files (->> (js/Array.from file-lists)
                   (mapcat #(js/Array.from %))
                   distinct
                   vec)
        results (await (js/Promise.all (clj->js (map read-contract-file! files))))]
    (->> (js/Array.from results)
         (remove nil?)
         (mapcat identity)
         (remove nil?)
         vec)))

(defn ^:async load-all-contracts!
  "Discover all .edn files under all contract roots, parse+validate each,
   deduplicate on [kind id]. Returns Promise<vector<contract-record>>.

   Rejected records are filtered out here, so this function's contract is
   unchanged for every lookup caller. Only `load-all-contract-records!` exposes
   them, for callers that must surface an invalid resource as a blocker instead
   of silently omitting it."
  [config]
  (dedup-contracts (filterv :ok? (await (load-all-contract-records! config)))))

(defn ^:async list-contract-ids!
  ([config] (list-contract-ids! config "agents"))
  ([config contract-class]
   (let [klass (normalize-contract-class contract-class)
         all (await (load-all-contracts! config))]
     (->> all
          (filter #(= (:contractClass %) klass))
          (mapv :id)
          sort
          vec))))

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
         identity-path (some-> (find-contract-record-sync config klass id) :file-path)
         existing (some (fn [root]
                          (find-contract-file-recursive root klass filename))
                        (contract-root-paths config))]
     (or identity-path
         existing
         (.join path (resolve-contracts-dir config) klass filename)))))

(defn role-file-path       [config slug]     (contract-file-path config "roles"        slug))
(defn capability-file-path [config slug]     (contract-file-path config "capabilities" slug))
(defn actor-file-path      [config actor-id] (contract-file-path config "actors"       actor-id))

(defn ^:async read-edn-file!
  [file-path]
  (let [text (await (.readFile fs file-path "utf8"))]
    (reader/read-string (str text))))

(defn ensure-dir!
  [dir]
  (.mkdir fs dir #js {:recursive true}))

(defn ^:async write-edn-file!
  [file-path edn-text]
  (invalidate-sync-contract-cache!)
  (let [dir (.dirname path file-path)]
    (await (ensure-dir! dir))
    (await (.writeFile fs file-path edn-text "utf8"))))

(defn list-contract-ids-sync
  [config contract-class]
  (let [klass (normalize-contract-class contract-class)]
    (->> (load-all-contracts-sync config)
         (filter #(= klass (:contractClass %)))
         (map :id)
         distinct
         sort
         vec)))

(defn ^:async load-contract!
  ([config contract-id] (load-contract! config "agents" contract-id))
  ([config contract-class contract-id]
   (let [klass (normalize-contract-class contract-class)
         wanted-id (some-> contract-id str str/trim not-empty)
         records (await (load-all-contracts! config))]
     (if-let [record (some (fn [candidate]
                             (when (and (= klass (:contractClass candidate))
                                        (= wanted-id (:id candidate)))
                               candidate))
                           records)]
       {:ok? true
        :edn-text (:edn-text record)
        :contract (if (= klass "agents")
                    (actor-scope/normalize-agent-contract (:contract record))
                    (:contract record))
        :validation {:ok true :errors []}}
       {:ok? false :edn-text "" :contract nil
        :validation {:ok false :errors [{:path [] :message "Contract not found"}]}}))))
