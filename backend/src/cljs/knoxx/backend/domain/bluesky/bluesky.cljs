(ns knoxx.backend.domain.bluesky.bluesky
  "Bluesky ATProto tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.domain.bluesky.client :as bsky-client]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.text :refer [clip-text tool-text-result]]
            [knoxx.backend.domain.actor.credentials :as actor-credentials]
            [knoxx.backend.domain.media :as media]
            [knoxx.backend.domain.tools :refer [maybe-tool-update! create-tool-obj]]))

(def publish-params
  [:map
   [:text {:description "Bluesky post text. Keep it concise and under platform limits."} :string]
   [:images {:optional true :description "Optional image sources (workspace paths, URLs, or data URLs). Up to 4 images."} [:vector :string]]
   [:imageAlts {:optional true :description "Optional alt text for each image."} [:vector :string]]
   [:replyTo {:optional true :description "Optional AT-URI of a post to reply to."} :string]])

(def profile-params
  [:map
   [:actor {:optional true :description "Optional Bluesky handle or DID. Defaults to the authenticated account."} :string]])

(def search-params
  [:map
   [:query {:description "Search query for Bluesky posts or actors."} :string]
   [:kind {:optional true :description "posts or actors. Defaults to posts."} :string]
   [:limit {:optional true :description "Maximum results to return."} [:int {:min 1 :max 25}]]])

(def feed-params
  [:map
   [:actor {:description "Bluesky handle or DID whose feed should be read."} :string]
   [:limit {:optional true :description "Maximum posts to return."} [:int {:min 1 :max 25}]]])

(def timeline-params
  [:map
   [:limit {:optional true :description "Maximum timeline posts to return."} [:int {:min 1 :max 25}]]
   [:cursor {:optional true :description "Optional pagination cursor from a previous bluesky.timeline call."} :string]])

(def post-uri-params
  [:map
   [:uri {:description "AT-URI of the Bluesky post (at://did/collection/rkey)."} :string]])

(def actor-params
  [:map
   [:actor {:description "Bluesky handle or DID."} :string]])

(def thread-params
  [:map
   [:uri {:description "AT-URI of the root post to read the thread for."} :string]
   [:depth {:optional true :description "Depth of replies to fetch. Default 6."} [:int {:min 1 :max 10}]]])

(def notifications-params
  [:map
   [:limit {:optional true :description "Maximum notifications to return."} [:int {:min 1 :max 50}]]
   [:cursor {:optional true :description "Optional pagination cursor."} :string]])

(def chat-send-params
  [:map
   [:convoId {:description "Conversation ID (from bluesky.chat.list)."} :string]
   [:text {:description "Message text to send."} :string]
   [:replyToMessageId {:optional true :description "Optional message ID to reply to within the conversation."} :string]])

(def chat-list-params
  [:map
   [:limit {:optional true :description "Maximum conversations to return."} [:int {:min 1 :max 50}]]])

(def chat-read-params
  [:map
   [:convoId {:description "Conversation ID (from bluesky.chat.list)."} :string]
   [:limit {:optional true :description "Maximum messages to return."} [:int {:min 1 :max 100}]]])

(def chat-react-params
  [:map
   [:convoId {:description "Conversation ID."} :string]
   [:messageId {:description "Message ID to react to."} :string]
   [:emoji {:description "Emoji reaction (e.g. ❤️, 🔥, 😂)."} :string]])

(defn- bluesky-client []
  (bsky-client/client))

(defn- bluesky-auth-config! [runtime]
  (-> (actor-credentials/get-credential! runtime "bluesky")
      (.then (fn [credential]
               (let [identifier (or (actor-credentials/secret-value credential :identifier :handle :username)
                                    (:accountIdentifier credential))
                     password (actor-credentials/secret-value credential :appPassword :app-password :password)]
                 (when (or (str/blank? (str identifier))
                           (str/blank? (str password)))
                   (throw (js/Error. "Bluesky actor credential must include identifier and appPassword.")))
                 {:identifier identifier :password password})))))

(defn- bluesky-create-session! [runtime]
  (-> (bluesky-auth-config! runtime)
      (.then (fn [credentials]
               (bsky-client/create-session! (bluesky-client) credentials)))))

(defn- bluesky-upload-blob! [session buffer mime-type]
  (bsky-client/upload-blob! (bluesky-client) session buffer mime-type))

(defn- bluesky-create-record! [session collection record]
  (bsky-client/create-record! (bluesky-client) session collection record))

