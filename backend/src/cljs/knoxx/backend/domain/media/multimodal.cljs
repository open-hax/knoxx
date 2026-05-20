(ns knoxx.backend.tools.multimodal
  "Multimodal upload tools for images, audio, video, and documents."
  (:require [knoxx.backend.domain.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.tools.media :as media :refer [normalize-tool-path-arg media-source->content-part! multimodal-upload-max-bytes]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! create-tool-obj]]))

(def upload-params
  [:map
   [:source {:description "Workspace path, URL, or data URL for the media to load."} :string]
   [:title {:optional true :description "Optional human-readable title for the media."} :string]])

(defn upload-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        source (or (aget params "source") "")
        title (normalize-tool-path-arg (aget params "title"))]
    (maybe-tool-update! on-update "Loading multimodal media…")
    (-> (media-source->content-part! runtime config source multimodal-upload-max-bytes)
        (.then (fn [{asset :source part :part}]
                 (let [part* (cond-> part
                               title (assoc :filename title))
                       label (case (:type part*)
                               "image" "image"
                               "audio" "audio"
                               "video" "video"
                               "document")]
                   (tool-text-result
                    (str "Loaded " label " " (or title (:filename part*) (:filename asset) source) " for multimodal model context.")
                    {:source source
                     :source_kind (:source-kind asset)
                     :mimeType (:mimeType part*)
                     :filename (:filename part*)
                     :content_parts [part*]})))))))

(def upload-tool
  (partial create-tool-obj
           "multimodal.upload"
           "Multimodal Upload"
           "Load an image, audio file, video, or document from the workspace, a URL, or a data URL so the model can inspect it."
           "Load external or workspace media into the multimodal conversation context."
           ["Use multimodal.upload when the user shares a media URL or asks you to inspect audio, images, video, or documents."
            "Prefer multimodal.upload for remote or inline media; prefer workspace_media.attach when the goal is to include a workspace file in the final reply."
            "If the model contract does not support a media type directly, explain that and consider audio.spectrogram for audio analysis via vision."]
           upload-params
           upload-execute))

(defn create-multimodal-custom-tools
  ([runtime config] (create-multimodal-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "multimodal.upload")
                  (upload-tool runtime config))]))))))
