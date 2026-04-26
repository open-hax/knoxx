(ns knoxx.backend.contracts-routes
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.event-agents :as event-agents]
            [knoxx.backend.redis-client :as redis]
            [knoxx.backend.runtime.actor-scope :as actor-scope]
            [knoxx.backend.runtime.contract-loader :as loader]
            [knoxx.backend.runtime.contract-validator :as validator]
            [knoxx.backend.util.time :refer [now-iso]]
            ["node:fs" :as node-fs]
            ["node:fs/promises" :as fs]))

(def ^:private contracts-index-key "contracts:index")
(def ^:private contract-watch-debounce-ms 350)

(defonce ^:private contract-watchers* (atom []))
(defonce ^:private contract-watch-timer* (atom nil))
(defonce ^:private contract-watch-running?* (atom false))

(defn- keyword->slug
  [value]
  (let [base (last (str/split (cond
                                (keyword? value) (str value)
                                :else (str (or value ""))) #"/"))]
    (some-> base
            (str/replace #"^-+" "")
            (str/replace #"-" "_")
            str/trim
            not-empty)))

(defn- normalize-contract-class
  [raw]
  (loader/normalize-contract-class raw))

(defn- contract-id->index-key
  [contract-class contract-id]
  (str (normalize-contract-class contract-class) "/" contract-id))

(defn- model-id->slug
  [model-id]
  (some-> model-id
          str
          (str/replace #"[^A-Za-z0-9._-]+" "_")
          (str/replace #"_+" "_")))

(defn- parsed-record-id
  [contract-class value]
  (case (normalize-contract-class contract-class)
    "agents" (some-> (:contract/id value) str)
    "policies" (some-> (:contract/id value) str)
    "actors" (some-> (:actor/id value) str)
    "roles" (some-> (:role/id value) keyword->slug)
    "capabilities" (some-> (:cap/id value) keyword->slug (as-> slug (str "cap_" slug)))
    "model_families" (some-> (:model-family/id value) str)
    "models" (some-> (:model/id value) model-id->slug)
    nil))

(defn- parsed-kind-label
  [contract-class value]
  (case (normalize-contract-class contract-class)
    "agents" (some-> (:contract/kind value) name)
    "policies" "policy"
    "actors" (some-> (:actor/kind value) name)
    "roles" "role"
    "capabilities" "capability"
    "model_families" "model-family"
    "models" "model"
    "unknown"))

(defn- parsed-version
  [contract-class value]
  (case (normalize-contract-class contract-class)
    "agents" (or (:contract/version value) 1)
    "policies" (or (:contract/version value) 1)
    1))

(defn- parsed-enabled?
  [contract-class value]
  (case (normalize-contract-class contract-class)
    "agents" (not (false? (:enabled value)))
    "policies" (not (false? (:enabled value)))
    true))

(defn- validate-contract-edn
  [contract-class edn-text]
  (let [trimmed (str/trim (str edn-text))]
    (if (str/blank? trimmed)
      {:ok false
       :contract nil
       :errors [{:path [] :message "EDN text is empty"}]
       :warnings []}
      (try
        (let [raw-contract (reader/read-string trimmed)
              contract (if (= (normalize-contract-class contract-class) "agents")
                         (actor-scope/normalize-agent-contract raw-contract)
                         raw-contract)
              base (validator/validate contract-class contract)]
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
  [raw-id]
  (try
    {:ok true
     :id (loader/safe-path-segment! raw-id "contract-id")}
    (catch :default err
      {:ok false
       :error (or (.-message err) (str err))})))

(defn- safe-contract-class
  [raw-class]
  (try
    {:ok true
     :class (normalize-contract-class raw-class)}
    (catch :default err
      {:ok false
       :error (or (.-message err) (str err))})))

(defn- update-id-in-edn-text
  [contract-class edn-text new-id]
  (case (normalize-contract-class contract-class)
    "agents"
    (if (str/includes? edn-text ":contract/id")
      (str/replace edn-text #":contract/id\s+\"[^\"]+\"" (str ":contract/id \"" new-id "\""))
      (str ":contract/id \"" new-id "\"\n" edn-text))

    "policies"
    (if (str/includes? edn-text ":contract/id")
      (str/replace edn-text #":contract/id\s+\"[^\"]+\"" (str ":contract/id \"" new-id "\""))
      (str ":contract/id \"" new-id "\"\n" edn-text))

    "actors"
    (if (str/includes? edn-text ":actor/id")
      (str/replace edn-text #":actor/id\s+\"[^\"]+\"" (str ":actor/id \"" new-id "\""))
      (str ":actor/id \"" new-id "\"\n" edn-text))

    "roles"
    (let [keyword-id (str ":role/" (str/replace new-id #"_" "-"))]
      (if (str/includes? edn-text ":role/id")
        (str/replace edn-text #":role/id\s+:[^\s\]}]+" (str ":role/id " keyword-id))
        (str ":role/id " keyword-id "\n" edn-text)))

    "capabilities"
    (let [slug (str/replace (str new-id) #"^cap_" "")
          keyword-id (str ":cap/" (str/replace slug #"_" "-"))]
      (if (str/includes? edn-text ":cap/id")
        (str/replace edn-text #":cap/id\s+:[^\s\]}]+" (str ":cap/id " keyword-id))
        (str ":cap/id " keyword-id "\n" edn-text)))

    "model_families"
    (if (str/includes? edn-text ":model-family/id")
      (str/replace edn-text #":model-family/id\s+\"[^\"]+\"" (str ":model-family/id \"" new-id "\""))
      (str ":model-family/id \"" new-id "\"\n" edn-text))

    "models"
    (let [model-id (str/replace (str new-id) #"_" ":")]
      (if (str/includes? edn-text ":model/id")
        (str/replace edn-text #":model/id\s+\"[^\"]+\"" (str ":model/id \"" model-id "\""))
        (str ":model/id \"" model-id "\"\n" edn-text)))

    edn-text))

(defn- contract-metadata!
  [config contract-class contract-id]
  (let [file-path (loader/contract-file-path config contract-class contract-id)]
    (-> (.stat fs file-path)
        (.then (fn [stats]
                 (-> (.readFile fs file-path "utf8")
                     (.then (fn [edn-text]
                              (let [parsed (try
                                             (reader/read-string (str edn-text))
                                             (catch :default _ nil))]
                                {:id contract-id
                                 :contractClass (normalize-contract-class contract-class)
                                 :kind (parsed-kind-label contract-class parsed)
                                 :version (parsed-version contract-class parsed)
                                 :enabled (parsed-enabled? contract-class parsed)
                                 :title contract-id
                                 :path (str (normalize-contract-class contract-class) "/" contract-id ".edn")
                                 :ednHash (hash (str edn-text))
                                 :compiledAt nil
                                 :updatedAt (or (some-> stats .-mtime .toISOString) (now-iso))})))))))))

(defn sync-contract-index!
  "Sync contracts/*.edn → Redis contracts:index set.

   Redis is a cache + fast index; disk is canonical."
  [config]
  (if-let [client (redis/get-client)]
    (-> (.all js/Promise
              (clj->js (map (fn [klass]
                              (-> (loader/list-contract-ids! config klass)
                                  (.then (fn [ids]
                                           (mapv (fn [id] (contract-id->index-key klass id)) ids)))))
                            loader/contract-class-order)))
        (.then (fn [nested]
                 (let [ids (vec (mapcat identity (js->clj nested))) ]
                   (-> (redis/smembers client contracts-index-key)
                       (.then (fn [existing]
                                (let [existing-set (set (map str (js/Array.from (or existing #js []))))
                                      desired-set (set ids)
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
                                                :count (count ids)}))))))))))
        (.catch (fn [err]
                  (println "[contracts] sync-contract-index! failed:" (.-message err))
                  {:ok false :error (.-message err)})))
    (js/Promise.resolve {:ok false :error "Redis not connected"})))

(defn- clear-contract-watch-timer!
  []
  (when-let [timer @contract-watch-timer*]
    (js/clearTimeout timer)
    (reset! contract-watch-timer* nil)))

(defn stop-contract-watcher!
  []
  (clear-contract-watch-timer!)
  (doseq [watcher @contract-watchers*]
    (when watcher
      (try
        (.close watcher)
        (catch :default _ nil))))
  (reset! contract-watchers* [])
  (reset! contract-watch-running?* false)
  nil)

(defn- watchable-contract-change?
  [filename]
  (or (nil? filename)
      (str/ends-with? (str/lower-case (str filename)) ".edn")))

(defn- schedule-contract-refresh!
  [config reason]
  (clear-contract-watch-timer!)
  (reset! contract-watch-timer*
          (js/setTimeout
           (fn []
             (reset! contract-watch-timer* nil)
             (println "[contracts] watcher refresh triggered by" reason)
             (-> (sync-contract-index! config)
                 (.catch (fn [err]
                           (println "[contracts] watcher sync failed:" (.-message err))
                           nil))
                 (.then (fn [_]
                          (event-agents/reload!)
                          (println "[contracts] event-agent runtime reloaded after contract change")
                          nil))
                 (.catch (fn [err]
                           (println "[contracts] watcher reload failed:" (.-message err))
                           nil))))
           contract-watch-debounce-ms)))

(defn start-contract-watcher!
  [config]
  (when-not @contract-watch-running?*
    (let [roots (->> (loader/contract-root-paths config)
                     (filter #(.existsSync node-fs %))
                     distinct
                     vec)
          watch-root (fn [root]
                       (try
                         (.watch node-fs
                                 root
                                 #js {:recursive true}
                                 (fn [event-type filename]
                                   (let [filename-str (some-> filename str)]
                                     (when (watchable-contract-change? filename-str)
                                       (schedule-contract-refresh! config
                                                                  (str root " :: " event-type " :: " (or filename-str "<unknown>")))))))
                         (catch :default err
                           (println "[contracts] failed to watch" root ":" (.-message err))
                           nil)))
          watchers (->> roots
                        (map watch-root)
                        (remove nil?)
                        vec)]
      (when (seq watchers)
        (reset! contract-watchers* watchers)
        (reset! contract-watch-running?* true)
        (println "[contracts] watching" (count watchers) "contract roots for live reload")))))

(defn- handle-list-contracts
  [do-json config contract-class]
  (if-not contract-class
    ;; No class filter: use flat recursive scan for ALL contracts
    (-> (loader/list-all-contracts-flat! config)
        (.then (fn [contracts]
                 (let [with-metadata (atom [])]
                   (doseq [c contracts]
                     (let [id (:id c)
                           folder (:folder c)
                           klass (some (fn [prefix]
                                       (when (.startsWith folder (str prefix "/"))
                                         prefix))
                                     loader/contract-class-order)
                           ;; Extract contract details from file
                           edn-text (try
                                     (.readFileSync node-fs (:path c) "utf8")
                                     (catch :default ""))
                           parsed (try
                                  (reader/read-string edn-text)
                                  (catch :default nil))]
                       (swap! with-metadata conj
                             {:id id
                              :contractClass (or klass "agents")
                              :kind (parsed-kind-label (or klass "agents") parsed)
                              :version (parsed-version (or klass "agents") parsed)
                              :enabled (parsed-enabled? (or klass "agents") parsed)
                              :title id
                              :path folder
                              :folder folder
                              :ednHash (hash edn-text)
                              :compiledAt nil
                              :updatedAt (now-iso)})))
                   (do-json 200 {:contracts (sort-by :id @with-metadata)}))))
        (.catch (fn [err]
                  (do-json 500 {:detail (str "Failed to list contracts: " (.-message err))})))
    ;; Class filter specified: use original class-based scan
    (let [classes (if contract-class
                   [(normalize-contract-class contract-class)]
                   loader/contract-class-order)]
      (-> (.all js/Promise
                (clj->js
                 (map (fn [klass]
                        (-> (loader/list-contract-ids! config klass)
                            (.then (fn [ids]
                                     (.all js/Promise (clj->js (map (partial contract-metadata! config klass) ids)))))))
                      classes)))
          (.then (fn [nested]
                   (let [contracts (->> (js->clj nested :keywordize-keys true)
                                        (mapcat identity)
                                        (sort-by (juxt :contractClass :id))
                                        vec)]
                     (do-json 200 {:contracts contracts}))))
          (.catch (fn [err]
                    (do-json 500 {:detail (str "Failed to list contracts: " (.-message err))}))))))

(defn- handle-get-contract
  [do-json config contract-class contract-id]
  (-> (.readFile fs (loader/contract-file-path config contract-class contract-id) "utf8")
      (.then (fn [edn-text]
               (let [validation (validate-contract-edn contract-class (or edn-text ""))]
                 (do-json 200 {:contractClass (normalize-contract-class contract-class)
                               :ednText (or edn-text "")
                               :contract (:contract validation)
                               :validation (dissoc validation :contract)}))))
      (.catch (fn [err]
                (if (= "ENOENT" (.-code err))
                  (do-json 404 {:detail (str "Contract not found: " contract-id)})
                  (do-json 500 {:detail (str "Failed to read contract: " (.-message err))}))))))

(defn- handle-save-contract
  [do-json config contract-class contract-id edn-text]
  (let [klass (normalize-contract-class contract-class)
        validation (validate-contract-edn klass edn-text)
        validation-out (dissoc validation :contract)
        parsed (:contract validation)
        parsed-id (parsed-record-id klass parsed)
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
      (let [file-path (loader/contract-file-path config klass route-id)]
        (-> (loader/write-edn-file! file-path edn-text)
            (.then (fn [_]
                     (sync-contract-index! config)))
            (.then (fn [_]
                     (do-json 200 {:ok true
                                   :contractClass klass
                                   :ednText edn-text
                                   :contract parsed
                                   :validation validation-out})))
            (.catch (fn [err]
                      (do-json 500 {:detail (str "Failed to save contract: " (.-message err))}))))))))

(defn- handle-copy-contract
  [do-json config contract-class source-id new-id]
  (-> (.readFile fs (loader/contract-file-path config contract-class source-id) "utf8")
      (.then (fn [source-edn]
               (let [text (or source-edn "")
                     cloned (update-id-in-edn-text contract-class text new-id)]
                 (handle-save-contract do-json config contract-class new-id cloned))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Failed to copy contract: " (.-message err))})))) )

(defn- handle-validate-contract
  [do-json contract-class edn-text]
  (do-json 200 (assoc (validate-contract-edn contract-class edn-text)
                      :contractClass (normalize-contract-class contract-class))))

(defn- handle-agent-list-contracts
  [do-text config]
  (-> (loader/list-agent-contract-ids! config)
      (.then (fn [ids]
               (do-text 200 (pr-str ids))))
      (.catch (fn [err]
                (do-text 500 (str ";; Failed to list contracts: " (.-message err)))))))

(defn- handle-agent-get-contract-edn
  [do-text config contract-id]
  (-> (.readFile fs (loader/contract-file-path config "agents" contract-id) "utf8")
      (.then (fn [edn-text]
               (do-text 200 (str edn-text))))
      (.catch (fn [err]
                (if (= "ENOENT" (.-code err))
                  (do-text 404 (str ";; Contract not found: " contract-id))
                  (do-text 500 (str ";; Failed to read contract: " (.-message err))))))))

(defn- handle-agent-put-contract-edn
  [do-text config contract-id edn-text]
  (let [validation (validate-contract-edn "agents" edn-text)]
    (if-not (:ok validation)
      (do-text 422 (pr-str {:ok false
                            :errors (:errors validation)}))
      (let [parsed (:contract validation)
            parsed-id (parsed-record-id "agents" parsed)
            route-id (str contract-id)]
        (if (not= route-id parsed-id)
          (do-text 400 (pr-str {:ok false
                                :error "contract_id_mismatch"
                                :routeContractId route-id
                                :ednContractId parsed-id}))
          (-> (loader/write-edn-file! (loader/contract-file-path config "agents" route-id) edn-text)
              (.then (fn [_]
                       (sync-contract-index! config)))
              (.then (fn [_]
                       (do-text 200 (pr-str {:ok true
                                             :contract/id route-id
                                             :contract parsed}))))
              (.catch (fn [err]
                        (do-text 500 (str ";; Failed to save contract: " (.-message err)))))))))))

(defn- register-agent-contract-routes!
  [app runtime config helpers]
  (let [do-route (:route! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)
        do-text (fn [reply status text]
                  (.end reply (.status reply status) text #js {"Content-Type" "text/plain; charset=utf-8"}))]
    (do-route app "GET" "/api/agent/contracts"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (when ctx (do-perm ctx "agent.chat.use"))
                      (handle-agent-list-contracts (partial do-text reply) config)
                      (catch :default err
                        (do-err reply err)))))))
    (do-route app "GET" "/api/agent/contracts/:contractId"
              (fn [request reply]
                (do-ctx runtime request reply
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
                (do-ctx runtime request reply
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
                        (do-err reply err)))))))))

(defn- register-admin-contract-routes!
  [app runtime config helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)]
    (do-route app "GET" "/api/admin/contracts"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (when ctx (do-perm ctx "agent.chat.use"))
                      (let [kind (or (aget request "query" "kind")
                                     (aget request "query" "class"))
                            safe-kind (if kind (safe-contract-class kind) {:ok true :class nil})]
                        (if-not (:ok safe-kind)
                          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
                          (handle-list-contracts (partial do-json reply) config (:class safe-kind))))
                      (catch :default err
                        (do-err reply err)))))))
    (do-route app "GET" "/api/admin/contracts/:contractId"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (when ctx (do-perm ctx "agent.chat.use"))
                      (let [contract-id (str (or (aget request "params" "contractId") ""))
                            kind (or (aget request "query" "kind")
                                     (aget request "query" "class")
                                     "agents")]
                        (if (str/blank? contract-id)
                          (do-json reply 400 {:detail "contractId is required"})
                          (let [safe (safe-contract-id contract-id)
                                safe-kind (safe-contract-class kind)]
                            (cond
                              (not (:ok safe-kind))
                              (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})

                              (not (:ok safe))
                              (do-json reply 400 {:detail "Invalid contractId" :error (:error safe)})

                              :else
                              (handle-get-contract (partial do-json reply) config (:class safe-kind) (:id safe))))))
                      (catch :default err
                        (do-err reply err)))))))
    (do-route app "PUT" "/api/admin/contracts/:contractId"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [contract-id (str (or (aget request "params" "contractId") ""))
                            body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                            kind (or (:kind body) (:class body) (aget request "query" "kind") "agents")
                            edn-text (str (or (:ednText body) ""))]
                        (if (str/blank? contract-id)
                          (do-json reply 400 {:detail "contractId is required"})
                          (let [safe (safe-contract-id contract-id)
                                safe-kind (safe-contract-class kind)]
                            (cond
                              (not (:ok safe-kind))
                              (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})

                              (not (:ok safe))
                              (do-json reply 400 {:detail "Invalid contractId" :error (:error safe)})

                              :else
                              (handle-save-contract (partial do-json reply) config (:class safe-kind) (:id safe) edn-text)))))
                      (catch :default err
                        (do-err reply err)))))))
    (do-route app "POST" "/api/admin/contracts/validate"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                            kind (or (:kind body) (:class body) "agents")
                            safe-kind (safe-contract-class kind)
                            edn-text (str (or (:ednText body) ""))]
                        (if-not (:ok safe-kind)
                          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
                          (handle-validate-contract (partial do-json reply) (:class safe-kind) edn-text)))
                      (catch :default err
                        (do-err reply err)))))))
    (do-route app "POST" "/api/admin/contracts/:contractId/copy"
              (fn [request reply]
                (do-ctx runtime request reply
                  (fn [ctx]
                    (try
                      (do-perm ctx "platform.org.create")
                      (let [source-id (str (or (aget request "params" "contractId") ""))
                            body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                            new-id (str (or (:newId body) ""))
                            kind (or (:kind body) (:class body) "agents")
                            safe-kind (safe-contract-class kind)]
                        (cond
                          (not (:ok safe-kind))
                          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})

                          (or (str/blank? source-id) (str/blank? new-id))
                          (do-json reply 400 {:detail "source contractId and newId are required"})

                          :else
                          (let [safe-source (safe-contract-id source-id)
                                safe-new (safe-contract-id new-id)]
                            (cond
                              (not (:ok safe-source))
                              (do-json reply 400 {:detail "Invalid source contractId" :error (:error safe-source)})

                              (not (:ok safe-new))
                              (do-json reply 400 {:detail "Invalid newId" :error (:error safe-new)})

                              :else
                              (handle-copy-contract (partial do-json reply) config (:class safe-kind) (:id safe-source) (:id safe-new))))))
                      (catch :default err
                        (do-err reply err)))))))))

(defn register-contracts-routes!
  [app runtime config helpers]
  (register-agent-contract-routes! app runtime config helpers)
  (register-admin-contract-routes! app runtime config helpers)
  nil)