(defn- bluesky-delete-record! [session collection rkey]
  (bsky-client/delete-record! (bluesky-client) session collection rkey))

(defn- parse-at-uri [uri]
  (let [parts (some-> uri str (str/split #"/"))
        repo (nth parts 2 nil)
        collection (nth parts 3 nil)
        rkey (nth parts 4 nil)]
    (when (and repo collection rkey)
      {:repo repo :collection collection :rkey rkey})))

(defn- bluesky-resolve-post! [uri]
  (bsky-client/resolve-post! (bluesky-client) uri))

(defn- bluesky-post-url [handle uri]
  (let [post-id (some-> uri str (str/split #"/") last)]
    (when (and (not (str/blank? (str handle)))
               (not (str/blank? (str post-id))))
      (str "https://bsky.app/profile/" handle "/post/" post-id))))

(defn- format-posts [prefix rows]
  (let [lines (->> rows
                   (map (fn [{:keys [displayName handle text url]}]
                          (str "- " (or (not-empty displayName) handle "unknown")
                               (when (not (str/blank? (str handle))) (str " (@" handle ")"))
                               ": " (clip-text (or text "") 220)
                               (when (not (str/blank? (str url))) (str "\n  " url)))))
                   (str/join "\n"))]
    (str prefix (when-not (str/blank? lines) (str "\n" lines)))))

(defn- bluesky-search! [runtime query kind limit]
  (let [kind (if (= kind "actors") "actors" "posts")]
    (-> (bluesky-create-session! runtime)
        (.then (fn [session]
                 (if (= kind "actors")
                   (bsky-client/search-actors! (bluesky-client) session query limit)
                   (bsky-client/search-posts! (bluesky-client) session query limit))))
        (.then (fn [payload]
                 (if (= kind "actors")
                   (let [results (->> (or (:actors payload) [])
                                      (mapv (fn [actor]
                                              {:handle (or (:handle actor) "")
                                               :displayName (or (:displayName actor) "")
                                               :description (or (:description actor) "")
                                               :did (or (:did actor) "")
                                               :url (when-let [handle (some-> (:handle actor) str not-empty)]
                                                      (str "https://bsky.app/profile/" handle))})))]
                     {:kind kind :results results})
                   (let [results (->> (or (:posts payload) [])
                                      (mapv (fn [post]
                                              (let [author (:author post)
                                                    record (:record post)
                                                    handle (or (:handle author) "")]
                                                {:handle handle
                                                 :displayName (or (:displayName author) "")
                                                 :text (or (:text record) "")
                                                 :createdAt (or (:createdAt record) "")
                                                 :uri (or (:uri post) "")
                                                 :url (bluesky-post-url handle (:uri post))}))))]
                     {:kind kind :results results})))))))

(defn- bluesky-profile! [runtime actor]
  (let [actor (some-> actor str str/trim)
        actor-promise (if (str/blank? actor)
                        (-> (bluesky-create-session! runtime)
                            (.then (fn [session]
                                     (or (:handle session) (:did session)))))
                        (js/Promise.resolve actor))]
    (-> actor-promise
        (.then (fn [resolved-actor]
                 (bsky-client/profile! (bluesky-client) resolved-actor)))
        (.then (fn [profile]
                 {:did (or (:did profile) "")
                  :handle (or (:handle profile) "")
                  :displayName (or (:displayName profile) "")
                  :description (or (:description profile) "")
                  :followersCount (or (:followersCount profile) 0)
                  :followsCount (or (:followsCount profile) 0)
                  :postsCount (or (:postsCount profile) 0)
                  :url (when-let [handle (some-> (:handle profile) str not-empty)]
                         (str "https://bsky.app/profile/" handle))})))))

(defn- bluesky-author-feed! [actor limit]
  (-> (bsky-client/actor-feed! (bluesky-client) nil actor limit)
      (.then (fn [payload]
               (let [results (->> (or (:feed payload) [])
                                  (mapv (fn [entry]
                                          (let [post (:post entry)
                                                author (:author post)
                                                record (:record post)
                                                handle (or (:handle author) "")]
                                            {:handle handle
                                             :displayName (or (:displayName author) "")
                                             :text (or (:text record) "")
                                             :createdAt (or (:createdAt record) "")
                                             :uri (or (:uri post) "")
                                             :url (bluesky-post-url handle (:uri post))}))))]
                 {:actor actor :results results})))))

(defn- bluesky-timeline! [runtime limit cursor]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (bsky-client/timeline! (bluesky-client) session {:limit limit :cursor cursor})))
      (.then (fn [payload]
               (let [results (->> (or (:feed payload) [])
                                  (mapv (fn [entry]
                                          (let [post (:post entry)
                                                author (:author post)
                                                record (:record post)
                                                handle (or (:handle author) "")]
                                            {:handle handle
                                             :displayName (or (:displayName author) "")
                                             :text (or (:text record) "")
                                             :createdAt (or (:createdAt record) "")
                                             :uri (or (:uri post) "")
                                             :url (bluesky-post-url handle (:uri post))}))))]
                 {:cursor (:cursor payload) :results results})))))

(defn- text->utf8-bytes [text]
  (js/Uint8Array.from (js/Array.from (.encode (js/TextEncoder.) text))))

(defn- build-hashtag-facets [text]
  (let [hashtag-re (js/RegExp. "#(\\w+)" "g")
        matches (loop [m (.exec hashtag-re text)
                       acc []]
                  (if (nil? m)
                    acc
                    (let [full-match (aget m 0)
                          tag (aget m 1)
                          start-char (.-index m)
                          prefix (.substring text 0 start-char)
                          start-byte (.-length (text->utf8-bytes prefix))
                          end-byte (+ start-byte (.-length (text->utf8-bytes full-match)))]
                      (recur (.exec hashtag-re text)
                             (conj acc {"$type" "app.bsky.richtext.facet"
                                        :index {:byteStart start-byte :byteEnd end-byte}
                                        :features [{"$type" "app.bsky.richtext.facet#tag"
                                                    :tag tag}]})))))]
    (when (seq matches) matches)))

(defn- load-and-upload-images! [runtime config session images alts]
  (if (seq images)
    (-> (js/Promise.all
         (clj->js
          (map-indexed
           (fn [idx img-src]
             (-> (media/load-media-source! runtime config img-src media/multimodal-upload-max-bytes)
                 (.then (fn [source]
                          (bluesky-upload-blob! session (:buffer source) (:mime-type source))))
                 (.then (fn [blob-result]
                          (let [blob (or (:blob blob-result) blob-result)
                                alt (or (nth alts idx nil) "")]
                            {:alt alt :image blob})))))
           images)))
        (.then (fn [uploaded]
                 {"$type" "app.bsky.embed.images" :images (vec (array-seq uploaded))})))
    (js/Promise.resolve nil)))

(defn- resolve-reply-refs! [reply-to-uri]
  (if (str/blank? reply-to-uri)
    (js/Promise.resolve nil)
    (-> (bluesky-resolve-post! reply-to-uri)
        (.then (fn [parent]
                 {:parent {:uri (:uri parent) :cid (:cid parent)}
                  :root {:uri (:uri parent) :cid (:cid parent)}})))))

(defn- bluesky-publish! [runtime config text images image-alts reply-to]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (-> (promise/all-vec [(load-and-upload-images! runtime config session images image-alts)
                                     (resolve-reply-refs! reply-to)])
                   (.then (fn [[embed reply-refs]]
                            (let [facets (build-hashtag-facets text)
                                  record (cond-> {"$type" "app.bsky.feed.post"
                                                  :text text
                                                  :createdAt (.toISOString (js/Date.))}
                                           facets (assoc :facets facets)
                                           embed (assoc :embed embed)
                                           reply-refs (assoc :reply reply-refs))]
                              (-> (bluesky-create-record! session "app.bsky.feed.post" record)
                                  (.then (fn [result]
                                           (let [uri (or (:uri result) "")]
                                             {:uri uri
                                              :cid (or (:cid result) "")
                                              :url (or (bluesky-post-url (:handle session) uri) "")}))))))))))))

