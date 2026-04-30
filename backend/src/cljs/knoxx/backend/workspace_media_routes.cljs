(ns knoxx.backend.workspace-media-routes
  "HTTP routes for serving workspace media (audio/video/images/docs) as raw bytes.

   This is intended to support frontend embedding of media linked in Markdown.
   It deliberately reuses the same path normalization / root-allowlist logic as
   workspace_media.attach, but returns streamed content instead of base64.

   NOTE: This is a *browser* route, not a model tool. Access control is applied
   via request context when available (RBAC), but defaults to permissive when
   no auth context exists (typical for local/dev)."
  (:require [clojure.string :as str]
            [knoxx.backend.tools.media :as media]))

(defn- reply-header!
  [^js reply name value]
  (.header reply name value))

(defn- request-header
  [^js request name]
  (let [headers (aget request "headers")]
    (when headers
      (aget headers name))))


(defn- mime-type-for-path
  [relative absolute]
  (or (media/workspace-media-mime-type relative)
      (media/workspace-media-mime-type absolute)
      "application/octet-stream"))

(defn- parse-range-header
  "Parse a Range header of the form: bytes=start-end.
   Returns {:start n :end n :length n} or nil when invalid/unsupported."
  [range-header total-size]
  (let [raw (some-> range-header str str/trim)
        match (when (and raw (str/starts-with? raw "bytes="))
                (.match raw #"^bytes=(\\d*)-(\\d*)$"))]
    (when match
      (let [start-str (aget match 1)
            end-str (aget match 2)
            start (when (and start-str (not (str/blank? start-str)))
                    (js/parseInt start-str 10))
            end (when (and end-str (not (str/blank? end-str)))
                  (js/parseInt end-str 10))
            start* (cond
                     (and (number? start) (not (js/isNaN start))) start
                     ;; suffix range "bytes=-500" => last 500 bytes
                     (and (nil? start) (number? end) (not (js/isNaN end))) (max 0 (- total-size end))
                     :else nil)
            end* (cond
                   (and (number? end) (not (js/isNaN end))) (min (dec total-size) end)
                   :else (dec total-size))]
        (when (and (number? start*) (<= 0 start*) (< start* total-size) (<= start* end*))
          {:start start*
           :end end*
           :length (inc (- end* start*))})))))

(defn register-workspace-media-routes!
  [app runtime config {:keys [route!
                              json-response!
                              error-response!
                              with-request-context!
                              ensure-tool!]}]
  (route! app "GET" "/api/workspace-media/raw"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  ;; Reuse the existing tool gate when RBAC is enabled.
                  ;; If ctx is nil (dev/no-auth), we allow access.
                  (when ctx
                    (ensure-tool! ctx "read"))

                  (let [^js node-fs (aget runtime "fs")
                        node-path (aget runtime "path")
                        raw-path (or (aget request "query" "path") "")
                        normalized (media/normalize-tool-path-arg raw-path)
                        {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config normalized)
                        mime-type (mime-type-for-path relative absolute)
                        range-header (or (request-header request "range")
                                         (request-header request "Range"))]
                    (-> (media/fs-stat! node-fs absolute)
                        (.then (fn [stat]
                                 (when-not (.isFile stat)
                                   (throw (js/Error. (str relative " is not a file"))))
                                 (let [total-size (.-size stat)
                                       range (when (and range-header (pos? total-size))
                                               (parse-range-header range-header total-size))
                                       filename (media/path-basename node-path absolute)]
                                   (reply-header! reply "Content-Type" mime-type)
                                   (reply-header! reply "Accept-Ranges" "bytes")
                                   (reply-header! reply "Cache-Control" "private, max-age=0")
                                   (reply-header! reply "Content-Disposition" (str "inline; filename=\"" filename "\""))
                                   (if range
                                     (let [{:keys [start end length]} range
                                           stream (.createReadStream node-fs absolute #js {:start start :end end})]
                                       (reply-header! reply "Content-Range" (str "bytes " start "-" end "/" total-size))
                                       (reply-header! reply "Content-Length" (str length))
                                       (.code reply 206)
                                       (.send reply stream))
                                     (let [stream (.createReadStream node-fs absolute)]
                                       (reply-header! reply "Content-Length" (str total-size))
                                       (.code reply 200)
                                       (.send reply stream))))))
                        (.catch (fn [err]
                                 (json-response! reply 404 {:detail (str err)})))))
                  (catch :default err
                    (error-response! reply err))))))))

(defn register-workspace-media-routes
  ([app runtime config handlers]
   (register-workspace-media-routes! app runtime config handlers)))
