(ns knoxx.backend.multimodal-routes
  "Routes for multimodal file uploads and content serving.
   
   Supports images, audio, video, and documents for multimodal AI interactions.
   Files are stored temporarily and served back to the frontend for preview/playback."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [with-request-context! ensure-permission!]]
            [knoxx.backend.http :refer [json-response! error-response! js-array-seq]]
            [knoxx.backend.runtime-config :refer [now-iso]]))

(def ^:private upload-dir "uploads/multimodal")
(def ^:private max-file-size-bytes (* 100 1024 1024)) ;; 100MB

(def ^:private supported-mime-types
  #{"image/png" "image/jpeg" "image/gif" "image/webp" "image/svg+xml"
    "audio/mpeg" "audio/mp3" "audio/wav" "audio/ogg" "audio/m4a" "audio/flac" "audio/aac"
    "video/mp4" "video/webm" "video/quicktime" "video/x-msvideo"
    "application/pdf"
    "text/plain" "text/markdown" "text/csv" "application/json"})

(defn- sanitize-filename
  "Sanitize a filename to prevent directory traversal and other attacks."
  [filename]
  (let [safe-name (-> (or filename "upload.bin")
                      (str/replace #"[^\w\-.]" "_")
                      (str/replace #"_+" "_"))]
    (if (str/blank? safe-name)
      "upload.bin"
      safe-name)))

(defn- mime-type-supported?
  "Check if the MIME type is supported for multimodal upload."
  [mime-type]
  (or (some #(str/starts-with? mime-type %) ["image/" "audio/" "video/"])
      (contains? supported-mime-types mime-type)))

(defn- content-type-from-mime
  "Determine the content category from MIME type."
  [mime-type]
  (cond
    (str/starts-with? mime-type "image/") "image"
    (str/starts-with? mime-type "audio/") "audio"
    (str/starts-with? mime-type "video/") "video"
    :else "document"))

(defn- generate-file-id
  "Generate a unique file ID."
  []
  (str (js/Date.now) "-" (.. js/Math.random (toString 36) (slice 2 11))))

(defn- ensure-upload-dir!
  "Ensure the upload directory exists."
  [runtime]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")
        upload-path (.join node-path upload-dir)]
    (.then
     (.mkdir node-fs upload-path #js {:recursive true})
     (fn [] upload-path))))

(defn- save-upload-file!
  "Save an uploaded file and return its metadata."
  [runtime config file-part filename]
  (let [node-fs (aget runtime "fs")
        node-path (aget runtime "path")]
    (.then
     (ensure-upload-dir! runtime)
     (fn [upload-path]
       (let [file-id (generate-file-id)
             safe-name (sanitize-filename filename)
             ext (if (str/includes? safe-name ".")
                   (let [dot-idx (str/last-index-of safe-name ".")]
                     (subs safe-name dot-idx))
                   "")
             stored-name (str file-id ext)
             abs-path (.join node-path upload-path stored-name)]
         (.then
          (.arrayBuffer (js/Response. (aget file-part "file")))
          (fn [buf]
            (.then
             (.writeFile node-fs abs-path (.from js/Buffer buf))
             (fn []
               {:file_id file-id
                :filename safe-name
                :stored_name stored-name
                :path abs-path
                :url (str "/api/multimodal/files/" file-id)
                :size (.-byteLength buf)})))))))))

(defn register-multimodal-routes!
  "Register routes for multimodal file handling."
  [app runtime config {:keys [route! json-response! error-response!
                              with-request-context! ensure-permission!]}]
  
  ;; Upload files for multimodal messages
  (route! app "POST" "/api/multimodal/upload"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "multimodal.upload"))
                (-> (.fromAsync js/Array (.parts request))
                    (.then
                     (fn [parts]
                       (let [part-seq (js-array-seq parts)
                             file-parts (filter #(= (aget % "type") "file") part-seq)
                             upload-promises
                             (mapv
                              (fn [part]
                                (let [filename (or (aget part "filename") "upload.bin")
                                      mime-type (or (aget part "type") "application/octet-stream")]
                                  (if-not (mime-type-supported? mime-type)
                                    (js/Promise.resolve
                                     {:error (str "Unsupported file type: " mime-type)
                                      :filename filename})
                                    (if (> (aget part "size" 0) max-file-size-bytes)
                                      (js/Promise.resolve
                                       {:error (str "File too large. Max: " (/ max-file-size-bytes 1024 1024) "MB")
                                        :filename filename})
                                      (.then
                                       (save-upload-file! runtime config part filename)
                                       (fn [result]
                                         (assoc result
                                                :mime_type mime-type
                                                :content_type (content-type-from-mime mime-type)
                                                :uploaded_at (now-iso))))))))
                              file-parts)]
                         (.then
                          (js/Promise.all (clj->js upload-promises))
                          (fn [results]
                            (let [uploads (js-array-seq results)
                                  successful (filter #(not (:error %)) uploads)
                                  failed (filter :error uploads)]
                              (json-response! reply 200
                                              {:ok true
                                               :uploaded (vec successful)
                                               :failed (vec failed)
                                               :total (count uploads)}))))))
                    (.catch
                     (fn [err]
                       (json-response! reply 500
                                       {:detail (str "Upload failed: " err)})))))))))
  
  ;; Serve uploaded files
  (route! app "GET" "/api/multimodal/files/:fileId"
          (fn [request reply]
            (let [node-fs (aget runtime "fs")
                  node-path (aget runtime "path")
                  file-id (aget request "params" "fileId")]
              (-> (.readdir node-fs (.join node-path upload-dir))
                  (.then
                   (fn [files]
                     (let [matching (first (filter #(str/starts-with? % file-id) (js-array-seq files)))]
                       (if matching
                         (let [abs-path (.join node-path upload-dir matching)]
                           (-> (.readFile node-fs abs-path)
                               (.then
                                (fn [buf]
                                  (let [ext (if (str/includes? matching ".")
                                              (subs matching (str/last-index-of matching "."))
                                              "")
                                        content-type (cond
                                                      (contains? #{".png"} ext) "image/png"
                                                      (contains? #{".jpg" ".jpeg"} ext) "image/jpeg"
                                                      (contains? #{".gif"} ext) "image/gif"
                                                      (contains? #{".webp"} ext) "image/webp"
                                                      (contains? #{".svg"} ext) "image/svg+xml"
                                                      (contains? #{".mp3"} ext) "audio/mpeg"
                                                      (contains? #{".wav"} ext) "audio/wav"
                                                      (contains? #{".ogg"} ext) "audio/ogg"
                                                      (contains? #{".mp4"} ext) "video/mp4"
                                                      (contains? #{".webm"} ext) "video/webm"
                                                      (contains? #{".pdf"} ext) "application/pdf"
                                                      :else "application/octet-stream")]
                                    (.header reply "Content-Type" content-type)
                                    (.header reply "Cache-Control" "public, max-age=31536000")
                                    (.send reply buf))))))
                         (json-response! reply 404 {:detail "File not found"})))))
                  (.catch
                   (fn [err]
                     (json-response! reply 500
                                     {:detail (str "Failed to read file: " err)})))))))
  
  ;; Delete uploaded files
  (route! app "DELETE" "/api/multimodal/files/:fileId"
          (fn [request reply]
            (with-request-context! runtime request reply
              (fn [ctx]
                (when ctx (ensure-permission! ctx "multimodal.upload"))
                (let [node-fs (aget runtime "fs")
                      node-path (aget runtime "path")
                      file-id (aget request "params" "fileId")]
                  (-> (.readdir node-fs (.join node-path upload-dir))
                      (.then
                       (fn [files]
                         (let [matching (first (filter #(str/starts-with? % file-id) (js-array-seq files)))]
                           (if matching
                             (let [abs-path (.join node-path upload-dir matching)]
                               (-> (.rm node-fs abs-path)
                                   (.then
                                    (fn []
                                      (json-response! reply 200
                                                      {:ok true :deleted file-id})))))
                             (json-response! reply 404 {:detail "File not found"})))))
                      (.catch
                       (fn [err]
                         (json-response! reply 500
                                         {:detail (str "Delete failed: " err)})))))))))
  
  ;; Get upload metadata
  (route! app "GET" "/api/multimodal/info"
          (fn [_request reply]
            (json-response! reply 200
                           {:max_file_size_bytes max-file-size-bytes
                            :max_file_size_mb (/ max-file-size-bytes 1024 1024)
                            :supported_mime_types (vec supported-mime-types)}))))

(defn register-multimodal-routes
  ([app runtime config handlers]
   (register-multimodal-routes! app runtime config handlers)))
