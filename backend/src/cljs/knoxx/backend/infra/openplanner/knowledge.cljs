(ns knoxx.backend.infra.openplanner.knowledge
  "Attributed knowledge writes and authorized reads over the existing OpenPlanner SDK."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.social.conversation :as conversations]
            [knoxx.backend.domain.text :as text]
            [knoxx.backend.domain.time :as time]
            [knoxx.backend.domain.tools :as tools]
            [knoxx.backend.extern.crypto :as crypto]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.infra.agent.social-delivery :as delivery]
            [knoxx.backend.infra.agent.tool-context :as tool-context]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.clients.openplanner :as planner]
            [knoxx.backend.infra.openplanner.knowledge-scope :as identity]
            [knoxx.backend.infra.openplanner.memory :as memory]
            [knoxx.backend.law.conversation :as conversation-law]
            [knoxx.backend.law.knowledge :as law]))

(def project "knoxx-knowledge")

(defn- isolated-client
  "Use an explicitly supplied actor knowledge store. The generic OpenPlanner
   client is also exposed through a broad proxy and is not privacy authority."
  [config]
  (or (:social-knowledge-client config)
      (throw (ex-info "No isolated social knowledge store has been configured"
                      {:status 503 :code "social_knowledge_unavailable"}))))

(defn- new-id [kind]
  (str project ":" kind ":" (crypto/random-hex 16)))

(defn- scope [row]
  (get-in row [:extra :memory_access]))

(defn- project-row [row]
  (let [extra (:extra row)]
    (cond-> {:id (:id row) :kind (:knowledge_kind extra) :title (:label extra)
     :text (:text row) :created_at (:ts row) :owner (:actor_id (scope row))
     :visibility (:visibility (scope row)) :tier (:epistemic_tier extra)
     :confidence (:confidence extra) :evidence (:evidence extra)
     :source (:source_node_id extra) :target (:target_node_id extra) :relation (:relation extra)}
      (:conversation_cursor extra) (assoc :conversation (:conversation_cursor extra)))))

(defn- ^:async original! [client id]
  (when (and (string? id) (str/starts-with? id (str project ":")))
    (first (:rows (await (planner/mongo-query! client
                                             {:collection "events" :filter {:id id :project project}
                                              :limit 1}))))))

(defn- ^:async authorized-original! [client principal id]
  (let [row (await (original! client id))]
    (when-not (and row (law/visible? principal (scope row)))
      (throw (ex-info "Knowledge resource is outside the acting identity"
                      {:status 403 :code "knowledge_scope_denied"})))
    row))

(defn entry-events
  "Build the indexable original and its separate projection for an isolated store."
  [config id principal parameters]
  (let [{:keys [text title visibility tier confidence evidence]} parameters
        access (law/scope-for principal visibility)
        node (memory/session-graph-node-event
     (assoc config :session-project-name project)
     {:event-id id :node-id id :ts (time/now-iso) :session id :message id
      :node-type "knowledge" :text text :label (or title (subs text 0 (min 100 (count text))))
      :extra (merge {:knowledge_kind "entry" :memory_access access
                     :epistemic_tier (or tier "provisional") :confidence (or confidence 0.5)
                     :evidence (vec evidence) :actor_id (:actor-id principal)
                     :org_slug (:org-slug principal)}
                    (when (:org-id principal) {:org_id (:org-id principal)}))})]
      [(assoc node :kind "knoxx.knowledge")
       (assoc node :id (str id ":node"))]))

