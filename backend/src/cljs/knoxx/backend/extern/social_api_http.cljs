(ns knoxx.backend.extern.social-api-http
  "Owns platform HTTP/authentication, URL encoding, bounded JSON and private session caching."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.social.api-response :as api-response]
            [knoxx.backend.domain.social.api-actions :as actions]
            [knoxx.backend.extern.social-api-codec :as codec]
            [knoxx.backend.law.platform-api :as law]
            [knoxx.backend.law.platform-api-request :as request-law]
            ["node:crypto" :as crypto]))

(defonce ^:private sessions* (atom {}))
(def ^:private max-response-bytes (* 2 1024 1024))
(def ^:private session-cache-ms (* 5 60 1000))

(defn- https-origin
  [text]
  (let [url (js/URL. text)]
    (when-not (and (= "https:" (.-protocol url)) (seq (.-hostname url))
                   (str/blank? (.-username url)) (str/blank? (.-password url))
                   (contains? #{"" "/"} (.-pathname url))
                   (str/blank? (.-search url)) (str/blank? (.-hash url)))
      (throw (ex-info "Configured Bluesky PDS must be an HTTPS origin" {:code "platform_pds_origin_invalid"})))
    (.-origin url)))

(defn request-url
  "Encode each pinned path binding and query value; array query values repeat their key."
  [base {:keys [path-template path query]}]
  (let [route (str/replace path-template #"\{([^}]+)\}"
                           (fn [[_ key]] (js/encodeURIComponent (str (get path key)))))
        parameters (js/URLSearchParams.)]
    (doseq [[key value] query item (if (vector? value) value [value])]
      (.append parameters key (str item)))
    (str base route (when (pos? (.-size parameters)) (str "?" (.toString parameters))))))

(defn- ^:async response-json!
  [response limit empty-allowed?]
  (let [declared (js/Number (.get (.-headers response) "content-length"))
        reader (some-> response .-body (.getReader))]
    (when (> declared limit)
      (when reader (await (.cancel reader)))
      (throw (ex-info "Platform response exceeds the byte limit" {:code "platform_response_too_large"})))
    (when (and (not reader) (not empty-allowed?))
      (throw (ex-info "Platform response has no JSON body" {:code "platform_response_not_json"})))
    (if-not reader
      {:body nil :empty-body? true}
      (try
      (loop [chunks [] bytes 0]
        (let [part (await (.read reader))]
          (if (.-done part)
            (if (and (zero? bytes) empty-allowed?)
              {:body nil :empty-body? true}
              (try {:body (codec/decode (.toString (.concat js/Buffer (clj->js chunks)) "utf8"))
                    :empty-body? false}
                   (catch :default _ (throw (ex-info "Platform response is not JSON" {:code "platform_response_not_json"})))))
            (let [total (+ bytes (.-byteLength (.-value part)))]
              (when (> total limit)
                (await (.cancel reader))
                (throw (ex-info "Platform response exceeds the byte limit" {:code "platform_response_too_large"})))
              (recur (conj chunks (.-value part)) total)))))
      (finally (.releaseLock reader))))))

(defn- ^:async request!
  [fetcher url method headers body options]
  (let [body? (if (contains? options :body-present?) (:body-present? options) (some? body))
        encoded (when body? (codec/encode body))
        _ (when (and encoded (> (.byteLength js/Buffer encoded "utf8") (get options :max-request-bytes 1048576)))
            (throw (ex-info "Platform request exceeds the JSON byte limit" {:code "platform_request_too_large" :phase :preflight})))
        response (try
                   (await (fetcher url (clj->js (cond-> {:method method :headers headers :redirect "error"
                                                       :signal (.timeout js/AbortSignal (get options :timeout-ms 30000))}
                                                body? (assoc :body encoded)))))
                   (catch :default _ (throw (ex-info "Platform request failed or timed out" {:code "platform_request_unavailable"}))))
        result (await (response-json! response (get options :max-response-bytes max-response-bytes)
                                     (contains? (set (:empty-success-statuses options)) (.-status response))))]
    {:ok (.-ok response) :status (.-status response) :body (:body result) :empty-body? (:empty-body? result)
     :rate-limit (into {} (keep (fn [key] (when-let [value (.get (.-headers response) key)] [key value])))
                       ["retry-after" "x-ratelimit-bucket" "x-ratelimit-remaining" "x-ratelimit-reset-after"])}))

(defn- ^:async create-session!
  [fetcher {:keys [identifier password pds]} options]
  (let [origin (https-origin pds)
        result (await (request! fetcher (str origin "/xrpc/com.atproto.server.createSession") "POST"
                               {"Content-Type" "application/json" "Accept" "application/json"}
                               {:identifier identifier :password password} options))
        token (get-in result [:body "accessJwt"]) did (get-in result [:body "did"])]
    (when-not (and (:ok result) (string? token) (seq token) (string? did) (str/starts-with? did "did:"))
      (throw (ex-info "Bluesky actor authentication failed" {:code "platform_actor_authentication_failed" :status (:status result)})))
    (let [did-document (get-in result [:body "didDoc"])
          pds (when (= did (get did-document "id"))
                (some #(when (or (= "#atproto_pds" (get % "id"))
                                  (= (str did "#atproto_pds") (get % "id")))
                         (get % "serviceEndpoint"))
                      (get did-document "service")))]
      {:token token :did did :handle (get-in result [:body "handle"])
       :pds (if pds (https-origin pds) origin)})))

