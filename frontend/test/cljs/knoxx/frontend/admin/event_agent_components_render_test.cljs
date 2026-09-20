(ns knoxx.frontend.admin.event-agent-components-render-test
  "Renders real Helix view components (not pure logic) under the :node-test
  build via react-dom/server. Proves Helix components are unit-testable in
  node; the only known blocker is ESM-only npm deps (uxx-helix) vs this
  build's CJS output."
  (:require ["react-dom/server" :as rds]
            [cljs.test :as t]
            [clojure.string :as str]
            [helix.core :as hx]
            [knoxx.frontend.admin.event-agent-components :as c]
            [knoxx.frontend.admin.event-agent-infrastructure :as infra]))

(defn- render [el]
  (rds/renderToStaticMarkup el))

(t/deftest badge-renders-tone-and-children
  (t/testing "success tone"
    (let [html (render (hx/$ c/badge {:tone :success} "all good"))]
      (t/is (str/includes? html "all good"))
      (t/is (str/includes? html "text-emerald-200"))))
  (t/testing "unknown tone falls back to default classes"
    (let [html (render (hx/$ c/badge {:tone :nonsense} "meh"))]
      (t/is (str/includes? html "text-slate-200")))))

(t/deftest status-badge-conditional-rendering
  (t/testing "disabled wins over everything"
    (let [html (render (hx/$ c/status-badge {:status "ok" :enabled false :running true}))]
      (t/is (str/includes? html "disabled"))
      (t/is (str/includes? html "text-amber-200"))))
  (t/testing "running"
    (let [html (render (hx/$ c/status-badge {:status "ok" :enabled true :running true}))]
      (t/is (str/includes? html "running"))
      (t/is (str/includes? html "text-cyan-200"))))
  (t/testing "ok status when idle"
    (let [html (render (hx/$ c/status-badge {:status "ok" :enabled true :running false}))]
      (t/is (str/includes? html "ok"))
      (t/is (str/includes? html "text-emerald-200")))))

(t/deftest collapsible-panel-renders-structure
  (let [html (render (hx/$ c/collapsible-panel
                        {:title "Panel title"
                         :description "Panel description"
                         :default-open true}
                        (hx/$ :span "panel body")))]
    (t/is (str/includes? html "<details"))
    (t/is (str/includes? html "open"))
    (t/is (str/includes? html "Panel title"))
    (t/is (str/includes? html "Panel description"))
    (t/is (str/includes? html "panel body"))))

(t/deftest hooks-component-renders-initial-state
  ;; event-dispatch calls hooks/use-state three times — proves hook-using
  ;; Helix components render in node, with their initial state visible.
  (let [html (render (hx/$ infra/event-dispatch
                        {:can-manage true
                         :dispatching-event false
                         :on-dispatch (fn [_ _ _])}))]
    (t/is (str/includes? html "Source kind"))
    (t/is (str/includes? html "discord"))
    (t/is (str/includes? html "Payload JSON"))
    (t/is (str/includes? html "{}"))))
