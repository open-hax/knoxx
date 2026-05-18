(ns knoxx.backend.routes.contracts
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [knoxx.backend.contracts.resolve :as contracts-resolve]
            [knoxx.backend.events.runtime :as events-runtime]
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
    "sources" (some-> (:contract/id value) str)
    "actions" (some-> (:contract/id value) str)
    "pipelines" (some-> (:contract/id value) str)
    "triggers" (some-> (:contract/id value) str)
    "sub_agents" (some-> (:contract/id value) str)
    "actors" (some-> (:actor/id value) str)
    "roles" (some-> (:role/id value) name)
    "capabilities" (some-> (:cap/id value) name)
    "model_families" (some-> (:model-family/id value) str)
    "models" (some-> (:model/id value) model-id->slug)
    nil))

(defn- parsed-kind-label
  [contract-class value]
  (case (normalize-contract-class contract-class)
    "agents" (some-> (:contract/kind value) name)
    "policies" "policy"
    "sources" "source"
    "actors" (some-> (:actor/kind value) name)
    "roles" "role"
    "capabilities" "capability"
    "model_families" "model-family"
    "models" "model"
    "actions" "action"
    "pipelines" "pipeline"
    "triggers" "trigger"
    "sub_agents" "sub-agent"
    "unknown"))

(defn- parsed-version
  [contract-class value]
  (case (normalize-contract-class contract-class)
    "agents" (or (:contract/version value) 1)
    "policies" (or (:contract/version value) 1)
    "sources" (or (:contract/version value) 1)
    1))

(defn- parsed-enabled?
  [contract-class value]
  (case (normalize-contract-class contract-class)
    "agents" (not (false? (:enabled value)))
    "policies" (not (false? (:enabled value)))
    "sources" (not (false? (:source/enabled? value)))
    true))

(defn- wire-key
  [key]
  (if (keyword? key)
    (if-let [key-ns (namespace key)]
      (str key-ns "/" (name key))
      (name key))
    key))

(defn- wire-value
  [value]
  (cond
    (keyword? value) (wire-key value)
    (symbol? value) (wire-key value)
    (map? value) (into {} (map (fn [[k v]] [(wire-key k) (wire-value v)])) value)
    (set? value) (mapv wire-value value)
    (sequential? value) (mapv wire-value value)
    :else value))

(defn- keywordish-name
  [value]
  (cond
    (keyword? value) (wire-key value)
    (symbol? value) (wire-key value)
    (some? value) (str value)
    :else nil))

(defn- trigger-summary
  [record]
  (let [contract (:contract record)
        source (:trigger/source contract)]
    {:kind (keywordish-name (:trigger/kind contract))
     :target (:trigger/target contract)
     :schedule (:trigger/schedule contract)
     :source (wire-value source)
     :filters (wire-value (get-in contract [:data :filters]))
     :context (wire-value (get-in contract [:data :context]))}))

(defn- pipeline-summary
  [record]
  {:steps (mapv (fn [step]
                  {:id (:step/id step)
                   :contract (:step/contract step)})
                (get-in record [:contract :pipeline/steps]))})

(defn- action-summary
  [record]
  {:handler (get-in record [:contract :action/handler])})

(defn- contract-list-summary
  [record]
  (cond-> (dissoc record :contract :edn-text :file-path :ok?)
    (= "triggers" (:contractClass record))
    (assoc :trigger (trigger-summary record))

    (= "pipelines" (:contractClass record))
    (assoc :pipeline (pipeline-summary record))

    (= "actions" (:contractClass record))
    (assoc :action (action-summary record))))

(defn- wire-validation
  [validation]
  (cond-> validation
    (:contract validation) (update :contract wire-value)))

(defn- validation-warning
  [path message]
  {:path path
   :message message
   :severity "warn"})

(def ^:private mutable-agent-data-keys
  #{:world_state :world-state
    :plot_log :plot-log
    :composition_log :composition-log
    :last_tick_timestamp :last-tick-timestamp
    :composition_count :composition-count})

