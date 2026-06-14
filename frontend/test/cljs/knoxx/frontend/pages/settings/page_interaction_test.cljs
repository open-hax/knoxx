(ns knoxx.frontend.pages.settings.page-interaction-test
  "Written FIRST (TDD) — contract for the Helix port of
  src/pages/SettingsPage.tsx: frontend config card + runtime status rows
  (per-endpoint ping → OK / Unavailable). API ns mocked via set!."
  (:require [cljs.test :refer [deftest is async use-fixtures]]
            ["@testing-library/react" :as rtl]
            [helix.core :refer [$]]
            [knoxx.frontend.pages.settings.api :as api]
            [knoxx.frontend.pages.settings.view :refer [settings-page]]))

;; jsdom globals come from the :test build's :prepend-js.

(def ping-results (atom {}))
(def ping-calls (atom []))

(def ^:private real-config api/frontend-config)
(def ^:private real-ping api/ping)

(use-fixtures :each
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

(deftest renders-config-and-status-rows
  (async done
    (let [r (rtl/render ($ settings-page))]
      (-> (wait-until "config card" #(some? (.queryByText r "1.2.3")))
          (.then (fn []
                   (is (some? (.queryByText r "dev")) "environment shown")
                   (is (some? (.queryByText r "Enabled")) "github oauth flag")
                   (is (some? (.queryByText r "No")) "auth not required")
                   (doseq [label ["Backend API" "Proxx Health" "Events" "Agents"]]
                     (is (some? (.queryByText r label)) (str label " row present")))
                   (wait-until "pings resolve"
                               #(= 3 (count (.queryAllByText r "● OK"))))))
          (.then (fn []
                   (is (some? (.queryByText r "✕ Unavailable")) "failing endpoint flagged")
                   (is (= 4 (count @ping-calls)) "all four endpoints pinged")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest tolerates-config-failure
  (async done
    (set! api/frontend-config (fn [] (js/Promise.reject (js/Error. "down"))))
    (let [r (rtl/render ($ settings-page))]
      (-> (wait-until "settings header" #(some? (.queryByText r "Settings")))
          (.then (fn []
                   (is (pos? (count (.queryAllByText r "—"))) "missing config renders dashes")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))
