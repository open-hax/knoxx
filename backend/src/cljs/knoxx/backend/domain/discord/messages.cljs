(ns knoxx.backend.domain.discord.messages
  "Pure Discord message projections and label vocabulary."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.label.quality :as quality-labels]))

(defn- discord-attachments
  [message]
  (->> (or (:attachments message) [])
       (mapv (fn [attachment]
               {:id (or (:id attachment) "")
                :filename (or (:filename attachment) "")
                :contentType (or (:content_type attachment) (:contentType attachment))
                :size (or (:size attachment) 0)
                :url (or (:url attachment) "")}))))

(defn- discord-embeds
  [message]
  (->> (or (:embeds message) [])
       (mapv (fn [embed]
               {:title (:title embed)
                :description (:description embed)
                :url (:url embed)}))))

(defn discord-message->map
  "Project discord-message->map."
  [message]
  (let [author (or (:author message) {})]
    {:id (or (:id message) "")
     :channelId (or (:channel_id message) (:channelId message) "")
     :content (or (:content message) "")
     :authorId (or (:id author) "")
     :authorUsername (or (:username author) "unknown")
     :authorIsBot (boolean (:bot author))
     :timestamp (or (:timestamp message) "")
   :attachments (discord-attachments message)
     :embeds (discord-embeds message)}))

(defn- discord-message-line
  [message]
  (let [content (str/trim (str (or (:content message) "")))
        attachments (:attachments message)
        attachment-text (when (seq attachments)
                          (str " attachments="
                               (str/join ", "
                                         (map (fn [attachment]
                                                (str (:filename attachment)
                                                     (when-let [url (:url attachment)]
                                                       (str " <" url ">"))))
                                              attachments))))
        embeds (:embeds message)
        embed-text (when (seq embeds)
                     (str " embeds="
                          (str/join ", "
                                    (map (fn [embed]
                                           (str (or (:title embed) "embed")
                                                (when-let [url (:url embed)]
                                                  (str " <" url ">"))))
                                         embeds))))]
    (str "<" (:authorUsername message) " (id:" (:authorId message) ")> "
         (if (str/blank? content) "[no text]" content)
         (or attachment-text "")
         (or embed-text ""))))

(defn discord-messages-text
  "Project discord-messages-text."
  [heading messages]
  (str heading
       (if (seq messages)
         (str "\n\n" (str/join "\n\n" (map discord-message-line messages)))
         "\n\nNo messages found.")))

(defn discord-record-id
  "Project discord-record-id."
  [message]
  (str "discord:message:" (:channelId message) ":" (:id message)))

(defn discord-message-quality
  "Project discord-message-quality."
  [message]
  (quality-labels/quality-label message))

(defn drop-bad-discord-messages
  "Project drop-bad-discord-messages."
  [messages]
  (quality-labels/drop-bad messages))

(defn label-for-record-id
  "Project label-for-record-id."
  [labels record-id]
  (or (get labels record-id)
      (get labels (keyword record-id))
      {}))

(defn discord-message-payload
  "Project discord-message-payload."
  [chunk reply-to state]
  (cond-> {:content chunk}
    (and reply-to (nil? (:messageId state)))
    (assoc :message_reference {:message_id reply-to})))

(defn text-channel-type?
  "Project text-channel-type?."
  [channel]
  (contains? #{0 5 11 12} (:type channel)))