(defn publish-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        text (or (aget params "text") "")
        images (or (aget params "images") [])
        image-alts (or (aget params "imageAlts") [])
        reply-to (or (aget params "replyTo") "")]
    (when (str/blank? (str/trim text))
      (throw (js/Error. "text is required")))
    (maybe-tool-update! on-update "Publishing to Bluesky…")
    (-> (bluesky-publish! runtime config text images image-alts reply-to)
        (.then (fn [result]
                 (tool-text-result (str "Published Bluesky post\n" (or (:url result) (:uri result) ""))
                                   result))))))

(defn profile-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        actor (or (aget params "actor") "")]
    (maybe-tool-update! on-update "Reading Bluesky profile…")
    (-> (bluesky-profile! runtime actor)
        (.then (fn [profile]
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
                  profile))))))

(defn search-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        query (or (aget params "query") "")
        kind (or (aget params "kind") "posts")
        limit (max 1 (min 25 (or (aget params "limit") 5)))]
    (when (str/blank? (str/trim query))
      (throw (js/Error. "query is required")))
    (maybe-tool-update! on-update "Searching Bluesky…")
    (-> (bluesky-search! runtime query kind limit)
        (.then (fn [result]
                 (tool-text-result (format-posts (str "Bluesky search (" (:kind result) ")") (:results result))
                                   result))))))

