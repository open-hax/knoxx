(ns knoxx.backend.domain.discord.discord-reaction-labels
  "Weak reaction-label ingestion for Discord messages.

   Any Discord message that receives a reaction is explicitly added to the
   OpenPlanner corpus. The reaction is also written as a weak quality claim when
   it has a reserved meaning: ✅ = good output, ❌ = bad output."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.discord.gateway :as dg]
            [knoxx.backend.infra.http :as http]
            [knoxx.backend.domain.time :as time]))

(defonce ^:private reaction-unsubscribe* (atom nil))

(defn- js-get
  [obj key]
  (when obj (aget obj key)))

(defn- discord-record-id
  [channel-id message-id]
  (str "discord:message:" channel-id ":" message-id))

(defn- quality-from-emoji
  [emoji]
  (case (str/trim (str emoji))
    "✅" "good"
    "☑️" "good"
    "✔️" "good"
    "✔" "good"
    "❌" "bad"
    "✖️" "bad"
    "✖" "bad"
    "❎" "bad"
    nil))

(defn- message->openplanner-event
  [config message emoji user-id]
  (let [channel-id (str (or (js-get message "channelId") ""))
        message-id (str (or (js-get message "id") ""))
        author-id (str (or (js-get message "authorId") ""))
        record-id (discord-record-id channel-id message-id)
        content (str (or (js-get message "content") ""))
        ts (or (js-get message "timestamp") (time/now-iso))
        quality (quality-from-emoji emoji)]
    {:schema "openplanner.event.v1"
     :id record-id
     :ts ts
     :source "discord"
     :kind "discord.message"
     :source_ref {:project (:session-project-name config)
                  :session channel-id
                  :message message-id}
     :text content
     :meta {:role "user"
            :author author-id
            :tags ["discord" "message" "reaction-corpus"]}
     :extra (cond-> {:channel_id channel-id
                     :message_id message-id
                     :author_id author-id
                     :author_username (str (or (js-get message "authorUsername") "unknown"))
                     :author_is_bot (boolean (js-get message "authorIsBot"))
                     :reaction_ingested true
                     :reaction_emoji emoji
                     :reaction_user_id user-id
                     :openplanner_labels {:claim_system "weak-reaction-v1"
                                          :reaction_emojis [emoji]
                                          :labels [(if quality (str "quality:" quality) (str "reaction:" emoji))]
                                          :updated_at (time/now-iso)}}
              quality (assoc-in [:openplanner_labels :quality] quality)
              (= quality "good") (assoc-in [:openplanner_labels :explicit_meaning] "good output")
              (= quality "bad") (assoc-in [:openplanner_labels :explicit_meaning] "bad output"))}))

(defn ingest-reaction!
  [config reaction]
  (let [message (js-get reaction "message")
        emoji (str/trim (str (or (js-get reaction "emoji") "")))
        user-id (str (or (js-get reaction "userId") ""))
        channel-id (str (or (js-get reaction "channelId") ""))
        message-id (str (or (js-get reaction "messageId") ""))]
    (if (or (str/blank? emoji) (str/blank? channel-id) (str/blank? message-id) (not (http/openplanner-enabled? config)))
      (js/Promise.resolve {:ok false :skipped true})
      (let [record-id (discord-record-id channel-id message-id)
            event (message->openplanner-event config message emoji user-id)]
        (-> (http/openplanner-request! config "POST" "/v1/events" {:events [event]})
            (.then (fn [_]
                     (http/openplanner-request! config "POST" (str "/v1/labels/records/" (js/encodeURIComponent record-id) "/reaction")
                                                {:emoji emoji
                                                 :source "discord-gateway-reaction"
                                                 :user_id user-id})))
            (.catch (fn [err]
                      (.warn js/console "[discord-reaction-labels] failed to ingest reaction" err)
                      {:ok false :error (.-message err)})))))))

(defn bind!
  [config]
  (when-let [unsubscribe @reaction-unsubscribe*]
    (unsubscribe)
    (reset! reaction-unsubscribe* nil))
  (when-let [manager (dg/gateway-manager)]
    (when (fn? (aget manager "onReaction"))
      (reset! reaction-unsubscribe*
              (.onReaction manager
                           (fn [mapped _raw-reaction _user]
                             (ingest-reaction! config mapped)))))))
