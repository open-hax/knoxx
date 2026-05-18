(ns knoxx.backend.routes.workspace-media
  "HTTP routes for serving workspace media (audio/video/images/docs) as raw bytes.

   This is intended to support frontend embedding of media linked in Markdown.
   It deliberately reuses the same path normalization / root-allowlist logic as
   workspace_media.attach, but returns streamed content instead of base64.

   NOTE: This is a *browser* route, not a model tool. Access control is applied
   via request context when available (RBAC), but defaults to permissive when
   no auth context exists (typical for local/dev)."
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [knoxx.backend.tools.media :as media]
            ["node:fs" :as node-fs]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

(defn- reply-header!
  [^js reply name value]
  (.header reply name value))

(defn- request-header
  [^js request name]
  (let [headers (aget request "headers")]
    (when headers
      (aget headers name))))


(defn- safe-content-disposition
  "Build a Content-Disposition header value that handles non-ASCII filenames.
   Uses RFC 5987 encoding for filenames with special characters."
  [filename]
  (let [ascii-safe? (every? #(let [c (.charCodeAt % 0)]
                               (and (>= c 32) (<= c 126) (not= c 34) (not= c 92)))
                             (str filename))]
    (if ascii-safe?
      (str "inline; filename=\"" filename "\"")
      ;; RFC 5987: filename*=UTF-8''percent-encoded
      (let [encoded (js/encodeURIComponent filename)]
        (str "inline; filename*=UTF-8''" encoded)))))

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

                  (let [raw-path (or (aget request "query" "path") "")
                        normalized (media/normalize-tool-path-arg raw-path)
                        {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config normalized)
                        mime-type (mime-type-for-path relative absolute)
                        range-header (or (request-header request "range")
                                         (request-header request "Range"))]
                    (-> (media/fs-stat! fs absolute)
                        (.then (fn [stat]
                                 (when-not (.isFile stat)
                                   (throw (js/Error. (str relative " is not a file"))))
                                 (let [total-size (.-size stat)
                                       range (when (and range-header (pos? total-size))
                                               (parse-range-header range-header total-size))
                                       filename (media/path-basename path absolute)]
                                   (reply-header! reply "Content-Type" mime-type)
                                   (reply-header! reply "Accept-Ranges" "bytes")
                                   (reply-header! reply "Cache-Control" "private, max-age=0")
                                   (reply-header! reply "Content-Disposition" (safe-content-disposition filename))
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

(defn- audio-extensions
  "Set of recognized audio file extensions."
  []
  #{".mp3" ".wav" ".ogg" ".m4a" ".flac" ".aac" ".opus" ".wma"})

(defn- audio-mime-type
  [ext]
  (case ext
    ".mp3" "audio/mpeg"
    ".wav" "audio/wav"
    ".ogg" "audio/ogg"
    ".m4a" "audio/mp4"
    ".flac" "audio/flac"
    ".aac" "audio/aac"
    ".opus" "audio/opus"
    "audio/mpeg"))

(defn- walk-audio-files!
  "Recursively walk a directory and collect audio file metadata."
  [root-dir base-relative depth max-depth]
  (if (> depth max-depth)
    (p/resolved [])
    (-> (.readdir fs root-dir #js {:withFileTypes true})
        (.then (fn [entries]
                 (let [entries-arr (vec (array-seq entries))
                       promises (mapv
                                 (fn [entry]
                                   (let [name (.-name entry)
                                         abs-path (.join path root-dir name)
                                         rel-path (if (str/blank? base-relative)
                                                    name
                                                    (str base-relative "/" name))]
                                     (cond
                                       ;; Skip hidden files/dirs
                                       (str/starts-with? name ".")
                                       (p/resolved [])

                                       ;; Recurse into directories
                                       (.isDirectory entry)
                                       (walk-audio-files! abs-path rel-path (inc depth) max-depth)

                                       ;; Collect audio files
                                       :else
                                       (let [ext (str/lower-case (or (some-> (.extname path name) str/trim) ""))]
                                         (if (contains? (audio-extensions) ext)
                                           (-> (.stat fs abs-path)
                                               (.then (fn [stat]
                                                        [{:name name
                                                          :path rel-path
                                                          :ext ext
                                                          :size (.-size stat)
                                                          :modified (-> (.-mtime stat) (.getTime))
                                                          :mime (audio-mime-type ext)}]))
                                               (.catch (fn [_] [])))
                                           (p/resolved []))))))
                                 entries-arr)]
                   (-> (p/all (into-array promises))
                       (.then (fn [results]
                                (vec (mapcat identity results))))))))
        (.catch (fn [_] [])))))

(defn register-workspace-media-routes
  ([app runtime config handlers]
   (register-workspace-media-routes! app runtime config handlers)))

;; ── Audio library route ─────────────────────────────────────────────
(defn register-audio-library-routes!
  "Routes for the broadcast studio audio library."
  [app runtime config {:keys [route!
                              json-response!
                              error-response!
                              with-request-context!
                              ensure-tool!]}]
  ;; List audio files in workspace
  (route! app "GET" "/api/workspace-media/audio-library"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (when ctx
                    (ensure-tool! ctx "read"))
                  (let [;; Optional subdirectory filter (e.g. "Music" or "Music/midrolls")
                        subpath (or (aget request "query" "path") "")
                        max-depth (let [d (js/parseInt (or (aget request "query" "depth") "3") 10)]
                                    (if (js/isNaN d) 3 (min d 8)))
                        ;; Resolve the root to scan
                        scan-root (if (str/blank? subpath)
                                    (:workspace-root config)
                                    (let [normalized (media/normalize-tool-path-arg subpath)
                                          {:keys [absolute]} (media/resolve-workspace-media-path runtime config normalized)]
                                      absolute))
                        rel-base (str/trim (or subpath ""))]
                    (-> (walk-audio-files! scan-root rel-base 0 max-depth)
                        (.then (fn [files]
                                 ;; Sort by modified desc (newest first)
                                 (let [sorted (vec (sort-by :modified > files))]
                                   (json-response! reply 200
                                                   {:ok true
                                                    :root rel-base
                                                    :count (count sorted)
                                                    :files sorted}))))
                        (.catch (fn [err]
                                  (json-response! reply 500 {:detail (str "Failed to scan audio library: " err)})))))
                  (catch :default err
                    (error-response! reply err)))))))

  ;; Create/ensure a directory exists (for organizing clips)
  (route! app "POST" "/api/workspace-media/audio-library/ensure-dir"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (when ctx
                    (ensure-tool! ctx "write"))
                  (let [body (or (aget request "body") #js {})
                        dir-path (or (aget body "path") "")]
                    (if (str/blank? dir-path)
                      (json-response! reply 400 {:detail "path is required"})
                      (let [normalized (media/normalize-tool-path-arg dir-path)
                            {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config normalized)]
                        (-> (.mkdir fs absolute #js {:recursive true})
                            (.then (fn []
                                     (json-response! reply 200 {:ok true :path relative})))
                            (.catch (fn [err]
                                      (json-response! reply 500 {:detail (str "Failed to create directory: " err)})))))))
                  (catch :default err
                    (error-response! reply err)))))))

  ;; Rename/move an audio file
  (route! app "POST" "/api/workspace-media/audio-library/rename"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (try
                  (when ctx
                    (ensure-tool! ctx "write"))
                  (let [body (or (aget request "body") #js {})
                        from-path (or (aget body "from") "")
                        to-path (or (aget body "to") "")]
                    (cond
                      (str/blank? from-path)
                      (json-response! reply 400 {:detail "from is required"})

                      (str/blank? to-path)
                      (json-response! reply 400 {:detail "to is required"})

                      :else
                      (let [from-norm (media/normalize-tool-path-arg from-path)
                            to-norm (media/normalize-tool-path-arg to-path)
                            {from-abs :absolute} (media/resolve-workspace-media-path runtime config from-norm)
                            {to-abs :absolute} (media/resolve-workspace-media-path runtime config to-norm)]
                        (-> (.rename fs from-abs to-abs)
                            (.then (fn []
                                     (json-response! reply 200 {:ok true :from from-norm :to to-norm})))
                            (.catch (fn [err]
                                      (json-response! reply 500 {:detail (str "Rename failed: " err)})))))))
                  (catch :default err
                    (error-response! reply err))))))))
