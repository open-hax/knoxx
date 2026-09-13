(ns knoxx.frontend.pages.settings.api
  "Settings page REST calls. CLJS port of getFrontendConfig plus the
   status-row pings from src/pages/SettingsPage.tsx."
  (:require [knoxx.frontend.lib.api :as api]))

(defn frontend-config
  "Read runtime configuration exposed to the authenticated frontend."
  []
  (api/request "/api/config"))

(defn ^:async ping
  "Resolves to true when `url` answers 2xx; false on error or rejection."
  [url]
  (try
    (let [^js response (await (js/fetch url #js {:credentials "same-origin"}))]
      (.-ok response))
    (catch :default _error false)))
