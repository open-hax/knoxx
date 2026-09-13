(ns knoxx.backend.domain.bluesky.bluesky
  "Compatibility exports for the Bluesky tool API. Native behavior lives in named extern boundaries."
  (:require [knoxx.backend.extern.bluesky-operations :as operations]
            [knoxx.backend.extern.bluesky-tool-catalog :as catalog]
            [knoxx.backend.extern.bluesky-tool-execution :as execution]
            [knoxx.backend.law.bluesky-tools :as schemas]))

(def publish-params
  "Compatibility export for publish-params."
  schemas/publish-params)

(def profile-params
  "Compatibility export for profile-params."
  schemas/profile-params)

(def search-params
  "Compatibility export for search-params."
  schemas/search-params)

(def feed-params
  "Compatibility export for feed-params."
  schemas/feed-params)

(def timeline-params
  "Compatibility export for timeline-params."
  schemas/timeline-params)

(def post-uri-params
  "Compatibility export for post-uri-params."
  schemas/post-uri-params)

(def actor-params
  "Compatibility export for actor-params."
  schemas/actor-params)

(def thread-params
  "Compatibility export for thread-params."
  schemas/thread-params)

(def notifications-params
  "Compatibility export for notifications-params."
  schemas/notifications-params)

(def chat-send-params
  "Compatibility export for chat-send-params."
  schemas/chat-send-params)

(def chat-list-params
  "Compatibility export for chat-list-params."
  schemas/chat-list-params)

(def chat-read-params
  "Compatibility export for chat-read-params."
  schemas/chat-read-params)

(def chat-react-params
  "Compatibility export for chat-react-params."
  schemas/chat-react-params)

(def bluesky-auth-config!
  "Compatibility export for bluesky-auth-config!."
  operations/bluesky-auth-config!)

(def bluesky-create-session!
  "Compatibility export for bluesky-create-session!."
  operations/bluesky-create-session!)

(def bluesky-search!
  "Compatibility export for bluesky-search!."
  operations/bluesky-search!)

(def bluesky-profile!
  "Compatibility export for bluesky-profile!."
  operations/bluesky-profile!)

(def bluesky-author-feed!
  "Compatibility export for bluesky-author-feed!."
  operations/bluesky-author-feed!)

(def bluesky-timeline!
  "Compatibility export for bluesky-timeline!."
  operations/bluesky-timeline!)

(def load-and-upload-image!
  "Compatibility export for load-and-upload-image!."
  operations/load-and-upload-image!)

(def load-and-upload-images!
  "Compatibility export for load-and-upload-images!."
  operations/load-and-upload-images!)

(def resolve-reply-refs!
  "Compatibility export for resolve-reply-refs!."
  operations/resolve-reply-refs!)

(def bluesky-publish!
  "Compatibility export for bluesky-publish!."
  operations/bluesky-publish!)

(def publish-execute
  "Compatibility export for publish-execute."
  execution/publish-execute)

(def profile-execute
  "Compatibility export for profile-execute."
  execution/profile-execute)

(def search-execute
  "Compatibility export for search-execute."
  execution/search-execute)

(def author-feed-execute
  "Compatibility export for author-feed-execute."
  execution/author-feed-execute)

(def timeline-execute
  "Compatibility export for timeline-execute."
  execution/timeline-execute)

(def bluesky-repost!
  "Compatibility export for bluesky-repost!."
  operations/bluesky-repost!)

(def bluesky-like!
  "Compatibility export for bluesky-like!."
  operations/bluesky-like!)

(def bluesky-unlike!
  "Compatibility export for bluesky-unlike!."
  operations/bluesky-unlike!)

(def bluesky-follow!
  "Compatibility export for bluesky-follow!."
  operations/bluesky-follow!)

(def bluesky-unfollow!
  "Compatibility export for bluesky-unfollow!."
  operations/bluesky-unfollow!)

(def bluesky-delete-post!
  "Compatibility export for bluesky-delete-post!."
  operations/bluesky-delete-post!)

(def bluesky-thread!
  "Compatibility export for bluesky-thread!."
  operations/bluesky-thread!)

(def bluesky-notifications!
  "Compatibility export for bluesky-notifications!."
  operations/bluesky-notifications!)

