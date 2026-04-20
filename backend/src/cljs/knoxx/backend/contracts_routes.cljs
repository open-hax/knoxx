(ns knoxx.backend.contracts-routes
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.runtime.contract-loader :as loader]
            [knoxx.backend.runtime.contract-validator :as validator]
            [knoxx.backend.util.time :refer [now-iso]]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

(def ^:private contracts-index-key "contracts:index")

(defn- contract-id-from-filename
  [filename]
  (when (and (string? filename) (str/ends-with? filename ".edn"))
    (subs filename 0 (- (count filename) 4))))

(defn- validate-contract-edn
  [edn-text]
  (let [trimmed (str/trim (str edn-text))]
    (if (str/blank? trimmed)
      {:ok false
       :contract nil
       :errors [{:path [] :message "EDN text is empty"}]
       :warnings []}
      (try
        (let [contract (reader/read-string trimmed)
              base (validator/validate contract)]
          {:ok (:ok base)
           :contract contract
           :errors (:errors base)
           :warnings []})
        (catch :default err
          {:ok false
           :contract nil
           :errors [{:path [] :message (str "EDN parse error: " (.-message err))}]
           :warnings []})))))

(defn- safe-contract-id
  "Validate a contract id segment early (before we call loader/contract-file-path).

   Returns {:ok true :id <string>} or {:ok false :error <string>}." 
  [raw-id]
  (try
    {:ok true
     :id (loader/safe-path-segment! raw-id "contract-id")}
    (catch :default err
      {:ok false
       :error (or (.-message err) (str err))})))

(defn- contract-metadata!
  "Return Promise of ContractListItem-like map for a contract id." 
  [config contract-id]
  (let [file-path (loader/contract-file-path config contract-id)]
    (-> (.stat fs file-path)
        (.then (fn [stats]
                 (-> (.readFile fs file-path "utf8")
                     (.then (fn [edn-text]
                              {:id contract-id
                               :kind "agent"
                               :version 1
                               :enabled true
                               :ednHash (hash (str edn-text))
                               :compiledAt nil
                               :updatedAt (or (some-> stats .-mtime .toISOString) (now-iso))}))))))))