(defn- ^:async persist-entry!
  [config principal parameters cursor]
  (let [parameters (law/assert-valid! law/RecordParameters parameters)
        client (planner/assert-event-projection-repair-supported! (isolated-client config))
        id (new-id "entry")
        events (entry-events config id principal parameters)
        [original graph-node] (if cursor (mapv #(assoc-in % [:extra :conversation_cursor] cursor) events) events)]
    (try
      (await (planner/ingest-events-awaiting-projections! client [original]))
      (await (planner/events! client [graph-node]))
      (catch :default error
        (throw (ex-info (str "Knowledge projection did not complete; original id=" id)
                        {:status 503 :code "knowledge_projection_incomplete" :id id} error))))
    (project-row (await (authorized-original! client principal id)))))

(defn record-execute
  "Persist original knowledge and await its vector projection before success."
  [auth-context]
  (^:async fn [_runtime config _call-id params _a _b _c]
    (let [parameters (law/assert-valid! law/RecordParameters (xtools/parameter-map params))
          principal (law/assert-valid! law/Principal (identity/principal config auth-context))
          row (await (persist-entry! config principal parameters nil))]
      (text/tool-text-result (pr-str row) row))))

(defn remember-execute
  "Remember an actual current-turn delivery; callers supply only semantic notes."
  [auth-context]
  (^:async fn [_runtime config _call-id params _a _b _c]
    (let [parameters (conversation-law/assert-valid! conversation-law/Remember (xtools/parameter-map params))
          principal (law/assert-valid! law/Principal (identity/principal config auth-context))
          context (tool-context/current)
          _ (when-not (= (:actor-id principal) (get-in context [:agent-spec :actor-id]))
              (throw (ex-info "Conversation memory requires this actor in a live agent turn; direct operator MCP has no current-turn publication evidence."
                              {:code "conversation_agent_turn_required" :phase :preflight})))
          run (delivery/captured-run! {:actor-id (:actor-id principal) :conversation-id (:conversation-id context)}
                                     "Conversation memory")
          cursor (conversations/derive-cursor run parameters (time/now-iso))
          record {:title (str "social-conversation: " (:platform cursor) " / "
                              (subs (:topic cursor) 0 (min 120 (count (:topic cursor)))))
                  :text (str "social-conversation receipt-derived snapshot " (pr-str cursor))
                  :visibility "private" :tier "observed" :confidence 1
                  :evidence [(get-in cursor [:latest :id]) (get-in cursor [:root :id]) (str "run:" (:run-id cursor))]}
          row (await (persist-entry! config principal record cursor))]
      (text/tool-text-result (pr-str row) row))))

(defn- conversation-filter
  [principal {:keys [platform root]}]
  (cond-> {:project project :kind "knoxx.knowledge"
           "extra.memory_access.actor_id" (:actor-id principal)
           "extra.memory_access.org_slug" (:org-slug principal)
           "extra.memory_access.visibility" "private"
           "extra.conversation_cursor.version" 1}
    (:org-id principal) (assoc :$or [{"extra.memory_access.org_id" (:org-id principal)}
                                     {"extra.memory_access.org_id" {:$exists false}}
                                     {"extra.memory_access.org_id" nil}
                                     {"extra.memory_access.org_id" ""}])
    platform (assoc "extra.conversation_cursor.platform" platform)
    root (assoc "extra.conversation_cursor.root.id" root)))

(defn conversations-execute
  "Read private receipt-derived snapshots newest first; history is paginated."
  [auth-context]
  (^:async fn [_runtime config _call-id params _a _b _c]
    (let [parameters (conversation-law/assert-valid! conversation-law/ListParameters (xtools/parameter-map params))
          principal (law/assert-valid! law/Principal (identity/principal config auth-context))
          offset (get parameters :offset 0) limit (get parameters :limit 6)
          result (await (planner/mongo-query! (isolated-client config)
                                             {:collection "events" :filter (conversation-filter principal parameters)
                                              :sort {:ts -1 :id -1} :skip offset :limit limit}))
          visible (filter #(and (= "private" (:visibility (scope %)))
                                (law/visible? principal (scope %))
                                (= (:actor-id principal) (get-in % [:extra :conversation_cursor :actor-id]))
                                ((get conversation-law/validators conversation-law/Cursor) (get-in % [:extra :conversation_cursor])))
                          (:rows result))
          response {:snapshots (mapv project-row visible) :order "server observation time, newest first"
                    :nextOffset (when (< (+ offset limit) (:total result 0)) (+ offset limit))
                    :history true :visibility "private"
                    :guidance "Use the newest snapshot for a root; older entries remain history. Read actual platform context before acting. Topic/state/question are provisional model annotations; IDs and observation time come from receipts."}]
      (text/tool-text-result (pr-str response) response))))

(defn read-execute
  "Read exactly one original event after actor ownership authorization."
  [auth-context]
  (^:async fn [_runtime config _call-id params _a _b _c]
    (let [{:keys [id]} (law/assert-valid! law/ReadParameters (xtools/parameter-map params))
          principal (law/assert-valid! law/Principal (identity/principal config auth-context))
          row (await (authorized-original! (isolated-client config) principal id))]
      (text/tool-text-result (pr-str (project-row row)) (project-row row)))))

(defn search-execute
  "Search one knowledge project; never return vector text without the original ACL."
  [auth-context]
  (^:async fn [_runtime config _call-id params _a _b _c]
    (let [{:keys [query limit]} (law/assert-valid! law/SearchParameters (xtools/parameter-map params))
          principal (law/assert-valid! law/Principal (identity/principal config auth-context))
          client (isolated-client config)
          literal (await (planner/mongo-query! client
                                               {:collection "events"
                                                :filter {:project project :kind "knoxx.knowledge"
                                                         :text {:$regex (law/literal-query-pattern query) :$options "i"}}
                                                :limit 50}))
          result (await (planner/vector-search! client {:q query :project project :kind "knoxx.knowledge" :k 50}))
          hits (memory/vector-result-hits (:result result))
          ids (distinct (concat (keep :id (:rows literal))
                                (keep #(or (get-in % [:metadata :session]) (:session %)) hits)))
          rows (loop [remaining ids visible []]
                 (if-let [id (first remaining)]
                   (let [row (await (original! client id))]
                     (recur (next remaining)
                            (cond-> visible (and row (law/visible? principal (scope row)))
                              (conj (project-row row)))))
                   visible))
          response {:query query :entries (vec (take (or limit 6) rows))
                    :store "local OpenPlanner SDK knowledge graph"
                    :authorization "Original event ownership checked before returning text"}]
      (text/tool-text-result (pr-str response) response))))

(defn link-execute
  "Append an explicit relationship claim; its identity cannot overwrite another claim."
  [auth-context]
  (^:async fn [_runtime config _call-id params _a _b _c]
    (let [{:keys [source target relation text visibility evidence]}
          (law/assert-valid! law/LinkParameters (xtools/parameter-map params))
          principal (law/assert-valid! law/Principal (identity/principal config auth-context))
          client (isolated-client config)
          source-row (await (authorized-original! client principal source))
          target-row (await (authorized-original! client principal target))
          _ (law/assert-link-endpoints! source-row target-row)
          access (law/link-scope principal (scope source-row) (scope target-row) visibility)
          id (new-id "link")
          event (memory/session-graph-edge-event
                 (assoc config :session-project-name project)
                 {:event-id id :ts (time/now-iso) :session id :message id
                  :edge-type (str "knowledge/" relation "/" (last (str/split id #":")))
                  :source-node-id source :target-node-id target :source-lake project :target-lake project
                  :extra {:knowledge_kind "link" :memory_access access :relation relation
                          :claim text :evidence (vec evidence) :epistemic_tier "derived"
                          :actor_id (:actor-id principal) :org_slug (:org-slug principal)}})]
      (await (planner/events! client [(assoc event :text text)]))
      (let [row (await (authorized-original! client principal id))]
        (text/tool-text-result (pr-str (project-row row)) (project-row row))))))

(defn create-tools
  "Expose only resolved capabilities; identity and ownership are rechecked on execution."
  [runtime config auth-context]
  (into []
        (keep (fn [[id description schema execute]]
                (when (or (nil? auth-context) (authz/ctx-tool-allowed? auth-context id))
                  (tools/create-tool-obj id id description description
                                         ["Owner identity comes from the authenticated actor, never from input. Private is the default. Sharing and relationships are explicit claims with source evidence."]
                                         schema execute runtime config))))
        [["social.remember" "Remember your latest confirmed Discord or Bluesky conversation in this live agent turn. Supply only topic, open/closed state and an optional question. Actual message/post IDs, root, CID, actor, run and observation time are derived from receipts, never copied from model input. Creates private memory and sends no public message." (conversation-law/input-schema conversation-law/Remember) (remember-execute auth-context)]
         ["social.conversations" "Read your private receipt-derived conversation snapshots, newest first. Optionally filter platform or exact root and page history. Use the newest snapshot for that root and read current platform context before replying." (conversation-law/input-schema conversation-law/ListParameters) (conversations-execute auth-context)]
         ["knowledge.record" "Create an attributed private or explicitly shared knowledge entry and await its embedding." law/RecordParameters (record-execute auth-context)]
         ["knowledge.read" "Read an exact knowledge entry or relationship only under its allowed actor scope." law/ReadParameters (read-execute auth-context)]
         ["knowledge.search" "Search accessible shared/own-private knowledge; authorize originals before returning text." law/SearchParameters (search-execute auth-context)]
         ["knowledge.link" "Record an explicit evidenced relationship between accessible entries; never expose private endpoints." law/LinkParameters (link-execute auth-context)]]))
