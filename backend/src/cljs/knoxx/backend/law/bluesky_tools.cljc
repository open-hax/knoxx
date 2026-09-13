(ns knoxx.backend.law.bluesky-tools
  "Bluesky tool parameter contracts.")

(def publish-params
  "Parameter schema for publish-params."
  [:map
   [:text {:description "Bluesky post text. Keep it concise and under platform limits."} :string]
   [:images {:optional true :description "Optional image sources (workspace paths, URLs, or data URLs). Up to 4 images."} [:vector :string]]
   [:imageAlts {:optional true :description "Optional alt text for each image."} [:vector :string]]
   [:replyTo {:optional true :description "Optional AT-URI of a post to reply to."} :string]])

(def profile-params
  "Parameter schema for profile-params."
  [:map
   [:actor {:optional true :description "Optional Bluesky handle or DID. Defaults to the authenticated account."} :string]])

(def search-params
  "Parameter schema for search-params."
  [:map
   [:query {:description "Search query for Bluesky posts or actors."} :string]
   [:kind {:optional true :description "posts or actors. Defaults to posts."} :string]
   [:limit {:optional true :description "Maximum results to return."} [:int {:min 1 :max 25}]]])

(def feed-params
  "Parameter schema for feed-params."
  [:map
   [:actor {:description "Bluesky handle or DID whose feed should be read."} :string]
   [:limit {:optional true :description "Maximum posts to return."} [:int {:min 1 :max 25}]]])

(def timeline-params
  "Parameter schema for timeline-params."
  [:map
   [:limit {:optional true :description "Maximum timeline posts to return."} [:int {:min 1 :max 25}]]
   [:cursor {:optional true :description "Optional pagination cursor from a previous bluesky.timeline call."} :string]])

(def post-uri-params
  "Parameter schema for post-uri-params."
  [:map
   [:uri {:description "AT-URI of the Bluesky post (at://did/collection/rkey)."} :string]])

(def actor-params
  "Parameter schema for actor-params."
  [:map
   [:actor {:description "Bluesky handle or DID."} :string]])

(def thread-params
  "Parameter schema for thread-params."
  [:map
   [:uri {:description "AT-URI of the root post to read the thread for."} :string]
   [:depth {:optional true :description "Depth of replies to fetch. Default 6."} [:int {:min 1 :max 10}]]])

(def notifications-params
  "Parameter schema for notifications-params."
  [:map
   [:limit {:optional true :description "Maximum notifications to return."} [:int {:min 1 :max 50}]]
   [:cursor {:optional true :description "Optional pagination cursor."} :string]])

(def chat-send-params
  "Parameter schema for chat-send-params."
  [:map
   [:convoId {:description "Conversation ID (from bluesky.chat.list)."} :string]
   [:text {:description "Message text to send."} :string]
   [:replyToMessageId {:optional true :description "Optional message ID to reply to within the conversation."} :string]])

(def chat-list-params
  "Parameter schema for chat-list-params."
  [:map
   [:limit {:optional true :description "Maximum conversations to return."} [:int {:min 1 :max 50}]]])

(def chat-read-params
  "Parameter schema for chat-read-params."
  [:map
   [:convoId {:description "Conversation ID (from bluesky.chat.list)."} :string]
   [:limit {:optional true :description "Maximum messages to return."} [:int {:min 1 :max 100}]]])

(def chat-react-params
  "Parameter schema for chat-react-params."
  [:map
   [:convoId {:description "Conversation ID."} :string]
   [:messageId {:description "Message ID to react to."} :string]
   [:emoji {:description "Emoji reaction (e.g. ❤️, 🔥, 😂)."} :string]])

