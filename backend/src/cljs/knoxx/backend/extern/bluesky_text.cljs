(ns knoxx.backend.extern.bluesky-text
  "Unicode text measurement at the Bluesky publication boundary."
  (:require [clojure.string :as str]))

(defn validate-record-text!
  "Apply the API Unicode limits to actual record text, including image-only posts."
  [text]
  (when-not (string? text)
    (throw (ex-info "[bluesky_text_invalid] No platform call was made. Supply a string for record text."
                    {:code "bluesky_text_invalid"})))
  (let [segments (.segment (js/Intl.Segmenter. "en" #js {:granularity "grapheme"}) text)
        graphemes (.-length (js/Array.from segments))
        utf8-bytes (.-length (.encode (js/TextEncoder.) text))]
    (when (or (> graphemes 300) (> utf8-bytes 3000))
      (throw (ex-info (str "[bluesky_text_invalid] No platform call was made. This text has "
                          graphemes " graphemes and " utf8-bytes
                          " UTF-8 bytes; Bluesky allows 300 graphemes and 3000 UTF-8 bytes."
                          " Shorten this part or move a distinct point into a later self-reply."
                          " This local refusal has not consumed a publication attempt.")
                      {:code "bluesky_text_invalid" :graphemes graphemes :utf8-bytes utf8-bytes})))
    {:graphemes graphemes :utf8-bytes utf8-bytes}))

(defn validate-post-text!
  "Convenience publication requires nonblank prose as well as the API limits."
  [text]
  (when-not (and (string? text) (not (str/blank? text)))
    (throw (ex-info "[bluesky_text_invalid] Supply nonblank post text. No platform call was made."
                    {:code "bluesky_text_invalid"})))
  (validate-record-text! text))
