(ns knoxx.backend.tools.workspace-media
  "Workspace media attachment tools."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.media :as media]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional]]))

(defn create-workspace-media-custom-tools
  ([runtime config] (create-workspace-media-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         node-fs (aget runtime "fs")
         node-path (aget runtime "path")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))
         attach-params (.Object Type
                                #js {:path (.String Type #js {:description "Workspace-relative path to the image, audio file, video, or document to attach."})
                                     :title (type-optional Type (.String Type #js {:description "Optional human-readable label for the attachment."}))})
         attach-execute (fn [_tool-call-id params a b c]
                          (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                raw-path (or (aget params "path") "")
                                title (media/normalize-tool-path-arg (aget params "title"))
                                {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config raw-path)
                                mime-type (media/workspace-media-mime-type relative)
                                filename (media/path-basename node-path absolute)]
                            (when-not mime-type
                              (throw (js/Error. (str "Unsupported workspace media type for " relative ". Supported formats: images, audio, video, pdf, txt, md, csv, json."))))
                            (maybe-tool-update! on-update (str "Attaching workspace file " relative "…"))
                            (-> (media/fs-stat! node-fs absolute)
                                (.then (fn [stat]
                                         (when-not (.isFile stat)
                                           (throw (js/Error. (str relative " is not a file"))))
                                         (when (> (.-size stat) media/workspace-media-max-bytes)
                                           (throw (js/Error. (str "File exceeds " media/workspace-media-max-bytes " bytes. Choose a smaller file or summarize it instead."))))
                                         (media/fs-read-file! node-fs absolute)))
                                (.then (fn [buffer]
                                         (let [size (.-length buffer)
                                               data-url (str "data:" mime-type ";base64," (.toString buffer "base64"))
                                               part {:type (media/workspace-media-type mime-type)
                                                     :data data-url
                                                     :mimeType mime-type
                                                     :filename (or title filename)
                                                     :size size}
                                               label (case (:type part)
                                                       "image" "image"
                                                       "audio" "audio file"
                                                       "video" "video"
                                                       "document" "document"
                                                       "file")]
                                           #js {:content #js [#js {:type "text"
                                                                   :text (str "Attached workspace " label " " relative " for the final reply.")}]
                                                :details #js {:path relative
                                                              :title (or title filename)
                                                              :content_parts (clj->js [part])}}))))))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "workspace_media.attach")
                  (doto (js-obj)
                    (aset "name" "workspace_media.attach")
                    (aset "label" "Attach Workspace Media")
                    (aset "description" "Attach an image, audio file, video, or document from the workspace so the user can see or play it inline.")
                    (aset "promptSnippet" "Attach a workspace image, audio file, video, or document directly into the reply when the user asks to show or play a file.")
                    (aset "promptGuidelines" (clj->js ["Use workspace_media.attach when the user wants to show, display, render, play, or attach a workspace file."
                                                       "Prefer this tool over replying with only a path when the user explicitly wants the media itself."
                                                       "Pass a workspace-relative path when possible."
                                                       "If the file is too large or unsupported, explain that clearly and offer the path instead."]))
                    (aset "parameters" attach-params)
                    (aset "execute" attach-execute)))]))))))

