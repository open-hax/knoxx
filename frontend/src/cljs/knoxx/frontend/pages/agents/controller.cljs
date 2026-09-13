(ns knoxx.frontend.pages.agents.controller
  "React state and client composition for the agent contract workbench."
  (:require ["@open-hax/knoxx-app-bridge" :as app]
            ["react-router-dom" :as router]
            [clojure.string :as str]
            [helix.hooks :as hooks]
            [knoxx.frontend.api.contracts :as contracts-api]
            [knoxx.frontend.api.event-agents :as event-api]
            [knoxx.frontend.auth.context :as auth-ctx]
            [knoxx.frontend.pages.agents.commands :as commands]
            [knoxx.frontend.pages.agents.model :as model]))

(def ^:private contract-client
  {:list contracts-api/list-contracts :get contracts-api/get-contract
   :validate contracts-api/validate-contract :save contracts-api/save-contract})
(def ^:private runtime-client
  {:get event-api/get-event-agent-control :start event-api/start-event-agent-runtime
   :stop event-api/stop-event-agent-runtime :reset event-api/reset-event-agent-runtime :fire event-api/fire-trigger})
(def ^:private initial-state
  {:tab "contracts" :agents [] :triggers [] :pipelines [] :role-options [] :selected-id nil
   :draft model/default-agent-contract :edn-text (model/draft->edn model/default-agent-contract)
   :parse-error nil :loading-library false :loading-contract false :saving false :validating false
   :notice nil :error "" :runtime-status nil :runtime-loading false :runtime-error ""
   :runtime-notice nil :running-job-id nil :toggling-runtime false :resetting-runtime false})

(defn- query-for ^js [^js location] (js/URLSearchParams. (str/replace (or (.-search location) "") #"^\?" "")))
(defn- location-tab [^js location] (if (= "audit" (.get (query-for location) "tab")) "audit" "contracts"))
(defn- tab-location [^js location tab]
  (let [query (query-for location)]
    (.set query "tab" tab)
    (str (.-pathname location) "?" (.toString query) (.-hash location))))
(defn- permitted? [^js auth permission]
  (boolean (or (.-isSystemAdmin auth) (.includes (or (.-permissions auth) #js []) permission))))

(defn- use-chat []
  (app/useChatWorkspaceController
    #js {:initialShowCanvas false :defaultRole "agent_librarian" :defaultActorId "agent_librarian"
         :sessionIdKey "knoxx_agents_session_id" :scratchpadStorageKey "knoxx_agents_scratchpad_state"
         :pinnedContextStorageKey "knoxx_agents_pinned_context" :sessionStateKey "knoxx_agents_chat_session_state"
         :sidebarWidthKey "knoxx_agents_sidebar_width_px"}))

(defn- chat-context [{:keys [tab selected-id draft edn-text]}]
  (when-let [contract-id (or selected-id (:contract/id draft))]
    (if (= tab "audit")
      {:id (str "agent-audit:" contract-id) :title (str contract-id " audit") :path "/agents?tab=audit"
       :snippet (str "Active and historical runs for agent contract " contract-id ".") :kind "file"}
      {:id (str "agent:" contract-id) :title contract-id :path (str "/ops/contracts/agents/" contract-id)
       :snippet (if edn-text (subs edn-text 0 (min 120 (count edn-text))) "") :kind "file"})))

(defn- use-chat-binding [^js chat {:keys [tab selected-id draft edn-text] :as state}]
  (hooks/use-effect [tab selected-id (:contract/id draft) edn-text]
    (when chat
      (try (when-let [pin (.-pinContextItem chat)]
             (when-let [context (chat-context state)] (pin (clj->js context))))
           (catch :default _ nil))))
  (hooks/use-effect [tab selected-id (:contract/id draft)]
    (when (and chat (= tab "audit"))
      (when-let [contract-id (model/selected-agent-contract-id selected-id draft)]
        (when-let [set-active-agent (.-setActiveAgentId chat)] (set-active-agent contract-id))))))

(defn- bind-commands [context]
  {:load-agent-library! #(commands/load-library! context)
   :load-runtime! #(commands/load-runtime! context)
   :handle-start-runtime! #(commands/runtime-command! context :start)
   :handle-stop-runtime! #(commands/runtime-command! context :stop)
   :handle-reset-runtime! #(commands/runtime-command! context :reset)
   :handle-run-job! #(commands/run-trigger! context %)
   :update-draft! (fn [operation & args] (apply commands/update-draft! context operation args))
   :handle-raw-change #(commands/raw-change! context %)
   :handle-new! #(commands/new-contract! context)
   :handle-validate! #(commands/validate! context)
   :handle-save! #(commands/save! context)})

(defn use-controller
  "Bind the current user's capabilities, source selection and agent chat context."
  []
  (let [auth (auth-ctx/use-auth) location (router/useLocation) navigate (router/useNavigate)
        [state set-state] (hooks/use-state #(assoc initial-state :tab (location-tab location)))
        set-field! (hooks/use-callback [set-state] (fn [field value] (set-state #(assoc % field value))))
        chat (use-chat)
        context {:state state :set-state set-state :contracts contract-client :runtime runtime-client
                 :can-save-contracts? (permitted? auth "platform.org.create")
                 :can-control-runtime? (permitted? auth "org.event_agents.control")}]
    (hooks/use-effect [location] (set-field! :tab (location-tab location)))
    (hooks/use-effect :once (commands/load-library! context))
    (hooks/use-effect [(:selected-id state)]
      (if-let [selected-id (:selected-id state)] (commands/load-contract! context selected-id)
              (commands/replace-draft! context model/default-agent-contract)))
    (use-chat-binding chat state)
    (merge state (select-keys context [:can-save-contracts? :can-control-runtime?]) (bind-commands context)
           {:chat chat :set-selected-id #(set-field! :selected-id %)
            :selected-contract-id (model/selected-agent-contract-id (:selected-id state) (:draft state))
            :select-tab! #(navigate (tab-location location %))})))