(def bluesky-followers!
  "Compatibility export for bluesky-followers!."
  operations/bluesky-followers!)

(def bluesky-follows!
  "Compatibility export for bluesky-follows!."
  operations/bluesky-follows!)

(def bluesky-chat-list!
  "Compatibility export for bluesky-chat-list!."
  operations/bluesky-chat-list!)

(def bluesky-chat-messages!
  "Compatibility export for bluesky-chat-messages!."
  operations/bluesky-chat-messages!)

(def bluesky-chat-send!
  "Compatibility export for bluesky-chat-send!."
  operations/bluesky-chat-send!)

(def bluesky-chat-react!
  "Compatibility export for bluesky-chat-react!."
  operations/bluesky-chat-react!)

(def repost-execute
  "Compatibility export for repost-execute."
  execution/repost-execute)

(def like-execute
  "Compatibility export for like-execute."
  execution/like-execute)

(def unlike-execute
  "Compatibility export for unlike-execute."
  execution/unlike-execute)

(def follow-execute
  "Compatibility export for follow-execute."
  execution/follow-execute)

(def unfollow-execute
  "Compatibility export for unfollow-execute."
  execution/unfollow-execute)

(def delete-execute
  "Compatibility export for delete-execute."
  execution/delete-execute)

(def thread-execute
  "Compatibility export for thread-execute."
  execution/thread-execute)

(def notifications-execute
  "Compatibility export for notifications-execute."
  execution/notifications-execute)

(def followers-execute
  "Compatibility export for followers-execute."
  execution/followers-execute)

(def follows-execute
  "Compatibility export for follows-execute."
  execution/follows-execute)

(def chat-list-execute
  "Compatibility export for chat-list-execute."
  execution/chat-list-execute)

(def chat-send-execute
  "Compatibility export for chat-send-execute."
  execution/chat-send-execute)

(def chat-read-execute
  "Compatibility export for chat-read-execute."
  execution/chat-read-execute)

(def chat-react-execute
  "Compatibility export for chat-react-execute."
  execution/chat-react-execute)

(def publish-tool
  "Compatibility export for publish-tool."
  catalog/publish-tool)

(def profile-tool
  "Compatibility export for profile-tool."
  catalog/profile-tool)

(def search-tool
  "Compatibility export for search-tool."
  catalog/search-tool)

(def author-feed-tool
  "Compatibility export for author-feed-tool."
  catalog/author-feed-tool)

(def timeline-tool
  "Compatibility export for timeline-tool."
  catalog/timeline-tool)

(def repost-tool
  "Compatibility export for repost-tool."
  catalog/repost-tool)

(def like-tool
  "Compatibility export for like-tool."
  catalog/like-tool)

(def unlike-tool
  "Compatibility export for unlike-tool."
  catalog/unlike-tool)

(def follow-tool
  "Compatibility export for follow-tool."
  catalog/follow-tool)

(def unfollow-tool
  "Compatibility export for unfollow-tool."
  catalog/unfollow-tool)

(def delete-tool
  "Compatibility export for delete-tool."
  catalog/delete-tool)

(def thread-tool
  "Compatibility export for thread-tool."
  catalog/thread-tool)

(def notifications-tool
  "Compatibility export for notifications-tool."
  catalog/notifications-tool)

(def followers-tool
  "Compatibility export for followers-tool."
  catalog/followers-tool)

(def follows-tool
  "Compatibility export for follows-tool."
  catalog/follows-tool)

(def chat-list-tool
  "Compatibility export for chat-list-tool."
  catalog/chat-list-tool)

(def chat-send-tool
  "Compatibility export for chat-send-tool."
  catalog/chat-send-tool)

(def chat-read-tool
  "Compatibility export for chat-read-tool."
  catalog/chat-read-tool)

(def chat-react-tool
  "Compatibility export for chat-react-tool."
  catalog/chat-react-tool)

(def bluesky-tool-factories
  "Compatibility export for bluesky-tool-factories."
  catalog/bluesky-tool-factories)

(def create-bluesky-custom-tools
  "Compatibility export for create-bluesky-custom-tools."
  catalog/create-bluesky-custom-tools)

