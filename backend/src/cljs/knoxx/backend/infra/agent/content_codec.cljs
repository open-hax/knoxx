(ns knoxx.backend.infra.agent.content-codec
  "Agent content/media codec and materialization port."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.agent.message :as msg]))

(defprotocol IContentCodec
  (content-parts->provider [codec content-parts])
  (provider->content-parts [codec provider-content]))

(defprotocol IMediaMaterializer
  (materialize-media! [materializer content-part auth-context]))

(defn fetch-b64!
  [url media-type]
  (-> (js/fetch url)
      (.then (fn [r]
               (when-not (.-ok r)
                 (throw (js/Error. (str media-type " fetch failed: " (.-status r)))))
               (.-arrayBuffer r)))
      (.then (fn [ab]
               (let [buf (js/Buffer.from ab)]
                 (str "data:" media-type ";base64," (.toString buf "base64")))))))

(defn- audio-format
  [mime]
  (msg/mime->audio-format mime))

(defn media-map
  [part-type data mime]
  (cond-> {:type part-type
           :data data
           :mimeType mime}
    (= "audio" part-type) (assoc :format (or (audio-format mime) "mp3"))))

(defn materialize!
  [part]
  (let [part-type (some-> (:type part) str str/lower-case)
        url (some-> (:url part) str not-empty)
        data (some-> (:data part) str not-empty)
        mime (or (some-> (:mimeType part) str not-empty)
                 (if (= "audio" part-type) "audio/mpeg" "image/png"))]
    (cond
      (and data (str/starts-with? data "data:"))
      (js/Promise.resolve
       (let [comma (.indexOf data ",")]
         (media-map part-type (if (>= comma 0) (.slice data (inc comma)) data) mime)))

      (and data (not (str/starts-with? data "http")))
      (js/Promise.resolve (media-map part-type data mime))

      url
      (-> (fetch-b64! url mime)
          (.then (fn [data-url]
                   (let [comma (.indexOf data-url ",")]
                     (media-map part-type (if (>= comma 0) (.slice data-url (inc comma)) data-url) mime)))))

      :else (js/Promise.resolve nil))))

(defrecord DefaultContentCodec []
  IContentCodec
  (content-parts->provider [_ content-parts]
    (mapv msg/stored-content-part->agent-part (or content-parts [])))

  (provider->content-parts [_ provider-content]
    (vec (or provider-content []))))

(defrecord DefaultMediaMaterializer []
  IMediaMaterializer
  (materialize-media! [_ content-part _auth-context]
    (materialize! content-part)))

(def default-content-codec
  (->DefaultContentCodec))

(def default-media-materializer
  (->DefaultMediaMaterializer))
