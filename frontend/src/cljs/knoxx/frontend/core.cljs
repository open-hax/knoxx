(ns knoxx.frontend.core
  "shadow-cljs browser entrypoint.

   This bundle is loaded into the Vite app as a plain script. It MUST NOT
   mount into #root (Vite owns the main React tree).

   Its job is to ensure CLJS namespaces are loaded so exported components are
   available under window.knoxx.* for TS wrappers to call." 
  (:require [knoxx.frontend.admin.event-agents-panel]
            [knoxx.frontend.app :as app]
            [knoxx.frontend.lib.ws]
            [knoxx.frontend.pages.documents.page]
            [knoxx.frontend.pages.settings.view]
            [knoxx.frontend.pages.source-doc.view]))

(defn ^:dev/after-load after-load
  "Remount the existing app after a development source reload."
  []
  (js/console.log "[knoxx-frontend] hot reload")
  (app/mount!))

(defn ^:dev/once init
  "Mount the existing app after loading its compiled entrypoint."
  []
  (js/console.log "[knoxx-frontend] cljs bundle loaded")
  (app/mount!))
