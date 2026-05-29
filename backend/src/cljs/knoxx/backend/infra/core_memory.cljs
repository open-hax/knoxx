(ns knoxx.backend.infra.core-memory
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :refer [system-admin? ctx-org-id ctx-membership-id ctx-user-id ctx-permitted?]]
            [knoxx.backend.infra.document-state :refer [normalize-relative-path]]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.extern.row-extra :as row-extra]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.infra.config :refer [cfg]]
            [knoxx.backend.infra.tooling :as tooling]))

(defn parse-json-object
  [value]
  (row-extra/parse-core-memory-extra value))

(defn row-extra-map
  [row]
  (or (row-extra/parse-core-memory-extra (:extra row)) {}))

(def workspace-path-pattern
  #"((?:orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.ημ)/[A-Za-z0-9._~:/+-]+)")

(def devel-path-pattern
  "Backward-compatible alias for callers/tests using the old devel-lake name."
  workspace-path-pattern)

(def url-pattern
  #"https?://[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%-]+")

(defn trim-mention-token
  [value]
  (-> (str value)
      (str/replace #"^[\s`'\"\(\[\{<]+" "")
      (str/replace #"[\s`'\"\)\]\}>:;,.!?]+$" "")))

(defn normalize-web-url
  [value]
  (let [raw (trim-mention-token value)]
    (if (str/blank? raw)
      nil
      (try
        (let [parsed (js/URL. raw)]
          (set! (.-hash parsed) "")
          (when (str/blank? (.-pathname parsed))
            (set! (.-pathname parsed) "/"))
          (.toString parsed))
        (catch :default _ nil)))))



(defn extract-mentioned-urls
  [text]
  (->> (re-seq url-pattern (or text ""))
       (map normalize-web-url)
       (remove nil?)
       distinct
       vec))



(def ^:private known-extensionless-files
  #{"Dockerfile" "Makefile" "Justfile" "Brewfile" "Procfile" "Caddyfile"})

(defn- basename
  [path]
  (some-> (str path) (str/split #"/") last))

(defn- normalize-workspace-path
  [path]
  (normalize-relative-path path))

(defn- workspace-project-name
  []
  (or (:project-name (cfg)) "workspace"))

(defn- likely-file-path?
  "Heuristic: treat workspace mentions as file nodes when the token looks like a file.

  Everything else is treated as a directory structural node (<project>:dir:*)."
  [path]
  (let [b (basename path)]
    (or (contains? known-extensionless-files b)
        (str/starts-with? b ".")
        (re-find #"\." b))))

(defn- workspace-target-node
  [path]
  (let [path (normalize-workspace-path path)
        project-name (workspace-project-name)]
    (when-not (str/blank? path)
      (if (likely-file-path? path)
        {:path path
         :target_kind "file"
         :target_node_id (str project-name ":file:" path)}
        {:path path
         :target_kind "dir"
         :target_node_id (str project-name ":dir:" path)}))))

(defn extract-mentioned-workspace-paths
  [text]
  (->> (re-seq workspace-path-pattern (or text ""))
       (map second)
       (map workspace-target-node)
       (remove nil?)
       distinct
       vec))

(defn extract-mentioned-devel-paths
  "Backward-compatible alias for the workspace path extractor."
  [text]
  (extract-mentioned-workspace-paths text))

(defn session-visible?
  [ctx rows]
  (cond
    (nil? ctx) true
    (system-admin? ctx) true
    :else
    (let [extras (map row-extra-map rows)
          org-ids (into #{} (keep #(some-> % :org_id str not-empty)) extras)
          membership-ids (into #{} (keep #(some-> % :membership_id str not-empty)) extras)
          user-ids (into #{} (keep #(some-> % :user_id str not-empty)) extras)
          same-org? (contains? org-ids (str (ctx-org-id ctx)))]
      (cond
        ;; Legacy OpenPlanner sessions may not have org_id/membership_id/user_id
        ;; embedded in :extra. If the caller already has cross-session memory
        ;; permission, allow these sessions to be visible.
        (empty? org-ids) (ctx-permitted? ctx "agent.memory.cross_session")
        (not same-org?) false
        (ctx-permitted? ctx "agent.memory.cross_session") true
        :else (or (contains? membership-ids (str (ctx-membership-id ctx)))
                  (contains? user-ids (str (ctx-user-id ctx))))))))

(defn session-extra-value-from-rows
  [rows keys]
  (some (fn [row]
          (let [extra (row-extra-map row)]
            (some (fn [k]
                    (some-> (get extra k)
                            str
                            str/trim
                            not-empty))
                  keys)))
        (reverse (vec (or rows [])))))

(defn session-contract-id-from-rows
  [rows]
  (session-extra-value-from-rows rows [:contract_id :contract-id :contractId]))

(defn session-contract-actors-from-rows
  [rows]
  (some (fn [row]
          (let [extra (row-extra-map row)
                actors (actor-scope/normalize-actor-claims
                        (or (:contract_actors extra)
                            (:contract-actors extra)
                            (:contractActors extra)))]
            (when (seq actors)
              actors)))
        (reverse (vec (or rows [])))))

(defn session-actor-id-from-rows
  [rows]
  (session-extra-value-from-rows rows [:actor_id :actor-id :actorId]))

(defn session-sub-agent-id-from-rows
  [rows]
  (session-extra-value-from-rows rows [:sub_agent_id :sub-agent-id :subAgentId]))

(defn session-parent-agent-id-from-rows
  [rows]
  (session-extra-value-from-rows rows [:parent_agent_id :parent-agent-id :parentAgentId]))

(defn session-parent-run-id-from-rows
  [rows]
  (session-extra-value-from-rows rows [:parent_run_id :parent-run-id :parentRunId]))

(defn session-spawn-kind-from-rows
  [rows]
  (session-extra-value-from-rows rows [:spawn_kind :spawn-kind :spawnKind]))

(defn session-trigger-id-from-rows
  [rows]
  (session-extra-value-from-rows rows [:trigger_id :trigger-id :triggerId]))

(defn session-event-type-from-rows
  [rows]
  (session-extra-value-from-rows rows [:event_type :event-type :eventType :trigger_event_type :trigger-event-type :triggerEventType]))

(defn session-event-types-from-rows
  [rows]
  (some (fn [row]
          (let [extra (row-extra-map row)
                values (or (:event_types extra)
                           (:event-types extra)
                           (:eventTypes extra))]
            (when (sequential? values)
              (->> values
                   (map str)
                   (remove str/blank?)
                   distinct
                   vec))))
        (reverse (vec (or rows [])))))

(defn session-event-id-from-rows
  [rows]
  (session-extra-value-from-rows rows [:event_id :event-id :eventId]))

(defn session-event-scope-id-from-rows
  [rows]
  (session-extra-value-from-rows rows [:event_scope_id :event-scope-id :eventScopeId]))

(defn session-schedule-id-from-rows
  [rows]
  (session-extra-value-from-rows rows [:schedule_id :schedule-id :scheduleId]))

(defn session-summary-scope-from-rows
  [rows]
  (let [contract-id (session-contract-id-from-rows rows)
        actor-id (session-actor-id-from-rows rows)
        contract-actors (session-contract-actors-from-rows rows)
        wire-actors (when (seq contract-actors)
                      (actor-scope/actor-claims->wire contract-actors))
        sub-agent-id (session-sub-agent-id-from-rows rows)
        parent-agent-id (session-parent-agent-id-from-rows rows)
        parent-run-id (session-parent-run-id-from-rows rows)
        spawn-kind (session-spawn-kind-from-rows rows)
        trigger-id (session-trigger-id-from-rows rows)
        event-type (session-event-type-from-rows rows)
        event-types (session-event-types-from-rows rows)
        event-id (session-event-id-from-rows rows)
        event-scope-id (session-event-scope-id-from-rows rows)
        schedule-id (session-schedule-id-from-rows rows)]
    (cond-> {}
      contract-id (assoc :contract_id contract-id)
      actor-id (assoc :actor_id actor-id)
      (seq wire-actors) (assoc :contract_actors wire-actors)
      sub-agent-id (assoc :sub_agent_id sub-agent-id)
      parent-agent-id (assoc :parent_agent_id parent-agent-id)
      parent-run-id (assoc :parent_run_id parent-run-id)
      spawn-kind (assoc :spawn_kind spawn-kind)
      trigger-id (assoc :trigger_id trigger-id)
      event-type (assoc :event_type event-type)
      (seq event-types) (assoc :event_types event-types)
      event-id (assoc :event_id event-id)
      event-scope-id (assoc :event_scope_id event-scope-id)
      schedule-id (assoc :schedule_id schedule-id))))

(defn session-actor-claims-from-rows
  [config rows]
  (let [legacy-fallback #{actor-scope/legacy-chat-actor-id}]
    (or (some-> (session-actor-id-from-rows rows)
                vector
                actor-scope/normalize-actor-claims)
        (session-contract-actors-from-rows rows)
        (some-> (session-contract-id-from-rows rows)
                (tooling/resolve-agent-contract config)
                :contract-actors
                actor-scope/normalize-actor-claims)
        legacy-fallback)))

(defn- actor-claim-includes?
  [actors actor-id]
  (let [wanted (actor-scope/normalize-actor-claim actor-id)]
    (and wanted
         (contains? (actor-scope/normalize-actor-claims actors) wanted))))

(defn session-matches-page-actor-filter?
  [config rows include-actor-id exclude-actor-ids]
  (let [include-actor-id (some-> include-actor-id str str/trim not-empty)
        exclude-actor-ids (->> (or exclude-actor-ids [])
                               (keep #(some-> % str str/trim not-empty))
                               distinct
                               vec)
        actors (session-actor-claims-from-rows config rows)]
    (and (or (str/blank? (str (or include-actor-id "")))
             (actor-scope/actor-allowed? actors include-actor-id))
         (not-any? #(actor-scope/actor-allowed? actors %) exclude-actor-ids))))

(defn session-matches-contract-filter?
  [config rows contract-id]
  (let [target (some-> contract-id str str/trim not-empty)]
    (if-not target
      true
      (let [actors (session-actor-claims-from-rows config rows)
            contract-actors (session-contract-actors-from-rows rows)]
        (or (= target (session-contract-id-from-rows rows))
            (= target (session-sub-agent-id-from-rows rows))
            (= target (session-parent-agent-id-from-rows rows))
            (= target (session-actor-id-from-rows rows))
            (actor-claim-includes? contract-actors target)
            (actor-claim-includes? actors target))))))

(defn session-visible-for-page-actor?
  [config rows page-actor-id]
  (session-matches-page-actor-filter? config rows page-actor-id []))

(defn- fetch-openplanner-session-mode-rows!
  [config session-id mode opts]
  (-> (openplanner-client/session! (openplanner-client/client config)
                                   session-id
                                   (merge {:project (:session-project-name config)
                                           :mode mode}
                                          opts))
      (.then (fn [body]
               (vec (or (:rows body) []))))))

(defn fetch-openplanner-session-rows!
  [config session-id]
  (fetch-openplanner-session-mode-rows! config session-id "full" {}))

(defn fetch-openplanner-session-visibility-rows!
  [config session-id]
  (fetch-openplanner-session-mode-rows! config session-id "visibility" {:limit 1}))

(defn authorized-session-ids!
  [config ctx session-ids]
  (let [session-ids (->> session-ids
                         (map str)
                         (remove str/blank?)
                         distinct
                         vec)]
    (if (or (nil? ctx) (system-admin? ctx) (empty? session-ids))
      (js/Promise.resolve (set session-ids))
      (.then (promise/all-vec
              (map (fn [session-id]
                     (.then (fetch-openplanner-session-rows! config session-id)
                            (fn [rows]
                              {:session session-id
                               :allowed (session-visible? ctx rows)})
                            (fn [_]
                              {:session session-id
                               :allowed false})))
                   session-ids))
             (fn [results]
               (->> results
                    (filter :allowed)
                    (map :session)
                    set))))))

(defn hit-session-id
  [hit]
  (or (:session hit)
      (get-in hit [:metadata :session])
      (get-in hit [:extra :session])))

(defn- hit-text
  [hit]
  (str (or (:snippet hit)
           (:document hit)
           (:text hit)
           (get-in hit [:metadata :text])
           "")))

(defn- reasoning-hit?
  [hit]
  (let [metadata (or (:metadata hit) hit {})
        kind (str (or (:kind hit) (:kind metadata) ""))
        role (str (or (:role hit) (:role metadata) ""))
        id (str (or (:id hit) (:parent_id metadata) (:parent-id metadata) ""))]
    (or (= kind "knoxx.reasoning")
        (= kind "reasoning")
        (= (:node_type metadata) "reasoning")
        (= (:node-type metadata) "reasoning")
        (= role "reasoning")
        (str/includes? id ":reasoning"))))

(defn- operational-failure-hit?
  [hit]
  (let [text (hit-text hit)]
    (boolean
     (or (re-find #"(?i)\b403\s+No upstream providers are allowed\b" text)
         (re-find #"(?i)\bNo upstream providers are allowed for this tenant and request\b" text)
         (re-find #"(?i)\bprovider_not_allowed\b" text)))))

(defn filter-authorized-memory-hits!
  [config ctx hits]
  (let [hits (vec hits)
        session-ids (map hit-session-id hits)]
    (-> (authorized-session-ids! config ctx session-ids)
        (.then (fn [allowed]
                 (->> hits
                      (filter (fn [hit]
                                (and (contains? allowed (str (or (hit-session-id hit) "")))
                                     (not (reasoning-hit? hit))
                                     (not (operational-failure-hit? hit)))))
                      vec))))))
