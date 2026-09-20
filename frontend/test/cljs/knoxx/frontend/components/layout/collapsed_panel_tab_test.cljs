(ns knoxx.frontend.components.layout.collapsed-panel-tab-test
  "Written FIRST (TDD) — defines the render contract for the Helix port of
  src/components/CollapsedPanelTab.tsx before the namespace exists."
  (:require ["react-dom/server" :as rds]
            [cljs.test :as t]
            [clojure.string :as str]
            [helix.core :as hx]
            [knoxx.frontend.components.layout.collapsed-panel-tab :as collapsed]))

(defn- render [el]
  (rds/renderToStaticMarkup el))

(t/deftest vertical-edges-render-rotated-label
  (doseq [edge ["left" "right"]]
    (t/testing (str "edge " edge)
      (let [html (render (hx/$ collapsed/collapsed-panel-tab
                            {:label "Files" :edge edge :on-expand (fn [])}))]
        (t/is (str/includes? html "<button") (str edge ": is a button"))
        (t/is (str/includes? html "width:28px") (str edge ": fixed 28px width"))
        (t/is (str/includes? html "writing-mode:vertical-rl") (str edge ": rotated label"))
        (t/is (str/includes? html "Files") (str edge ": label text"))))))

(t/deftest edge-specific-borders
  (t/testing "left edge separates with a right border"
    (t/is (str/includes?
         (render (hx/$ collapsed/collapsed-panel-tab {:label "L" :edge "left" :on-expand (fn [])}))
         "border-right:1px solid var(--token-colors-border-default)")))
  (t/testing "right edge separates with a left border"
    (t/is (str/includes?
         (render (hx/$ collapsed/collapsed-panel-tab {:label "R" :edge "right" :on-expand (fn [])}))
         "border-left:1px solid var(--token-colors-border-default)")))
  (t/testing "bottom edge separates with a top border"
    (t/is (str/includes?
         (render (hx/$ collapsed/collapsed-panel-tab {:label "B" :edge "bottom" :on-expand (fn [])}))
         "border-top:1px solid var(--token-colors-border-default)"))))

(t/deftest bottom-edge-is-horizontal
  (let [html (render (hx/$ collapsed/collapsed-panel-tab
                        {:label "Terminal" :edge "bottom" :on-expand (fn [])}))]
    (t/is (str/includes? html "height:28px"))
    (t/is (str/includes? html "width:100%"))
    (t/is (not (str/includes? html "writing-mode")) "no rotated label on bottom edge")))

(t/deftest title-defaults-and-overrides
  (t/testing "default title and aria-label derive from label"
    (let [html (render (hx/$ collapsed/collapsed-panel-tab
                          {:label "Files" :edge "left" :on-expand (fn [])}))]
      (t/is (str/includes? html "title=\"Show Files panel\""))
      (t/is (str/includes? html "aria-label=\"Show Files panel\""))))
  (t/testing "explicit title wins"
    (let [html (render (hx/$ collapsed/collapsed-panel-tab
                          {:label "Files" :edge "left" :on-expand (fn [])
                           :title "Custom tip"}))]
      (t/is (str/includes? html "title=\"Custom tip\""))
      (t/is (str/includes? html "aria-label=\"Custom tip\"")))))
