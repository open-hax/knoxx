(ns knoxx.backend.law.discord-tools
  "Discord tool parameter contracts.")

(def send-params
  "Parameter schema for send-params."
  [:map
   [:channel_id {:description "Discord channel ID to send the message to. Use discord.list.channels to discover IDs."} :string]
   [:text {:description "Message content to send. Long messages will be chunked automatically."} :string]
   [:reply_to {:optional true :description "Optional message ID to reply to."} :string]
   [:attachment_urls {:optional true
                      :description "Optional attachment sources to upload: HTTP(S) URLs, data URLs, absolute file paths, or workspace-relative paths (e.g. sandbox output files, generated images)."}
    [:vector :string]]])
(def channel-messages-params
  "Parameter schema for channel-messages-params."
  [:map
   [:channel_id {:description "Discord channel ID to fetch messages from."} :string]
   [:limit {:optional true :description "Maximum number of messages to fetch (default 50, max 100)."} [:int {:min 1 :max 100}]]
   [:before {:optional true :description "Fetch messages before this message ID."} :string]
   [:after {:optional true :description "Fetch messages after this message ID."} :string]
   [:around {:optional true :description "Fetch messages around this message ID."} :string]])

(def channel-scroll-params
  "Parameter schema for channel-scroll-params."
  [:map
   [:channel_id {:description "Discord channel ID to fetch older messages from."} :string]
   [:oldest_seen_id {:description "Oldest message ID already seen; fetch messages before this."} :string]
   [:limit {:optional true :description "Maximum number of older messages to fetch."} [:int {:min 1 :max 100}]]])

(def dm-messages-params
  "Parameter schema for dm-messages-params."
  [:map
   [:user_id {:description "Discord user ID whose DM channel should be read."} :string]
   [:limit {:optional true :description "Maximum number of DM messages to fetch."} [:int {:min 1 :max 100}]]
   [:before {:optional true :description "Fetch DM messages before this message ID."} :string]])

(def search-params
  "Parameter schema for search-params."
  [:map
   [:scope {:description "Search scope: channel or dm."} :string]
   [:channel_id {:optional true :description "Discord channel ID to search when scope=channel."} :string]
   [:user_id {:optional true :description "Discord user ID to search against when scope=dm or to filter author."} :string]
   [:query {:optional true :description "Optional substring query to filter messages by content."} :string]
   [:limit {:optional true :description "Maximum number of matching messages to return."} [:int {:min 1 :max 100}]]
   [:before {:optional true :description "Fetch messages before this message ID."} :string]
   [:after {:optional true :description "Fetch messages after this message ID."} :string]
   [:since_hours {:optional true :description "Prefer matching messages within this many hours (default 168); pass a larger value to override the timeframe."} [:int {:min 1}]]])

(def list-servers-params
  "Parameter schema for list-servers-params."
  [:map])

(def list-channels-params
  "Parameter schema for list-channels-params."
  [:map
   [:guild_id {:optional true :description "Optional guild/server ID. If omitted, returns channels across all visible guilds."} :string]])

(def channels-params
  "Parameter schema for channels-params."
  [:map
   [:guild_id {:description "Discord guild ID to list channels for."} :string]])

(def react-params
  "Parameter schema for react-params."
  [:map
   [:channel_id {:description "Discord channel ID containing the message to react to."} :string]
   [:message_id {:description "Discord message ID to react to."} :string]
   [:emoji {:description "Emoji to react with (e.g. 👍, 🎉, 💀)."} :string]])

(def thread-create-params
  "Parameter schema for thread-create-params."
  [:map
   [:channel_id {:description "Discord channel ID to create the thread in."} :string]
   [:message_id {:optional true :description "Optional message ID to create a thread from. If omitted, creates a standalone thread in the channel."} :string]
   [:name {:description "Name of the thread (max 100 chars)."} :string]
   [:auto_archive_duration {:optional true :description "Auto-archive duration in minutes: 60, 1440 (default), 4320, or 10080."} :int]])

(def publish-params
  "Parameter schema for publish-params."
  [:map
   [:channel_id {:description "Discord channel ID to post the message to."} :string]
   [:content {:description "Message content to post to the Discord channel."} :string]
   [:attachment_urls {:optional true
                      :description "Optional attachment sources to upload: HTTP(S) URLs, data URLs, absolute file paths, or workspace-relative paths (e.g. sandbox output files, generated images)."}
    [:vector :string]]])

(def read-params
  "Parameter schema for read-params."
  [:map
   [:channel_id {:description "Discord channel ID to read messages from."} :string]
   [:limit {:optional true :description "Maximum number of messages to return."} [:int {:min 1 :max 100}]]])

