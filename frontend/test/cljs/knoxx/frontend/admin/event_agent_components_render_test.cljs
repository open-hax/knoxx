(ns knoxx.frontend.admin.event-agent-components-render-test
  "Renders real Helix view components (not pure logic) under the :node-test
  build via react-dom/server. Proves Helix components are unit-testable in
  node; the only known blocker is ESM-only npm deps (uxx-helix) vs this
  build's CJS output."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            ["react-dom/server" :as rds]
            [helix.core :refer [$]]
            [knoxx.frontend.admin.event-agent-components :as c]
            [knoxx.frontend.admin.event-agent-infrastructure :as infra]))

(defn render [el]
  (rds/renderToStaticMarkup el))

(deftest badge-renders-tone-and-children
  (testing "success tone"
    (let [html (render ($ c/badge {:tone :success} "all good"))]
      (is (str/includes? html "all good"))
      (is (str/includes? html "text-emerald-200"))))
  (testing "unknown tone falls back to default classes"
    (let [html (render ($ c/badge {:tone :nonsense} "meh"))]
      (is (str/includes? html "text-slate-200")))))

(deftest status-badge-conditional-rendering
  (testing "disabled wins over everything"
    (let [html (render ($ c/status-badge {:status "ok" :enabled false :running true}))]
      (is (str/includes? html "disabled"))
      (is (str/includes? html "text-amber-200"))))
  (testing "running"
    (let [html (render ($ c/status-badge {:status "ok" :enabled true :running true}))]
      (is (str/includes? html "running"))
      (is (str/includes? html "text-cyan-200"))))
  (testing "ok status when idle"
    (let [html (render ($ c/status-badge {:status "ok" :enabled true :running false}))]
      (is (str/includes? html "ok"))
      (is (str/includes? html "text-emerald-200")))))

(deftest collapsible-panel-renders-structure
  (let [html (render ($ c/collapsible-panel
                        {:title "Panel title"
                         :description "Panel description"
                         :default-open true}
                        ($ :span "panel body")))]
    (is (str/includes? html "<details"))
    (is (str/includes? html "open"))
    (is (str/includes? html "Panel title"))
    (is (str/includes? html "Panel description"))
    (is (str/includes? html "panel body"))))

(deftest hooks-component-renders-initial-state
  ;; event-dispatch calls hooks/use-state three times — proves hook-using
  ;; Helix components render in node, with their initial state visible.
  (let [html (render ($ infra/event-dispatch
                        {:can-manage true
                         :dispatching-event false
                         :on-dispatch (fn [_ _ _])}))]
    (is (str/includes? html "Source kind"))
    (is (str/includes? html "discord"))
    (is (str/includes? html "Payload JSON"))
    (is (str/includes? html "{}"))))
