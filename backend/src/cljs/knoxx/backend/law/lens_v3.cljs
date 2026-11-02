(ns knoxx.backend.law.lens-v3
  "Malli contracts for KnoxxLens v3 capabilities: WebGL connections graph,
   deterministic manifest verification, GovOne extraction, and browser/media control."
  (:require [malli.core :as m]))

(def connections-graph-params
  [:map
   [:query {:optional true
            :description "Semantic query or topic to center the graph around."}
    :string]
   [:lakes {:optional true
            :description "List of lakes to include (e.g. [\"discord\" \"bluesky\" \"desktop\" \"web\" \"topic\"])."}
    [:vector :string]]
   [:maxNodes {:optional true
               :description "Maximum number of nodes to return in the visual graph (default 150, max 600)."}
    [:int {:min 10 :max 600}]]
   [:seedNodeIds {:optional true
                  :description "Optional explicit seed node IDs to expand outward from."}
    [:vector :string]]
   [:includeDesktop {:optional true
                     :description "Include desktop windows and workspaces in graph."}
    :boolean]])

(def inspect-manifest-params
  [:map
   [:items {:description "List of items that must be inspected in the conversational scope."}
    [:vector
     [:map
      [:type {:description "Item type: link, image, attachment, or reaction."}
       [:enum "link" "image" "attachment" "reaction"]]
      [:id {:description "Unique ID or URL of the target item."} :string]
      [:status {:optional true :description "Outcome status if already inspected."} :string]]]]])

(def govone-extract-params
  [:map
   [:text {:description "Raw Discord message or discussion text to sanitize."} :string]
   [:spans {:optional true
            :description "Optional explicit entity spans to redact."}
    [:vector
     [:map
      [:entityType :string]
      [:value :string]]]]])

(def govone-send-params
  [:map
   [:content {:description "Message text to transmit over the governed channel."} :string]
   [:priority {:optional true
               :description "Message priority: normal or urgent."}
    [:enum "normal" "urgent"]]
   [:sessionId {:optional true
                :description "Optional governed session ID; defaults to active GOVONE_CHANNEL_SESSION_ID."}
    :string]])

(def govone-read-params
  [:map
   [:sessionId {:optional true
                :description "Optional governed session ID; defaults to active GOVONE_CHANNEL_SESSION_ID."}
    :string]])

(def browser-control-params
  [:map
   [:url {:description "Public HTTP/HTTPS URL to navigate to."} :string]
   [:actions {:optional true
              :description "Sequence of interactive actions (click, type, wait, scroll)."}
    [:vector
     [:map
      [:action [:enum "click" "type" "scroll" "wait"]]
      [:selector {:optional true} :string]
      [:text {:optional true} :string]
      [:ms {:optional true} :int]
      [:y {:optional true} :int]]]]
   [:screenshot {:optional true
                 :description "Whether to take a viewport screenshot (default true)."}
    :boolean]
   [:ocr {:optional true
          :description "Whether to run Intel NPU OCR on the captured screenshot (default true)."}
    :boolean]])

(def media-extract-params
  [:map
   [:url {:description "Media URL, attachment URL, or web page containing media."} :string]
   [:extract {:optional true
              :description "Modalities to extract: images, video, audio, attachments."}
    [:vector [:enum "images" "video" "audio" "attachments"]]]
   [:ocr {:optional true
          :description "Run NPU OCR on extracted images (default true)."}
    :boolean]])