(defn- positive-int
  [value]
  (let [n (js/parseInt value 10)]
    (when (and (number? n) (not (js/isNaN n)) (pos? n))
      n)))

(defn- role-ref-warnings
  [path value]
  (let [warn-bare (validation-warning path "Role refs should use :role/<kebab-slug>; bare or snake_case role refs are tolerated but cause drift.")
        warn-snake (validation-warning path "Role refs should use kebab-case, e.g. :role/contract-librarian, not underscores.")]
    (cond
      (keyword? value)
      (cond-> []
        (not= "role" (namespace value)) (conj warn-bare)
        (str/includes? (name value) "_") (conj warn-snake))

      (string? value)
      (cond-> []
        (str/includes? value "_") (conj warn-snake))

      :else [])))

(defn- prompt-state-path-warnings
  [contract]
  (let [prompts [(get-in contract [:prompts :system])
                 (get-in contract [:prompts :task])]
        stale-ref? (some (fn [prompt]
                           (and (string? prompt)
                                (re-find #"(:data/|/world_state|/plot_log|:data/world_state|:data/plot_log)" prompt)))
                         prompts)]
    (if stale-ref?
      [(validation-warning ["prompts"] "Prompt references mutable :data paths (for example :data/world_state or :data/plot_log). Agent contract :data is static config; use a real state store or durable files instead.")]
      [])))

(defn- agent-contract-warnings
  [contract]
  (let [data (:data contract)
        source-config (get-in contract [:data :source])
        max-messages (positive-int (or (:max-messages source-config)
                                       (:maxMessages source-config)))
        role (get-in contract [:agent :role])
        roles (get-in contract [:agent :roles])
        source-mode (:source-mode contract)
        filters (get-in contract [:data :filters])
        channels (or (:channels filters) [])
        publish-channels (or (:publishChannels filters) (:publish_channels filters) [])]
    (vec
     (concat
      (cond-> []
        (contains? data :filter)
        (conj (validation-warning ["data" "filter"] "Runtime ignores :data/:filter. Use :data/:filters."))

        (contains? contract :source)
        (conj (validation-warning ["source"] "Event-agent runtime ignores top-level :source. Use :data {:source ...}."))

        (contains? contract :capabilities)
        (conj (validation-warning ["capabilities"] "Top-level :capabilities is legacy/inert in contract resolution. Put capability refs under :actor {:capabilities [...]}, or grant them through roles."))

        (and max-messages (> max-messages 100))
        (conj (validation-warning ["data" "source" "max-messages"] "Event-agent source max-messages is clamped to 100 at runtime."))

        (and (keyword? source-mode)
             (not= "source-mode" (namespace source-mode))
             (contains? #{:synthesize :template-synthesize} source-mode))
        (conj (validation-warning ["source-mode"] "Use :source-mode/discord-synthesis instead of opaque bare synthesis modes."))

        (and (= :source-mode/discord-synthesis source-mode)
             (empty? channels)
             (seq publish-channels))
        (conj (validation-warning ["data" "filters" "publishChannels"] ":publishChannels are output sinks only. Add explicit :channels or :guildIds for Discord source reads.")))
      (mapcat (fn [k]
                (when (contains? data k)
                  [(validation-warning ["data" (name k)] "This looks like mutable runtime state inside a static contract. Prefer Redis/OpenPlanner/durable files, not contract :data mutation.")]))
              mutable-agent-data-keys)
      (role-ref-warnings ["agent" "role"] role)
      (mapcat (fn [[idx value]]
                (role-ref-warnings ["agent" "roles" (str idx)] value))
              (map-indexed vector (or roles [])))
      (prompt-state-path-warnings contract)))))

(defn- contract-warnings
  [contract-class contract]
  (if (and (= (normalize-contract-class contract-class) "agents")
           (map? contract))
    (agent-contract-warnings contract)
    []))

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
           :warnings (contract-warnings contract-class contract)})
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

    "sources"
    (let [source-id (str ":source/" (str/replace (str new-id) #"_" "-"))]
      (-> (if (str/includes? edn-text ":contract/id")
            (str/replace edn-text #":contract/id\s+\"[^\"]+\"" (str ":contract/id \"" new-id "\""))
            (str ":contract/id \"" new-id "\"\n" edn-text))
          (cond-> (str/includes? edn-text ":source/id")
            (str/replace #":source/id\s+:[^\s\]}]+" (str ":source/id " source-id)))))

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

   Redis is a cache + fast index; disk is canonical. Invalid contract files are
   omitted by the loader and must not block backend startup or the repair UI."
  [config]
  (if-let [client (redis/get-client)]
    (-> (loader/load-all-contracts! config)
        (.then (fn [records]
                 (let [ids (->> records
                                (map (fn [record]
                                       (contract-id->index-key (:contractClass record) (:id record))))
                                distinct
                                sort
                                vec)]
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
                  (println "[contracts] sync-contract-index! failed; startup continuing:" (.-message err))
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
                           (events-runtime/debounced-reload!)
                           (println "[contracts] event-agent runtime reload scheduled after contract change")
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

(defn handle-list-contracts
  "List all contracts, optionally filtered by contract-class.
   Public so tests can call it directly."
  [do-json config contract-class]
  (-> (loader/load-all-contracts! config)
      (.then (fn [all]
               (let [contracts (cond->> all
                                 contract-class (filter #(= (:contractClass %)
                                                            (normalize-contract-class contract-class)))
                                 :always        (sort-by (juxt :contractClass :id))
                                 :always        vec)]
                  (do-json 200 {:contracts (mapv contract-list-summary contracts)}))))
      (.catch (fn [err]
                (do-json 500 {:detail (str "Failed to list contracts: " (.-message err))})))))

(defn- handle-get-contract
  [do-json config contract-class contract-id]
  (-> (.readFile fs (loader/contract-file-path config contract-class contract-id) "utf8")
      (.then (fn [edn-text]
               (let [validation (validate-contract-edn contract-class (or edn-text ""))]
                 (do-json 200 {:contractClass (normalize-contract-class contract-class)
                               :ednText (or edn-text "")
                               :contract (wire-value (:contract validation))
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
                                   :contract (wire-value parsed)
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
  (do-json 200 (assoc (wire-validation (validate-contract-edn contract-class edn-text))
                      :contractClass (normalize-contract-class contract-class))))

(defn- handle-agent-list-contracts
  [do-text config contract-class]
  (-> (loader/list-contract-ids! config contract-class)
      (.then (fn [ids]
               (do-text 200 (pr-str ids))))
      (.catch (fn [err]
                (do-text 500 (str ";; Failed to list contracts: " (.-message err)))))))

(defn- handle-agent-get-contract-edn
  [do-text config contract-class contract-id]
  (-> (.readFile fs (loader/contract-file-path config contract-class contract-id) "utf8")
      (.then (fn [edn-text]
               (do-text 200 (str edn-text))))
      (.catch (fn [err]
                (if (= "ENOENT" (.-code err))
                  (do-text 404 (str ";; Contract not found: " contract-id))
                  (do-text 500 (str ";; Failed to read contract: " (.-message err))))))))

(defn- handle-agent-validate-contract-edn
  [do-json contract-class edn-text]
  (do-json 200 (assoc (wire-validation (validate-contract-edn contract-class edn-text))
                      :contractClass (normalize-contract-class contract-class))))

(defn- handle-agent-put-contract-edn
  [do-text config contract-class contract-id edn-text]
  (let [klass (normalize-contract-class contract-class)
        validation (validate-contract-edn klass edn-text)]
    (if-not (:ok validation)
      (do-text 422 (pr-str {:ok false
                            :errors (:errors validation)
                            :warnings (:warnings validation)}))
      (let [parsed (:contract validation)
            parsed-id (parsed-record-id klass parsed)
            route-id (str contract-id)]
        (if (not= route-id parsed-id)
          (do-text 400 (pr-str {:ok false
                                :error "contract_id_mismatch"
                                :routeContractId route-id
                                :ednContractId parsed-id}))
          (-> (loader/write-edn-file! (loader/contract-file-path config klass route-id) edn-text)
              (.then (fn [_]
                       (sync-contract-index! config)))
              (.then (fn [_]
                       (do-text 200 (pr-str {:ok true
                                             :contractClass klass
                                             :contract/id route-id
                                             :contract parsed
                                             :warnings (:warnings validation)}))))
              (.catch (fn [err]
                        (do-text 500 (str ";; Failed to save contract: " (.-message err)))))))))))

(defn- handle-ui-actions
  [do-json config actor-id surface]
  (let [resolved (contracts-resolve/ui-actions-for-actor config actor-id surface)]
    (do-json 200 {:actor_id (:actor-id resolved)
                  :surface (:surface resolved)
                  :default_agent_id (:default-agent-id resolved)
                  :actions (:actions resolved)})))

(defn- text-response!
  [reply status text]
  (.end reply (.status reply status) text #js {"Content-Type" "text/plain; charset=utf-8"}))

(defn- body-map
  [request]
  (js->clj (or (aget request "body") #js {}) :keywordize-keys true))

(defn- request-contract-class
  [request default]
  (or (aget request "query" "kind")
      (aget request "query" "class")
      default))

(defn- body-contract-class
  ([body default]
   (body-contract-class body nil default))
  ([body request default]
   (or (:kind body)
       (:class body)
       (:contract_class body)
       (:contract-class body)
       (some-> request (aget "query" "kind"))
       (some-> request (aget "query" "class"))
       default)))

(defn- body-edn-text
  [body]
  (str (or (:ednText body)
           (:edn_text body)
           (:edn-text body)
           "")))

(defn- with-route-context
  [runtime do-ctx do-err f]
  (fn [request reply]
    (do-ctx runtime request reply
      (fn [ctx]
        (try
          (f ctx request reply)
          (catch :default err
            (do-err reply err)))))))

(defn- agent-ui-actions-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [actor-id (or (aget request "query" "actor")
                         (aget request "query" "actor_id")
                         (aget request "query" "actorId"))
            surface (or (aget request "query" "surface")
                        (aget request "query" "surface_id")
                        (aget request "query" "surfaceId"))]
        (handle-ui-actions (partial do-json reply) config actor-id surface)))))

(defn- agent-list-contracts-route
  [runtime config do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [safe-kind (safe-contract-class (request-contract-class request "agents"))]
        (if-not (:ok safe-kind)
          (text-response! reply 400 (str ";; Invalid contract class: " (:error safe-kind)))
          (handle-agent-list-contracts (partial text-response! reply) config (:class safe-kind)))))))

(defn- agent-validate-contract-route
  [runtime do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [body (body-map request)
            safe-kind (safe-contract-class (body-contract-class body request "agents"))]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (handle-agent-validate-contract-edn (partial do-json reply) (:class safe-kind) (body-edn-text body)))))))

(defn- agent-get-contract-route
  [runtime config do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [contract-id (str (or (aget request "params" "contractId") ""))
            safe (safe-contract-id contract-id)
            safe-kind (safe-contract-class (request-contract-class request "agents"))]
        (cond
          (str/blank? contract-id) (text-response! reply 400 ";; contractId is required")
          (not (:ok safe-kind)) (text-response! reply 400 (str ";; Invalid contract class: " (:error safe-kind)))
          (not (:ok safe)) (text-response! reply 400 (str ";; Invalid contractId: " (:error safe)))
          :else (handle-agent-get-contract-edn (partial text-response! reply) config (:class safe-kind) (:id safe)))))))

(defn- agent-put-contract-route
  [runtime config do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [contract-id (str (or (aget request "params" "contractId") ""))
            safe (safe-contract-id contract-id)
            safe-kind (safe-contract-class (request-contract-class request "agents"))
            edn-text (str (or (aget request "body") ""))]
        (cond
          (str/blank? contract-id) (text-response! reply 400 ";; contractId is required")
          (not (:ok safe-kind)) (text-response! reply 400 (str ";; Invalid contract class: " (:error safe-kind)))
          (not (:ok safe)) (text-response! reply 400 (str ";; Invalid contractId: " (:error safe)))
          :else (handle-agent-put-contract-edn (partial text-response! reply) config (:class safe-kind) (:id safe) edn-text))))))

(defn- register-agent-contract-routes!
  [app runtime config helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)]
    (do-route app "GET" "/api/contracts/ui-actions"
              (agent-ui-actions-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/agent/contracts"
              (agent-list-contracts-route runtime config do-err do-ctx do-perm))
    (do-route app "POST" "/api/agent/contracts/validate"
              (agent-validate-contract-route runtime do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/agent/contracts/:contractId"
              (agent-get-contract-route runtime config do-err do-ctx do-perm))
    (do-route app "PUT" "/api/agent/contracts/:contractId"
              (agent-put-contract-route runtime config do-err do-ctx do-perm))))

(defn- admin-list-contracts-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [kind (request-contract-class request nil)
            safe-kind (if kind (safe-contract-class kind) {:ok true :class nil})]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (handle-list-contracts (partial do-json reply) config (:class safe-kind)))))))

(defn- admin-get-contract-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [contract-id (str (or (aget request "params" "contractId") ""))
            safe (safe-contract-id contract-id)
            safe-kind (safe-contract-class (request-contract-class request "agents"))]
        (cond
          (str/blank? contract-id) (do-json reply 400 {:detail "contractId is required"})
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (not (:ok safe)) (do-json reply 400 {:detail "Invalid contractId" :error (:error safe)})
          :else (handle-get-contract (partial do-json reply) config (:class safe-kind) (:id safe)))))))

(defn- admin-save-contract-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [contract-id (str (or (aget request "params" "contractId") ""))
            body (body-map request)
            safe (safe-contract-id contract-id)
            safe-kind (safe-contract-class (body-contract-class body request "agents"))]
        (cond
          (str/blank? contract-id) (do-json reply 400 {:detail "contractId is required"})
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (not (:ok safe)) (do-json reply 400 {:detail "Invalid contractId" :error (:error safe)})
          :else (handle-save-contract (partial do-json reply) config (:class safe-kind) (:id safe) (body-edn-text body)))))))

(defn- admin-validate-contract-route
  [runtime do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [body (body-map request)
            safe-kind (safe-contract-class (body-contract-class body "agents"))]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (handle-validate-contract (partial do-json reply) (:class safe-kind) (body-edn-text body)))))))

(defn- admin-copy-contract-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [source-id (str (or (aget request "params" "contractId") ""))
            body (body-map request)
            new-id (str (or (:newId body) ""))
            safe-kind (safe-contract-class (body-contract-class body "agents"))
            safe-source (safe-contract-id source-id)
            safe-new (safe-contract-id new-id)]
        (cond
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (or (str/blank? source-id) (str/blank? new-id)) (do-json reply 400 {:detail "source contractId and newId are required"})
          (not (:ok safe-source)) (do-json reply 400 {:detail "Invalid source contractId" :error (:error safe-source)})
          (not (:ok safe-new)) (do-json reply 400 {:detail "Invalid newId" :error (:error safe-new)})
          :else (handle-copy-contract (partial do-json reply) config (:class safe-kind) (:id safe-source) (:id safe-new)))))))

(defn- register-admin-contract-routes!
  [app runtime config helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)]
    (do-route app "GET" "/api/admin/contracts"
              (admin-list-contracts-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/admin/contracts/:contractId"
              (admin-get-contract-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "PUT" "/api/admin/contracts/:contractId"
              (admin-save-contract-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "POST" "/api/admin/contracts/validate"
              (admin-validate-contract-route runtime do-json do-err do-ctx do-perm))
    (do-route app "POST" "/api/admin/contracts/:contractId/copy"
              (admin-copy-contract-route runtime config do-json do-err do-ctx do-perm))))

(defn register-contracts-routes!
  [app runtime config helpers]
  (register-agent-contract-routes! app runtime config helpers)
  (register-admin-contract-routes! app runtime config helpers)
  nil)
