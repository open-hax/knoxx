(ns knoxx.backend.domain.bluesky.client
  "Bluesky/ATProto client protocol.

   Domain/tool code should depend on IBlueskyClient rather than constructing
   XRPC URLs, auth headers, JSON bodies, rich-text wire payloads, or native
   fetch calls inline."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.extern.json :as xjson]
            [promesa.core :as p]))

(defprotocol IBlueskyClient
  (create-session! [client credentials])
  (upload-blob! [client session buffer mime-type])
  (create-record! [client session collection record])
  (delete-record! [client session collection rkey])
  (resolve-post! [client uri])
  (profile! [client actor])
  (search-posts! [client session query limit])
  (search-actors! [client session query limit])
  (actor-feed! [client session actor limit])
  (timeline! [client session opts])
  (thread! [client session uri depth])
  (notifications! [client session opts])
  (followers! [client actor limit])
  (follows! [client actor limit])
  (chat-list! [client session opts])
  (chat-read! [client session convo-id opts])
  (chat-send! [client session convo-id payload])
  (chat-react! [client session convo-id message-id emoji]))

(def ^:private bluesky-service-base-url "https://bsky.social")
(def ^:private bluesky-public-base-url "https://public.api.bsky.app")
(def ^:private bluesky-chat-base-url "https://api.bsky.chat")

(defn- query-url
  [base params]
  (let [search (js/URLSearchParams.)]
    (doseq [[k v] params]
      (when-not (or (nil? v) (and (string? v) (str/blank? v)))
        (.append search (name k) (str v))))
    (let [query (.toString search)]
      (if (str/blank? query) base (str base "?" query)))))

(defn- bearer-token
  [session]
  (or (:accessJwt session)
      (:access-jwt session)
      (when-not (map? session) (aget session "accessJwt"))))

(defn- session-did
  [session]
  (or (:did session)
      (when-not (map? session) (aget session "did"))))

(defn- json-request!
  [http-client {:keys [method url headers body label timeout-ms]}]
  (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                             {:url url
                              :opts (cond-> {:method method
                                             :headers headers}
                                      (some? body) (assoc :body (xjson/stringify body)))
                              :timeout-ms (or timeout-ms 30000)})]
    (if (:ok resp)
      (:body resp)
      (throw (js/Error. (str label " error " (:status resp) ": " (pr-str (:body resp))))))))

(defn- json-get!
  [http-client url headers label]
  (json-request! http-client {:method "GET" :url url :headers headers :label label}))

(defn- json-post!
  [http-client url headers body label]
  (json-request! http-client {:method "POST" :url url :headers headers :body body :label label}))

(defn- public-headers
  []
  {"Accept" "application/json"
   "User-Agent" "Knoxx-Agent/1.0"})

(defn- json-headers
  []
  {"Content-Type" "application/json"
   "User-Agent" "Knoxx-Agent/1.0"})

(defn- auth-headers
  [session]
  (assoc (public-headers) "Authorization" (str "Bearer " (bearer-token session))))

(defn- auth-json-headers
  [session]
  (assoc (json-headers) "Authorization" (str "Bearer " (bearer-token session))))