(defn author-feed-execute [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        actor (or (aget params "actor") "")
        limit (max 1 (min 25 (or (aget params "limit") 8)))]
    (when (str/blank? (str/trim actor))
      (throw (js/Error. "actor is required")))
    (maybe-tool-update! on-update (str "Reading Bluesky feed for " actor "…"))
    (-> (bluesky-author-feed! actor limit)
        (.then (fn [result]
                 (tool-text-result (format-posts (str "Bluesky author feed: " actor) (:results result))
                                   result))))))

(defn timeline-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        limit (max 1 (min 25 (or (aget params "limit") 8)))
        cursor (or (aget params "cursor") "")]
    (maybe-tool-update! on-update "Reading authenticated Bluesky timeline…")
    (-> (bluesky-timeline! runtime limit cursor)
        (.then (fn [result]
                 (tool-text-result (format-posts "Bluesky timeline" (:results result))
                                   result))))))

;; -------------------------------------------------------------------------
;; Repost / Like / Follow / Delete
;; -------------------------------------------------------------------------

(defn- bluesky-repost! [runtime uri]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (-> (bluesky-resolve-post! uri)
                   (.then (fn [post]
                            (bluesky-create-record!
                             session "app.bsky.feed.repost"
                             {"$type" "app.bsky.feed.repost"
                              :subject {:uri (:uri post) :cid (:cid post)}
                              :createdAt (.toISOString (js/Date.))}))))))))

(defn- bluesky-like! [runtime uri]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (-> (bluesky-resolve-post! uri)
                   (.then (fn [post]
                            (bluesky-create-record!
                             session "app.bsky.feed.like"
                             {"$type" "app.bsky.feed.like"
                              :subject {:uri (:uri post) :cid (:cid post)}
                              :createdAt (.toISOString (js/Date.))}))))))))

(defn- bluesky-unlike! [runtime uri]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (let [{:keys [repo collection rkey]} (parse-at-uri uri)]
                 (if (and repo collection rkey)
                   (bluesky-delete-record! session collection rkey)
                   (throw (js/Error. (str "Invalid AT-URI for unlike: " uri)))))))))

(defn- bluesky-follow! [runtime actor]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (-> (bluesky-profile! runtime actor)
                   (.then (fn [profile]
                            (bluesky-create-record!
                             session "app.bsky.graph.follow"
                             {"$type" "app.bsky.graph.follow"
                              :subject (:did profile)
                              :createdAt (.toISOString (js/Date.))}))))))))

(defn- bluesky-unfollow! [runtime uri]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (let [{:keys [repo collection rkey]} (parse-at-uri uri)]
                 (if (and repo collection rkey)
                   (bluesky-delete-record! session collection rkey)
                   (throw (js/Error. (str "Invalid AT-URI for unfollow: " uri)))))))))

(defn- bluesky-delete-post! [runtime uri]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (let [{:keys [repo collection rkey]} (parse-at-uri uri)]
                 (if (and repo collection rkey)
                   (bluesky-delete-record! session collection rkey)
                   (throw (js/Error. (str "Invalid AT-URI for delete: " uri)))))))))

;; -------------------------------------------------------------------------
;; Thread / Notifications / Social lists
;; -------------------------------------------------------------------------

(defn- format-thread-reply [reply depth]
  (let [post (:post reply)
        author (when post (:author post))
        record (when post (:record post))
        handle (or (when author (:handle author)) "")
        display-name (or (when author (:displayName author)) "")
        text (or (when record (:text record)) "")
        uri (or (when post (:uri post)) "")
        indent (str/join "" (repeat depth "  "))]
    (str indent "- " (or (not-empty display-name) handle "unknown")
         (when-not (str/blank? handle) (str " (@" handle ")"))
         ": " (clip-text text 180)
         (when-not (str/blank? uri) (str "\n" indent "  " uri)))))

