(ns knoxx.frontend.core
  "Browser entry point for the Shadow-owned application root and hot reload.
   Loading this bundle also exposes native component namespaces to existing
   TypeScript adapters. The app namespace owns the retained React root."
  (:require [knoxx.frontend.admin.event-agents-panel]
            [knoxx.frontend.app :as app]
            [knoxx.frontend.pages.documents.page]
            [knoxx.frontend.pages.settings.view]
            [knoxx.frontend.pages.source-doc.view]))

(defn ^:dev/after-load after-load
  "Render updated application code into the retained browser root."
  []
  (js/console.log "[knoxx-frontend] hot reload")
  (app/mount!))

(defn ^:dev/once init
  "Mount the authenticated application when the browser bundle loads."
  []
  (js/console.log "[knoxx-frontend] cljs bundle loaded")
  (app/mount!))
