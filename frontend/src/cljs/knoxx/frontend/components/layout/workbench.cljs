(ns knoxx.frontend.components.layout.workbench
  "IDE-style workbench layout primitives.

   Shared across all pages. Provides collapsible, resizable sidebars
   and a main content area."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.components.layout.collapsed-panel-tab :as collapsed]
            [knoxx.frontend.lib.storage :as storage]))

(defn- stored-int-or [stored fallback min-value max-value]
  (let [parsed (when stored (js/parseInt stored 10))]
    (if (and parsed (js/Number.isFinite parsed))
      (min max-value (max min-value parsed))
      fallback)))

;; ── WorkbenchShell ──────────────────────────────────────────────────────────

(hx/defnc WorkbenchShell
  "Constrain the shared workspace shell to the available viewport."
  [{:keys [children class-name]}]
  (d/div {:class-name (or class-name "")
          :style #js {:display "flex"
                      :flex "1 1 0%"
                      :width "100%"
                      :height "100%"
                      :minWidth 0
                      :minHeight 0
                      :overflow "hidden"
                      :gap 0}}
         children))

(defn- initial-open? [storage-key]
  (let [stored (storage/safe-get-item (str storage-key "_open"))]
    (if (some? stored) (= stored "true") true)))

(defn- begin-window-drag! [cursor on-move]
  (set! (.-cursor (.-style js/document.body)) cursor)
  (set! (.-userSelect (.-style js/document.body)) "none")
  (letfn [(move [event] (on-move event))
          (up []
            (.removeEventListener js/window "mousemove" move)
            (.removeEventListener js/window "mouseup" up)
            (set! (.-cursor (.-style js/document.body)) "")
            (set! (.-userSelect (.-style js/document.body)) ""))]
    (.addEventListener js/window "mousemove" move)
    (.addEventListener js/window "mouseup" up)))

(hx/defnc PanelHeader
  "Render an optional panel header with its collapse action."
  [{:keys [label header on-collapse subtle]}]
  (when header
    (d/div {:style #js {:flexShrink 0
                        :display "flex"
                        :alignItems "center"
                        :justifyContent "space-between"
                        :padding "8px 10px"
                        :borderBottom (str "1px solid " (if subtle
                                                           "var(--token-colors-border-subtle)"
                                                           "var(--token-colors-border-default)"))
                        :gap 8
                        :background "var(--token-colors-background-surface)"}}
           (d/div {:style #js {:minWidth 0 :overflow "hidden"}} header)
           (d/button {:type "button"
                      :on-click on-collapse
                      :title (str "Collapse " label)
                      :style #js {:padding "2px 8px"
                                  :fontSize "11px"
                                  :border "1px solid var(--token-colors-border-default)"
                                  :borderRadius 4
                                  :background "var(--token-colors-background-surface)"
                                  :color "var(--token-colors-text-muted)"
                                  :cursor "pointer"
                                  :flexShrink 0}}
                     "Collapse"))))

(hx/defnc SideResizeHandle
  "Resize a sidebar by dragging toward or away from its selected edge."
  [{:keys [label edge width min-width max-width on-resize]}]
  (d/button {:type "button"
             :aria-label (str "Resize " label)
             :title (str "Resize " label)
             :style #js {:width 4
                         :minWidth 4
                         :cursor "col-resize"
                         :flexShrink 0
                         :background "var(--token-colors-border-default)"
                         :border "none"
                         :padding 0
                         :transition "background 0.15s"}
             :on-mouse-enter #(set! (.. % -currentTarget -style -background)
                                    "var(--token-colors-accent-blue)")
             :on-mouse-leave #(set! (.. % -currentTarget -style -background)
                                    "var(--token-colors-border-default)")
             :on-mouse-down
             (fn [event]
               (.preventDefault event)
               (let [start-x (.-clientX event)
                     start-width width
                     direction (if (= edge "left") 1 -1)]
                 (begin-window-drag!
                  "col-resize"
                  (fn [move-event]
                    (let [delta (* (- (.-clientX move-event) start-x) direction)
                          resized (min max-width (max min-width (+ start-width delta)))]
                      (on-resize resized))))))}))

(hx/defnc SidePanelFrame
  "Frame a sidebar at its persisted width and preserve its scroll boundary."
  [{:keys [children edge label header class-name width on-collapse]}]
  (d/aside {:class-name (or class-name "")
            :style #js {:width width
                        :minWidth width
                        :maxWidth width
                        :display "flex"
                        :flexDirection "column"
                        :minHeight 0
                        :overflow "hidden"
                        :background "var(--token-colors-background-surface)"
                        :borderRight (when (= edge "left") "1px solid var(--token-colors-border-default)")
                        :borderLeft (when (= edge "right") "1px solid var(--token-colors-border-default)")
                        :flexShrink 0}}
           (hx/$ PanelHeader {:label label :header header :on-collapse on-collapse})
           (d/div {:style #js {:flex 1
                               :minHeight 0
                               :overflow "hidden"
                               :display "flex"
                               :flexDirection "column"}}
                  children)))

;; ── WorkbenchPanel (left/right sidebar) ─────────────────────────────────────

