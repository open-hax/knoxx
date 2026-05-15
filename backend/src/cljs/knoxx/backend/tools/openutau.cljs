(ns knoxx.backend.tools.openutau
  (:require [clojure.string :as str]))

(def default-ustx-version "0.6")
(def default-ticks-per-quarter 480)
(def default-renderer "WORLDLINE-R")
(def default-track-color "Blue")
(def render-script-path "/home/err/devel/services/utau-renderer/render-ustx.sh")

;; Available singers with their phonemizers
(def available-singers
  {"teto" {:name "重音テト OU用日本語統合ライブラリー"
           :phonemizer "OpenUtau.Core.Ustx.JapaneseCVPhonemizer"
           :language "ja"
           :description "Kasane Teto - Japanese integrated voicebank"}
   "ritsu" {:name "波音リツ連続音Ver1.5.1"
             :phonemizer "OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer"
             :language "ja"
             :description "Namine Ritsu - Japanese VCV connected voicebank"}
    "teto-en" {:name "重音テト音声ライブラリー"
               :phonemizer "OpenUtau.Plugin.Builtin.ArpasingPhonemizer"
               :language "en"
               :description "Kasane Teto - English voicebank"}})

(def default-singer "teto")

(defn resolve-singer
  "Resolve singer config by ID or return default."
  [singer-id]
  (or (get available-singers (str/lower-case (str singer-id)))
      (get available-singers default-singer)))

