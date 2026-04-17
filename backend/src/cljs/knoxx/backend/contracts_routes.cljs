(ns knoxx.backend.contracts-routes
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.redis-client :as redis]))

(def ^:private contracts-index-key "contracts:index")

(defn- contract-key
  [contract-id]
  (str "contract:edn:" contract-id))

(defn- now-iso []
  (.toISOString (js/Date.)))

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
              id (:contract/id contract)
              kind (:contract/kind contract)
              errors (cond-> []
                       (not (string? id))
                       (conj {:path ["contract/id"] :message ":contract/id must be a string"})
                       (not (keyword? kind))
                       (conj {:path ["contract/kind"] :message ":contract/kind must be a keyword"}))]
          {:ok (empty? errors)
           :contract contract
           :errors errors
           :warnings []})
        (catch :default err
          {:ok false
           :contract nil
           :errors [{:path [] :message (str "EDN parse error: " (.-message err))}]
           :warnings []})))))

(defn- compile-contract->sql
  [contract]
  (let [id (:contract/id contract)
        version (or (:contract/version contract) 1)
        enabled (boolean (get contract :enabled true))
        uses (vec (or (:contract/uses contract) []))
        events (:events contract)
        always (vec (or (:always events) []))
        maybe (vec (or (:maybe events) []))
        tools (or (get-in contract [:data :tools])
                  (get-in contract [:data/tools])
                  [])]
    {:contract {:id id
                :version version
                :kind (name (:contract/kind contract))
                :enabled enabled
                :edn_text (pr-str contract)
                :edn_hash (hash (pr-str contract))
                :compiled_at (now-iso)}
     :event-kinds (vec (concat
                        (map (fn [k] {:contract_id id :event_kind (name k) :mode "always"}) always)
                        (map (fn [k] {:contract_id id :event_kind (name k) :mode "maybe"}) maybe)))
     :bindings (vec (map-indexed (fn [i dep]
                                  {:contract_id id
                                   :dep_id (if (keyword? dep) (name dep) (str dep))
                                   :load_order i})
                                uses))
     :tools (vec (map (fn [t]
                        {:contract_id id
                         :tool_name (cond
                                      (keyword? t) (name t)
                                      (string? t) t
                                      (map? t) (or (:toolId t) (:tool/id t) (str t))
                                      :else (str t))})
                      tools))}))

;; ---------------------------------------------------------------------------
;; Route handler helpers (extracted for paren balance + closure capture safety)
;; ---------------------------------------------------------------------------

