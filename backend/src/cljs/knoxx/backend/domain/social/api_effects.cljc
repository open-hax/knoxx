(ns knoxx.backend.domain.social.api-effects
  "Describe direct API effects independently of HTTP verbs and execution grants."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.social.discord-effects :as discord]
            [knoxx.backend.domain.social.bluesky-effects :as bluesky]
            [knoxx.backend.law.platform-api-effects :as law]))

(defn- mutation
  [operation]
  (let [id (get operation "id") method (get operation "method")]
    (cond
      (= "GET" method) :read
      (= "DELETE" method) :delete
      (= "PATCH" method) :update
      (= "PUT" method) :replace
      (re-find #"(?:^|[.])(?:delete|remove|revoke|deactivate|prune)|^bulk_delete" id) :delete
      (re-find #"(?:^|[.])create|^bulk_add" id) :create
      :else :execute)))

(defn- base-effect
  [operation]
  (let [id (get operation "id") kind (get operation "kind")]
    (cond
      (= id "com.atproto.server.getServiceAuth") :credential
      (= kind "query") :read
      (= kind "subscription") :subscription
      (= kind "record") (get bluesky/records id :unclassified)
      (not= kind "procedure") :definition
      (= "discord" (get operation "platform")) (get discord/operations id :unclassified)
      :else (get bluesky/procedures id :unclassified))))

(defn- audience
  [operation effect]
  (let [id (get operation "id")]
    (cond
      (contains? #{:read :definition} effect) :none
      (= "record" (get operation "kind")) :public
      (= effect :conversation) :conversation
      (str/starts-with? id "chat.bsky.") :conversation
      (or (str/starts-with? id "tools.ozone.") (str/starts-with? id "com.atproto.admin.")) :operator
      (= "discord" (get operation "platform")) :channel-or-resource
      (contains? #{:binary :repo-import :repository} effect) :public
      :else :account)))

(defn- separate-auth?
  [operation]
  (boolean
   (or (re-find #"\{(?:webhook|interaction)_token\}" (or (get operation "route") ""))
       (some #(re-find #"(?:^|_)(?:token|password|secret|authorization)(?:$|_)"
                       (-> % (str/replace #"([a-z])([A-Z])" "$1_$2") str/lower-case))
             (concat (get operation "parameterNames") (get operation "requestFields"))))))

(defn operation-effects
  "Conservative discovery metadata. A body-dependent operation must be expanded
   before admission; this descriptor never grants permission to execute it."
  [operation]
  (let [effect (base-effect operation) id (get operation "id") change (mutation operation)
        dynamic? (or (contains? #{:repository :repo-import} effect)
                     (contains? #{"create_thread" "create_interaction_response"
                                  "chat.bsky.convo.sendMessageBatch"} id))]
    (law/assert-effect!
     {:effect effect :audience (audience operation effect)
     :mutation (if (= effect :read) :read change)
     :publication? (contains? #{:publish :edit :amplify :repo-import :repository} effect)
     :credential? (= effect :credential)
     :destructive? (or (contains? #{:delete :replace :update} change) (= effect :repo-import))
     :separate-auth? (separate-auth? operation)
     :body-dependent? (boolean dynamic?)
     :inspection-required (if (= effect :repo-import) [:repository-records-in-car] [])
     :classification :derived-from-pinned-operation})))

(defn- record-action
  [collection change path]
  (let [effect (get bluesky/records collection :unclassified)]
    {:effect (if (= change :delete) :delete effect) :collection collection
     :audience :public :mutation change :path path
     :publication? (and (not= change :delete) (contains? #{:publish :amplify :unclassified} effect))
     :credential? false :destructive? (contains? #{:delete :replace} change)
     :separate-auth? false :body-dependent? false :inspection-required []
     :classification :derived-from-pinned-record-collection}))

(defn- refuse!
  [message]
  (throw (ex-info message {:code "platform_effect_unresolved" :phase :preflight})))

(defn- repository-actions
  [operation body]
  (let [id (get operation "id") collection (get body "collection")]
    (if (= id "com.atproto.repo.applyWrites")
      (let [writes (get body "writes")]
        (when-not (vector? writes) (refuse! "Repository batch effects require the actual writes array"))
        (mapv (fn [index write]
                (let [change (case (get write "$type")
                               "com.atproto.repo.applyWrites#create" :create
                               "com.atproto.repo.applyWrites#update" :replace
                               "com.atproto.repo.applyWrites#delete" :delete
                               (refuse! "Unknown repository batch operation; no effect admission is possible"))]
                  (when-not (string? (get write "collection"))
                    (refuse! "Every repository batch item requires its actual collection"))
                  (record-action (get write "collection") change ["writes" index])))
              (range (count writes)) writes))
      (do
        (when-not (string? collection) (refuse! "Record effects require the actual repository collection"))
        [(record-action collection
                        (case id "com.atproto.repo.createRecord" :create
                                 "com.atproto.repo.putRecord" :replace
                                 "com.atproto.repo.deleteRecord" :delete)
                        [])]))))

(defn- interaction-actions
  [base body]
  (let [[effect publishes?] (case (get body "type")
                              (4 5) [:publish true]
                              7 [:edit true]
                              9 [:conversation true]
                              (1 6 8 12) [:conversation false]
                              13 [:account false]
                              (refuse! "Unknown interaction callback type; inspect its pinned schema"))]
    [(assoc base :effect effect :publication? publishes? :body-dependent? false :path []
            :mutation (case effect :publish :create :edit :update :execute)
            :destructive? (= effect :edit))]))

(defn- chat-batch-actions
  [base body]
  (let [items (get body "items")]
    (when-not (vector? items) (refuse! "Conversation batch effects require the actual items array"))
    (mapv (fn [index item]
            (when-not (string? (get item "convoId"))
              (refuse! "Every conversation batch item requires its actual conversation ID"))
            (assoc base :effect :conversation :target (get item "convoId") :mutation :create
                   :body-dependent? false :path ["items" index]))
          (range (count items)) items)))

(defn- expand-actions
  [operation body base]
  (let [id (get operation "id")]
    (cond
      (= :repository (:effect base)) (repository-actions operation body)
      (= "create_interaction_response" id) (interaction-actions base body)
      (= "chat.bsky.convo.sendMessageBatch" id) (chat-batch-actions base body)
      (= "create_thread" id)
      (cond-> [(assoc base :body-dependent? false :path [])]
        (contains? body "message")
        (conj (assoc base :effect :publish :publication? true :body-dependent? false :path ["message"])))
      (= "chat.bsky.convo.sendMessage" id) [(assoc base :effect :conversation :mutation :create :path [])]
      :else [(assoc base :path [])])))

(defn request-effects
  "Expand actual record/chat batches, callback kinds and forum starter messages.
   Body keys are JSON strings. Payload validation and actor authorization remain
   independent obligations; validate=false cannot hide a record's effect."
  [operation body]
  (let [base (operation-effects operation) id (get operation "id")
        actions (expand-actions operation body base)]
    (law/assert-analysis!
     {:operation id :platform (get operation "platform") :actions actions
     :complete? (and (not-any? #(= :unclassified (:effect %)) actions)
                     (not-any? :body-dependent? actions)
                     (not-any? #(seq (:inspection-required %)) actions))
     :callable? (contains? #{"query" "procedure"} (get operation "kind"))
     :classification "Effect analysis only; not payload validation, actor permission or a platform receipt."})))