(defn- collect-thread-replies [thread-node depth max-depth acc]
  (if (> depth max-depth)
    acc
    (reduce (fn [a reply]
              (let [new-acc (conj a (format-thread-reply reply depth))]
                (collect-thread-replies reply (inc depth) max-depth new-acc)))
            acc
            (or (:replies thread-node) []))))

(defn- bluesky-thread! [uri depth]
  (-> (bsky-client/thread! (bluesky-client) nil uri depth)
      (.then (fn [payload]
               (let [thread (:thread payload)
                     root-post (when thread (:post thread))
                     root-author (when root-post (:author root-post))
                     root-record (when root-post (:record root-post))
                     root-handle (or (when root-author (:handle root-author)) "")
                     root-display (or (when root-author (:displayName root-author)) "")
                     root-text (or (when root-record (:text root-record)) "")
                     root-uri (or (when root-post (:uri root-post)) "")
                     root-line (str "- " (or (not-empty root-display) root-handle "unknown")
                                    (when-not (str/blank? root-handle) (str " (@" root-handle ")"))
                                    ": " (clip-text root-text 200)
                                    (when-not (str/blank? root-uri) (str "\n  " root-uri)))
                     reply-lines (collect-thread-replies thread 1 depth [])]
                 {:root {:uri root-uri :text root-text :handle root-handle}
                  :lines (into [root-line] reply-lines)})))))

(defn- bluesky-notifications! [runtime limit]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (bsky-client/notifications! (bluesky-client) session {:limit limit})))))

(defn- bluesky-followers! [actor limit]
  (-> (bsky-client/followers! (bluesky-client) actor limit)
      (.then (fn [payload]
               {:actor actor
                :results (mapv (fn [f]
                                 {:handle (or (:handle f) "")
                                  :displayName (or (:displayName f) "")
                                  :did (or (:did f) "")})
                               (or (:followers payload) []))}))))

(defn- bluesky-follows! [actor limit]
  (-> (bsky-client/follows! (bluesky-client) actor limit)
      (.then (fn [payload]
               {:actor actor
                :results (mapv (fn [f]
                                 {:handle (or (:handle f) "")
                                  :displayName (or (:displayName f) "")
                                  :did (or (:did f) "")})
                               (or (:follows payload) []))}))))

;; -------------------------------------------------------------------------
;; Chat / DM
;; -------------------------------------------------------------------------

(defn- bluesky-chat-list! [runtime limit]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (bsky-client/chat-list! (bluesky-client) session {:limit limit})))))

(defn- bluesky-chat-messages! [runtime convo-id limit]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (bsky-client/chat-read! (bluesky-client) session convo-id {:limit limit})))))

(defn- bluesky-chat-send! [runtime convo-id text reply-to-msg-id]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (let [msg (cond-> {"$type" "chat.bsky.convo.defs#messageInput"
                                  :text text}
                           (not (str/blank? reply-to-msg-id))
                           (assoc :replyTo {"$type" "chat.bsky.convo.defs#messageRef"
                                            :messageId reply-to-msg-id}))]
                 (bsky-client/chat-send! (bluesky-client) session convo-id msg))))))

(defn- bluesky-chat-react! [runtime convo-id message-id emoji]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (bsky-client/chat-react! (bluesky-client) session convo-id message-id emoji)))))

;; -------------------------------------------------------------------------
;; Execute functions for new tools
;; -------------------------------------------------------------------------

(defn repost-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Reposting on Bluesky…")
    (-> (bluesky-repost! runtime uri)
        (.then (fn [result]
                 (tool-text-result (str "Reposted Bluesky post\n" (or (:uri result) uri))
                                   result))))))

(defn like-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Liking Bluesky post…")
    (-> (bluesky-like! runtime uri)
        (.then (fn [result]
                 (tool-text-result (str "Liked Bluesky post\n" (or (:uri result) uri))
                                   result))))))

(defn unlike-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Removing Bluesky like…")
    (-> (bluesky-unlike! runtime uri)
        (.then (fn [_]
                 (tool-text-result (str "Removed like from " uri) {}))))))

(defn follow-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        actor (or (aget params "actor") "")]
    (when (str/blank? actor)
      (throw (js/Error. "actor is required")))
    (maybe-tool-update! on-update (str "Following " actor " on Bluesky…"))
    (-> (bluesky-follow! runtime actor)
        (.then (fn [result]
                 (tool-text-result (str "Followed " actor "\n" (or (:uri result) ""))
                                   result))))))

