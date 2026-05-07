(ns knoxx.backend.actions.invoke-sub-agent
  "Action: :invoke/sub-agent

   Spawns a child agent from a parent agent's context. Supports three modes:
   - :fire-and-forget — spawn and return immediately
   - :await           — spawn and wait for the queue/spawn result with timeout
   - :collect         — spawn multiple children and aggregate their spawn results

   Sub-agent contracts live in contracts/sub_agents/*.edn. The action may also
   pass pre-resolved contracts in :action/with for tests or custom runners."
  (:require [clojure.string :as str]
            [knoxx.backend.actions.registry :refer [run-action!]]
            [knoxx.backend.contracts.loader :as contracts]
            [knoxx.backend.contracts.roles :as roles]
            [knoxx.backend.tools.registry :as tools]))

(defn- id->string
  [value]
  (some-> (cond
            (keyword? value) (name value)
            (some? value) (str value)
            :else nil)
          str/trim
          not-empty))

(defn- mode->keyword
  [value]
  (cond
    (keyword? value) value
    (some? value) (keyword (str/replace (str/lower-case (str/trim (str value))) #"_" "-"))
    :else nil))

(defn- load-sub-agent-contract!
  "Load a sub-agent contract by ID. Resolves to the parsed contract map or nil."
  [config sub-agent-id]
  (if-let [id (id->string sub-agent-id)]
    (-> (contracts/load-contract! config "sub_agents" id)
        (.then (fn [{:keys [ok? contract validation]}]
                 (cond
                   ok?
                   contract

                   (= "Contract not found" (some-> validation :errors first :message))
                   (do (js/console.warn "[sub-agent] contract not found:" id)
                       nil)

                   :else
                   (do (js/console.warn "[sub-agent] invalid contract" id (pr-str validation))
                       nil)))))
    (js/Promise.resolve nil)))

(defn- contract-from-action
  [action-with sub-agent-id]
  (let [id (id->string sub-agent-id)
        contracts-by-id (:sub-agent-contracts action-with)]
    (or (:sub-agent-contract action-with)
        (get contracts-by-id sub-agent-id)
        (get contracts-by-id id)
        (get contracts-by-id (keyword id)))))

(defn- resolve-sub-agent-contract!
  [config action-with sub-agent-id]
  (if-let [contract (contract-from-action action-with sub-agent-id)]
    (js/Promise.resolve contract)
    (load-sub-agent-contract! config sub-agent-id)))

(defn- fallback-contract
  [sub-agent-id]
  {:contract/id (or (id->string sub-agent-id) "unknown-sub-agent")
   :contract/kind :sub-agent})

(defn- enabled-contract?
  [contract]
  (not (false? (:enabled contract))))

(defn- parent-context-preamble
  "Build a context preamble for the sub-agent describing its parent lineage."
  [parent-job parent-event]
  (let [parent-id (:id parent-job)
        event-kind (:eventKind parent-event)
        source-kind (:sourceKind parent-event)]
    (str "[Sub-Agent Context]\n"
         "You are a sub-agent spawned by parent agent: " parent-id "\n"
         "Trigger event: " event-kind " from " source-kind "\n"
         "Complete only the delegated task and report your result clearly.\n\n")))

(defn- capability->tool-ids
  [config cap]
  (let [from-contract (roles/capability-tool-ids config cap)
        direct-tool (some-> (if (keyword? cap) (name cap) cap)
                            tools/normalize-tool-id
                            str/trim
                            not-empty)]
    (if (seq from-contract)
      from-contract
      (cond-> [] direct-tool (conj direct-tool)))))

(defn- capabilities->tool-policies
  [config caps]
  (->> (or caps [])
       (mapcat #(capability->tool-ids config %))
       (remove str/blank?)
       distinct
       sort
       (mapv (fn [tool-id] {:toolId tool-id :effect "allow"}))))

(defn- parent-tool-policies
  [parent-spec]
  (vec (or (:toolPolicies parent-spec)
           (:tool-policies parent-spec)
           [])))

(defn- resolve-tool-policies
  [config parent-spec sub-agent-config sub-agent-contract]
  (let [parent-mode (or (mode->keyword (:sub-agent/parent-capabilities sub-agent-contract))
                        :inherit)
        configured-caps (or (:sub-agent/capabilities sub-agent-contract)
                            (:restrict-capabilities sub-agent-config))
        sub-policies (capabilities->tool-policies config configured-caps)
        parent-policies (parent-tool-policies parent-spec)]
    (case parent-mode
      :none sub-policies
      :restrict sub-policies
      :inherit (if (seq sub-policies)
                 (->> (concat parent-policies sub-policies)
                      (group-by :toolId)
                      vals
                      (map first)
                      (sort-by :toolId)
                      vec)
                 parent-policies)
      (if (seq sub-policies) sub-policies parent-policies))))

(defn- compact-spec
  [spec]
  (into {}
        (filter (fn [[_ value]]
                  (not (or (nil? value)
                           (and (string? value) (str/blank? value))
                           (and (sequential? value) (empty? value))))))
        spec))

(defn- merge-sub-agent-spec
  "Merge parent agent spec with sub-agent contract to create the spawn spec."
  [config parent-spec sub-agent-config sub-agent-contract full-system-prompt task-prompt]
  (let [contract-agent (or (:agent sub-agent-contract) {})
        model (or (:sub-agent/model sub-agent-contract)
                  (:model contract-agent)
                  (:model sub-agent-config)
                  (:model parent-spec))
        thinking (or (:sub-agent/thinking sub-agent-contract)
                     (:thinking contract-agent)
                     (:thinking sub-agent-config)
                     (:thinkingLevel parent-spec)
                     (:thinking-level parent-spec))
        role (or (:sub-agent/role sub-agent-contract)
                 (:role contract-agent)
                 (when (:inherit-role sub-agent-config)
                   (:role parent-spec)))
        context-policy (or (:context sub-agent-contract)
                           (:context-policy sub-agent-config))
        tool-policies (resolve-tool-policies config parent-spec sub-agent-config sub-agent-contract)]
    (compact-spec
     {:model model
      :role role
      ;; Event-agent runner consumes camel-case keys; direct-start also accepts
      ;; snake/kebab variants after this becomes an HTTP/direct payload.
      :thinkingLevel thinking
      :thinking-level thinking
      :systemPrompt full-system-prompt
      :system-prompt full-system-prompt
      :taskPrompt task-prompt
      :task-prompt task-prompt
      :toolPolicies tool-policies
      :tool-policies tool-policies
      :contextPolicy context-policy
      :context-policy context-policy
      :subAgentId (:contract/id sub-agent-contract)
      :sub-agent-id (:contract/id sub-agent-contract)
      :parentAgentId (:id (:parent-job sub-agent-config))
      :parent-agent-id (:id (:parent-job sub-agent-config))
      :parentRunId (:parent-run-id sub-agent-config)
      :parent-run-id (:parent-run-id sub-agent-config)
      :spawnKind "sub-agent"
      :spawn-kind "sub-agent"
      :parentCapabilitiesMode (:sub-agent/parent-capabilities sub-agent-contract)})))

(defn- build-sub-agent-payload
  "Build the spawn payload for a sub-agent run."
  [config parent-job parent-event sub-agent-id sub-agent-config sub-agent-contract]
  (let [parent-spec (:agentSpec parent-job {})
        system-prompt (get-in sub-agent-contract [:prompts :system] "")
        task-prompt (get-in sub-agent-contract [:prompts :task] "")
        shared-context (or (:shared-context sub-agent-config)
                           (get-in sub-agent-config [:shared-context])
                           (get-in sub-agent-contract [:context])
                           {})
        context-preamble (parent-context-preamble parent-job parent-event)
        full-system-prompt (str context-preamble system-prompt)
        result-key (or (:result-key sub-agent-config) (id->string sub-agent-id))
        sub-agent-config (assoc sub-agent-config
                                :parent-job parent-job
                                :parent-run-id (get-in parent-job [:data :run-id]))
        agent-spec (merge-sub-agent-spec config parent-spec sub-agent-config sub-agent-contract full-system-prompt task-prompt)
        spawn-id (str "sub-agent-" (id->string sub-agent-id) "-" (:id parent-job) "-" (.now js/Date))
        timeout-ms (or (:sub-agent/timeout-ms sub-agent-contract)
                       (:timeout-ms sub-agent-config)
                       30000)
        mode (or (mode->keyword (:mode sub-agent-config))
                 (mode->keyword (:sub-agent/mode sub-agent-contract))
                 :fire-and-forget)]
    {:sub-agent-id (id->string sub-agent-id)
     :job-id spawn-id
     :parent-id (:id parent-job)
     :parent-run-id (get-in parent-job [:data :run-id])
     :result-key result-key
     :agent-spec agent-spec
     :system-prompt full-system-prompt
     :task-prompt task-prompt
     :shared-context shared-context
     :mode mode
     :timeout-ms timeout-ms}))

(defn- enriched-content
  [payload event-payload]
  (str/join "\n\n"
            (remove str/blank?
                    [(:task-prompt payload)
                     (when (seq (:shared-context payload))
                       (str "Shared context:\n" (pr-str (:shared-context payload))))
                     (or (:content event-payload)
                         (:summary event-payload)
                         (:payloadPreview event-payload))])))

(defn- sub-agent-job
  [job payload]
  (-> job
      (assoc :id (:job-id payload))
      (assoc :name (str "Sub-agent: " (:sub-agent-id payload)))
      (assoc :description (str "Sub-agent spawned by " (:parent-id payload)))
      (assoc :agentSpec (:agent-spec payload))
      (assoc :contractSourceId nil)
      (assoc :contractSourceKind "sub-agent")
      (assoc :contractSourceKey (str "sub-agent:" (:sub-agent-id payload)))
      (assoc-in [:source :config :stickySession] false)
      (assoc-in [:data :parent-id] (:parent-id payload))
      (assoc-in [:data :parent-run-id] (:parent-run-id payload))
      (assoc-in [:data :sub-agent-id] (:sub-agent-id payload))
      (assoc-in [:data :result-key] (:result-key payload))
      (assoc-in [:data :shared-context] (:shared-context payload))))

(defn- sub-agent-event
  [event payload]
  (update event :payload
          (fn [event-payload]
            (let [event-payload (or event-payload {})]
              (assoc event-payload :content (enriched-content payload event-payload))))))

(defn- timeout-promise
  [payload]
  (js/Promise.
   (fn [_resolve reject]
     (js/setTimeout #(reject (js/Error. (str "Sub-agent timeout: " (:sub-agent-id payload))))
                    (:timeout-ms payload 30000)))))

(defn- spawn-once!
  [ctx payload]
  (let [{:keys [run-agent! config event job]} ctx]
    (run-agent! config (sub-agent-job job payload) (sub-agent-event event payload))))

(defn- fire-and-forget!
  [ctx payload]
  (-> (spawn-once! ctx payload)
      (.then (fn [result]
               {:ok true
                :action/kind :invoke/sub-agent
                :mode :fire-and-forget
                :sub-agent-id (:sub-agent-id payload)
                :parent-id (:parent-id payload)
                :result-key (:result-key payload)
                :spawn-result result}))))

(defn- await-result!
  [ctx payload]
  (-> (js/Promise.race (clj->js [(spawn-once! ctx payload)
                                 (timeout-promise payload)]))
      (.then (fn [result]
               {:ok true
                :action/kind :invoke/sub-agent
                :mode :await
                :sub-agent-id (:sub-agent-id payload)
                :parent-id (:parent-id payload)
                :result-key (:result-key payload)
                :result result}))
      (.catch (fn [err]
                {:ok false
                 :action/kind :invoke/sub-agent
                 :mode :await
                 :sub-agent-id (:sub-agent-id payload)
                 :parent-id (:parent-id payload)
                 :result-key (:result-key payload)
                 :error (or (.-message err) (str err))}))))

(defn- run-payload!
  [ctx payload]
  (case (:mode payload)
    :fire-and-forget (fire-and-forget! ctx payload)
    :await (await-result! ctx payload)
    :collect (await-result! ctx payload)
    (fire-and-forget! ctx payload)))

(defn- collect-results!
  [ctx payloads]
  (-> (js/Promise.all (clj->js (mapv #(run-payload! ctx %) payloads)))
      (.then (fn [results]
               {:ok (every? :ok results)
                :action/kind :invoke/sub-agent
                :mode :collect
                :results (reduce (fn [acc result]
                                   (assoc acc (:result-key result) result))
                                 {}
                                 results)
                :count (count results)}))))

(defn- payload-for-id!
  [ctx action-with shared-config sub-id]
  (let [{:keys [config event job]} ctx
        id (id->string sub-id)]
    (-> (resolve-sub-agent-contract! config action-with id)
        (.then (fn [loaded-contract]
                 (let [sub-agent-contract (or loaded-contract (fallback-contract id))]
                   (if-not (enabled-contract? sub-agent-contract)
                     (throw (js/Error. (str "Sub-agent disabled: " id)))
                     (build-sub-agent-payload config job event id shared-config sub-agent-contract))))))))

(defmethod run-action! :invoke/sub-agent
  [{:keys [config] :as ctx} action]
  (let [action-with (:action/with action {})
        sub-agent-ids (or (:sub-agents action-with)
                          (:sub-agent-ids action-with)
                          [])
        explicit-mode (or (mode->keyword (:mode action-with))
                          (mode->keyword (:sub-agent/mode action-with)))
        shared-config (cond-> (or (:sub-agent-config action-with) {})
                        explicit-mode (assoc :mode explicit-mode))]
    (cond
      (:sub-agent-id action-with)
      (-> (payload-for-id! ctx action-with shared-config (:sub-agent-id action-with))
          (.then #(run-payload! ctx %)))

      (seq sub-agent-ids)
      (-> (js/Promise.all (clj->js (mapv #(payload-for-id! ctx action-with shared-config %) sub-agent-ids)))
          (.then (fn [payloads]
                   (let [payloads (vec payloads)
                         mode (or explicit-mode (:mode (first payloads)) :fire-and-forget)]
                     (if (= :fire-and-forget mode)
                       (-> (js/Promise.all (clj->js (mapv #(fire-and-forget! ctx %) payloads)))
                           (.then (fn [results]
                                    {:ok (every? :ok results)
                                     :action/kind :invoke/sub-agent
                                     :mode :fire-and-forget
                                     :results (vec results)
                                     :count (count results)})))
                       (collect-results! ctx payloads))))))

      :else
      (js/Promise.resolve
       {:ok false
        :action/kind :invoke/sub-agent
        :error "No sub-agent-id or sub-agents specified in action/with"}))))
