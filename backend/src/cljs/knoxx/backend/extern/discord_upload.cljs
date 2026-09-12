(ns knoxx.backend.extern.discord-upload
  "Discord attachment loading, native data URLs and SVG rendering."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.discord.rest-client :as discord-rest]
            [knoxx.backend.domain.media :as media]
            [knoxx.backend.domain.text :as text]
            [knoxx.backend.infra.svg-render :as svg-render]))

(defn- infer-upload-filename
[url idx]
(let [pathname (try (.-pathname (js/URL. (str url))) (catch :default _ ""))
      candidate (some-> pathname (str/split #"/") last str/trim not-empty)]
  (or candidate (str "attachment-" idx ".bin"))))

(defn- file-url->path
[source]
(try
  (js/decodeURIComponent (.-pathname (js/URL. source)))
  (catch :default _
    (subs source (count "file://")))))

(defn- svg-buffer->png-buffer!
"Render an SVG buffer to PNG using headless Chromium. Returns a promise."
[svg-buffer]
(let [svg-str (text/sanitize-svg-content (.toString svg-buffer "utf8"))]
  (svg-render/svg->png svg-str {:width 600 :height 300})))

(def svg-code-block-pattern
  "Discord boundary operation: svg-code-block-pattern."
#"(?is)```(?:svg|image/svg\+xml)\s*\n([\s\S]*?)\n```")

(defn extract-inline-svg-code-blocks
"Pull fenced ```svg code blocks out of message text so they can be rendered
   and attached as PNGs instead of being sent as raw code."
[text]
(let [raw-text (str (or text ""))
      matches (re-seq svg-code-block-pattern raw-text)
      svg-blocks (->> matches
                      (map second)
                      (map #(str/trim (str %)))
                      (filter #(str/includes? (str/lower-case %) "<svg"))
                      vec)
      cleaned-text (reduce (fn [acc [full-match _svg]]
                             (str/replace acc full-match "[image]"))
                           raw-text
                           matches)
      attachment-urls (mapv (fn [svg]
                              (str "data:image/svg+xml;charset=utf-8,"
                                   (js/encodeURIComponent svg)))
                            svg-blocks)]
  {:text cleaned-text
   :attachmentUrls attachment-urls}))

(defn ^:async maybe-render-svg!
"If the resolved attachment is an SVG, render it to PNG transparently.
   On render failure, returns original attachment."
  [{filename :name :keys [mimeType buffer] :as attachment}]
  (if (or (= mimeType "image/svg+xml")
          (some-> filename str/lower-case (str/ends-with? ".svg")))
    (try
      (let [png-buf (await (svg-buffer->png-buffer! buffer))]
        {:name (if (some-> filename str/lower-case (str/ends-with? ".svg"))
                 (str/replace filename #"(?i)\.svg$" ".png")
                 (str (or filename "attachment") ".png"))
         :mimeType "image/png"
         :buffer png-buf})
      (catch :default error
        (.warn js/console "[discord-tools] SVG render failed; uploading original SVG" error)
        attachment))
    attachment))

(defn- data-url-upload-attachment [source idx]
  (let [data-start (.indexOf source ",")
        metadata (when (>= data-start 0) (subs source 5 data-start))
        payload (when (>= data-start 0) (subs source (inc data-start)))]
    (when (or (nil? metadata) (nil? payload))
      (throw (js/Error. "Invalid data URL attachment")))
    (let [metadata-parts (str/split metadata #";")
          mime-type (media/sanitize-mime-type (first metadata-parts) "application/octet-stream")
          base64? (boolean (some #{"base64"} (rest metadata-parts)))
          buffer (if base64?
                   (.from js/Buffer payload "base64")
                   (.from js/Buffer (js/decodeURIComponent payload) "utf8"))]
      (media/ensure-source-size! (.-length buffer) media/workspace-media-max-bytes "Discord attachment")
      {:name (str "attachment-" idx (media/mime-type->extension mime-type))
       :mimeType mime-type
       :buffer buffer})))

(defn ^:async http-upload-attachment!
  "Discord boundary operation: http-upload-attachment!." [source idx]
  (let [{:keys [ok status headers body]} (await (discord-rest/fetch-attachment! (discord-rest/client nil) source))]
    (if ok
      (let [buffer (.from js/Buffer body)
            mime-type (media/sanitize-mime-type (get headers "content-type")
                                                (media/workspace-media-mime-type source))]
        (media/ensure-source-size! (.-length buffer) media/workspace-media-max-bytes "Discord attachment")
        {:name (infer-upload-filename source idx)
         :mimeType mime-type
         :buffer buffer})
      (throw (js/Error. (str "Attachment fetch failed " status))))))

(defn ^:async local-upload-attachment!
  "Discord boundary operation: local-upload-attachment!." [runtime config source idx]
  ;; Local file path — resolve through shared workspace media rules so
  ;; @-prefixed, workspace-relative, and allowed absolute paths work, while
  ;; paths outside allowed media roots are rejected before Discord sees them.
  (let [raw-source (if (media/source-file-url? source)
                     (file-url->path source)
                     source)
        loaded (await (media/load-media-source! runtime config raw-source media/workspace-media-max-bytes))]
    {:name (or (:filename loaded)
               (str "attachment-" idx (media/mime-type->extension (:mime-type loaded))))
     :mimeType (media/sanitize-mime-type (:mime-type loaded) "application/octet-stream")
     :buffer (:buffer loaded)}))

(defn ^:async fetch-discord-upload-attachment!
  "Fetch an attachment from a URL, data URL, or local file path.
   Returns a promise resolving to {:name :mimeType :buffer}.
   SVG files are automatically rendered to PNG before upload."
  [runtime config url idx]
  (let [source (str (or url ""))]
    (cond
      (str/blank? source) (throw (js/Error. "Empty attachment source"))
      (media/source-data-url? source) (data-url-upload-attachment source idx)
      (media/source-http-url? source) (await (http-upload-attachment! source idx))
      :else (await (local-upload-attachment! runtime config source idx)))))

(defn ^:async resolve-discord-upload-attachment!
  "Discord boundary operation: resolve-discord-upload-attachment!."
  [runtime config idx url]
  (await (maybe-render-svg! (await (fetch-discord-upload-attachment! runtime config url idx)))))

