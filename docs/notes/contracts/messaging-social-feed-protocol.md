---
title: "μ Messaging & Social Feed Protocol Contracts"
category: contracts
created: 2026-04-27
original: 2026.04.27.13.20.53.md
status: note
---

Good — I have the full picture of the existing capability surface . Now I can design these protocols cleanly against what already exists.

***

## #μ Messaging & Social Feed Protocol Contracts

The key insight: every platform has **two distinct capability surfaces** —
- **Messaging** (`msg.*`): directed, addressed, 1:1 or group. Thread-aware. Read/write/react.
- **Feed** (`feed.*`): public broadcast, unaddressed. Post/boost/reply/search/timeline.

These are **platform-independent protocols**. Platform capabilities (`cap/discord`, `cap/telegram`, etc.) implement them. Agents only depend on the protocol capability, not the platform.

***

## #μ Malli Schemas — Core Protocol Types

```clojure
(ns knoxx.contracts.protocols.schema
  (:require [malli.core :as m]))

;; ─── Shared ──────────────────────────────────────────────────────────────────

(def Platform
  [:enum :discord :telegram :signal :slack
         :bluesky :mastodon :x :reddit :linkedin])

(def MessageId   :string)
(def ChannelId   :string)  ;; channel / room / thread / subreddit / board
(def UserId      :string)
(def ContentBody :string)  ;; plain text; rich content in :attachments

(def Attachment
  [:map
   [:kind [:enum :image :video :audio :file :embed :poll]]
   [:url  {:optional true} :string]
   [:alt  {:optional true} :string]
   [:data {:optional true} :any]])

(def Message
  "A received or composed message — works for both messaging and feed replies."
  [:map
   [:msg/id        {:optional true} MessageId]
   [:msg/platform  Platform]
   [:msg/channel   {:optional true} ChannelId]   ;; nil for DMs
   [:msg/thread    {:optional true} MessageId]   ;; parent thread id
   [:msg/author    {:optional true} UserId]
   [:msg/body      ContentBody]
   [:msg/ts        {:optional true} :string]     ;; ISO-8601
   [:msg/is-bot    {:optional true} :boolean]
   [:msg/attachments {:optional true} [:vector Attachment]]])

(def Post
  "A feed post — broadcast, not addressed."
  [:map
   [:post/id       {:optional true} :string]
   [:post/platform Platform]
   [:post/channel  {:optional true} ChannelId]  ;; subreddit, hashtag, board
   [:post/author   {:optional true} UserId]
   [:post/body     ContentBody]
   [:post/ts       {:optional true} :string]
   [:post/tags     {:optional true} [:vector :string]]
   [:post/attachments {:optional true} [:vector Attachment]]
   [:post/metrics  {:optional true}
    [:map
     [:likes    {:optional true} :int]
     [:reposts  {:optional true} :int]
     [:replies  {:optional true} :int]
     [:views    {:optional true} :int]]]])

;; ─── Tool param schemas ───────────────────────────────────────────────────────

(def MsgReadParams
  [:map
   [:channel ChannelId]
   [:limit   {:optional true} :int]          ;; default 25, max 100
   [:before  {:optional true} MessageId]     ;; cursor
   [:after   {:optional true} MessageId]])

(def MsgSendParams
  [:map
   [:channel     ChannelId]
   [:body        ContentBody]
   [:thread      {:optional true} MessageId]
   [:attachments {:optional true} [:vector Attachment]]
   [:ephemeral   {:optional true} :boolean]])  ;; only sender sees it

(def MsgReactParams
  [:map [:message MessageId] [:emoji :string]])

(def MsgThreadParams
  [:map
   [:message     MessageId]
   [:title       {:optional true} :string]
   [:body        {:optional true} ContentBody]])

(def FeedPostParams
  [:map
   [:body        ContentBody]
   [:channel     {:optional true} ChannelId]  ;; subreddit, hashtag board
   [:tags        {:optional true} [:vector :string]]
   [:attachments {:optional true} [:vector Attachment]]
   [:reply-to    {:optional true} :string]])  ;; post/id to reply to

(def FeedSearchParams
  [:map
   [:query   :string]
   [:channel {:optional true} ChannelId]
   [:limit   {:optional true} :int]
   [:sort    {:optional true} [:enum :new :top :hot :relevance]]])

(def FeedTimelineParams
  [:map
   [:channel {:optional true} ChannelId]
   [:limit   {:optional true} :int]
   [:cursor  {:optional true} :string]])
```