(defn unfollow-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Unfollowing on Bluesky…")
    (-> (bluesky-unfollow! runtime uri)
        (.then (fn [_]
                 (tool-text-result (str "Unfollowed " uri) {}))))))

(defn delete-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Deleting Bluesky post…")
    (-> (bluesky-delete-post! runtime uri)
        (.then (fn [_]
                 (tool-text-result (str "Deleted Bluesky post " uri) {}))))))

(defn thread-execute [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        uri (or (aget params "uri") "")
        depth (max 1 (min 10 (or (aget params "depth") 6)))]
    (when (str/blank? uri)
      (throw (js/Error. "uri is required")))
    (maybe-tool-update! on-update "Reading Bluesky thread…")
    (-> (bluesky-thread! uri depth)
        (.then (fn [result]
                 (tool-text-result (str "Bluesky thread\n" (str/join "\n" (:lines result)))
                                   result))))))

(defn notifications-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        limit (max 1 (min 50 (or (aget params "limit") 20)))]
    (maybe-tool-update! on-update "Reading Bluesky notifications…")
    (-> (bluesky-notifications! runtime limit)
        (.then (fn [payload]
                 (let [notifications (or (:notifications payload) [])
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
                                     payload)))))))

(defn followers-execute [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        actor (or (aget params "actor") "")
        limit (max 1 (min 50 (or (aget params "limit") 25)))]
    (when (str/blank? actor)
      (throw (js/Error. "actor is required")))
    (maybe-tool-update! on-update (str "Reading followers of " actor "…"))
    (-> (bluesky-followers! actor limit)
        (.then (fn [result]
                 (tool-text-result (format-posts (str "Followers of " actor) (:results result))
                                   result))))))

(defn follows-execute [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        actor (or (aget params "actor") "")
        limit (max 1 (min 50 (or (aget params "limit") 25)))]
    (when (str/blank? actor)
      (throw (js/Error. "actor is required")))
    (maybe-tool-update! on-update (str "Reading who " actor " follows…"))
    (-> (bluesky-follows! actor limit)
        (.then (fn [result]
                 (tool-text-result (format-posts (str "Follows of " actor) (:results result))
                                   result))))))

(defn chat-list-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        limit (max 1 (min 50 (or (aget params "limit") 20)))]
    (maybe-tool-update! on-update "Listing Bluesky conversations…")
    (-> (bluesky-chat-list! runtime limit)
        (.then (fn [payload]
                 (let [convos (or (:convos payload) [])
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
                                     payload)))))))

(defn chat-send-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        convo-id (or (aget params "convoId") "")
        text (or (aget params "text") "")
        reply-to (or (aget params "replyToMessageId") "")]
    (when (str/blank? convo-id)
      (throw (js/Error. "convoId is required")))
    (when (str/blank? text)
      (throw (js/Error. "text is required")))
    (maybe-tool-update! on-update "Sending Bluesky DM…")
    (-> (bluesky-chat-send! runtime convo-id text reply-to)
        (.then (fn [result]
                 (tool-text-result (str "Sent DM in conversation " convo-id)
                                   result))))))

(defn chat-read-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        convo-id (or (aget params "convoId") "")
        limit (max 1 (min 100 (or (aget params "limit") 25)))]
    (when (str/blank? convo-id)
      (throw (js/Error. "convoId is required")))
    (maybe-tool-update! on-update (str "Reading Bluesky DMs in " convo-id "…"))
    (-> (bluesky-chat-messages! runtime convo-id limit)
        (.then (fn [payload]
                 (let [messages (or (:messages payload) [])
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
                                     payload)))))))

(defn chat-react-execute [runtime _config _tool-call-id params a b c]
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
    (-> (bluesky-chat-react! runtime convo-id message-id emoji)
        (.then (fn [result]
                 (tool-text-result (str "Reacted " emoji " to message " message-id)
                                   result))))))

(def publish-tool
  (partial create-tool-obj
     "bluesky.publish" "Bluesky Publish"
     "Publish a post to Bluesky using the configured account."
     "Post a concise update to Bluesky when public social publishing is useful."
     ["Use for original posts, replies (replyTo), and image posts (images param)."
      "Always include relevant hashtags in the text body; they will be auto-faceted."
      "For image posts, provide workspace paths, URLs, or data URLs in the images vector."
      "When replying, pass the parent post AT-URI as replyTo."]
     publish-params
     publish-execute))

