(ns knoxx.backend.entrypoint
  "All-CLJS backend entrypoint.

   The runtime now owns its Node module imports from CLJS namespaces instead of
   receiving a dependency bundle from this entrypoint. Keep this namespace thin:
   shadow-cljs calls `init`, and bootstrap/lifecycle code decides what to start."
  (:require [knoxx.backend.bootstrap :as bootstrap]))

(defn init
  "shadow-cljs module init-fn. Starts the Knoxx backend."
  []
  (bootstrap/bootstrap!))