(defn sync-contract-index!
  "Sync contracts/agents/*.edn → Redis contracts:index set.

   Redis is a cache + fast index; disk is canonical." 
  [config]
  (if-let [client (redis/get-client)]
    (-> (loader/list-agent-contract-ids! config)
        (.then (fn [ids]
                 (-> (redis/smembers client contracts-index-key)
                     (.then (fn [existing]
                              (let [existing-set (set (map str (js/Array.from (or existing #js []))))
                                    desired-set (set (map str ids))
                                    to-add (vec (sort (set/difference desired-set existing-set)))
                                    to-remove (vec (sort (set/difference existing-set desired-set)))
                                    ops (concat
                                         (for [id to-add]
                                           (redis/sadd client contracts-index-key id))
                                         (for [id to-remove]
                                           (redis/srem client contracts-index-key id)))]
                                (-> (js/Promise.all (clj->js ops))
                                    (.then (fn [_]
                                             (println "[contracts] synced Redis index; add=" (count to-add) "remove=" (count to-remove))
                                             {:ok true
                                              :added to-add
                                              :removed to-remove
                                              :count (count ids)})))))))))
        (.catch (fn [err]
                 (println "[contracts] sync-contract-index! failed:" (.-message err))
                 {:ok false :error (.-message err)})))
    (js/Promise.resolve {:ok false :error "Redis not connected"})))

;; ===========================================================================
;; Admin route handlers (JSON API)
;; ===========================================================================

(defn- handle-list-contracts
  [do-json config]
  (-> (loader/list-agent-contract-ids! config)
      (.then (fn [ids]
               (-> (.all js/Promise (clj->js (map (partial contract-metadata! config) ids)))
                   (.then (fn [items]
                            (do-json 200 {:contracts (vec (js->clj items :keywordize-keys true))}))))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Failed to list contracts: " (.-message err))})))) )

(defn- handle-get-contract
  [do-json config contract-id]
  (-> (.readFile fs (loader/contract-file-path config contract-id) "utf8")
      (.then (fn [edn-text]
               (let [validation (validate-contract-edn (or edn-text ""))]
                 (do-json 200 {:ednText (or edn-text "")
                               :contract (:contract validation)
                               :validation (dissoc validation :contract)}))))
      (.catch (fn [err]
                (if (= "ENOENT" (.-code err))
                  (do-json 404 {:detail (str "Contract not found: " contract-id)})
                  (do-json 500 {:detail (str "Failed to read contract: " (.-message err))}))))))

(defn- handle-save-contract
  [do-json config contract-id edn-text]
  (let [validation (validate-contract-edn edn-text)
        validation-out (dissoc validation :contract)
        parsed (:contract validation)
        parsed-id (some-> (:contract/id parsed) str)
        route-id (str contract-id)]
    (cond
      (not (:ok validation))
      (do-json 400 {:ok false
                    :detail "Contract EDN failed validation"
                    :validation validation-out})

      (not= route-id parsed-id)
      (do-json 400 {:ok false
                    :detail "Refusing to save contract: :contract/id does not match route contractId"
                    :routeContractId route-id
                    :ednContractId parsed-id
                    :validation validation-out})

      :else
      (let [file-path (loader/contract-file-path config route-id)]
        (-> (loader/write-edn-file! file-path edn-text)
            (.then (fn [_]
                     (sync-contract-index! config)))
            (.then (fn [_]
                     (do-json 200 {:ok true
                                   :ednText edn-text
                                   :contract parsed
                                   :validation validation-out})))
            (.catch (fn [err]
                      (do-json 500 {:detail (str "Failed to save contract: " (.-message err))}))))))))

(defn- handle-copy-contract
  [do-json config source-id new-id]
  (-> (.readFile fs (loader/contract-file-path config source-id) "utf8")
      (.then (fn [source-edn]
               (let [text (or source-edn "")
                     cloned (if (str/includes? text ":contract/id")
                              (str/replace text #":contract/id\s+\"[^\"]+\"" (str ":contract/id \"" new-id "\""))
                              (str ":contract/id \"" new-id "\"\n" text))]
                 (handle-save-contract do-json config new-id cloned))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Failed to copy contract: " (.-message err))})))) )

(defn- handle-validate-contract
  [do-json edn-text]
  (do-json 200 (validate-contract-edn edn-text)))

;; ===========================================================================
;; Agent route handlers (EDN-native API)
;; ===========================================================================

(defn- handle-agent-list-contracts
  "Return a vector of contract IDs as EDN text." 
  [do-text config]
  (-> (loader/list-agent-contract-ids! config)
      (.then (fn [ids]
               (do-text 200 (pr-str ids))))
      (.catch (fn [err]
                (do-text 500 (str ";; Failed to list contracts: " (.-message err)))))))

(defn- handle-agent-get-contract-edn
  "Return raw EDN text for a contract." 
  [do-text config contract-id]
  (-> (.readFile fs (loader/contract-file-path config contract-id) "utf8")
      (.then (fn [edn-text]
               (do-text 200 (str edn-text))))
      (.catch (fn [err]
                (if (= "ENOENT" (.-code err))
                  (do-text 404 (str ";; Contract not found: " contract-id))
                  (do-text 500 (str ";; Failed to read contract: " (.-message err))))))))

(defn- handle-agent-put-contract-edn
  "Accept raw EDN text and store it. Validates before saving.
   Returns validation result as EDN." 
  [do-text config contract-id edn-text]
  (let [validation (validate-contract-edn edn-text)]
    (if-not (:ok validation)
      (do-text 422 (pr-str {:ok false
                            :errors (:errors validation)}))
      (let [parsed (:contract validation)
            parsed-id (some-> (:contract/id parsed) str)
            route-id (str contract-id)]
        (if (not= route-id parsed-id)
          (do-text 400 (pr-str {:ok false
                                :error "contract_id_mismatch"
                                :routeContractId route-id
                                :ednContractId parsed-id}))
          (-> (loader/write-edn-file! (loader/contract-file-path config route-id) edn-text)
              (.then (fn [_]
                       (sync-contract-index! config)))
              (.then (fn [_]
                       (do-text 200 (pr-str {:ok true
                                             :contract/id route-id
                                             :contract parsed}))))
              (.catch (fn [err]
                       (do-text 500 (str ";; Failed to save contract: " (.-message err)))))))))))

;; ===========================================================================
;; Route registration
;; ===========================================================================

(defn register-contracts-routes!
  [app _runtime config helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)
        do-text (fn [reply status text]
                  (.end reply (.status reply status) text #js {"Content-Type" "text/plain; charset=utf-8"}))]

    ;; ── Agent API (EDN-native) — registered FIRST so they don't get
    ;;    swallowed by the admin :contractId parameterized routes ────────

    (do-route app "GET" "/api/agent/contracts"
              (fn [request reply]
                (do-ctx _runtime request reply
                  (fn [ctx]
                    (try
                      (when ctx (do-perm ctx "agent.chat.use"))
                      (handle-agent-list-contracts (partial do-text reply) config)
                      (catch :default err
                        (do-err reply err)))))))

    (do-route app "GET" "/api/agent/contracts/:contractId"
              (fn [request reply]
                (do-ctx _runtime request reply
                  (fn [ctx]
                    (try
                      (when ctx (do-perm ctx "agent.chat.use"))
                      (let [contract-id (str (or (aget request "params" "contractId") ""))]
                        (if (str/blank? contract-id)
                          (do-text reply 400 ";; contractId is required")
                          (let [safe (safe-contract-id contract-id)]
                            (if-not (:ok safe)
                              (do-text reply 400 (str ";; Invalid contractId: " (:error safe)))
                              (handle-agent-get-contract-edn (partial do-text reply) config (:id safe))))))
                      (catch :default err
                        (do-err reply err)))))))

    (do-route app "PUT" "/api/agent/contracts/:contractId"
              (fn [request reply]
                (do-ctx _runtime request reply
                  (fn [ctx]
                    (try
                      (when ctx (do-perm ctx "agent.chat.use"))
                      (let [contract-id (str (or (aget request "params" "contractId") ""))
                            edn-text (str (or (aget request "body") ""))]
                        (if (str/blank? contract-id)
                          (do-text reply 400 ";; contractId is required")
                          (let [safe (safe-contract-id contract-id)]
                            (if-not (:ok safe)
                              (do-text reply 400 (str ";; Invalid contractId: " (:error safe)))
                              (handle-agent-put-contract-edn (partial do-text reply) config (:id safe) edn-text)))))
                      (catch :default err
                        (do-err reply err)))))))

    ;; ── Admin API (JSON) ──────────────────────────────────────────────

    (do-route app "GET" "/api/admin/contracts"
              (fn [request reply]
                (do-ctx _runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (handle-list-contracts (partial do-json reply) config)
                      (catch :default err
                        (do-err reply err)))))))

    (do-route app "GET" "/api/admin/contracts/:contractId"
              (fn [request reply]
                (do-ctx _runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [contract-id (str (or (aget request "params" "contractId") ""))]
                        (if (str/blank? contract-id)
                          (do-json reply 400 {:detail "contractId is required"})
                          (let [safe (safe-contract-id contract-id)]
                            (if-not (:ok safe)
                              (do-json reply 400 {:detail "Invalid contractId" :error (:error safe)})
                              (handle-get-contract (partial do-json reply) config (:id safe))))))
                      (catch :default err
                        (do-err reply err)))))))

    (do-route app "PUT" "/api/admin/contracts/:contractId"
              (fn [request reply]
                (do-ctx _runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [contract-id (str (or (aget request "params" "contractId") ""))
                            body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                            edn-text (str (or (:ednText body) ""))]
                        (if (str/blank? contract-id)
                          (do-json reply 400 {:detail "contractId is required"})
                          (let [safe (safe-contract-id contract-id)]
                            (if-not (:ok safe)
                              (do-json reply 400 {:detail "Invalid contractId" :error (:error safe)})
                              (handle-save-contract (partial do-json reply) config (:id safe) edn-text)))))
                      (catch :default err
                        (do-err reply err)))))))

    (do-route app "POST" "/api/admin/contracts/validate"
              (fn [request reply]
                (do-ctx _runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                            edn-text (str (or (:ednText body) ""))]
                        (handle-validate-contract (partial do-json reply) edn-text))
                      (catch :default err
                        (do-err reply err)))))))

    (do-route app "POST" "/api/admin/contracts/:contractId/copy"
              (fn [request reply]
                (do-ctx _runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [source-id (str (or (aget request "params" "contractId") ""))
                            body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                            new-id (str (or (:newId body) ""))]
                        (if (or (str/blank? source-id) (str/blank? new-id))
                          (do-json reply 400 {:detail "source contractId and newId are required"})
                          (let [safe-source (safe-contract-id source-id)
                                safe-new (safe-contract-id new-id)]
                            (cond
                              (not (:ok safe-source))
                              (do-json reply 400 {:detail "Invalid source contractId" :error (:error safe-source)})

                              (not (:ok safe-new))
                              (do-json reply 400 {:detail "Invalid newId" :error (:error safe-new)})

                              :else
                              (handle-copy-contract (partial do-json reply) config (:id safe-source) (:id safe-new))))))
                      (catch :default err
                        (do-err reply err)))))))

    nil))
