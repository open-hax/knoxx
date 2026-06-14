(ns knoxx.frontend.components.layout.collapsed-panel-tab-test
  "Written FIRST (TDD) — defines the render contract for the Helix port of
  src/components/CollapsedPanelTab.tsx before the namespace exists."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            ["react-dom/server" :as rds]
            [helix.core :refer [$]]
            [knoxx.frontend.components.layout.collapsed-panel-tab :refer [collapsed-panel-tab]]))

(defn render [el]
  (rds/renderToStaticMarkup el))

(deftest vertical-edges-render-rotated-label
  (doseq [edge ["left" "right"]]
    (testing (str "edge " edge)
      (let [html (render ($ collapsed-panel-tab
                            {:label "Files" :edge edge :on-expand (fn [])}))]
        (is (str/includes? html "<button") (str edge ": is a button"))
        (is (str/includes? html "width:28px") (str edge ": fixed 28px width"))
        (is (str/includes? html "writing-mode:vertical-rl") (str edge ": rotated label"))
        (is (str/includes? html "Files") (str edge ": label text"))))))

(deftest edge-specific-borders
  (testing "left edge separates with a right border"
    (is (str/includes?
         (render ($ collapsed-panel-tab {:label "L" :edge "left" :on-expand (fn [])}))
         "border-right:1px solid var(--token-colors-border-default)")))
  (testing "right edge separates with a left border"
    (is (str/includes?
         (render ($ collapsed-panel-tab {:label "R" :edge "right" :on-expand (fn [])}))
         "border-left:1px solid var(--token-colors-border-default)")))
  (testing "bottom edge separates with a top border"
    (is (str/includes?
         (render ($ collapsed-panel-tab {:label "B" :edge "bottom" :on-expand (fn [])}))
         "border-top:1px solid var(--token-colors-border-default)"))))

(deftest bottom-edge-is-horizontal
  (let [html (render ($ collapsed-panel-tab
                        {:label "Terminal" :edge "bottom" :on-expand (fn [])}))]
    (is (str/includes? html "height:28px"))
    (is (str/includes? html "width:100%"))
    (is (not (str/includes? html "writing-mode")) "no rotated label on bottom edge")))

(deftest title-defaults-and-overrides
  (testing "default title and aria-label derive from label"
    (let [html (render ($ collapsed-panel-tab
                          {:label "Files" :edge "left" :on-expand (fn [])}))]
      (is (str/includes? html "title=\"Show Files panel\""))
      (is (str/includes? html "aria-label=\"Show Files panel\""))))
  (testing "explicit title wins"
    (let [html (render ($ collapsed-panel-tab
                          {:label "Files" :edge "left" :on-expand (fn [])
                           :title "Custom tip"}))]
      (is (str/includes? html "title=\"Custom tip\""))
      (is (str/includes? html "aria-label=\"Custom tip\"")))))
