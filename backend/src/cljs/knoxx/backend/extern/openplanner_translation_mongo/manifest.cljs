(ns knoxx.backend.extern.openplanner-translation-mongo.manifest
  "Tenant-scoped translation export manifests."
  (:require [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [knoxx.backend.law.openplanner-translation :as contract]
            [openplanner.translations.core :as translation]))

(defn- language-stats-pipeline
  "Per-language segment status totals for the matched tenant scope."
  [segment-selector]
  [{:$match segment-selector}
   {:$group {:_id "$target_lang"
             :total {:$sum 1}
             :approved (common/status-count "approved")
             :rejected (common/status-count "rejected")
             :pending (common/status-count "pending")
             :in_review (common/status-count "in_review")}}])

(defn- label-stats-pipeline
  "Correction, labeler, and label-count facets joined back to their segments."
  [label-selector]
  [{:$match label-selector}
   {:$addFields {:segment_object_id {:$convert {:input "$segment_id"
                                                :to "objectId"
                                                :onError nil
                                                :onNull nil}}}}
   {:$lookup {:from common/segment-collection-name
              :localField "segment_object_id"
              :foreignField "_id"
              :as "segment"}}
   {:$unwind "$segment"}
   {:$facet
    {:corrections
     [{:$match {:corrected_text {:$exists true :$nin [nil ""]}}}
      {:$group {:_id {:target_lang "$segment.target_lang"
                      :segment_id "$segment_id"}}}
      {:$group {:_id "$_id.target_lang" :count {:$sum 1}}}]
     :labelers
     [{:$group {:_id "$labeler_email" :segments_labeled {:$sum 1}}}]
     :label_counts
     [{:$group {:_id "$segment.target_lang" :count {:$sum 1}}}]}}])

(defn- facet-rows
  [stats facet]
  (array-seq (or (common/jget stats facet) #js [])))

(defn- count-by-id
  "Index `{_id count}` facet rows by their grouping key."
  [rows]
  (into {}
        (map (fn [row] [(common/jget row "_id") (common/jget row "count")]))
        rows))

(defn- language-summary
  [row]
  {:target-lang (common/jget row "_id")
   :total (common/jget row "total")
   :approved (common/jget row "approved")
   :rejected (common/jget row "rejected")
   :pending (common/jget row "pending")
   :in-review (common/jget row "in_review")})

(defn- labeler-summary
  [row]
  {:email (common/jget row "_id")
   :segments-labeled (common/jget row "segments_labeled")})

(defn- with-average-labels
  "Annotate each shaped language with its mean label count per segment."
  [shaped label-counts]
  (update shaped :languages
          (fn [by-language]
            (into {}
                  (map (fn [[language data]]
                         [language
                          (assoc data :avg_labels_per_segment
                                 (if (pos? (:total_segments data))
                                   (/ (get label-counts language 0)
                                      (:total_segments data))
                                   0))]))
                  by-language))))

(defn- manifest-response
  "Shape the manifest wire response from the language and label aggregations."
  [request language-rows stats]
  (-> (translation/manifest-shape
       {:project (:project request)
        :languages (mapv language-summary language-rows)
        :corrections-by-language (count-by-id (facet-rows stats "corrections"))
        :labelers (mapv labeler-summary (facet-rows stats "labelers"))})
      (with-average-labels (count-by-id (facet-rows stats "label_counts")))
      (assoc :generated_at (.toISOString (js/Date.)))))

(defn ^:async manifest!
  "Build a tenant-scoped translation manifest with indexed label filtering."
  [opts]
  (await (common/ensure-indexes!))
  (let [request (contract/assert-valid! :translation-export-manifest/request
                                        contract/TranslationManifestRequest
                                        (or opts {}))
        org-id (common/required-org-id! (:org_id request))
        segment-selector (assoc (common/filter-map request [:project])
                                :org_id org-id)
        label-selector (cond-> {:org_id org-id}
                         (:project request) (assoc :project (:project request)))
        {:keys [segments labels]} (common/collections (await (common/db!)))
        language-rows (await
                       (.toArray
                        (.aggregate segments
                                    (clj->js (language-stats-pipeline segment-selector)))))
        label-rows (await
                    (.toArray
                     (.aggregate labels
                                 (clj->js (label-stats-pipeline label-selector)))))
        response (manifest-response request
                                    (array-seq language-rows)
                                    (first (array-seq label-rows)))]
    (common/assert-response! :translation-export-manifest/response
                             contract/TranslationManifestResponse
                             response)))