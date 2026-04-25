(ns knoxx.backend.tools.bluesky
  "Bluesky ATProto tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [clip-text tool-text-result]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional]]))

(defn- env-nonblank
  [k]
  (some-> (aget js/process.env k) str str/trim not-empty))

(defn- bluesky-service-base-url
  []
  (or (env-nonblank "BLUESKY_SERVICE_URL")
      "https://bsky.social"))

(defn- bluesky-public-base-url
  []
  (or (env-nonblank "BLUESKY_PUBLIC_API_URL")
      "https://public.api.bsky.app"))

(defn- bluesky-json-fetch!
  [url options label]
  (-> (js/fetch url options)
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str label " error " (.-status resp) ": " text)))))))))))

(defn- bluesky-auth-config
  []
  (let [identifier (env-nonblank "BLUESKY_IDENTIFIER")
        password (env-nonblank "BLUESKY_APP_PASSWORD")]
    (when (or (nil? identifier) (nil? password))
      (throw (js/Error. "Bluesky credentials not configured. Set BLUESKY_IDENTIFIER and BLUESKY_APP_PASSWORD.")))
    {:identifier identifier
     :password password}))

(defn- bluesky-create-session!
  []
  (let [{:keys [identifier password]} (bluesky-auth-config)]
    (bluesky-json-fetch!
     (str (bluesky-service-base-url) "/xrpc/com.atproto.server.createSession")
     #js {:method "POST"
          :headers #js {"Content-Type" "application/json"
                        "User-Agent" "Knoxx-Agent/1.0"}
          :body (.stringify js/JSON #js {:identifier identifier
                                         :password password})}
     "Bluesky auth")))

(defn- bluesky-post-url
  [handle uri]
  (let [post-id (some-> uri str (str/split #"/") last)]
    (when (and (not (str/blank? (str handle)))
               (not (str/blank? (str post-id))))
      (str "https://bsky.app/profile/" handle "/post/" post-id))))

(defn- bluesky-search!
  [query kind limit]
  (let [kind (if (= kind "actors") "actors" "posts")
        endpoint (if (= kind "actors")
                   "app.bsky.actor.searchActors"
                   "app.bsky.feed.searchPosts")]
    (-> (bluesky-json-fetch!
         (discord-query-url (str (bluesky-public-base-url) "/xrpc/" endpoint)
                            {:q query :limit limit})
         #js {:method "GET"
              :headers #js {"Accept" "application/json"
                            "User-Agent" "Knoxx-Agent/1.0"}}
         "Bluesky search")
        (.then (fn [payload]
                 (if (= kind "actors")
                   (let [results (->> (if (array? (aget payload "actors"))
                                        (array-seq (aget payload "actors"))
                                        [])
                                      (mapv (fn [actor]
                                              {:handle (or (aget actor "handle") "")
                                               :displayName (or (aget actor "displayName") "")
                                               :description (or (aget actor "description") "")
                                               :did (or (aget actor "did") "")
                                               :url (when-let [handle (some-> (aget actor "handle") str not-empty)]
                                                      (str "https://bsky.app/profile/" handle))})))]
                     {:kind kind :results results})
                   (let [results (->> (if (array? (aget payload "posts"))
                                        (array-seq (aget payload "posts"))
                                        [])
                                      (mapv (fn [post]
                                              (let [author (aget post "author")
                                                    record (aget post "record")
                                                    handle (or (aget author "handle") "")]
                                                {:handle handle
                                                 :displayName (or (aget author "displayName") "")
                                                 :text (or (aget record "text") "")
                                                 :createdAt (or (aget record "createdAt") "")
                                                 :uri (or (aget post "uri") "")
                                                 :url (bluesky-post-url handle (aget post "uri"))}))))]
                     {:kind kind :results results})))))))

(defn- bluesky-profile!
  [actor]
  (let [actor (some-> actor str str/trim)
        actor-promise (if (str/blank? actor)
                        (-> (bluesky-create-session!)
                            (.then (fn [session]
                                     (or (aget session "handle")
                                         (aget session "did")))))
                        (js/Promise.resolve actor))]
    (-> actor-promise
        (.then (fn [resolved-actor]
                 (bluesky-json-fetch!
                  (discord-query-url (str (bluesky-public-base-url) "/xrpc/app.bsky.actor.getProfile")
                                     {:actor resolved-actor})
                  #js {:method "GET"
                       :headers #js {"Accept" "application/json"
                                     "User-Agent" "Knoxx-Agent/1.0"}}
                  "Bluesky profile")))
        (.then (fn [profile]
                 {:did (or (aget profile "did") "")
                  :handle (or (aget profile "handle") "")
                  :displayName (or (aget profile "displayName") "")
                  :description (or (aget profile "description") "")
                  :followersCount (or (aget profile "followersCount") 0)
                  :followsCount (or (aget profile "followsCount") 0)
                  :postsCount (or (aget profile "postsCount") 0)
                  :url (when-let [handle (some-> (aget profile "handle") str not-empty)]
                         (str "https://bsky.app/profile/" handle))})))))

