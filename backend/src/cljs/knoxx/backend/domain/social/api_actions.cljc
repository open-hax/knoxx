(ns knoxx.backend.domain.social.api-actions
  "Canonical native/API intents and audience-specific publication projections."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.social.action-admission :as admission]
            [knoxx.backend.law.platform-api-action :as law]))

(defn- source-text
  [payload]
  (str/join "\n" (filter string? (tree-seq coll? seq payload))))

(defn- record-value
  [plan effect payload]
  (when (:collection effect)
    (get payload (if (= "com.atproto.repo.applyWrites" (:operation plan)) "value" "record"))))

(defn- target
  [plan effect payload actor-id]
  (or (:target effect) (get-in plan [:path "channel_id"])
      (get payload "convoId") (get-in plan [:path "guild_id"])
      (get-in plan [:path "webhook_id"]) actor-id))

(defn- discord-message
  [plan effect payload]
  (when (and (= "discord" (:platform plan)) (= :publish (:effect effect))
              (get-in plan [:path "channel_id"]))
    (let [params {:channel_id (get-in plan [:path "channel_id"])
                  :text (get payload "content" "")
                  :reply_to (get-in payload ["message_reference" "message_id"])}
          plain (admission/native-intent "discord_send" params nil)
          reference-extra (dissoc (get payload "message_reference") "message_id")
          extras (cond-> (dissoc payload "content" "message_reference")
                   (seq reference-extra) (assoc "message_reference" reference-extra))]
      (cond-> plain (seq extras) (assoc-in [:arguments :api-fields] extras)))))

(defn- bluesky-post
  [plan effect record actor-id]
  (when (and (= "bluesky" (:platform plan)) (= :publish (:effect effect))
              (= "app.bsky.feed.post" (:collection effect)))
    (let [plain (admission/native-intent "bluesky_publish"
                                        {:text (get record "text" "") :replyTo (get-in record ["reply" "parent" "uri"] "")}
                                        actor-id)
          extras (dissoc record "$type" "text" "createdAt" "reply")
          record-target (select-keys (if (= "com.atproto.repo.applyWrites" (:operation plan))
                                       (get-in (:body plan) (:path effect)) (:body plan))
                                     ["rkey" "swapRecord" "swapCommit"])]
      (cond-> plain
        (seq extras) (assoc-in [:arguments :api-fields] extras)
        (or (seq record-target) (= :replace (:mutation effect)))
        (assoc-in [:arguments :api-record-target] (assoc record-target "mutation" (name (:mutation effect))))))))

(defn- chat-intent
  [plan payload actor-id]
(when (contains? #{"chat.bsky.convo.sendMessage" "chat.bsky.convo.sendMessageBatch"} (:operation plan))
        (let [message (get payload "message")
              plain (admission/native-intent "bluesky_chat_send"
                                              {:convoId (get payload "convoId") :text (get message "text")
                                               :replyToMessageId (get-in message ["replyTo" "messageId"])} actor-id)
              extra (dissoc message "$type" "text" "replyTo")]
          (cond-> plain (seq extra) (assoc-in [:arguments :api-fields] extra)))))

(defn- convenience-intent
  [plan effect payload record actor-id]
  (or (discord-message plan effect payload)
      (bluesky-post plan effect record actor-id)
      (when (and (not= :delete (:mutation effect))
                 (contains? #{"app.bsky.feed.repost" "app.bsky.feed.like"} (:collection effect)))
        (admission/native-intent (if (= :amplify (:effect effect)) "bluesky_repost" "bluesky_like")
                                 {:uri (get-in record ["subject" "uri"])} actor-id))
      (when (and (= :create (:mutation effect))
                 (= "app.bsky.graph.follow" (:collection effect)))
        (admission/native-intent "bluesky_follow" {:actor (get record "subject")} actor-id))
      (chat-intent plan payload actor-id)
      (case (:operation plan)
        "add_my_message_reaction"
        (admission/native-intent "discord_react"
                                 {:channel_id (get-in plan [:path "channel_id"])
                                  :message_id (get-in plan [:path "message_id"])
                                  :emoji (get-in plan [:path "emoji_name"])} actor-id)
        ("create_thread_from_message" "create_thread")
        (when (= :thread (:effect effect))
          (let [plain (admission/native-intent "discord_thread_create"
                                   {:channel_id (get-in plan [:path "channel_id"])
                                    :message_id (get-in plan [:path "message_id"])
                                    :name (get payload "name")
                                    :auto_archive_duration (get payload "auto_archive_duration")} actor-id)
                extra (dissoc payload "name" "auto_archive_duration")]
            (cond-> plain (seq extra) (assoc-in [:arguments :api-fields] extra))))
        nil)))

(defn- response-kind
  [plan effect]
  (cond
    (:collection effect) :bluesky-record
    (and (= "discord" (:platform plan)) (contains? #{:publish :edit} (:effect effect))) :discord-message
    (and (= "discord" (:platform plan)) (= :thread (:effect effect))) :discord-thread
    (= "add_my_message_reaction" (:operation plan)) :discord-reaction
    (contains? #{"chat.bsky.convo.sendMessage" "chat.bsky.convo.sendMessageBatch"} (:operation plan)) :bluesky-chat
    :else :operation))

(defn- response-path
  [plan effect]
  (case (:operation plan)
    "com.atproto.repo.applyWrites" ["results" (second (:path effect))]
    "chat.bsky.convo.sendMessageBatch" ["items" (second (:path effect))]
    "create_thread" (if (= :publish (:effect effect)) ["message"] [])
    []))

(defn- descriptor
  [plan effect actor-id]
  (let [payload (if (seq (:path effect)) (get-in (:body plan) (:path effect)) (:body plan))
        record (record-value plan effect payload)
        audience (target plan effect payload actor-id)
        intent (or (convenience-intent plan effect payload record actor-id)
                   {:platform (:platform plan) :effect (:effect effect) :target audience
                    :arguments {:operation (:operation plan) :path (:path plan) :query (:query plan) :body payload}})
        text (or (get record "text") (get payload "content") (get-in payload ["message" "text"]) "")
        reply (or (get-in record ["reply" "parent" "uri"])
                  (when (= :amplify (:effect effect)) (get-in record ["subject" "uri"])) "")]
    {:operation (:operation plan) :intent intent :effect effect :payload payload :record record
     :kind (response-kind plan effect) :response-path (response-path plan effect)
     :guard-params (cond-> {:text text :source-text (source-text (or record payload))}
                     (= "discord" (:platform plan)) (assoc :channel_id audience)
                     (= "bluesky" (:platform plan)) (assoc :replyTo reply))}))

(defn planned-actions
  "Project every actual effect; read-only POST operations reserve no write.
   Body-derived fields are data and cannot grant or disable a delivery policy."
  [{:keys [plan effects]} actor-id]
  (law/assert-descriptors!
   (mapv #(descriptor plan % actor-id) (remove #(= :read (:effect %)) (:actions effects)))))

(defn post-records
  "Only repository writes to feed.post carry authored post/reply invariants."
  [plan]
  (let [body (:body plan)]
    (case (:operation plan)
      ("com.atproto.repo.createRecord" "com.atproto.repo.putRecord")
      (if (= "app.bsky.feed.post" (get body "collection")) [(get body "record")] [])
      "com.atproto.repo.applyWrites"
      (->> (get body "writes")
           (filter #(and (= "app.bsky.feed.post" (get % "collection"))
                         (not= "com.atproto.repo.applyWrites#delete" (get % "$type"))))
           (mapv #(get % "value")))
      [])))
