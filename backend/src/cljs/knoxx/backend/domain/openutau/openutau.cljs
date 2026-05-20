(ns knoxx.backend.domain.openutau
  (:require [clojure.string :as str]))

(def default-ustx-version "0.6")
(def default-ticks-per-quarter 480)
(def default-renderer "WORLDLINE-R")
(def default-track-color "Blue")

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

(defn lyric-text
  [note]
  (let [lyric (some-> (or (:lyric note) (:text note)) str str/trim not-empty)
        phonetic-hint (some-> (or (:phonetic_hint note)
                                  (:phonetic-hint note)
                                  (:phoneticHint note))
                              str str/trim not-empty)]
    (cond
      (and lyric phonetic-hint) (str lyric " [" phonetic-hint "]")
      phonetic-hint (str "[" phonetic-hint "]")
      lyric lyric
      :else "a")))

(defn normalize-notes
  [notes]
  (loop [remaining (seq notes)
         cursor 0
         normalized []]
    (if-not remaining
      normalized
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
        singer-id (or (some-> (:singer_id opts) str str/trim not-empty)
                      (some-> (:singer-id opts) str str/trim not-empty)
                      "")
        phonemizer (or (some-> (:phonemizer opts) str str/trim not-empty) "")
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
       "## Research note\n"
       "OpenUtau currently documents UI-based export and does not expose a supported headless `.ustx -> .wav` workflow for backend automation, so Knoxx generates a ready-to-edit project instead of pretending it already rendered audio.\n"))