(ns knoxx.backend.domain.openutau.tools
  (:require [clojure.string :as str]
            [knoxx.backend.domain.openutau.openutau :as openutau]
            [shadow.cljs.modern :refer [js-await]]))

(def render-script-path "/home/err/devel/services/utau-renderer/render-ustx.sh")

(def default-ustx-version openutau/default-ustx-version)
(def default-ticks-per-quarter openutau/default-ticks-per-quarter)
(def default-renderer openutau/default-renderer)
(def default-track-color openutau/default-track-color)
(def available-singers openutau/available-singers)
(def default-singer openutau/default-singer)
(def min-renderable-notes openutau/min-renderable-notes)

(def resolve-singer openutau/resolve-singer)
(def slugify openutau/slugify)
(def default-project-relative-path openutau/default-project-relative-path)
(def sanitize-lyric openutau/sanitize-lyric)
(def lyric-text openutau/lyric-text)
(def normalize-notes openutau/normalize-notes)
(def build-project openutau/build-project)
(def emit-yaml-lines openutau/emit-yaml-lines)
(def project->ustx-yaml openutau/project->ustx-yaml)
(def readme-markdown openutau/readme-markdown)

(defn render-ustx-to-wav
  "Render a .ustx file to .wav using the headless OpenUTAU pipeline.
   Returns a promise that resolves to {:wav_path string} or rejects with error."
  [ustx-path output-wav-path]
  (let [child-process (js/require "node:child_process")
        util (js/require "node:util")
        exec-file (.promisify util (.-execFile child-process))
        script render-script-path]
    (js-await [result (exec-file script #js [ustx-path output-wav-path]
                                 #js {:timeout 600000 :maxBuffer 4194304})]
      (let [stdout (.-stdout result)]
        (if (str/includes? stdout "Success!")
          {:wav_path output-wav-path
           :stdout stdout}
          (throw (js/Error. (str "Render did not report success. stdout: " stdout))))))))