(defrecord FetchBlueskyClient [http-client]
  IBlueskyClient
  (create-session! [_ credentials]
    (json-post! http-client
                (str bluesky-service-base-url "/xrpc/com.atproto.server.createSession")
                (json-headers)
                {:identifier (:identifier credentials)
                 :password (:password credentials)}
                "Bluesky auth"))
  (upload-blob! [_ session buffer mime-type]
    (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                               {:url (str bluesky-service-base-url "/xrpc/com.atproto.repo.uploadBlob")
                                :opts {:method "POST"
                                       :headers {"Content-Type" mime-type
                                                 "Authorization" (str "Bearer " (bearer-token session))}
                                       :body buffer}
                                :timeout-ms 60000})]
      (if (:ok resp)
        (:body resp)
        (throw (js/Error. (str "Blob upload error " (:status resp) ": " (pr-str (:body resp))))))))
  (create-record! [_ session collection record]
    (json-post! http-client
                (str bluesky-service-base-url "/xrpc/com.atproto.repo.createRecord")
                (auth-json-headers session)
                {:repo (session-did session)
                 :collection collection
                 :record record}
                "Bluesky create record"))
  (delete-record! [_ session collection rkey]
    (json-post! http-client
                (str bluesky-service-base-url "/xrpc/com.atproto.repo.deleteRecord")
                (auth-json-headers session)
                {:repo (session-did session)
                 :collection collection
                 :rkey rkey}
                "Bluesky delete record"))
  (resolve-post! [_ uri]
    (p/let [payload (json-get! http-client
                               (query-url (str bluesky-public-base-url "/xrpc/app.bsky.feed.getPosts")
                                          {:uris uri})
                               (public-headers)
                               "Bluesky resolve post")]
      (let [post (first (or (:posts payload) []))]
        (when-not post
          (throw (js/Error. (str "Could not resolve post: " uri))))
        {:uri uri
         :cid (:cid post)
         :text (or (get-in post [:record :text]) "")
         :authorHandle (or (get-in post [:author :handle]) "")
         :authorDid (or (get-in post [:author :did]) "")})))
  (profile! [_ actor]
    (json-get! http-client
               (query-url (str bluesky-public-base-url "/xrpc/app.bsky.actor.getProfile")
                          {:actor actor})
               (public-headers)
               "Bluesky profile"))
  (search-posts! [_ session query limit]
    (json-get! http-client
               (query-url (str bluesky-service-base-url "/xrpc/app.bsky.feed.searchPosts")
                          {:q query :limit limit})
               (auth-headers session)
               "Bluesky search"))
  (search-actors! [_ session query limit]
    (json-get! http-client
               (query-url (str bluesky-service-base-url "/xrpc/app.bsky.actor.searchActors")
                          {:q query :limit limit})
               (auth-headers session)
               "Bluesky search"))
  (actor-feed! [_ _session actor limit]
    (json-get! http-client
               (query-url (str bluesky-public-base-url "/xrpc/app.bsky.feed.getAuthorFeed")
                          {:actor actor :limit limit})
               (public-headers)
               "Bluesky author feed"))
  (timeline! [_ session {:keys [limit cursor]}]
    (json-get! http-client
               (query-url (str bluesky-service-base-url "/xrpc/app.bsky.feed.getTimeline")
                          {:limit limit :cursor cursor})
               (auth-headers session)
               "Bluesky timeline"))
  (thread! [_ _session uri depth]
    (json-get! http-client
               (query-url (str bluesky-public-base-url "/xrpc/app.bsky.feed.getPostThread")
                          {:uri uri :depth depth})
               (public-headers)
               "Bluesky thread"))
  (notifications! [_ session {:keys [limit cursor]}]
    (json-get! http-client
               (query-url (str bluesky-service-base-url "/xrpc/app.bsky.notification.listNotifications")
                          {:limit limit :cursor cursor})
               (auth-headers session)
               "Bluesky notifications"))
  (followers! [_ actor limit]
    (json-get! http-client
               (query-url (str bluesky-public-base-url "/xrpc/app.bsky.graph.getFollowers")
                          {:actor actor :limit limit})
               (public-headers)
               "Bluesky followers"))
  (follows! [_ actor limit]
    (json-get! http-client
               (query-url (str bluesky-public-base-url "/xrpc/app.bsky.graph.getFollows")
                          {:actor actor :limit limit})
               (public-headers)
               "Bluesky follows"))
  (chat-list! [_ session {:keys [limit]}]
    (json-get! http-client
               (query-url (str bluesky-chat-base-url "/xrpc/chat.bsky.convo.listConvos")
                          {:limit limit})
               (auth-headers session)
               "Bluesky chat list"))
  (chat-read! [_ session convo-id {:keys [limit]}]
    (json-get! http-client
               (query-url (str bluesky-chat-base-url "/xrpc/chat.bsky.convo.getMessages")
                          {:convoId convo-id :limit limit})
               (auth-headers session)
               "Bluesky chat messages"))
  (chat-send! [_ session convo-id payload]
    (json-post! http-client
                (str bluesky-chat-base-url "/xrpc/chat.bsky.convo.sendMessage")
                (auth-json-headers session)
                {:convoId convo-id
                 :message payload}
                "Bluesky chat send"))
  (chat-react! [_ session convo-id message-id emoji]
    (json-post! http-client
                (str bluesky-chat-base-url "/xrpc/chat.bsky.convo.addReaction")
                (auth-json-headers session)
                {:convoId convo-id
                 :messageId message-id
                 :value emoji}
                "Bluesky chat react")))

(defn client
  ([] (client {}))
  ([{:keys [http-client]}]
   (->FetchBlueskyClient (or http-client xfetch/default-client))))