(defn- ^:async session!
  [fetcher auth options]
  (let [now (.now js/Date)
        digest (-> (.createHash crypto "sha256")
                   (.update (.stringify js/JSON (clj->js auth))) (.digest "hex"))
        cached (get @sessions* digest)]
    (if (and (not= false (:cache? options)) (> (or (:until cached) 0) now))
      (await (:promise cached))
      (let [pending (create-session! fetcher auth options)]
        (when (not= false (:cache? options))
          (swap! sessions* #(assoc (into {} (filter (fn [[_ entry]] (> (:until entry) now))) %)
                                   digest {:until (+ now session-cache-ms) :promise pending})))
        (try (await pending)
             (catch :default error
               (swap! sessions* #(if (identical? pending (get-in % [digest :promise])) (dissoc % digest) %))
               (throw error)))))))

(defn ^:async read!
  "Execute a validated schema-selected GET with the current actor's credential. Options are internal test seams."
  ([plan authentication] (await (read! plan authentication {})))
  ([plan authentication options]
   (law/assert-valid! law/ReadPlan plan)
   (let [discord? (= (:platform plan) "discord")
         _ (law/assert-valid! (if discord? law/DiscordReadCredential law/BlueskyReadCredential) authentication)
         fetcher (or (:fetch-fn options) js/fetch)
         session (when-not discord? (await (session! fetcher authentication options)))
         base (if discord? "https://discord.com/api/v10" (:pds session))
         headers (cond-> {"Accept" "application/json" "User-Agent" "Knoxx-Agent/1.0"
                          "Authorization" (str (if discord? "Bot " "Bearer ")
                                               (if discord? (:token authentication) (:token session)))}
                   (:proxy plan) (assoc "atproto-proxy" (:proxy plan)))
         result (await (request! fetcher (request-url base plan) "GET" headers nil options))
         sanitized (api-response/redact (:body result)
                                        [(if discord? (:token authentication) (:token session))
                                         (:password authentication)])]
     (assoc result :body (:body sanitized) :redacted-fields (:redacted-fields sanitized)))))

(defn- assert-owned-repository!
  [plan session]
  (when-let [repo (when (= "bluesky" (:platform plan)) (get-in plan [:body "repo"]))]
    (when-not (and (= repo (:account-repository plan))
                  (or (= repo (:did session))
                  (and (string? (:handle session))
                       (= (str/lower-case repo) (str/lower-case (:handle session))))))
      (throw (ex-info "Repository writes must target the authenticated actor's DID or verified handle"
                      {:code "platform_repository_not_owned" :phase :preflight})))))

