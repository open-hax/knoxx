(ns knoxx.frontend.core
  "shadow-cljs browser entrypoint.

   This bundle is loaded into the Vite app as a plain script. It MUST NOT
   mount into #root (Vite owns the main React tree).

   Its job is to ensure CLJS namespaces are loaded so exported components are
   available under window.knoxx.* for TS wrappers to call." 
  (:require [knoxx.frontend.app :as app]
            [knoxx.frontend.admin.event-agents-panel]))

(defn ^:dev/after-load after-load []
  (js/console.log "[knoxx-frontend] hot reload")
  (app/mount!))

(defn ^:dev/once init []
  (js/console.log "[knoxx-frontend] cljs bundle loaded")
  (app/mount!))
