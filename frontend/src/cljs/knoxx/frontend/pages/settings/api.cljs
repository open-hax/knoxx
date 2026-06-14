(ns knoxx.frontend.pages.settings.api
  "Settings page REST calls. CLJS port of getFrontendConfig plus the
   status-row pings from src/pages/SettingsPage.tsx."
  (:require [knoxx.frontend.lib.api :as api]))

(defn frontend-config []
  (api/request "/api/config"))

(defn ping
  "Resolves to true when `url` answers 2xx; false on error or rejection."
  [url]
  (-> (js/fetch url #js {:credentials "same-origin"})
      (.then (fn [^js res] (.-ok res)))
      (.catch (fn [_] false))))