***

## #μ Capability Contracts

### Messaging Protocol (platform-independent)

```edn
;; contracts/capabilities/cap_messaging.edn
{:cap/id    :cap/messaging
 :cap/label "Messaging Protocol"
 :cap/description
 "Platform-neutral directed messaging surface.
  Agents use msg.* tools; platform drivers implement them.
  All tools take a :platform key to route to the correct driver."
 :cap/tools [:msg.read          ;; fetch recent messages from a channel/DM
             :msg.send          ;; send a message to a channel or thread
             :msg.reply         ;; reply to a specific message (thread-safe)
             :msg.react         ;; add an emoji reaction
             :msg.thread.create ;; start a thread from a message
             :msg.thread.read   ;; read a thread's messages
             :msg.dm.send       ;; send a direct message to a user
             :msg.dm.read       ;; read DM history with a user
             :msg.search        ;; search messages in a channel
             :msg.delete        ;; delete own message
             :msg.edit]         ;; edit own message
 :cap/user-surfaces
 [{:surface/id    :workspace/messaging-inbox
   :surface/label "Messaging Inbox"
   :surface/kind  :reader
   :surface/routes ["/ops/messaging"]
   :surface/description
   "Unified inbox across messaging platforms. Humans triage and reply; agents use msg.* tools."}]}
```

### Feed Protocol (platform-independent)

```edn
;; contracts/capabilities/cap_feed.edn
{:cap/id    :cap/feed
 :cap/label "Feed / Microblog Protocol"
 :cap/description
 "Platform-neutral public broadcast surface.
  Covers post, boost/repost, reply, search, and timeline reads.
  All tools take a :platform key."
 :cap/tools [:feed.post          ;; publish a new post/status/submission
             :feed.reply         ;; reply to an existing post (is also a post)
             :feed.repost        ;; boost/retweet/reblog/cross-post
             :feed.search        ;; search public posts
             :feed.timeline      ;; read home / following / subreddit timeline
             :feed.thread        ;; read a full post thread (replies tree)
             :feed.profile       ;; fetch a user's public profile + recent posts
             :feed.delete        ;; delete own post
             :feed.reactions     ;; like/upvote/downvote/award
             :feed.dm.send       ;; platform DMs embedded inside social (Twitter DM, Reddit chat)
             :feed.dm.read]
 :cap/user-surfaces
 [{:surface/id    :workspace/feed-composer
   :surface/label "Feed Composer"
   :surface/kind  :composer
   :surface/routes ["/ops/social"]
   :surface/description
   "Draft, schedule, and publish to social feeds. Agents post, search, and read timelines."}]}
```

### Platform capability contracts (thin wrappers — list which protocol tools are *actually* supported)

