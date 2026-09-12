(ns knoxx.backend.extern.bluesky-tool-execution
  "Decode Bluesky SDK arguments and encode tool results."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.text :refer [clip-text tool-text-result]]
            [knoxx.backend.domain.tools :refer [maybe-tool-update!]]
            [knoxx.backend.extern.bluesky-operations :as operations]
            [knoxx.backend.shape.bluesky :as shape]))

(defn ^:async publish-execute
  "SDK execution for publish-execute." [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        text (or (aget params "text") "")
        images (or (aget params "images") [])
        image-alts (or (aget params "imageAlts") [])
        reply-to (or (aget params "replyTo") "")]
    (when (str/blank? (str/trim text))
      (throw (js/Error. "text is required")))
    (maybe-tool-update! on-update "Publishing to Bluesky…")
    (let [result (await (operations/bluesky-publish! runtime config text images image-alts reply-to))]
      (tool-text-result (str "Published Bluesky post\n" (or (:url result) (:uri result) ""))
                        result))))

(defn ^:async profile-execute
  "SDK execution for profile-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        actor (or (aget params "actor") "")]
    (maybe-tool-update! on-update "Reading Bluesky profile…")
    (let [profile (await (operations/bluesky-profile! runtime actor))]
      (tool-text-result
       (str "Bluesky profile: " (or (:displayName profile) (:handle profile) "unknown")
            (when-not (str/blank? (str (:handle profile))) (str " (@" (:handle profile) ")"))
            "\nFollowers: " (:followersCount profile)
            " | Following: " (:followsCount profile)
            " | Posts: " (:postsCount profile)
            (when-not (str/blank? (str (:description profile)))
              (str "\n\n" (:description profile)))
            (when-not (str/blank? (str (:url profile)))
              (str "\n\n" (:url profile))))
       profile))))

(defn ^:async search-execute
  "SDK execution for search-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        query (or (aget params "query") "")
        kind (or (aget params "kind") "posts")
        limit (max 1 (min 25 (or (aget params "limit") 5)))]
    (when (str/blank? (str/trim query))
      (throw (js/Error. "query is required")))
    (maybe-tool-update! on-update "Searching Bluesky…")
    (let [result (await (operations/bluesky-search! runtime query kind limit))]
      (tool-text-result (shape/format-posts (str "Bluesky search (" (:kind result) ")") (:results result))
                        result))))

(defn ^:async author-feed-execute
  "SDK execution for author-feed-execute." [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        actor (or (aget params "actor") "")
        limit (max 1 (min 25 (or (aget params "limit") 8)))]
    (when (str/blank? (str/trim actor))
      (throw (js/Error. "actor is required")))
    (maybe-tool-update! on-update (str "Reading Bluesky feed for " actor "…"))
    (let [result (await (operations/bluesky-author-feed! actor limit))]
      (tool-text-result (shape/format-posts (str "Bluesky author feed: " actor) (:results result))
                        result))))

(defn ^:async timeline-execute
  "SDK execution for timeline-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        limit (max 1 (min 25 (or (aget params "limit") 8)))
        cursor (or (aget params "cursor") "")]
    (maybe-tool-update! on-update "Reading authenticated Bluesky timeline…")
    (let [result (await (operations/bluesky-timeline! runtime limit cursor))]
      (tool-text-result (shape/format-posts "Bluesky timeline" (:results result))
                        result))))

;; -------------------------------------------------------------------------
;; Repost / Like / Follow / Delete
;; -------------------------------------------------------------------------

(defn ^:async repost-execute
  "SDK execution for repost-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Reposting on Bluesky…")
    (let [result (await (operations/bluesky-repost! runtime uri))]
      (tool-text-result (str "Reposted Bluesky post\n" (or (:uri result) uri))
                        result))))

(defn ^:async like-execute
  "SDK execution for like-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Liking Bluesky post…")
    (let [result (await (operations/bluesky-like! runtime uri))]
      (tool-text-result (str "Liked Bluesky post\n" (or (:uri result) uri))
                        result))))

(defn ^:async unlike-execute
  "SDK execution for unlike-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Removing Bluesky like…")
    (await (operations/bluesky-unlike! runtime uri))
    (tool-text-result (str "Removed like from " uri) {})))

(defn ^:async follow-execute
  "SDK execution for follow-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        actor (or (aget params "actor") "")]
    (when (str/blank? actor)
      (throw (js/Error. "actor is required")))
    (maybe-tool-update! on-update (str "Following " actor " on Bluesky…"))
    (let [result (await (operations/bluesky-follow! runtime actor))]
      (tool-text-result (str "Followed " actor "\n" (or (:uri result) ""))
                        result))))

(defn ^:async unfollow-execute
  "SDK execution for unfollow-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Unfollowing on Bluesky…")
    (await (operations/bluesky-unfollow! runtime uri))
    (tool-text-result (str "Unfollowed " uri) {})))

(defn ^:async delete-execute
  "SDK execution for delete-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Deleting Bluesky post…")
    (await (operations/bluesky-delete-post! runtime uri))
    (tool-text-result (str "Deleted Bluesky post " uri) {})))

