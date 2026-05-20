(ns knoxx.backend.domain.contracts.tools
  "Contract librarian tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.tools :refer [maybe-tool-update! create-tool-obj sanitize-custom-tools filter-custom-tools-by-allow-set]]))

(def contract-list-params
  [:map
  [:contract_class {:optional true :description "Contract class to list: agents, roles, capabilities, actors, policies, source_modes, sources, models, model_families, actions, pipelines, triggers, sub_agents. Defaults to agents."} :string]])

(def contract-read-params
  [:map
   [:contract_id {:description "Contract ID to read. For roles/capabilities, use the body identity slug such as fork-tales-composer or contract-write."} :string]
   [:contract_class {:optional true :description "Contract class: agents, roles, capabilities, actors, policies, source_modes, sources, models, model_families, actions, pipelines, triggers, sub_agents. Defaults to agents."} :string]])

(def contract-write-params
  [:map
   [:contract_id {:description "Contract ID to create or update. Must match the contract identity inside edn_text."} :string]
   [:edn_text {:description "Complete EDN contract text to save. Must be valid EDN and the record id must match contract_id."} :string]
   [:contract_class {:optional true :description "Contract class to save. Defaults to agents; use roles/capabilities/etc when editing non-agent contracts."} :string]])

(def contract-validate-params
  [:map
   [:edn_text {:description "EDN contract text to validate. Returns :ok, :errors, :warnings, :contract-class, and parsed :contract on success."} :string]
   [:contract_class {:optional true :description "Contract class hint: agents, roles, capabilities, actors, policies, source_modes, sources, models, model_families, actions, pipelines, triggers, sub_agents. Defaults to agents."} :string]])

(defn- param-value
  [params snake camel fallback]
  (or (aget params snake)
      (aget params camel)
      fallback))

(defn- param-string
  [params snake camel fallback]
  (some-> (param-value params snake camel fallback)
          str
          str/trim
          not-empty))

(defn- param-text
  [params snake camel fallback]
  (some-> (param-value params snake camel fallback)
          str))

(defn- contract-class-param
  [params]
  (param-string params "contract_class" "contractClass" "agents"))

(defn- class-query
  [klass]
  (str "?kind=" (js/encodeURIComponent (or klass "agents"))))

(defn- fetch-text-result
  [url options]
  (-> (js/fetch url options)
      (.then (fn [resp]
               (-> (.text resp)
                   (.then (fn [text]
                            {:ok (.-ok resp)
                             :status (.-status resp)
                             :text text})))))))

(defn contract-list-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        klass (contract-class-param params)
        base-url (or (:knoxx-base-url config) "")]
    (maybe-tool-update! on-update (str "Listing " klass " contracts…"))
    (-> (fetch-text-result (str base-url "/api/agent/contracts" (class-query klass))
                           #js {:method "GET"})
        (.then (fn [{:keys [ok status text]}]
                 (tool-text-result
                  (str (if ok "Contract list" "Contract list failed")
                       " for class " klass " (HTTP " status "):\n" text)
                  {:contract_class klass :ok ok :status status :result text}))))))

(defn contract-read-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        contract-id (param-string params "contract_id" "contractId" "")
        klass (contract-class-param params)
        base-url (or (:knoxx-base-url config) "")]
    (when (str/blank? contract-id)
      (throw (js/Error. "contract_id is required")))
    (maybe-tool-update! on-update (str "Reading " klass "/" contract-id "…"))
    (-> (fetch-text-result (str base-url "/api/agent/contracts/" (js/encodeURIComponent contract-id) (class-query klass))
                           #js {:method "GET"})
        (.then (fn [{:keys [ok status text]}]
                 (tool-text-result
                  (str (if ok "Contract read" "Contract read failed")
                       " for " klass "/" contract-id " (HTTP " status "):\n" text)
                  {:contract_id contract-id :contract_class klass :ok ok :status status :edn_text text}))))))

(defn contract-write-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        contract-id (param-string params "contract_id" "contractId" "")
        edn-text (param-text params "edn_text" "ednText" "")
        klass (contract-class-param params)
        base-url (or (:knoxx-base-url config) "")]
    (when (str/blank? contract-id)
      (throw (js/Error. "contract_id is required")))
    (when (str/blank? edn-text)
      (throw (js/Error. "edn_text is required")))
    (maybe-tool-update! on-update (str "Saving " klass "/" contract-id "…"))
    (-> (fetch-text-result (str base-url "/api/agent/contracts/" (js/encodeURIComponent contract-id) (class-query klass))
                           #js {:method "PUT"
                                :headers #js {"Content-Type" "text/plain; charset=utf-8"}
                                :body edn-text})
        (.then (fn [{:keys [ok status text]}]
                 (tool-text-result
                  (str (if ok "Contract save result" "Contract save failed")
                       " for " klass "/" contract-id " (HTTP " status "):\n" text)
                  {:contract_id contract-id :contract_class klass :ok ok :status status :result text}))))))