```edn
;; contracts/capabilities/cap_discord.edn  (replaces existing)
{:cap/id       :cap/discord
 :cap/label    "Discord"
 :cap/protocol :cap/messaging          ;; implements messaging protocol
 :cap/platform :discord
 :cap/tools    [:msg.read   :msg.send   :msg.reply   :msg.react
                :msg.thread.create      :msg.thread.read
                :msg.dm.send            :msg.dm.read
                :msg.search
                ;; discord-native aliases kept for back-compat
                :discord.guilds         :discord.list.servers
                :discord.list.channels  :discord.channels]
 :cap/user-surfaces
 [{:surface/id    :workspace/discord-ops
   :surface/label "Discord Operations"
   :surface/kind  :admin-console
   :surface/routes ["/event-agents"]}]}

;; contracts/capabilities/cap_telegram.edn  (NEW)
{:cap/id       :cap/telegram
 :cap/label    "Telegram"
 :cap/protocol :cap/messaging
 :cap/platform :telegram
 :cap/tools    [:msg.read   :msg.send   :msg.reply   :msg.react
                :msg.thread.read
                :msg.dm.send  :msg.dm.read
                :msg.search
                :telegram.bot.info     ;; platform-specific extras
                :telegram.sticker.send]}

;; contracts/capabilities/cap_signal.edn  (NEW)
{:cap/id       :cap/signal
 :cap/label    "Signal"
 :cap/protocol :cap/messaging
 :cap/platform :signal
 :cap/tools    [:msg.read   :msg.send   :msg.reply
                :msg.dm.send  :msg.dm.read]
 ;; Signal has no public search, no reactions, limited thread model
 :cap/notes    "No public search. No reactions. Groups only via signal-cli."}

;; contracts/capabilities/cap_slack.edn  (NEW)
{:cap/id       :cap/slack
 :cap/label    "Slack"
 :cap/protocol :cap/messaging
 :cap/platform :slack
 :cap/tools    [:msg.read   :msg.send   :msg.reply   :msg.react
                :msg.thread.create      :msg.thread.read
                :msg.dm.send            :msg.dm.read
                :msg.search
                :msg.edit               :msg.delete
                :slack.channel.list     :slack.workspace.info
                :slack.user.profile]}

;; contracts/capabilities/cap_bluesky.edn  (replaces existing)
{:cap/id       :cap/bluesky
 :cap/label    "Bluesky"
 :cap/protocol :cap/feed
 :cap/platform :bluesky
 :cap/tools    [:feed.post   :feed.reply  :feed.repost
                :feed.search :feed.timeline :feed.thread
                :feed.profile :feed.reactions :feed.delete
                :feed.dm.send :feed.dm.read]  ;; Bluesky DMs via chat.bsky
 :cap/user-surfaces
 [{:surface/id    :workspace/social-publisher
   :surface/label "Social Publisher"
   :surface/kind  :composer
   :surface/routes ["/ops/contracts"]}]}

;; contracts/capabilities/cap_mastodon.edn  (NEW)
{:cap/id       :cap/mastodon
 :cap/label    "Mastodon"
 :cap/protocol :cap/feed
 :cap/platform :mastodon
 :cap/tools    [:feed.post   :feed.reply  :feed.repost
                :feed.search :feed.timeline :feed.thread
                :feed.profile :feed.reactions :feed.delete
                :feed.dm.send :feed.dm.read]}  ;; Mastodon DMs via "followers-only" posts + DM API

;; contracts/capabilities/cap_x.edn  (NEW)
{:cap/id       :cap/x
 :cap/label    "X (Twitter)"
 :cap/protocol :cap/feed
 :cap/platform :x
 :cap/tools    [:feed.post   :feed.reply  :feed.repost
                :feed.search :feed.timeline :feed.thread
                :feed.profile :feed.reactions :feed.delete
                :feed.dm.send :feed.dm.read]
 :cap/notes    "Rate limits apply. Search requires Basic+ tier API."}

;; contracts/capabilities/cap_reddit.edn  (NEW)
{:cap/id       :cap/reddit
 :cap/label    "Reddit"
 :cap/protocol :cap/feed
 :cap/platform :reddit
 :cap/tools    [:feed.post       ;; text, link, image, video post
                :feed.reply      ;; comment
                :feed.search     ;; subreddit or sitewide
                :feed.timeline   ;; subreddit listing (new/hot/top/rising)
                :feed.thread     ;; comment tree
                :feed.profile    ;; user profile + post/comment history
                :feed.reactions  ;; upvote/downvote/award
                :feed.delete
                :feed.dm.send    ;; Reddit chat / modmail
                :feed.dm.read
                :reddit.subreddit.info
                :reddit.flair.list]}

;; contracts/capabilities/cap_linkedin.edn  (NEW)
{:cap/id       :cap/linkedin
 :cap/label    "LinkedIn"
 :cap/protocol :cap/feed
 :cap/platform :linkedin
 :cap/tools    [:feed.post   :feed.reply
                :feed.search :feed.timeline
                :feed.profile :feed.reactions
                :feed.dm.send :feed.dm.read]
 :cap/notes    "LinkedIn API heavily restricted. feed.search and feed.timeline require Partner API access."}
```