(defn ^:async thread-execute
  "SDK execution for thread-execute." [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")
        depth (max 1 (min 10 (or (aget params "depth") 6)))]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Reading Bluesky thread…")
    (let [result (await (operations/bluesky-thread! uri depth))]
      (tool-text-result (str "Bluesky thread\n" (str/join "\n" (:lines result)))
                        result))))

(defn ^:async notifications-execute
  "SDK execution for notifications-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        limit (max 1 (min 50 (or (aget params "limit") 20)))]
    (maybe-tool-update! on-update "Reading Bluesky notifications…")
    (let [payload (await (operations/bluesky-notifications! runtime limit))
          notifications (or (:notifications payload) [])
          lines (mapv (fn [n]
                        (let [author (:author n)
                              reason (:reason n)
                              post (:post n)
                              text (or (get-in post [:record :text]) "")]
                          (str "- [" reason "] "
                               (or (:displayName author) "")
                               " (@" (or (:handle author) "") "): "
                               (clip-text text 120))))
                      notifications)]
      (tool-text-result (str "Bluesky notifications (" (count notifications) ")\n"
                             (str/join "\n" lines))
                        payload))))

(defn ^:async followers-execute
  "SDK execution for followers-execute." [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        actor (or (aget params "actor") "")
        limit (max 1 (min 50 (or (aget params "limit") 25)))]
    (when (str/blank? actor)
      (throw (js/Error. "actor is required")))
    (maybe-tool-update! on-update (str "Reading followers of " actor "…"))
    (let [result (await (operations/bluesky-followers! actor limit))]
      (tool-text-result (shape/format-posts (str "Followers of " actor) (:results result))
                        result))))

(defn ^:async follows-execute
  "SDK execution for follows-execute." [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        actor (or (aget params "actor") "")
        limit (max 1 (min 50 (or (aget params "limit") 25)))]
    (when (str/blank? actor)
      (throw (js/Error. "actor is required")))
    (maybe-tool-update! on-update (str "Reading who " actor " follows…"))
    (let [result (await (operations/bluesky-follows! actor limit))]
      (tool-text-result (shape/format-posts (str "Follows of " actor) (:results result))
                        result))))

(defn ^:async chat-list-execute
  "SDK execution for chat-list-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        limit (max 1 (min 50 (or (aget params "limit") 20)))]
    (maybe-tool-update! on-update "Listing Bluesky conversations…")
    (let [payload (await (operations/bluesky-chat-list! runtime limit))
          convos (or (:convos payload) [])
          lines (mapv (fn [convo]
                        (let [members (or (:members convo) [])
                              names (str/join ", " (map #(or (:displayName %) (:handle %) "") members))
                              last-msg (:lastMessage convo)]
                          (str "- " (:id convo) ": " names
                               (when last-msg
                                 (str " — " (clip-text (or (:text last-msg) "") 80))))))
                      convos)]
      (tool-text-result (str "Bluesky conversations (" (count convos) ")\n"
                             (str/join "\n" lines))
                        payload))))

(defn ^:async chat-send-execute
  "SDK execution for chat-send-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        convo-id (or (aget params "convoId") "")
        text (or (aget params "text") "")
        reply-to (or (aget params "replyToMessageId") "")]
    (when (str/blank? convo-id)
      (throw (js/Error. "convoId is required")))
    (when (str/blank? text)
      (throw (js/Error. "text is required")))
    (maybe-tool-update! on-update "Sending Bluesky DM…")
    (let [result (await (operations/bluesky-chat-send! runtime convo-id text reply-to))]
      (tool-text-result (str "Sent DM in conversation " convo-id)
                        result))))

(defn ^:async chat-read-execute
  "SDK execution for chat-read-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        convo-id (or (aget params "convoId") "")
        limit (max 1 (min 100 (or (aget params "limit") 25)))]
    (when (str/blank? convo-id)
      (throw (js/Error. "convoId is required")))
    (maybe-tool-update! on-update (str "Reading Bluesky DMs in " convo-id "…"))
    (let [payload (await (operations/bluesky-chat-messages! runtime convo-id limit))
          messages (or (:messages payload) [])
          lines (mapv (fn [msg]
                        (let [sender (:sender msg)
                              text (or (:text msg) "")
                              msg-id (or (:id msg) "")]
                          (str "- [" msg-id "] "
                               (or (:displayName sender) "")
                               " (@" (or (:handle sender) "") "): "
                               (clip-text text 200))))
                      messages)]
      (tool-text-result (str "Bluesky DMs (" (count messages) ")\n"
                             (str/join "\n" lines))
                        payload))))

(defn ^:async chat-react-execute
  "SDK execution for chat-react-execute." [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        convo-id (or (aget params "convoId") "")
        message-id (or (aget params "messageId") "")
        emoji (or (aget params "emoji") "")]
    (when (str/blank? convo-id)
      (throw (js/Error. "convoId is required")))
    (when (str/blank? message-id)
      (throw (js/Error. "messageId is required")))
    (when (str/blank? emoji)
      (throw (js/Error. "emoji is required")))
    (maybe-tool-update! on-update (str "Reacting to message " message-id "…"))
    (let [result (await (operations/bluesky-chat-react! runtime convo-id message-id emoji))]
      (tool-text-result (str "Reacted " emoji " to message " message-id)
                        result))))

