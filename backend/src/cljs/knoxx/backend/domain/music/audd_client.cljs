(ns knoxx.backend.domain.music.audd-client
  "AudD/music-identification client protocol.

   Covers music-recognition HTTP requests and remote metadata lookups. This
   client owns AudD/AcoustID/MusicBrainz request construction and response
   parsing so tool functions do not call native fetch directly."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [promesa.core :as p]))

(defprotocol IAudDClient
  (recognize! [client media])
  (acoustid-lookup! [client fingerprint duration])
  (musicbrainz-recording! [client mbid])
  (fetch-url! [client url opts]))

(defn- configured?
  [value]
  (not (str/blank? (str value))))

(defn- checked-body!
  [resp label]
  (if (:ok resp)
    (:body resp)
    (throw (js/Error. (str label " failed (" (:status resp) "): " (pr-str (:body resp)))))))

(defn- audd-form
  [api-token {:keys [buffer mime-type filename]}]
  (let [form (js/FormData.)]
    (.append form "api_token" api-token)
    (.append form "return" "apple_music,spotify,deezer")
    (.append form
             "file"
             (js/Blob. #js [buffer] #js {:type mime-type})
             filename)
    form))

(defn- acoustid-url
  [api-key fingerprint duration]
  (let [params (js/URLSearchParams.)]
    (.set params "client" api-key)
    (.set params "duration" (str (or duration 25)))
    (.set params "fingerprint" (str fingerprint))
    (.set params "meta" "recordings+recordingids+releasegroups")
    (str "https://api.acoustid.org/v2/lookup?" (.toString params))))

(defn- musicbrainz-url
  [mbid]
  (str "https://musicbrainz.org/ws/2/recording/"
       (js/encodeURIComponent (str mbid))
       "?inc=isrcs+releases+release-groups&fmt=json"))

(defn- sleep!
  [ms]
  (js/Promise. (fn [resolve]
                 (js/setTimeout resolve ms))))

(defrecord FetchAudDClient [config http-client timeout-ms]
  IAudDClient
  (recognize! [_ media]
    (let [audd-token (:audd-api-token config)]
      (if-not (configured? audd-token)
        (js/Promise.resolve {:error "AUDD_API_TOKEN not configured"
                             :hint "Set AUDD_API_TOKEN to enable music identification"})
        (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                                   {:url "https://api.audd.io/"
                                    :opts {:method "POST"
                                           :form (audd-form audd-token media)}
                                    :timeout-ms (or timeout-ms 60000)})]
          (checked-body! resp "AudD recognition")))))
  (acoustid-lookup! [_ fingerprint duration]
    (let [acoustid-key (:acoustid-api-key config)]
      (if-not (configured? acoustid-key)
        (js/Promise.resolve {:error "ACOUSTID_API_KEY not configured"
                             :hint "Set acoustid-api-key in Knoxx config to enable AcoustID lookups"})
        (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                                   {:url (acoustid-url acoustid-key fingerprint duration)
                                    :opts {:method "GET"}
                                    :timeout-ms (or timeout-ms 60000)})]
          (checked-body! resp "AcoustID lookup")))))
  (musicbrainz-recording! [_ mbid]
    (p/let [_ (sleep! 1100)
            resp (xfetch/json! (or http-client xfetch/default-client)
                               {:url (musicbrainz-url mbid)
                                :opts {:method "GET"
                                       :headers {"User-Agent" "Knoxx-Agent/1.0 (discord bot)"}}
                                :timeout-ms (or timeout-ms 60000)})]
      (checked-body! resp "MusicBrainz recording lookup")))
  (fetch-url! [_ url opts]
    (xfetch/json! (or http-client xfetch/default-client)
                  {:url url
                   :opts opts
                   :timeout-ms (or timeout-ms 60000)})))

(defn client
  ([config] (client config {}))
  ([config {:keys [http-client timeout-ms]}]
   (->FetchAudDClient config (or http-client xfetch/default-client) (or timeout-ms 60000))))
