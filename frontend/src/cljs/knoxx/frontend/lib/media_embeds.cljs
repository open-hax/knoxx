(ns knoxx.frontend.lib.media-embeds
  "Markdown media-embed extraction, ported from src/lib/mediaEmbeds.ts.
   Pure — no React, no DOM (only js/encodeURIComponent). Canonical impl; the TS
   copy retires when its consumer (ChatMessageList) migrates.

   extract-embeds-from-markdown returns {:markdown <rewritten> :content-parts
   [{:type :url :filename :mimeType} ...]}. Content-part maps use the codebase's
   camelCase content-part keys (:mimeType)."
  (:require [clojure.string :as str]))

(def ^:private ext->kind
  {".png"  {:kind "image" :mimeType "image/png"}
   ".apng" {:kind "image" :mimeType "image/png"}
   ".jpg"  {:kind "image" :mimeType "image/jpeg"}
   ".jpeg" {:kind "image" :mimeType "image/jpeg"}
   ".gif"  {:kind "image" :mimeType "image/gif"}
   ".webp" {:kind "image" :mimeType "image/webp"}
   ".svg"  {:kind "image" :mimeType "image/svg+xml"}
   ".mp3"  {:kind "audio" :mimeType "audio/mpeg"}
   ".wav"  {:kind "audio" :mimeType "audio/wav"}
   ".ogg"  {:kind "audio" :mimeType "audio/ogg"}
   ".m4a"  {:kind "audio" :mimeType "audio/mp4"}
   ".flac" {:kind "audio" :mimeType "audio/flac"}
   ".aac"  {:kind "audio" :mimeType "audio/aac"}
   ".mp4"  {:kind "video" :mimeType "video/mp4"}
   ".webm" {:kind "video" :mimeType "video/webm"}
   ".mov"  {:kind "video" :mimeType "video/quicktime"}
   ".avi"  {:kind "video" :mimeType "video/x-msvideo"}
   ".pdf"  {:kind "document" :mimeType "application/pdf"}})

(defn- strip-query-and-fragment
  [href]
  (or (first (str/split (str href) #"[?#]")) href))

(defn- extname
  [href]
  (let [raw (str/trim (strip-query-and-fragment href))]
    (when-let [last-dot (str/last-index-of raw ".")]
      (str/lower-case (subs raw last-dot)))))

(defn- basename
  [href]
  (let [raw (str/replace (strip-query-and-fragment href) #"\\" "/")
        parts (filter seq (str/split raw #"/"))]
    (or (last parts) raw)))

(defn- http-url?
  [href]
  (boolean (re-find #"(?i)^https?://" (str href))))

(defn- parse-markdown-link-target
  "Best-effort: (url \"title\") => url; <url> => url."
  [raw-target]
  (let [trimmed (str/trim (str raw-target))]
    (if (= "" trimmed)
      trimmed
      (let [unwrapped (if (and (str/starts-with? trimmed "<") (str/ends-with? trimmed ">"))
                        (subs trimmed 1 (dec (count trimmed)))
                        trimmed)]
        (or (first (str/split unwrapped #"\s+")) unwrapped)))))

(defn- workspace-raw-url
  [path]
  (let [normalized (str/trim (str/replace (str path) #"^@" ""))]
    (str "/api/workspace-media/raw?path=" (js/encodeURIComponent normalized))))

(defn- href->content-part
  "Map a media href to a content part, or nil. Remote http(s) media is never
   auto-embedded; same-origin/workspace paths are."
  [href label]
  (when-let [ext (extname href)]
    (when-let [mapping (ext->kind ext)]
      (let [url (cond
                  (http-url? href) nil
                  (str/starts-with? href "/") href
                  :else (workspace-raw-url href))]
        (when url
          (let [trimmed-label (str/trim (or label ""))]
            {:type (:kind mapping)
             :url url
             :filename (if (= "" trimmed-label) (basename href) trimmed-label)
             :mimeType (:mimeType mapping)}))))))

(defn- mask-fenced-code-blocks
  [markdown]
  (let [blocks (atom [])
        masked (str/replace markdown #"```[\s\S]*?```"
                            (fn [match]
                              (let [id (count @blocks)]
                                (swap! blocks conj match)
                                (str "@@CODEBLOCK_" id "@@"))))]
    {:masked masked :blocks @blocks}))

(defn- unmask-fenced-code-blocks
  [masked blocks]
  (reduce (fn [out i]
            (str/replace out (str "@@CODEBLOCK_" i "@@") (nth blocks i "")))
          masked
          (range (count blocks))))

(defn extract-embeds-from-markdown
  "Replace embeddable media links with a short hint and return the derived
   content parts. Images run before links; fenced code blocks are untouched."
  [markdown]
  (if (str/blank? (str markdown))
    {:markdown markdown :content-parts []}
    (let [{:keys [masked blocks]} (mask-fenced-code-blocks markdown)
          parts (atom [])
          seen (atom #{})
          handle (fn [match label raw-target]
                   (let [href (parse-markdown-link-target raw-target)
                         part (href->content-part href label)]
                     (if (or (nil? part) (nil? (:url part)))
                       match
                       (do
                         (let [k (str (:type part) ":" (:url part))]
                           (when-not (contains? @seen k)
                             (swap! seen conj k)
                             (swap! parts conj part)))
                         (let [l (str/trim (or label ""))]
                           (str (if (= "" l) (or (:filename part) "media") l)
                                " (embedded below)"))))))
          after-images (str/replace masked #"!\[([^\]]*)\]\(([^)]+)\)"
                                    (fn [[match alt raw-target]] (handle match alt raw-target)))
          after-links (str/replace after-images #"\[([^\]]+)\]\(([^)]+)\)"
                                   (fn [[match label raw-target]] (handle match label raw-target)))]
      {:markdown (unmask-fenced-code-blocks after-links blocks)
       :content-parts @parts})))
