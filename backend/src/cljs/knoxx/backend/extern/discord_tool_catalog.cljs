(ns knoxx.backend.extern.discord-tool-catalog
  "Construct native Discord SDK tools from contracts and capability selection."
  (:require [knoxx.backend.domain.tools :refer [create-tool-obj]]
            [knoxx.backend.extern.discord :as xdiscord]
            [knoxx.backend.extern.discord-tool-execution :as execution]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.law.discord-tools :as schemas]))

(def discord-send-tool
  "Discord tool catalog entry: discord-send-tool." (partial create-tool-obj "discord.send" "Discord Send" "Send a message to a Discord channel, optionally as a reply to an existing message."
                          "Send a Discord message or reply to a specific message id."
                          ["Use discord.publish or discord.send to share results in a Discord channel."
                                    "Provide channelId/channel_id and content/text."
                                    "Include attachmentUrls/attachment_urls to upload files, images, or generated assets."
                                    "Pass file paths as plain strings (e.g. Graphics/seal.svg or Voice/clip.mp3). Do NOT wrap them in <|\"| delimiters."
                                    "To mention a user, use <@user_id> in the text. Do NOT use @username — it will not ping."]
                          schemas/send-params
                          execution/discord-send-execute))


;; ---------------------------------------------------------------------------
;; Hoisted tool definitions: params, execute, tool-obj per tool
;; ---------------------------------------------------------------------------

(def channel-messages-tool
  "Discord tool catalog entry: channel-messages-tool." (partial create-tool-obj "discord.channel.messages" "Discord Channel Messages"
                                    "Fetch messages from a Discord channel with before/after/around cursors."
                                    "Fetch channel messages from Discord with pagination cursors when you need exact transcript context."
                                    ["Use this when you know the channel id and need exact message history."
                                              "Use before/after/around for precise pagination."]
                                    schemas/channel-messages-params
                                    execution/channel-messages-execute))

(def channel-scroll-tool
  "Discord tool catalog entry: channel-scroll-tool." (partial create-tool-obj "discord.channel.scroll" "Discord Channel Scroll"
                                  "Scroll older channel messages by fetching messages before the oldest already-seen message id."
                                  "Scroll backwards in a Discord channel once you already know the oldest seen id."
                                  ["Use discord.channel.scroll as sugar over discord.channel.messages before=oldest_seen_id."
                                            "Useful for paging backward through long histories."]
                                  schemas/channel-scroll-params
                                  execution/channel-scroll-execute))

(def dm-messages-tool
  "Discord tool catalog entry: dm-messages-tool." (partial create-tool-obj "discord.dm.messages" "Discord DM Messages"
                               "Fetch messages from the DM channel shared with a Discord user."
                               "Read DM history with a Discord user by user id."
                               ["Use this when the relevant conversation is in DMs rather than a guild channel."
                                         "Provide the target user id."]
                               schemas/dm-messages-params
                               execution/dm-messages-execute))

(def search-tool
  "Discord tool catalog entry: search-tool." (partial create-tool-obj "discord.search" "Discord Search"
                          "Search channel or DM messages by content and/or author using client-side filtering."
                          "Search Discord messages by text and scope to find relevant discussion quickly."
                          ["Use scope=channel with channel_id for guild channels or scope=dm with user_id for DMs."
                                    "Messages marked bad in OpenPlanner labels are never shown."
                                    "Matching good-marked messages are returned first in chronological order, then unbad messages chronologically."
                                    "The default timeframe is 168 hours; pass since_hours to override when needed."
                                    "This falls back to client-side filtering when native search is unavailable."]
                          schemas/search-params
                          execution/search-execute))

(def list-servers-tool
  "Discord tool catalog entry: list-servers-tool." (partial create-tool-obj "discord.list.servers" "Discord List Servers"
                                "List all Discord servers/guilds the bot can access."
                                "List Discord servers before choosing channels or replying into a guild."
                                ["Use this before discord.list.channels when you need discovery."
                                          "Do not guess guild ids."]
                                schemas/list-servers-params
                                execution/list-servers-execute))

(def guilds-tool
  "Discord tool catalog entry: guilds-tool." (partial create-tool-obj "discord.guilds" "Discord Guilds"
                          "List Discord guilds/servers the bot is in."
                          "List Discord guilds to discover available servers."
                          ["Alias for discord.list.servers."
                                    "Use before listing channels or posting to a specific server."]
                          schemas/list-servers-params
                          execution/list-servers-execute))