(def profile-tool
  (partial create-tool-obj
     "bluesky.profile" "Bluesky Profile"
     "Read a Bluesky profile by handle or DID, or default to the authenticated account."
     "Read a Bluesky profile by handle or DID, or default to the authenticated account."
     ["Use to inspect a user's bio, follower count, and posts before engaging."
      "Leave actor empty to read the authenticated account's own profile."
      "Accepts handles like @alice.bsky.social or raw DIDs."]
     profile-params
     profile-execute))

(def search-tool
  (partial create-tool-obj
     "bluesky.search" "Bluesky Search"
     "Search public Bluesky posts or actors."
     "Search public Bluesky posts or actors."
     ["Use to discover trending topics, find inspiration, or locate specific posts."
      "Default kind is posts; set kind to actors to search for users."
      "Hashtag searches work best with the # prefix: #generative, #music, etc."]
     search-params
     search-execute))

(def author-feed-tool
  (partial create-tool-obj
     "bluesky.author.feed" "Bluesky Author Feed"
     "Read recent posts from a specific Bluesky author."
     "Read recent posts from a specific Bluesky author."
     ["Use to browse a specific creator's recent posts before interacting."
      "Pass the actor's handle or DID."
      "Great for finding content to reply to, repost, or draw inspiration from."]
     feed-params
     author-feed-execute))

(def timeline-tool
  (partial create-tool-obj
     "bluesky.timeline" "Bluesky Timeline"
     "Read the authenticated account's Bluesky timeline."
     "Read the authenticated account's Bluesky timeline."
     ["Use CONSTANTLY to stay aware of the current vibe and trending content."
      "This is your primary source of context before posting or engaging."
      "Pass cursor from a previous call to paginate further back."]
     timeline-params
     timeline-execute))

(def repost-tool
  (partial create-tool-obj
            "bluesky.repost" "Bluesky Repost"
            "Repost (quote-retweet) a Bluesky post by AT-URI."
            "Repost a Bluesky post to share it with followers."
            ["Use when you want to amplify content you find valuable or entertaining."
             "Pass the full AT-URI of the post to repost."
             "Reposting builds social capital and signals taste to your followers."]
            post-uri-params
            repost-execute))

(def like-tool
  (partial create-tool-obj
            "bluesky.like" "Bluesky Like"
            "Like a Bluesky post by AT-URI."
            "Like a Bluesky post to show appreciation."
            ["Use generously to signal engagement and build goodwill."
             "Pass the full AT-URI of the post to like."
             "Liking is low-cost social capital; do it often for posts that resonate."]
            post-uri-params
            like-execute))

(def unlike-tool
  (partial create-tool-obj
            "bluesky.unlike" "Bluesky Unlike"
            "Remove a like from a Bluesky post by like record AT-URI."
            "Remove a like from a Bluesky post."
            ["Use to remove a previous like."
             "Pass the AT-URI of the like record itself (not the post URI)."]
            post-uri-params
            unlike-execute))

(def follow-tool
  (partial create-tool-obj
            "bluesky.follow" "Bluesky Follow"
            "Follow a Bluesky actor by handle or DID."
            "Follow a Bluesky user."
            ["Use to follow creators whose content you want to see in your timeline."
             "Pass the handle or DID of the actor."
             "Aggressively curate your following list; unfollow if content quality drops."]
            actor-params
            follow-execute))

(def unfollow-tool
  (partial create-tool-obj
            "bluesky.unfollow" "Bluesky Unfollow"
            "Unfollow a Bluesky actor by follow record AT-URI."
            "Unfollow a Bluesky user."
            ["Use to unfollow an actor."
             "Pass the AT-URI of the follow record (not the actor handle)."]
            post-uri-params
            unfollow-execute))

(def delete-tool
  (partial create-tool-obj
            "bluesky.delete" "Bluesky Delete"
            "Delete one of your own Bluesky posts by AT-URI."
            "Delete a Bluesky post you authored."
            ["Use to remove posts that flopped, aged poorly, or were mistakes."
             "Pass the AT-URI of your own post."
             "Curate your feed like a gallery; delete without remorse."]
            post-uri-params
            delete-execute))

(def thread-tool
  (partial create-tool-obj
            "bluesky.thread" "Bluesky Thread"
            "Read a Bluesky post thread including replies."
            "Read a post and its reply thread."
            ["Use to read full conversation threads before jumping in."
             "Pass the AT-URI of the root post."
             "Adjust depth for deeper reply trees; default is 6."]
            thread-params
            thread-execute))

