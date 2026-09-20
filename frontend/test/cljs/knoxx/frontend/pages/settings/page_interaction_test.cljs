(ns knoxx.frontend.pages.settings.page-interaction-test
  "Written FIRST (TDD) — contract for the Helix port of
  src/pages/SettingsPage.tsx: frontend config card + runtime status rows
  (per-endpoint ping → OK / Unavailable). API ns mocked via set!."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.pages.settings.api :as api]
            [knoxx.frontend.pages.settings.view :as view]))

;; jsdom globals come from the :test build's :prepend-js.

(def ^:private ping-results (atom {}))
(def ^:private ping-calls (atom []))

(def ^:private real-config api/frontend-config)
(def ^:private real-ping api/ping)

(t/use-fixtures :each
  {:before (fn []
             (reset! ping-calls [])
             (reset! ping-results {"/api/config" true
                                   "/api/proxx/health" true
                                   "/api/admin/config/events" false
                                   "/api/admin/agents/active?limit=1" true})
             (set! api/frontend-config
                   (fn []
                     (js/Promise.resolve {:env "dev" :version "1.2.3"
                                          :github_enabled true :auth_required false})))
             (set! api/ping
                   (fn [url]
                     (swap! ping-calls conj url)
                     (js/Promise.resolve (get @ping-results url false)))))
   :after (fn []
            (rtl/cleanup)
            (set! api/frontend-config real-config)
            (set! api/ping real-ping))})

(defn- wait-until [msg pred]
  (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))))

(t/deftest ^:async renders-config-and-status-rows
  (let [r (rtl/render (hx/$ view/settings-page))]
    (await (wait-until "config card" #(some? (.queryByText r "1.2.3"))))
    (t/is (some? (.queryByText r "dev")) "environment shown")
    (t/is (some? (.queryByText r "Enabled")) "github oauth flag")
    (t/is (some? (.queryByText r "No")) "auth not required")
    (doseq [label ["Backend API" "Proxx Health" "Events" "Agents"]]
      (t/is (some? (.queryByText r label)) (str label " row present")))
    (await (wait-until "pings resolve" #(= 3 (count (.queryAllByText r "● OK")))))
    (t/is (some? (.queryByText r "✕ Unavailable")) "failing endpoint flagged")
    (t/is (= 4 (count @ping-calls)) "all four endpoints pinged")))

(t/deftest ^:async tolerates-config-failure
  (set! api/frontend-config (fn [] (js/Promise.reject (js/Error. "down"))))
  (let [r (rtl/render (hx/$ view/settings-page))]
    (await (wait-until "settings header" #(some? (.queryByText r "Settings"))))
    (t/is (pos? (count (.queryAllByText r "—"))) "missing config renders dashes")))
