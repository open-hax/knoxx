(ns knoxx.backend.runtime.state)

;; Mutable runtime state registers (kept separate from env-only config).

(defonce config* (atom nil))
