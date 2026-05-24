(ns knoxx.backend.runtime.state)

;; Mutable runtime state registers (kept separate from env-only config).
;;
;; These are durable process-local handles that survive shadow-cljs HTTP route
;; reloads. Keep host module imports out of here; store only live process state.

(defonce config* (atom nil))
(defonce runtime* (atom nil))
(defonce policy-context* (atom nil))

(defn remember-context!
  [runtime config policy-context]
  (reset! runtime* runtime)
  (reset! config* config)
  (reset! policy-context* policy-context)
  true)

(defn current-policy-db
  []
  @policy-context*)
