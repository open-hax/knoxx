(ns knoxx.frontend.core
  "Helix entry point for the Knoxx frontend.

   shadow-cljs mounts this namespace at process boot and hot-reloads it
   via the ^:dev/after-load hook."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.dom :as d]
            ["react-dom/client" :as rdc]
            ["@open-hax/knoxx-frontend-bridge" :as bridge]))

(defnc app []
  (d/div {:class-name "min-h-screen bg-slate-900 text-white p-8"}
    (d/h1 {:class-name "text-3xl font-bold mb-4"}
      "Knoxx")
    (d/p "Hello from shadow-cljs + helix!")
    ;; Mount a TypeScript component via the bridge
    ($ bridge/EmptyState {:title "Nothing here yet"
                          :message "This TS component is imported from shadow-cljs."
                          :icon "🧪"})))

(defn ^:dev/after-load after-load []
  (println "[knoxx-frontend] hot reload"))

(defn ^:dev/once init []
  (let [root-el (js/document.getElementById "root")
        root (.createRoot rdc root-el)]
    (.render root ($ app))))