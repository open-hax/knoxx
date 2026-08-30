(ns knoxx.backend.infra.translation-content-integrity
  "Authenticate translated bytes against the digest named by their receipt."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.publication-source-revision :as source-revision]))

(defn content-digest
  "The canonical digest used for translated content evidence."
  [content]
  (when (string? content)
    (source-revision/content-revision content)))

(defn authenticated-content?
  "Whether `content` is nonblank and exactly matches `receipt`'s bound bytes."
  [receipt content]
  (and (some? receipt)
       (string? content)
       (not (str/blank? content))
       (some? (:translation/content-digest receipt))
       (= (:translation/content-digest receipt)
          (content-digest content))))
