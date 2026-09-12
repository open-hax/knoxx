(ns knoxx.frontend.pages.contracts.library
  "Searchable contract library with class folders and the existing narrow overlay."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.lib.contracts-view :as boundary]
            [knoxx.frontend.pages.contracts.style :as style]))

(hx/defnc contract-entry
  "Select the exact contract class and identifier, preserving runtime badges."
  [{:keys [state actions entry]}]
  (let [selected? (and (= (:id entry) (:selected-id state)) (= (:contractClass entry) (:selected-class state)))
        enabled? (:enabled entry)]
    (d/button
     {:type "button" :on-click #((:select-contract actions) (:id entry) (:contractClass entry))
      :on-mouse-enter #(boundary/hover-entry! % selected? true) :on-mouse-leave #(boundary/hover-entry! % selected? false)
      :style {:width "100%" :padding "10px 12px" :textAlign "left" :borderRadius (style/token state :radius :md)
              :border (str "1px solid " (if selected? (style/color state :accent :cyan) "transparent"))
              :background (if selected? "rgba(102, 217, 239, 0.08)" "transparent") :cursor "pointer"
              :transition "background 0.1s, border-color 0.1s" :marginLeft 8}}
     (d/div {:style (merge style/row {:justifyContent "space-between" :gap 8})}
       (d/div {:style (merge style/row {:gap 6 :minWidth 0})}
         (d/span {:style {:fontSize (style/token state :fontSize :sm) :fontWeight 500
                         :color (style/color state (if selected? :accent :fg) (if selected? :cyan :default))
                         :overflow "hidden" :textOverflow "ellipsis" :whiteSpace "nowrap"}}
           (or (:title entry) (:id entry))))
       (d/span {:style {:fontSize (style/token state :fontSize :xs) :padding "1px 6px"
                       :borderRadius (style/token state :radius :xs)
                       :background (if enabled? "rgba(166, 226, 46, 0.1)" "rgba(117, 113, 94, 0.15)")
                       :color (style/color state (if enabled? :accent :fg) (if enabled? :green :muted))}}
         (if enabled? "on" "off")))
     (d/div {:style (merge (style/small state) {:display "flex" :gap 8 :marginTop 4})}
       (d/span (:contractClass entry)) (d/span "·")
       (d/span (if (some? (:version entry)) (str "v" (:version entry)) "runtime"))))))

(hx/defnc contract-folder
  "Show matching folders during search and retain stored collapse state afterward."
  [{:keys [state actions folder entries]}]
  (let [collapsed? (contains? (set (:collapsed-folders state)) folder)
        searching? (:is-searching state)]
    (d/div
     (d/button {:type "button" :disabled searching?
                :on-click #(when-not searching? ((:toggle-folder actions) folder))
                :style (merge (style/heading state)
                              {:width "100%" :padding "8px 10px" :textAlign "left"
                               :borderRadius (style/token state :radius :sm) :border "none"
                               :background "rgba(255,255,255,0.03)" :display "flex" :alignItems "center" :gap 6
                               :cursor (if searching? "default" "pointer")})}
       (d/span {:style {:transform (if collapsed? "rotate(-90deg)" "rotate(0deg)") :transition "transform 0.15s"}}
         (if (or searching? collapsed?) "▸" "▾"))
       (str folder " (" (count entries) ")"))
     (when (or searching? (not collapsed?))
       (for [entry entries]
         (hx/$ contract-entry {:key (str (:contractClass entry) ":" (:id entry)) :state state :actions actions :entry entry}))))))

(hx/defnc library-panel
  "Render the visible contract library and its existing create and refresh affordances."
  [{:keys [state actions]}]
  (let [entries (:agent-entries state) classes (count (set (map :contractClass entries)))]
    (d/div {:style (style/sidebar state)}
      (d/div {:style (merge style/row {:padding "16px 16px 12px" :borderBottom (style/border state)
                                     :justifyContent "space-between" :gap 10})}
        (d/div {:style {:minWidth 0}}
          (d/div {:style {:fontSize (style/token state :fontSize :lg) :fontWeight 600 :color (style/color state :fg :default)}} "Contracts")
          (d/div {:style (assoc (style/small state) :marginTop 4)}
            (str (count entries) " contract" (when (not= 1 (count entries)) "s") " · " classes " class" (when (not= 1 classes) "es"))))
        (d/button {:type "button" :on-click (:toggle-left-panel actions)
                   :style (assoc (style/button state) :padding "4px 10px" :flexShrink 0)} "Hide"))
      (d/div {:style {:flex 1 :overflowY "auto" :padding "10px 10px 12px"}}
        (d/div {:style {:marginBottom 12}}
          (d/input {:value (:search-query state) :on-change #((:set-search actions) (boundary/input-value %))
                    :placeholder "Search contracts..."
                    :style (assoc (style/input state) :padding "8px 10px" :background (style/color state :bg :default))}))
        (cond
          (:loading-agents state) (d/div {:style (assoc (style/small state) :fontSize (style/token state :fontSize :sm) :padding "8px")} "Loading agents…")
          (empty? (:filtered-contracts state)) (d/div {:style (assoc (style/small state) :fontSize (style/token state :fontSize :sm) :padding "8px")} "No contracts found.")
          :else
          (d/div {:style {:display "flex" :flexDirection "column" :gap 4}}
            (for [[folder items] (:filtered-contracts state)]
              (hx/$ contract-folder {:key folder :state state :actions actions :folder folder :entries items}))
            (d/button {:type "button" :on-click (:new-contract actions)
                       :style {:width "100%" :padding "10px 12px" :textAlign "center"
                               :borderRadius (style/token state :radius :md) :border (str "1px dashed " (style/color state :fg :subtle))
                               :background "transparent" :cursor "pointer" :fontSize (style/token state :fontSize :sm)
                               :color (style/color state :fg :muted) :marginTop 8}} "+ New contract")))))))

(hx/defnc library-dock
  "Expose the library overlay or its collapsed desktop rail."
  [{:keys [state actions]}]
  (if (:show-left-panel state)
    (hx/$ library-panel {:state state :actions actions})
    (when-not (:is-narrow state)
      (d/div {:style (merge style/row {:width 38 :borderRight (style/border state)
                                     :background (style/color state :bg :darker) :justifyContent "center"})}
        (d/button {:type "button" :on-click (:toggle-left-panel actions)
                   :style (assoc (style/button state) :writingMode "vertical-rl" :transform "rotate(180deg)" :padding "10px 6px")} "Show")))))
