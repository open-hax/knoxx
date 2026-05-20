(ns knoxx.backend.domain.media.workspace
  "Workspace media attachment tools."
  (:require [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.media :as media]
            [knoxx.backend.domain.tools :refer [maybe-tool-update! create-tool-obj]]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]))

(def attach-params
  [:map
   [:path {:description "Workspace-relative path to the image, audio file, video, or document to attach."} :string]
   [:title {:optional true :description "Optional human-readable label for the attachment."} :string]])

(defn attach-execute [runtime config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
        raw-path (or (aget params "path") "")
        title (media/normalize-tool-path-arg (aget params "title"))
        {:keys [absolute relative]} (media/resolve-workspace-media-path runtime config raw-path)
        mime-type (media/workspace-media-mime-type relative)
        filename (media/path-basename path absolute)]
    (when-not mime-type
      (throw (js/Error. (str "Unsupported workspace media type for " relative ". Supported formats: images, audio, video, pdf, txt, md, csv, json."))))
    (maybe-tool-update! on-update (str "Attaching workspace file " relative "…"))
    (-> (media/fs-stat! fs absolute)
        (.then (fn [stat]
                 (when-not (.isFile stat)
                   (throw (js/Error. (str relative " is not a file"))))
                 (when (> (.-size stat) media/workspace-media-max-bytes)
                   (throw (js/Error. (str "File exceeds " media/workspace-media-max-bytes " bytes. Choose a smaller file or summarize it instead."))))
                 (media/fs-read-file! fs absolute)))
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
                                      :content_parts (clj->js [part])}}))))))

(def attach-tool
  (partial create-tool-obj
           "workspace_media.attach"
           "Attach Workspace Media"
           "Attach an image, audio file, video, or document from the workspace so the user can see or play it inline."
           "Attach a workspace image, audio file, video, or document directly into the reply when the user asks to show or play a file."
           ["Use workspace_media.attach when the user wants to show, display, render, play, or attach a workspace file."
            "Prefer this tool over replying with only a path when the user explicitly wants the media itself."
            "Pass a workspace-relative path when possible."
            "If the file is too large or unsupported, explain that clearly and offer the path instead."]
           attach-params
           attach-execute))

(defn create-workspace-media-custom-tools
  ([runtime config] (create-workspace-media-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "workspace_media.attach")
                  (attach-tool runtime config))]))))))
