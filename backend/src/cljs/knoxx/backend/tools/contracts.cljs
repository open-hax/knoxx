(ns knoxx.backend.tools.contracts
  "Contract librarian tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional sanitize-custom-tools filter-custom-tools-by-allow-set agent-custom-tool-suite]]))

(defn create-contract-custom-tools
  "Create contract tools for the contract librarian agent.
   Only contract.write — the librarian reads context via general read/memory tools
   and writes EDN contracts. Validation is implicit (the write endpoint validates)."
  ([runtime config] (create-contract-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))

         ;; Contract write tool — accepts EDN text, validates, stores
         contract-write-params (.Object Type
                                        #js {:contract_id (.String Type #js {:description "Contract ID to create or update."})
                                             :edn_text (.String Type #js {:description "Complete EDN contract text to save. Must be valid EDN with :contract/id matching contract_id."})})

         base-url (or (:knoxx-base-url config) "")

         contract-write-execute (fn [_tool-call-id params a b c]
                                  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                        contract-id (or (aget params "contract_id") (aget params "contractId") "")
                                        edn-text (or (aget params "edn_text") (aget params "ednText") "")]
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

         ;; Contract validate tool — dry-run parse+validate, no write
         contract-validate-params (.Object Type
                                   #js {:edn_text (.String Type #js {:description "EDN contract text to validate. Returns :ok, :errors, :contract-class, and parsed :contract on success."})
                                       :contract_class (type-optional Type "Contract class hint: agents, roles, capabilities, actors, policies. Defaults to agents.")})

         contract-validate-execute (fn [_tool-call-id params a b c]
                                    (let [on-update   (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                          edn-text    (or (aget params "edn_text") (aget params "ednText") "")
                                          klass       (or (aget params "contract_class") "agents")]
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
                                                             (str/join "\n" (map #(str "  • " %) errors))))
                                                        (js->clj result :keywordize-keys true))))))))

         ]

     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "contract.write")
                  (doto (js-obj)
                    (aset "name" "contract.write")
                    (aset "label" "Contract Write")
                    (aset "description" "Create or update a contract by writing EDN text. Validates before saving. This is your ONLY write tool — use it to create and edit contracts.")
                    (aset "promptSnippet" "Write or update a contract's EDN text.")
                    (aset "promptGuidelines" (clj->js ["Use contract.write to save contract EDN."
                                                       "The EDN must have :contract/id matching the contract_id parameter."
                                                       "The server validates before saving — if validation fails, fix the EDN and retry."
                                                       "This is the ONLY write tool available to you. No bash, no discord, no general write."
                                                       "Before saving, call contract.validate to catch parse errors without side effects."]))
                    (aset "parameters" contract-write-params)
                    (aset "execute" contract-write-execute)))

                (when (allowed? "contract.validate")
                  (doto (js-obj)
                    (aset "name" "contract.validate")
                    (aset "label" "Contract Validate")
                    (aset "description" "Parse and validate EDN contract text without saving. Use this BEFORE contract.write to catch errors early.")
                    (aset "promptSnippet" "Validate contract EDN before saving.")
                    (aset "promptGuidelines" (clj->js ["Always validate before writing a contract."
                                                       "If :ok is false, fix the errors and validate again before calling contract.write."
                                                       "Use contract_class hint when you know the type (agents, roles, capabilities, actors, policies)."]))
                    (aset "parameters" contract-validate-params)
                    (aset "execute" contract-validate-execute)))]))))))

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