(def list-channels-tool
  "Discord tool catalog entry: list-channels-tool." (partial create-tool-obj "discord.list.channels" "Discord List Channels"
                                 "List channels in one Discord guild or across all visible guilds."
                                 "List Discord channels to discover readable/postable targets."
                                 ["If guild_id is omitted, returns channels across all visible guilds."
                                           "Use returned channel ids with discord.channel.messages or discord.send."]
                                 schemas/list-channels-params
                                 execution/list-channels-execute))

(def channels-tool
  "Discord tool catalog entry: channels-tool." (partial create-tool-obj "discord.channels" "Discord Channels"
                            "List channels in a Discord guild."
                            "List channels in a Discord guild to find the right channel for reading or posting."
                            ["Alias for discord.list.channels."
                                      "Use guildId/guild_id when you already know the server."]
                            schemas/channels-params
                            execution/channels-execute))

(def react-tool
  "Discord tool catalog entry: react-tool." (partial create-tool-obj "discord.react" "Discord React"
                         "Add an emoji reaction to a Discord message."
                         "React to a Discord message with an emoji."
                         ["Use discord.react to add emoji reactions to messages."
                                   "Provide channel_id, message_id, and an emoji (e.g. 👍, 🎉, 💀)."]
                         schemas/react-params
                         execution/react-execute))

(def thread-create-tool
  "Discord tool catalog entry: thread-create-tool." (partial create-tool-obj "discord.thread.create" "Discord Thread Create"
                                 "Create a Discord thread from a message or in a channel."
                                 "Create a thread to spin off a conversation."
                                 ["Use discord.thread.create to start a thread from a message or in a channel."
                                           "Provide channel_id and a name. Optionally pass message_id to create a thread from that message."
                                           "After creating a thread, use the returned threadId as channel_id with discord.send to post in it."]
                                 schemas/thread-create-params
                                 execution/thread-create-execute))

;; ---------------------------------------------------------------------------
;; Wrapper tools: remap params then delegate to an existing execute
;; ---------------------------------------------------------------------------

(def publish-tool
  "Discord tool catalog entry: publish-tool." (partial create-tool-obj "discord.publish" "Discord Publish"
                           "Post a message to a Discord channel using the configured Knoxx Discord bot."
                           "Post updates, summaries, or notifications to Discord channels."
                           ["Use discord.publish or discord.send to share results in a Discord channel."
                                     "Provide channelId/channel_id and content/text."
                                     "Include attachmentUrls/attachment_urls to upload files, images, or generated assets."
                                     "To mention a user, use <@user_id> in the text. Do NOT use @username — it will not ping."]
                           schemas/publish-params
                           execution/publish-execute))

(def read-tool
  "Discord tool catalog entry: read-tool." (partial create-tool-obj "discord.read" "Discord Read"
                        "Read recent messages from a Discord channel."
                        "Read recent messages from a Discord channel to understand context."
                        ["Use discord.read as a simple alias for discord.channel.messages."
                                  "For pagination or cursors, use discord.channel.messages or discord.channel.scroll directly."]
                        schemas/read-params
                        execution/read-execute))

;; ---------------------------------------------------------------------------
;; create-discord-custom-tools: gate + collect
;; ---------------------------------------------------------------------------

(defn create-discord-custom-tools
  "Discord tool catalog entry: create-discord-custom-tools."
  ([runtime config] (create-discord-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (xdiscord/tool-array
      (remove nil?
              [(when (allowed? "discord.publish") (publish-tool runtime config))
               (when (allowed? "discord.send") (discord-send-tool runtime config))
               (when (allowed? "discord.react") (react-tool runtime config))
               (when (allowed? "discord.thread.create") (thread-create-tool runtime config))
               (when (allowed? "discord.read") (read-tool runtime config))
               (when (allowed? "discord.channel.messages") (channel-messages-tool runtime config))
               (when (allowed? "discord.channel.scroll") (channel-scroll-tool runtime config))
               (when (allowed? "discord.dm.messages") (dm-messages-tool runtime config))
               (when (allowed? "discord.search") (search-tool runtime config))
               (when (allowed? "discord.guilds") (guilds-tool runtime config))
               (when (allowed? "discord.list.servers") (list-servers-tool runtime config))
               (when (allowed? "discord.channels") (channels-tool runtime config))
               (when (allowed? "discord.list.channels") (list-channels-tool runtime config))])))))
