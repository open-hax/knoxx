(ns knoxx.backend.extern.bluesky-tool-catalog
  "Construct native SDK tools from Bluesky contracts and authorized capabilities."
  (:require [knoxx.backend.domain.tools :as tools]
            [knoxx.backend.extern.bluesky-tool-execution :as execution]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.law.bluesky-tools :as schemas]))

(def publish-tool
  "Bluesky tool catalog entry: publish-tool."
  (partial tools/create-tool-obj
     "bluesky.publish" "Bluesky Publish"
     "Publish a post to Bluesky using the configured account."
     "Post a concise update to Bluesky when public social publishing is useful."
     ["Use for original posts, replies (replyTo), and image posts (images param)."
      "Always include relevant hashtags in the text body; they will be auto-faceted."
      "URLs in the text body are auto-faceted as clickable links."
      "For image posts, provide workspace paths, URLs, or data URLs in the images vector."
      "When replying, pass the parent post AT-URI as replyTo."]
     schemas/publish-params
     execution/publish-execute))

(def profile-tool
  "Bluesky tool catalog entry: profile-tool."
  (partial tools/create-tool-obj
     "bluesky.profile" "Bluesky Profile"
     "Read a Bluesky profile by handle or DID, or default to the authenticated account."
     "Read a Bluesky profile by handle or DID, or default to the authenticated account."
     ["Use to inspect a user's bio, follower count, and posts before engaging."
      "Leave actor empty to read the authenticated account's own profile."
      "Accepts handles like @alice.bsky.social or raw DIDs."]
     schemas/profile-params
     execution/profile-execute))

(def search-tool
  "Bluesky tool catalog entry: search-tool."
  (partial tools/create-tool-obj
     "bluesky.search" "Bluesky Search"
     "Search public Bluesky posts or actors."
     "Search public Bluesky posts or actors."
     ["Use to discover trending topics, find inspiration, or locate specific posts."
      "Default kind is posts; set kind to actors to search for users."
      "Hashtag searches work best with the # prefix: #generative, #music, etc."]
     schemas/search-params
     execution/search-execute))

(def author-feed-tool
  "Bluesky tool catalog entry: author-feed-tool."
  (partial tools/create-tool-obj
     "bluesky.author.feed" "Bluesky Author Feed"
     "Read recent posts from a specific Bluesky author."
     "Read recent posts from a specific Bluesky author."
     ["Use to browse a specific creator's recent posts before interacting."
      "Pass the actor's handle or DID."
      "Great for finding content to reply to, repost, or draw inspiration from."]
     schemas/feed-params
     execution/author-feed-execute))

(def timeline-tool
  "Bluesky tool catalog entry: timeline-tool."
  (partial tools/create-tool-obj
     "bluesky.timeline" "Bluesky Timeline"
     "Read the authenticated account's Bluesky timeline."
     "Read the authenticated account's Bluesky timeline."
     ["Use CONSTANTLY to stay aware of the current vibe and trending content."
      "This is your primary source of context before posting or engaging."
      "Pass cursor from a previous call to paginate further back."]
     schemas/timeline-params
     execution/timeline-execute))

(def repost-tool
  "Bluesky tool catalog entry: repost-tool."
  (partial tools/create-tool-obj
            "bluesky.repost" "Bluesky Repost"
            "Repost (quote-retweet) a Bluesky post by AT-URI."
            "Repost a Bluesky post to share it with followers."
            ["Use when you want to amplify content you find valuable or entertaining."
             "Pass the full AT-URI of the post to repost."
             "Reposting builds social capital and signals taste to your followers."]
            schemas/post-uri-params
            execution/repost-execute))

(def like-tool
  "Bluesky tool catalog entry: like-tool."
  (partial tools/create-tool-obj
            "bluesky.like" "Bluesky Like"
            "Like a Bluesky post by AT-URI."
            "Like a Bluesky post to show appreciation."
            ["Use generously to signal engagement and build goodwill."
             "Pass the full AT-URI of the post to like."
             "Liking is low-cost social capital; do it often for posts that resonate."]
            schemas/post-uri-params
            execution/like-execute))

(def unlike-tool
  "Bluesky tool catalog entry: unlike-tool."
  (partial tools/create-tool-obj
            "bluesky.unlike" "Bluesky Unlike"
            "Remove a like from a Bluesky post by like record AT-URI."
            "Remove a like from a Bluesky post."
            ["Use to remove a previous like."
             "Pass the AT-URI of the like record itself (not the post URI)."]
            schemas/post-uri-params
            execution/unlike-execute))

(def follow-tool
  "Bluesky tool catalog entry: follow-tool."
  (partial tools/create-tool-obj
            "bluesky.follow" "Bluesky Follow"
            "Follow a Bluesky actor by handle or DID."
            "Follow a Bluesky user."
            ["Use to follow creators whose content you want to see in your timeline."
             "Pass the handle or DID of the actor."
             "Aggressively curate your following list; unfollow if content quality drops."]
            schemas/actor-params
            execution/follow-execute))

(def unfollow-tool
  "Bluesky tool catalog entry: unfollow-tool."
  (partial tools/create-tool-obj
            "bluesky.unfollow" "Bluesky Unfollow"
            "Unfollow a Bluesky actor by follow record AT-URI."
            "Unfollow a Bluesky user."
            ["Use to unfollow an actor."
             "Pass the AT-URI of the follow record (not the actor handle)."]
            schemas/post-uri-params
            execution/unfollow-execute))

(def delete-tool
  "Bluesky tool catalog entry: delete-tool."
  (partial tools/create-tool-obj
            "bluesky.delete" "Bluesky Delete"
            "Delete one of your own Bluesky posts by AT-URI."
            "Delete a Bluesky post you authored."
            ["Use to remove posts that flopped, aged poorly, or were mistakes."
             "Pass the AT-URI of your own post."
             "Curate your feed like a gallery; delete without remorse."]
            schemas/post-uri-params
            execution/delete-execute))

