(ns knoxx.backend.infra.resources.handlers
  "Serve validated resource reads, writes, copies, and agent compatibility operations."
  (:require [knoxx.backend.domain.contracts.resolve :as contracts-resolve]
            [knoxx.backend.domain.resources.editing :as editing]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.resources.presentation :as presentation]
            [knoxx.backend.domain.resources.validation :as validation]
            [knoxx.backend.extern.resource-files :as io]
            [knoxx.backend.infra.resources.writes :as writes]))

(defn ^:async handle-list-resources
  "List all resources, optionally filtered by resource kind/class.
   Public so tests can call it directly."
  [do-json config resource-kind]
  (try
    (let [all (await (resources/load-all-resources! config))
          resource-class (when resource-kind (editing/normalize-resource-class resource-kind))
          selected (cond->> all
                     resource-class (filter #(= (:resource/class %)
                                                resource-class))
                     :always        (sort-by (juxt :resource/class :resource/id))
                     :always        vec)]
      (do-json 200 {:resources (mapv presentation/resource-list-summary selected)}))
    (catch :default err
      (do-json 500 {:detail (str "Failed to list resources: " (ex-message err))}))))

(defn ^:async handle-list-contracts
  "Compatibility alias for old /contracts clients."
  [do-json config contract-class]
  (try
    (let [all (await (resources/load-all-resources! config))
          resource-class (when contract-class (editing/normalize-resource-class contract-class))
          selected (cond->> all
                     resource-class (filter #(= (:resource/class %)
                                                resource-class))
                     :always        (sort-by (juxt :resource/class :resource/id))
                     :always        vec)]
      (do-json 200 {:contracts (mapv presentation/contract-list-summary selected)}))
    (catch :default err
      (do-json 500 {:detail (str "Failed to list contracts: " (ex-message err))}))))

(defn ^:async handle-get-resource
  "Read and validate one resource, preserving missing-file and parse diagnostics."
  [do-json config resource-kind resource-id]
  (try
    (let [edn-text (await (io/read-text! (resources/resource-file-path config resource-kind resource-id)))
          resource-class (editing/normalize-resource-class resource-kind)
          validation (validation/validate-resource-edn resource-class (or edn-text ""))]
      (do-json 200 {:resourceClass resource-class
                    :resource/id resource-id
                    :ednText (or edn-text "")
                    :resource (presentation/wire-value (:contract validation))
                    :validation (dissoc validation :contract)}))
    (catch :default err
      (if (= "ENOENT" (io/error-code err))
        (do-json 404 {:detail (str "Resource not found: " resource-id)})
        (do-json 500 {:detail (str "Failed to read resource: " (ex-message err))})))))

(defn ^:async handle-get-contract
  "Read a resource using the legacy contract response shape."
  [do-json config contract-class contract-id]
  (try
    (let [edn-text (await (io/read-text! (resources/resource-file-path config contract-class contract-id)))
          validation (validation/validate-contract-edn contract-class (or edn-text ""))]
      (do-json 200 {:contractClass (editing/normalize-contract-class contract-class)
                    :ednText (or edn-text "")
                    :contract (presentation/wire-value (:contract validation))
                    :validation (dissoc validation :contract)}))
    (catch :default err
      (if (= "ENOENT" (io/error-code err))
        (do-json 404 {:detail (str "Contract not found: " contract-id)})
        (do-json 500 {:detail (str "Failed to read contract: " (ex-message err))})))))

(defn- ^:async persist-resource!
  [do-json config {:keys [resource-class route-id edn-text ctx admit! parsed validation-out]}]
  (try
         (let [file-path (resources/resource-file-path config resource-class route-id)
               admission (await
                          (writes/write-resource-and-admit!
                           config file-path edn-text
                           (when admit!
                             (fn []
                               (writes/admit-saved-publication-resource!
                                config ctx resource-class parsed admit!)))))]
             (do-json 200 (cond-> {:ok true
                                   :resourceClass resource-class
                                   :resource/id route-id
                                   :ednText edn-text
                                   :resource (presentation/wire-value parsed)
                                   :validation validation-out}
                            admission (assoc :admission (presentation/wire-value admission)))))
         (catch :default err
           (let [status (or (:status (ex-data err)) 500)]
             (do-json status {:ok false
                              :detail (str "Failed to save and admit resource: " (ex-message err))
                              :code (:code (ex-data err))})))))

