(ns knoxx.backend.domain.media.blaze-client
  "Blaze/media-generation provider client protocol.

   Covers generation requests and provider-hosted media URL downloads in
   `domain.media.blaze`. This namespace owns Proxx/Blaze URL construction,
   authorization headers, fetch timeouts, and response parsing."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.media :as media]
            [knoxx.backend.extern.fetch :as xfetch]
            [promesa.core :as p]))

(defprotocol IBlazeClient
  (generate! [client modality payload attempt-context])
  (fetch-generated-media! [client url]))

(defn- blank->nil
  [v]
  (let [s (str/trim (str (or v "")))]
    (when-not (str/blank? s) s)))

(defn- config-value
  [config keyword-key js-key camel-key]
  (or (when (map? config) (get config keyword-key))
      (when-not (map? config) (aget config js-key))
      (when-not (map? config) (aget config camel-key))))

(defn- env-value
  [& names]
  (some (fn [name]
          (some-> js/process .-env (aget name) blank->nil))
        names))

(defn- proxx-api-key
  [config]
  (or (blank->nil (config-value config :proxx-auth-token "proxx-auth-token" "proxxAuthToken"))
      (env-value "PROXX_AUTH_TOKEN" "PROXY_AUTH_TOKEN")))

(defn proxx-base-url
  [config]
  (str/replace
   (or (blank->nil (config-value config :proxx-base-url "proxx-base-url" "proxxBaseUrl"))
       (env-value "PROXX_BASE_URL")
       "http://proxx:8789")
   #"/+$" ""))

(defn- generation-url
  [config modality]
  (str (proxx-base-url config)
       (case modality
         "image" "/v1/images/generations"
         "video" "/v1/videos/generations"
         "music" "/v1/music/generations"
         "tts" "/v1/audio/speech"
         "/v1/chat/completions")))

(defn- generation-headers
  [config attempt-context]
  (cond-> {"Authorization" (str "Bearer " (or (proxx-api-key config)
                                               (throw (js/Error. "PROXX_AUTH_TOKEN/PROXY_AUTH_TOKEN not configured for Proxx-authenticated Blaze proxying"))))
           "Content-Type" "application/json"
           "Accept" "application/json"}
    (:tool_call_id attempt-context) (assoc "X-Open-Hax-Tool-Call-Id" (:tool_call_id attempt-context))))

(defn- checked-json-body!
  [resp]
  (if (:ok resp)
    (:body resp)
    (throw (js/Error. (str "Proxx Blaze proxy HTTP " (:status resp) ": " (pr-str (:body resp)))))))

(defn- checked-media-body!
  [url resp]
  (if (:ok resp)
    (let [arr (:body resp)
          buffer (.from js/Buffer (js/Uint8Array. arr))]
      {:mime-type (media/sanitize-mime-type (get (:headers resp) "content-type") "application/octet-stream")
       :buffer buffer
       :source-url url})
    (throw (js/Error. (str "Generated asset download HTTP " (:status resp) ": " (pr-str (:body resp)))))))

(defrecord FetchBlazeClient [config http-client]
  IBlazeClient
  (generate! [_ modality payload attempt-context]
    (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                               {:url (generation-url config modality)
                                :opts {:method "POST"
                                       :headers (generation-headers config attempt-context)
                                       :json payload}
                                :timeout-ms 1200000})]
      (checked-json-body! resp)))
  (fetch-generated-media! [_ url]
    (p/let [resp (xfetch/array-buffer! (or http-client xfetch/default-client)
                                       {:url url
                                        :opts {:method "GET"
                                               :headers {"Accept" "image/*,audio/*,video/*,*/*"}}
                                        :timeout-ms 120000000})]
      (checked-media-body! url resp))))

(defn client
  ([config] (client config {}))
  ([config {:keys [http-client]}]
   (->FetchBlazeClient config (or http-client xfetch/default-client))))