(defn- ^:async assert-reply!
  [fetcher base headers reply options]
  (let [uri (get-in reply ["parent" "uri"]) cid (get-in reply ["parent" "cid"])]
    (when-not (and (string? uri) (re-matches #"at://did:[^/]+/app\.bsky\.feed\.post/[^/?#]+" uri) (string? cid) (seq cid))
      (throw (ex-info "A reply requires the exact returned parent URI and CID" {:code "platform_reply_reference_invalid" :phase :preflight})))
    (let [url (request-url base {:path-template "/xrpc/app.bsky.feed.getPostThread" :path {}
                                :query {"uri" uri "depth" 0 "parentHeight" 0}})
          response (await (request! fetcher url "GET" headers nil (assoc options :body-present? false :empty-success-statuses [])))
          post (get-in response [:body "thread" "post"])
          parent {"uri" (get post "uri") "cid" (get post "cid")}
          root (or (get-in post ["record" "reply" "root"]) parent)]
      (when-not (and (:ok response) (= parent (select-keys (get reply "parent") ["uri" "cid"]))
                                     (= (select-keys root ["uri" "cid"]) (select-keys (get reply "root") ["uri" "cid"])))
        (throw (ex-info "Reply parent/root references do not match the current platform conversation"
                        {:code "platform_reply_reference_mismatch" :phase :preflight}))))))

(defn- ^:async assert-replies!
  [fetcher base headers plan options]
  (try
    (doseq [record (actions/post-records plan)]
      (when (contains? record "reply") (await (assert-reply! fetcher base headers (get record "reply") options))))
    (catch :default error
      (throw (ex-info "Reply references could not be verified before publication"
                      (assoc (or (ex-data error) {:code "platform_reply_read_failed"}) :phase :preflight) error)))))

(defn- assert-request-size!
  [plan options]
  (when (and (contains? plan :body)
             (> (.byteLength js/Buffer (codec/encode (:body plan)) "utf8") (get options :max-request-bytes 1048576)))
    (throw (ex-info "Platform request exceeds the JSON byte limit" {:code "platform_request_too_large" :phase :preflight}))))

(defn- ^:async authenticated-session!
  [fetcher authentication options]
  (try (await (session! fetcher authentication options))
       (catch :default error
         (throw (ex-info "Actor authentication failed before the requested platform operation"
                         {:code "platform_actor_authentication_failed" :phase :preflight} error)))))

(defn ^:async execute-json!
  "Internal transport for an already planned AND admitted JSON request. This
   does not grant tools or perform action admission. It never retries a write.
   Options are internal transport test seams, never model-supplied inputs."
  ([plan authentication] (await (execute-json! plan authentication {})))
  ([plan authentication options]
   (request-law/assert-valid! request-law/Plan plan)
   (let [discord? (= (:platform plan) "discord")
         _ (law/assert-valid! (if discord? law/DiscordReadCredential law/BlueskyReadCredential) authentication)
         _ (assert-request-size! plan options)
         fetcher (or (:fetch-fn options) js/fetch)
         session (when-not discord? (await (authenticated-session! fetcher authentication options)))
         _ (assert-owned-repository! plan session)
         base (if discord? "https://discord.com/api/v10" (:pds session))
         headers (cond-> {"Accept" "application/json" "User-Agent" "Knoxx-Agent/1.0"
                          "Authorization" (str (if discord? "Bot " "Bearer ")
                                               (if discord? (:token authentication) (:token session)))}
                   (contains? plan :body) (assoc "Content-Type" "application/json")
                   (:proxy plan) (assoc "atproto-proxy" (:proxy plan)))
         _ (await (assert-replies! fetcher base headers plan options))
         result (await (request! fetcher (request-url base plan) (:method plan) headers (:body plan)
                                 (assoc options :body-present? (contains? plan :body)
                                        :empty-success-statuses (:empty-success-statuses plan))))
         sanitized (api-response/redact (:body result)
                                        [(if discord? (:token authentication) (:token session))
                                         (:password authentication)])]
     (request-law/assert-valid! request-law/Response
                                (cond-> (assoc result :body (:body sanitized) :redacted-fields (:redacted-fields sanitized))
                                  session (assoc :authenticated-did (:did session)))))))
