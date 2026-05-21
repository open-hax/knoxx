(ns knoxx.backend.domain.discord.rest-client
  "Discord REST client boundary.

   Tool/runtime namespaces should depend on IDiscordRestClient instead of
   constructing discord.com URLs, bot auth headers, RequestInit objects, or
   native fetch calls inline. Discord gateway SDK `.fetch` calls are a separate
   SDK boundary and should get their own gateway adapter methods."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.extern.json :as xjson]
            [promesa.core :as p]))

(defprotocol IDiscordRestClient
  (channel-messages! [client channel-id opts]
    "Fetch channel messages. opts may include :limit, :before, :after, :around.")
  (current-user-guilds! [client]
    "List guilds for the authenticated bot user.")
  (guild-channels! [client guild-id]
    "List channels for a guild.")
  (open-dm-channel! [client user-id]
    "Open or fetch a DM channel with user-id.")
  (create-channel-message! [client channel-id payload]
    "Create a channel message from a CLJS Discord JSON payload.")
  (create-channel-message-form! [client channel-id form-data]
    "Create a channel message using a prepared FormData body for file uploads.")
  (add-reaction! [client channel-id message-id emoji]
    "Add the bot user's reaction to a message.")
  (create-thread! [client channel-id message-id payload]
    "Create a thread in a channel, or from a message when message-id is present.")
  (fetch-attachment! [client url]
    "Fetch arbitrary attachment media. Resolves {:ok :status :headers :body ArrayBuffer}."))

(def ^:private discord-base-url "https://discord.com/api/v10")

(defn- bot-headers
  ([token] (bot-headers token true))
  ([token json?]
   (cond-> {"Authorization" (str "Bot " token)}
     json? (assoc "Content-Type" "application/json"))))

(defn- require-token!
  [token]
  (when (str/blank? (str token))
    (throw (js/Error. "Discord bot token not configured")))
  token)

(defn- query-url
  [base params]
  (let [search (js/URLSearchParams.)]
    (doseq [[k v] params]
      (when-not (or (nil? v) (and (string? v) (str/blank? v)))
        (.append search (name k) (str v))))
    (let [query (.toString search)]
      (if (str/blank? query) base (str base "?" query)))))

(defn- discord-json!
  [http-client url opts timeout-ms label]
  (p/let [resp (xfetch/json! http-client {:url url :opts opts :timeout-ms timeout-ms})]
    (if (:ok resp)
      (:body resp)
      (throw (js/Error. (str label " Discord API error " (:status resp) ": " (pr-str (:body resp))))))))

(defrecord FetchDiscordRestClient [bot-token http-client timeout-ms user-agent]
  IDiscordRestClient
  (channel-messages! [_ channel-id {:keys [limit before after around]}]
    (require-token! bot-token)
    (discord-json! (or http-client xfetch/default-client)
                   (query-url (str discord-base-url "/channels/" channel-id "/messages")
                              {:limit (max 1 (min 100 (or limit 50)))
                               :before before
                               :after after
                               :around around})
                   {:method "GET" :headers (bot-headers bot-token)}
                   (or timeout-ms 15000)
                   "Fetch messages"))
  (current-user-guilds! [_]
    (require-token! bot-token)
    (discord-json! (or http-client xfetch/default-client)
                   (str discord-base-url "/users/@me/guilds")
                   {:method "GET" :headers (bot-headers bot-token)}
                   (or timeout-ms 15000)
                   "List guilds"))
  (guild-channels! [_ guild-id]
    (require-token! bot-token)
    (discord-json! (or http-client xfetch/default-client)
                   (str discord-base-url "/guilds/" guild-id "/channels")
                   {:method "GET" :headers (bot-headers bot-token)}
                   (or timeout-ms 15000)
                   "List channels"))
  (open-dm-channel! [_ user-id]
    (require-token! bot-token)
    (discord-json! (or http-client xfetch/default-client)
                   (str discord-base-url "/users/@me/channels")
                   {:method "POST"
                    :headers (bot-headers bot-token)
                    :body (xjson/stringify {:recipient_id user-id})}
                   (or timeout-ms 15000)
                   "Open DM channel"))
  (create-channel-message! [_ channel-id payload]
    (require-token! bot-token)
    (discord-json! (or http-client xfetch/default-client)
                   (str discord-base-url "/channels/" channel-id "/messages")
                   {:method "POST"
                    :headers (bot-headers bot-token)
                    :body (xjson/stringify payload)}
                   (or timeout-ms 15000)
                   "Create message"))
  (create-channel-message-form! [_ channel-id form-data]
    (require-token! bot-token)
    (discord-json! (or http-client xfetch/default-client)
                   (str discord-base-url "/channels/" channel-id "/messages")
                   {:method "POST"
                    :headers (bot-headers bot-token false)
                    :body form-data}
                   (or timeout-ms 30000)
                   "Create multipart message"))
  (add-reaction! [_ channel-id message-id emoji]
    (require-token! bot-token)
    (discord-json! (or http-client xfetch/default-client)
                   (str discord-base-url "/channels/" channel-id "/messages/" message-id "/reactions/" (js/encodeURIComponent emoji) "/@me")
                   {:method "PUT" :headers (bot-headers bot-token false)}
                   (or timeout-ms 15000)
                   "Add reaction"))
  (create-thread! [_ channel-id message-id payload]
    (require-token! bot-token)
    (let [url (if (str/blank? (str message-id))
                (str discord-base-url "/channels/" channel-id "/threads")
                (str discord-base-url "/channels/" channel-id "/messages/" message-id "/threads"))]
      (discord-json! (or http-client xfetch/default-client)
                     url
                     {:method "POST"
                      :headers (bot-headers bot-token)
                      :body (xjson/stringify payload)}
                     (or timeout-ms 15000)
                     "Create thread")))
  (fetch-attachment! [_ url]
    (xfetch/array-buffer! (or http-client xfetch/default-client)
                          {:url url
                           :opts {:method "GET"
                                  :headers {"User-Agent" (or user-agent "Knoxx-Agent/1.0")}}
                           :timeout-ms (or timeout-ms 30000)})))

(defn client
  ([bot-token] (client bot-token {}))
  ([bot-token {:keys [http-client timeout-ms user-agent]}]
   (->FetchDiscordRestClient bot-token (or http-client xfetch/default-client) (or timeout-ms 15000) user-agent)))