(def notifications-tool
  (partial create-tool-obj
            "bluesky.notifications" "Bluesky Notifications"
            "Read notifications for the authenticated Bluesky account."
            "Check notifications on Bluesky."
            ["Use frequently to stay on top of replies, mentions, likes, and follows."
             "Engagement compounds; respond to replies and mentions promptly."
             "Notifications reveal who is interacting with you and why."]
            notifications-params
            notifications-execute))

(def followers-tool
  (partial create-tool-obj
     "bluesky.followers" "Bluesky Followers"
     "List followers of a Bluesky actor."
     "Read the followers list of a Bluesky user."
     ["Use to inspect who follows a given actor."
      "Pass the actor's handle or DID."
      "Useful for understanding audience overlap and community composition."]
     feed-params
     followers-execute))

(def follows-tool
  (partial create-tool-obj
     "bluesky.follows" "Bluesky Follows"
     "List accounts a Bluesky actor follows."
     "Read the following list of a Bluesky user."
     ["Use to discover who a creator follows for taste graph exploration."
      "Pass the actor's handle or DID."
      "Great for finding new creators to follow via transitive taste."]
     feed-params
     follows-execute))

(def chat-list-tool
  (partial create-tool-obj
     "bluesky.chat.list" "Bluesky Chat List"
     "List Bluesky DM conversations."
     "List direct message conversations."
     ["Use to see active DM conversations and their last messages."
      "Start here before reading or sending DMs to get the conversation IDs."]
     chat-list-params
     chat-list-execute))

(def chat-send-tool
  (partial create-tool-obj
     "bluesky.chat.send" "Bluesky Chat Send"
     "Send a direct message in a Bluesky conversation."
     "Send a DM in a Bluesky chat."
     ["Use to reply in DM threads."
      "Pass the convoId from chat.list and the message text."
      "Use replyToMessageId to thread replies within a conversation."]
     chat-send-params
     chat-send-execute))

(def chat-read-tool
  (partial create-tool-obj
     "bluesky.chat.read" "Bluesky Chat Read"
     "Read messages from a Bluesky DM conversation."
     "Read DMs in a Bluesky conversation."
     ["Use to read the message history of a specific DM conversation."
      "Pass the convoId from chat.list."
      "Check DMs regularly; they may contain collaboration invites or feedback."]
     chat-read-params
     chat-read-execute))

(def chat-react-tool
  (partial create-tool-obj
     "bluesky.chat.react" "Bluesky Chat React"
     "Add an emoji reaction to a message in a Bluesky DM."
     "React to a Bluesky DM message."
     ["Use to react to DM messages with emoji."
      "Pass convoId, messageId (from chat.read), and an emoji like ❤️ or 🔥."
      "Reactions are lightweight engagement for DMs."]
     chat-react-params
     chat-react-execute))

(defn create-bluesky-custom-tools
  ([runtime config] (create-bluesky-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "bluesky.publish")
                  (publish-tool runtime config))
                (when (allowed? "bluesky.profile")
                  (profile-tool runtime config))
                (when (allowed? "bluesky.search")
                  (search-tool runtime config))
                (when (allowed? "bluesky.author.feed")
                  (author-feed-tool runtime config))
                (when (allowed? "bluesky.timeline")
                  (timeline-tool runtime config))
                (when (allowed? "bluesky.repost")
                  (repost-tool runtime config))
                (when (allowed? "bluesky.like")
                  (like-tool runtime config))
                (when (allowed? "bluesky.unlike")
                  (unlike-tool runtime config))
                (when (allowed? "bluesky.follow")
                  (follow-tool runtime config))
                (when (allowed? "bluesky.unfollow")
                  (unfollow-tool runtime config))
                (when (allowed? "bluesky.delete")
                  (delete-tool runtime config))
                (when (allowed? "bluesky.thread")
                  (thread-tool runtime config))
                (when (allowed? "bluesky.notifications")
                  (notifications-tool runtime config))
                (when (allowed? "bluesky.followers")
                  (followers-tool runtime config))
                (when (allowed? "bluesky.follows")
                  (follows-tool runtime config))
                (when (allowed? "bluesky.chat.list")
                  (chat-list-tool runtime config))
                (when (allowed? "bluesky.chat.read")
                  (chat-read-tool runtime config))
                (when (allowed? "bluesky.chat.send")
                  (chat-send-tool runtime config))
                (when (allowed? "bluesky.chat.react")
                  (chat-react-tool runtime config))]))))))
