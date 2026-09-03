(ns knoxx.frontend.pages.translations.review-form-sync
  "React synchronization for the selected split's immutable review state."
  (:require [helix.hooks :as hooks]
            [knoxx.frontend.pages.translations.logic :as logic]))

(defn use-review-form-sync!
  "Rehydrate when selection, candidate authority, or latest review changes."
  [selected-segment set-form!]
  (hooks/use-effect
   [(:id selected-segment)
    (:candidate_set_id selected-segment)
    (logic/segment-review-identity selected-segment)]
   (set-form! (logic/segment-review-form selected-segment))
   nil))