(def thread-tool
  "Bluesky tool catalog entry: thread-tool."
  (partial tools/create-tool-obj
            "bluesky.thread" "Bluesky Thread"
            "Read a Bluesky post thread including replies."
            "Read a post and its reply thread."
            ["Use to read full conversation threads before jumping in."
             "Pass the AT-URI of the root post."
             "Adjust depth for deeper reply trees; default is 6."]
            schemas/thread-params
            execution/thread-execute))

(def notifications-tool
  "Bluesky tool catalog entry: notifications-tool."
  (partial tools/create-tool-obj
            "bluesky.notifications" "Bluesky Notifications"
            "Read notifications for the authenticated Bluesky account."
            "Check notifications on Bluesky."
            ["Use frequently to stay on top of replies, mentions, likes, and follows."
             "Engagement compounds; respond to replies and mentions promptly."
             "Notifications reveal who is interacting with you and why."]
            schemas/notifications-params
            execution/notifications-execute))

(def followers-tool
  "Bluesky tool catalog entry: followers-tool."
  (partial tools/create-tool-obj
     "bluesky.followers" "Bluesky Followers"
     "List followers of a Bluesky actor."
     "Read the followers list of a Bluesky user."
     ["Use to inspect who follows a given actor."
      "Pass the actor's handle or DID."
      "Useful for understanding audience overlap and community composition."]
     schemas/feed-params
     execution/followers-execute))

(def follows-tool
  "Bluesky tool catalog entry: follows-tool."
  (partial tools/create-tool-obj
     "bluesky.follows" "Bluesky Follows"
     "List accounts a Bluesky actor follows."
     "Read the following list of a Bluesky user."
     ["Use to discover who a creator follows for taste graph exploration."
      "Pass the actor's handle or DID."
      "Great for finding new creators to follow via transitive taste."]
     schemas/feed-params
     execution/follows-execute))

(def chat-list-tool
  "Bluesky tool catalog entry: chat-list-tool."
  (partial tools/create-tool-obj
     "bluesky.chat.list" "Bluesky Chat List"
     "List Bluesky DM conversations."
     "List direct message conversations."
     ["Use to see active DM conversations and their last messages."
      "Start here before reading or sending DMs to get the conversation IDs."]
     schemas/chat-list-params
     execution/chat-list-execute))

(def chat-send-tool
  "Bluesky tool catalog entry: chat-send-tool."
  (partial tools/create-tool-obj
     "bluesky.chat.send" "Bluesky Chat Send"
     "Send a direct message in a Bluesky conversation."
     "Send a DM in a Bluesky chat."
     ["Use to reply in DM threads."
      "Pass the convoId from chat.list and the message text."
      "Use replyToMessageId to thread replies within a conversation."]
     schemas/chat-send-params
     execution/chat-send-execute))

(def chat-read-tool
  "Bluesky tool catalog entry: chat-read-tool."
  (partial tools/create-tool-obj
     "bluesky.chat.read" "Bluesky Chat Read"
     "Read messages from a Bluesky DM conversation."
     "Read DMs in a Bluesky conversation."
     ["Use to read the message history of a specific DM conversation."
      "Pass the convoId from chat.list."
      "Check DMs regularly; they may contain collaboration invites or feedback."]
     schemas/chat-read-params
     execution/chat-read-execute))

(def chat-react-tool
  "Bluesky tool catalog entry: chat-react-tool."
  (partial tools/create-tool-obj
     "bluesky.chat.react" "Bluesky Chat React"
     "Add an emoji reaction to a message in a Bluesky DM."
     "React to a Bluesky DM message."
     ["Use to react to DM messages with emoji."
      "Pass convoId, messageId (from chat.read), and an emoji like ❤️ or 🔥."
      "Reactions are lightweight engagement for DMs."]
     schemas/chat-react-params
     execution/chat-react-execute))

(def bluesky-tool-factories
  "Bluesky tool catalog entry: bluesky-tool-factories."
  [["bluesky.publish" publish-tool]
   ["bluesky.profile" profile-tool]
   ["bluesky.search" search-tool]
   ["bluesky.author.feed" author-feed-tool]
   ["bluesky.timeline" timeline-tool]
   ["bluesky.repost" repost-tool]
   ["bluesky.like" like-tool]
   ["bluesky.unlike" unlike-tool]
   ["bluesky.follow" follow-tool]
   ["bluesky.unfollow" unfollow-tool]
   ["bluesky.delete" delete-tool]
   ["bluesky.thread" thread-tool]
   ["bluesky.notifications" notifications-tool]
   ["bluesky.followers" followers-tool]
   ["bluesky.follows" follows-tool]
   ["bluesky.chat.list" chat-list-tool]
   ["bluesky.chat.read" chat-read-tool]
   ["bluesky.chat.send" chat-send-tool]
   ["bluesky.chat.react" chat-react-tool]])

(defn- auth-context-allows-tool? [auth-context tool-id]
  (or (nil? auth-context)
      (authz/ctx-tool-allowed? auth-context tool-id)))

(defn- create-allowed-bluesky-tool [runtime config allowed? [tool-id tool-factory]]
  (when (allowed? tool-id)
    (tool-factory runtime config)))

(defn create-bluesky-custom-tools
  "Bluesky tool catalog entry: create-bluesky-custom-tools."
  ([runtime config] (create-bluesky-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (partial auth-context-allows-tool? auth-context)]
     (clj->js
      (vec (keep (partial create-allowed-bluesky-tool runtime config allowed?)
                 bluesky-tool-factories))))))
