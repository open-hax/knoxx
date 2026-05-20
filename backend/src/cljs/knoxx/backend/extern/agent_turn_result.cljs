(ns knoxx.backend.extern.agent-turn-result
  "JS boundary helpers for eta-mu agent turn result objects.
   Raw assistant-message JS shape access lives here; callers receive CLJS maps.")

(defn usage-tokens
  [assistant-message]
  (let [usage (when assistant-message
                (or (aget assistant-message "usage") #js {}))]
    {:input-tokens (or (some-> usage (aget "input")) 0)
     :output-tokens (or (some-> usage (aget "output")) 0)}))
