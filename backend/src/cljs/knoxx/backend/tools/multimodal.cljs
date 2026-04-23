(ns knoxx.backend.tools.multimodal
  "Multimodal upload tools for images, audio, video, and documents."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional]]))

(defn create-multimodal-custom-tools
  ([runtime config] (create-multimodal-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))
         upload-params (.Object Type
                               #js {:source (.String Type #js {:description "Workspace path, URL, or data URL for the media to load."})
                                    :title (type-optional Type (.String Type #js {:description "Optional human-readable title for the media."}))})
         upload-execute (fn [_tool-call-id params a b c]
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
                                             :content_parts [part*]})))))))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "multimodal.upload")
                  (doto (js-obj)
                    (aset "name" "multimodal.upload")
                    (aset "label" "Multimodal Upload")
                    (aset "description" "Load an image, audio file, video, or document from the workspace, a URL, or a data URL so the model can inspect it.")
                    (aset "promptSnippet" "Load external or workspace media into the multimodal conversation context.")
                    (aset "promptGuidelines" (clj->js ["Use multimodal.upload when the user shares a media URL or asks you to inspect audio, images, video, or documents."
                                                       "Prefer multimodal.upload for remote or inline media; prefer workspace_media.attach when the goal is to include a workspace file in the final reply."
                                                       "If the model contract does not support a media type directly, explain that and consider audio.spectrogram for audio analysis via vision."]))
                    (aset "parameters" upload-params)
                    (aset "execute" upload-execute)))]))))))

