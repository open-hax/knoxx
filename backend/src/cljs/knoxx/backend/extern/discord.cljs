(ns knoxx.backend.extern.discord
  "Discord boundary adapters.

   Owns Discord-specific JS runtime shapes: SDK status objects, multipart
   FormData/Blob bodies, promise array aggregation, runtime tool arrays, and raw
   tool parameter ingress. Domain Discord code should call these functions with
   CLJS maps/vectors and avoid generic JS helpers."
  (:require [clojure.string :as str]))

(defn gateway-started?
  [manager]
  (when (some? manager)
    (let [status (.status manager)]
      (boolean (or (aget status "ready")
                   (aget status "started"))))))

(defn promise-all-vector
  "Resolve a CLJS collection of promises into a CLJS vector of JS/CLJS results."
  [promises]
  (-> (js/Promise.all (clj->js (vec promises)))
      (.then (fn [results]
               (vec (array-seq results))))))

(defn message-form-data
  "Build Discord multipart message FormData from a CLJS payload and files.

   Files are CLJS maps with :name, :mimeType, and :buffer."
  [{:keys [payload files]}]
  (let [form (js/FormData.)]
    (.append form "payload_json" (.stringify js/JSON (clj->js payload)))
    (doseq [[idx file] (map-indexed vector (or files []))]
      (.append form
               (str "files[" idx "]")
               (js/Blob. #js [(:buffer file)] #js {:type (:mimeType file)})
               (:name file)))
    form))

(defn normalize-tool-params
  "Convert raw runtime params to a CLJS keyword map."
  [params]
  (cond
    (nil? params) {}
    (map? params) params
    :else (js->clj params :keywordize-keys true)))

(defn tool-array
  "Convert a CLJS vector of runtime tool objects into the JS array expected by eta-mu."
  [tools]
  (clj->js (vec tools)))

(defn trim-path-delims
  [s]
  (-> (str (or s ""))
      (str/replace #"<\\|\"|\"[|>]" "")
      str/trim))
