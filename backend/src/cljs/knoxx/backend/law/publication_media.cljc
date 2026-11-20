(ns knoxx.backend.law.publication-media
  "Contracts for media references contained in approved publication text.

   Asset references stay inside the website's staged public asset roots.
   This validates a reference, not asset existence, ownership or byte custody."
  (:require [clojure.string :as str]))

(defn require-text-blocks!
  "Require the semantic string blocks accepted by the publication renderer."
  [blocks]
  (when-not (or (nil? blocks)
                (and (sequential? blocks) (every? string? blocks)))
    (throw (ex-info "Publication rendering requires semantic text blocks" {})))
  blocks)

(defn- staged-path? [root extensions value]
  (when (and (string? value) (<= (count value) 2048)
             (str/starts-with? value (str "/" root "/")))
    (let [segments (str/split (subs value (+ 2 (count root))) #"/" -1)]
      (and (every? #(and (not (str/starts-with? % "."))
                        ;; Space and non-ASCII percent bytes may name an asset;
                        ;; encoded ASCII delimiters, traversal and controls may not.
                        (re-matches #"(?:[A-Za-z0-9_~.!$&'()+,;=@-]|%(?:20|[89A-Fa-f][0-9A-Fa-f]))+" %))
                   segments)
           (contains? extensions (last (str/split (str/lower-case value) #"\.")))))))

(defn staged-image-path?
  "Admit only a safe same-origin reference beneath the staged graphics root."
  [value]
  (boolean (staged-path? "graphics" #{"svg" "png" "jpg" "jpeg" "gif" "webp" "avif"} value)))

(defn staged-audio-path?
  "Admit only a safe same-origin reference beneath the staged music root."
  [value]
  (boolean (staged-path? "music" #{"mp3" "wav" "ogg" "m4a" "flac" "aac" "opus"} value)))

(defn suno-song-url?
  "Admit a canonical Suno clip destination without queries, embeds or userinfo.

   This does not establish public visibility; the publisher supplies that evidence."
  [value]
  (boolean (and (string? value)
                (re-matches #"https://suno\.com/song/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" value))))
