(ns knoxx.frontend.components.layout.workbench
  "IDE-style workbench layout primitives.

   Shared across all pages. Provides collapsible, resizable sidebars
   and a main content area."
  (:require [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            ["@open-hax/knoxx-app-bridge" :refer [CollapsedPanelTab]]))

;; ── safe localStorage helpers ───────────────────────────────────────────────

(defn- safe-set-item [key value]
  (try
    (.setItem js/localStorage key value)
    (catch js/Error _
      ;; quota exceeded — clear old knoxx keys
      (try
        (let [keys-to-remove (atom [])]
          (doseq [i (range (.-length js/localStorage))]
            (when-let [k (.key js/localStorage i)]
              (when (and (str/starts-with? k "knoxx_")
                         (not= k key))
                (swap! keys-to-remove conj k))))
          (doseq [k (take (Math/floor (/ (count @keys-to-remove) 2))
                          @keys-to-remove)]
            (try (.removeItem js/localStorage k)
                 (catch js/Error _ nil))))
        (.setItem js/localStorage key value)
        (catch js/Error _ nil)))))

(defn- safe-get-item [key]
  (try
    (.getItem js/localStorage key)
    (catch js/Error _ nil)))

(defn- stored-int-or [stored fallback min-value max-value]
  (let [parsed (when stored (js/parseInt stored 10))]
    (if (and parsed (js/Number.isFinite parsed))
      (min max-value (max min-value parsed))
      fallback)))

;; ── WorkbenchShell ──────────────────────────────────────────────────────────

(defnc WorkbenchShell
  [{:keys [children class-name]}]
  (d/div {:class-name (or class-name "")
          :style #js {:display "flex"
                      :height "100%"
                      :minHeight 0
                      :overflow "hidden"
                      :gap 0}}
         children))

(defn- initial-open? [storage-key]
  (let [stored (safe-get-item (str storage-key "_open"))]
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

(defnc PanelHeader
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

(defnc SideResizeHandle
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
                          next (min max-width (max min-width (+ start-width delta)))]
                      (on-resize next))))))}))

(defnc SidePanelFrame
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
           ($ PanelHeader {:label label :header header :on-collapse on-collapse})
           (d/div {:style #js {:flex 1
                               :minHeight 0
                               :overflow "hidden"
                               :display "flex"
                               :flexDirection "column"}}
                  children)))

;; ── WorkbenchPanel (left/right sidebar) ─────────────────────────────────────

(defnc WorkbenchPanel
  [{:keys [children edge label storage-key
            default-width min-width max-width
            class-name header]
    :or {default-width 320
         min-width 200
         max-width 600}}]
  (let [[open? set-open!] (hooks/use-state #(initial-open? storage-key))
         [width set-width!] (hooks/use-state
                              (fn []
                                (stored-int-or (safe-get-item (str storage-key "_width"))
                                               default-width
                                               min-width
                                               max-width)))]

    (hooks/use-effect
     [open?]
     (safe-set-item (str storage-key "_open") (str open?))
     nil)

    (hooks/use-effect
     [width]
     (safe-set-item (str storage-key "_width") (str width))
     nil)

    (if-not open?
      ($ CollapsedPanelTab
         {:label label
           :edge edge
           :onExpand #(set-open! true)
           :title (str "Show " label " panel")})
       (let [resize-handle ($ SideResizeHandle {:label label
                                                :edge edge
                                                :width width
                                                :min-width min-width
                                                :max-width max-width
                                                :on-resize set-width!})
             panel ($ SidePanelFrame {:label label
                                      :edge edge
                                      :header header
                                      :class-name class-name
                                      :width width
                                      :on-collapse #(set-open! false)}
                      children)]
         (d/div {:style #js {:display "flex"
                             :height "100%"
                             :minHeight 0
                             :overflow "hidden"
                             :flexShrink 0}}
                (when (= edge "right") resize-handle)
                panel
                (when (= edge "left") resize-handle))))))

;; ── WorkbenchMain ───────────────────────────────────────────────────────────

(defnc WorkbenchMain
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

(defnc BottomResizeHandle
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
                          next (min max-height (max min-height (+ start-height delta)))]
                      (on-resize next))))))}))

(defnc BottomPanelFrame
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
         ($ PanelHeader {:label label
                         :header header
                         :on-collapse on-collapse
                         :subtle true})
         (d/div {:style #js {:flex 1 :minHeight 0 :overflow "hidden"}}
                children)))

(defnc WorkbenchBottomPanel
  [{:keys [children label storage-key
           default-height min-height max-height
           class-name header]
    :or {default-height 240
         min-height 120
         max-height 600}}]
  (let [[open? set-open!] (hooks/use-state #(initial-open? storage-key))
         [height set-height!] (hooks/use-state
                               (fn []
                                 (stored-int-or (safe-get-item (str storage-key "_height"))
                                                default-height
                                                min-height
                                                max-height)))]

    (hooks/use-effect
     [open?]
     (safe-set-item (str storage-key "_open") (str open?))
     nil)

    (hooks/use-effect
     [height]
     (safe-set-item (str storage-key "_height") (str height))
     nil)

    (if-not open?
      ($ CollapsedPanelTab
         {:label label
           :edge "bottom"
           :onExpand #(set-open! true)
           :title (str "Show " label " panel")})
      ($ BottomPanelFrame {:label label
                           :header header
                           :class-name class-name
                           :height height
                           :on-collapse #(set-open! false)
                           :resize-handle ($ BottomResizeHandle {:label label
                                                                 :height height
                                                                 :min-height min-height
                                                                 :max-height max-height
                                                                 :on-resize set-height!})}
         children))))
