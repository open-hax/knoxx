(ns knoxx.backend.extern.bluesky-facets
  "UTF-8 byte offsets for native ATProto rich-text facets.")

(defn- text->utf8-bytes [text]
  (js/Uint8Array.from (js/Array.from (.encode (js/TextEncoder.) text))))

(defn- build-hashtag-facets [text]
  (let [hashtag-re (js/RegExp. "#(\\w+)" "g")
        matches (loop [m (.exec hashtag-re text)
                       acc []]
                  (if (nil? m)
                    acc
                    (let [full-match (aget m 0)
                          tag (aget m 1)
                          start-char (.-index m)
                          prefix (.substring text 0 start-char)
                          start-byte (.-length (text->utf8-bytes prefix))
                          end-byte (+ start-byte (.-length (text->utf8-bytes full-match)))]
                      (recur (.exec hashtag-re text)
                             (conj acc {"$type" "app.bsky.richtext.facet"
                                        :index {:byteStart start-byte :byteEnd end-byte}
                                        :features [{"$type" "app.bsky.richtext.facet#tag"
                                                    :tag tag}]})))))]
    (when (seq matches) matches)))

(defn- build-url-facets [text]
  (let [url-re (js/RegExp. "(https?://[^\\s]+)" "g")
        matches (loop [m (.exec url-re text)
                       acc []]
                  (if (nil? m)
                    acc
                    (let [full-match (aget m 0)
                          start-char (.-index m)
                          prefix (.substring text 0 start-char)
                          start-byte (.-length (text->utf8-bytes prefix))
                          end-byte (+ start-byte (.-length (text->utf8-bytes full-match)))]
                      (recur (.exec url-re text)
                             (conj acc {"$type" "app.bsky.richtext.facet"
                                        :index {:byteStart start-byte :byteEnd end-byte}
                                        :features [{"$type" "app.bsky.richtext.facet#link"
                                                    :uri full-match}]})))))]
    (when (seq matches) matches)))

(defn build-facets
  "Project build-facets." [text]
  (let [hashtags (build-hashtag-facets text)
        urls (build-url-facets text)]
    (when (or hashtags urls)
      (vec (concat hashtags urls)))))

