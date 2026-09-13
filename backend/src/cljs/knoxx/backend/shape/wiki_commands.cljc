(ns knoxx.backend.shape.wiki-commands
  "Closed browser/tool command decoding; wire data never carries authority."
  (:require [knoxx.backend.law.source-review :as law]))

(def AssistWire
  "An assistant suggestion is bound to the document the caller actually saw."
  [:map {:closed true}
   [:instruction law/NonBlank] [:expected_revision law/NonBlank]])
