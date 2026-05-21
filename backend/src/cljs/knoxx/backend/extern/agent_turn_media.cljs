(ns knoxx.backend.extern.agent-turn-media
  "JS/browser boundary helpers for agent-turn media materialization.
   URL parsing, fetch request JS objects, auth header JS objects, response
   ArrayBuffer/Buffer conversion, and data URL string conversion live here."
  (:require [clojure.string :as str]))

(defn- nonblank
  [value]
  (some-> value str str/trim not-empty))

(defn resolve-media-url
  [value]
  (cond
    (not (string? value)) value
    (str/starts-with? value "/") (str "http://127.0.0.1:8000" value)
    :else value))

(defn data-url?
  [value]
  (and (string? value) (str/starts-with? value "data:")))

(defn looks-like-url?
  [value]
  (and (string? value)
       (or (str/starts-with? value "http://")
           (str/starts-with? value "https://"))))

(defn media-url?
  [value]
  (and (string? value)
       (or (looks-like-url? value)
           (str/starts-with? value "/"))))

(defn strip-data-url
  [raw-data]
  (when-let [data (nonblank raw-data)]
    (let [comma (.indexOf data ",")]
      (if (and (data-url? data) (>= comma 0))
        (.slice data (inc comma))
        data))))

(defn studio-stream-path
  [value]
  (try
    (let [url (js/URL. (resolve-media-url value))]
      (when (= (.-pathname url) "/api/studio/stream")
        (nonblank (.get (.-searchParams url) "path"))))
    (catch :default _ nil)))

(defn local-knoxx-url?
  [url]
  (and (string? url)
       (or (str/starts-with? url "http://127.0.0.1:8000/")
           (str/starts-with? url "http://localhost:8000/")
           (str/starts-with? url "http://0.0.0.0:8000/")
           (str/starts-with? url "http://knoxx.promethean.rest/")
           (str/starts-with? url "https://knoxx.promethean.rest/"))))

(defn auth-header-map
  [auth-context url]
  (let [resolved-url (resolve-media-url url)
        auth-email (or (nonblank (:userEmail auth-context))
                       (nonblank (:user-email auth-context))
                       (nonblank (get-in auth-context [:user :email])))
        auth-org-slug (or (nonblank (:orgSlug auth-context))
                          (nonblank (:org-slug auth-context))
                          (nonblank (get-in auth-context [:org :slug])))
        auth-membership-id (or (nonblank (:membershipId auth-context))
                               (nonblank (:membership-id auth-context))
                               (nonblank (get-in auth-context [:membership :id])))]
    (cond-> {}
      (and (local-knoxx-url? resolved-url) auth-email)
      (assoc :x-knoxx-user-email auth-email)

      (and (local-knoxx-url? resolved-url) auth-org-slug)
      (assoc :x-knoxx-org-slug auth-org-slug)

      (and (local-knoxx-url? resolved-url) auth-membership-id)
      (assoc :x-knoxx-membership-id auth-membership-id))))

(defn auth-headers
  [auth-context url]
  (clj->js (auth-header-map auth-context url)))

(defn- ensure-max-bytes!
  [size max-bytes message]
  (when (and (number? size)
             (number? max-bytes)
             (> size max-bytes))
    (throw (js/Error. message))))

(defn fetch-data-url-with-fetch!
  [fetch-fn url fallback-mime label max-bytes auth-context]
  (let [resolved-url (resolve-media-url url)
        label (or label "media")]
    (-> (fetch-fn resolved-url #js {:method "GET"
                                    :headers (auth-headers auth-context resolved-url)})
        (.then
         (fn [resp]
           (when-not (.-ok resp)
             (throw (js/Error. (str "Failed to fetch " label ": HTTP " (.-status resp)))))
           (let [len-header (some-> resp (.-headers) (.get "content-length"))
                 len (when len-header (js/parseInt len-header 10))]
             (when (and (number? len) (not (js/isNaN len)) (pos? len) (> len max-bytes))
               (throw (js/Error. (str "Remote " label " exceeds max bytes: " len))))
             (-> (.arrayBuffer resp)
                 (.then
                  (fn [array-buffer]
                    (let [buffer (js/Buffer.from array-buffer)
                          size (.-length buffer)
                          _ (ensure-max-bytes! size max-bytes (str "Remote " label " exceeds max bytes: " size))
                          mime-type (or (some-> resp (.-headers) (.get "content-type")) fallback-mime)
                          payload (.toString buffer "base64")]
                      (str "data:" mime-type ";base64," payload)))))))))))

(defn fetch-data-url!
  [url fallback-mime label max-bytes auth-context]
  (fetch-data-url-with-fetch! js/fetch url fallback-mime label max-bytes auth-context))
