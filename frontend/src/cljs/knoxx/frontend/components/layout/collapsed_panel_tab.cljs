(ns knoxx.frontend.components.layout.collapsed-panel-tab
  "Collapsed edge tab that re-expands a workbench panel.
   Helix port of src/components/CollapsedPanelTab.tsx."
  (:require [helix.core :refer [defnc]]
            [helix.dom :as d]))

(defn- tab-style [edge vertical?]
  (merge {:display "flex"
          :alignItems "center"
          :justifyContent "center"
          :flexShrink 0
          :border "none"
          :background "var(--token-colors-background-surface)"
          :color "var(--token-colors-text-muted)"
          :fontSize "12px"
          :cursor "pointer"}
         (if vertical?
           {:width 28 :padding 0}
           {:height 28 :width "100%" :padding "0 8px"})
         (case edge
           "left" {:borderRight "1px solid var(--token-colors-border-default)"}
           "right" {:borderLeft "1px solid var(--token-colors-border-default)"}
           "bottom" {:borderTop "1px solid var(--token-colors-border-default)"}
           {})))

(defnc collapsed-panel-tab
  "Thin clickable strip shown where a collapsed panel used to be.
   `edge` is \"left\", \"right\" or \"bottom\"; left/right render the label
   rotated (vertical-rl)."
  [{:keys [label edge on-expand title]}]
  (let [vertical? (contains? #{"left" "right"} edge)
        tip (or title (str "Show " label " panel"))]
    (d/button {:on-click on-expand
               :title tip
               :aria-label tip
               :style (clj->js (tab-style edge vertical?))}
              (d/span {:style (when vertical?
                                #js {:writingMode "vertical-rl"
                                     :textOrientation "mixed"})}
                      label))))
