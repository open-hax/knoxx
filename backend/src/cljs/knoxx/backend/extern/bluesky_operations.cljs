(ns knoxx.backend.extern.bluesky-operations
  "Bluesky remote operations, native media and clock boundary."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.bluesky.client :as bsky-client]
            [knoxx.backend.domain.media :as media]
            [knoxx.backend.domain.text :refer [clip-text]]
            [knoxx.backend.extern.bluesky-facets :as facets]
            [knoxx.backend.infra.actor.credentials :as actor-credentials]
            [knoxx.backend.shape.bluesky :as shape]))

(defn- bluesky-client []
  (bsky-client/client))

(defn ^:async bluesky-auth-config!
  "Bluesky boundary operation: bluesky-auth-config!." [runtime]
  (let [credential (await (actor-credentials/get-credential! runtime "bluesky"))
        identifier (or (actor-credentials/secret-value credential :identifier :handle :username)
                       (:accountIdentifier credential))
        password (actor-credentials/secret-value credential :appPassword :app-password :password)]
    (when (or (str/blank? (str identifier))
              (str/blank? (str password)))
      (throw (js/Error. "Bluesky actor credential must include identifier and appPassword.")))
    {:identifier identifier :password password}))

(defn ^:async bluesky-create-session!
  "Bluesky boundary operation: bluesky-create-session!." [runtime]
  (let [credentials (await (bluesky-auth-config! runtime))]
    (await (bsky-client/create-session! (bluesky-client) credentials))))

(defn- bluesky-upload-blob! [session buffer mime-type]
  (bsky-client/upload-blob! (bluesky-client) session buffer mime-type))

(defn- bluesky-create-record! [session collection record]
  (bsky-client/create-record! (bluesky-client) session collection record))

(defn- bluesky-delete-record! [session collection rkey]
  (bsky-client/delete-record! (bluesky-client) session collection rkey))

(defn- bluesky-resolve-post! [uri]
  (bsky-client/resolve-post! (bluesky-client) uri))

(defn ^:async bluesky-search!
  "Bluesky boundary operation: bluesky-search!." [runtime query kind limit]
  (let [kind (if (= kind "actors") "actors" "posts")
        session (await (bluesky-create-session! runtime))
        payload (if (= kind "actors")
                  (await (bsky-client/search-actors! (bluesky-client) session query limit))
                  (await (bsky-client/search-posts! (bluesky-client) session query limit)))]
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
                                    :url (shape/bluesky-post-url handle (:uri post))}))))]
        {:kind kind :results results}))))

(defn ^:async bluesky-profile!
  "Bluesky boundary operation: bluesky-profile!." [runtime actor]
  (let [actor (some-> actor str str/trim)
        resolved-actor (if (str/blank? actor)
                         (let [session (await (bluesky-create-session! runtime))]
                           (or (:handle session) (:did session)))
                         actor)
        profile (await (bsky-client/profile! (bluesky-client) resolved-actor))]
    {:did (or (:did profile) "")
     :handle (or (:handle profile) "")
     :displayName (or (:displayName profile) "")
     :description (or (:description profile) "")
     :followersCount (or (:followersCount profile) 0)
     :followsCount (or (:followsCount profile) 0)
     :postsCount (or (:postsCount profile) 0)
     :url (when-let [handle (some-> (:handle profile) str not-empty)]
            (str "https://bsky.app/profile/" handle))}))

(defn ^:async bluesky-author-feed!
  "Bluesky boundary operation: bluesky-author-feed!." [actor limit]
  (let [payload (await (bsky-client/actor-feed! (bluesky-client) nil actor limit))
        results (->> (or (:feed payload) [])
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
                                :url (shape/bluesky-post-url handle (:uri post))}))))]
    {:actor actor :results results}))

(defn ^:async bluesky-timeline!
  "Bluesky boundary operation: bluesky-timeline!." [runtime limit cursor]
  (let [session (await (bluesky-create-session! runtime))
        payload (await (bsky-client/timeline! (bluesky-client) session {:limit limit :cursor cursor}))
        results (->> (or (:feed payload) [])
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
                                :url (shape/bluesky-post-url handle (:uri post))}))))]
    {:cursor (:cursor payload) :results results}))

