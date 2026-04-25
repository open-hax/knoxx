(ns knoxx.backend.tools.voice
  "OpenUtau/voice synthesis tool factories."
  (:require [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [tool-text-result]]
            [knoxx.backend.tools.media :as media :refer [normalize-tool-path-arg]]
            [knoxx.backend.tools.openutau :as openutau]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional]]
            [knoxx.backend.document-state :refer [normalize-relative-path]]))

(defn create-voice-synth-custom-tools
  ([runtime config] (create-voice-synth-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         node-fs (aget runtime "fs")
         node-path (aget runtime "path")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))
         openutau-note-params (.Object Type
                                      #js {:lyric (type-optional Type (.String Type #js {:description "Lyric to place on the note. Use + or +~ for slurs when needed."}))
                                           :phonetic_hint (type-optional Type (.String Type #js {:description "Optional OpenUtau phonetic hint without brackets, e.g. 'l aa'."}))
                                           :tone (.Number Type #js {:description "MIDI note number, where C4 = 60."})
                                           :duration (.Number Type #js {:description "Note duration in ticks. OpenUtau uses 480 ticks per quarter note."})
                                           :position (type-optional Type (.Number Type #js {:description "Optional start tick. If omitted, notes are placed sequentially."}))})
         time-signature-params (.Object Type
                                       #js {:beat_per_bar (type-optional Type (.Number Type #js {:description "Time signature numerator."}))
                                            :beat_unit (type-optional Type (.Number Type #js {:description "Time signature denominator."}))})
         openutau-project-params (.Object Type
                                         #js {:project_name (.String Type #js {:description "Human-readable OpenUtau project name."})
                                              :notes (.Array Type openutau-note-params #js {:description "Ordered note plan for the OpenUtau vocal part."})
                                              :tempo (type-optional Type (.Number Type #js {:description "Tempo in BPM. Defaults to 120."}))
                                              :time_signature (type-optional Type time-signature-params)
                                              :singer_id (type-optional Type (.String Type #js {:description "Optional OpenUtau singer id/folder name. Leave blank to choose in the UI later."}))
                                              :phonemizer (type-optional Type (.String Type #js {:description "Optional OpenUtau phonemizer class/tag. Leave blank to choose in the UI later."}))
                                              :track_name (type-optional Type (.String Type #js {:description "Optional vocal track name."}))
                                              :part_name (type-optional Type (.String Type #js {:description "Optional voice part name."}))
                                              :output_path (type-optional Type (.String Type #js {:description "Optional workspace-relative output path for the .ustx file."}))
                                              :comment (type-optional Type (.String Type #js {:description "Optional project comment embedded in the .ustx file."}))})
         openutau-project-execute (fn [_tool-call-id params a b c]
                                    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                                          project-name (or (normalize-tool-path-arg (aget params "project_name")) "Knoxx OpenUtau Project")
                                          requested-path (or (normalize-tool-path-arg (aget params "output_path"))
                                                             (openutau/default-project-relative-path project-name))
                                          {:keys [workspace-root absolute relative]} (media/resolve-workspace-media-path runtime config requested-path)
                                          output-dir (.dirname node-path absolute)
                                          filename (media/path-basename node-path absolute)
                                          readme-absolute (.join node-path output-dir "README.md")
                                          readme-relative (normalize-relative-path (media/path-relative node-path workspace-root readme-absolute))
                                          notes (openutau/normalize-notes (js->clj (or (aget params "notes") #js []) :keywordize-keys true))
                                          project (openutau/build-project {:project_name (aget params "project_name")
                                                                          :tempo (aget params "tempo")
                                                                          :time_signature (js->clj (or (aget params "time_signature") #js {}) :keywordize-keys true)
                                                                          :singer_id (aget params "singer_id")
                                                                          :phonemizer (aget params "phonemizer")
                                                                          :track_name (aget params "track_name")
                                                                          :part_name (aget params "part_name")
                                                                          :comment (aget params "comment")}
                                                                         notes)
                                          ustx-yaml (openutau/project->ustx-yaml project)
                                          readme-text (openutau/readme-markdown {:project-name project-name
                                                                                 :ustx-path relative
                                                                                 :readme-path readme-relative
                                                                                 :note-count (count notes)
                                                                                 :tempo (or (aget params "tempo") 120)
                                                                                 :singer-id (or (aget params "singer_id") "")
                                                                                 :phonemizer (or (aget params "phonemizer") "")})
                                          data-url (str "data:text/yaml;base64," (.toString (.from js/Buffer ustx-yaml "utf8") "base64"))]
                                      (when-not (seq notes)
                                        (throw (js/Error. "notes must contain at least one note")))
                                      (maybe-tool-update! on-update (str "Writing OpenUtau project " relative "…"))
                                      (-> (media/fs-mkdir! node-fs output-dir #js {:recursive true})
                                          (.then (fn [] (media/fs-write-file! node-fs absolute ustx-yaml "utf8")))
                                          (.then (fn [] (media/fs-write-file! node-fs readme-absolute readme-text "utf8")))
                                          (.then (fn []
                                                   (tool-text-result
                                                    (str "Created OpenUtau project at " relative
                                                         " with " (count notes) " notes. Open it in OpenUtau and export audio from the UI.")
                                                    {:path relative
                                                     :readme_path readme-relative
                                                     :project_name project-name
                                                     :note_count (count notes)
                                                     :tempo (or (aget params "tempo") 120)
                                                     :renderer openutau/default-renderer
                                                     :headless_render_supported false
                                                     :content_parts [{:type "document"
                                                                      :data data-url
                                                                      :mimeType "text/yaml"
                                                                      :filename filename}]}))))))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "voice.openutau_project")
                  (doto (js-obj)
                    (aset "name" "voice.openutau_project")
                    (aset "label" "OpenUtau Project")
                    (aset "description" "Create an OpenUtau .ustx singing project from structured notes so a human can open it and export audio.")
                    (aset "promptSnippet" "Generate an OpenUtau singing project when the user wants lyric-timed vocal synthesis work or a render-ready handoff.")
                    (aset "promptGuidelines" (clj->js ["Use voice.openutau_project when the user wants singing synthesis via OpenUtau or an editable vocal project."
                                                       "Provide explicit notes with lyric, MIDI tone, and duration in ticks; add phonetic_hint when pronunciation needs help."
                                                       "OpenUtau export is UI-driven, so do not pretend you already rendered audio unless a real audio artifact exists."
                                                       "WORLDLINE-R is the default safe renderer in the generated project."]))
                    (aset "parameters" openutau-project-params)
                    (aset "execute" openutau-project-execute)))]))))))

