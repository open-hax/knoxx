(ns knoxx.frontend.law.ws
  "Named contracts for ordinary token deltas and explicit UTF-16 replacements."
  (:require [malli.core :as m]))

(def registry
  "Data contracts at the WebSocket token-decoding boundary."
  {:knoxx.frontend.law.ws/payload [:map
              [:token {:optional true} :any]
              [:run_id {:optional true} :any]
              [:kind {:optional true} :any]
              [:operation {:optional true} :any]
              [:offset {:optional true} :any]]
   :knoxx.frontend.law.ws/decode-input [:map [:payload :knoxx.frontend.law.ws/payload] [:ordinary-token :string]]
   :knoxx.frontend.law.ws/replacement [:and
                  [:map
                   [:token :string]
                   [:kind [:enum "assistant_message" "reasoning"]]
                   [:operation [:= "replace"]]
                   [:offset [:and [:fn number?]
                             [:fn #(and (integer? %) (<= 0 % 9007199254740991))]]]]
                  [:fn #(<= (:offset %) (count (:token %)))]]})

(def ^:private options {:registry (merge (m/default-schemas) registry)})
(def decode-input-valid?
  "Whether the adapter supplied a payload map and JS-coerced ordinary token."
  (m/validator :knoxx.frontend.law.ws/decode-input options))
(def replacement-valid?
  "Whether this replacement preserves a valid token kind and UTF-16 offset."
  (m/validator :knoxx.frontend.law.ws/replacement options))