(defn- handle-list-contracts
  [do-json client]
  (-> (redis/smembers client contracts-index-key)
      (.then
       (fn [ids]
         (let [ids (sort (map str (js/Array.from (or ids #js []))))
               items (map (fn [id]
                            {:id id
                             :kind "agent"
                             :version 1
                             :enabled true
                             :ednHash 0
                             :compiledAt nil
                             :updatedAt (now-iso)})
                          ids)]
           (do-json 200 {:contracts items}))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Redis error: " (.-message err))})))))

(defn- handle-get-contract
  [do-json client contract-id]
  (-> (redis/get-json client (contract-key contract-id))
      (.then (fn [edn-text]
               (let [edn-text (or edn-text "")
                     validation (validate-contract-edn edn-text)]
                 (do-json 200 {:ednText edn-text
                               :contract (:contract validation)
                               :validation (dissoc validation :contract)}))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Redis error: " (.-message err))})))))

(defn- handle-save-contract
  [do-json client contract-id edn-text validation]
  (-> (redis/set-json client (contract-key contract-id) edn-text)
      (.then (fn [_]
               (redis/sadd client contracts-index-key contract-id)))
      (.then (fn [_]
               (do-json 200 {:ok true
                             :ednText edn-text
                             :contract (:contract validation)
                             :validation (dissoc validation :contract)})))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Redis error: " (.-message err))})))))

(defn- handle-compile-contract
  [do-json client contract-id]
  (-> (redis/get-json client (contract-key contract-id))
      (.then (fn [edn-text]
               (let [validation (validate-contract-edn (or edn-text ""))]
                 (if-not (:ok validation)
                   (do-json 200 {:ok false
                                 :errors (:errors validation)
                                 :contract nil
                                 :sql nil})
                   (let [contract (:contract validation)]
                     (do-json 200 {:ok true
                                   :contract contract
                                   :sql (compile-contract->sql contract)}))))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Redis error: " (.-message err))})))))

(defn- handle-copy-contract
  [do-json client source-id new-id]
  (-> (redis/get-json client (contract-key source-id))
      (.then (fn [source-edn]
               (let [text (or source-edn "")
                     cloned (if (str/includes? text ":contract/id")
                              (str/replace text #":contract/id\s+\"[^\"]+\"" (str ":contract/id \"" new-id "\""))
                              (str ":contract/id \"" new-id "\"\n" text))
                     validation (validate-contract-edn cloned)]
                 (-> (redis/set-json client (contract-key new-id) cloned)
                     (.then (fn [_]
                              (redis/sadd client contracts-index-key new-id)))
                     (.then (fn [_]
                              (do-json 200 {:ok true
                                            :ednText cloned
                                            :contract (:contract validation)
                                            :validation (dissoc validation :contract)}))))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Redis error: " (.-message err))}))))))

;; ---------------------------------------------------------------------------
;; Route registration
;; ---------------------------------------------------------------------------

(defn register-contracts-routes!
  [app runtime helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)]

    ;; List
    (do-route app "GET" "/api/admin/contracts"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (if-let [client (redis/get-client)]
                        (handle-list-contracts (partial do-json reply) client)
                        (do-json reply 503 {:detail "Redis not connected"}))
                      (catch :default err
                        (do-err reply err)))))))

    ;; Get
    (do-route app "GET" "/api/admin/contracts/:contractId"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [contract-id (str (or (aget request "params" "contractId") ""))]
                        (if (str/blank? contract-id)
                          (do-json reply 400 {:detail "contractId is required"})
                          (if-let [client (redis/get-client)]
                            (handle-get-contract (partial do-json reply) client contract-id)
                            (do-json reply 503 {:detail "Redis not connected"}))))
                      (catch :default err
                        (do-err reply err)))))))

    ;; Save
    (do-route app "PUT" "/api/admin/contracts/:contractId"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [contract-id (str (or (aget request "params" "contractId") ""))
                            body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                            edn-text (str (or (:ednText body) ""))
                            validation (validate-contract-edn edn-text)
                            client (redis/get-client)]
                        (cond
                          (str/blank? contract-id)
                          (do-json reply 400 {:detail "contractId is required"})

                          (not client)
                          (do-json reply 503 {:detail "Redis not connected"})

                          :else
                          (handle-save-contract (partial do-json reply) client contract-id edn-text validation)))
                      (catch :default err
                        (do-err reply err)))))))

    ;; Validate
    (do-route app "POST" "/api/admin/contracts/validate"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                            edn-text (str (or (:ednText body) ""))
                            validation (validate-contract-edn edn-text)]
                        (do-json reply 200 validation))
                      (catch :default err
                        (do-err reply err)))))))

    ;; Compile
    (do-route app "POST" "/api/admin/contracts/:contractId/compile"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [contract-id (str (or (aget request "params" "contractId") ""))]
                        (if (str/blank? contract-id)
                          (do-json reply 400 {:detail "contractId is required"})
                          (if-let [client (redis/get-client)]
                            (handle-compile-contract (partial do-json reply) client contract-id)
                            (do-json reply 503 {:detail "Redis not connected"}))))
                      (catch :default err
                        (do-err reply err)))))))

    ;; Copy
    (do-route app "POST" "/api/admin/contracts/:contractId/copy"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [source-id (str (or (aget request "params" "contractId") ""))
                            body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                            new-id (str (or (:newId body) ""))]
                        (if (or (str/blank? source-id) (str/blank? new-id))
                          (do-json reply 400 {:detail "source contractId and newId are required"})
                          (if-let [client (redis/get-client)]
                            (handle-copy-contract (partial do-json reply) client source-id new-id)
                            (do-json reply 503 {:detail "Redis not connected"}))))
                      (catch :default err
                        (do-err reply err)))))))

    nil))