(defn ^:async load-and-upload-image!
  "Bluesky boundary operation: load-and-upload-image!." [runtime config session alts idx img-src]
  (let [source (await (media/load-media-source! runtime config img-src media/multimodal-upload-max-bytes))
        blob-result (await (bluesky-upload-blob! session (:buffer source) (:mime-type source)))
        blob (or (:blob blob-result) blob-result)
        alt (or (nth alts idx nil) "")]
    {:alt alt :image blob}))

(defn ^:async load-and-upload-images!
  "Bluesky boundary operation: load-and-upload-images!." [runtime config session images alts]
  (when (seq images)
    (let [uploaded (await (js/Promise.all
                           (clj->js
                            (map-indexed
                             (partial load-and-upload-image! runtime config session alts)
                             images))))]
      {"$type" "app.bsky.embed.images" :images (vec (array-seq uploaded))})))

(defn ^:async resolve-reply-refs!
  "Bluesky boundary operation: resolve-reply-refs!." [reply-to-uri]
  (when-not (str/blank? reply-to-uri)
    (let [parent (await (bluesky-resolve-post! reply-to-uri))]
      {:parent {:uri (:uri parent) :cid (:cid parent)}
       :root {:uri (:uri parent) :cid (:cid parent)}})))

(defn ^:async bluesky-publish!
  "Bluesky boundary operation: bluesky-publish!." [runtime config text images image-alts reply-to]
  (let [session (await (bluesky-create-session! runtime))
        embed (await (load-and-upload-images! runtime config session images image-alts))
        reply-refs (await (resolve-reply-refs! reply-to))
        facets (facets/build-facets text)
        record (cond-> {"$type" "app.bsky.feed.post"
                        :text text
                        :createdAt (.toISOString (js/Date.))}
                 facets (assoc :facets facets)
                 embed (assoc :embed embed)
                 reply-refs (assoc :reply reply-refs))
        result (await (bluesky-create-record! session "app.bsky.feed.post" record))
        uri (or (:uri result) "")]
    {:uri uri
     :cid (or (:cid result) "")
     :url (or (shape/bluesky-post-url (:handle session) uri) "")}))

(defn ^:async bluesky-repost!
  "Bluesky boundary operation: bluesky-repost!." [runtime uri]
  (let [session (await (bluesky-create-session! runtime))
        post (await (bluesky-resolve-post! uri))]
    (await (bluesky-create-record!
            session "app.bsky.feed.repost"
            {"$type" "app.bsky.feed.repost"
             :subject {:uri (:uri post) :cid (:cid post)}
             :createdAt (.toISOString (js/Date.))}))))

(defn ^:async bluesky-like!
  "Bluesky boundary operation: bluesky-like!." [runtime uri]
  (let [session (await (bluesky-create-session! runtime))
        post (await (bluesky-resolve-post! uri))]
    (await (bluesky-create-record!
            session "app.bsky.feed.like"
            {"$type" "app.bsky.feed.like"
             :subject {:uri (:uri post) :cid (:cid post)}
             :createdAt (.toISOString (js/Date.))}))))

(defn ^:async bluesky-unlike!
  "Bluesky boundary operation: bluesky-unlike!." [runtime uri]
  (let [session (await (bluesky-create-session! runtime))
        {:keys [repo collection rkey]} (shape/parse-at-uri uri)]
    (if (and repo collection rkey)
      (await (bluesky-delete-record! session collection rkey))
      (throw (js/Error. (str "Invalid AT-URI for unlike: " uri))))))

(defn ^:async bluesky-follow!
  "Bluesky boundary operation: bluesky-follow!." [runtime actor]
  (let [session (await (bluesky-create-session! runtime))
        profile (await (bluesky-profile! runtime actor))]
    (await (bluesky-create-record!
            session "app.bsky.graph.follow"
            {"$type" "app.bsky.graph.follow"
             :subject (:did profile)
             :createdAt (.toISOString (js/Date.))}))))