(defn ^:async handle-save-resource
  "Validate route identity before persisting and admitting a resource."
  ([do-json config resource-kind resource-id edn-text]
   (handle-save-resource do-json config resource-kind resource-id edn-text nil nil))
  ([do-json config resource-kind resource-id edn-text ctx admit!]
   (let [resource-class (editing/normalize-resource-class resource-kind)
         validation (validation/validate-resource-edn resource-class edn-text)
         validation-out (dissoc validation :contract)
         parsed (:contract validation)
         parsed-id (editing/parsed-resource-id resource-class parsed)
         route-id (str resource-id)]
     (cond
       (not (:ok validation))
       (do-json 400 {:ok false
                     :detail "Resource EDN failed validation"
                     :validation validation-out})

       (and parsed-id (not= route-id parsed-id))
       (do-json 400 {:ok false
                     :detail "Refusing to save resource: record id does not match route resourceId"
                     :routeResourceId route-id
                     :ednResourceId parsed-id
                     :validation validation-out})

       :else
       (persist-resource! do-json config
                          {:resource-class resource-class :route-id route-id :edn-text edn-text
                           :ctx ctx :admit! admit! :parsed parsed :validation-out validation-out})))))

(defn- ^:async persist-contract!
  [do-json config {:keys [resource-class route-id edn-text ctx admit! parsed validation-out]}]
  (try
         (let [file-path (resources/resource-file-path config resource-class route-id)
               admission (await
                          (writes/write-resource-and-admit!
                           config file-path edn-text
                           (when admit!
                             (fn []
                               (writes/admit-saved-publication-resource!
                                config ctx resource-class parsed admit!)))))]
           (do-json 200 (cond-> {:ok true
                                 :contractClass resource-class
                                 :ednText edn-text
                                 :contract (presentation/wire-value parsed)
                                 :validation validation-out}
                          admission (assoc :admission (presentation/wire-value admission)))))
         (catch :default err
           (let [status (or (:status (ex-data err)) 500)]
             (do-json status {:ok false
                              :detail (str "Failed to save and admit contract: "
                                           (ex-message err))
                              :code (:code (ex-data err))})))))

(defn ^:async handle-save-contract
  "Validate route identity before persisting with the legacy contract response shape."
  ([do-json config contract-class contract-id edn-text]
   (handle-save-contract do-json config contract-class contract-id edn-text nil nil))
  ([do-json config contract-class contract-id edn-text ctx admit!]
   (let [klass (editing/normalize-contract-class contract-class)
         validation (validation/validate-contract-edn klass edn-text)
         validation-out (dissoc validation :contract)
         parsed (:contract validation)
         parsed-id (editing/parsed-resource-id klass parsed)
         route-id (str contract-id)]
     (cond
       (not (:ok validation))
       (do-json 400 {:ok false
                     :detail "Contract EDN failed validation"
                     :validation validation-out})

       (and parsed-id (not= route-id parsed-id))
       (do-json 400 {:ok false
                     :detail "Refusing to save contract: record id does not match route contractId"
                     :routeContractId route-id
                     :ednContractId parsed-id
                     :validation validation-out})

       :else
       (persist-contract! do-json config
                          {:resource-class klass :route-id route-id :edn-text edn-text
                           :ctx ctx :admit! admit! :parsed parsed :validation-out validation-out})))))

(defn ^:async handle-copy-resource
  "Copy source EDN with a rewritten identity, then apply normal save admission."
  ([do-json config resource-kind source-id new-id]
   (handle-copy-resource do-json config resource-kind source-id new-id nil nil))
  ([do-json config resource-kind source-id new-id ctx admit!]
   (try
     (let [source-edn (await (io/read-text! (resources/resource-file-path config resource-kind source-id)))
           text (or source-edn "")
           cloned (editing/update-resource-id-in-edn-text resource-kind text new-id)]
       (await (handle-save-resource do-json config resource-kind new-id cloned ctx admit!)))
     (catch :default err
       (do-json 500 {:detail (str "Failed to copy resource: " (ex-message err))})))))

(defn ^:async handle-copy-contract
  "Copy legacy contract EDN through the same validated save lifecycle."
  ([do-json config contract-class source-id new-id]
   (handle-copy-contract do-json config contract-class source-id new-id nil nil))
  ([do-json config contract-class source-id new-id ctx admit!]
   (try
     (let [source-edn (await (io/read-text! (resources/resource-file-path config contract-class source-id)))
           text (or source-edn "")
           cloned (editing/update-resource-id-in-edn-text contract-class text new-id)]
       (await (handle-save-contract do-json config contract-class new-id cloned ctx admit!)))
     (catch :default err
       (do-json 500 {:detail (str "Failed to copy contract: " (ex-message err))})))))

