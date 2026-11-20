(ns knoxx.backend.infra.media.inspection
  "Inspect actual media through an explicitly selected Proxx model.
   SPDX-License-Identifier: GPL-3.0-or-later"
  (:require [clojure.string :as str]
            [malli.core :as m]
            [knoxx.backend.domain.media :as media]
            [knoxx.backend.domain.media.inspection :as inspection]
            [knoxx.backend.domain.models :as models]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.tools :refer [create-tool-obj]]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.extern.media-video :as video]
            [knoxx.backend.extern.schedule-timers :as clock]
            [knoxx.backend.law.media-video :as video-law]
            [knoxx.backend.infra.agent.policy :as policy]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.infra.clients.proxx :as proxx]))

(defn- ^:async prepare-media!
  [part source parameters]
  (if (= "video" (:type part))
    (let [prepared (await (video/prepare!
                          (assoc part :source_url (when (media/source-http-url? source) source))
                          (select-keys parameters [:start_ms :duration_ms])))]
      (when-not (m/validate video-law/Prepared prepared)
        (throw (ex-info "Invalid video derivative evidence" {:code "video_preparation_invalid"})))
      (let [parts (if (= "transcribe" (:operation parameters))
                    (filterv #(= "audio" (:type %)) (:parts prepared)) (:parts prepared))]
        (when (empty? parts)
          (throw (ex-info "Requested video window has no usable media for this operation"
                          {:code "video_media_unavailable" :coverage (:coverage prepared) :failed (:failed prepared)})))
        {:parts parts :coverage (:coverage prepared) :failed (:failed prepared)}))
    {:parts [part]}))

(defn- assert-inputs!
  [config model parts]
  (doseq [part parts]
    (when-not (models/model-supports-input? config model (:type part))
      (throw (ex-info "Selected model contract does not support this media type"
                      {:model model :type (:type part)})))))

(defn- ^:async inspect-one!
  [config source model question operation part context timeout-ms]
  (let [request (inspection/request-for-parts model question [part] operation context)
        response (await (proxx/chat-completions!
                         (proxx/client (assoc config :proxx-chat-timeout-ms timeout-ms)) request))]
    (inspection/inspection-observation source model response)))

(defn- ^:async observe-sample!
  [config source model question operation prepared part deadline]
  (let [remaining (- deadline (clock/now-ms))
        position (get-in part [:derived_from :requested_ms])
        identity {:modality (:type part) :requested_ms position}]
    (if (< remaining 1000)
      {:failed (assoc identity :code "video_perception_budget_exhausted" :status nil)}
      (try
        (let [question (if (= "audio" (:type part)) inspection/audio-description-task
                           (str "Inspect only this representative image frame; no audio or other frames are supplied in this request. Answer only visible details. " question))
              context (pr-str {:coverage (select-keys (:coverage prepared)
                                                       (if (= "audio" (:type part))
                                                         [:source_duration_ms :start_ms :end_ms :has_audio :source_span_omitted]
                                                         [:source_duration_ms :start_ms :end_ms :source_span_omitted :visual_sampling]))
                               :sample (select-keys (:derived_from part) [:kind :requested_ms :decoded_audio_duration_ms])})
              observed (await (inspect-one! config source model question operation part context
                                            (min remaining video-law/sample-perception-timeout-ms)))]
          {:observed (merge identity observed)})
        (catch :default error
          {:failed (assoc identity :code "video_perception_failed" :status (:status (ex-data error)))})))))

(defn- sample-label [sample]
  (str (:modality sample) " sample requested at " (:requested_ms sample) "ms: " (:observation sample)))

(defn- ^:async observe-video!
  [config source model question operation prepared]
  (let [deadline (+ (clock/now-ms) video-law/perception-budget-ms)
        results (loop [parts (:parts prepared) results []]
                  (if-let [part (first parts)]
                    (recur (next parts)
                           (conj results (await (observe-sample! config source model question operation prepared part deadline))))
                    results))
        observations (vec (keep :observed results))
        failed (vec (keep :failed results))]
    (when (empty? observations)
      (throw (ex-info (str "No video sample produced a perception observation: " (pr-str failed))
                      {:code "video_perception_unavailable"})))
    {:source source :model model :observations observations :video_model_failed failed
     :observation (if (= operation "transcribe") (:observation (first observations))
                      (str "Separate model observations; samples are not the whole video.\n"
                           (str/join "\n" (map sample-label observations))
                           (when (seq failed)
                             (str "\nUnavailable observations: "
                                  (str/join "; " (map #(str "Prepared " (:modality %) " input at " (:requested_ms %)
                                                            "ms exists, but its model observation is unavailable (" (:code %) ").") failed))
                                  " This is not evidence that source media or audio is absent."))))
     :provenance "Attributed per-sample model observations of actual derivative bytes; inferred details may be wrong."}))

(defn make-inspect-execute
  "Capture authorization per tool instance, independently of global turn context."
  [auth-context]
  (^:async fn [runtime config _tool-call-id params _a _b _c]
    (let [{:keys [source model question operation] :as parameters} (xtools/parameter-map params)]
      (when-not (m/validate inspection/Parameters parameters)
        (throw (ex-info "Invalid media inspection parameters" {:errors (m/explain inspection/Parameters parameters)})))
      (when-not (models/allowlisted-model-id? config model)
        (throw (ex-info "Perception model is not allowlisted" {:model model})))
      (await (policy/enforce-chat-policy! auth-context model))
      (let [{:keys [part]} (await (media/media-source->content-part! runtime config source media/multimodal-upload-max-bytes))
            prepared (await (prepare-media! part source parameters))
            _ (assert-inputs! config model (:parts prepared))
            result (if (:coverage prepared)
                     (await (observe-video! config source model question (or operation "describe") prepared))
                     (await (inspect-one! config source model question (or operation "describe")
                                          (first (:parts prepared)) nil 120000)))
            context (when (:coverage prepared)
                      (pr-str {:coverage (:coverage prepared) :failed (:failed prepared)
                               :model_failed (:video_model_failed result)}))]
          (tool-text-result (str "Media inspection\nSource: " source "\nModel: " model
                                 "\nObservation: " (:observation result)
                                 "\n" (:provenance result)
                                 (when context (str "\nVideo observation limits: " context)))
                            (cond-> result
                              (:coverage prepared) (assoc :video_coverage (:coverage prepared)
                                                          :video_failed (:failed prepared))))))))

(defn create-inspection-tools
  "Expose attributed perception text and compact derivation evidence."
  [runtime config auth-context]
  (if (or (nil? auth-context) (ctx-tool-allowed? auth-context "multimodal.inspect"))
    [(create-tool-obj "multimodal.inspect" "Inspect Media"
                      "Inspect image, audio or a bounded video window using an explicit Proxx model. Video supplies representative frames and real audio; use gemma4:e4b, start_ms and duration_ms (max12000). Transcribe uses audio only."
                      "Use a perception model for media your own model cannot receive."
                      ["Use gemma4:e4b for video/audio. Video returns clip/sampling/failure limits and retains source lineage for memory. Request later windows explicitly; never imply all frames or the whole source were observed. Treat model observations as fallible evidence."]
                      inspection/Parameters (make-inspect-execute auth-context) runtime config)]
    []))