(defn ^:async bluesky-unfollow!
  "Bluesky boundary operation: bluesky-unfollow!." [runtime uri]
  (let [session (await (bluesky-create-session! runtime))
        {:keys [repo collection rkey]} (shape/parse-at-uri uri)]
    (if (and repo collection rkey)
      (await (bluesky-delete-record! session collection rkey))
      (throw (js/Error. (str "Invalid AT-URI for unfollow: " uri))))))

(defn ^:async bluesky-delete-post!
  "Bluesky boundary operation: bluesky-delete-post!." [runtime uri]
  (let [session (await (bluesky-create-session! runtime))
        {:keys [repo collection rkey]} (shape/parse-at-uri uri)]
    (if (and repo collection rkey)
      (await (bluesky-delete-record! session collection rkey))
      (throw (js/Error. (str "Invalid AT-URI for delete: " uri))))))

;; -------------------------------------------------------------------------
;; Thread / Notifications / Social lists
;; -------------------------------------------------------------------------

(defn ^:async bluesky-thread!
  "Bluesky boundary operation: bluesky-thread!." [uri depth]
  (let [payload (await (bsky-client/thread! (bluesky-client) nil uri depth))
        thread (:thread payload)
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
        reply-lines (shape/collect-thread-replies thread 1 depth [])]
    {:root {:uri root-uri :text root-text :handle root-handle}
     :lines (into [root-line] reply-lines)}))

(defn ^:async bluesky-notifications!
  "Bluesky boundary operation: bluesky-notifications!." [runtime limit]
  (let [session (await (bluesky-create-session! runtime))]
    (await (bsky-client/notifications! (bluesky-client) session {:limit limit}))))

(defn ^:async bluesky-followers!
  "Bluesky boundary operation: bluesky-followers!." [actor limit]
  (let [payload (await (bsky-client/followers! (bluesky-client) actor limit))]
    {:actor actor
     :results (mapv (fn [f]
                      {:handle (or (:handle f) "")
                       :displayName (or (:displayName f) "")
                       :did (or (:did f) "")})
                    (or (:followers payload) []))}))

(defn ^:async bluesky-follows!
  "Bluesky boundary operation: bluesky-follows!." [actor limit]
  (let [payload (await (bsky-client/follows! (bluesky-client) actor limit))]
    {:actor actor
     :results (mapv (fn [f]
                      {:handle (or (:handle f) "")
                       :displayName (or (:displayName f) "")
                       :did (or (:did f) "")})
                    (or (:follows payload) []))}))

;; -------------------------------------------------------------------------
;; Chat / DM
;; -------------------------------------------------------------------------

(defn ^:async bluesky-chat-list!
  "Bluesky boundary operation: bluesky-chat-list!." [runtime limit]
  (let [session (await (bluesky-create-session! runtime))]
    (await (bsky-client/chat-list! (bluesky-client) session {:limit limit}))))

(defn ^:async bluesky-chat-messages!
  "Bluesky boundary operation: bluesky-chat-messages!." [runtime convo-id limit]
  (let [session (await (bluesky-create-session! runtime))]
    (await (bsky-client/chat-read! (bluesky-client) session convo-id {:limit limit}))))

(defn ^:async bluesky-chat-send!
  "Bluesky boundary operation: bluesky-chat-send!." [runtime convo-id text reply-to-msg-id]
  (let [session (await (bluesky-create-session! runtime))
        msg (cond-> {"$type" "chat.bsky.convo.defs#messageInput"
                     :text text}
              (not (str/blank? reply-to-msg-id))
              (assoc :replyTo {"$type" "chat.bsky.convo.defs#messageRef"
                               :messageId reply-to-msg-id}))]
    (await (bsky-client/chat-send! (bluesky-client) session convo-id msg))))

(defn ^:async bluesky-chat-react!
  "Bluesky boundary operation: bluesky-chat-react!." [runtime convo-id message-id emoji]
  (let [session (await (bluesky-create-session! runtime))]
    (await (bsky-client/chat-react! (bluesky-client) session convo-id message-id emoji))))

;; -------------------------------------------------------------------------
;; Execute functions for new tools
;; -------------------------------------------------------------------------

