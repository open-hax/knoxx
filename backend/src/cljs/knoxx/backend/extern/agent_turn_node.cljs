(ns knoxx.backend.extern.agent-turn-node
  "Node.js boundary helpers for agent turn orchestration.
   Direct node:crypto and node:fs/promises imports live here so turn
   orchestration can remain CLJS-data oriented."
  (:require ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs]))

(defn random-uuid!
  []
  (.randomUUID crypto))

(defn file-data-url-with-fs!
  [^js node-fs absolute-path mime-type label max-bytes]
  (let [mime-type (or mime-type "application/octet-stream")
        label (or label "media")]
    (-> (.stat node-fs absolute-path)
        (.then
         (fn [^js stat]
           (when-not (.isFile stat)
             (throw (js/Error. (str "Attached " label " is not a file"))))
           (let [size (.-size stat)]
             (when (> size max-bytes)
               (throw (js/Error. (str "Attached " label " exceeds max bytes: " size "; max=" max-bytes))))
             (.readFile node-fs absolute-path))))
        (.then
         (fn [^js buffer]
           (str "data:" mime-type ";base64," (.toString buffer "base64")))))))

(defn file-data-url!
  [absolute-path mime-type label max-bytes]
  (file-data-url-with-fs! fs absolute-path mime-type label max-bytes))
