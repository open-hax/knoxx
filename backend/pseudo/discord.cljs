(ns knoxx.backend.tools.discord
  "Discord API wrappers and tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.discord-gateway :as dg]
            [knoxx.backend.http :as backend-http :refer [js-array-seq]]
            [knoxx.backend.text :refer [tool-text-result]]

            ["@sinclair/typebox" :refer [Type]]
            ["@resvg/resvg-js" :default resvg-mod]
            ;; ["@resvg/resvg-js" :as resvg-mod]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional live-config ->params]]))


(defn create-tool-obj [ name label description prompt prompt-guidelines params  execute  runtime config]

  #js {:name name
       :label label
       :description description
       :promptSnippet prompt
       :promptGuidelines prompt-guidelines
       :parameters (->params params)
       :execute (partial execute runtime config)})
(def send-params
  [:map
   [:channel_id {:description "Discord channel ID to send the message to. Use discord.list.channels to discover IDs."} :string]
   [:text {:description "Message content to send. Long messages will be chunked automatically."} :string]
   [:reply_to {:optional true :description "Optional message ID to reply to."} :string]
   [:attachment_urls {:optional true :description "Optional attachment sources to upload: HTTP(S) URLs, data URLs, absolute file paths, or workspace-relative paths (e.g. sandbox output files, generated images)."} [:vector :string]]])
(defn discord-send-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (aget params "channel_id") (aget params "channelId") "")
        text (or (aget params "text") (aget params "content") "")
        reply-to (or (aget params "reply_to") (aget params "replyTo"))
        attachment-urls (or (aget params "attachment_urls") (aget params "attachmentUrls") #js [])]
    (maybe-tool-update! on-update (str "Sending Discord message to " channel-id "…"))
    (-> (discord-send-message! runtime config channel-id text reply-to attachment-urls)
        (.then (fn [result]
                 (tool-text-result (str "Sent Discord message " (:messageId result) " to channel " channel-id)
                                   result))))))
(def discord-sent-tool (partial create-tool-obj "discord.send" "Discord Send" "Send a message to a Discord channel, optionally as a reply to an existing message."
                          send-params
                          discord-send-execute))

(defn create-tool-context [])
;; (.Object Type #js {:channelId (.String Type #js {:description "Discord channel ID to read messages from."})
;;                    :limit (type-optional Type (.Number Type #js {:description "Maximum number of messages to return." :minimum 1 :maximum 100}))})
(def discord-tools [])
(defn create-discord-tools [runtime config auth-context]
  (let [Type (aget runtime "Type")
        allowed? (fn [tool-id] (or (nil? auth-context) (ctx-tool-allowed? auth-context-tool-id)))]

    )
  )


(create-tool
 (fn [tool-call-id params signal on-update ctx]
                                )
 )

(when (allowed? "discord.send")
                  (doto (js-obj)
                    (aset "name" "discord.send")
                    (aset "label" "Discord Send")
                    (aset "description" "Send a message to a Discord channel, optionally as a reply to an existing message.")
                    (aset "promptSnippet" "Send a Discord message or reply to a specific message id.")
                    (aset "promptGuidelines" (clj->js ["Use discord.send to post or reply in channels once you know the channel id."
                                                       "Use discord.list.servers and discord.list.channels first if you need discovery."
                                                       "Include attachment_urls to upload local files, images, data URLs, or remote URLs."
                                                       "To mention a user, use <@user_id> in the text. Do NOT use @username — it will not ping."
                                                       "To reply to a message, pass its message id as reply_to."
                                                       "Thread IDs work as channel_ids for sending messages inside threads."]))
                    (aset "parameters" send-params)
                    (aset "execute" discord-send-execute)))
(deftool discord-send-execute
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        channel-id (or (aget params "channel_id") (aget params "channelId") "")
        text (or (aget params "text") (aget params "content") "")
        reply-to (or (aget params "reply_to") (aget params "replyTo"))
        attachment-urls (or (aget params "attachment_urls") (aget params "attachmentUrls") #js [])]
    (maybe-tool-update! on-update (str "Sending Discord message to " channel-id "…"))
    (-> (discord-send-message! runtime config channel-id text reply-to attachment-urls)
        (.then (fn [result]
                 (tool-text-result (str "Sent Discord message " (:messageId result) " to channel " channel-id)
                                   result)))))
  )
