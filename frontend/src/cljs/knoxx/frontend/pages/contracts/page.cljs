(ns knoxx.frontend.pages.contracts.page
  "Compose the native contract view with established React controller and widget instances."
  (:require [helix.core :as hx]
            [knoxx.frontend.lib.contracts-view :as boundary]
            [knoxx.frontend.pages.contracts.view :as view]))

(hx/defnc ContractsPage
  "Mount the existing controller and provide its opaque editor and chat components."
  [{:keys [Controller Editor ChatPane]}]
  (hx/$ Controller
    {:render (fn [raw]
               (hx/$ view/contracts-view
                 {& (assoc (boundary/decode-controller raw) :editor Editor :chat-pane ChatPane)}))}))
