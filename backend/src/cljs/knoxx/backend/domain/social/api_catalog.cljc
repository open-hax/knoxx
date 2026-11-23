(ns knoxx.backend.domain.social.api-catalog
  "Pure operation discovery over pinned, public Discord and Bluesky schemas."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.social.api-effects :as effects]
            [knoxx.backend.domain.social.api-schema :as schema-guide]))

(defn assert-catalog!
  "Validate the decoded asset boundary before interpreting any operation."
  [catalog]
  (when-not (and (= 1 (get catalog "schemaVersion"))
                 (vector? (get catalog "operations")) (map? (get catalog "documents"))
                 (map? (get catalog "sources")))
    (throw (ex-info "Invalid platform API catalog" {:code "platform_catalog_invalid"})))
  catalog)

(def ^:private synonyms
  {"make" ["make" "create"] "start" ["start" "create"] "send" ["send" "create"]
   "edit" ["edit" "update"] "change" ["change" "update"] "remove" ["remove" "delete"]
   "manage" ["manage" "create" "update" "delete"] "reply" ["reply" "reference"]
   "server" ["server" "guild"] "room" ["room" "channel"]
   "say" ["say" "message"] "post" ["post" "message"] "dm" ["dm" "convo"]
   "lists" ["list" "lists"] "threads" ["thread" "threads"]
   "messages" ["message" "messages"] "polls" ["poll" "polls"]})
(def ^:private stop-words
  #{"a" "an" "the" "to" "my" "in" "of" "on" "how" "do" "i" "what" "can" "with" "for" "is" "are" "please"})

(defn- words
  [text]
  (-> (or text "") (str/replace #"([a-z])([A-Z])" "$1 $2")
      str/lower-case (str/replace #"[^a-z0-9]+" " ") str/trim))

(defn- query-words
  [query]
  (->> (str/split (words query) #"\s+")
       (remove #(or (str/blank? %) (contains? stop-words %)))
       (map #(get synonyms % [%])) distinct vec))

(defn- word-match?
  [text token]
  (boolean (re-find (re-pattern (str "(^|\\s)" token "(s|es)?($|\\s)")) text)))

(defn- score
  [tokens operation]
  (let [name (words (get operation "id"))
        haystack (str (get operation "search") " " (words (get operation "description"))
                      " " (words (get operation "kind")))
        documentation? (some #(some #{"schema" "permission" "definition"} %) tokens)
        penalty (if (or documentation? (contains? #{"query" "procedure" "record" "subscription"}
                                                 (get operation "kind"))) 0 16)]
    (- (reduce + (map (fn [alternatives]
                       (apply max (map #(cond (word-match? name %) 8
                                              (word-match? haystack %) 2
                                              :else 0) alternatives))) tokens)) penalty)))

(defn- rank-order
  [[rank op]]
  [(- rank) (if (= "record" (get op "kind")) 0 1)
   (if (seq (get op "nativeTools")) 0 1) (count (get op "id"))
   (get op "platform") (get op "id")])

(defn- matches-all?
  [tokens operation]
  (let [haystack (str (get operation "search") " " (words (get operation "description")))]
    (every? (fn [alternatives] (some #(word-match? haystack %) alternatives)) tokens)))

(defn- operation-view
  [operation]
  {:platform (get operation "platform") :operation (get operation "id")
   :kind (get operation "kind") :method (get operation "method") :path (get operation "route")
   :family (get operation "family") :description (get operation "description")
   :parameters (get operation "parameterNames") :requestFields (get operation "requestFields")
   :encodings (get operation "encodings") :relatedNativeTools (get operation "nativeTools")
   :recordStorage (get operation "storage")
   :effects (effects/operation-effects operation)
   :experimental (get operation "experimental")})

(defn- selected-operations
  [catalog platform]
  (cond->> (get (assert-catalog! catalog) "operations")
    platform (filter #(= platform (get % "platform")))))

(defn- families
  [operations]
  (->> operations (group-by #(vector (get % "platform") (get % "family")))
       (sort-by key)
       (mapv (fn [[[platform family] rows]]
               {:platform platform :family family :entries (count rows)}))))

(defn discover
  "Search feature language and actual request fields; page without hiding total coverage."
  [catalog {:keys [platform query limit offset] :or {query "" limit 12 offset 0}}]
  (let [operations (selected-operations catalog platform)
        tokens (query-words query)
        scored (when (seq tokens) (->> operations (map #(vector (score tokens %) %)) (filter #(pos? (first %)))))
        complete (filter #(matches-all? tokens (second %)) scored)
        ranked (sort-by rank-order (if (seq complete) complete scored))
        page (take limit (drop offset ranked)) next-offset (+ offset (count page))]
    {:query query :platform platform :totalEntries (count operations)
     :matched (count ranked) :results (mapv #(operation-view (second %)) page)
     :matching (if (or (empty? tokens) (seq complete)) "all search terms" "partial search terms; refine if needed")
     :nextOffset (when (< next-offset (count ranked)) next-offset)
     :families (when (empty? tokens) (families operations))
     :sources (get catalog "sources")
     :execution "Discovery only. Related native tools may support fewer fields than the complete API. Inspect an operation before use; current actor permissions still apply."}))

(defn- operation!
  [catalog platform id]
  (or (some #(when (= id (get % "id")) %) (selected-operations catalog platform))
      (throw (ex-info "Platform operation not found; search social.features first"
                      {:code "platform_operation_not_found" :platform platform :operation id}))))

(defn- local-pointer
  [document reference]
  (when (str/starts-with? reference "#/")
    (get-in document (mapv #(-> % (str/replace "~1" "/") (str/replace "~0" "~"))
                          (str/split (subs reference 2) #"/")))))

(defn- reference-view
  [catalog platform document-id reference]
  (let [[nsid fragment] (str/split reference #"#" 2)
        lexicon-id (if (str/blank? nsid) document-id nsid)
        discord? (= platform "discord")
        schema (if discord?
                 (local-pointer (get-in catalog ["documents" document-id]) reference)
                 (when (re-matches #"[a-zA-Z][a-zA-Z0-9-]*(?:\.[a-zA-Z][a-zA-Z0-9-]*){2,}" lexicon-id)
                   (get-in catalog ["documents" lexicon-id "defs" (or fragment "main")])))]
    (when (nil? schema)
      (throw (ex-info "Schema reference is absent or not a local public definition"
                      {:code "platform_schema_reference_invalid" :reference reference})))
    {:document (if discord? document-id lexicon-id) :reference reference :schema schema}))

(defn describe
  "Return one operation or referenced public definition, never credentials or an HTTP call."
  [catalog {:keys [platform operation reference]}]
  (let [op (operation! catalog platform operation) document-id (get op "document")
        document (get-in catalog ["documents" document-id])
        schema (get-in document (get op "pointer"))
        source (some #(when (= document-id (get % "key")) %) (get catalog "sourceDocuments"))]
    {:operation (operation-view op) :requestGuide (when-not reference (schema-guide/request-guide catalog document-id schema))
     :source source :sources (get catalog "sources")
     :definition (if reference (reference-view catalog platform document-id reference)
                     {:document document-id :schema schema})
     :security (when (= platform "discord") (or (get schema "security") (get document "security")))
     :execution "This describes the API; it does not execute it. Related native tools are convenience interfaces, not proof of full field coverage. Actor credentials and platform permissions are checked at execution."}))