(defn contract-validate-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        edn-text (param-text params "edn_text" "ednText" "")
        klass (contract-class-param params)
        base-url (or (:knoxx-base-url config) "")]
    (when (str/blank? edn-text)
      (throw (js/Error. "edn_text is required")))
    (maybe-tool-update! on-update (str "Validating " klass " contract EDN…"))
    (-> (js/fetch (str base-url "/api/agent/contracts/validate")
                  #js {:method "POST"
                       :headers #js {"Content-Type" "application/json"}
                       :body (.stringify js/JSON #js {:edn_text edn-text :contract_class klass})})
        (.then (fn [resp] (.json resp)))
        (.then (fn [result]
                 (let [r (js->clj result :keywordize-keys true)
                       ok? (:ok r)
                       errors (or (:errors r) [])
                       warnings (or (:warnings r) [])]
                   (tool-text-result
                    (str
                     (if ok?
                       (str "✓ Contract valid"
                            (when-let [cid (get-in r [:contract :contract/id])]
                              (str " — :contract/id " cid))
                            "\nClass: " (or (:contractClass r) (:contract-class r) klass))
                       (str "✗ Validation failed:\n"
                            (str/join "\n" (map (fn [err]
                                                   (let [path (or (:path err) "root")
                                                         msg (or (:message err) "Unknown error")]
                                                     (str "  • [" (str/join " > " path) "]: " msg)))
                                                 errors))))
                     (when (seq warnings)
                       (str "\nWarnings:\n"
                            (str/join "\n" (map (fn [warning]
                                                   (let [path (or (:path warning) "root")
                                                         msg (or (:message warning) "Warning")]
                                                     (str "  • [" (str/join " > " path) "]: " msg)))
                                                 warnings)))))
                    r)))))))

(def contract-list-tool
  (partial create-tool-obj
           "contract.list"
           "Contract List"
           "List contract IDs by class before reading or writing. Use this to discover canonical contract identities instead of guessing paths."
           "List contract IDs by class."
           ["Call contract.list before creating or editing when you do not know the exact contract id."
            "Use contract_class for non-agent contracts such as roles or capabilities."
            "Never create data/contracts or :data/* filesystem folders as a substitute for reading canonical contracts."]
           contract-list-params
           contract-list-execute))

(def contract-read-tool
  (partial create-tool-obj
           "contract.read"
           "Contract Read"
           "Read the exact EDN for an existing contract by class and id. Use this before patching or cloning behavior."
           "Read contract EDN."
           ["Always read the target or nearest existing contract before writing a replacement."
            "Use the returned EDN as the source of truth; do not infer contract shape from memory."
            "Use contract_class for non-agent contracts such as roles or capabilities."]
           contract-read-params
           contract-read-execute))

(def contract-write-tool
  (partial create-tool-obj
           "contract.write"
           "Contract Write"
           "Create or update a contract by writing EDN text. Validates before saving. This is your ONLY contract write tool — use contract.list/read first when possible."
           "Write or update a contract's EDN text."
           ["Use contract.write to save contract EDN."
            "Set contract_class correctly; defaults to agents, but roles/capabilities/actors require their own class."
            "The EDN identity must match the contract_id parameter."
            "The server validates before saving — if validation fails, fix the EDN and retry."
            "Before saving, call contract.validate to catch parse errors and shape warnings without side effects."
            "Do not invent mutable :data folders or data/contracts files. Contract :data is static config unless a real state backend says otherwise."]
           contract-write-params
           contract-write-execute))

(def contract-validate-tool
  (partial create-tool-obj
           "contract.validate"
           "Contract Validate"
           "Parse and validate EDN contract text without saving. Use this BEFORE contract.write to catch errors and warnings early."
           "Validate contract EDN before saving."
           ["Always validate before writing a contract."
            "If :ok is false, fix the errors and validate again before calling contract.write."
            "Treat :warnings as real drift signals; fix them unless preserving legacy behavior intentionally."
            "Use contract_class hint when you know the type."]
           contract-validate-params
           contract-validate-execute))

(defn create-contract-custom-tools
  "Create contract tools for the contract librarian agent."
  ([runtime config] (create-contract-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "contract.list")
                  (contract-list-tool runtime config))
                (when (allowed? "contract.read")
                  (contract-read-tool runtime config))
                (when (allowed? "contract.write")
                  (contract-write-tool runtime config))
                (when (allowed? "contract.validate")
                  (contract-validate-tool runtime config))]))))))

(defn create-contract-librarian-tools
  "Create the contract tool suite for the contract librarian agent."
  ([runtime config] (create-contract-librarian-tools runtime config nil))
  ([runtime config auth-context]
   (create-contract-librarian-tools runtime config auth-context nil))
  ([runtime config auth-context allowed-tool-ids]
   (let [contract-tools (create-contract-custom-tools runtime config auth-context)]
     (-> (sanitize-custom-tools contract-tools)
         (filter-custom-tools-by-allow-set allowed-tool-ids)))))