(defn slugify
  [value]
  (let [base (-> (str (or value "openutau-project"))
                 str/lower-case
                 (str/replace #"[^a-z0-9]+" "-")
                 (str/replace #"^-+|-+$" ""))]
    (if (str/blank? base) "openutau-project" base)))

(defn default-project-relative-path
  [project-name]
  (let [slug (slugify project-name)]
    (str "orgs/open-hax/openplanner/packages/agents/knoxx/uploads/openutau/"
         slug
         "/"
         slug
         ".ustx")))

(defn- finite-number?
  [value]
  (and (number? value) (js/Number.isFinite value)))

(defn- parse-number
  [value]
  (cond
    (finite-number? value) value
    (string? value) (let [parsed (js/parseFloat value)]
                      (when (js/Number.isFinite parsed) parsed))
    :else nil))

(defn- clamp-int
  [value fallback min-value max-value]
  (let [n (parse-number value)]
    (if (nil? n)
      fallback
      (-> n
          js/Math.round
          (max min-value)
          (min max-value)))))

(defn- clamp-float
  [value fallback min-value max-value]
  (let [n (parse-number value)]
    (if (nil? n)
      fallback
      (-> n
          (max min-value)
          (min max-value)))))

(def ^:private known-problematic-lyrics
  "Lyric strings that hang OpenUTAU's renderer after phonemization.
   Map of problematic -> safe replacement."
  {"kyoukai" "kaikyou"})

(defn sanitize-lyric
  "Sanitize lyric text to avoid OpenUTAU renderer hangs.
   - Removes hyphens and spaces
   - Replaces known problematic romaji patterns
   - Falls back to 'a' for empty strings"
  [lyric]
  (when lyric
    (let [cleaned (-> lyric
                      str/trim
                      (str/replace #"[-\s]+" "")
                      str/lower-case)]
      (or (get known-problematic-lyrics cleaned)
          (not-empty cleaned)
          "a"))))

(defn lyric-text
  [note]
  (let [lyric (some-> (or (:lyric note) (:text note)) str str/trim not-empty)
        phonetic-hint (some-> (or (:phonetic_hint note)
                                  (:phonetic-hint note)
                                  (:phoneticHint note))
                              str str/trim not-empty)]
    (cond
      (and lyric phonetic-hint) (str (sanitize-lyric lyric) " [" phonetic-hint "]")
      phonetic-hint (str "[" phonetic-hint "]")
      lyric (sanitize-lyric lyric)
      :else "a")))

(def ^:const min-renderable-notes
  "OpenUTAU's WORLDLINE-R renderer appears to hang when processing
   voice parts with fewer than ~12 notes. Pad with rests to ensure
   reliable headless rendering."
  12)

(def ^:private hiragana-pad-lyrics
  "Cyclic pool of safe hiragana syllables for note padding.
   JapaneseCVPhonemizer accepts these; romaji vowels like 'a' hang."
  ["あ" "い" "う" "え" "お" "か" "き" "く" "け" "こ" "さ" "し"])

(defn- pad-notes
  "Ensure at least MIN-RENDERABLE-NOTES by appending hiragana pad notes.
   OpenUTAU's WORLDLINE-R renderer hangs with fewer than ~12 singable notes.
   Pads with cyclic hiragana (never rests/romaji) to keep phonemizer happy."
  [notes]
  (let [current-count (count notes)]
    (if (>= current-count min-renderable-notes)
      notes
      (let [last-end (if (seq notes)
                       (+ (:position (last notes)) (:duration (last notes)))
                       0)
            pad-duration default-ticks-per-quarter
            needed (- min-renderable-notes current-count)
            pad-notes (mapv (fn [i]
                              {:position (+ last-end (* i pad-duration))
                               :duration pad-duration
                               :tone 60
                               :lyric (nth hiragana-pad-lyrics
                                           (mod i (count hiragana-pad-lyrics)))})
                            (range needed))]
        (into notes pad-notes)))))

(defn normalize-notes
  [notes]
  (loop [remaining (seq notes)
         cursor 0
         normalized []]
    (if-not remaining
      (pad-notes normalized)
      (let [note (first remaining)
            explicit-position (parse-number (or (:position note) (:start_tick note) (:start-tick note)))
            position (if (nil? explicit-position)
                       cursor
                       (max 0 (js/Math.round explicit-position)))
            duration (clamp-int (or (:duration note) (:duration_ticks note) (:duration-ticks note))
                                default-ticks-per-quarter
                                10
                                (* 64 default-ticks-per-quarter))
            tone (clamp-int (or (:tone note) (:midi note) (:note note) (:pitch note)) 60 0 127)
            normalized-note {:position position
                             :duration duration
                             :tone tone
                             :lyric (lyric-text note)}]
        (recur (next remaining)
               (+ position duration)
               (conj normalized normalized-note))))))

(defn- voice-part-duration
  [normalized-notes]
  (+ (reduce (fn [max-end {:keys [position duration]}]
               (max max-end (+ position duration)))
             0
             normalized-notes)
     default-ticks-per-quarter))

(defn build-project
  [opts normalized-notes]
  (let [project-name (or (some-> (:project_name opts) str str/trim not-empty)
                         (some-> (:project-name opts) str str/trim not-empty)
                         "Knoxx OpenUtau Project")
        bpm (clamp-float (:tempo opts) 120 20 300)
        beat-per-bar (clamp-int (or (:beat_per_bar opts)
                                    (:beat-per-bar opts)
                                    (get-in opts [:time_signature :beat_per_bar])
                                    (get-in opts [:time_signature :beat-per-bar])
                                    (get-in opts [:time-signature :beat_per_bar])
                                    (get-in opts [:time-signature :beat-per-bar]))
                                4
                                1
                                32)
        beat-unit (clamp-int (or (:beat_unit opts)
                                 (:beat-unit opts)
                                 (get-in opts [:time_signature :beat_unit])
                                 (get-in opts [:time_signature :beat-unit])
                                 (get-in opts [:time-signature :beat_unit])
                                 (get-in opts [:time-signature :beat-unit]))
                             4
                             1
                             32)
         singer-config (resolve-singer (or (:singer_id opts) (:singer-id opts)))
         singer-id (:name singer-config)
         phonemizer (or (some-> (:phonemizer opts) str str/trim not-empty)
                        (:phonemizer singer-config))
        track-name (or (some-> (:track_name opts) str str/trim not-empty)
                       (some-> (:track-name opts) str str/trim not-empty)
                       "Vocal")
        part-name (or (some-> (:part_name opts) str str/trim not-empty)
                      (some-> (:part-name opts) str str/trim not-empty)
                      "Main Part")
        comment (or (some-> (:comment opts) str str/trim not-empty)
                    "Generated by Knoxx for OpenUtau. Open in OpenUtau and export audio from the UI.")]
    (array-map
     "name" project-name
     "comment" comment
     "output_dir" "Export"
     "cache_dir" "UCache"
     "ustx_version" default-ustx-version
     "resolution" default-ticks-per-quarter
     "key" (clamp-int (:key opts) 0 0 11)
     "time_signatures" [(array-map
                          "bar_position" 0
                          "beat_per_bar" beat-per-bar
                          "beat_unit" beat-unit)]
     "tempos" [(array-map
                 "position" 0
                 "bpm" bpm)]
     "tracks" [(array-map
                 "singer" singer-id
                 "phonemizer" phonemizer
                 "renderer_settings" (array-map "renderer" default-renderer)
                 "track_name" track-name
                 "track_color" default-track-color
                 "mute" false
                 "solo" false
                 "volume" 0
                 "pan" 0
                 "voice_color_names" [""])]
     "voice_parts" [(array-map
                      "duration" (voice-part-duration normalized-notes)
                      "name" part-name
                      "track_no" 0
                      "position" 0
                      "notes" (mapv (fn [{:keys [position duration tone lyric]}]
                                       (array-map
                                        "position" position
                                        "duration" duration
                                        "tone" tone
                                        "lyric" lyric
                                        "pitch" (array-map
                                                 "data" [(array-map "x" -40 "y" 0 "shape" "io")
                                                         (array-map "x" 40 "y" 0 "shape" "io")]
                                                 "snap_first" true)
                                        "vibrato" (array-map
                                                   "length" 0
                                                   "period" 175
                                                   "depth" 25
                                                   "in" 10
                                                   "out" 10
                                                   "shift" 0
                                                   "drift" 0
                                                   "vol_link" 0)
                                        "phoneme_expressions" []
                                        "phoneme_overrides" []))
                                     normalized-notes)
                      "curves" [])]
     "wave_parts" [])))

(defn- yaml-scalar
  [value]
  (cond
    (nil? value) "null"
    (string? value) (str "'" (str/replace value #"'" "''") "'")
    (keyword? value) (yaml-scalar (name value))
    (boolean? value) (if value "true" "false")
    (number? value) (str value)
    :else (yaml-scalar (str value))))

(defn- indent-str
  [n]
  (apply str (repeat n " ")))

(declare emit-yaml-lines)

(defn- emit-map-lines
  [m indent]
  (mapcat (fn [[k v]]
            (let [key (str k)
                  prefix (str (indent-str indent) key)]
              (cond
                (map? v)
                (if (empty? v)
                  [(str prefix ": {}")]
                  (into [(str prefix ":")]
                        (emit-yaml-lines v (+ indent 2))))

                (vector? v)
                (if (empty? v)
                  [(str prefix ": []")]
                  (into [(str prefix ":")]
                        (emit-yaml-lines v (+ indent 2))))

                :else
                [(str prefix ": " (yaml-scalar v))])))
          m))

(defn- emit-vector-lines
  [xs indent]
  (mapcat (fn [item]
            (let [prefix (str (indent-str indent) "-")]
              (cond
                (map? item)
                (if (empty? item)
                  [(str prefix " {}")]
                  (into [prefix]
                        (emit-yaml-lines item (+ indent 2))))

                (vector? item)
                (if (empty? item)
                  [(str prefix " []")]
                  (into [prefix]
                        (emit-yaml-lines item (+ indent 2))))

                :else
                [(str prefix " " (yaml-scalar item))])))
          xs))

(defn emit-yaml-lines
  [value indent]
  (cond
    (map? value) (emit-map-lines value indent)
    (vector? value) (emit-vector-lines value indent)
    :else [(str (indent-str indent) (yaml-scalar value))]))

(defn project->ustx-yaml
  [project]
  (str (str/join "\n" (emit-yaml-lines project 0)) "\n"))

(defn readme-markdown
  [{:keys [project-name ustx-path readme-path note-count tempo singer-id phonemizer]}]
  (str "# " project-name "\n\n"
       "Generated by Knoxx as an OpenUtau singing-project scaffold.\n\n"
       "## Files\n"
       "- `" ustx-path "` — OpenUtau `.ustx` project\n"
       "- `" readme-path "` — this workflow note\n\n"
       "## Current settings\n"
       "- Notes: " note-count "\n"
       "- Tempo: " tempo " BPM\n"
       "- Renderer: `" default-renderer "`\n"
       "- Singer: " (if (str/blank? (str singer-id)) "_(choose in OpenUtau)_" (str "`" singer-id "`")) "\n"
       "- Phonemizer: " (if (str/blank? (str phonemizer)) "_(choose in OpenUtau if needed)_" (str "`" phonemizer "`")) "\n\n"
       "## Render workflow\n"
       "1. Open `" ustx-path "` in OpenUtau.\n"
       "2. If singer or phonemizer is blank, select them on the vocal track.\n"
       "3. Review lyrics, phonemes, and pitch/timing.\n"
       "4. Export audio with `File > Export Audio > Export wav Files` or `Mixdown To Wav File`.\n\n"
        "## Headless render\n"
        "Use the `voice.openutau_render` tool to render this project to WAV without opening the UI.\n"
        "Requires: Xvfb + OpenUTAU + voicebank + WORLDLINE-R renderer.\n"
        "\n"
        "## Available singers\n"
        "- `teto` — 重音テト OU用日本語統合ライブラリー (Kasane Teto - Japanese)\n"
        "- `ritsu` — 波音リツ連続音Ver1.5.1 (Namine Ritsu - Japanese)\n"
        "- `teto-en` — 重音テト音声ライブラリー (Kasane Teto - English)\n"))

(defn render-ustx-to-wav
  "Render a .ustx file to .wav using the headless OpenUTAU pipeline.
   Returns a promise that resolves to {:wav_path string} or rejects with error."
  [ustx-path output-wav-path]
  (let [child-process (js/require "node:child_process")
        path        (js/require "node:path")
        util        (js/require "node:util")
        exec-file   (util/promisify (.-execFile child-process))
        script      render-script-path]
    (-> (exec-file script #js [ustx-path output-wav-path]
                    #js {:timeout 600000 :maxBuffer 4194304})
        (.then (fn [result]
                 (let [stdout (.-stdout result)]
                   (if (str/includes? stdout "Success!")
                     {:wav_path output-wav-path
                      :stdout   stdout}
                     (throw (js/Error. (str "Render did not report success. stdout: " stdout)))))))
        (.catch (fn [err]
                  (throw (js/Error. (str "OpenUTAU render failed: " (.-message err)
                                         " stderr: " (or (.-stderr err) "N/A")))))))))
