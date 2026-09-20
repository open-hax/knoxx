(ns knoxx.frontend.components.layout.workbench-interaction-test
  "Exercise persisted panel state and real drag events through Helix rendering."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.components.layout.workbench :as workbench]))

(def ^:private stored (atom {}))
(def ^:private previous-storage (atom nil))

(t/use-fixtures :each
  {:before (fn []
             (reset! previous-storage (.-localStorage js/globalThis))
             (reset! stored {})
             (set! (.-localStorage js/globalThis)
                   #js {:getItem #(get @stored % nil)
                        :setItem #(swap! stored assoc %1 %2)}))
   :after (fn []
            (.mouseUp rtl/fireEvent js/window)
            (rtl/cleanup)
            (set! (.-localStorage js/globalThis) @previous-storage))})

(defn- side-panel [edge]
  (rtl/render (hx/$ workbench/WorkbenchPanel
                   {:edge edge :label "Files" :storage-key "layout-probe"
                    :header "Files header" :default-width 300 :min-width 200 :max-width 450}
                   "Panel contents")))

(defn- drag! [handle start-position end-position]
  (.mouseDown rtl/fireEvent handle (clj->js start-position))
  (.mouseMove rtl/fireEvent js/window (clj->js end-position))
  (.mouseUp rtl/fireEvent js/window))

(t/deftest restores-collapsed-state-and-keeps-resized-width-across-remount
  (reset! stored {"layout-probe_open" "false" "layout-probe_width" "900"})
  (let [rendered (side-panel "left")]
    (t/is (nil? (.queryByText rendered "Panel contents")))
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Show Files panel"}))
    (t/is (= "450px" (.. rendered -container (querySelector "aside") -style -width))
          "stored dimensions are clamped before rendering")
    (drag! (.getByRole rendered "button" #js {:name "Resize Files"})
           {:clientX 100} {:clientX 0})
    (t/is (= "350" (get @stored "layout-probe_width")))
    (.click rtl/fireEvent (.getByTitle rendered "Collapse Files"))
    (t/is (= "false" (get @stored "layout-probe_open")))
    (.unmount rendered)
    (let [remounted (side-panel "left")]
      (.click rtl/fireEvent (.getByRole remounted "button" #js {:name "Show Files panel"}))
      (t/is (= "350px" (.. remounted -container (querySelector "aside") -style -width))))))

(t/deftest right-sidebar-drag-reverses-direction-and-clamps-width
  (let [rendered (side-panel "right")
        handle (.getByRole rendered "button" #js {:name "Resize Files"})]
    (drag! handle {:clientX 100} {:clientX 150})
    (t/is (= "250" (get @stored "layout-probe_width")))
    (drag! handle {:clientX 100} {:clientX 1000})
    (t/is (= "200" (get @stored "layout-probe_width")))
    (t/is (= "" (.. js/document -body -style -cursor)))
    (t/is (= "" (.. js/document -body -style -userSelect)))))

(t/deftest bottom-panel-drag-persists-height-and-collapse-independently
  (reset! stored {"bottom-probe_height" "invalid"})
  (let [rendered (rtl/render
                 (hx/$ workbench/WorkbenchBottomPanel
                       {:label "Terminal" :storage-key "bottom-probe" :header "Terminal header"
                        :default-height 240 :min-height 120 :max-height 260}
                       "Terminal content"))]
    (t/is (= "240" (get @stored "bottom-probe_height")))
    (drag! (.getByRole rendered "button" #js {:name "Resize Terminal"})
           {:clientY 100} {:clientY 0})
    (t/is (= "260" (get @stored "bottom-probe_height")))
    (.click rtl/fireEvent (.getByTitle rendered "Collapse Terminal"))
    (t/is (nil? (.queryByText rendered "Terminal content")))
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Show Terminal panel"}))
    (t/is (some? (.queryByText rendered "Terminal content")))))