(defn handle-validate-resource
  "Return resource validation diagnostics without changing persisted state."
  [do-json resource-kind edn-text]
  (let [resource-class (editing/normalize-resource-class resource-kind)]
    (do-json 200 (assoc (presentation/wire-validation (validation/validate-resource-edn resource-class edn-text))
                        :resourceClass resource-class))))

(defn handle-validate-contract
  "Return validation diagnostics in the legacy contract response shape."
  [do-json contract-class edn-text]
  (do-json 200 (assoc (presentation/wire-validation (validation/validate-contract-edn contract-class edn-text))
                      :contractClass (editing/normalize-contract-class contract-class))))

(defn ^:async handle-agent-list-contracts
  "List agent contract identifiers as EDN text."
  [do-text config contract-class]
  (try
    (let [ids (await (resources/list-resource-ids! config contract-class))]
      (do-text 200 (pr-str ids)))
    (catch :default err
      (do-text 500 (str ";; Failed to list contracts: " (ex-message err))))))

(defn ^:async handle-agent-get-contract-edn
  "Read agent contract EDN while preserving the plain-text error contract."
  [do-text config contract-class contract-id]
  (try
    (let [edn-text (await (io/read-text! (resources/resource-file-path config contract-class contract-id)))]
      (do-text 200 (str edn-text)))
    (catch :default err
      (if (= "ENOENT" (io/error-code err))
        (do-text 404 (str ";; Contract not found: " contract-id))
        (do-text 500 (str ";; Failed to read contract: " (ex-message err)))))))

(defn handle-agent-validate-contract-edn
  "Return agent contract validation as EDN data."
  [do-json contract-class edn-text]
  (do-json 200 (assoc (presentation/wire-validation (validation/validate-contract-edn contract-class edn-text))
                      :contractClass (editing/normalize-contract-class contract-class))))

(defn- ^:async persist-agent-contract-edn!
  [do-text config route-id edn-text ctx admit! parsed warnings]
  (try
    (let [admission
          (await
           (writes/write-resource-and-admit!
            config (resources/resource-file-path config "agents" route-id)
            edn-text
            (when admit!
              (fn []
                (writes/admit-saved-publication-resource!
                 config ctx "agents" parsed admit!)))))]
      (do-text 200 (pr-str (cond-> {:ok true
                                    :contractClass "agents"
                                    :contract/id route-id
                                    :contract parsed
                                    :warnings warnings}
                             admission (assoc :admission admission)))))
    (catch :default err
      (do-text (or (:status (ex-data err)) 500)
               (str ";; Failed to save and admit contract: "
                    (ex-message err))))))

(defn ^:async handle-agent-put-contract-edn
  "Compatibility PUT handler retained for agent clients during resource migration."
  ([do-text config contract-class contract-id edn-text]
   (handle-agent-put-contract-edn do-text config contract-class contract-id edn-text nil nil))
  ([do-text config contract-class contract-id edn-text ctx admit!]
   (let [klass (editing/normalize-contract-class contract-class)
         route-id (str contract-id)]
     (if-not (= "agents" klass)
       (do-text 400 (pr-str {:ok false
                             :error "compatibility_contract_class_not_writable"
                             :contractClass klass}))
       (let [validation (validation/validate-contract-edn klass edn-text)
             parsed (:contract validation)
             parsed-id (editing/parsed-resource-id klass parsed)]
         (cond
           (not (:ok validation))
           (do-text 422 (pr-str {:ok false
                                 :errors (:errors validation)
                                 :warnings (:warnings validation)}))

           (and parsed-id (not= route-id parsed-id))
           (do-text 400 (pr-str {:ok false
                                 :error "contract_id_mismatch"
                                 :routeContractId route-id
                                 :ednContractId parsed-id}))

           :else
           (await (persist-agent-contract-edn!
                   do-text config route-id edn-text ctx admit! parsed
                   (:warnings validation)))))))))

(defn handle-ui-actions
  "Return resolved UI actions for one actor and surface."
  [do-json config actor-id surface]
  (let [resolved (contracts-resolve/ui-actions-for-actor config actor-id surface)]
    (do-json 200 {:actor_id (:actor-id resolved)
                  :surface (:surface resolved)
                  :default_agent_id (:default-agent-id resolved)
                  :actions (:actions resolved)})))
