(ns knoxx.frontend.pages.source-doc.forum-thread
  "Pure forum-thread parsing/preparation logic, ported from the TypeScript
   ForumThreadView.tsx. No React, no DOM, no uxx — so it is unit-testable under
   the :node-test build. The Helix view will consume these; the TS copies retire
   when SourceDocPage migrates.

   Parsed threads are CLJS maps with keywordized (camelCase) keys matching the
   ForumThread/ForumPost shapes (:postId, :contentFull, :threadTitle, …)."
  (:require [clojure.string :as str]))

(defn parse-forum-thread
  "Parse a forum-thread JSON document. Returns the thread map when `path` is a
   .json file whose content parses to an object with a :posts array; nil
   otherwise (non-json path, invalid JSON, or missing posts)."
  [path content]
  (when (re-find #"(?i)\.json$" (str path))
    (try
      (let [parsed (js->clj (js/JSON.parse content) :keywordize-keys true)]
        (when (vector? (:posts parsed))
          parsed))
      (catch :default _ nil))))

(defn extract-inline-image-urls
  "Pull image URLs out of post body text: [img]…[/img] BBCode, ![](…) markdown,
   and bare image links. Trimmed, de-duplicated, order-preserving."
  [text]
  (if (str/blank? (str text))
    []
    (let [bb (map second (re-seq #"(?i)\[img\](https?://[^\[]+)\[/img\]" text))
          md (map second (re-seq #"(?i)!\[[^\]]*\]\((https?://[^\s)]+)\)" text))
          plain (re-seq #"(?i)https?://[^\s\"'<>]+\.(?:jpg|jpeg|png|gif|webp|avif)(?:\?[^\s\"'<>]*)?" text)]
      (->> (concat bb md plain)
           (map str/trim)
           (remove str/blank?)
           distinct
           vec))))

(defn format-post-date
  "Human label for a post's date: prefer :rawDate, else format :date, else
   'Unknown date' for missing/invalid timestamps."
  [post]
  (cond
    (:rawDate post) (:rawDate post)
    (not (:date post)) "Unknown date"
    :else (let [d (js/Date. (:date post))]
            (if (js/Number.isNaN (.getTime d))
              "Unknown date"
              (.toLocaleString d)))))

(defn build-prepared-posts
  "Derive the per-post render model: body text, display label/key, and the
   merged (declared + inline) image URL list."
  [thread]
  (->> (or (:posts thread) [])
       (map-indexed
        (fn [index post]
          (let [body (or (:contentFull post) (:content post) "")
                declared (when (vector? (:images post)) (:images post))
                image-urls (->> (concat declared (extract-inline-image-urls body))
                                distinct
                                vec)]
            {:post post
             :index index
             :body body
             :post-label (or (:postId post) (str "#" (inc index)))
             :post-key (str (or (:postId post) index))
             :image-urls image-urls})))
       vec))