(defn- bluesky-author-feed!
  [actor limit]
  (-> (bluesky-json-fetch!
       (discord-query-url (str (bluesky-public-base-url) "/xrpc/app.bsky.feed.getAuthorFeed")
                          {:actor actor :limit limit})
       #js {:method "GET"
            :headers #js {"Accept" "application/json"
                          "User-Agent" "Knoxx-Agent/1.0"}}
       "Bluesky author feed")
      (.then (fn [payload]
               (let [results (->> (if (array? (aget payload "feed"))
                                    (array-seq (aget payload "feed"))
                                    [])
                                  (mapv (fn [entry]
                                          (let [post (aget entry "post")
                                                author (aget post "author")
                                                record (aget post "record")
                                                handle (or (aget author "handle") "")]
                                            {:handle handle
                                             :displayName (or (aget author "displayName") "")
                                             :text (or (aget record "text") "")
                                             :createdAt (or (aget record "createdAt") "")
                                             :uri (or (aget post "uri") "")
                                             :url (bluesky-post-url handle (aget post "uri"))}))))]
                 {:actor actor :results results})))))

(defn- bluesky-timeline!
  [limit cursor]
  (-> (bluesky-create-session!)
      (.then (fn [session]
               (bluesky-json-fetch!
                (discord-query-url (str (bluesky-service-base-url) "/xrpc/app.bsky.feed.getTimeline")
                                   {:limit limit :cursor cursor})
                #js {:method "GET"
                     :headers #js {"Accept" "application/json"
                                   "User-Agent" "Knoxx-Agent/1.0"
                                   "Authorization" (str "Bearer " (aget session "accessJwt"))}}
                "Bluesky timeline")))
      (.then (fn [payload]
               (let [results (->> (if (array? (aget payload "feed"))
                                    (array-seq (aget payload "feed"))
                                    [])
                                  (mapv (fn [entry]
                                          (let [post (aget entry "post")
                                                author (aget post "author")
                                                record (aget post "record")
                                                handle (or (aget author "handle") "")]
                                            {:handle handle
                                             :displayName (or (aget author "displayName") "")
                                             :text (or (aget record "text") "")
                                             :createdAt (or (aget record "createdAt") "")
                                             :uri (or (aget post "uri") "")
                                             :url (bluesky-post-url handle (aget post "uri"))}))))]
                 {:cursor (aget payload "cursor") :results results})))))

(defn- bluesky-publish!
  [text]
  (-> (bluesky-create-session!)
      (.then (fn [session]
               (-> (bluesky-json-fetch!
                    (str (bluesky-service-base-url) "/xrpc/com.atproto.repo.createRecord")
                    #js {:method "POST"
                         :headers #js {"Content-Type" "application/json"
                                       "User-Agent" "Knoxx-Agent/1.0"
                                       "Authorization" (str "Bearer " (aget session "accessJwt"))}
                         :body (.stringify js/JSON #js {:repo (aget session "did")
                                                        :collection "app.bsky.feed.post"
                                                        :record #js {"$type" "app.bsky.feed.post"
                                                                     :text text
                                                                     :createdAt (.toISOString (js/Date.))}})}
                    "Bluesky publish")
                   (.then (fn [result]
                            (let [uri (or (aget result "uri") "")]
                              {:uri uri
                               :cid (or (aget result "cid") "")
                               :url (or (bluesky-post-url (aget session "handle") uri)
                                        "")}))))))))