(defn- use-persisted-panel [storage-key dimension default-size min-size max-size]
  (let [[open? set-open!] (hooks/use-state #(initial-open? storage-key))
        [size set-size!] (hooks/use-state
                         #(stored-int-or (storage/safe-get-item (str storage-key "_" dimension))
                                         default-size min-size max-size))]
    (hooks/use-effect [open?]
      (storage/safe-set-item (str storage-key "_open") (str open?))
      nil)
    (hooks/use-effect [size]
      (storage/safe-set-item (str storage-key "_" dimension) (str size))
      nil)
    {:open? open? :set-open! set-open! :size size :set-size! set-size!}))

(hx/defnc ExpandedSidePanel
  "Place the resize handle on the inner edge of an expanded sidebar."
  [{:keys [children edge label header class-name min-width max-width width on-resize on-collapse]}]
  (let [resize-handle (hx/$ SideResizeHandle {:label label :edge edge :width width
                                            :min-width min-width :max-width max-width :on-resize on-resize})]
    (d/div {:style #js {:display "flex" :height "100%" :minHeight 0 :overflow "hidden" :flexShrink 0}}
      (when (= edge "right") resize-handle)
      (hx/$ SidePanelFrame {:label label :edge edge :header header :class-name class-name
                             :width width :on-collapse on-collapse} children)
      (when (= edge "left") resize-handle))))

(hx/defnc WorkbenchPanel
  "Maintain a collapsible sidebar with bounded width persisted in local storage."
  [{:keys [children edge label storage-key default-width min-width max-width class-name header]
    :or {default-width 320 min-width 200 max-width 600}}]
  (let [{:keys [open? set-open! size set-size!]}
        (use-persisted-panel storage-key "width" default-width min-width max-width)]
    (if-not open?
      (hx/$ collapsed/collapsed-panel-tab {:label label :edge edge :on-expand #(set-open! true)
                                           :title (str "Show " label " panel")})
      (hx/$ ExpandedSidePanel {:label label :edge edge :header header :class-name class-name
                                :width size :min-width min-width :max-width max-width
                                :on-resize set-size! :on-collapse #(set-open! false)} children))))

;; ── WorkbenchMain ───────────────────────────────────────────────────────────

(hx/defnc WorkbenchMain
  "Compose the flexible main viewport and optional bottom panel."
  [{:keys [children class-name bottom-panel]}]
  (d/div {:class-name (or class-name "")
          :style #js {:flex 1
                      :minWidth 0
                      :overflow "hidden"
                      :display "flex"
                      :flexDirection "column"
                      :minHeight 0}}
         (d/div {:style #js {:flex 1
                             :minHeight 0
                             :overflow "hidden"
                             :display "flex"
                             :flexDirection "column"}}
                children)
         bottom-panel))

;; ── WorkbenchBottomPanel ────────────────────────────────────────────────────

(hx/defnc BottomResizeHandle
  "Resize the bottom panel with a vertically clamped drag."
  [{:keys [label height min-height max-height on-resize]}]
  (d/button {:type "button"
             :aria-label (str "Resize " label)
             :title (str "Resize " label)
             :style #js {:position "absolute"
                         :top -3
                         :left 0
                         :right 0
                         :height 6
                         :cursor "row-resize"
                         :zIndex 10
                         :border "none"
                         :background "transparent"
                         :padding 0}
             :on-mouse-down
             (fn [event]
               (.preventDefault event)
               (let [start-y (.-clientY event)
                     start-height height]
                 (begin-window-drag!
                  "row-resize"
                  (fn [move-event]
                    (let [delta (- start-y (.-clientY move-event))
                          resized (min max-height (max min-height (+ start-height delta)))]
                      (on-resize resized))))))}))

(hx/defnc BottomPanelFrame
  "Frame bottom content at its persisted height with an optional header."
  [{:keys [children label header class-name height on-collapse resize-handle]}]
  (d/div {:class-name (or class-name "")
          :style #js {:height height
                      :minHeight height
                      :maxHeight height
                      :display "flex"
                      :flexDirection "column"
                      :minWidth 0
                      :overflow "hidden"
                      :flexShrink 0
                      :borderTop "1px solid var(--token-colors-border-default)"
                      :position "relative"}}
         resize-handle
         (hx/$ PanelHeader {:label label
                         :header header
                         :on-collapse on-collapse
                         :subtle true})
         (d/div {:style #js {:flex 1 :minHeight 0 :overflow "hidden"}}
                children)))

(hx/defnc WorkbenchBottomPanel
  "Maintain a collapsible bottom panel with a bounded persisted height."
  [{:keys [children label storage-key default-height min-height max-height class-name header]
    :or {default-height 240 min-height 120 max-height 600}}]
  (let [{:keys [open? set-open! size set-size!]}
        (use-persisted-panel storage-key "height" default-height min-height max-height)]
    (if-not open?
      (hx/$ collapsed/collapsed-panel-tab {:label label :edge "bottom" :on-expand #(set-open! true)
                                           :title (str "Show " label " panel")})
      (hx/$ BottomPanelFrame {:label label :header header :class-name class-name :height size
                               :on-collapse #(set-open! false)
                               :resize-handle (hx/$ BottomResizeHandle {:label label :height size
                                                                          :min-height min-height :max-height max-height
                                                                          :on-resize set-size!})} children))))
