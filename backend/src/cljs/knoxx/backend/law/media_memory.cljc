(ns knoxx.backend.law.media-memory
  "Binary media projection and query contracts, independent of storage effects."
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [knoxx.backend.law.knowledge :as knowledge]
            [malli.core :as m]))

(def transform-version "knoxx-media-v1-jpeg512-pcm16k-4s")
(def max-source-bytes (* 25 1024 1024))
(def max-audio-bytes (* 20 1024 1024))
(def audio-window-bytes (* 16000 2 4))

(def Part
  "Only media actually supplied to the model or returned by a successful tool."
  [:map [:type [:enum "image" "audio"]]
   [:data {:optional true} :string] [:url {:optional true} :string]
   [:mimeType {:optional true} :string]])

(def Projection
  "One byte-derived vector beside an immutable, actor-scoped original event."
  [:map [:id [:string {:min 1}]]
   [:event [:map [:extra [:map [:memory_access knowledge/Scope]]]]]
   [:modality [:enum "image" "audio"]]
   [:embedding [:vector {:min 1} [:double {:min -1 :max 1}]]]
   [:embedding-model [:string {:min 1}]]])

(defn assert-valid!
  "Validate a named boundary before any persistence effect."
  [schema value]
  (when-not (m/validate schema value)
    (throw (ex-info "Invalid media-memory boundary" {:status 422 :code "media_memory_invalid"})))
  value)

(defn- part?
  [part]
  (and (m/validate Part part)
       (or (not (str/blank? (:data part))) (not (str/blank? (:url part))))))

(defn run-parts
  "Preserve encounter order and remove duplicate raw content within one run.
   Failed tool results and text descriptions never become binary evidence."
  [run]
  (let [request (last (:request_messages run))
        request-parts (map #(dissoc % :derived_from) (or (:content-parts request) (:content_parts request) []))
        tool-parts (mapcat (fn [receipt]
                            (when (and (= "completed" (:status receipt))
                                       (not= "memory_session" (:tool_name receipt))
                                       (not (:is_error receipt))
                                       (not (get-in receipt [:result :isError])))
                              (let [source (or (get-in receipt [:result :details :source])
                                               (get-in receipt [:input :source]))]
                                (map #(cond-> (if (= "multimodal_inspect" (:tool_name receipt)) % (dissoc % :derived_from))
                                        (and (string? source) (re-find #"^https?://" source))
                                        (assoc :source_url source))
                                     (or (seq (get-in receipt [:result :details :media_memory_parts]))
                                         (seq (get-in receipt [:result :details :content_parts]))
                                         (get-in receipt [:result :content]) [])))))
                          (:tool_receipts run))]
    (->> (concat request-parts tool-parts (map #(dissoc % :derived_from) (:content_parts run)))
         (map #(cond-> % (keyword? (:type %)) (update :type name)))
         (filter part?)
         (reduce (fn [{:keys [seen] :as state} part]
                   (let [identity [(:type part) (:data part) (when-not (:data part) (:url part)) (:derived_from part)]]
                     (if (contains? seen identity) state
                         (-> state (update :seen conj identity) (update :parts conj part)))))
                 {:seen #{} :parts []})
         :parts)))

(defn partition-model
  "Keep binary audio out of the image/text vector space."
  [model modality]
  (if (= "audio" modality) (str model "-audio") model))

(defn query-model
  "Normal recall excludes the dedicated audio lane unless explicitly selected."
  [base-model partition payload]
  (if (= partition (partition-model base-model "audio"))
    (when (and (= "knoxx.media" (:kind payload))
               (= partition (get-in payload [:where :embedding_model]))) base-model)
    partition))

(defn search-filter
  "The caller selects a modality, never an arbitrary provider/model namespace."
  [model modality]
  (when modality
    (assert-valid! [:enum "image" "audio"] modality)
    {:kind "knoxx.media" :where {:embedding_model (partition-model model modality)}}))

(defn session-summary-rows
  "Ordinary transcript recall carries media references, never encoded byte blobs."
  [rows]
  (walk/postwalk
   (fn [value]
     (if (map? value)
       (cond-> (dissoc value :media_content :media_memory_parts)
         (or (:media_content value) (:media_memory_parts value)) (assoc :media_available true)
         (and (contains? #{"image" "audio" "video"} (:type value)) (:data value))
         (-> (dissoc :data) (assoc :media_available true)))
       value)) (vec rows)))

(defn recalled-media
  "Select one media item only after the caller authorizes its entire session."
  [rows id]
  (let [row (some #(when (= id (:id %)) %) rows)
        part (get-in row [:extra :media_content])]
    (when-not (and (= "knoxx.media" (:kind row)) (m/validate Part part))
      (throw (ex-info "Media entry is unavailable in this session" {:status 404 :code "media_memory_not_found"})))
    {:id id :graph_node_id (get-in row [:extra :graph_node_id])
     :provenance (get-in row [:extra :media_provenance]) :content_parts [part]}))