(defn create-bluesky-custom-tools
  ([runtime config] (create-bluesky-custom-tools runtime config nil))
  ([runtime _config auth-context]
   (let [Type (aget runtime "Type")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))
         publish-params (.Object Type
                                 #js {:text (.String Type #js {:description "Bluesky post text. Keep it concise and under platform limits."})})
         profile-params (.Object Type
                                 #js {:actor (type-optional Type (.String Type #js {:description "Optional Bluesky handle or DID. Defaults to the authenticated account."}))})
         search-params (.Object Type
                                #js {:query (.String Type #js {:description "Search query for Bluesky posts or actors."})
                                     :kind (type-optional Type (.String Type #js {:description "posts or actors. Defaults to posts."}))
                                     :limit (type-optional Type (.Number Type #js {:description "Maximum results to return." :minimum 1 :maximum 25}))})
         feed-params (.Object Type
                              #js {:actor (.String Type #js {:description "Bluesky handle or DID whose feed should be read."})
                                   :limit (type-optional Type (.Number Type #js {:description "Maximum posts to return." :minimum 1 :maximum 25}))})
         timeline-params (.Object Type
                                  #js {:limit (type-optional Type (.Number Type #js {:description "Maximum timeline posts to return." :minimum 1 :maximum 25}))
                                       :cursor (type-optional Type (.String Type #js {:description "Optional pagination cursor from a previous bluesky.timeline call."}))})
         format-posts (fn [prefix rows]
                        (let [lines (->> rows
                                         (map (fn [{:keys [displayName handle text url]}]
                                                (str "- " (or (not-empty displayName) handle "unknown")
                                                     (when (not (str/blank? (str handle))) (str " (@" handle ")"))
                                                     ": " (clip-text (or text "") 220)
                                                     (when (not (str/blank? (str url))) (str "\n  " url)))))
                                         (str/join "\n"))]
                          (str prefix (when-not (str/blank? lines) (str "\n" lines)))))
         publish-execute (fn [_tool-call-id params a b c]
                           (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                 text (or (aget params "text") "")]
                             (when (str/blank? (str/trim text))
                               (throw (js/Error. "text is required")))
                             (maybe-tool-update! on-update "Publishing to Bluesky…")
                             (-> (bluesky-publish! text)
                                 (.then (fn [result]
                                          (tool-text-result (str "Published Bluesky post\n" (or (:url result) (:uri result) ""))
                                                            result))))))
         profile-execute (fn [_tool-call-id params a b c]
                           (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                 actor (or (aget params "actor") "")]
                             (maybe-tool-update! on-update "Reading Bluesky profile…")
                             (-> (bluesky-profile! actor)
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
         search-execute (fn [_tool-call-id params a b c]
                          (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                query (or (aget params "query") "")
                                kind (or (aget params "kind") "posts")
                                limit (max 1 (min 25 (or (aget params "limit") 5)))]
                            (when (str/blank? (str/trim query))
                              (throw (js/Error. "query is required")))
                            (maybe-tool-update! on-update "Searching Bluesky…")
                            (-> (bluesky-search! query kind limit)
                                (.then (fn [result]
                                         (tool-text-result (format-posts (str "Bluesky search (" (:kind result) ")") (:results result))
                                                           result))))))
         author-feed-execute (fn [_tool-call-id params a b c]
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
         timeline-execute (fn [_tool-call-id params a b c]
                            (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                  limit (max 1 (min 25 (or (aget params "limit") 8)))
                                  cursor (or (aget params "cursor") "")]
                              (maybe-tool-update! on-update "Reading authenticated Bluesky timeline…")
                              (-> (bluesky-timeline! limit cursor)
                                  (.then (fn [result]
                                           (tool-text-result (format-posts "Bluesky timeline" (:results result))
                                                             result))))))
         ]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "bluesky.publish")
                  (doto (js-obj)
                    (aset "name" "bluesky.publish")
                    (aset "label" "Bluesky Publish")
                    (aset "description" "Publish a post to Bluesky using the configured account.")
                    (aset "promptSnippet" "Post a concise update to Bluesky when public social publishing is useful.")
                    (aset "parameters" publish-params)
                    (aset "execute" publish-execute)))
                (when (allowed? "bluesky.profile")
                  (doto (js-obj)
                    (aset "name" "bluesky.profile")
                    (aset "label" "Bluesky Profile")
                    (aset "description" "Read a Bluesky profile by handle or DID, or default to the authenticated account.")
                    (aset "parameters" profile-params)
                    (aset "execute" profile-execute)))
                (when (allowed? "bluesky.search")
                  (doto (js-obj)
                    (aset "name" "bluesky.search")
                    (aset "label" "Bluesky Search")
                    (aset "description" "Search public Bluesky posts or actors.")
                    (aset "parameters" search-params)
                    (aset "execute" search-execute)))
                (when (allowed? "bluesky.author.feed")
                  (doto (js-obj)
                    (aset "name" "bluesky.author.feed")
                    (aset "label" "Bluesky Author Feed")
                    (aset "description" "Read recent posts from a specific Bluesky author.")
                    (aset "parameters" feed-params)
                    (aset "execute" author-feed-execute)))
                (when (allowed? "bluesky.timeline")
                  (doto (js-obj)
                    (aset "name" "bluesky.timeline")
                    (aset "label" "Bluesky Timeline")
                    (aset "description" "Read the authenticated account's Bluesky timeline.")
                    (aset "parameters" timeline-params)
                    (aset "execute" timeline-execute))))))))))
