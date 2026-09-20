(ns knoxx.backend.shape.bluesky
  "Pure Bluesky URI and presentation projections."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.text :as text]))

(defn parse-at-uri
  "Project parse-at-uri." [uri]
  (let [parts (some-> uri str (str/split #"/"))
        repo (nth parts 2 nil)
        collection (nth parts 3 nil)
        rkey (nth parts 4 nil)]
    (when (and repo collection rkey)
      {:repo repo :collection collection :rkey rkey})))

(defn bluesky-post-url
  "Project bluesky-post-url." [handle uri]
  (let [post-id (some-> uri str (str/split #"/") last)]
    (when (and (not (str/blank? (str handle)))
               (not (str/blank? (str post-id))))
      (str "https://bsky.app/profile/" handle "/post/" post-id))))

(defn format-posts
  "Project format-posts." [prefix rows]
  (let [lines (->> rows
                   (map (fn [{:keys [displayName handle text url]}]
                          (str "- " (or (not-empty displayName) handle "unknown")
                               (when (not (str/blank? (str handle))) (str " (@" handle ")"))
                               ": " (text/clip-text (or text "") 220)
                               (when (not (str/blank? (str url))) (str "\n  " url)))))
                   (str/join "\n"))]
    (str prefix (when-not (str/blank? lines) (str "\n" lines)))))

(defn- format-thread-reply [reply depth]
  (let [post (:post reply)
        author (when post (:author post))
        record (when post (:record post))
        handle (or (when author (:handle author)) "")
        display-name (or (when author (:displayName author)) "")
        text (or (when record (:text record)) "")
        uri (or (when post (:uri post)) "")
        indent (str/join "" (repeat depth "  "))]
    (str indent "- " (or (not-empty display-name) handle "unknown")
         (when-not (str/blank? handle) (str " (@" handle ")"))
         ": " (text/clip-text text 180)
         (when-not (str/blank? uri) (str "\n" indent "  " uri)))))

(defn collect-thread-replies
  "Project collect-thread-replies." [thread-node depth max-depth acc]
  (if (> depth max-depth)
    acc
    (reduce (fn [a reply]
              (let [new-acc (conj a (format-thread-reply reply depth))]
                (collect-thread-replies reply (inc depth) max-depth new-acc)))
            acc
            (or (:replies thread-node) []))))