***

## #μ Protocol Dispatch Schema

The tool router needs to know *which driver* to invoke. The dispatch key is always `:platform`.

```clojure
(ns knoxx.backend.tools.platform-dispatch
  "Routes msg.* and feed.* tool calls to the correct platform driver.")

(def ^:private msg-drivers
  {:discord  #'knoxx.backend.drivers.discord/msg-handler
   :telegram #'knoxx.backend.drivers.telegram/msg-handler
   :signal   #'knoxx.backend.drivers.signal/msg-handler
   :slack    #'knoxx.backend.drivers.slack/msg-handler})

(def ^:private feed-drivers
  {:bluesky  #'knoxx.backend.drivers.bluesky/feed-handler
   :mastodon #'knoxx.backend.drivers.mastodon/feed-handler
   :x        #'knoxx.backend.drivers.x/feed-handler
   :reddit   #'knoxx.backend.drivers.reddit/feed-handler
   :linkedin #'knoxx.backend.drivers.linkedin/feed-handler})

(defn dispatch-msg!
  [{:keys [platform] :as params}]
  (if-let [driver (get msg-drivers (keyword platform))]
    (driver params)
    (js/Promise.reject
      (js/Error. (str "No messaging driver for platform: " platform)))))

(defn dispatch-feed!
  [{:keys [platform] :as params}]
  (if-let [driver (get feed-drivers (keyword platform))]
    (driver params)
    (js/Promise.reject
      (js/Error. (str "No feed driver for platform: " platform)))))
```

***

## Platform × Tool Coverage Matrix

| Tool | Discord | Telegram | Signal | Slack | Bluesky | Mastodon | X | Reddit | LinkedIn |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `msg.read` | ✓ | ✓ | ✓ | ✓ | — | — | — | — | — |
| `msg.send` | ✓ | ✓ | ✓ | ✓ | — | — | — | — | — |
| `msg.reply` | ✓ | ✓ | ✓ | ✓ | — | — | — | — | — |
| `msg.react` | ✓ | ✓ | — | ✓ | — | — | — | — | — |
| `msg.thread.*` | ✓ | ✓ | — | ✓ | — | — | — | — | — |
| `msg.search` | ✓ | — | — | ✓ | — | — | — | — | — |
| `msg.dm.*` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `feed.post` | — | — | — | — | ✓ | ✓ | ✓ | ✓ | ✓ |
| `feed.reply` | — | — | — | — | ✓ | ✓ | ✓ | ✓ | ✓ |
| `feed.repost` | — | — | — | — | ✓ | ✓ | ✓ | — | — |
| `feed.search` | — | — | — | — | ✓ | ✓ | ✓† | ✓ | ✓† |
| `feed.timeline` | — | — | — | — | ✓ | ✓ | ✓ | ✓ | ✓† |
| `feed.reactions` | — | — | — | — | ✓ | ✓ | ✓ | ✓ | ✓ |

† = API tier or partner access required 

***

## Role Example Using Both Protocols

```edn
;; contracts/roles/social_broadcaster.edn
{:role/id           :role/social-broadcaster
 :role/label        "Social Broadcaster"
 :role/capabilities [:cap/feed            ;; protocol — works across all feed platforms
                     :cap/messaging        ;; protocol — works across all messaging platforms
                     :cap/memory
                     :cap/websearch]
 :role/description
 "Posts to and monitors social feeds; responds to direct messages and channel mentions across platforms."}
```

An agent holding `:role/social-broadcaster` gains `feed.*` and `msg.*` tools simultaneously, and the platform is resolved at call-time from the `:platform` param — no platform-specific role needed .
