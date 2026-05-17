(ns knoxx.backend.core-memory
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [system-admin? ctx-org-id ctx-membership-id ctx-user-id ctx-permitted?]]
            [knoxx.backend.document-state :refer [normalize-relative-path]]
            [knoxx.backend.http :as backend-http :refer [js-array-seq]]
            [knoxx.backend.runtime.actor-scope :as actor-scope]
            [knoxx.backend.runtime.config :refer [cfg]]
            [knoxx.backend.tooling :as tooling]))

(defn parse-json-object
  [value]
  (cond
    (map? value) value
    (string? value) (try
                      (js->clj (.parse js/JSON value) :keywordize-keys true)
                      (catch :default _ nil))
    :else nil))

(defn row-extra-map
  [row]
  (or (parse-json-object (:extra row)) {}))

(def devel-path-pattern
  #"((?:orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.ημ)/[A-Za-z0-9._~:/+-]+)")

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

(defn normalize-devel-path
  [value]
  (let [trimmed (trim-mention-token value)
        no-prefix (cond
                    (str/starts-with? trimmed "/app/workspace/devel/") (subs trimmed (count "/app/workspace/devel/"))
                    (str/starts-with? trimmed (:workspace-root (cfg))) (subs trimmed (inc (count (:workspace-root (cfg)))))
                    :else trimmed)
        normalized (normalize-relative-path no-prefix)]
    (when (and (not (str/blank? normalized))
               (re-find #"^(orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.ημ)/" normalized))
      normalized)))

(defn extract-mentioned-urls
  [text]
  (->> (re-seq url-pattern (or text ""))
       (map normalize-web-url)
       (remove nil?)
       distinct
       vec))

(defn- basename
  [path]
  (let [s (-> (str path)
              (str/replace #"\\\\" "/")
              (str/replace #"/+" "/"))
        parts (->> (str/split s #"/")
                   (remove str/blank?))]
    (or (last parts) s)))

(def ^:private known-extensionless-files
  #{"Dockerfile" "Makefile" "Justfile" "Brewfile" "Procfile" "Caddyfile"})

(defn- likely-file-path?
  "Heuristic: treat devel mentions as file nodes when the token looks like a file.

  Everything else is treated as a directory structural node (devel:dir:*)."
  [path]
  (let [b (basename path)]
    (or (contains? known-extensionless-files b)
        (str/starts-with? b ".")
        (re-find #"\\." b))))

(defn- devel-target-node
  [path]
  (let [path (normalize-devel-path path)]
    (when-not (str/blank? path)
      (if (likely-file-path? path)
        {:path path
         :target_kind "file"
         :target_node_id (str "devel:file:" path)}
        {:path path
         :target_kind "dir"
         :target_node_id (str "devel:dir:" path)}))))

(defn extract-mentioned-devel-paths
  [text]
  (->> (re-seq devel-path-pattern (or text ""))
       (map second)
       (map devel-target-node)
       (remove nil?)
       distinct
       vec))

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

(defn session-contract-id-from-rows
  [rows]
  (some (fn [row]
          (some-> (or (:contract_id (row-extra-map row))
                      (:contract-id (row-extra-map row)))
                  str
                  str/trim
                  not-empty))
        (reverse (vec (or rows [])))))

(defn session-contract-actors-from-rows
  [rows]
  (some (fn [row]
          (let [extra (row-extra-map row)
                actors (actor-scope/normalize-actor-claims
                        (or (:contract_actors extra)
                            (:contract-actors extra)))]
            (when (seq actors)
              actors)))
        (reverse (vec (or rows [])))))

(defn session-actor-id-from-rows
  [rows]
  (some (fn [row]
          (some-> (or (:actor_id (row-extra-map row))
                      (:actor-id (row-extra-map row))
                      (:actorId (row-extra-map row)))
                  str
                  str/trim
                  not-empty))
        (reverse (vec (or rows [])))))

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

(defn session-visible-for-page-actor?
  [config rows page-actor-id]
  (session-matches-page-actor-filter? config rows page-actor-id []))

(defn fetch-openplanner-session-rows!
  [config session-id]
  (-> (backend-http/openplanner-request! config "GET" (str "/v1/sessions/" (js/encodeURIComponent (str session-id))
                                               "?project=" (js/encodeURIComponent (:session-project-name config))
                                               "&mode=full"))
      (.then (fn [body]
               (vec (or (:rows body) []))))))

(defn authorized-session-ids!
  [config ctx session-ids]
  (let [session-ids (->> session-ids
                         (map str)
                         (remove str/blank?)
                         distinct
                         vec)]
    (if (or (nil? ctx) (system-admin? ctx) (empty? session-ids))
      (js/Promise.resolve (set session-ids))
      (.then (js/Promise.all
              (clj->js
               (map (fn [session-id]
                      (.then (fetch-openplanner-session-rows! config session-id)
                             (fn [rows]
                               {:session session-id
                                :allowed (session-visible? ctx rows)})
                             (fn [_]
                               {:session session-id
                                :allowed false})))
                    session-ids)))
             (fn [results]
               (->> (js-array-seq results)
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
