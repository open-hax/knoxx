(ns knoxx.backend.tools.contracts
  "Contract librarian tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! create-tool-obj sanitize-custom-tools filter-custom-tools-by-allow-set]]))

(def contract-write-params
  [:map
   [:contract_id {:description "Contract ID to create or update."} :string]
   [:edn_text {:description "Complete EDN contract text to save. Must be valid EDN with :contract/id matching contract_id."} :string]])

(def contract-validate-params
  [:map
   [:edn_text {:description "EDN contract text to validate. Returns :ok, :errors, :contract-class, and parsed :contract on success."} :string]
   [:contract_class {:optional true :description "Contract class hint: agents, roles, capabilities, actors, policies. Defaults to agents."} :string]])

(defn contract-write-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        contract-id (or (aget params "contract_id") (aget params "contractId") "")
        edn-text (or (aget params "edn_text") (aget params "ednText") "")
        base-url (or (:knoxx-base-url config) "")]
    (when (str/blank? contract-id)
      (throw (js/Error. "contract_id is required")))
    (when (str/blank? edn-text)
      (throw (js/Error. "edn_text is required")))
    (maybe-tool-update! on-update (str "Saving contract " contract-id "…"))
    (-> (js/fetch (str base-url "/api/agent/contracts/" (js/encodeURIComponent contract-id))
                  #js {:method "PUT"
                       :headers #js {"Content-Type" "text/plain; charset=utf-8"}
                       :body edn-text})
        (.then (fn [resp]
                 (.text resp)))
        (.then (fn [result-text]
                 (tool-text-result (str "Contract save result for " contract-id ":\n" result-text)
                                   {:contract_id contract-id :result result-text}))))))

(defn contract-validate-execute [_runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        edn-text (or (aget params "edn_text") (aget params "ednText") "")
        klass (or (aget params "contract_class") "agents")
        base-url (or (:knoxx-base-url config) "")]
    (when (str/blank? edn-text)
      (throw (js/Error. "edn_text is required")))
    (maybe-tool-update! on-update "Validating contract EDN…")
    (-> (js/fetch (str base-url "/api/admin/contracts/validate")
                  #js {:method "POST"
                       :headers #js {"Content-Type" "application/json"}
                       :body (.stringify js/JSON #js {:edn_text edn-text :class klass})})
        (.then (fn [resp] (.json resp)))
        (.then (fn [result]
                 (let [r (js->clj result :keywordize-keys true)
                       ok? (:ok r)
                       errors (or (:errors r) [])]
                   (tool-text-result
                    (if ok?
                      (str "✓ Contract valid"
                           (when-let [cid (get-in r [:contract :contract/id])]
                             (str " — :contract/id " cid))
                           "\nClass: " (or (:contract-class r) klass))
                      (str "✗ Validation failed:\n"
                           (str/join "\n" (map (fn [err]
                                                  (let [path (or (:path err) "root")
                                                        msg (or (:message err) "Unknown error")]
                                                    (str "  • [" (str/join " > " path) "]: " msg)))
                                                errors))))
                    (js->clj result :keywordize-keys true))))))))

(def contract-write-tool
  (partial create-tool-obj
           "contract.write"
           "Contract Write"
           "Create or update a contract by writing EDN text. Validates before saving. This is your ONLY write tool — use it to create and edit contracts."
           "Write or update a contract's EDN text."
           ["Use contract.write to save contract EDN."
            "The EDN must have :contract/id matching the contract_id parameter."
            "The server validates before saving — if validation fails, fix the EDN and retry."
            "This is the ONLY write tool available to you. No bash, no discord, no general write."
            "Before saving, call contract.validate to catch parse errors without side effects."]
           contract-write-params
           contract-write-execute))

(def contract-validate-tool
  (partial create-tool-obj
           "contract.validate"
           "Contract Validate"
           "Parse and validate EDN contract text without saving. Use this BEFORE contract.write to catch errors early."
           "Validate contract EDN before saving."
           ["Always validate before writing a contract."
            "If :ok is false, fix the errors and validate again before calling contract.write."
            "Use contract_class hint when you know the type (agents, roles, capabilities, actors, policies)."]
           contract-validate-params
           contract-validate-execute))

(defn create-contract-custom-tools
  "Create contract tools for the contract librarian agent.
   Only contract.write — the librarian reads context via general read/memory tools
   and writes EDN contracts. Validation is implicit (the write endpoint validates)."
  ([runtime config] (create-contract-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "contract.write")
                  (contract-write-tool runtime config))
                (when (allowed? "contract.validate")
                  (contract-validate-tool runtime config))]))))))

(defn create-contract-librarian-tools
  "Create the full tool suite for the contract librarian agent.
   Has all READ tools (memory, graph, semantic, websearch) plus contract.write.
   This is the toolset used in the contract editor chat panel."
  ([runtime config] (create-contract-librarian-tools runtime config nil))
  ([runtime config auth-context]
   (create-contract-librarian-tools runtime config auth-context nil))
  ([runtime config auth-context allowed-tool-ids]
   (let [contract-tools (create-contract-custom-tools runtime config auth-context)
         read-tools #js []
         memory-tools #js []]
     (-> (sanitize-custom-tools (.concat (.concat contract-tools read-tools) memory-tools))
         (filter-custom-tools-by-allow-set allowed-tool-ids)))))
