(ns knoxx.backend.extern.multipart
  "Multipart upload boundary helpers for Fastify parts."
  (:require [clojure.string :as str]))

(defn ^:async parts!
  [request]
  (let [parts (await (.fromAsync js/Array (.parts request)))]
    (vec (array-seq parts))))

(defn file-part?
  [part]
  (= "file" (aget part "type")))

(defn file-parts
  [parts]
  (filter file-part? parts))

(defn part-filename
  [part]
  (or (aget part "filename") "upload.bin"))

(defn part-mime-type
  [part]
  (or (aget part "mimetype")
      (aget part "type")
      "application/octet-stream"))

(defn part-size
  [part]
  (or (aget part "size") 0))

(defn ^:async part-buffer!
  [part]
  (let [buf (await (.arrayBuffer (js/Response. (aget part "file"))))]
    (.from js/Buffer buf)))

(defn part-array-buffer!
  [part]
  (.arrayBuffer (js/Response. (aget part "file"))))
