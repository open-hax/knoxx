(ns knoxx.backend.runtime.state)

;; Mutable runtime state registers (kept separate from env-only config).
;;
;; These are durable process-local handles that survive shadow-cljs HTTP route
;; reloads. Keep host module imports out of here; store only live process state.

(defonce config* (atom nil))
(defonce runtime* (atom nil))
(defonce policy-db* (atom nil))

(defn remember-context!
  [runtime config policy-db]
  (reset! runtime* runtime)
  (reset! config* config)
  (reset! policy-db* policy-db)
  true)

(defn current-policy-db
  []
  @policy-db*)
