(ns knoxx.backend.law.media-video
  "Bounded video observation plans; requested sample positions are not measured frame PTS.
   SPDX-License-Identifier: GPL-3.0-or-later")

(def max-source-bytes (* 25 1024 1024))
(def max-window-ms 12000)
(def sample-step-ms 4000)
(def perception-budget-ms 240000)
(def sample-perception-timeout-ms 120000)
(def transform-version "knoxx-video-v1-jpeg512-wav16k-window12s")

(def Window
  [:map {:closed true}
   [:start_ms {:optional true} [:int {:min 0 :max 86400000}]]
   [:duration_ms {:optional true} [:int {:min 1 :max 12000}]]])

(def Probe
  [:map {:closed true} [:duration_ms [:int {:min 1 :max 86400000}]]
   [:has_audio :boolean]])

(def Coverage
  [:map {:closed true}
   [:source_duration_ms [:int {:min 1 :max 86400000}]]
   [:start_ms [:int {:min 0}]] [:end_ms [:int {:min 1}]] [:has_audio :boolean]
   [:source_span_omitted :boolean]
   [:visual_sampling [:= "representative frames only; intervening frames not observed"]]
   [:requested_frame_ms [:vector {:min 1 :max 3} [:int {:min 0}]]]
   [:timestamp_basis [:string {:max 160}]]])

(def Prepared
  "A bounded derivative bundle; root bytes remain in the original inspection receipt."
  [:map {:closed true}
   [:parts [:vector {:max 4}
            [:map [:type [:enum "image" "audio"]] [:mimeType :string]
             [:data [:string {:min 1 :max 800000}]] [:derived_from :map]]]]
   [:coverage Coverage] [:source :map]
   [:failed [:vector {:max 4}
             [:map {:closed true} [:kind [:enum "video-frame" "video-audio"]]
              [:requested_ms [:int {:min 0}]] [:code [:string {:max 100}]]]]]])

(defn window-plan
  "Select a bounded clip and representative positions; disclose omitted source span."
  [{:keys [duration_ms has_audio]} window]
  (let [start (get window :start_ms 0)
        end (min duration_ms (+ start (get window :duration_ms max-window-ms)))]
    (when-not (< start end)
      (throw (ex-info "Requested video window is outside the source"
                      {:code "video_window_out_of_range"})))
    {:source_duration_ms duration_ms :start_ms start :end_ms end
     :has_audio has_audio :source_span_omitted (or (pos? start) (< end duration_ms))
     :visual_sampling "representative frames only; intervening frames not observed"
     :requested_frame_ms (vec (range start end sample-step-ms))
     :timestamp_basis "requested sampling positions; original decoded frame PTS not measured"}))

(defn lineage
  "Retain root bytes, requested positions and incomplete coverage with each derivative."
  [source plan kind position]
  (merge source
         (select-keys plan [:source_duration_ms :start_ms :end_ms :source_span_omitted :timestamp_basis :visual_sampling])
         {:transform_version transform-version :kind kind :requested_ms position}))
