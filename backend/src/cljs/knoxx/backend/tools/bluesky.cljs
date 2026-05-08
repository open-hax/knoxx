(ns knoxx.backend.tools.bluesky
  "Bluesky ATProto tool factories."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [clip-text tool-text-result]]
            [knoxx.backend.tools.actor-credentials :as actor-credentials]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! create-tool-obj]]))

(def publish-params
  [:map
   [:text {:description "Bluesky post text. Keep it concise and under platform limits."} :string]])

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

(defn- bluesky-service-base-url []
  "https://bsky.social")

(defn- bluesky-public-base-url []
  "https://public.api.bsky.app")

(defn- query-url [base params]
  (let [search (js/URLSearchParams.)]
    (doseq [[k v] params]
      (when-not (or (nil? v) (and (string? v) (str/blank? v)))
        (.append search (name k) (str v))))
    (let [query (.toString search)]
      (if (str/blank? query) base (str base "?" query)))))

(defn- bluesky-json-fetch! [url options label]
  (-> (js/fetch url options)
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (-> (.text resp)
                     (.then (fn [text]
                              (throw (js/Error. (str label " error " (.-status resp) ": " text)))))))))))

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
      (.then (fn [{:keys [identifier password]}]
               (bluesky-json-fetch!
                (str (bluesky-service-base-url) "/xrpc/com.atproto.server.createSession")
                #js {:method "POST"
                     :headers #js {"Content-Type" "application/json"
                                   "User-Agent" "Knoxx-Agent/1.0"}
                     :body (.stringify js/JSON #js {:identifier identifier :password password})}
                "Bluesky auth")))))

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

(defn- bluesky-search! [query kind limit]
  (let [kind (if (= kind "actors") "actors" "posts")
        endpoint (if (= kind "actors") "app.bsky.actor.searchActors" "app.bsky.feed.searchPosts")]
    (-> (bluesky-json-fetch!
         (query-url (str (bluesky-public-base-url) "/xrpc/" endpoint)
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

(defn- bluesky-profile! [runtime actor]
  (let [actor (some-> actor str str/trim)
        actor-promise (if (str/blank? actor)
                        (-> (bluesky-create-session! runtime)
                            (.then (fn [session]
                                     (or (aget session "handle") (aget session "did")))))
                        (js/Promise.resolve actor))]
    (-> actor-promise
        (.then (fn [resolved-actor]
                 (bluesky-json-fetch!
                  (query-url (str (bluesky-public-base-url) "/xrpc/app.bsky.actor.getProfile")
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

(defn- bluesky-author-feed! [actor limit]
  (-> (bluesky-json-fetch!
       (query-url (str (bluesky-public-base-url) "/xrpc/app.bsky.feed.getAuthorFeed")
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

(defn- bluesky-timeline! [runtime limit cursor]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (bluesky-json-fetch!
                (query-url (str (bluesky-service-base-url) "/xrpc/app.bsky.feed.getTimeline")
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

(defn- bluesky-publish! [runtime text]
  (-> (bluesky-create-session! runtime)
      (.then (fn [session]
               (-> (bluesky-json-fetch!
                    (str (bluesky-service-base-url) "/xrpc/com.atproto.repo.createRecord")
                    #js {:method "POST"
                         :headers #js {"Content-Type" "application/json"
                                       "User-Agent" "Knoxx-Agent/1.0"
                                       "Authorization" (str "Bearer " (aget session "accessJwt"))}
                         :body (.stringify js/JSON
                                           #js {:repo (aget session "did")
                                                :collection "app.bsky.feed.post"
                                                :record #js {"$type" "app.bsky.feed.post"
                                                             :text text
                                                             :createdAt (.toISOString (js/Date.))}})}
                    "Bluesky publish")
                   (.then (fn [result]
                            (let [uri (or (aget result "uri") "")]
                              {:uri uri
                               :cid (or (aget result "cid") "")
                               :url (or (bluesky-post-url (aget session "handle") uri) "")}))))))))

(defn publish-execute [runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        text (or (aget params "text") "")]
    (when (str/blank? (str/trim text))
      (throw (js/Error. "text is required")))
    (maybe-tool-update! on-update "Publishing to Bluesky…")
    (-> (bluesky-publish! runtime text)
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

(defn search-execute [_runtime _config _tool-call-id params a b c]
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

(def publish-tool
  (partial create-tool-obj
           "bluesky.publish" "Bluesky Publish"
           "Publish a post to Bluesky using the configured account."
           "Post a concise update to Bluesky when public social publishing is useful."
           []
           publish-params
           publish-execute))

(def profile-tool
  (partial create-tool-obj
           "bluesky.profile" "Bluesky Profile"
           "Read a Bluesky profile by handle or DID, or default to the authenticated account."
           "Read a Bluesky profile by handle or DID, or default to the authenticated account."
           []
           profile-params
           profile-execute))

(def search-tool
  (partial create-tool-obj
           "bluesky.search" "Bluesky Search"
           "Search public Bluesky posts or actors."
           "Search public Bluesky posts or actors."
           []
           search-params
           search-execute))

(def author-feed-tool
  (partial create-tool-obj
           "bluesky.author.feed" "Bluesky Author Feed"
           "Read recent posts from a specific Bluesky author."
           "Read recent posts from a specific Bluesky author."
           []
           feed-params
           author-feed-execute))

(def timeline-tool
  (partial create-tool-obj
           "bluesky.timeline" "Bluesky Timeline"
           "Read the authenticated account's Bluesky timeline."
           "Read the authenticated account's Bluesky timeline."
           []
           timeline-params
           timeline-execute))

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
                  (timeline-tool runtime config))]))))))
